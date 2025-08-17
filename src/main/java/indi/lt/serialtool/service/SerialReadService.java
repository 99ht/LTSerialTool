package indi.lt.serialtool.service;

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SerialReadService extends Service<Void> {
    private final Logger LOG = LogManager.getLogger(SerialReadService.class);

    private final SerialPort comPort;
    private final TextArea targetTextArea;

    private final CheckBox cbTimeDisplay;
    private static final int MAXLINES = 5000;

    public SerialReadService(SerialPort comPort, TextArea targetTextArea, CheckBox cbTimeDisplay) {
        this.comPort = comPort;
        this.targetTextArea = targetTextArea;
        this.cbTimeDisplay = cbTimeDisplay;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                try (InputStream in = comPort.getInputStream()) {
                    byte[] buffer = new byte[1024];
                    int numRead;
                    StringBuilder sb = new StringBuilder();

                    while (!isCancelled() && (numRead = in.read(buffer)) > 0) {
                        String chunk = new String(buffer, 0, numRead, StandardCharsets.UTF_8);
                        sb.append(chunk);

                        int newlineIndex;
                        while ((newlineIndex = sb.indexOf("\n")) >= 0) {
                            // 取出一行
                            String line = sb.substring(0, newlineIndex + 1);
                            sb.delete(0, newlineIndex + 1);

                            Platform.runLater(() -> {
                                String finalLine = "";
                                if (cbTimeDisplay.isSelected()) {
                                    String timestamp = java.time.LocalDateTime.now()
                                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
                                    finalLine = "[" + timestamp + "] " + line;
                                } else {
                                    finalLine = line;
                                }
                                targetTextArea.appendText(finalLine);

                                // 限制最大行数
                                ObservableList<CharSequence> paragraphs = targetTextArea.getParagraphs();
                                if (paragraphs.size() > MAXLINES) {
                                    int linesToRemove = paragraphs.size() - MAXLINES;
                                    String fullText = targetTextArea.getText();
                                    int cutIndex = 0;
                                    int linesRemoved = 0;

                                    for (int i = 0; i < fullText.length(); i++) {
                                        if (fullText.charAt(i) == '\n') {
                                            linesRemoved++;
                                            if (linesRemoved >= linesToRemove) {
                                                cutIndex = i + 1;
                                                break;
                                            }
                                        }
                                    }
                                    targetTextArea.replaceText(0, cutIndex, "");
                                }
                            });
                        }

                    }
                } catch (IOException e) {
                    LOG.error(e);
                } finally {
                    if (comPort.isOpen()) {
                        comPort.closePort();
                    }
                }
                return null;
            }
        };
    }
}
