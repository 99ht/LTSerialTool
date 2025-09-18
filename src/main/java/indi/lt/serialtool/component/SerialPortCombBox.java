package indi.lt.serialtool.component;

import com.fazecast.jSerialComm.SerialPort;
import github.nonoas.jfx.flat.ui.AppState;
import github.nonoas.jfx.flat.ui.concurrent.TaskHandler;
import github.nonoas.jfx.flat.ui.stage.ToastQueue;
import indi.lt.serialtool.ConfigManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ComboBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    private Runnable onOpenSucceed;
    private Runnable onOpenFailed;

    /**
     * 启用状态，启用状态下，切换串口会自动关闭上一个，并打开下一个串口
     */
    private final SimpleBooleanProperty activeProperty = new SimpleBooleanProperty(false);

    private int timeOutMode;

    public SerialPortCombBox() {
    }

    /**
     * 初始化串口下拉框
     *
     * @param keyLastSerial    最后一次选中的串口配置 KEY
     * @param baudRateProperty 波特率绑定值
     * @param activeProperty   启用状态绑定
     * @param timeOutMode      超时模式
     */
    public void init(String keyLastSerial,
                     ObjectProperty<Integer> baudRateProperty,
                     BooleanProperty activeProperty,
                     int timeOutMode) {
        this.timeOutMode = timeOutMode;
        this.keyLastSerial = Objects.requireNonNull(keyLastSerial);
        this.baudRateProperty.bind(Objects.requireNonNull(baudRateProperty));
        this.activeProperty.bind(activeProperty);

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
            // 如果未激活，则不做处理
            if (!activeProperty.get() || Objects.equals(oldVal, newVal)) {
                return;
            }
            // TODO 以下操作为耗时操作，需要异步出处理
            if (oldVal != null) {
                closeSelectSerial();
            }
            if (newVal != null) {
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
            LOG.debug("读取完成" + val);
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
    public synchronized void openSelectedSerial() {
        new TaskHandler<Boolean>().whenCall(() -> {
            try {
                LOG.info("尝试打开" + getValue());
                if (getItems().isEmpty()) {
                    LOG.warn("串口列表为空，无法打开串口");
                    ToastQueue.show(AppState.getStage(), "未检测到串口设备", 800);
                    return false;
                }

                String selectedSerial = getValue();
                if (selectedSerial == null || selectedSerial.isEmpty()) {
                    selectedSerial = getItems().get(0);
                    setValue(selectedSerial);
                }

                String selectedSerialFinal = selectedSerial;
                SerialPort[] ports = SerialPort.getCommPorts();
                int index = getSelectionModel().getSelectedIndex();

                // 1️⃣ 检查索引合法性
                if (index < 0 || index >= ports.length) {
                    LOG.warn("串口索引超出范围");
                    return false;
                }

                // 2️⃣ 关闭已有串口
                if (comPort != null && comPort.isOpen()) {
                    comPort.closePort();
                    LOG.info("关闭旧串口");
                }
                comPort = ports[index];
                int baudRate = getBaudRate();
                comPort.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);

                // 3️⃣ 设置读写超时模式（保持非阻塞或半阻塞都可以）
                comPort.setComPortTimeouts(timeOutMode, 0, 0);

                // 4️⃣ 异步打开串口 + 超时控制
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<Boolean> future = executor.submit(() -> comPort.openPort());

                boolean opened = false;
                try {
                    // 设置超时时间，例如 1000ms
                    opened = future.get(1000, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    LOG.warn("串口打开超时: " + comPort.getSystemPortName());
                    future.cancel(true); // 尝试取消
                } catch (Exception e) {
                    LOG.error("串口打开异常", e);
                } finally {
                    executor.shutdown();
                }

                // 5️⃣ 打印结果并保存配置
                if (opened) {
                    LOG.info("串口已打开: " + comPort.getSystemPortName() + " @ " + baudRate);
                    ConfigManager.set(keyLastSerial, selectedSerialFinal);
                } else {
                    LOG.error("串口打开失败: " + comPort.getSystemPortName());
                }
                return opened;
            } catch (Exception e) {
                LOG.error(e);
                return false;
            }
        }).andThen(opened -> {
            if (opened) {
                if (onOpenSucceed != null) {
                    onOpenSucceed.run();
                }
                ToastQueue.show(AppState.getStage(), "串口已打开: " + comPort.getSystemPortName(), 800);
            } else {
                if (onOpenFailed != null) {
                    onOpenFailed.run();
                }
                ToastQueue.show(AppState.getStage(), "串口打开失败", 800);
            }
        }).handle();
    }

    public boolean isActive() {
        return activeProperty.get();
    }

    public SimpleBooleanProperty activePropertyProperty() {
        return activeProperty;
    }

    public void setActiveProperty(boolean activeProperty) {
        this.activeProperty.set(activeProperty);
    }

    private int getBaudRate() {
        Object value = baudRateProperty.getValue();
        if (value == null) {
            return 115200;
        }
        return (int) value;
    }

    public SerialPort getSelectedPort() {
        return comPort;
    }

    public void closeSelectSerial() {
        new TaskHandler<Void>().whenCall(
                () -> {
                    if (null != comPort && comPort.isOpen()) {
                        comPort.closePort();
                    }
                    return null;
                }
        ).andThen(e -> {

        }).handle();
    }

    private static List<SerialPort> getSerialPorts() {
        return Arrays.asList(SerialPort.getCommPorts());
    }

    public void setOnOpenSucceed(Runnable onOpenSucceed) {
        this.onOpenSucceed = onOpenSucceed;
    }

    public void setOnOpenFailed(Runnable onOpenFailed) {
        this.onOpenFailed = onOpenFailed;
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
