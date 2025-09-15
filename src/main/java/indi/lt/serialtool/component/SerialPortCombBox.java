package indi.lt.serialtool.component;

import com.fazecast.jSerialComm.SerialPort;
import github.nonoas.jfx.flat.ui.concurrent.TaskHandler;
import indi.lt.serialtool.ConfigManager;
import javafx.scene.control.ComboBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Nonoas
 * @date 2025/9/16
 * @since 1.0.0
 */
public class SerialPortCombBox extends ComboBox<String> {

    private final Logger LOG = LogManager.getLogger(SerialPortCombBox.class);

    public SerialPortCombBox() {
    }

    public void init(String keyLastSerial) {
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
