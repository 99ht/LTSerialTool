package indi.lt.serialtool.controller;

import github.nonoas.jfx.flat.ui.AppState;
import github.nonoas.jfx.flat.ui.concurrent.TaskHandler;
import github.nonoas.jfx.flat.ui.stage.ToastQueue;
import indi.lt.serialtool.component.CommandTableView;
import indi.lt.serialtool.data.CommandRepository;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

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

        new TaskHandler<List<CommandTableView.CommandItem>>()
                .whenCall(CommandRepository.INSTANCE::loadAll)
                .andThen(e -> table.getItems().addAll(e))
                .handle();
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
        CommandTableView.CommandItem commandItem = new CommandTableView.CommandItem(
                UUID.randomUUID().toString(),
                tfRemark.getText(), tfCommand.getText(), commandType);
        table.getItems().add(commandItem);
        CommandRepository.INSTANCE.add(commandItem);
    }
}
