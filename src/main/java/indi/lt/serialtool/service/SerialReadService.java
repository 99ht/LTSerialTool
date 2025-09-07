package indi.lt.serialtool.service;

import com.fazecast.jSerialComm.SerialPort;
import indi.lt.serialtool.component.PromptInlineCssTextArea;
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

/**
 * 串口读取服务：
 * - 按 UTF-8 增量解码，避免多字节拆包导致乱码
 * - 按行切分，统一使用 '\n' 结尾；支持 \r\n
 * - 追加到 InlineCssTextArea 后按“最大行数”裁剪（段落级，性能更好）
 * - 可选集成高亮器：每次追加/裁剪后调度一次高亮刷新
 */
public class SerialReadService extends Service<SerialReadService.LogText> {
    private static final Logger LOG = LogManager.getLogger(SerialReadService.class);

    private final SerialPort comPort;
    private final PromptInlineCssTextArea targetTextArea;
    private final CheckBox cbTimeDisplay;

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

    // —— 构造 —— //

    public SerialReadService(SerialPort comPort,
                             PromptInlineCssTextArea targetTextArea,
                             CheckBox cbTimeDisplay,
                             HighlighterScheduler highlighter) {
        this.comPort = comPort;
        this.targetTextArea = targetTextArea;
        this.cbTimeDisplay = cbTimeDisplay;
        this.highlighterScheduler = highlighter;
        valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                LOG.debug("追加文本：" + newValue);
                String text;
                if (cbTimeDisplay.isSelected()) {
                    text = newValue.toString();
                } else {
                    text = newValue.text;
                }
                appendText(text + "\n");
            }
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
                        if (n == 0) continue;   // 无数据                        // 解码
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
                    try {
                        if (comPort.isOpen()) comPort.closePort();
                    } catch (Exception ex) {
                        LOG.warn("closePort failed", ex);
                    }
                }
                return null;
            }
        };
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
                uiBatch.append(formatLine(lineBuf.toString()).text);
                lineBuf.setLength(0);
            } else {
                lineBuf.append(c);
            }
        }
    }

    /**
     * 根据是否带时间戳，格式化一行，并追加换行符。
     */
    private LogText formatLine(String raw) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        return new LogText(ts, raw);
    }

    public static class LogText {
        private final String timeStamp;
        private final String text;

        public LogText(String timeStamp, String text) {
            this.timeStamp = timeStamp;
            this.text = text;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof LogText)) {
                return false;
            }
            return String.valueOf(this).equals(String.valueOf(obj));
        }

        @Override
        public String toString() {
            return "[" + timeStamp + "] " + text;
        }
    }
}
