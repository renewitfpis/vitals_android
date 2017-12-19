package sg.lifecare.medicare.database.model;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import sg.lifecare.medicare.utils.DateUtil;
import sg.lifecare.medicare.utils.JSONUtil;
import timber.log.Timber;

/**
 * Lifecare profile
 */
public class LifecareProfile implements Cloneable {

    private static final String TAG = "LifecareProfile";

    public static LifecareProfile parse(JSONArray data) {
        if (data == null || data.length() == 0) {
            Timber.w("parse: invalid profile");
            return null;
        }

        if (data.length() != 1) {
            Timber.w("parse: invalid profile " + data.length());
            return null;
        }

        try {
            JSONObject jsonObject = data.getJSONObject(0);

            String id = JSONUtil.getString(jsonObject, "_id");

            if (!TextUtils.isEmpty(id)) {
                LifecareProfile profile = new LifecareProfile(id);
                profile.setFirstName(JSONUtil.getString(jsonObject, "first_name"));
                profile.setLastName(JSONUtil.getString(jsonObject, "last_name"));
                profile.setName(JSONUtil.getString(jsonObject, "name"));
                profile.setDateOfBirth(JSONUtil.getString(jsonObject, "date_established"));
                profile.setGender(JSONUtil.getString(jsonObject,"type"));

                if (jsonObject.has("phones")) {
                    profile.setPhones(LifecarePhone.parse(jsonObject.getJSONArray("phones")));
                }

                if (jsonObject.has("addresses")) {
                    profile.setAddress(LifecareAddress.parse(jsonObject.getJSONArray("addresses")));
                }

                return profile;

            }
        } catch (JSONException e) {
            Timber.e(e.getMessage(), e);
        }

        return null;
    }

    private String id;
    private String firtName;
    private String lastName;
    private String name;
    private ArrayList<LifecarePhone> phones = new ArrayList<>();
    private LifecareAddress address;
    private Date dateOfBirth;
    private String gender;

    private LifecareProfile(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public LifecareAddress getAddress() {
        return address;
    }

    public int getAge() {
        if (isValidDateOfBirth()) {
            Calendar now = Calendar.getInstance();
            Calendar then = Calendar.getInstance();
            then.setTime(dateOfBirth);

            if (now.get(Calendar.YEAR) > then.get(Calendar.YEAR)) {
                return now.get(Calendar.YEAR) - then.get(Calendar.YEAR);
            }
        }

        return -1;
    }

    public LifecarePhone getPhone() {
        if (phones.size() > 0) {
            return phones.get(0);
        }

        return null;
    }

    public boolean isValidGender() {
        Timber.d("isValidGender: " + gender);
        if ("M".equalsIgnoreCase(gender) || "F".equalsIgnoreCase(gender)) {
            return true;
        }

        return false;
    }

    public boolean isValidDateOfBirth() {
        return dateOfBirth != null;
    }

    public boolean isFemale() {
        return "F".equalsIgnoreCase(gender);
    }



    public void setFirstName(String name) {
        this.firtName = name;
    }

    public void setLastName(String name) {
        this.lastName = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhones(ArrayList<LifecarePhone> phones) {
        this.phones = phones;
    }

    public void setAddress(LifecareAddress address) {
        this.address = address;
    }

    public void setGender(String gender) {
        if ("M".equalsIgnoreCase(gender) || "F".equalsIgnoreCase(gender)) {
            this.gender = gender;
        }
    }

    public void setDateOfBirth(String dateOfBirth) {
        DateFormat df = new SimpleDateFormat(DateUtil.ISO8601_TIMESTAMP);

        if (TextUtils.isEmpty(dateOfBirth)) {
            return;
        }

        try {
            setDateOfBirth(df.parse(dateOfBirth));
        } catch (ParseException e) {
            Timber.e(e.getMessage(), e);
        }
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
}
