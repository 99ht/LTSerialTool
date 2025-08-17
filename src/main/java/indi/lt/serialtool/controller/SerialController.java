package indi.lt.serialtool.controller;

import com.fazecast.jSerialComm.SerialPort;
import indi.lt.serialtool.service.SerialReadService;
import indi.lt.serialtool.ui.TaskHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SerialController implements Initializable {

    private final Logger LOG = LogManager.getLogger(SerialController.class);

    @FXML
    private BorderPane rootPane;

    @FXML
    private MenuBar menuBar;

    @FXML
    private ComboBox<String> cbSerialList;

    @FXML
    private TextArea textAreaOrigin;

    @FXML
    private Button btnOpenSerial;

    @FXML
    private CheckBox cbTimeDisplay;

    private SerialReadService serialReadService;

    private SerialPort[] ports = new SerialPort[0];

    public MenuBar getMenuBar() {
        return menuBar;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initSerialList();
        initOpenSerialButton();
    }

    private void openSelectSerial() {
        int selectedIndex = cbSerialList.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            LOG.info("请先选择一个串口设备");
            return;
        }

        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            LOG.info("未检测到串口设备");
            return;
        }

        SerialPort comPort = ports[selectedIndex];
        comPort.setComPortParameters(1500000, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);

        if (!comPort.openPort()) {
            LOG.error("串口打开失败");
            return;
        }

        // 创建并启动Service
        this.serialReadService = new SerialReadService(comPort, textAreaOrigin, cbTimeDisplay);
        serialReadService.start();
        // 后面如果要停止：
        // service.cancel();
    }


    private void initOpenSerialButton() {
        btnOpenSerial.setOnAction(event -> {
            if ("打开".equals(btnOpenSerial.getText())) {
                btnOpenSerial.setText("关闭");
                openSelectSerial();
            } else {
                closeSelectSerial();
                btnOpenSerial.setText("打开");
            }
        });
    }

    private void closeSelectSerial() {
        if (serialReadService != null) {
            serialReadService.cancel();
        }
    }

    private void initSerialList() {
        new TaskHandler<List<String>>()
                .whenCall(new Supplier<List<String>>() {
                    @Override
                    public List<String> get() {
                        List<String> serialList = new ArrayList<>();
                        for (SerialPort serialPort : getSerialPorts()) {
                            serialList.add(serialPort.getDescriptivePortName());
                        }
                        return serialList;
                    }
                }).andThen(new Consumer<List<String>>() {
                    @Override
                    public void accept(List<String> val) {
                        LOG.info("读取完成" + val);
                        cbSerialList.getItems().addAll(val);
                        cbSerialList.getSelectionModel().selectFirst();
                    }
                }).handle();
    }

    private static List<SerialPort> getSerialPorts() {
        return Arrays.asList(SerialPort.getCommPorts());
    }
}