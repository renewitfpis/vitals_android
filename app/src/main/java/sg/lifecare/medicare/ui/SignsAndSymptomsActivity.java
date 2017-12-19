package sg.lifecare.medicare.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;

import io.realm.Realm;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.VitalConfigs;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.Symptom;
import sg.lifecare.medicare.database.model.User;
import sg.lifecare.medicare.database.sync.UnsyncedData;
import sg.lifecare.medicare.ui.adapter.SymptomsAdapter;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.utils.LifeCareHandler;
import timber.log.Timber;

/**
 * Sign and symptoms activity
 */
public class SignsAndSymptomsActivity extends AppCompatActivity {

    private TypedArray maleIcons, femaleIcons;
    private EditText etRemarks;
    private SymptomsAdapter adapter;
    //private SignsAndSymptomsAdapter adapter;
    public boolean[] isSelected = new boolean[17];
    private String selectedSymptoms = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.activity_signandsymptoms);
        setContentView(R.layout.activity_signs_symptoms);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        Timber.tag("SignsAndSymptoms");

        String symptoms = "";
        if(getIntent().hasExtra("selected_symptoms")){
            symptoms = getIntent().getStringExtra("selected_symptoms");
        }

        for (int i = 0; i < isSelected.length; i++) {
            isSelected[i] = false;
        }
        if(!symptoms.isEmpty()){
            String[] data = symptoms.split(",");
            for(int i = 0; i < data.length; i++){
                int foo = Integer.parseInt(data[i]) - 1;
                isSelected[foo] = true;
                //isSelected[i] = data[i].equalsIgnoreCase("t");
            }
        }

        SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
        String entityId = sh.getString("entity_id", "");

        maleIcons = getResources().obtainTypedArray(R.array.sign_and_symptoms_male_icons);
        femaleIcons = getResources().obtainTypedArray(R.array.sign_and_symptoms_female_icons);
        String[] symptomNames = getResources().getStringArray(R.array.symptoms_names);

        Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
        User mUser = realm.where(User.class).equalTo("entityId",entityId).findFirst();
        if ((mUser != null) && (mUser.isFemale())) {
            adapter = new SymptomsAdapter(this,symptomNames,femaleIcons, isSelected);
            //adapter = new SignsAndSymptomsAdapter(this, symptoms, femaleIcons);
        } else {

            adapter = new SymptomsAdapter(this,symptomNames,maleIcons,isSelected);
           // adapter = new SignsAndSymptomsAdapter(this, symptoms, maleIcons);
        }
        realm.close();

        CustomToolbar mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_activity_sign_and_symptoms);
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.setRightButtonImage(R.drawable.ic_toolbar_tick);
        mToolbar.hideSecondRightButton();
        mToolbar.setListener(mToolbarListener);

       /* SignsAndSymptomsGridView grid = (SignsAndSymptomsGridView) findViewById(R.id.gridView);
        grid.setAdapter(adapter);
        grid.setExpanded(true);*/
        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(adapter);

        etRemarks = (EditText) findViewById(R.id.edit_remarks);
    }

    private void prepareSymptomsResult(){
        if(adapter.globalInc==0) {
            new AlertDialog.Builder(SignsAndSymptomsActivity.this)
                    .setTitle(getResources().getString(R.string.error_title_no_selection))
                    .setMessage(getResources().getString(R.string.error_msg_no_selection))
                    .setCancelable(true)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create().show();
            return;
        }

        adapter.globalInc = 0;

        String symptoms = "";
        selectedSymptoms = "";
        String remarks = etRemarks.getText().toString().trim();
        //String colon = getResources().getString(R.string.special_colon);
        //String and = getResources().getString(R.string.special_and);

        Timber.d("Remarks bef: " + remarks);
        try {
            remarks = URLEncoder.encode(remarks, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Timber.d("Remarks aft: " + remarks);

        if ((isSelected != null) && (isSelected.length > 0)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < isSelected.length; i++) {
                if (isSelected[i]) {
                    sb.append("t");
                    selectedSymptoms = selectedSymptoms + (i+1) + ",";
                } else {
                    sb.append("f");
                }

                if (i != (isSelected.length - 1)) {
                    sb.append(",");
                }
            }
            symptoms = sb.toString();

            Timber.d("isSelected : " + symptoms);
        }else{

            Timber.d("isSelected = null");
        }

        if(!symptoms.isEmpty()){
            Intent intent = new Intent();
            intent.putExtra("symptoms",selectedSymptoms);
            setResult(Activity.RESULT_OK,intent);
            finish();
        }
    }

    /*private void submitSymptoms(){
        if(adapter.globalInc==0) {
            new AlertDialog.Builder(SignsAndSymptomsActivity.this)
                    .setTitle(getResources().getString(R.string.error_title_no_selection))
                    .setMessage(getResources().getString(R.string.error_msg_no_selection))
                    .setCancelable(true)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create().show();
            return;
        }

        adapter.globalInc = 0;

        String symptoms = "";
        selectedSymptoms = "";
        String remarks = etRemarks.getText().toString().trim();
        //String colon = getResources().getString(R.string.special_colon);
        //String and = getResources().getString(R.string.special_and);

        Timber.d("Remarks bef: " + remarks);
        try {
            remarks = URLEncoder.encode(remarks, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Timber.d("Remarks aft: " + remarks);

        if ((isSelected != null) && (isSelected.length > 0)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < isSelected.length; i++) {
                if (isSelected[i]) {
                    sb.append("t");
                    selectedSymptoms = selectedSymptoms + (i+1) + ",";
                } else {
                    sb.append("f");
                }

                if (i != (isSelected.length - 1)) {
                    sb.append(",");
                }
            }
            symptoms = sb.toString();
        }

        Timber.d("symptoms: " + symptoms);
        Timber.d("remark  : " + remarks);

        Symptom symptom = new Symptom();
        symptom.setId(PrimaryKeyFactory.getInstance().nextKey(Symptom.class));
        symptom.setSymptoms(selectedSymptoms);
        symptom.setRemark(remarks);
        symptom.setDate(Calendar.getInstance().getTime());
        symptom.setEntityId(entityId);

        Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
        PatientData.getInstance().addSymptoms(realm,symptom);
        realm.close();

        if(NetworkChangeReceiver.isInternetAvailable(this)) {
            new UploadSymptomsToServer(symptom).execute();
        }
        else{
            SyncHandler.registerNetworkBroadcast();
            UnsyncedData.symptomsList.add(symptom.getDate());
        }
        finish();
    }*/

    private class UploadSymptomsToServer extends AsyncTask<Void, Void, Void>
    {
        private Symptom symptom;
        private boolean success;

        public UploadSymptomsToServer(Symptom symptom)
        {
            this.symptom = symptom;
        }

        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {
            try {
                JSONObject data = new JSONObject();

                //Types format = 1,3,5,...
                String extraData = "Types:" + selectedSymptoms
                        + "&Remarks:"+ symptom.getRemark();

                Timber.d("Extra Data= " + extraData);

                //MedicalDevice device = MedicalDevice.findModel(Model.TERUMO_MEDISAFE_FIT);

                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT_UPLOAD);

                //String serial = device.getDeviceId();

                //data.put("DeviceId", LifeCareHandler.getInstance().getMyDeviceID(SignsAndSymptomsActivity.this));
                data.put("EntityId", symptom.getEntityId());
                data.put("ExtraData", extraData);
                data.put("EventTypeName", "Symptoms");
                data.put("EventTypeId", "20063"); //symptom
                data.put("Zone", "56398aa0e4b00e308ce460ec");
                data.put("ZoneCode", "OT");
                data.put("CreateDate", sdf.format(symptom.getDate()));
                data.put("WriteToSocket",true);
                //data.put("SmartDeviceId", serial);

                String jsonString = data.toString();

                Timber.w("Uploading Symptoms Data To Server : " + jsonString);

                Request request = new Request.Builder()
                        .url(VitalConfigs.URL + "mlifecare/event/addEvent")
                        .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                        .build();

                Response response = LifeCareHandler.getInstance().okclient.newCall(request).execute();

                JSONObject json = new JSONObject(response.body().string());
                success = !json.getBoolean("Error"); //Error = true if contains error
                //success = response.isSuccessful();
                Timber.d("Successful upload: " + success + " , " + json.getString("ErrorDesc"));
            } catch(JSONException ex) {
                Timber.e(ex.getMessage(), ex);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void res){
            if(!success) {
                UnsyncedData.symptomsList.add(symptom.getDate());
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        maleIcons.recycle();
        femaleIcons.recycle();
    }

    private CustomToolbar.OnToolbarClickListener mToolbarListener = new CustomToolbar.OnToolbarClickListener() {
        @Override
        public void leftButtonClick() {
            onBackPressed();
        }

        @Override public void rightButtonClick() {
            prepareSymptomsResult();
            //submitSymptoms();
        }

        @Override public void secondRightButtonClick() {
        }
    };
}



