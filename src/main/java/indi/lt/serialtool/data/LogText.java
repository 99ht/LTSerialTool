package indi.lt.serialtool.data;

import indi.lt.serialtool.constant.LogType;

public class LogText {
    private final String timeStamp;
    private final String text;

    private final LogType logType;

    public LogText(String timeStamp, String text, LogType logType) {
        this.logType = logType;
        this.timeStamp = timeStamp;
        this.text = text;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LogText)) {
            return false;
        }
        return String.valueOf(this).equals(String.valueOf(obj));
    }

    public String getText() {
        return text;
    }

    public String getLogText(boolean hasTimeStamp) {
        if (hasTimeStamp) {
            return toString();
        } else {
            return "[" + logType.getType() + "] " + text;
        }
    }

    @Override
    public String toString() {
        return "[" + timeStamp + " " + logType.getType() + "] " + text;
    }
}
