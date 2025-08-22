package indi.lt.serialtool.view;

import indi.lt.serialtool.controller.SerialReceiveCtrl;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * @author Nonoas
 * @date 2025/8/22
 * @since 1.0.0
 */
public class SerialReceivePane extends VBox {

    private SerialReceiveCtrl controller;

    public SerialReceivePane(String serialName){
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/fxml/serial-receive-pane.fxml")
        );
        fxmlLoader.setRoot(this);      // 将 this 作为 fxml 根节点

        try {
            fxmlLoader.load(); // 加载 fxml
            controller = fxmlLoader.getController(); // 获取逻辑控制器
            controller.setSerialName(serialName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
