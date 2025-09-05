package indi.lt.serialtool.view;

import indi.lt.serialtool.controller.SerialSendCtrl;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;

import java.io.IOException;

public class SerialSendPane extends SplitPane {

    private SerialSendCtrl  controller;

    public SerialSendPane(String serialName){
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/fxml/serial-send-pane.fxml")
        );
        fxmlLoader.setRoot(this);      // 将 this 作为 fxml 根节点

        try {
            fxmlLoader.load(); // 加载 fxml
            controller = fxmlLoader.getController(); // 获取逻辑控制器
            //controller.setSerialName(serialName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
