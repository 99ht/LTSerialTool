package indi.lt.serialtool.controller;

import com.fazecast.jSerialComm.SerialPort;
import indi.lt.serialtool.service.SerialReadService;
import indi.lt.serialtool.ui.TaskHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class HelloController implements Initializable {

    private final Logger LOG = LogManager.getLogger(HelloController.class);

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
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            System.out.println("未检测到串口设备");
        }

        SerialPort comPort = ports[0];
        comPort.setComPortParameters(1500000, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);

        if (!comPort.openPort()) {
            System.out.println("串口打开失败");
        }

        // 创建并启动Service
        this.serialReadService = new SerialReadService(comPort, textAreaOrigin, 100);
        serialReadService.start();

        // 后面如果要停止：
        // service.cancel();
    }


    private void initOpenSerialButton() {
        btnOpenSerial.setOnAction(event -> {
            openSerial();
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

    private void openSerial() {
        LOG.debug("打开" + cbSerialList.getSelectionModel().getSelectedItem());
    }

    private void initSerialList() {
        new TaskHandler<List<String>>()
                .whenCall(() -> {
                    List<String> serialList = new ArrayList<>();
                    for (SerialPort serialPort : getSerialPorts()) {
                        serialList.add(serialPort.getDescriptivePortName());
                    }
                    return serialList;
                }).andThen(val -> {
                    LOG.info("读取完成" + val);
                    cbSerialList.getItems().addAll(val);
                    cbSerialList.getSelectionModel().selectFirst();
                }).handle();
    }

    private static List<SerialPort> getSerialPorts() {
        return Arrays.asList(SerialPort.getCommPorts());
    }
}