package indi.lt.serialtool.controller;

import com.fazecast.jSerialComm.SerialPort;
import github.nonoas.jfx.flat.ui.AppState;
import github.nonoas.jfx.flat.ui.concurrent.TaskHandler;
import github.nonoas.jfx.flat.ui.stage.ToastQueue;
import indi.lt.serialtool.component.CommandTableView;
import indi.lt.serialtool.data.CommandRepository;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * 串口发送控制器
 * 固定使用 COM3，115200，8N1
 * @author
 */
public class SerialSendCtrl implements Initializable {

    private final Logger LOG = LogManager.getLogger(SerialSendCtrl.class);

    @FXML public TextField tfRemark;
    @FXML public TextField tfCommand;
    @FXML public TextArea taSendArea;
    @FXML public CheckBox cbIsHex;
    @FXML public Button btnSend; //发送按钮
    @FXML private StackPane spTableContainer;

    private final CommandTableView table = new CommandTableView();

    // 串口对象
    private SerialPort serialPort;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        spTableContainer.getChildren().add(table);

        // 加载历史指令
        new TaskHandler<List<CommandTableView.CommandItem>>()
                .whenCall(CommandRepository.INSTANCE::loadAll)
                .andThen(e -> table.getItems().addAll(e))
                .handle();

        // 打开串口（先固定 COM3 115200 8N1）
        openFixedSerialPort();

        // 绑定发送按钮
        btnSend.setOnAction(e -> sendData());
    }

    /**
     * 打开固定参数的串口
     */
    private void openFixedSerialPort() {
        try {
            serialPort = SerialPort.getCommPort("COM7"); // 固定为 COM3
            serialPort.setComPortParameters(1500000, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

            if (serialPort.openPort()) {
                LOG.info("串口已打开: " + serialPort.getSystemPortName());
                ToastQueue.show(AppState.getStage(), "串口已打开: " + serialPort.getSystemPortName(), 1000);
            } else {
                LOG.error("串口打开失败");
                ToastQueue.show(AppState.getStage(), "串口打开失败", 1000);
            }
        } catch (Exception e) {
            LOG.error("打开串口异常", e);
        }
    }

    /**
     * 点击发送按钮时调用
     */
    private void sendData() {
        if (serialPort == null || !serialPort.isOpen()) {
            ToastQueue.show(AppState.getStage(), "串口未打开", 800);
            return;
        }
        String text = taSendArea.getText();
        if (text == null || text.trim().isEmpty()) {
            ToastQueue.show(AppState.getStage(), "发送内容不能为空", 800);
            return;
        }

        try {
            byte[] data;
            if (cbIsHex.isSelected()) {
                data = hexStringToBytes(text.trim());
            } else {
                data = text.getBytes();
            }
            serialPort.writeBytes(data, data.length);
            LOG.info("发送成功: " + text);
        } catch (Exception e) {
            LOG.error("发送失败", e);
            ToastQueue.show(AppState.getStage(), "发送失败: " + e.getMessage(), 1000);
        }
    }

    /**
     * 添加自定义指令
     */
    @FXML
    public void addCommand() {
        if (tfCommand.getText().trim().isEmpty()) {
            ToastQueue.show(AppState.getStage(), "指令不能为空", 500);
            return;
        }
        String commandType = cbIsHex.isSelected() ? "HEX" : "TXT";
        CommandTableView.CommandItem commandItem = new CommandTableView.CommandItem(
                UUID.randomUUID().toString(),
                tfRemark.getText(),
                tfCommand.getText(),
                commandType
        );
        table.getItems().add(commandItem);
        CommandRepository.INSTANCE.add(commandItem);
    }

    /**
     * 窗口关闭时释放串口
     */
    public void close() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            LOG.info("串口已关闭");
        }
    }

    /**
     * HEX字符串转字节数组
     */
    private byte[] hexStringToBytes(String hex) {
        hex = hex.replaceAll("\\s+", "");
        if (hex.length() % 2 != 0) {
            hex = "0" + hex;
        }
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int index = i * 2;
            int val = Integer.parseInt(hex.substring(index, index + 2), 16);
            result[i] = (byte) val;
        }
        return result;
    }
}
