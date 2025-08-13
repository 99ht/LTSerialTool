package indi.lt.serialtool.serialtool;

/**
 * @author Nonoas
 * @date 2025/8/10
 * @since
 */
import com.fazecast.jSerialComm.SerialPort;

public class SerialReader {
    public static void main(String[] args) {
        // 获取所有可用串口
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            System.out.println("未检测到串口设备");
            return;
        }
        for (SerialPort port : ports) {
            // System.out.println(port.getSystemPortName());
            System.out.println(port.getDescriptivePortName());
        }
        // 选择第一个串口（可根据需求更换）
        SerialPort comPort = ports[0];
        System.out.println("使用串口: " + comPort.getSystemPortName());

        // 配置串口参数（波特率、数据位、停止位、校验位）
        comPort.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);

        // 打开串口
        if (comPort.openPort()) {
            System.out.println("串口已打开");
        } else {
            System.out.println("串口打开失败");
            return;
        }

        // 接收数据
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                while (true) {
                    int numRead = comPort.getInputStream().read(buffer);
                    if (numRead > 0) {
                        String data = new String(buffer, 0, numRead);
                        System.out.println("收到数据: " + data);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                comPort.closePort();
            }
        }).start();
    }
}
