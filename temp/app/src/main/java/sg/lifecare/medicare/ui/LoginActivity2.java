package sg.lifecare.medicare.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import sg.lifecare.medicare.BuildConfig;
import sg.lifecare.medicare.MediCareApplication;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.model.LifecareAssisted;
import sg.lifecare.medicare.database.model.LifecareSharedPreference;
import sg.lifecare.medicare.database.sync.NetworkChangeReceiver;
import sg.lifecare.medicare.utils.EnterpriseHandler;
import sg.lifecare.medicare.utils.LifeCareHandler;
import sg.lifecare.medicare.utils.LinearLayoutThatDetectsSoftKeyboard;
import sg.lifecare.medicare.utils.LinearLayoutThatDetectsSoftKeyboard.OnSoftKeyboardListener;
import timber.log.Timber;


public class LoginActivity2 extends Activity
{
    public String TAG = "LoginActivity2";

    private SharedPreferences sh;
    private SharedPreferences.Editor editor;

    private Context context = this;

    private Button loginButton, signupButton;
    private RelativeLayout editFieldView;
    private EditText userField, passField;
    private ProgressBar loading;
    private TextView forgotPassword, signupText;

    private LinearLayout buttonLayout;
    private String mGCMToken;

    private LifecareSharedPreference mLifecareSharedPref;

    private static final String GCM_SENDER_ID = "1076112719492";

