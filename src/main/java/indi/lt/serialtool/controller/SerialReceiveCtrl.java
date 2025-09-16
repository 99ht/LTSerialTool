package indi.lt.serialtool.controller;

import com.fazecast.jSerialComm.SerialPort;
import github.nonoas.jfx.flat.ui.concurrent.TaskHandler;
import indi.lt.serialtool.ConfigManager;
import indi.lt.serialtool.component.InlineCssRegexHighlighter;
import indi.lt.serialtool.component.PromptInlineCssTextArea;
import indi.lt.serialtool.component.SerialPortCombBox;
import indi.lt.serialtool.component.SerialToggleButton;
import indi.lt.serialtool.service.SerialReadService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Nonoas
 * @date 2025/8/22
 * @since 1.0.0
 */
public class SerialReceiveCtrl implements Initializable {
    private final Logger LOG = LogManager.getLogger(SerialController.class);

    @FXML
    private BorderPane rootPane;

    @FXML
    private MenuBar menuBar;

    @FXML
    private Label lbSerialName;

    @FXML
    private SerialPortCombBox cbSerialList;

    @FXML
    private ComboBox<Integer> cbBautRateList;

    @FXML
    private PromptInlineCssTextArea textAreaOrigin;

    @FXML
    private SerialToggleButton btnOpenSerial;

    @FXML
    private CheckBox cbTimeDisplay;

    @FXML
    private TextField tfKeyWord;

    private SerialReadService serialReadService;

    private InlineCssRegexHighlighter highlighter;

    private String keyLastSerial;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initBautRateList();
        registerSerialEvent();

        highlighter = new InlineCssRegexHighlighter(textAreaOrigin);
        highlighter.patternTextProperty().bind(tfKeyWord.textProperty());
        // 创建并启动Service
        this.serialReadService = new SerialReadService(cbSerialList.getSelectedPort(), textAreaOrigin,
                cbTimeDisplay,
                () -> highlighter.schedule());
        serialReadService.start();
    }

    private void registerSerialEvent() {
        initOpenSerialButtonAction();
        initBautRateComboBoxAction();
    }

    /**
     * 恢复自动滚动
     */
    @FXML
    private void restoreScrolling() {
        textAreaOrigin.setAutoScroll(true);
    }

    @FXML
    private void clearLogs() {
        textAreaOrigin.getArea().clear();
    }

    @FXML
    private void saveOriginLogs() {
        LOG.info("saveOriginLogs");

        // 1. 获取文本
        String content = textAreaOrigin.getText();
        if (content == null || content.isEmpty()) {
            LOG.info("没有日志内容可保存");
            return;
        }

        // 2. 选择保存文件
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存日志文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("文本文件", "*.txt")
        );
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );

        File file = fileChooser.showSaveDialog(textAreaOrigin.getScene().getWindow());
        if (file == null) {
            return; // 用户取消
        }

        // 3. 写入文件
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(content);
            LOG.info("日志已保存到: " + file.getAbsolutePath());
        } catch (IOException e) {
            LOG.error("保存日志失败", e);
        }
    }


    @FXML
    private void saveFilterLogs() {
        LOG.info("saveFilterLogs");
        //textAreaOrigin.getArea().clear();
    }

    public void initSerialComboBoxAction() {
        // 1. 展开下拉框时刷新列表（保留，但刷新逻辑已优化）
        cbSerialList.init(keyLastSerial, cbBautRateList.valueProperty());
        cbSerialList.getSelectionModel().selectedItemProperty().addListener((observableValue, oldVal, newVal) -> {
            if (null != newVal && null != oldVal) {
                LOG.debug(oldVal + "->" + newVal);
                onSerialPortClicked();
            }
        });

    }

    private void onSerialPortClicked() {
        String selected = cbSerialList.getValue();
        if (selected != null) {
            TaskHandler.backRun(() -> ConfigManager.set(keyLastSerial, selected));
            LOG.info("保存上次串口选择: " + selected);
        }
        if (!btnOpenSerial.isSelected()) {
            return;
        }
        closeSelectSerial();
        cbSerialList.openSelectedSerial();
    }


    private void initOpenSerialButtonAction() {
        btnOpenSerial.selectedProperty().addListener((observableValue, oldVal, newVal) -> {
            if (newVal) {
                cbSerialList.openSelectedSerial();
            } else {
                closeSelectSerial();
            }
        });
    }

    private void initBautRateComboBoxAction() {
        // 监听波特率选中项变化事件
        cbBautRateList.getSelectionModel().selectedItemProperty().addListener((observable) -> {
            onBaudRateChanged();
        });
    }

    private void onBaudRateChanged() {
        closeSelectSerial();
        cbSerialList.openSelectedSerial();
    }

    private void closeSelectSerial() {
        cbSerialList.closeSelectSerial();
        if (null != serialReadService) {
            serialReadService.cancel();
        }
    }

    private void initBautRateList() {
        List<Integer> baudRates = new ArrayList<>(Arrays.asList( // list可以增删改
                1200, 2400, 4800, 9600, 38400, 57600, 115200, 230400, 1500000, 2000000, 3000000));

        // 清空旧的 items 并添加
        cbBautRateList.getItems().clear();
        cbBautRateList.getItems().addAll(baudRates);

        // 默认选中 115200
        cbBautRateList.getSelectionModel().select(Integer.valueOf(1500000));

        LOG.info("波特率初始化完成：" + cbBautRateList.getItems());
    }

    public void setSerialName(String serialName) {
        this.lbSerialName.setText(serialName);
    }
    public void setKeyLastSerial(String keyLastSerial) {
        this.keyLastSerial = keyLastSerial;
    }
}
