package indi.lt.serialtool.controller;

import com.fazecast.jSerialComm.SerialPort;
import github.nonoas.jfx.flat.ui.AppState;
import github.nonoas.jfx.flat.ui.concurrent.TaskHandler;
import github.nonoas.jfx.flat.ui.stage.ToastQueue;
import indi.lt.serialtool.component.*;
import indi.lt.serialtool.data.CommandRepository;
import indi.lt.serialtool.service.SerialReadService;
import indi.lt.serialtool.service.SerialSenderService;
import indi.lt.serialtool.utils.StringUtil;
import indi.lt.serialtool.utils.UIUtil;
import indi.lt.serialtool.view.BaseStage;
import indi.lt.serialtool.view.SerialSendPane;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * 串口发送控制器
 */
public class SerialSendCtrl implements Initializable {

    private final Logger LOG = LogManager.getLogger(SerialSendCtrl.class);

    private static final int DEFAULT_BAUTRATE = 1500000;

    /**
     * 自动换行
     */
    @FXML
    public CheckBox lineBreak;
    public ToggleButton tgWindowMode;
    public SerialToggleButton btnScheduleSend;

    @FXML
    private TextField tfRemark;
    @FXML
    private TextField tfCommand;
    @FXML
    private PromptInlineCssTextArea taRecvArea;
    @FXML
    private TextArea taSendArea;
    @FXML
    private CheckBox cbHexDisplay;
    @FXML
    private CheckBox cbTimeStampDisplay;
    @FXML
    private CheckBox cbIsHex;


    @FXML
    private SerialPortCombBox cbSerialList;
    @FXML
    private ComboBox<Integer> cbBautrate;
    @FXML
    private Button btnSend;
    @FXML
    private SerialToggleButton btnOpenSerial;
    @FXML
    private StackPane spTableContainer;

    private SerialSendPane rootPane;

    private final CommandTableView table = new CommandTableView();

    private BaseStage currStage;

    private SerialSenderService serialSenderService;
    private SerialReadService serialReadService;

    // 保存上次串口选择的 key
    private final String keyLastSerial = "sendModeLastSerialPort";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        spTableContainer.getChildren().add(table);
        initBaudRateList();
        initSerialComboBox();
        loadHistoryCommands();
        setupButtonActions();

