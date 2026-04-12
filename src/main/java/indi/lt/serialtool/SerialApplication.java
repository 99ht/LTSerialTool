package indi.lt.serialtool;

import github.nonoas.jfx.flat.ui.AppState;
import github.nonoas.jfx.flat.ui.AutoReleaseApplication;
import github.nonoas.jfx.flat.ui.theme.LightTheme;
import indi.lt.serialtool.controller.MainController;
import indi.lt.serialtool.global.ConfigManager;
import indi.lt.serialtool.view.MainStage;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static indi.lt.serialtool.global.ConfigManager.KEY_RECEIVE_SPLIT_PANE_DIVIDER_POSITIONS;

public class SerialApplication extends AutoReleaseApplication {

    private final Logger LOG = LogManager.getLogger(MainController.class);

    private MainController controller;

    @Override
    public void start(Stage stage) throws IOException {
        // 全局UI线程异常捕获
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> LOG.error("未知异常", e));

        // 注意资源路径，通常加前导斜杠更稳
        FXMLLoader fxmlLoader = new FXMLLoader(SerialApplication.class.getResource("/fxml/main-view.fxml"));
        // 先加载 -> 创建场景图和 Controller 并完成 @FXML 注入
        Parent root = fxmlLoader.load();
        // 再拿 Controller
        controller = fxmlLoader.getController();

        setUserAgentStylesheet(new LightTheme().getUserAgentStylesheet());

        MainStage appStage = new MainStage();
        appStage.setTitle("LTSerialTool-v2.16.0");

        StackPane rootPane = new StackPane(root);
        HeaderBar headerBar = appStage.getHeaderBar();
        headerBar.setViewOrder(-1);
        headerBar.setMaxWidth(Region.USE_PREF_SIZE);
        headerBar.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(headerBar, Pos.TOP_RIGHT);
        rootPane.getChildren().add(headerBar);
        appStage.setContentView(rootPane);
        // 现在 controller 已经不是 null 了，且其 @FXML 成员已注入
        appStage.registryDragger(controller.getMenuBar());

        AppState.setStage(appStage.getStage());
        appStage.show();
    }

    @Override
    public void stop() throws Exception {
        saveLayout();
        super.stop();
    }

    private void saveLayout() {
        Stage stage = AppState.getStage();
        ConfigManager.set("window.x", String.valueOf(stage.getX()));
        ConfigManager.set("window.y", String.valueOf(stage.getY()));
        ConfigManager.set("window.width", String.valueOf(stage.getWidth()));
        ConfigManager.set("window.height", String.valueOf(stage.getHeight()));
        ConfigManager.set("window.isMaximized", String.valueOf(stage.isMaximized()));
        ConfigManager.set(KEY_RECEIVE_SPLIT_PANE_DIVIDER_POSITIONS, String.join(",", controller.getDividePosition()));
    }
}