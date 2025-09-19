package indi.lt.serialtool.controller

import com.fazecast.jSerialComm.SerialPort
import indi.lt.serialtool.component.InlineCssRegexHighlighter
import indi.lt.serialtool.component.PromptInlineCssTextArea
import indi.lt.serialtool.component.SerialPortCombBox
import indi.lt.serialtool.component.SerialToggleButton
import indi.lt.serialtool.service.SerialReadService
import javafx.beans.Observable
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.stage.FileChooser
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.FileWriter
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.function.Supplier

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

    // === 内部变量 ===
    private var serialReadService: SerialReadService? = null
    private var highlighter: InlineCssRegexHighlighter? = null
    private var keyLastSerial: String? = null

    override fun initialize(url: URL?, resourceBundle: ResourceBundle?) {
        initBautRateList()
        registerSerialEvent()
    }

    private fun registerSerialEvent() {
        initOpenSerialButtonAction()
        initBautRateComboBoxAction()
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
                FileChooser.ExtensionFilter("文本文件", "*.txt"),
                FileChooser.ExtensionFilter("所有文件", "*.*")
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
            keyLastSerial!!, { cbBautRateList.value }, btnOpenSerial.selectedProperty(),
            SerialPort.TIMEOUT_READ_SEMI_BLOCKING
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
                cbSerialList.selectedPort, textAreaOrigin, cbTimeDisplay
            ) { highlighter?.schedule() }.also { it.start() }
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

        cbBautRateList.items.setAll(baudRates)
        cbBautRateList.selectionModel.select(1500000)

        logger.info("波特率初始化完成：${cbBautRateList.items}")
    }

    fun setSerialName(serialName: String?) {
        lbSerialName.text = serialName
    }

    fun setKeyLastSerial(keyLastSerial: String?) {
        this.keyLastSerial = keyLastSerial
    }
}
