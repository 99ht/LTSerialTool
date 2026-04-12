package indi.lt.serialtool.service;

import com.fazecast.jSerialComm.SerialPort;
import indi.lt.serialtool.component.PromptInlineCssTextArea;
import indi.lt.serialtool.constant.LogType;
import indi.lt.serialtool.data.LogText;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.CheckBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 串口读取服务：
 * - 按 UTF-8 增量解码，避免多字节拆包导致乱码
 * - 按行切分，统一使用 '\n' 结尾；支持 \r\n
 * - 追加到 InlineCssTextArea 后按“最大行数”裁剪（段落级，性能更好）
 * - 可选集成高亮器：每次追加/裁剪后调度一次高亮刷新
 */
public class SerialReadService extends Service<LogText> {
    private static final Logger LOG = LogManager.getLogger(SerialReadService.class);

    private final SerialPort comPort;
    private final PromptInlineCssTextArea targetTextArea;

    /**
     * 可选高亮调度器：例如你实现的 InlineCssRegexHighlighter，提供 schedule() 即可
     */
    public interface HighlighterScheduler {
        void schedule();
    }

    private final HighlighterScheduler highlighterScheduler; // 可为 null

    /**
     * 一次读取的缓冲大小（字节）
     */
    private static final int READ_BUF_SIZE = 2048;

    /**
     * 是否显示时间戳
     */
    private final SimpleBooleanProperty timeStampDisplayProperty = new SimpleBooleanProperty(false);

    /**
     * 接收字节数统计
     */
    private final AtomicLong recvBytesCount = new AtomicLong(0);

    /**
     * 接收字节数变化回调
     */
    private Consumer<Long> onRecvBytesChanged;

    // —— 构造 —— //

    public SerialReadService(SerialPort comPort,
                             PromptInlineCssTextArea targetTextArea,
                             BooleanProperty timeStampDisplayProperty,
                             HighlighterScheduler highlighter) {
        this(comPort, targetTextArea, timeStampDisplayProperty, highlighter, false);
    }
    public SerialReadService(SerialPort comPort,
                             PromptInlineCssTextArea targetTextArea,
                             BooleanProperty timeStampDisplayProperty,
                             HighlighterScheduler highlighter,
                             boolean showLogType) {
        this.comPort = Objects.requireNonNull(comPort);
        this.targetTextArea = targetTextArea;
        this.timeStampDisplayProperty.bind(timeStampDisplayProperty);
        this.highlighterScheduler = highlighter;

        valueProperty().addListener((observable, oldValue, newValue) -> {
            String logText = newValue.getLogText(timeStampDisplayProperty.get(), showLogType);
            targetTextArea.appendText(logText + "\n");
        });
    }

    @Override
    protected Task<LogText> createTask() {
        return new Task<>() {
            @Override
            protected LogText call() {
                try (InputStream in = comPort.getInputStream()) {

                    // UTF-8 增量解码器：跨包安全
                    CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                            .onMalformedInput(CodingErrorAction.REPLACE)
                            .onUnmappableCharacter(CodingErrorAction.REPLACE);

                    byte[] rawBuf = new byte[READ_BUF_SIZE];
                    ByteBuffer byteBuf = ByteBuffer.allocate(READ_BUF_SIZE * 2);
                    CharBuffer charBuf = CharBuffer.allocate(READ_BUF_SIZE * 2);

                    // 行与 UI 批量缓冲
                    StringBuilder lineBuf = new StringBuilder();
                    StringBuilder uiBatch = new StringBuilder();

                    while (!isCancelled()) {
                        int n = in.read(rawBuf);
                        if (n < 0) break;       // EOF
                        if (n == 0) continue;   // 无数据

                        // 统计接收字节数
                        long totalBytes = recvBytesCount.addAndGet(n);
                        if (onRecvBytesChanged != null) {
                            onRecvBytesChanged.accept(totalBytes);
                        }

                        // 解码
                        byteBuf.clear();
                        byteBuf.put(rawBuf, 0, n).flip();
                        while (byteBuf.hasRemaining()) {
                            CoderResult cr = decoder.decode(byteBuf, charBuf, false);
                            charBuf.flip();
                            if (charBuf.hasRemaining()) {
                                feedChars(charBuf, lineBuf, uiBatch);
                                charBuf.clear();
                            }
                            if (cr.isError()) cr.throwException();
                            if (cr.isUnderflow()) break;
                        }

                        // 批量刷 UI
                        if (!uiBatch.isEmpty()) {
                            String batch = uiBatch.toString();
                            uiBatch.setLength(0);
                            updateValue(formatLine(batch));
                        }
                    }

                    // 收尾：如果还有未结束的一行（无换行结尾）
                    if (!lineBuf.isEmpty()) {
                        LogText leftover = formatLine(lineBuf.toString());
                        updateValue(leftover);
                    }
                } catch (CharacterCodingException e) {
                    LOG.error("UTF-8 decoding error", e);
                } catch (IOException e) {
                    if (!isCancelled()) {
                        LOG.error("Serial read error", e);
                    }
                } finally {
                    tryClosePort();
                }
                return null;
            }
        };
    }

    private void tryClosePort() {
        try {
            if (comPort != null && comPort.isOpen()) {
                comPort.closePort();
            }
        } catch (Exception ex) {
            LOG.warn("closePort failed", ex);
        }
    }

    private void appendText(String batch) {
        targetTextArea.appendText(batch);
        if (highlighterScheduler != null) highlighterScheduler.schedule();
    }

    /**
     * 将解码后的字符流按行切分追加到 uiBatch。
     * 支持 \n 与 \r\n；统一以 '\n' 结尾。
     */
    private void feedChars(CharBuffer chars, StringBuilder lineBuf, StringBuilder uiBatch) {
        while (chars.hasRemaining()) {
            char c = chars.get();
            if (c == '\n') {
                // 处理 \r\n：去掉行尾 \r
                int end = lineBuf.length();
                if (end > 0 && lineBuf.charAt(end - 1) == '\r') {
                    lineBuf.setLength(end - 1);
                }
                uiBatch.append(formatLine(lineBuf.toString()).getText());
                lineBuf.setLength(0);
            } else {
                lineBuf.append(c);
            }
        }
    }

    @Override
    public void start() {
        super.start();
        LOG.info("串口 [" + comPort + "] 读取服务开启");
    }

    @Override
    public boolean cancel() {
        if (super.cancel()) {
            LOG.info("串口 [" + comPort + "] 读取服务关闭");
            return true;
        }
        LOG.error("串口 [" + comPort + "] 读取服务失败");
        return false;
    }

    /**
     * 设置接收字节数变化回调
     */
    public void setOnRecvBytesChanged(Consumer<Long> callback) {
        this.onRecvBytesChanged = callback;
    }

    /**
     * 获取接收字节数
     */
    public long getRecvBytesCount() {
        return recvBytesCount.get();
    }

    /**
     * 重置接收字节数
     */
    public void resetRecvBytesCount() {
        recvBytesCount.set(0);
        if (onRecvBytesChanged != null) {
            onRecvBytesChanged.accept(0L);
        }
    }

    /**
     * 根据是否带时间戳，格式化一行，并追加换行符。
     */
    private LogText formatLine(String raw) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        return new LogText(ts, raw, LogType.RECEIVE);
    }
}
