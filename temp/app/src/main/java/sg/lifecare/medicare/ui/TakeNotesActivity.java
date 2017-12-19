package sg.lifecare.medicare.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import io.realm.Realm;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.PrimaryKeyFactory;
import sg.lifecare.medicare.database.model.Note;
import sg.lifecare.medicare.database.model.Photo;
import sg.lifecare.medicare.database.model.Symptom;
import sg.lifecare.medicare.database.sync.NetworkChangeReceiver;
import sg.lifecare.medicare.database.sync.SyncHandler;
import sg.lifecare.medicare.database.sync.UnsyncedData;
import sg.lifecare.medicare.ui.squarecamera.ImageParameters;
import sg.lifecare.medicare.ui.squarecamera.ImageUtility;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.utils.HtmlUtil;
import sg.lifecare.medicare.utils.LifeCareHandler;
import timber.log.Timber;

import static sg.lifecare.medicare.R.id.photo;

/**
 * Created by wanping on 21/9/16.
 */
public class TakeNotesActivity extends Activity {
    public static final int NOTE = 0;
    public static final int REQ_PHOTO = 1;
    public static final int REQ_SYMPTOM = 2;
    public static final String TYPE = "Type";

    private ImageView ivFood;
    private TextView tvSymptoms, tvFoodTitle, tvSymptomsTitle;
    private LinearLayout foodIntakeBtn, symptomBtn, foodSymptomBtnLayout;
    private ProgressDialog dialog;
    private Button addBtn;
    private EditText etNotes;

