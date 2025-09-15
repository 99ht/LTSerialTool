package indi.lt.serialtool.controller;

import com.fazecast.jSerialComm.SerialPort;
import github.nonoas.jfx.flat.ui.AppState;
import github.nonoas.jfx.flat.ui.concurrent.TaskHandler;
import github.nonoas.jfx.flat.ui.stage.ToastQueue;
import indi.lt.serialtool.ConfigManager;
import indi.lt.serialtool.component.CommandTableView;
import indi.lt.serialtool.component.SerialPortCombBox;
import indi.lt.serialtool.component.SerialToggleButton;
import indi.lt.serialtool.data.CommandRepository;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * 串口发送控制器
 */
public class SerialSendCtrl implements Initializable {

    private final Logger LOG = LogManager.getLogger(SerialSendCtrl.class);

    @FXML
    private TextField tfRemark;
    @FXML
    private TextField tfCommand;
    @FXML
    private TextArea taSendArea;
    @FXML
    private CheckBox cbIsHex;

    @FXML
    private SerialPortCombBox cbSerialList;
    @FXML
    private ComboBox<Integer> cbBautrate;
    @FXML
    private Button btnSend;
    @FXML
    private SerialToggleButton btnOpenSerial;
    @FXML
    private StackPane spTableContainer;

    private final CommandTableView table = new CommandTableView();

    // 当前打开的串口
    private SerialPort comPort;

    // 保存上次串口选择的 key
    private final String keyLastSerial = "sendModeLastSerialPort";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        spTableContainer.getChildren().add(table);
        initBaudRateList();
        initSerialComboBox();
        loadHistoryCommands();
        setupButtonActions();

        // 启动时默认打开第一个串口
        openSelectedSerial();
    }

    /**
     * 初始化波特率下拉列表
     */
    private void initBaudRateList() {
        List<Integer> baudRates = Arrays.asList(1200, 2400, 4800, 9600, 38400, 57600, 115200, 230400, 1500000, 2000000, 3000000);
        cbBautrate.getItems().clear();
        cbBautrate.getItems().addAll(baudRates);
        cbBautrate.getSelectionModel().select(Integer.valueOf(115200));
    }

    /**
     * 初始化串口下拉列表
     */
    private void initSerialComboBox() {
        // 串口下拉框初始化
        cbSerialList.init(keyLastSerial);
        // 选中串口时自动打开
        cbSerialList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                openSelectedSerial();
            }
        });

        // 选中波特率变化时重新打开串口
        cbBautrate.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (comPort != null && comPort.isOpen()) {
                openSelectedSerial();
            }
        });
    }

    /**
     * 加载历史命令
     */
    private void loadHistoryCommands() {
        new TaskHandler<List<CommandTableView.CommandItem>>()
                .whenCall(CommandRepository.INSTANCE::loadAll)
                .andThen(table.getItems()::addAll)
                .handle();
    }

    /**
     * 绑定按钮事件
     */
    private void setupButtonActions() {
        btnSend.setOnAction(e -> sendData());

        btnOpenSerial.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                openSelectedSerial();
            } else {
                if (comPort != null && comPort.isOpen()) {
                    // 串口已打开 → 关闭
                    closeSerial();
                }
            }
        });
    }

    /**
     * 打开用户选择的串口
     */
    private void openSelectedSerial() {
        if (cbSerialList.getItems().isEmpty()) {
            LOG.warn("串口列表为空，无法打开串口");
            ToastQueue.show(AppState.getStage(), "未检测到串口设备", 800);
            return;
        }

        String selectedSerial = cbSerialList.getValue();
        if (selectedSerial == null || selectedSerial.isEmpty()) {
            selectedSerial = cbSerialList.getItems().get(0);
            cbSerialList.setValue(selectedSerial);
        }

        SerialPort[] ports = SerialPort.getCommPorts();
        int index = cbSerialList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= ports.length) {
            LOG.warn("串口索引超出范围");
            return;
        }

        // 关闭已有串口
        if (comPort != null && comPort.isOpen()) {
            comPort.closePort();
            LOG.info("关闭旧串口");
        }

        comPort = ports[index];

        int baudRate = cbBautrate.getValue() != null ? cbBautrate.getValue() : 115200;
        comPort.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

        if (comPort.openPort()) {
            LOG.info("串口已打开: " + comPort.getSystemPortName() + " @ " + baudRate);
            ToastQueue.show(AppState.getStage(), "串口已打开: " + comPort.getSystemPortName(), 800);
            ConfigManager.set(keyLastSerial, selectedSerial);
        } else {
            LOG.error("串口打开失败: " + comPort.getSystemPortName());
            ToastQueue.show(AppState.getStage(), "串口打开失败", 800);
        }
    }

    /**
     * 关闭串口
     */
    public void closeSerial() {
        if (comPort != null && comPort.isOpen()) {
            comPort.closePort();
            LOG.info("串口已关闭");
            ToastQueue.show(AppState.getStage(), "串口已关闭", 800);
        }
    }

    /**
     * 发送数据
     */
    private void sendData() {
        if (comPort == null || !comPort.isOpen()) {
            ToastQueue.show(AppState.getStage(), "串口未打开", 800);
            return;
        }

        String text = taSendArea.getText();
        if (text == null || text.trim().isEmpty()) {
            ToastQueue.show(AppState.getStage(), "发送内容不能为空", 800);
            return;
        }

        try {
            byte[] data = cbIsHex.isSelected() ? hexStringToBytes(text.trim()) : text.getBytes();
            comPort.writeBytes(data, data.length);
            LOG.info("发送成功: " + text);
        } catch (Exception e) {
            LOG.error("发送失败", e);
            ToastQueue.show(AppState.getStage(), "发送失败: " + e.getMessage(), 1000);
        }
    }

    /**
     * 添加自定义命令
     */
    @FXML
    private void addCommand() {
        if (tfCommand.getText().trim().isEmpty()) {
            ToastQueue.show(AppState.getStage(), "指令不能为空", 500);
            return;
        }

        String type = cbIsHex.isSelected() ? "HEX" : "TXT";
        CommandTableView.CommandItem item = new CommandTableView.CommandItem(
                UUID.randomUUID().toString(),
                tfRemark.getText(),
                tfCommand.getText(),
                type
        );
        table.getItems().add(item);
        CommandRepository.INSTANCE.add(item);
    }

    /**
     * HEX 转 byte
     */
    private byte[] hexStringToBytes(String hex) {
        hex = hex.replaceAll("\\s+", "");
        if (hex.length() % 2 != 0) hex = "0" + hex;
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }

    static class SerialPortData {
        private SerialPort[] serialPorts;
        private String lastSerial;

        public SerialPortData(SerialPort[] serialPorts, String lastSerial) {
            this.serialPorts = serialPorts;
            this.lastSerial = lastSerial;
        }

    }

    private static List<SerialPort> getSerialPorts() {
        return Arrays.asList(SerialPort.getCommPorts());
    }

}
