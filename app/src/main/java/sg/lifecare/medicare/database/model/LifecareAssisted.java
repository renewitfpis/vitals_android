package sg.lifecare.medicare.database.model;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import sg.lifecare.medicare.utils.JSONUtil;
import timber.log.Timber;

import static sg.lifecare.medicare.database.PatientData.DATE_DISPLAY_FORMAT;

/**
 * Assisted user information
 */
public class LifecareAssisted implements Cloneable {

    private static final String TAG = "LifecareAssisted";

    public static final String MODULE_SEMAS = "55fe4db2e4b09229ac541b50";
    public static final String MODULE_VS =  "5664e900e4b00d59f0b725e1";
    public static final String MODULE_WMAS= "5664e900e4b00d59f0b725e1";
    public static final String MODULE_HEMS = "55fe4e84e4b09229ac541b52";
    public static final String MODULE_CORE = "5664e85ce4b00d59f0b725d2";

    private String mId;
    private String mName;
    private String mFirstName;
    private String mLastName;
    private String mSipUsername;
    private String mSipId = "";
    private String mSipPrefix;
    private String mImgUrl;
    private String mThirdPartyId;
    private String mAuthenticationString;
    private boolean mApproved;
    private Calendar mLastUpdate;
    private String mType;
    private String mStatus;
    private String mLocationName;
    private boolean mDisabled;
    private ArrayList<LifecareDevice> mDevices = new ArrayList();
    private ArrayList<LifecareModule> mModules = new ArrayList<>();
    private ArrayList<LifecareRelatedEntity> mRelatedEntities = new ArrayList<>();
    private ArrayList<LifecarePhone> mPhones = new ArrayList<>();
    private Calendar mDateOfBirth;

    private boolean mVSM;
    private boolean mSEMAS;
    private boolean mHEMS;
    private boolean mWMAS;

