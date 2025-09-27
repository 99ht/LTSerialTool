package indi.lt.serialtool.constant;

/**
 * @author Nonoas
 * @version 1.0.0
 * @date 2025/9/25
 * @since 1.0.0
 */
public enum LogType {
    RECEIVE("RX"),
    SEND("TX"),
    ;

    private String type;

    private LogType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
