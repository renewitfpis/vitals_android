package sg.lifecare.medicare.database.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import timber.log.Timber;

/**
 * Lifecare Phone
 *
 * Sample output:
 * {
 *   "_id": "56e64c81abc9cfe80068ed25",
 *   "entity": "GHOUD1FR-N4V0NKBC-3BDBLD2U",
 *   "last_update": "2016-03-14T05:30:41.847Z",
 *   "create_date": "2016-03-14T05:30:41.847Z",
 *   "type": "P",
 *   "code": "SG",
 *   "country_code": "65",
 *   "digits": "97808922",
 *   "phone_digits": "+6597808922"
 }
 */
public class LifecarePhone implements Cloneable {

    private static final String TAG = "LifecarePhone";

    private String mId;
    private String mType;
    private String mCode;
    private String mCountryCode;
    private String mDigits;
    private String mPhoneDigits;

    public static ArrayList<LifecarePhone> parse(JSONArray data) {
        ArrayList<LifecarePhone> lifecarePhones = new ArrayList<>();
        if (data == null || data.length() == 0) {
            return lifecarePhones;
        }

        for (int j = 0; j < data.length(); j++) {
            try {
                JSONObject jPhone = data.getJSONObject(j);

                if (jPhone.has("_id") && jPhone.has("type") && jPhone.has("code")
                        && jPhone.has("country_code") && jPhone.has("digits")
                        && jPhone.has("phone_digits")) {

                    LifecarePhone phone = new LifecarePhone(jPhone.getString("_id"));
                    phone.setType(jPhone.getString("type"));
                    phone.setCode(jPhone.getString("code"));
                    phone.setCountryCode(jPhone.getString("country_code"));
                    phone.setDigits(jPhone.getString("digits"));
                    phone.setPhoneDigits(jPhone.getString("phone_digits"));
                    lifecarePhones.add(phone);
                }
            } catch (JSONException e) {
                Timber.e(e.getMessage(), e);
            }

        }

        return lifecarePhones;
    }

    public LifecarePhone(String id) {
        mId = id;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String getId() {
        return mId;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        mType = type;
    }

    public String getCode() {
        return mCode;
    }

    public void setCode(String code) {
        mCode = code;
    }

    public String getCountryCode() {
        return mCountryCode;
    }

    public void setCountryCode(String code) {
        mCountryCode = code;
        mPhoneDigits = mCountryCode + mDigits;
    }

    public String getDigits() {
        return mDigits;
    }

    public void setDigits(String digits) {
        mDigits = digits;
        mPhoneDigits = mCountryCode + mDigits;
    }

    public String getPhoneDigits() {
        return mPhoneDigits;
    }

    public void setPhoneDigits(String digits) {
        mPhoneDigits = digits;
    }
}
