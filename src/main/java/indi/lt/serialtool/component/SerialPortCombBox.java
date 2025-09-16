package indi.lt.serialtool.component;

import com.fazecast.jSerialComm.SerialPort;
import github.nonoas.jfx.flat.ui.AppState;
import github.nonoas.jfx.flat.ui.concurrent.TaskHandler;
import github.nonoas.jfx.flat.ui.stage.ToastQueue;
import indi.lt.serialtool.ConfigManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.ComboBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Nonoas
 * @date 2025/9/16
 * @since 1.0.0
 */
public class SerialPortCombBox extends ComboBox<String> {

    private final Logger LOG = LogManager.getLogger(SerialPortCombBox.class);

    private SerialPort comPort;

    private String keyLastSerial;

    private final SimpleIntegerProperty baudRateProperty = new SimpleIntegerProperty();

    public SerialPortCombBox() {
    }

    /**
     * 初始化串口下拉框
     *
     * @param keyLastSerial    最后一次选中的串口配置 KEY
     * @param baudRateProperty 波特率绑定值
     */
    public void init(String keyLastSerial, ObjectProperty<Integer> baudRateProperty) {
        this.keyLastSerial = Objects.requireNonNull(keyLastSerial);
        this.baudRateProperty.bind(Objects.requireNonNull(baudRateProperty));

        // 串口下拉框初始化
        new TaskHandler<SerialPortData>()
                .whenCall(() -> {
                    String lastSerial = ConfigManager.get(keyLastSerial, null);
                    SerialPort[] commPorts = SerialPort.getCommPorts();
                    return new SerialPortData(commPorts, lastSerial);
                })
                .andThen(data -> {
                    // 清空列表
                    getItems().clear();

                    for (SerialPort port : data.serialPorts) {
                        getItems().add(port.getSystemPortName() + " - " + port.getDescriptivePortName());
                    }
                    // 根據實際項目數量設定可見行數
                    int itemCount = getItems().size();
                    // 設定一個上限，例如10行
                    setVisibleRowCount(Math.min(itemCount, 5));

                    if (data.lastSerial != null && getItems().contains(data.lastSerial)) {
                        getSelectionModel().select((data.lastSerial));
                    } else if (!getItems().isEmpty()) {
                        getSelectionModel().selectFirst();
                    }
                })
                .handle();

        // 展开下拉框时刷新列表
        setOnShowing(event -> refreshSerialList());

        // 选中串口时自动打开
        getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                openSelectedSerial();
            }
        });
    }

    private void refreshSerialList() {
        // 刷新前记录当前选中的串口（用于后续恢复）
        String currentSelected = getValue();

        new TaskHandler<List<String>>().whenCall(() -> {
            List<String> serialList = new ArrayList<>();
            for (SerialPort serialPort : getSerialPorts()) {
                serialList.add(serialPort.getSystemPortName() + " - " + serialPort.getDescriptivePortName());
            }
            return serialList;
        }).andThen(val -> {
            LOG.info("读取完成" + val);
            getItems().clear();
            getItems().addAll(val);

            // 刷新后：如果之前有选中项且仍存在，则恢复选中；否则不自动选中
            if (currentSelected != null && val.contains(currentSelected)) {
                setValue(currentSelected); // 恢复之前的选中项
            } else {
                // 首次加载或选中项已消失，可选：不自动选中任何项
                getSelectionModel().clearSelection();
            }
        }).handle();
    }

    /**
     * 打开用户选择的串口
     */
    public void openSelectedSerial() {
        if (getItems().isEmpty()) {
            LOG.warn("串口列表为空，无法打开串口");
            ToastQueue.show(AppState.getStage(), "未检测到串口设备", 800);
            return;
        }

        String selectedSerial = getValue();
        if (selectedSerial == null || selectedSerial.isEmpty()) {
            selectedSerial = getItems().get(0);
            setValue(selectedSerial);
        }

        SerialPort[] ports = SerialPort.getCommPorts();
        int index = getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= ports.length) {
            LOG.warn("串口索引超出范围");
            return;
        }

        // 关闭已有串口
        if (comPort != null && comPort.isOpen()) {
            comPort.closePort();
            LOG.info("关闭旧串口");
        }

        comPort = ports[index];

        int baudRate = baudRateProperty.getValue() != null ? baudRateProperty.getValue() : 115200;
        comPort.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

        if (comPort.openPort()) {
            LOG.info("串口已打开: " + comPort.getSystemPortName() + " @ " + baudRate);
            ToastQueue.show(AppState.getStage(), "串口已打开: " + comPort.getSystemPortName(), 800);
            ConfigManager.set(keyLastSerial, selectedSerial);
        } else {
            LOG.error("串口打开失败: " + comPort.getSystemPortName());
            ToastQueue.show(AppState.getStage(), "串口打开失败", 800);
        }
    }

    public SerialPort getSelectedPort() {
        return comPort;
    }

    public void closeSelectSerial() {
        if (null != comPort) {
            comPort.closePort();
        }
    }

    private static List<SerialPort> getSerialPorts() {
        return Arrays.asList(SerialPort.getCommPorts());
    }

    static class SerialPortData {
        private final SerialPort[] serialPorts;
        private final String lastSerial;

        public SerialPortData(SerialPort[] serialPorts, String lastSerial) {
            this.serialPorts = serialPorts;
            this.lastSerial = lastSerial;
        }

    }
}
