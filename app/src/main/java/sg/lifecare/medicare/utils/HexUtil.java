package sg.lifecare.medicare.utils;

/**
 * Created by sweelai on 12/30/15.
 */
public class HexUtil {

    public static String toHexString(byte[] a) {
        if (a != null && a.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (byte b : a) {
                sb.append(String.format("%02X ", b &0xff));
            }
            return sb.toString();
        }

        return "";
    }
}
