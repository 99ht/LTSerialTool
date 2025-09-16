package indi.lt.serialtool.component;


import atlantafx.base.theme.Styles;
import github.nonoas.jfx.flat.ui.concurrent.TaskHandler;
import indi.lt.serialtool.data.CommandRepository;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Nonoas
 * @date 2025/9/6
 * @since
 */
public class CommandTableView extends TableView<CommandTableView.CommandItem> {

    private final Logger LOG = LogManager.getLogger(CommandTableView.class);


    public CommandTableView() {
        super(FXCollections.observableArrayList());

        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        // 列：备注
        TableColumn<CommandItem, String> remarkCol = new TableColumn<>("备注");
        remarkCol.setCellValueFactory(new PropertyValueFactory<>("remark"));
        remarkCol.setPrefWidth(150);

        // 列：指令
        TableColumn<CommandItem, String> commandCol = new TableColumn<>("指令");
        commandCol.setCellValueFactory(new PropertyValueFactory<>("command"));
        commandCol.setPrefWidth(200);

        // 列：操作
        TableColumn<CommandItem, CommandItem> actionCol = new TableColumn<>("操作");
        actionCol.setResizable(false);
        actionCol.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Label typeLabel = new Label();
            private final Button sendBtn = new Button("发送");
            private final Button deleteBtn = new Button("删除");
            private final HBox box = new HBox(5, typeLabel, sendBtn, deleteBtn);

            {
                HBox.setHgrow(typeLabel, Priority.ALWAYS);
                typeLabel.setMaxWidth(Double.MAX_VALUE);

                deleteBtn.setMinWidth(50);
                deleteBtn.getStyleClass().add(Styles.DANGER);
                box.setAlignment(Pos.CENTER_LEFT);
                sendBtn.setOnAction(e -> {
                    CommandItem item = getItem();
                    if (item != null) {
                        LOG.info("发送: " + item.getCommand());
                    }
                });
                sendBtn.getStyleClass().add(Styles.ACCENT);

                deleteBtn.setOnAction(e -> {
                    CommandItem item = getItem();
                    if (item != null) {
                        getTableView().getItems().remove(item);
                        TaskHandler.backRun(()-> CommandRepository.INSTANCE.remove(item));
                    }
                });
            }

            @Override
            protected void updateItem(CommandItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    typeLabel.setText(item.getCommandType());
                    setGraphic(box);
                }
            }
        });
        actionCol.setPrefWidth(155);

        // 列：定时发送
        TableColumn<CommandItem, CommandItem> scheduleCol = new TableColumn<>("定时发送");
        scheduleCol.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        scheduleCol.setCellFactory(col -> new TableCell<>() {
            private final TextField intervalField = new TextField();
            private final Label unitLabel = new Label("ms");
            private final CheckBox enableCheck = new CheckBox();
            private final HBox box = new HBox(5, intervalField, unitLabel, enableCheck);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                intervalField.setPrefWidth(60);
                // 只允许输入正整数
                intervalField.textProperty().addListener((obs, oldV, newV) -> {
                    if (!newV.matches("\\d*")) {
                        intervalField.setText(oldV);
                    }
                });
            }

            @Override
            protected void updateItem(CommandItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    intervalField.setText(String.valueOf(item.getInterval()));
                    enableCheck.setSelected(item.isScheduled());

                    intervalField.textProperty().addListener((o, ov, nv) -> {
                        if (!nv.isEmpty()) {
                            item.setInterval(Integer.parseInt(nv));
                        }
                    });
                    enableCheck.selectedProperty().addListener((o, ov, nv) -> item.setScheduled(nv));

                    setGraphic(box);
                }
            }
        });
        scheduleCol.setPrefWidth(200);

        getColumns().addAll(remarkCol, commandCol, actionCol, scheduleCol);
    }

    // 数据模型
    public static class CommandItem {

        private final StringProperty id = new SimpleStringProperty();

        private final StringProperty remark = new SimpleStringProperty();
        private final StringProperty command = new SimpleStringProperty();
        private final StringProperty commandType = new SimpleStringProperty("类型A");
        private final IntegerProperty interval = new SimpleIntegerProperty(1000);
        private final BooleanProperty scheduled = new SimpleBooleanProperty(false);

        public CommandItem(String id, String remark, String command, String commandType) {
            this.setId(id);
            this.remark.set(remark);
            this.command.set(command);
            this.commandType.set(commandType);
        }

        public String getRemark() {
            return remark.get();
        }

        public void setRemark(String v) {
            remark.set(v);
        }

        public StringProperty remarkProperty() {
            return remark;
        }

        public String getCommand() {
            return command.get();
        }

        public void setCommand(String v) {
            command.set(v);
        }

        public StringProperty commandProperty() {
            return command;
        }

        public String getCommandType() {
            return commandType.get();
        }

        public void setCommandType(String v) {
            commandType.set(v);
        }

        public StringProperty commandTypeProperty() {
            return commandType;
        }

        public int getInterval() {
            return interval.get();
        }

        public void setInterval(int v) {
            interval.set(v);
        }

        public IntegerProperty intervalProperty() {
            return interval;
        }

        public boolean isScheduled() {
            return scheduled.get();
        }

        public void setScheduled(boolean v) {
            scheduled.set(v);
        }

        public BooleanProperty scheduledProperty() {
            return scheduled;
        }

        public String getId() {
            return id.get();
        }

        public StringProperty idProperty() {
            return id;
        }

        public void setId(String id) {
            this.id.set(id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CommandItem)) {
                return false;
            }
            return getId().equals(((CommandItem) obj).getId());
        }
    }
}
