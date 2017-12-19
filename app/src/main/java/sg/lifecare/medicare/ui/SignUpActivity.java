package sg.lifecare.medicare.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import sg.lifecare.medicare.BuildConfig;
import sg.lifecare.medicare.MediCareApplication;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.sync.NetworkChangeReceiver;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.utils.EnterpriseHandler;
import sg.lifecare.medicare.utils.LifeCareHandler;
import timber.log.Timber;

/**
 * Created by wanping on 23/8/16.
 */
public class SignUpActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_signup);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        CustomToolbar mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_activity_signup);
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.hideRightButton();
        mToolbar.hideSecondRightButton();
        mToolbar.setListener(mToolbarListener);

        final EditText etFirstName = (EditText)findViewById(R.id.et_first_name);
        final EditText etLastName = (EditText)findViewById(R.id.et_last_name);
        final EditText etEmail = (EditText)findViewById(R.id.et_email);
        final EditText etPass = (EditText)findViewById(R.id.et_password);
        final EditText etConfirmPass = (EditText)findViewById(R.id.et_confirm_password);
        final CheckBox checkBox = (CheckBox)findViewById(R.id.check_box);

        Button signupBtn = (Button)findViewById(R.id.signup_button);
        signupBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!NetworkChangeReceiver.isInternetAvailable(SignUpActivity.this)){
                    new AlertDialog.Builder(SignUpActivity.this,R.style.dialog)
                            .setTitle(R.string.error_title_no_internet)
                            .setMessage(R.string.error_msg_no_internet)
                            .setNegativeButton(R.string.dialog_ok,null)
                            .show();
                    return;
                }
                CharSequence error = "Please fill in all fields.";

                String firstName = etFirstName.getText().toString().trim();
                String lastName = etLastName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String pass = etPass.getText().toString().trim();
                String confirmPass = etConfirmPass.getText().toString().trim();

                if(firstName.isEmpty())
                    etFirstName.setError(error);
                else if(lastName.isEmpty())
                    etLastName.setError(error);
                else if(email.isEmpty())
                    etEmail.setError(error);
                else if(pass.isEmpty())
                    etPass.setError(error);
                else if(confirmPass.isEmpty())
                    etConfirmPass.setError(error);
                else if(!pass.equals(confirmPass))
                    etConfirmPass.setError("Password does not match with confirmation password.");
                else if(!checkBox.isChecked())
                    checkBox.setError("You need to agree with our terms and conditions to sign up.");
                else{
                    new SignUpTask(email,firstName,lastName,pass).execute();
                }
            }
        });
    }

    private class SignUpTask extends AsyncTask<Void, Void, Integer>
    {
        final int SIGNUP_SUCCESS = 0;
        final int SIGNUP_FAIL = 1;

        String email, firstName, lastName, pass;
        String errorMessage = "Unknown Error";

        public SignUpTask(String email, String firstName, String lastName, String pass)
        {
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.pass = pass;
        }

        @Override
        protected Integer doInBackground(Void... paramVarArgs)
        {
            String result = "";
            if(BuildConfig.PRODUCT_FLAVOR == MediCareApplication.GENERAL) {
                result = LifeCareHandler.getInstance().signup(email, firstName, lastName, pass);
            }else if(BuildConfig.PRODUCT_FLAVOR == MediCareApplication.MEDISAFE){
                result = LifeCareHandler.getInstance().signup(email, firstName, lastName, pass, EnterpriseHandler.TERUMO_ID);
            }else {
                result = LifeCareHandler.getInstance().signup(email, firstName, lastName, pass);
            }
            Timber.e("Sign Up API response = " + result);

            try {
                JSONObject json = new JSONObject(result);
                if(json.has("Error")){
                    if(json.getBoolean("Error")){
                        Timber.e("Error signing up!");

                        if(json.getString("ErrorDesc").equalsIgnoreCase("User exists"))
                            errorMessage = "User already exists! Please login!";
                        else
                            errorMessage = json.getString("ErrorDesc");

                        return SIGNUP_FAIL;
                    }
                    else {
                        return SIGNUP_SUCCESS;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return SIGNUP_FAIL;
        }

        @Override
        protected void onPostExecute(Integer status){
            switch (status){
                case SIGNUP_SUCCESS:
                    signupSuccess();
                    break;

                case SIGNUP_FAIL:
                    signupFail(errorMessage);
                    break;
            }
        }

    }

    /*private void doSignUp(final String email, final String firstName, final String lastName, final String pass){
        new Thread(){
            public void run(){
                String result = LifeCareHandler.getInstance().signup(email,firstName,lastName,pass);
                Timber.e("Sign up API imageUrl = " + result);
                try {
                    JSONObject json = new JSONObject(result);
                    if(json.has("Error")){
                        if(json.getBoolean("Error")){
                            Timber.e("Error signing up!");

                            if(json.getString("ErrorDesc").equalsIgnoreCase("User exists")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                       signupFail("User already Exists!");
                                    }
                                });
                            }
                        }
                        else {
                            Timber.e("Successfully signed up!");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                   signupSuccess();
                                }
                            });
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }*/

    private void signupFail(String message){
        new AlertDialog.Builder(SignUpActivity.this)
                .setTitle("Sign Up Failed")
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .show();
    }

    private void signupSuccess(){
        new AlertDialog.Builder(SignUpActivity.this)
                .setTitle("Sign Up Successful")
                .setMessage("You've successfully signed up!\nA verification email has been sent to your inbox.")
                .setCancelable(true)
                .setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        finish();
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .show();
    }

    private CustomToolbar.OnToolbarClickListener mToolbarListener = new CustomToolbar.OnToolbarClickListener() {
        @Override
        public void leftButtonClick() {
            onBackPressed();
        }

        @Override public void rightButtonClick() {

        }

        @Override public void secondRightButtonClick() {

        }
    };
}
