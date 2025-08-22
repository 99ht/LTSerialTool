package indi.lt.serialtool;

import github.nonoas.jfx.flat.ui.theme.LightTheme;
import indi.lt.serialtool.controller.SerialController;
import indi.lt.serialtool.view.BaseStage;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class SerialApplication extends Application {

    private final Logger LOG = LogManager.getLogger(SerialController.class);

    @Override
    public void start(Stage stage) throws IOException {
        // 全局UI线程异常捕获
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> LOG.error("未知异常", e));

        // 注意资源路径，通常加前导斜杠更稳
        FXMLLoader fxmlLoader = new FXMLLoader(SerialApplication.class.getResource("/fxml/hello-view.fxml"));
        // 先加载 -> 创建场景图和 Controller 并完成 @FXML 注入
        Parent root = fxmlLoader.load();
        // 再拿 Controller
        SerialController controller = fxmlLoader.getController();

        setUserAgentStylesheet(new LightTheme().getUserAgentStylesheet());

        BaseStage appStage = new BaseStage();
        appStage.getStage().setWidth(700);
        appStage.getStage().setHeight(600);
        appStage.setMinWidth(700);
        appStage.setMinHeight(600);
        appStage.setTitle("LTSerialTool-v2.16.0");
        appStage.setContentView(root);
        // 现在 controller 已经不是 null 了，且其 @FXML 成员已注入
        appStage.registryDragger(controller.getMenuBar());

        appStage.show();
    }
}