package indi.lt.serialtool.controller;

import com.fazecast.jSerialComm.SerialPort;
import indi.lt.serialtool.ui.TaskHandler;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuBar;
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
    private Button btnOpenSerial;

    public MenuBar getMenuBar() {
        return menuBar;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initSerialList();
        initOpenSerialButton();
    }

    private void initOpenSerialButton() {
        btnOpenSerial.setOnAction(event -> {
            openSerial();
        });
    }

    private void openSerial() {
        System.out.println("打开" + cbSerialList.getSelectionModel().getSelectedItem());
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