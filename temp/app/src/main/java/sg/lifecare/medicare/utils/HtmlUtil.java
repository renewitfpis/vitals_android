package sg.lifecare.medicare.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Created by wanping on 16/9/16.
 */
public class HtmlUtil {

    public static String decodeString(String str){
        if(str == null || str.isEmpty())
            return "";

        try {
            return URLDecoder.decode(str,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }

    public static String encodeString(String str){
        if(str == null || str.isEmpty())
            return "";

        try {
            return URLEncoder.encode(str,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }


}
