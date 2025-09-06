package indi.lt.serialtool.controller;

import github.nonoas.jfx.flat.ui.AppState;
import github.nonoas.jfx.flat.ui.stage.ToastQueue;
import indi.lt.serialtool.component.CommandTableView;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    public TextField tfRemark;
    @FXML
    public TextField tfCommand;
    @FXML
    public CheckBox cbIsHex;

    @FXML
    private StackPane spTableContainer;

    private final CommandTableView table = new CommandTableView();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        spTableContainer.getChildren().add(table);
    }

    @FXML
    public void addCommand() {
        if (tfCommand.getText().trim().isEmpty()) {
            ToastQueue.show(AppState.getStage(), "指令不能为空", 500);
            return;
        }
        String commandType;
        if (cbIsHex.isSelected()) {
            commandType = "HEX";
        } else {
            commandType = "TXT";
        }
        table.getItems().add(new CommandTableView.CommandItem(tfRemark.getText(), tfCommand.getText(), commandType));
    }
}
