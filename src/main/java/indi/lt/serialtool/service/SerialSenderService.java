package indi.lt.serialtool.service;


import com.fazecast.jSerialComm.SerialPort;
import indi.lt.serialtool.component.CommandTableView;
import indi.lt.serialtool.utils.StringUtil;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;


/**
 * @author Nonoas
 * @date 2025/9/23
 * @since
 */
public class SerialSenderService extends Service<Void> {

    private final Logger LOG = LogManager.getLogger(SerialSenderService.class);

    private final List<CommandTableView.CommandItem> commands;

    private final SerialPort serialPort;

    private final TextArea taRecvArea;
    private final CheckBox cbHexDisplay;
    private final CheckBox cbTimeStampDisplay;

    public SerialSenderService(List<CommandTableView.CommandItem> commands,
                               SerialPort selectedPort, TextArea taRecvArea, CheckBox cbHexDisplay,
                                CheckBox cbTimeStampDisplay) {
        this.commands = commands;
        this.serialPort = selectedPort;
        this.taRecvArea = taRecvArea;
        this.cbHexDisplay = cbHexDisplay;
        this.cbTimeStampDisplay = cbTimeStampDisplay;
    }

    public SerialSenderService(FilteredList<CommandTableView.CommandItem> filtered, SerialPort selectedPort, TextArea taRecvArea, CheckBox cbHexDisplay, CheckBox cbTimeStampDisplay, List<CommandTableView.CommandItem> commands, SerialPort serialPort, TextArea taRecvArea1, CheckBox cbHexDisplay1, CheckBox cbTimeStampDisplay1) {
        this.commands = commands;
        this.serialPort = serialPort;
        this.taRecvArea = taRecvArea1;
        this.cbHexDisplay = cbHexDisplay1;
        this.cbTimeStampDisplay = cbTimeStampDisplay1;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                int commandIndex = 0;
                while (true) {
                    // 检查任务是否被取消
                    if (isCancelled()) {
                        LOG.info("任务已取消。");
                        break;
                    }

                    // 如果循环结束，可以重新开始或停止
                    if (commandIndex >= commands.size()) {
                        commandIndex = 0; // 重新开始循环
                    }

                    CommandTableView.CommandItem currentCommand = commands.get(commandIndex);

                    // 1. 更新UI（线程安全）
                    updateMessage("正在发送指令: " + currentCommand.getCommand());

                    // 2. 模拟耗时的串口发送
                    Thread.sleep(currentCommand.getInterval());

                    try {
                        String command = currentCommand.getCommand().trim() + "\n";
                        byte[] data = currentCommand.getCommandType().equals("HEX")
                                ? StringUtil.hexStringToBytes(command)
                                : command.getBytes();
                        serialPort.writeBytes(data, data.length);
                        LOG.info("已发送: " + currentCommand.getCommand());
                    } catch (Exception e) {
                        LOG.error("指令[{}]发送失败", currentCommand.getCommand(), e);
                    }
                    // 3. 准备下一个指令
                    commandIndex++;
                }
                return null;
            }
        };
    }
}