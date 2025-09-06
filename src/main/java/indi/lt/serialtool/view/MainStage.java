package indi.lt.serialtool.view;

import github.nonoas.jfx.flat.ui.control.UIFactory;
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
        stage = getStage();
        getStage().setWidth(1000);
        getStage().setHeight(600);
        setMinWidth(700);
        setMinHeight(600);

        Button pinButton = UIFactory.createPinButton(stage);
        Tooltip.install(pinButton,new Tooltip("窗口置顶"));
        getSystemButtons().add(0, pinButton);
    }
}
