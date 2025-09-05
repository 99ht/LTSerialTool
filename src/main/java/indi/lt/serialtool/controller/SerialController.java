package indi.lt.serialtool.controller;

import atlantafx.base.util.BBCodeParser;
import indi.lt.serialtool.view.SerialReceivePane;
import indi.lt.serialtool.view.SerialSendPane;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;

public class SerialController implements Initializable {

    private final Logger LOG = LogManager.getLogger(SerialController.class);

    @FXML
    private BorderPane rootPane;

    private SerialSendPane serialSendPane;

    private final SplitPane spReceive = new SplitPane();

    @FXML
    private StackPane stpRootPane;

    @FXML
    private MenuBar menuBar;

    public MenuBar getMenuBar() {
        return menuBar;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        SerialReceivePane serialReceivePane1 = new SerialReceivePane("串口1:");
        SerialReceivePane serialReceivePane2 = new SerialReceivePane("串口2:");
        spReceive.getItems().addAll(serialReceivePane1, serialReceivePane2);
        stpRootPane.getChildren().add(spReceive);
    }

    @FXML
    private void changeToReceiveMode() {
        spReceive.setVisible(true);
        if (serialSendPane != null) {
            serialSendPane.setVisible(false);
        }
    }

    @FXML
    private void changeToSendMode() {
        if (serialSendPane == null) {
            serialSendPane = new SerialSendPane("串口:");
            stpRootPane.getChildren().add(serialSendPane);
        }
        serialSendPane.setVisible(true);
        spReceive.setVisible(false);
    }
}