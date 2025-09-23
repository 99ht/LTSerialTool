package indi.lt.serialtool.utils;

/**
 * @author Nonoas
 * @date 2025/9/23
 * @since
 */
public class StringUtil {
    /**
     * HEX 转 byte
     */
    public static byte[] hexStringToBytes(String hex) {
        hex = hex.replaceAll("\\s+", "");
        if (hex.length() % 2 != 0) hex = "0" + hex;
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }
}
