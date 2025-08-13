package indi.lt.serialtool.serialtool.controller;

import com.fazecast.jSerialComm.SerialPort;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class HelloController implements Initializable {

    @FXML
    private BorderPane rootPane;

    @FXML
    private MenuBar menuBar;

    @FXML
    private ComboBox<String> cbSerialList;

    @FXML
    private Button btnOpenSerial;

    @FXML
    protected void onHelloButtonClick() {
    }

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
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                List<String> serialList = new ArrayList<>();
                for (SerialPort serialPort : getSerialPorts()) {
                    serialList.add(serialPort.getDescriptivePortName());
                }
                return serialList;
            }
        };

        task.valueProperty().addListener(
                (observableValue, strings, newVal) -> {
                    System.out.println("读取完成" + newVal);
                    cbSerialList.getItems().addAll(newVal);
                    cbSerialList.getSelectionModel().selectFirst();
                }
        );
        new Thread(task).start();
    }

    private static List<SerialPort> getSerialPorts() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return Arrays.asList(SerialPort.getCommPorts());
    }
}