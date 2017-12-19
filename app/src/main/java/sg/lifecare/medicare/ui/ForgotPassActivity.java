package sg.lifecare.medicare.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import org.json.JSONException;
import org.json.JSONObject;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.sync.NetworkChangeReceiver;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.ui.view.CustomToolbar.OnToolbarClickListener;
import sg.lifecare.medicare.utils.LifeCareHandler;

/**
 * Created by ct on 13/10/15.
 */
public class ForgotPassActivity extends Activity
{
    private ProgressBar progBar;
    private EditText etEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_pass);

        CustomToolbar mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_activity_forgot_pass);
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.hideRightButton();
        mToolbar.hideSecondRightButton();
        mToolbar.setListener(new OnToolbarClickListener() {
            @Override
            public void leftButtonClick() {
                onBackPressed();
            }

            @Override
            public void rightButtonClick() {

            }

            @Override
            public void secondRightButtonClick() {

            }
        });

        etEmail = (EditText) findViewById(R.id.edit_email);
        Button sendButton = (Button)findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
               processForm();
            }
        });

        progBar = (ProgressBar) findViewById(R.id.send_loading);
        progBar.setVisibility(View.GONE);
        progBar.getIndeterminateDrawable().setColorFilter(
                ContextCompat.getColor(this, R.color.progress_bar),
                android.graphics.PorterDuff.Mode.SRC_IN
        );
    }

    private void processForm(){
        if(!NetworkChangeReceiver.isInternetAvailable(ForgotPassActivity.this)){
            new AlertDialog.Builder(ForgotPassActivity.this,R.style.dialog)
                    .setTitle(R.string.error_title_no_internet)
                    .setMessage(R.string.error_msg_no_internet)
                    .setNegativeButton(R.string.dialog_ok,null)
                    .show();
            return;
        }

        String emailText = etEmail.getText().toString().trim();

        if(emailText.isEmpty()) {
            new AlertDialog.Builder(ForgotPassActivity.this,R.style.dialog)
                    .setTitle(R.string.error_title_incomplete_form)
                    .setMessage(R.string.error_msg_incomplete_form)
                    .setNegativeButton(R.string.dialog_ok,null)
                    .show();
        }else{
            new sendTempPassword(emailText).execute();
        }
    }

    private class sendTempPassword extends AsyncTask<Void, Void, Void>
    {
        //{"RowsReturned":null,"Data":null,"Error":true,"ErrorDesc":"No user found!","ErrorCode":500}
        //{"RowsReturned":null,"Data":"Forgot password request sent","Error":false,"ErrorDesc":null,"ErrorCode":null}

        private String email;
        private String errorDesc = "";
        private String successDesc = "";
        private boolean success = false;

        public sendTempPassword(String email)
        {
            this.email = email;
        }

        @Override
        public void onPreExecute()
        {
            progBar.setVisibility(View.VISIBLE);
        }

        @Override
        public Void doInBackground(Void... params)
        {
            JSONObject result = LifeCareHandler.getInstance().sendTempPassword(this.email);
            if(result != null && result.length() > 0)
            {
                try
                {
                    String error = result.getString("Error");
                    if(error.equalsIgnoreCase("true")){
                        errorDesc = result.getString("ErrorDesc");
                        success = false;
                    }
                    else{
                        successDesc = result.getString("Data");
                        success = true;
                    }
                }
                catch(JSONException e)
                {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        public void onPostExecute(Void result)
        {
            progBar.setVisibility(View.GONE);

            int title, message;
            if(!success){
                title = R.string.forgot_pass_fail;
                message = R.string.forgot_pass_fail_message;
            }
            else{
                title = R.string.forgot_pass_success;
                message = R.string.forgot_pass_success_message;
            }
            new AlertDialog.Builder(ForgotPassActivity.this,R.style.dialog)
                    .setTitle(title)
                    .setMessage(message)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setNegativeButton(R.string.dialog_ok, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            if(success){
                                finish();
                            }
                        }
                    })
                    .show();
        }
    }
}