        // 切换独立窗口
        tgWindowMode.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                BaseStage baseStage = new BaseStage();
                currStage = baseStage;
                ToolBar toolBar = new ToolBar();
                toolBar.setMinHeight(40);
                VBox.setVgrow(rootPane, Priority.ALWAYS);
                baseStage.registryDragger(toolBar);
                baseStage.setContentView(new VBox(toolBar, rootPane));
                // baseStage.setSize(rootPane.getWidth(), rootPane.getHeight() + 40);
                baseStage.show();
            } else {
                // TODO
                currStage.close();
            }
        });


        // 定时发送
        btnScheduleSend.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                startSendCommand();
            } else {
                if (serialSenderService != null && serialSenderService.isRunning()) {
                    serialSenderService.cancel();
                }
            }
        });

        // 启动时默认打开第一个串口
        cbSerialList.openSelectedSerial();
    }

    /**
     * 初始化波特率下拉列表
     */
    private void initBaudRateList() {
        List<Integer> baudRates = Arrays.asList(1200, 2400, 4800, 9600, 38400, 57600, 115200, 230400, DEFAULT_BAUTRATE, 2000000, 3000000);
        cbBautrate.getItems().clear();
        cbBautrate.getItems().addAll(baudRates);
        cbBautrate.getSelectionModel().select(Integer.valueOf(DEFAULT_BAUTRATE));
    }

    /**
     * 初始化串口下拉列表
     */
    private void initSerialComboBox() {
        // 串口下拉框初始化
        cbSerialList.init(keyLastSerial,
                () -> UIUtil.getSelectedInt(cbBautrate, DEFAULT_BAUTRATE),
                btnOpenSerial.selectedProperty(),
                SerialPort.TIMEOUT_WRITE_BLOCKING | SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                1000
        );

        // 选中波特率变化时重新打开串口
        cbBautrate.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (cbSerialList.getSelectedPort() != null && cbSerialList.getSelectedPort().isOpen()) {
                cbSerialList.openSelectedSerial();
            }
        });
    }

    /**
     * 加载历史命令
     */
    private void loadHistoryCommands() {
        new TaskHandler<List<CommandTableView.CommandItem>>()
                .whenCall(CommandRepository.INSTANCE::loadAll)
                .andThen(table.getItems()::addAll)
                .handle();
    }

    /**
     * 绑定按钮事件
     */
    private void setupButtonActions() {
        btnSend.setOnAction(e -> sendData());

        cbSerialList.disableProperty().bind(btnOpenSerial.disabledProperty());
        btnOpenSerial.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                btnOpenSerial.setDisable(true);
                cbSerialList.openSelectedSerial();
            } else {
                if (cbSerialList.getSelectedPort() != null && cbSerialList.getSelectedPort().isOpen()) {
                    // 串口已打开 → 关闭
                    closeSerial();
                }
            }
        });

        cbSerialList.setOnOpenSucceed(() -> {
            btnOpenSerial.setDisable(false);
            serialReadService = new SerialReadService(
                    cbSerialList.getSelectedPort(),
                    taRecvArea,
                    new CheckBox() {{
                        setSelected(true);
                    }},
                    new SerialReadService.HighlighterScheduler() {
                        @Override
                        public void schedule() {
                            new InlineCssRegexHighlighter(taRecvArea).schedule();
                        }
                    }
            );
            serialReadService.start();
        });
        cbSerialList.setOnOpenFailed(() -> {
            btnOpenSerial.setDisable(false);
            btnOpenSerial.setSelected(false);
        });
    }

    /**
     * 关闭串口
     */
    public void closeSerial() {
        if (cbSerialList.getSelectedPort() != null && cbSerialList.getSelectedPort().isOpen()) {
            cbSerialList.getSelectedPort().closePort();
            LOG.info("串口已关闭");
            ToastQueue.show(AppState.getStage(), "串口已关闭", 800);
        }
        if (serialReadService != null) {
            serialReadService.cancel();
        }
    }

    /**
     * 发送数据
     */
    private void sendData() {
        if (cbSerialList.getSelectedPort() == null || !cbSerialList.getSelectedPort().isOpen()) {
            ToastQueue.show(AppState.getStage(), "串口未打开", 800);
            return;
        }

        String text = taSendArea.getText();
        if (text == null || text.trim().isEmpty()) {
            ToastQueue.show(AppState.getStage(), "发送内容不能为空", 800);
            return;
        }

        if (lineBreak.isSelected()) {
            text += "\n";
        }

        try {
            byte[] data = cbIsHex.isSelected() ? StringUtil.hexStringToBytes(text.trim()) : text.getBytes();
            cbSerialList.getSelectedPort().writeBytes(data, data.length);
            LOG.info("发送成功: " + text);
        } catch (Exception e) {
            LOG.error("发送失败", e);
            ToastQueue.show(AppState.getStage(), "发送失败: " + e.getMessage(), 1000);
        }
    }

    private void startSendCommand() {
        if (cbSerialList.getSelectedPort() == null || !cbSerialList.getSelectedPort().isOpen()) {
            ToastQueue.show(AppState.getStage(), "串口未打开", 800);
            btnScheduleSend.setSelected(false);
            return;
        }

        serialSenderService = new SerialSenderService(
                table.getItems().filtered(CommandTableView.CommandItem::isScheduled),
                cbSerialList.getSelectedPort(),
                taRecvArea,
                cbHexDisplay,
                cbTimeStampDisplay
        );
        serialSenderService.start();
    }

    /**
     * 添加自定义命令
     */
    @FXML
    private void addCommand() {
        if (tfCommand.getText().trim().isEmpty()) {
            ToastQueue.show(AppState.getStage(), "指令不能为空", 500);
            return;
        }

        String type = cbIsHex.isSelected() ? "HEX" : "TXT";
        CommandTableView.CommandItem item = new CommandTableView.CommandItem(
                UUID.randomUUID().toString(),
                tfRemark.getText(),
                tfCommand.getText(),
                type
        );
        table.getItems().add(item);
        CommandRepository.INSTANCE.add(item);
    }

    public void setRootPane(SerialSendPane rootPane) {
        this.rootPane = rootPane;
    }
}