    public static ArrayList<LifecareAssisted> parse(JSONArray data) {

        ArrayList<LifecareAssisted> assisteds = new ArrayList();

        if (data == null || data.length() == 0) {
            return assisteds;
        }

        //Log.d(TAG,"Data json = " + data.toString());


        for (int i = 0; i < data.length(); i++) {
            try {
                JSONObject object = data.getJSONObject(i);

                if (!object.has("_id")) {
                    continue;
                }

                LifecareAssisted assisted = new LifecareAssisted(object.getString("_id"));

                assisted.setLocationName(JSONUtil.getString(object, "location_name"));
                assisted.setStatus(JSONUtil.getString(object, "status"));
                assisted.setType(JSONUtil.getString(object, "type"));
                assisted.setThirdPartyId(JSONUtil.getString(object, "third_party_id"));
                assisted.setFirstName(JSONUtil.getString(object, "first_name"));
                assisted.setLastName(JSONUtil.getString(object, "last_name"));
                assisted.setAuthenticationString(object.getString("authentication_string_lower"));
                assisted.setApproved(JSONUtil.getBoolean(object, "approved"));
                assisted.setLastUpdate(JSONUtil.getString(object, "last_update"));
                assisted.setName(JSONUtil.getString(object, "name"));
                assisted.setDisabled(JSONUtil.getBoolean(object, "disabled"));

                if (object.has("devices")) {
                    JSONArray jDevices = object.getJSONArray("devices");

                    for (int j = 0; j < jDevices.length(); j++) {
                        JSONObject jDevice = jDevices.getJSONObject(j);

                        if (jDevice.has("_id") && jDevice.has("type") && jDevice.has("name")) {
                            String id = jDevice.getString("_id");
                            String type = jDevice.getString("type");
                            String name = jDevice.getString("name");

                            LifecareDevice device = new LifecareDevice(id);
                            device.setType(type);
                            device.setName(name);
                            assisted.addDevice(device);
                        }

                    }
                }

                if (object.has("phones")) {
                    JSONArray jPhones = object.getJSONArray("phones");

                    for (int j = 0; j < jPhones.length(); j++) {
                        JSONObject jPhone = jPhones.getJSONObject(j);

                        if (jPhone.has("_id") && jPhone.has("type") && jPhone.has("code")
                            && jPhone.has("country_code") && jPhone.has("digits")
                            && jPhone.has("phone_digits")) {

                            LifecarePhone phone = new LifecarePhone(jPhone.getString("_id"));
                            phone.setType(jPhone.getString("type"));
                            phone.setCode(jPhone.getString("code"));
                            phone.setCountryCode(jPhone.getString("country_code"));
                            phone.setDigits(jPhone.getString("digits"));
                            phone.setPhoneDigits(jPhone.getString("phone_digits"));
                            assisted.addPhone(phone);
                        }

                    }
                }

                if (object.has("modules")) {
                    JSONArray jModules = object.getJSONArray("modules");

                    for (int j = 0; j < jModules.length(); j++) {
                        JSONObject jModule = jModules.getJSONObject(j);

                        if (jModule.has("_id") && jModule.has("code") && jModule.has("name")) {
                            String id = jModule.getString("_id");
                            String code = jModule.getString("code");
                            String name = jModule.getString("name");

                            LifecareModule module = new LifecareModule(id, code, name);
                            assisted.addModule(module);
                        }

                    }
                }

                if (object.has("related_entities")) {
                    JSONArray jRelatedEntities = object.getJSONArray("related_entities");

                    for (int j = 0; j < jRelatedEntities.length(); j++) {
                        JSONObject jRelatedEntity = jRelatedEntities.getJSONObject(j);

                        String id = JSONUtil.getString(jRelatedEntity, "_id");

                        if (!TextUtils.isEmpty(id)) {
                            LifecareRelatedEntity relatedEntity =
                                    new LifecareRelatedEntity(id, JSONUtil.getString(jRelatedEntity, "type"));
                            relatedEntity.setType2(JSONUtil.getInt(jRelatedEntity, "type2"));
                            relatedEntity.setType3(JSONUtil.getInt(jRelatedEntity, "type3"));
                            assisted.addRelatedEntity(relatedEntity);
                        }
                    }
                }

                if (object.has("medias")){
                    JSONArray jMedia = object.getJSONArray("medias");

                    if(jMedia.length()>0) {
                        JSONObject medias = (JSONObject) jMedia.get(0);
                        if (medias.has("img_url3")) {
                            assisted.setProfileImgUrl(medias.getString("img_url3"));
                        }
                    }

                }

                if (object.has("sips")){
                    JSONArray jSips = object.getJSONArray("sips");

                    if(jSips.length()>0) {
                        JSONObject jSipsObj =  (JSONObject) jSips.get(0);

                        if (jSipsObj.has("_id") && jSipsObj.has("username")) {
                            String id = jSipsObj.getString("_id");
                            String username = jSipsObj.getString("username");

                            assisted.setSipId(id);
                            assisted.setSipUsername(username);
                        }
                    }
                }

                if(object.has("enterprise")){
                    JSONObject jEnterprise = object.getJSONObject("enterprise");
                    if(jEnterprise.has("prefix")) {
                        String prefix = jEnterprise.getString("prefix");
                        assisted.setSipPrefix(prefix);
                    }
                }

                assisteds.add(assisted);

            }  catch (JSONException e) {
                Timber.e(e.getMessage(), e);
            }
        }

        return assisteds;
    }

    private LifecareAssisted(String id) {
        mId = id;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void setName(String name) {
        mName = name;
    }

    public void setFirstName(String name) {
        mFirstName = name;
    }

    public void setLastName(String name) {
        mLastName = name;
    }

    public void setProfileImgUrl(String url) {
        mImgUrl = url;
    }

    public void setSipId(String id) {
        mSipId = id;
    }

    public void setSipUsername(String username ) {
        mSipUsername = username;
    }

    public void setSipPrefix(String prefix) {
        mSipPrefix = prefix;
    }

    private void setThirdPartyId(String id) {
        mThirdPartyId = id;
    }

    private void setAuthenticationString(String s) {
        mAuthenticationString = s;
    }

    public void setApproved(boolean approved) {
        mApproved = approved;
    }

    public Calendar getLastUpdate() {
        return mLastUpdate;
    }

    public String getLastUpdateString() {
        return new SimpleDateFormat(DATE_DISPLAY_FORMAT, Locale.ENGLISH).format(mLastUpdate.getTime());
    }

    public void setLastUpdate(String time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(sdf.parse(time));
            mLastUpdate = cal;
        } catch(ParseException e) {
            e.printStackTrace();
            Timber.e(e.getMessage(), e);
        }
    }

    public String getLocationName() {
        return mLocationName;
    }

    public void setLocationName(String location) {
        mLocationName = location;
    }

