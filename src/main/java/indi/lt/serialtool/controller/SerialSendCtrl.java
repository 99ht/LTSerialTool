package indi.lt.serialtool.controller;

import indi.lt.serialtool.component.CommandTableView;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableRow;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.text.TableView;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * @author Nonoas
 * @date 2025/8/22
 * @since 1.0.0
 */
public class SerialSendCtrl implements Initializable {
    private final Logger LOG = LogManager.getLogger(SerialSendCtrl.class);

    @FXML
    private StackPane spTableContainer;

    private final CommandTableView table = new CommandTableView();


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        spTableContainer.getChildren().add(table);
    }

    @FXML
    public void addCommand() {
        table.getItems().addAll(
                new CommandTableView.CommandItem("备注1", "AT+CMD1")
        );

    }
}
