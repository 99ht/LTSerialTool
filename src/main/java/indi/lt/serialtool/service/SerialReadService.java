package indi.lt.serialtool.service;

import com.fazecast.jSerialComm.SerialPort;
import indi.lt.serialtool.component.PromptInlineCssTextArea;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.CheckBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fxmisc.richtext.InlineCssTextArea;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * 串口读取服务：
 * - 按 UTF-8 增量解码，避免多字节拆包导致乱码
 * - 按行切分，统一使用 '\n' 结尾；支持 \r\n
 * - 追加到 InlineCssTextArea 后按“最大行数”裁剪（段落级，性能更好）
 * - 可选集成高亮器：每次追加/裁剪后调度一次高亮刷新
 */
public class SerialReadService extends Service<Void> {
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

    private final HighlighterScheduler highlighter; // 可为 null

    /**
     * 最大保留的段落（行）数
     */
    private final int maxLines;

    /**
     * 一次读取的缓冲大小（字节）
     */
    private static final int READ_BUF_SIZE = 2048;

    // —— 构造 —— //

    public SerialReadService(SerialPort comPort,
                             PromptInlineCssTextArea targetTextArea,
                             CheckBox cbTimeDisplay) {
        this(comPort, targetTextArea, cbTimeDisplay, null, 5000);
    }

    public SerialReadService(SerialPort comPort,
                             PromptInlineCssTextArea targetTextArea,
                             CheckBox cbTimeDisplay,
                             HighlighterScheduler highlighter,
                             int maxLines) {
        this.comPort = comPort;
        this.targetTextArea = targetTextArea;
        this.cbTimeDisplay = cbTimeDisplay;
        this.highlighter = highlighter;
        this.maxLines = Math.max(1, maxLines);
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
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
                            Platform.runLater(() -> {
                                targetTextArea.appendText(batch);
                                trimByMaxLines(targetTextArea, maxLines);
                                if (highlighter != null) highlighter.schedule();
                                targetTextArea.moveToEnd();
                            });
                        }
                    }

                    // 收尾：如果还有未结束的一行（无换行结尾）
                    if (!lineBuf.isEmpty()) {
                        String leftover = formatLine(lineBuf.toString(), cbTimeDisplay.isSelected());
                        Platform.runLater(() -> {
                            targetTextArea.appendText(leftover);
                            trimByMaxLines(targetTextArea, maxLines);
                            if (highlighter != null) highlighter.schedule();
                            targetTextArea.moveToEnd();
                        });
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
                uiBatch.append(formatLine(lineBuf.toString(), cbTimeDisplay.isSelected()));
                lineBuf.setLength(0);
            } else {
                lineBuf.append(c);
            }
        }
    }

    /**
     * 根据是否带时间戳，格式化一行，并追加换行符。
     */
    private String formatLine(String raw, boolean withTime) {
        if (withTime) {
            String ts = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            return "[" + ts + "] " + raw + "\n";
        } else {
            return raw + "\n";
        }
    }

    /**
     * 使用 RichTextFX 段落 API 按“最大行数”裁剪头部文本。
     */
    private static void trimByMaxLines(PromptInlineCssTextArea area, int maxLines) {
        int paraCount = area.getParagraphs().size();
        if (paraCount <= maxLines) return;

        int remove = paraCount - maxLines;
        // 计算“第 remove 段开头”的全局偏移
        int cutOffset = area.getAbsolutePosition(remove, 0);
        area.replaceText(0, cutOffset, "");
    }
}