    private String entityId;
    private String symptomsArr = "";
    private ProgressDialog progress;
    private int activityReqCode = -1;
    private int attachmentType = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_notes);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.uploading_data));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);

        SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
        entityId = sh.getString("entity_id", "");

        foodSymptomBtnLayout = (LinearLayout) findViewById(R.id.food_symptom_buttons_layout);
        foodIntakeBtn = (LinearLayout) findViewById(R.id.food_intake_button);
        symptomBtn = (LinearLayout) findViewById(R.id.symptom_button);
        ivFood = (ImageView) findViewById(R.id.image_food);
        tvSymptomsTitle = (TextView) findViewById(R.id.text_symptoms_title);
        tvSymptoms = (TextView) findViewById(R.id.text_symptoms);
        tvFoodTitle = (TextView) findViewById(R.id.text_food_intake_title);
        etNotes = (EditText) findViewById(R.id.edit_notes);

        etNotes.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                ((ScrollView)findViewById(R.id.scroll_view)).requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });
        foodIntakeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(TakeNotesActivity.this, CameraActivity.class);
                startActivityForResult(i,REQ_PHOTO);
            }
        });

        symptomBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(TakeNotesActivity.this, SignsAndSymptomsActivity.class);
                if(!symptomsArr.isEmpty()){
                    i.putExtra("selected_symptoms",symptomsArr);
                }
                startActivityForResult(i,REQ_SYMPTOM);
            }
        });

        tvSymptoms.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(TakeNotesActivity.this, SignsAndSymptomsActivity.class);
                if(!symptomsArr.isEmpty()){
                    i.putExtra("selected_symptoms",symptomsArr);
                }
                startActivityForResult(i,REQ_SYMPTOM);
            }
        });

        addBtn = (Button) findViewById(R.id.button_add);
        addBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(attachmentType==REQ_PHOTO){
                    processPicture();
                }else if(attachmentType==REQ_SYMPTOM){
                    addSymptoms();
                }else if(attachmentType==NOTE){
                    //TODO: add Notes
                    addNote();
                }
            }
        });
        CustomToolbar mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle("Notes");
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.setRightButtonImage(R.drawable.ic_toolbar_tick);
        mToolbar.hideRightButton();
        mToolbar.hideSecondRightButton();
        mToolbar.setListener(mToolbarListener);

        if(getIntent()!=null){
            activityReqCode = getIntent().getIntExtra(TYPE,-1);
            if(activityReqCode == REQ_SYMPTOM){
                symptomBtn.performClick();
            }else if(activityReqCode == REQ_PHOTO){
                foodIntakeBtn.performClick();
            }
        }
    }

    private void setAttachmentType(int type){
        if(!(type == REQ_PHOTO || type == REQ_SYMPTOM)){
            return;
        }

        attachmentType = type;

        if(type == REQ_PHOTO){
            foodIntakeBtn.setClickable(true);
            symptomBtn.setClickable(false);
            tvFoodTitle.setTextColor(getResources().getColor(R.color.black));
            tvSymptomsTitle.setTextColor(getResources().getColor(R.color.gray));
            ivFood.setVisibility(View.VISIBLE);
            etNotes.setLines(3);
            etNotes.setHint("Write about your food...");
        }else{
            foodIntakeBtn.setClickable(false);
            symptomBtn.setClickable(true);
            tvFoodTitle.setTextColor(getResources().getColor(R.color.gray));
            tvSymptomsTitle.setTextColor(getResources().getColor(R.color.black));
            tvSymptoms.setVisibility(View.VISIBLE);
            etNotes.setLines(3);
            etNotes.setHint("Write about your symptoms...");
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            setAttachmentType(requestCode);
            if (requestCode == REQ_PHOTO) {
                String path = intent.getStringExtra("photo_uri_path");
                // Uri uri = Uri.parse(path);
                int rotation = intent.getIntExtra("rotation",0);
                ImageParameters imageParameters = intent.getExtras().getParcelable("image_parameter");
                if (imageParameters == null) {
                    return;
                }
                imageParameters.mIsPortrait =
                        getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

                Picasso.with(TakeNotesActivity.this).load("file:///"+path).into(ivFood);


            }else if(requestCode == REQ_SYMPTOM){
                symptomsArr = intent.getStringExtra("symptoms");
                Timber.d("Symptoms retrieved = " + symptomsArr);
                if(symptomsArr!=null && !symptomsArr.isEmpty()) {
                    String[] items = symptomsArr.split(",");

                    String[] symptomNames = getResources().getStringArray(R.array.symptoms_names);
                    //ArrayList<Integer> symptomId = new ArrayList<>();

                    tvSymptoms.setText("Selected Symptoms:\n\n");

                    for (int i = 0; i < items.length; i++) {
                        int foo = Integer.parseInt(items[i]) - 1;
                       tvSymptoms.append(symptomNames[foo]+"\n");
                    }
                }
                //TODO: display symptoms on this page
            }
            foodSymptomBtnLayout.setVisibility(View.GONE);
        }else if(resultCode == RESULT_CANCELED){
            if(activityReqCode != -1){
                finish();
            }
        }
    }

    private void addSymptoms(){
        Symptom symptom = new Symptom();
        symptom.setId(PrimaryKeyFactory.getInstance().nextKey(Symptom.class));
        symptom.setSymptoms(symptomsArr);
        symptom.setRemark(etNotes.getText().toString());
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
            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    private class UploadSymptomsToServer extends AsyncTask<Void, Void, Void>
    {
        private Symptom symptom;
        private boolean success;

        public UploadSymptomsToServer(Symptom symptom)
        {
            this.symptom = symptom;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(progress!=null) {
                progress.show();
            }
        }

        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {
            try {
                JSONObject data = new JSONObject();

                //Types format = 1,3,5,...
                String extraData = "Types:" + symptomsArr
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
                data.put("WriteToSocket",false);
                //data.put("SmartDeviceId", serial);

                String jsonString = data.toString();

                Timber.w("Uploading Symptoms Data To Server : " + jsonString);

                Request request = new Request.Builder()
                        .url("https://www.lifecare.sg/mlifecare/event/addEvent")
                        .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                        .build();

                okhttp3.Response response = LifeCareHandler.getInstance().okclient.newCall(request).execute();

                JSONObject json = new JSONObject(response.body().string());
                success = !json.getBoolean("Error"); //Error = true if contains error
                //success = response.isSuccessful();
                if(json.has("Data")){
                    JSONObject resData = json.getJSONObject("Data");
                    if(resData.has("_id")){
                        String id = resData.getString("_id");
                        Realm mRealm = Realm.getDefaultInstance();
                        mRealm.beginTransaction();
                        symptom.setEventId(id);
                        mRealm.copyToRealmOrUpdate(symptom);
                        mRealm.commitTransaction();
                        mRealm.close();

                        Timber.w("symptom event id after update = " + symptom.getEventId());
                    }
                }
                Timber.d("Successful upload: " + success + " , " + json.getString("ErrorDesc"));
            } catch(JSONException ex) {
                Timber.e(ex.getMessage(), ex);
            } catch (IOException e) {
                e.printStackTrace();
            } catch(Exception e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void res){

            if(success) {
                Toast.makeText(TakeNotesActivity.this,"Successfully uploaded!",Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(TakeNotesActivity.this,"Connection error! Added to syncing list!",Toast.LENGTH_SHORT).show();
                UnsyncedData.symptomsList.add(symptom.getDate());
            }

            if(progress!=null && progress.isShowing()){
                progress.dismiss();
            }
            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    private void rotatePicture(int rotation, byte[] data, ImageView photoImageView) {
        Bitmap bitmap = ImageUtility.decodeSampledBitmapFromByte(TakeNotesActivity.this, data);

        //        Timber.d( "original bitmap width " + bitmap.getWidth() + " height " + bitmap.getHeight());
        if (rotation != 0) {
            Bitmap oldBitmap = bitmap;

            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);

            bitmap = Bitmap.createBitmap(
                    oldBitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false
            );

            oldBitmap.recycle();
        }

        //Square the bitmap
        int length = bitmap.getWidth() < bitmap.getHeight() ? bitmap.getWidth() : bitmap.getHeight();

        Bitmap squareBitmap = Bitmap.createBitmap(bitmap, 0, 0, length, length);

        Bitmap scaledBitmap = squareBitmap;

        Timber.d("Squared bitmap size = " + bitmap.getWidth() + ", " + bitmap.getHeight());
        //Scale it down if the length is > 640
        if(length > 640) {
            scaledBitmap = Bitmap.createScaledBitmap(squareBitmap, 640, 640, false);
            squareBitmap.recycle(); //TODO CHECK
        }

        photoImageView.setImageBitmap(scaledBitmap);

        bitmap.recycle();
    }

    private void processPicture(){

        Bitmap bitmap = ((BitmapDrawable) ivFood.getDrawable()).getBitmap();

                /*double ratio = bitmap.getWidth()/150;
                Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, 150, (int)(bitmap.getHeight()/ratio) , false);
*/

        Timber.d("Saved bitmap width = " + bitmap.getWidth()+" , " + bitmap.getHeight());
        Uri photoUri = ImageUtility.savePicture(TakeNotesActivity.this, bitmap,entityId);
        //  Uri previewUri = ImageUtility.savePicture(getActivity(), previewBitmap);
        if(photoUri!=null)
        {
            Timber.d("Saved bitmap width = " + bitmap.getWidth()+" , " + bitmap.getHeight());

            String remarks = etNotes.getText().toString();
            remarks = HtmlUtil.encodeString(remarks);

            Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

            final Photo photo = new Photo();
            photo.setImage(photoUri.getPath());
            photo.setDate(Calendar.getInstance().getTime());
            photo.setRemark(remarks);
            photo.setEntityId(entityId);
            PatientData.getInstance().addPhoto(realm, photo);

            if(NetworkChangeReceiver.isInternetAvailable(this)) {
                new UploadImageToLocalServer(photo).execute();
            }
            else{
                SyncHandler.registerNetworkBroadcast();
                UnsyncedData.foodList.add(photo.getDate());
                setResult(RESULT_OK);
                finish();
            }

            realm.close();
        }
    }

    /*private void uploadImageToLocalServer(final Photo photo){
      *//*  dialog = new ProgressDialog(TakeNotesActivity.this);
        dialog.setMessage(getString(R.string.uploading_data));
        dialog.show();*//*
        progress.show();

        Timber.d("Beginning upload...");
        new UploadImageToLocalServer(photo).execute();
    }*/

    //cloudinary
    private class UploadImageToLocalServer extends AsyncTask<Void, Void, Void> {
        Photo photo;
        boolean success;
        JSONObject json, dataObj;

        public UploadImageToLocalServer(Photo photo) {
            this.photo = photo;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress.show();
        }

        @Override
        protected Void doInBackground(Void... paramVarArgs) {
            Timber.d("Beginning upload...2222");
            File file = new File(photo.getImage());
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file",file.getName(),RequestBody.create(MediaType.parse("image/jpeg"),file))
                    .build();
            String response = LifeCareHandler.getInstance().uploadPictureToLocalServer(requestBody,entityId);
            Timber.d("Response = " + response);

            try {
                json = new JSONObject(response);
                success = (json.getInt("error_code") == 0);
                Timber.d("Success = " + success);
                dataObj = json.getJSONObject("Data");

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void paramVoid) {
            if(success){
                new UploadImageToCloudTask(dataObj,photo).execute();
            }else{
                Timber.e("Unsuccessful upload to Local Server! Mission terminated");
                if(progress != null){
                    progress.cancel();
                    finish();
                }
            }
        }
    }

    //cloudinary
    private class UploadImageToCloudTask extends AsyncTask<Void, Void, Void>
    {
        JSONObject dataObj;
        Photo photo;
        boolean success;

        public UploadImageToCloudTask(JSONObject dataObj, Photo photo){
            this.dataObj = dataObj;
            this.photo = photo;
        }

        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {
            //TODO: complete the uploading task
            SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
            String entityId = sh.getString("entity_id", "");
            String res = "";
            String mediaId = "";

            if(dataObj!=null)
                res = LifeCareHandler.getInstance().uploadObjectPicture(dataObj,entityId);

            if(res!=null){
                Timber.d("RESPONSE FROM UPLOAD IMG = " + res);
                try {
                    JSONObject object = new JSONObject(res);
                    Timber.d("converted response to obj");

                    if(object.has("Data")){
                        JSONObject data = (JSONObject) object.get("Data");

                        if(data.has("_id")){
                            mediaId = data.getString("_id");
                        }
                    }

                    if(!mediaId.equals("")){
                        //upload food intake
                        uploadFood(mediaId);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        private void uploadFood(String mediaId) throws JSONException, IOException, Exception {
            JSONObject data = new JSONObject();

            String extraData = "Carbohydrates:" + photo.getValue()
                    + "&Remarks:"+ photo.getRemark();

            Timber.d("Extra Data= " + extraData);

            SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT_UPLOAD);

            //String serial = device.getDeviceId();
            data.put("EntityId", photo.getEntityId());
            data.put("ExtraData", extraData);
            data.put("EventTypeName", "Food Intake");
            data.put("EventTypeId", "20061"); //food intake
            data.put("CreateDate", sdf.format(photo.getDate()));

            JSONArray mediaIdArr = new JSONArray();
            mediaIdArr.put(mediaId);

            data.put("MediaIds",mediaIdArr);
            data.put("WriteToSocket",false);

            String jsonString = data.toString();

            Timber.w("Uploading Food Data To Server : " + jsonString);

            Request request = new Request.Builder()
                    .url("https://www.lifecare.sg/mlifecare/event/addEvent")
                    .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                    .build();

            okhttp3.Response response = LifeCareHandler.getInstance().okclient.newCall(request).execute();

            JSONObject json = new JSONObject(response.body().string());
            success = !json.getBoolean("Error"); //Error = true if contains error
            //success = response.isSuccessful();

            if(json.has("Data")){
                JSONObject resData = json.getJSONObject("Data");
                if(resData.has("_id")){
                    String id = resData.getString("_id");
                    Realm mRealm = Realm.getDefaultInstance();
                    mRealm.beginTransaction();
                    photo.setEventId(id);
                    mRealm.copyToRealmOrUpdate(photo);
                    mRealm.commitTransaction();
                    mRealm.close();

                    Timber.w("photo event id after update = " + photo.getEventId());
                }
            }

            Timber.d("Successful food upload: " + success + " , " + json.getString("ErrorDesc"));
        }

        @Override
        protected void onPostExecute(Void paramVoid)
        {
            progress.cancel();

            if(success) {
                Toast.makeText(TakeNotesActivity.this,"Successfully uploaded!",Toast.LENGTH_SHORT).show();
            }
            else {
                UnsyncedData.foodList.add(photo.getDate());
                Toast.makeText(TakeNotesActivity.this,"Connection error! Added to syncing list!",Toast.LENGTH_SHORT).show();
            }

            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    private void addNote(){
        if(etNotes.getText().toString().isEmpty()){
            new AlertDialog.Builder(TakeNotesActivity.this)
                    .setMessage("Field cannot be left empty!")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .setCancelable(true)
                    .show();
            return;
        }
        Note note = new Note();
        note.setNote(HtmlUtil.encodeString(etNotes.getText().toString()));
        note.setDate(Calendar.getInstance().getTime());
        note.setEntityId(entityId);

        Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
        PatientData.getInstance().addNote(realm,note);
        realm.close();

        setResult(Activity.RESULT_OK);
        finish();

        Timber.d("Added Notes!");
    }

    private CustomToolbar.OnToolbarClickListener mToolbarListener = new CustomToolbar.OnToolbarClickListener() {
        @Override
        public void leftButtonClick() {
            onBackPressed();
        }

        @Override public void rightButtonClick() {

            if(attachmentType==REQ_PHOTO){
                processPicture();
            }
            else if(attachmentType==REQ_SYMPTOM){
                addSymptoms();
            }else if(attachmentType==NOTE){
                //TODO: add Notes
                addNote();
            }

        }

        @Override public void secondRightButtonClick() {

        }
    };
}
