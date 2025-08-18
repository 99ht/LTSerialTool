package indi.lt.serialtool.component;

import javafx.scene.control.ToggleButton;

/**
 * 串口开关按钮
 *
 * @author Nonoas
 * @date 2025/8/19
 * @since 1.0.0
 */
public class SerialToggleButton extends ToggleButton {
    public SerialToggleButton() {
        getStyleClass().add("serial-toggle-button");
        selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                setText("关闭");
            } else {
                setText("开启");
            }
        });
    }
}
