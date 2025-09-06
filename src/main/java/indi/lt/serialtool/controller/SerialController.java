package indi.lt.serialtool.controller;

import github.nonoas.jfx.flat.ui.theme.DarkTheme;
import github.nonoas.jfx.flat.ui.theme.LightTheme;
import indi.lt.serialtool.SerialApplication;
import indi.lt.serialtool.view.SerialReceivePane;
import indi.lt.serialtool.view.SerialSendPane;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.MenuBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

public class SerialController implements Initializable {

    private final Logger LOG = LogManager.getLogger(SerialController.class);

    public RadioMenuItem lightTheme;

    public RadioMenuItem darkTheme;

    @FXML
    private BorderPane rootPane;

    private SerialSendPane serialSendPane;

    private final SplitPane spReceive = new SplitPane();

    @FXML
    private StackPane stpRootPane;

    @FXML
    private MenuBar menuBar;

    private ToggleGroup themeGroup;


    public MenuBar getMenuBar() {
        return menuBar;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 创建 ToggleGroup
        themeGroup = new ToggleGroup();
        lightTheme.setToggleGroup(themeGroup);
        darkTheme.setToggleGroup(themeGroup);
        // 监听选项变化
        themeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                RadioMenuItem selected = (RadioMenuItem) newVal;
                if (selected == lightTheme) {
                    SerialApplication.setUserAgentStylesheet(new LightTheme().getUserAgentStylesheet());
                } else {
                    SerialApplication.setUserAgentStylesheet(new DarkTheme().getUserAgentStylesheet());
                }
            }
        });

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

    @FXML
    public void goToWebsite(ActionEvent actionEvent) {
        try {
            Desktop.getDesktop().browse(new URI("https://github.com/99ht/LTSerialTool"));
        } catch (Exception ex) {
            LOG.error(ex);
        }
    }
}