package indi.lt.serialtool.controller;

import github.nonoas.jfx.flat.ui.theme.Theme;
import indi.lt.serialtool.SerialApplication;
import indi.lt.serialtool.global.ConfigManager;
import indi.lt.serialtool.global.ThemeManager;
import indi.lt.serialtool.view.AsciiStage;
import indi.lt.serialtool.view.SerialReceivePane;
import indi.lt.serialtool.view.SerialSendPane;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static indi.lt.serialtool.global.ConfigManager.KEY_RECEIVE_SPLIT_PANE_DIVIDER_POSITIONS;
import static org.kordamp.ikonli.material2.Material2OutlinedAL.ASSIGNMENT;
import static org.kordamp.ikonli.material2.Material2OutlinedAL.INFO;
import static org.kordamp.ikonli.material2.Material2OutlinedAL.INVERT_COLORS;
import static org.kordamp.ikonli.material2.Material2OutlinedMZ.SETTINGS;
import static org.kordamp.ikonli.material2.Material2OutlinedMZ.TUNE;

public class MainController implements Initializable {

    private final Logger LOG = LogManager.getLogger(MainController.class);

    public MenuButton mbTheme;

    @FXML
    public ToolBar toolBar;

    public MenuButton mbFile;

    @FXML
    public MenuButton mbSetting;
    public MenuButton mbTools;
    public MenuButton mbHelp;

    @FXML
    private BorderPane rootPane;

    private final SerialSendPane serialSendPane = new SerialSendPane();

    private final SplitPane spReceive = new SplitPane();

    private final ToolBar topToolBar = new ToolBar();


    @FXML
    private TabPane tabRootPane;

    private ToggleGroup themeGroup;


    public TabPane getMenuBar() {
        return tabRootPane;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initMenuButtons();

        SerialReceivePane serialReceivePane1 = new SerialReceivePane("串口1:", "serialKey1");
        SerialReceivePane serialReceivePane2 = new SerialReceivePane("串口2:", "serialKey2");

        String dividePostions = ConfigManager.get(KEY_RECEIVE_SPLIT_PANE_DIVIDER_POSITIONS);
        spReceive.getItems().addAll(serialReceivePane1, serialReceivePane2);
        double[] dividePositionList = Arrays.stream(dividePostions.split(",")).mapToDouble(Double::parseDouble).toArray();
        Platform.runLater(() -> spReceive.setDividerPositions(dividePositionList));

        Tab tabRec = new Tab("接收模式", spReceive);
        tabRec.setClosable(false);

        Tab tabSend = new Tab("发送模式", serialSendPane);
        tabSend.setClosable(false);
        tabRootPane.getTabs().addAll(tabSend,tabRec);
        tabRootPane.getSelectionModel().select(tabRec);
    }

    private void initMenuButtons() {

        for (Node item : toolBar.getItems()) {
            item.getStyleClass().add("flat-menu-button");
            item.prefWidth(14);
            item.prefHeight(14);
        }
        mbFile.setGraphic(new FontIcon(ASSIGNMENT));
        mbSetting.setGraphic(new FontIcon(SETTINGS));
        mbTheme.setGraphic(new FontIcon(INVERT_COLORS));
        mbTools.setGraphic(new FontIcon(TUNE));
        mbHelp.setGraphic(new FontIcon(INFO));

        Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/logo.png")));
        ImageView logoView = new ImageView(logo);
        logoView.setFitWidth(40);
        logoView.setFitHeight(40);
        Region region = new Region();
        region.setMinHeight(10);
        toolBar.getItems().add(0, logoView);
        toolBar.getItems().add(1, region);

        // 创建 ToggleGroup
        ToggleGroup themeGroup = new ToggleGroup();
        for (Theme theme : ThemeManager.getAll()) {
            RadioMenuItem radioMenuItem = new RadioMenuItem(theme.getName());
            radioMenuItem.setUserData(theme);
            radioMenuItem.setToggleGroup(themeGroup);
            mbTheme.getItems().add(radioMenuItem);
        }

        // 监听选项变化
        themeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Theme theme = (Theme) newVal.getUserData();
                SerialApplication.setUserAgentStylesheet(theme.getUserAgentStylesheet());
            }
        });
    }

    public String getDividePosition() {
        return Arrays.stream(spReceive.getDividerPositions())
                .mapToObj(e -> new BigDecimal(e).toPlainString())
                .collect(Collectors.joining(","));
    }

    @FXML
    private void changeToReceiveMode() {
        spReceive.setVisible(true);
        serialSendPane.setVisible(false);
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