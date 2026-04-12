package indi.lt.serialtool.controller;

import com.fazecast.jSerialComm.SerialPort;
import github.nonoas.jfx.flat.ui.AppState;
import github.nonoas.jfx.flat.ui.concurrent.TaskHandler;
import github.nonoas.jfx.flat.ui.stage.ToastQueue;
import indi.lt.serialtool.component.CommandTableView;
import indi.lt.serialtool.component.InlineCssRegexHighlighter;
import indi.lt.serialtool.component.PromptInlineCssTextArea;
import indi.lt.serialtool.component.SerialPortCombBox;
import indi.lt.serialtool.component.SerialToggleButton;
import indi.lt.serialtool.constant.CommandType;
import indi.lt.serialtool.constant.LogType;
import indi.lt.serialtool.data.CommandRepository;
import indi.lt.serialtool.data.LogText;
import indi.lt.serialtool.data.SerialPortSettings;
import indi.lt.serialtool.global.ConfigManager;
import indi.lt.serialtool.service.SerialReadService;
import indi.lt.serialtool.service.SerialSenderService;
import indi.lt.serialtool.utils.StringUtil;
import indi.lt.serialtool.utils.UIUtil;
import indi.lt.serialtool.view.BaseStage;
import indi.lt.serialtool.view.SerialSendPane;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private CheckBox cbHexSend;
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
    @FXML
    private Button btnMoreSettings;

    private SerialSendPane rootPane;

    private final CommandTableView table = new CommandTableView();

    private BaseStage currStage;

    private SerialSenderService serialSenderService;
    private SerialReadService serialReadService;

    private InlineCssRegexHighlighter highlighter;

    // 保存上次串口选择的 key
    private final String keyLastSerial = "sendModeLastSerialPort";

    // 串口参数设置
    private SerialPortSettings serialPortSettings;

    // 串口参数设置对话框 key
    private static final String KEY_SERIAL_SETTINGS = "sendModeSerialSettings";

    /**
     * 初始化串口参数设置
     */
    private void initSerialPortSettings() {
        // 从配置加载串口参数设置
        serialPortSettings = ConfigManager.getObject(KEY_SERIAL_SETTINGS, SerialPortSettings.class, SerialPortSettings.createDefault());

        // 应用设置到串口组件
        cbSerialList.setSerialPortSettings(serialPortSettings);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        highlighter = new InlineCssRegexHighlighter(taRecvArea);
        spTableContainer.getChildren().add(table);
        initSerialPortSettings();
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
                rootPane.setVisible(false);
                baseStage.setContentView(new VBox(toolBar, rootPane));
                // baseStage.setSize(rootPane.getWidth(), rootPane.getHeight() + 40);
                Platform.runLater(() -> {
                    baseStage.show();
                    rootPane.setVisible(true);
                });
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
        // cbSerialList.openSelectedSerial();
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
                0,  // timeOutMillionTime
                serialPortSettings);  // settings

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

        btnMoreSettings.setOnAction(e -> showMoreSettings());

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
                    cbTimeStampDisplay.selectedProperty(),
                    cbHexDisplay.selectedProperty(),
                    () -> highlighter.schedule(),
                    true
            );
            serialReadService.start();
        });
        cbSerialList.setOnOpenFailed(() -> {
            btnOpenSerial.setDisable(false);
            btnOpenSerial.setSelected(false);
        });
    }

    /**
     * 显示更多设置对话框
     */
    @FXML
    private void showMoreSettings() {
        try {
            // 加载对话框 FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/serial-settings-dialog.fxml"));
            DialogPane dialogPane = loader.load();

            // 获取控制器
            SerialSettingsDialogCtrl dialogCtrl = loader.getController();

            // 设置当前设置
            dialogCtrl.setSettings(serialPortSettings);

            // 创建对话框
            Dialog<SerialPortSettings> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("串口参数设置");
            dialog.setHeaderText("自定义串口参数");

            // 注意：FXML 中已经定义了按钮，不需要再次添加

            // 处理 OK 按钮
            dialog.setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    // 从 UI 更新设置
                    dialogCtrl.updateSettingsFromUI();
                    return dialogCtrl.getSettings();
                }
                return null;
            });

            // 显示对话框并等待结果
            dialog.showAndWait().ifPresent(settings -> {
                // 保存设置
                serialPortSettings = settings;
                ConfigManager.setObject(KEY_SERIAL_SETTINGS, settings);

                // 应用设置到串口组件
                cbSerialList.setSerialPortSettings(settings);

                // 同步波特率到主界面下拉框（如果设置中有指定波特率）
                cbBautrate.getSelectionModel().select(Integer.valueOf(settings.getBaudRate()));

                // 显示成功提示
                ToastQueue.show(AppState.getStage(), "串口参数已更新", 800);
            });

        } catch (Exception e) {
            LOG.error("显示串口设置对话框失败", e);
            ToastQueue.show(AppState.getStage(), "打开设置对话框失败", 1000);
        }
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

        try {
            boolean isHexSend = cbHexSend.isSelected();
            byte[] data;
            String logText;

            if (isHexSend) {
                data = StringUtil.hexStringToBytes(text);
                if (lineBreak.isSelected()) {
                    data = Arrays.copyOf(data, data.length + 1);
                    data[data.length - 1] = (byte) '\n';
                }
                logText = StringUtil.bytesToHexString(data);
            } else {
                String content = lineBreak.isSelected() ? text + "\n" : text;
                data = content.getBytes(StandardCharsets.UTF_8);
                logText = content;
            }

            cbSerialList.getSelectedPort().writeBytes(data, data.length);
            LOG.info("发送成功: {}", logText);
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            LogText sendLog = new LogText(ts, logText, LogType.SEND);
            taRecvArea.appendText(sendLog.getLogText(cbTimeStampDisplay.isSelected(), true) + "\r\n");
        } catch (IllegalArgumentException e) {
            LOG.error("HEX 发送失败", e);
            ToastQueue.show(AppState.getStage(), "HEX格式错误: " + e.getMessage(), 1200);
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

        CommandType type = cbIsHex.isSelected() ? CommandType.HEX : CommandType.TXT;
        CommandTableView.CommandItem item = new CommandTableView.CommandItem(
                UUID.randomUUID().toString(),
                tfRemark.getText(),
                tfCommand.getText(),
                type.toString()
        );
        table.getItems().add(item);
        CommandRepository.INSTANCE.add(item);
    }

    public void setRootPane(SerialSendPane rootPane) {
        this.rootPane = rootPane;
    }

    /**
     * 恢复自动滚动
     */
    @FXML
    private void restoreScrolling() {
        taRecvArea.setAutoScroll(true);
    }

    @FXML
    private void clearLogs() {
        taRecvArea.getArea().clear();
    }
}