    public String getStatusString() {

        switch(mStatus) {
            case "A" :
                return "Available";

            case "I" :
                 return "Idle";

            case "L" :
                return "Likely Away";

            case "W" :
                return "Away";

            default :
                 return "";
        }
    }

    public void setStatus(String status) {
        mStatus = status;
    }

    private void setType(String type) {
        mType = type;
    }

    public boolean isFemale() {
        return "F".equalsIgnoreCase(mType);
    }

    public boolean isMale() {
        return "M".equalsIgnoreCase(mType);
    }

    public void setMale() {
        mType = "M";
    }

    public void setFemale() {
        mType = "F";
    }

    private void setDisabled(boolean disabled) {
        mDisabled = disabled;
    }

    private void addDevice(LifecareDevice device) {
        mDevices.add(device);
    }

    private void addModule(LifecareModule module) {
        if ("SEMAS".equals(module.getCode())) {
            mSEMAS = true;
        } else if ("VSM".equals(module.getCode())) {
            mVSM = true;
        } else if ("HEMS".equals(module.getCode())) {
            mHEMS = true;
        } else if ("WMAS".equals(module.getCode())) {
            mWMAS = true;
        }

        mModules.add(module);
    }

    private void addRelatedEntity(LifecareRelatedEntity entity) {
        mRelatedEntities.add(entity);
    }

    public String getId() {
        return mId;
    }

    public boolean isApproved() {
        return mApproved;
    }

    public boolean isPrimaryElderly() {
        if (mRelatedEntities.size() > 0) {
            if ("1".equals(mRelatedEntities.get(0).getType2())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Assume first related entity
     * @return kinsip relation
     */
    public String getRelatedEntityId() {
        if (mRelatedEntities.size() > 0) {
            return mRelatedEntities.get(0).getId();
        }

        return "";
    }

    /**
     * Assume first related entity
     * @return kinsip relation
     */
    public String getKinshipRelation() {
        if (mRelatedEntities.size() > 0) {
            String relation = mRelatedEntities.get(0).getKinshipRelation();
            if (TextUtils.isEmpty(relation)) {
                relation = mName;
            }

            return relation;
        }

        return "";
    }

    /**
     * Assume first related entity
     */
    public String getKinship() {
        if (mRelatedEntities.size() > 0) {
            return mRelatedEntities.get(0).getKinship();
        }

        return "";
    }

    /**
     * Assume first related entity
     */
    public void setKinship(String type) {
        if (mRelatedEntities.size() > 0) {
            mRelatedEntities.get(0).setKinship(type);
        }

    }

    public String getDeviceId() {

        if (mDevices.size() > 0) {
            return mDevices.get(0).getId();
        }

        return "";
    }

    public String getFirstName() {
        return mFirstName;
    }

    public String getLastName() {
        return mLastName;
    }

    public String getProfileImgUrl() {
        return mImgUrl;
    }

    public String getName() {
        return mName;
    }

    public boolean hasVSM() {
        return mVSM;
    }

    public boolean hasSEMAS() {
        return mSEMAS;
    }

    public boolean hasHEMS() {
        return mHEMS;
    }

    public boolean hasWMAS() {
        return mWMAS;
    }

    public void addPhone(LifecarePhone phone) {
        mPhones.add(phone);
    }

    /**
     * Always return the first one
     * @return phone details
     */
    public LifecarePhone getPhone() {
        if (mPhones.size() > 0) {
            return mPhones.get(0);
        }

        return null;
    }

    public String getFirstPhoneNumber() {
        if (mPhones.size() > 0) {
            return mPhones.get(0).getPhoneDigits();
        }

        return "";
    }

    public Calendar getDateOfBirth() {
        return mDateOfBirth;
    }

    public void setDateOfBirth(String dob) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        Calendar dateFormat = Calendar.getInstance();

        try {
            dateFormat.setTime(sdf.parse(dob));
            mDateOfBirth = dateFormat;
        } catch(ParseException e) {
            Timber.e(e.getMessage(), e);
        }
    }

    public boolean isPrimary() {
        if (mRelatedEntities.size() > 0) {
            return mRelatedEntities.get(0).isPrimary();
        }

        return false;
    }

    public String getSipPrefix(){
        return mSipPrefix;
    }

    public String getSipUsername(){
        return mSipUsername;
    }

    public String getSipId(){
        return mSipId;
    }

    public boolean hasSip(){
        return (!mSipId.isEmpty());
    }
}