    public String getGCMToken() {
        if (TextUtils.isEmpty(mGCMToken)) {
            InstanceID instanceID = InstanceID.getInstance(context);

            try {
                mGCMToken = instanceID.getToken(GCM_SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                Log.d(TAG, "mGCMToken: " + mGCMToken);

                editor.putString("gcm_token", mGCMToken);
                editor.apply();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }

        return mGCMToken;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mLifecareSharedPref = LifecareSharedPreference.getInstance();

        if (getIntent().hasExtra("EXIT"))
        {
            if(getIntent().getBooleanExtra("EXIT", false)) {
                finish();
            }
        }

        sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
        editor = sh.edit();

        loading = (ProgressBar) findViewById(R.id.login_loading);
        loading.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(android.R.color.white),
                android.graphics.PorterDuff.Mode.SRC_IN
        );
        buttonLayout = (LinearLayout) findViewById(R.id.button_layout);
        /*
        ProgressBar loadingCenter = (ProgressBar) findViewById(R.id.login_loading2);
        loadingCenter.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(android.R.color.black),
                android.graphics.PorterDuff.Mode.SRC_IN
        );*/
        editFieldView = (RelativeLayout) findViewById(R.id.login_field_view);
        userField = (EditText) findViewById(R.id.username_field);
        passField = (EditText) findViewById(R.id.password_field);
        loginButton = (Button) findViewById(R.id.login_button);
        signupButton = (Button) findViewById(R.id.signup_button);
        forgotPassword = (TextView) findViewById(R.id.forget_password_text);
        signupText = (TextView) findViewById(R.id.signup_text);

        userField.getBackground().mutate().setColorFilter(getResources().getColor(R.color.colorPrimaryGreenLight), PorterDuff.Mode.SRC_ATOP);
        passField.getBackground().mutate().setColorFilter(getResources().getColor(R.color.colorPrimaryGreenLight), PorterDuff.Mode.SRC_ATOP);

        passField.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager)
                            getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                    loginButton.performClick();
                    return true;
                }
                return false;
            }
        });

        showLoadingView();
        ((LinearLayoutThatDetectsSoftKeyboard)findViewById(R.id.main_layout)).setOnSoftKeyboardListener(new OnSoftKeyboardListener() {
            @Override
            public void onShown() {
                //signupButton.setVisibility(View.GONE);
                findViewById(R.id.lifecare_mini_logo_layout).setVisibility(View.GONE);
            }

            @Override
            public void onHidden() {
                //signupButton.setVisibility(View.VISIBLE);
                findViewById(R.id.lifecare_mini_logo_layout).setVisibility(View.VISIBLE);
            }
        });

        Spannable span = Spannable.Factory.getInstance().newSpannable("Forgot Password?");
        span.setSpan(new ClickableSpan()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(LoginActivity2.this, ForgotPassActivity.class);
                startActivity(intent);
                v.invalidate();
            }
        }, 0, forgotPassword.getText().toString().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // All the rest will have the same spannable.
        ClickableSpan cs = new ClickableSpan()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(LoginActivity2.this, ForgotPassActivity.class);
                startActivity(intent);
                v.invalidate();
            } };

        // set the "test " spannable.
        span.setSpan(cs, 0, forgotPassword.getText().toString().length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        // set the " span" spannable
        span.setSpan(cs, 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.white)),
                0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        forgotPassword.setText(span);
        forgotPassword.setMovementMethod(LinkMovementMethod.getInstance());

        signupText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(LoginActivity2.this,SignUpActivity.class);
                startActivity(i);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                doLogin();
            }
        });

        showLoadingView();

        String tokenExpiry = LifeCareHandler.getExpiry();

        if(!TextUtils.isEmpty(tokenExpiry))
        {
            boolean isTokenValid = false;
            try
            {
                Calendar expiry = Calendar.getInstance();
                Calendar current = Calendar.getInstance();

                SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
                expiry.setTime(sdf.parse(tokenExpiry));

                isTokenValid = expiry.after(current);
                Log.w(TAG, "isTokenValid :" + isTokenValid);
            }
            catch(ParseException e)
            {
                e.printStackTrace();
            }

            if(isTokenValid){
                Timber.d("Is token valid");
                String entityId = sh.getString("entity_id","");

                if(entityId.isEmpty()){
                    exitLoadingView();
                    return;
                }
                if(sh.getBoolean("is_caregiver"+entityId,false)) {
                    if (NetworkChangeReceiver.isInternetAvailable(this)) {
                        new GetAssistedTask(entityId).execute();
                    } else {
                        Log.v(TAG,"Login error");
                        //Give Alert Notification
                        new AlertDialog.Builder(context, R.style.dialog)
                                .setTitle(R.string.login_fail)
                                .setMessage(R.string.login_fail_msg)
                                .setNegativeButton(R.string.dialog_ok, null)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                        /*String cachedResult = sh.getString("assisted_json_array" + entityId, "");
                        if (!cachedResult.isEmpty()) {
                            try {
                                Timber.d("offline mode");
                                JSONArray result = new JSONArray(cachedResult);
                                ArrayList<LifecareAssisted> assisteds = LifecareAssisted.parse(result);
                                if (assisteds != null && assisteds.size() > 0) {
                                    mLifecareSharedPref.setLifecareAssisteds(assisteds);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        Intent intent1 = new Intent(LoginActivity2.this, DashboardActivity.class);
                        startActivity(intent1);
                        finish();*/
                    }
                }else{
                    Intent intent1 = new Intent(LoginActivity2.this, DashboardActivity.class);
                    startActivity(intent1);
                    finish();
                }
            } else{
                exitLoadingView();
            }
        }
        else{
            exitLoadingView();
        }
    }

    private void doLogin(){
        userField.clearFocus();
        passField.clearFocus();
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        if(!NetworkChangeReceiver.isInternetAvailable(context)){
            new AlertDialog.Builder(context, R.style.dialog)
                    .setTitle(R.string.error_title_no_internet)
                    .setMessage(R.string.error_msg_no_internet)
                    .setNegativeButton(R.string.dialog_ok,null)
                    .show();
            return;
        }

        if (!userField.getText().toString().isEmpty()
                && !passField.getText().toString().isEmpty())
        {
            String user = userField.getText().toString();
            user = user.replaceAll("\\s+", "");
            user = user.toLowerCase();
            String pass = passField.getText().toString();

            new RetrieveLoginAuthentication(user, pass,
                    LifeCareHandler.getInstance().getMyDeviceID(getApplicationContext()),
                    null, "A").execute();
        }
        else
        {
            //Give Alert Notification
            new AlertDialog.Builder(context,R.style.dialog)
                    .setTitle(R.string.error_title_incomplete_form)
                    .setMessage(R.string.error_msg_incomplete_form)
                    .setNegativeButton(R.string.dialog_ok, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
        if(resultCode == RESULT_CANCELED)
        {
            editFieldView.setVisibility(View.VISIBLE);
        }
    }

    private class GetAssistedTask extends AsyncTask<Void, Void, Void>{
        String entityId;
        public GetAssistedTask(String entityId){
            this.entityId = entityId;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if(entityId.isEmpty()) return null;

            JSONArray result2 = LifeCareHandler.getInstance()
                    .getCurrentAssisted(entityId);

            if(result2==null || result2.toString().isEmpty()){
                String cachedResult =  sh.getString("assisted_json_array"+entityId, "");
                if(!cachedResult.isEmpty()){
                    try {
                        result2 = new JSONArray(cachedResult);
                        Timber.d("GET RES FROM OLD DATA");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            ArrayList<LifecareAssisted> assisteds = LifecareAssisted.parse(result2);

            if (assisteds != null && assisteds.size() > 0) {
                Timber.d("LoginLifecareTask: assisteds =" + assisteds.size());

                for(int i = 0 ; i < assisteds.size(); i++){
                    Timber.d("Assisted entity id = " + assisteds.get(i).getId());
                }

                editor.putString("assisted_json_array"+entityId, result2.toString());
                editor.putString("assisted_entity_id", assisteds.get(0).getId());
                mLifecareSharedPref.setLifecareAssisteds(assisteds);
            }
            //editor.putBoolean("is_caregiver",false);
            editor.apply();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            Intent intent1 = new Intent(LoginActivity2.this, DashboardActivity.class);
            startActivity(intent1);
            finish();
        }
    }

    //william
    //entity_id : TYHKTSWS-FKVL0F1F-UEWDDU63
    //authentication_id": "IYOI7M8X-0MITA4MZ-LF4T7PFB"
    private class RetrieveLoginAuthentication extends AsyncTask<String, Void, String>
    {
        String deviceID;
        String deviceType;
        String password;
        String tokenID;
        String user;
        String INVALID_ACCOUNT = "INVALID_ACCOUNT";
        String CONNECTION_TIMEOUT = "CONNECTION_TIMEOUT";
        boolean isCaregiver = false;
        boolean success = false;

        public RetrieveLoginAuthentication(String user, String password, String deviceID, String tokenID, String deviceType)
        {
            this.user = user;
            this.password = password;
            this.deviceID = deviceID;
            this.tokenID = tokenID;
            this.deviceType = deviceType;
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            showLoadingView();
        }

        @Override
        protected String doInBackground(String... paramVarArgs)
        {
            if (isCancelled()) {
                return null;
            }

            this.tokenID = mGCMToken = getGCMToken();
            Log.d(TAG,"tokenID = " + this.tokenID);
            String entityId = "";
            HashMap<String, String> map = null;

            boolean failed = false;
            int numOfFailure = 0;
            do {
                try {
                    failed = false;
                    map = LifeCareHandler.getInstance()
                            .login(this.user, this.password,
                                    this.deviceID, this.tokenID, this.deviceType);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    numOfFailure++;
                    failed = true;
                    Timber.e("Attempting to try login again: " + numOfFailure);
                }
            }while(numOfFailure<2 && failed);

            boolean isTerumoUser = false;
            if (map != null)
            {
                success = true;

                isTerumoUser = map.get("enterprise_id").equalsIgnoreCase(EnterpriseHandler.TERUMO_ID);
                if(isTerumoUser) {
                    EnterpriseHandler.setCurrentEnterprise(EnterpriseHandler.TERUMO);
                }else{
                    EnterpriseHandler.setCurrentEnterprise(EnterpriseHandler.GENERAL);
                }
                //MediCareApplication app = (MediCareApplication)getApplication();
                //app.setUser(this.user);

                //300 = normal, 400=caregiver, 500=admin
                String authorizationLvl = map.get("authorization_level");
                entityId = map.get("entity_id");

                if(authorizationLvl.equalsIgnoreCase("400") ||
                        authorizationLvl.equalsIgnoreCase("412")){
                    isCaregiver = true;
                }
                editor.putString("login_name", map.get("name"));
                editor.putString("entity_id", map.get("entity_id"));
                editor.putString("authorization_level", map.get("authorization_level"));
                editor.putBoolean("is_terumo_user",isTerumoUser);
                editor.putBoolean("is_caregiver"+entityId, isCaregiver);
                editor.putInt("current_enterprise",EnterpriseHandler.getCurrentEnterprise());
                editor.apply();

                Timber.d("isCaregiver = " + isCaregiver);
                Timber.d("EntityID = " + map.get("entity_id"));
                Timber.d("isTerumoUser = " + isTerumoUser);
            }
            else
            {
                success = false;
                if(failed) {
                    return CONNECTION_TIMEOUT;
                }
            }

            Timber.d("isGeneral or isMedisafe = " + BuildConfig.PRODUCT_FLAVOR);
            if(success && !isTerumoUser &&
                    (BuildConfig.PRODUCT_FLAVOR == MediCareApplication.MEDISAFE)){
                success = false;
                return INVALID_ACCOUNT;
            }

            return entityId;
        }

        @Override
        protected void onPostExecute(String entityId)
        {
            super.onPostExecute(entityId);

            if(success) {
                if(isCaregiver) {
                    new GetAssistedTask(entityId).execute();
                }else {
                    Intent intent1 = new Intent(LoginActivity2.this, DashboardActivity.class);
                    startActivity(intent1);
                    finish();
                }
            }
            else
            {
                if (!((Activity) context).isFinishing())
                {
                    Log.v(TAG,"Login error");
                    int message = entityId.equals(CONNECTION_TIMEOUT) ?
                            R.string.login_connect_to_server_fail_msg : R.string.login_fail_msg;
                    //Give Alert Notification
                    new AlertDialog.Builder(context, R.style.dialog)
                            .setTitle(R.string.login_fail)
                            .setMessage(message)
                            .setNegativeButton(R.string.dialog_ok, null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }

                if(entityId.equals(INVALID_ACCOUNT)){
                    new Logout().execute();
                }

                exitLoadingView();
            }
        }
    }

    private class Logout extends AsyncTask<Void, Void, Void> {
        boolean success = false;
        SharedPreferences sh;

        @Override
        protected Void doInBackground(Void... paramVarArgs) {
            sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
            String deviceId = LifeCareHandler.getInstance()
                    .getMyDeviceID(context);
            Timber.d("Logout! DEVICE ID = " + deviceId);

            success = LifeCareHandler.getInstance().logout(deviceId);
            LifeCareHandler.getInstance().clearPrefCookies();

            Timber.d("Successfully logged out from server? = " + success);

            return null;
        }
    }

    private void showLoadingView(){
        Timber.d("Showing loading");
        loading.setVisibility(View.VISIBLE);
        buttonLayout.setVisibility(View.INVISIBLE);
        signupText.setVisibility(View.GONE);
        editFieldView.setVisibility(View.GONE);
        forgotPassword.setVisibility(View.GONE);
    }

    private void exitLoadingView(){
        Timber.d("Exiting loading");
        loading.setVisibility(View.GONE);
        //TODO:buttonLayout.setVisibility(View.VISIBLE);
        signupText.setVisibility(View.VISIBLE);
        editFieldView.setVisibility(View.VISIBLE);
        forgotPassword.setVisibility(View.VISIBLE);
    }
}