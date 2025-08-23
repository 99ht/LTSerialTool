package indi.lt.serialtool.controller;

import com.fazecast.jSerialComm.SerialPort;
import indi.lt.serialtool.component.InlineCssRegexHighlighter;
import indi.lt.serialtool.component.PromptInlineCssTextArea;
import indi.lt.serialtool.component.SerialToggleButton;
import indi.lt.serialtool.service.SerialReadService;
import indi.lt.serialtool.ui.TaskHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fxmisc.richtext.InlineCssTextArea;

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
    private ComboBox<String> cbSerialList;

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

    private SerialPort comPort;

    private InlineCssRegexHighlighter highlighter;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initBautRateList();
        registerSerialEvent();
    }

    private void registerSerialEvent() {
        initSerialComboBoxAction();
        initOpenSerialButtonAction();
        initBautRateComboBoxAction();
    }

    private void openSelectSerial(ComboBox<String> f_cbSerialList, ComboBox<Integer> f_cbBautRateList) {
        int selectedIndex = f_cbSerialList.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            LOG.info("请先选择一个串口设备");
            return;
        }

        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            LOG.info("未检测到串口设备");
            return;
        }
        //        Integer bautrate = cbBautRateList.getSelectionModel().getSelectedItem();
        int bautrate = Integer.parseInt(String.valueOf(f_cbBautRateList.getSelectionModel().getSelectedItem()));
        LOG.info("此时的波特率:" + bautrate);

        comPort = ports[selectedIndex];
        try {
            comPort.setComPortParameters(bautrate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
        } catch (Exception e) {
            LOG.error("设置波特率时发生异常", e);
            return;
        }

        if (!comPort.openPort()) {
            LOG.error("串口打开失败");
            return;
        }

        highlighter = new InlineCssRegexHighlighter(textAreaOrigin);
        highlighter.patternTextProperty().bind(tfKeyWord.textProperty());
        // 创建并启动Service
        this.serialReadService = new SerialReadService(comPort, textAreaOrigin,
                cbTimeDisplay,
                () -> highlighter.schedule(), 500);
        serialReadService.start();
    }

    private void initSerialComboBoxAction() {
        // 1. 展开下拉框时刷新列表（保留，但刷新逻辑已优化）
        cbSerialList.setOnShowing(event -> refreshSerialList());

        cbSerialList.getSelectionModel().selectedItemProperty().addListener((observableValue, oldVal, newVal) -> {
            if (null != newVal && null != oldVal) {
                LOG.debug(oldVal + "->" + newVal);
                onSerialPortClicked();
            }
        });

        List<String> serialList = new ArrayList<>();
        for (SerialPort serialPort : getSerialPorts()) {
            serialList.add(serialPort.getSystemPortName() + " - " + serialPort.getDescriptivePortName());
        }
        cbSerialList.getItems().addAll(serialList);
        cbSerialList.getSelectionModel().selectFirst();
    }

    private void onSerialPortClicked() {
        // 手动选中后的逻辑（如切换串口）
        closeSelectSerial();
        openSelectSerial(cbSerialList, cbBautRateList);

    }

    private void initOpenSerialButtonAction() {
        btnOpenSerial.selectedProperty().addListener((observableValue, oldVal, newVal) -> {
            if (newVal) {
                openSelectSerial(cbSerialList, cbBautRateList);
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
        if (comPort != null && comPort.isOpen()) {
            closeSelectSerial();
            openSelectSerial(cbSerialList, cbBautRateList);
        }
    }

    private void closeSelectSerial() {
        if (null != comPort) {
            comPort.closePort();
        }
        serialReadService.cancel();
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

    private void refreshSerialList() {
        // 刷新前记录当前选中的串口（用于后续恢复）
        String currentSelected = cbSerialList.getValue();

        new TaskHandler<List<String>>().whenCall(() -> {
            List<String> serialList = new ArrayList<>();
            for (SerialPort serialPort : getSerialPorts()) {
                serialList.add(serialPort.getSystemPortName() + " - " + serialPort.getDescriptivePortName());
            }
            return serialList;
        }).andThen(val -> {
            LOG.info("读取完成" + val);
            cbSerialList.getItems().clear();
            cbSerialList.getItems().addAll(val);

            // 刷新后：如果之前有选中项且仍存在，则恢复选中；否则不自动选中
            if (currentSelected != null && val.contains(currentSelected)) {
                cbSerialList.setValue(currentSelected); // 恢复之前的选中项
            } else {
                // 首次加载或选中项已消失，可选：不自动选中任何项
                cbSerialList.getSelectionModel().clearSelection();
            }
        }).handle();
    }

    private static List<SerialPort> getSerialPorts() {
        return Arrays.asList(SerialPort.getCommPorts());
    }

    public void setSerialName(String serialName) {
        this.lbSerialName.setText(serialName);
    }
}
