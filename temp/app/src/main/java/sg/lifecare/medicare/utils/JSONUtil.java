package sg.lifecare.medicare.utils;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

/**
 * Created by sweelai on 14/7/16.
 */
public class JSONUtil {

    public static boolean getBoolean(JSONObject jsonObject, String field) {
        try {
            if (jsonObject.has(field)) {
                String s = jsonObject.getString(field);

                if ("null".equalsIgnoreCase(s) == false) {
                    return jsonObject.getBoolean(field);
                }
            }
        } catch (JSONException e) {
            Timber.e("JSONUtil", e.getMessage());
        }
        return false;
    }

    public static String getString(JSONObject jsonObject, String field) {
        try {
            if (jsonObject.has(field)) {
                String s = jsonObject.getString(field);

                if ("null".equalsIgnoreCase(s) == false) {
                    return s;
                }
            }
        } catch (JSONException e) {
            Timber.e("JSONUtil", e.getMessage());
        }
        return "";
    }

    public static int getInt(JSONObject jsonObject, String field) {
        try {
            String s = getString(jsonObject, field);

            if (!TextUtils.isEmpty(s)) {
                return jsonObject.getInt(field);
            }
        } catch (JSONException e) {
            Timber.e("JSONUtil", e.getMessage());
        }
        return -1;
    }
}
