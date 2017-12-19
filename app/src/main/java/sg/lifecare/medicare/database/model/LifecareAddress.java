package sg.lifecare.medicare.database.model;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import sg.lifecare.medicare.utils.JSONUtil;
import timber.log.Timber;

/**
 * Created by sweelai on 25/7/16.
 */
public class LifecareAddress implements Cloneable {

    private static final String TAG = "LifecareAddress";


    public static LifecareAddress parse(JSONArray data) {
        if (data == null || data.length() == 0) {
            return null;
        }

        if (data.length() != 1) {
            Timber.w("parse: invalid address " + data.length());
            return null;
        }

        try {
            JSONObject jsonObject = data.getJSONObject(0);

            String id = JSONUtil.getString(jsonObject, "_id");

            LifecareAddress address = new LifecareAddress(id);
            address.setCountry(JSONUtil.getString(jsonObject, "country"));

            return address;
        } catch (JSONException e) {
            Timber.e( e.getMessage(), e);
        }

        return null;
    }

    private String id;
    private String country;

    public LifecareAddress(String id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String contry) {
        this.country = contry;
    }
}
