package indi.lt.serialtool.controller

import com.fazecast.jSerialComm.SerialPort
import indi.lt.serialtool.component.InlineCssRegexHighlighter
import indi.lt.serialtool.component.PromptInlineCssTextArea
import indi.lt.serialtool.component.SerialPortCombBox
import indi.lt.serialtool.component.SerialToggleButton
import indi.lt.serialtool.data.SerialPortSettings
import indi.lt.serialtool.service.SerialReadService
import indi.lt.serialtool.global.ConfigManager
import indi.lt.serialtool.utils.UIUtil
import javafx.application.Platform
import javafx.beans.Observable
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.stage.FileChooser
import javafx.util.Callback
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.FileWriter
import java.io.IOException
import java.net.URL
import java.util.*

/**
 * 接收模式逻辑
 * @author Nonoas
 * @date 2025/8/22
 * @since 1.0.0
 */
class SerialReceiveCtrl : Initializable {
    private val logger: Logger = LogManager.getLogger(MainController::class.java)

    // === FXML 注入的组件 ===
    @FXML
    private lateinit var rootPane: BorderPane

    @FXML
    private lateinit var menuBar: MenuBar

    @FXML
    private lateinit var lbSerialName: Label

    @FXML
    private lateinit var cbSerialList: SerialPortCombBox

    @FXML
    private lateinit var cbBautRateList: ComboBox<Int>

    @FXML
    private lateinit var textAreaOrigin: PromptInlineCssTextArea

    @FXML
    private lateinit var btnOpenSerial: SerialToggleButton

    @FXML
    private lateinit var cbTimeDisplay: CheckBox

    @FXML
    private lateinit var tfKeyWord: TextField

    @FXML
    private lateinit var lbRecvBytes: Label

    @FXML
    private lateinit var btnMoreSettings: Button

    // === 内部变量 ===
    private var serialReadService: SerialReadService? = null
    private var highlighter: InlineCssRegexHighlighter? = null
    private var keyLastSerial: String? = null

    // 串口参数设置
    private var serialPortSettings: SerialPortSettings = SerialPortSettings.createDefault()

    // 串口参数设置对话框 key
    private val KEY_SERIAL_SETTINGS = "receiveModeSerialSettings"

    override fun initialize(url: URL?, resourceBundle: ResourceBundle?) {
        initSerialPortSettings()
        initBautRateList()
        registerSerialEvent()
    }

    /**
     * 初始化串口参数设置
     */
    private fun initSerialPortSettings() {
        // 从配置加载串口参数设置
        serialPortSettings = ConfigManager.get(KEY_SERIAL_SETTINGS, SerialPortSettings::class.java, SerialPortSettings.createDefault())

        // 应用设置到串口组件
        cbSerialList.setSerialPortSettings(serialPortSettings)
    }

    private fun registerSerialEvent() {
        initOpenSerialButtonAction()
        initBautRateComboBoxAction()
        initMoreSettingsButtonAction()
    }

    /**
     * 初始化更多设置按钮动作
     */
    private fun initMoreSettingsButtonAction() {
        btnMoreSettings.setOnAction {
            showMoreSettings()
        }
    }

    /**
     * 显示更多设置对话框
     */
    fun showMoreSettings() {
        try {
            // 加载对话框 FXML
            val loader = FXMLLoader(javaClass.getResource("/fxml/serial-settings-dialog.fxml"))
            val dialogPane = loader.load<DialogPane>()

            // 获取控制器
            val dialogCtrl = loader.getController<SerialSettingsDialogCtrl>()

            // 设置当前设置
            dialogCtrl.setSettings(serialPortSettings)

            // 创建对话框
            val dialog = Dialog<SerialPortSettings>()
            dialog.dialogPane = dialogPane
            dialog.title = "串口参数设置"
            dialog.headerText = "自定义串口参数"

            // 设置按钮
            dialog.dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

            // 处理 OK 按钮
            dialog.resultConverter = Callback<ButtonType, SerialPortSettings> { buttonType ->
                if (buttonType == ButtonType.OK) {
                    // 从 UI 更新设置
                    dialogCtrl.updateSettingsFromUI()
                    dialogCtrl.getSettings()
                } else {
                    null
                }
            }

            // 显示对话框并等待结果
            dialog.showAndWait().ifPresent { settings ->
                // 保存设置
                serialPortSettings = settings
                ConfigManager.set(KEY_SERIAL_SETTINGS, settings)

                // 应用设置到串口组件
                cbSerialList.setSerialPortSettings(settings)

                // 显示成功提示
                UIUtil.showToast("串口参数已更新")
            }

        } catch (e: Exception) {
            logger.error("显示串口设置对话框失败", e)
            UIUtil.showToast("打开设置对话框失败")
        }
    }

