package indi.lt.serialtool.controller;

import github.nonoas.jfx.flat.ui.theme.Theme;
import indi.lt.serialtool.SerialApplication;
import indi.lt.serialtool.global.ThemeManager;
import indi.lt.serialtool.view.AsciiStage;
import indi.lt.serialtool.view.SerialReceivePane;
import indi.lt.serialtool.view.SerialSendPane;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Menu;
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

    public Menu menuTheme;

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
        ToggleGroup themeGroup = new ToggleGroup();
        for (Theme theme : ThemeManager.getAll()) {
            RadioMenuItem radioMenuItem = new RadioMenuItem(theme.getName());
            radioMenuItem.setUserData(theme);
            radioMenuItem.setToggleGroup(themeGroup);
            menuTheme.getItems().add(radioMenuItem);
        }

        // 监听选项变化
        themeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Theme theme = (Theme) newVal.getUserData();
                SerialApplication.setUserAgentStylesheet(theme.getUserAgentStylesheet());
            }
        });

        SerialReceivePane serialReceivePane1 = new SerialReceivePane("串口1:","serialKey1");
        SerialReceivePane serialReceivePane2 = new SerialReceivePane("串口2:","serialKey2");

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
            Desktop.getDesktop().browse(new URI("https://nonoas.github.io/"));
        } catch (Exception ex) {
            LOG.error(ex);
        }
    }

    @FXML
    public void openAsciiTable(ActionEvent actionEvent) {
        AsciiStage.showStage();
    }
}