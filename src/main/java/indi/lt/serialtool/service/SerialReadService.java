package indi.lt.serialtool.service;

import com.fazecast.jSerialComm.SerialPort;
import indi.lt.serialtool.controller.HelloController;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
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
    private final int maxLines;

    public SerialReadService(SerialPort comPort, TextArea targetTextArea, int maxLines) {
        this.comPort = comPort;
        this.targetTextArea = targetTextArea;
        this.maxLines = maxLines;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                try (InputStream in = comPort.getInputStream()) {
                    byte[] buffer = new byte[1024];
                    int numRead;

                    while (!isCancelled() && (numRead = in.read(buffer)) > 0) {
                        String data = new String(buffer, 0, numRead, StandardCharsets.UTF_8);

                        // UI 更新放到主线程
                        Platform.runLater(() -> {
                            targetTextArea.appendText(data);

                            // 限制最大行数为10
                            ObservableList<CharSequence> paragraphs = targetTextArea.getParagraphs();
                            if (paragraphs.size() > maxLines) {
                                int linesToRemove = paragraphs.size() - maxLines;
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