    /**
     * 恢复自动滚动
     */
    @FXML
    private fun restoreScrolling() {
        textAreaOrigin.isAutoScroll = true
    }

    @FXML
    private fun clearLogs() {
        textAreaOrigin.area.clear()
        serialReadService?.resetRecvBytesCount()
        lbRecvBytes.text = "0 B"
    }

    @FXML
    private fun saveOriginLogs() {
        logger.info("saveOriginLogs")

        val content = textAreaOrigin.text
        if (content.isNullOrEmpty()) {
            logger.info("没有日志内容可保存")
            return
        }

        val fileChooser = FileChooser().apply {
            title = "保存日志文件"
            extensionFilters.addAll(
                FileChooser.ExtensionFilter("文本文件", "*.txt"), FileChooser.ExtensionFilter("所有文件", "*.*")
            )
        }

        val file = fileChooser.showSaveDialog(textAreaOrigin.scene.window) ?: return

        try {
            FileWriter(file, false).use { writer ->
                writer.write(content)
                logger.info("日志已保存到: ${file.absolutePath}")
            }
        } catch (e: IOException) {
            logger.error("保存日志失败", e)
        }
    }

    @FXML
    private fun saveFilterLogs() {
        logger.info("saveFilterLogs")
    }

    fun initSerialComboBoxAction() {
        cbSerialList.init(
            keyLastSerial, {
                UIUtil.getSelectedInt(cbBautRateList, 115200)
            }, btnOpenSerial.selectedProperty(), SerialPort.TIMEOUT_READ_SEMI_BLOCKING
        )
    }

    private fun initOpenSerialButtonAction() {
        cbSerialList.disableProperty().bind(btnOpenSerial.disableProperty())
        btnOpenSerial.selectedProperty().addListener { _: ObservableValue<out Boolean>?, _: Boolean?, newVal: Boolean ->
            if (newVal) {
                btnOpenSerial.isDisable = true
                cbSerialList.openSelectedSerial()
            } else {
                closeSelectSerial()
            }
        }
        cbSerialList.setOnOpenSucceed {
            btnOpenSerial.isDisable = false
            highlighter = InlineCssRegexHighlighter(textAreaOrigin).apply {
                patternTextProperty().bind(tfKeyWord.textProperty())
            }
            serialReadService = SerialReadService(
                cbSerialList.selectedPort, textAreaOrigin, cbTimeDisplay.selectedProperty()
            ) { highlighter?.schedule() }.also {
                it.setOnRecvBytesChanged { bytes ->
                    Platform.runLater {
                        lbRecvBytes.text = formatBytes(bytes)
                    }
                }
                it.start()
            }
        }
        cbSerialList.setOnOpenFailed {
            btnOpenSerial.isDisable = false
            btnOpenSerial.isSelected = false
        }
    }

    private fun initBautRateComboBoxAction() {
        if (!cbBautRateList.items.isEmpty()) {
            cbBautRateList.selectionModel.selectFirst()
        }
        cbBautRateList.selectionModel.selectedItemProperty().addListener { _: Observable? ->
            onBaudRateChanged()
        }
    }

    private fun onBaudRateChanged() {
        if (!cbSerialList.isActive) {
            return
        }
        closeSelectSerial()
        cbSerialList.openSelectedSerial()
    }

    private fun closeSelectSerial() {
        cbSerialList.closeSelectSerial()
        serialReadService?.cancel()
    }

    private fun initBautRateList() {
        val baudRates = FXCollections.observableArrayList(
            1200, 2400, 4800, 9600, 38400, 57600, 115200, 230400, 1500000, 2000000, 3000000
        )

        cbBautRateList.items = baudRates
        Platform.runLater {
            cbBautRateList.selectionModel.select(1500000)
            cbBautRateList.value = 1500000
        }
    }

    fun setSerialName(serialName: String?) {
        lbSerialName.text = serialName
    }

    fun setKeyLastSerial(keyLastSerial: String?) {
        this.keyLastSerial = keyLastSerial
    }

    /**
     * 将字节数格式化为人类可读的字符串
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}