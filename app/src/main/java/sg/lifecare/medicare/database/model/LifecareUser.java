package sg.lifecare.medicare.database.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import sg.lifecare.medicare.utils.JSONUtil;
import timber.log.Timber;

/**
 * Lifecare login user information
 */
public class LifecareUser implements Cloneable {

    private static final String TAG = "LifecareUser";

    private String mId;
    private String mThirdPartyId;
    private String mLocationName;
    private boolean mDisabled;
    private String mLastUpdate;
    private String mType;
    private boolean mApproved;
    private String mStatus;
    private String mName;
    private String mLastName;
    private String mFirstName;
    private String mAuthenticationString;
    private ArrayList<LifecareDevice> mDevices;
    private ArrayList<LifecareRelatedEntity> mEntities;
    private LifecarePhone mPhone;

    private String mAnName;
    private String mDateEstablished;
    private int mAuthorizationLevel;

    private ArrayList<LifecareMedia> mMedias;

    public static LifecareUser parse(JSONArray result) {
        if (result == null || result.length() != 1) {
            return null;
        }

        return new LifecareUser(result);
    }

    private LifecareUser(JSONArray data) {

        Timber.d("data: " + data.length());
        printLogStrint(data.toString());

        try {
            JSONObject object = data.getJSONObject(0);

            mId = object.getString("_id");

            if (object.has("third_party_id")) {
                mThirdPartyId = object.getString("third_party_id");
            }

            if (object.has("location_name")) {
                mLocationName = object.getString("location_name");
            }
            if (object.has("an_name")) {
                mAnName = object.getString("an_name");
            }

            mDisabled = JSONUtil.getBoolean(object, "disabled");

            if (object.has("date_established")) {
                mDateEstablished = object.getString("date_established");
            }

            if (object.has("last_update")) {
                mLastUpdate = object.getString("last_update");
            }

            if (object.has("type")) {
                mType = object.getString("type");
            }

            mApproved = JSONUtil.getBoolean(object, "approved");

            if (object.has("status")) {
                mStatus = object.getString("status");
            }

            if (object.has("name")) {
                mName = object.getString("name");
            }

            if (object.has("last_name")) {
                mLastName = object.getString("last_name");
            }

            if (object.has("first_name")) {
                mFirstName = object.getString("first_name");
            }

            if (object.has("authorization_level")) {
                mAuthorizationLevel = object.getInt("authorization_level");
            }

            mAuthenticationString = object.getString("authentication_string_lower");

            mDevices = new ArrayList<>();
            mEntities = new ArrayList<>();
            mMedias = new ArrayList<>();

            if (object.has("medias")) {
                JSONArray jmedias = object.getJSONArray("medias");
                for (int i = 0; i < jmedias.length(); i++) {
                    JSONObject jmedia = jmedias.getJSONObject(i);

                    if (jmedia.has("_id") && jmedia.has("media_url") && jmedia.has("type")) {
                        LifecareMedia media = new LifecareMedia(jmedia.getString("_id"));
                        media.setUrl(jmedia.getString("media_url"));
                        media.setType(jmedia.getString("type"));
                        mMedias.add(media);
                    }
                }
            }

            /*JSONArray relatedEntities = object.getJSONArray("related_entities");
            if (relatedEntities != null && relatedEntities.length() > 0) {
                for (int i = 0; i < relatedEntities.length(); i++) {
                    JSONObject relatedEntity = relatedEntities.getJSONObject(i);

                    String id = relatedEntity.getString("_id");
                    LogUtil.d(TAG, "id: " + id);

                    LifecareRelatedEntity lifecareRelatedEntity = new LifecareRelatedEntity(id);
                    JSONObject re = relatedEntity.getJSONObject("related_entity");

                    String reid = re.getString("_id");
                    String name = re.getString("name");
                    String authentication = re.getString("authentication_string");

                    LogUtil.d(TAG, "reid: " + reid + ", name=" + name + ", authentication=" + authentication);

                    lifecareRelatedEntity.setRelatedEntity(reid, name, authentication);

                    JSONArray jsonArray = re.getJSONArray("modules");
                    for (int j = 0; j < jsonArray.length(); j++) {
                        String moduleid = jsonArray.getString(j);
                        lifecareRelatedEntity.addModule(moduleid);
                        LogUtil.d(TAG, "moduleId=" + moduleid);
                    }

                }
            }*/

        } catch (JSONException e) {
            Timber.e(e.getMessage(), e);
        }
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String getFirstName() {
        return mFirstName;
    }

    public void setFirstName(String firstName) {
        mFirstName = firstName;
    }

    public String getLastName() {
        return mLastName;
    }

    public void setLastName(String lastName) {
        mLastName = lastName;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getId() {
        return mId;
    }

    public String getThirdPartyId() {
        return mThirdPartyId;
    }

    public LifecareMedia getMedia() {
        if(mMedias.size() > 0) {
            return mMedias.get(0);
        }
        return null;
    }

    public LifecarePhone getPhone() {
        return mPhone;
    }


    private void printLogStrint(String s) {
        int maxLogSize = 1000;
        for(int i = 0; i <= s.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = (i+1) * maxLogSize;
            end = end > s.length() ? s.length() : end;
            Timber.d(s.substring(start, end));
        }
    }
}
