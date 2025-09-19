package indi.lt.serialtool.view;

import github.nonoas.jfx.flat.ui.control.UIFactory;
import indi.lt.serialtool.global.ConfigManager;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

/**
 * @author Nonoas
 * @version 1.0.0
 * @date 2025/8/24
 * @since 1.0.0
 */
public class MainStage extends BaseStage {

    private final Stage stage;

    public MainStage() {
        String x = ConfigManager.get("window.x");
        String y = ConfigManager.get("window.y");
        double width = Double.parseDouble(ConfigManager.get("window.width", "1000"));
        double height = Double.parseDouble(ConfigManager.get("window.height", "600"));
        boolean isMaximized = Boolean.parseBoolean(ConfigManager.get("window.isMaximized", "false"));

        stage = getStage();
        stage.setWidth(width);
        stage.setHeight(height);
        if (isMaximized) {
            setMaximized(true);
        } else {
            setMinWidth(700);
            setMinHeight(600);
        }


        if (x != null && y != null) {
            stage.setX(Double.parseDouble(x));
            stage.setY(Double.parseDouble(y));
        }

        Button pinButton = UIFactory.createPinButton(stage);
        Tooltip.install(pinButton, new Tooltip("窗口置顶"));
        getSystemButtons().add(0, pinButton);
    }
}
