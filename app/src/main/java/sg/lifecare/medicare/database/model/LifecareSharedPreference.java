package sg.lifecare.medicare.database.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;
import java.util.ArrayList;

import timber.log.Timber;

/**
 * Singleton for shared preferences
 */
public class LifecareSharedPreference {

    private static final String TAG = "LifecareSharedPreference";

    public static final String APPLICATION_ID = "OmuNaMAhSiWahya7OS5tmK3sJnPl3gLy";

    private static final String GCM_SENDER_ID = "1076112719492";

    private static LifecareSharedPreference sInstance;

    private SharedPreferences mPref;
    private SharedPreferences.Editor mEditor;
    private Context mContext;

    private LifecareUser mLifecareUser;
    private ArrayList<LifecareAssisted> mLifecareAssisteds = new ArrayList<>();
    public int mCurrentAssisted = -1;
    private String mGCMToken = "";

    private boolean mIsLogin;

    public static LifecareSharedPreference getInstance() {
        return sInstance;
    }

    public static void init(Context context) {
        if (sInstance == null) {
            sInstance = new LifecareSharedPreference(context);
        }
    }

    private LifecareSharedPreference(Context context) {
        mPref = context.getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
        mEditor = mPref.edit();
        mContext = context;

        mGCMToken = mPref.getString("gcm_token", "");
    }

    /**
     * Get the GCM token. Do not called this function in main thread
     * @return GCM token
     */
    public String getGCMToken() {
        if (TextUtils.isEmpty(mGCMToken)) {
            InstanceID instanceID = InstanceID.getInstance(mContext);

            try {
                mGCMToken = instanceID.getToken(GCM_SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                Timber.d("mGCMToken: " + mGCMToken);

                mEditor.putString("gcm_token", mGCMToken);
                mEditor.commit();
            } catch (IOException e) {
                Timber.e( e.getMessage(), e);
            }
        }

        return mGCMToken;
    }

    /**
     * Clear GCM expired GCM token
     */
    public void resetGCMToken() {
        mGCMToken = "";
        mEditor.putString("gcm_token", mGCMToken);
        mEditor.commit();
    }

    /**
     * Get Singtel OTAC token
     * @return OTAC token
     */
    public String getOTAC() {

        return mPref.getString("otac", "");

        /*if (TextUtils.isEmpty(mOTAC)) {
            CommonLibrary comLib = new CommonLibrary(mContext);
            mOTAC = comLib.getOTAC(APPLICATION_ID);
            mEditor.putString("otac", mOTAC);
            mEditor.commit();
        }

        return mOTAC;*/
    }

    public void setOtac(String otac) {
        mEditor.putString("otac", otac);
        mEditor.commit();
    }

    public String getToken() {
        return mPref.getString("login_token", "");
    }

    public void setToken(String token) {
        mEditor.putString("login_token", token);
        mEditor.commit();
    }

    public String getExpiry() {
        return mPref.getString("login_expiry", "");
    }

    public void setExpiry(String expiry) {
        mEditor.putString("login_expiry", expiry);
        mEditor.commit();
    }

    public LifecareUser getLifecareUser() {
        return mLifecareUser;
    }

    public void setLifecareUser(LifecareUser user) {
        mLifecareUser = user;
    }

    public ArrayList<LifecareAssisted> getLifecareAssisted() {
        return mLifecareAssisteds;
    }

    public void setLifecareAssisteds(ArrayList<LifecareAssisted> assisteds) {
        mLifecareAssisteds = assisteds;
    }

    public LifecareAssisted getCurrentAssisted() {
        if ((mCurrentAssisted != -1) && (mLifecareAssisteds.size() > mCurrentAssisted)) {
            return mLifecareAssisteds.get(mCurrentAssisted);
        }

        return null;
    }

    public boolean setCurrentAssisted(int index) {
        if (mLifecareAssisteds.size() <= index) {
            mCurrentAssisted = -1;
            return false;
        }

        mCurrentAssisted = index;
        return true;
    }

    public LifecareAssisted getAsssistedById(String id) {
        if (!TextUtils.isEmpty(id)) {
            if (mLifecareAssisteds.size() > 0) {
                for (int i = 0; i < mLifecareAssisteds.size(); i++) {
                    if (id.equalsIgnoreCase(mLifecareAssisteds.get(i).getId())) {
                        return mLifecareAssisteds.get(i);
                    }
                }
            }
        }

        return null;
    }

    public boolean setCurrentAssistedById(String id) {
        if (!TextUtils.isEmpty(id)) {
            if (mLifecareAssisteds.size() > 0) {
                for (int i = 0; i < mLifecareAssisteds.size(); i++) {
                    if (id.equalsIgnoreCase(mLifecareAssisteds.get(i).getId())) {
                        mCurrentAssisted = i;
                        return true;
                    }
                }
             }

        }

        mCurrentAssisted = -1;

        return false;
    }

    public boolean isLogin() {
        return mIsLogin;
    }

    public void setLogin(boolean login) {
        mIsLogin = login;

        if (!login) {
            setOtac("");
            setExpiry("");
            setToken("");
            mLifecareUser = null;
            mCurrentAssisted = -1;
        }
    }
}
