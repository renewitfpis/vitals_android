package sg.lifecare.medicare.ui.fragment;

import android.Manifest;
import android.Manifest.permission;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import io.realm.Realm;
import okhttp3.Request;
import okhttp3.RequestBody;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.VitalConfigs;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.Photo;
import sg.lifecare.medicare.database.sync.NetworkChangeReceiver;
import sg.lifecare.medicare.database.sync.SyncHandler;
import sg.lifecare.medicare.database.sync.UnsyncedData;
import sg.lifecare.medicare.ui.CameraRuntimePermissionActivity;
import sg.lifecare.medicare.ui.squarecamera.ImageParameters;
import sg.lifecare.medicare.ui.squarecamera.ImageUtility;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.utils.LifeCareHandler;
import timber.log.Timber;

public class EditSavePhotoFragment extends Fragment {

    public static final String TAG = EditSavePhotoFragment.class.getSimpleName();
    public static final String BITMAP_KEY = "bitmap_byte_array";
    public static final String ROTATION_KEY = "rotation";
    public static final String IMAGE_INFO = "image_info";

    private static final int REQUEST_STORAGE = 1;
    private Calendar setDate;
    private ProgressDialog dialog;

    public static Fragment newInstance(byte[] bitmapByteArray, int rotation,
        @NonNull ImageParameters parameters) {
        Fragment fragment = new EditSavePhotoFragment();

        Bundle args = new Bundle();
        args.putByteArray(BITMAP_KEY, bitmapByteArray);
        args.putInt(ROTATION_KEY, rotation);
        args.putParcelable(IMAGE_INFO, parameters);

        fragment.setArguments(args);
        return fragment;
    }

    public EditSavePhotoFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_save_edit_photo, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int rotation = getArguments().getInt(ROTATION_KEY);
        byte[] data = getArguments().getByteArray(BITMAP_KEY);
        ImageParameters imageParameters = getArguments().getParcelable(IMAGE_INFO);

        if (imageParameters == null) {
            return;
        }

        CustomToolbar mToolbar = (CustomToolbar) getActivity().findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_activity_camera_edit);
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.setRightButtonImage(R.drawable.ic_toolbar_tick);
        mToolbar.hideSecondRightButton();
        mToolbar.setListener(mToolbarListener);

        final ScrollView sv = ((ScrollView)view.findViewById(R.id.scroll_view));
        final EditText etDate = (EditText) view.findViewById(R.id.etDate);
        final EditText etTime = (EditText) view.findViewById(R.id.etTime);
        final EditText etRemarks = (EditText) view.findViewById(R.id.edit_remarks);
        etRemarks.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(sv!=null) {
                    sv.requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
        });
        setDate = Calendar.getInstance();
        TimeZone tz = setDate.getTimeZone();

        Log.d("Time zone: ", tz.getDisplayName());

        Log.d("Time : ", new SimpleDateFormat(PatientData.DATE_FULL_FORMAT).format(setDate.getTime()) );
        final Calendar mCurrentDate=Calendar.getInstance();
        SimpleDateFormat basicFormat = new SimpleDateFormat("dd MMM yyyy");
        String todayDate = basicFormat.format(mCurrentDate.getTime());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");
        String currentTime = timeFormat.format(mCurrentDate.getTime());
        etDate.setText(todayDate);

        etDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                int mYear=setDate.get(Calendar.YEAR);
                int mMonth=setDate.get(Calendar.MONTH);
                int mDay=setDate.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog mDatePicker=new DatePickerDialog(getActivity(), new OnDateSetListener() {
                    public void onDateSet(DatePicker datepicker, int selectedyear, int selectedmonth, int selectedday) {
                        setDate.set(selectedyear,selectedmonth,selectedday);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
                        etDate.setText(sdf.format(setDate.getTime()));
                    }
                },mYear,mMonth,mDay);
                mDatePicker.setTitle("Select date");
                mDatePicker.show();
            }
        });

        etTime.setText(currentTime);
        etTime.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                int mHour = mCurrentDate.getTime().getHours();
                int mMinute = mCurrentDate.getTime().getMinutes();
                boolean is24HourView = true;
                TimePickerDialog mTimePicker = new TimePickerDialog(getActivity(), new OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int min) {
                        setDate.set(setDate.get(Calendar.YEAR),setDate.get(Calendar.MONTH),
                                setDate.get(Calendar.DAY_OF_MONTH),hour,min);

                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
                        etTime.setText(sdf.format(setDate.getTime()));
                    }
                }, mHour, mMinute, is24HourView);
                mTimePicker.show();
            }
        });
        final ImageView photoImageView = (ImageView) view.findViewById(R.id.photo);

        imageParameters.mIsPortrait =
            getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        /*final View topView = view.findViewById(R.id.topView);
        if (imageParameters.mIsPortrait) {
            topView.getLayoutParams().height = imageParameters.mCoverHeight;
        } else {
            topView.getLayoutParams().width = imageParameters.mCoverWidth;
        }*/

        rotatePicture(rotation, data, photoImageView);


    }

    private void rotatePicture(int rotation, byte[] data, ImageView photoImageView) {
        Bitmap bitmap = ImageUtility.decodeSampledBitmapFromByte(getActivity(), data);

        //        Log.d(TAG, "original bitmap width " + bitmap.getWidth() + " height " + bitmap.getHeight());
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

        Log.d(TAG,"Squared bitmap size = " + bitmap.getWidth() + ", " + bitmap.getHeight());
        //Scale it down if the length is > 640
        if(length > 640) {
            scaledBitmap = Bitmap.createScaledBitmap(squareBitmap, 640, 640, false);
            squareBitmap.recycle(); //TODO CHECK
        }

        photoImageView.setImageBitmap(scaledBitmap);

        bitmap.recycle();

    }


    public static Bitmap getResizedBitmap(Bitmap image, int newHeight, int newWidth) {
        int width = image.getWidth();
        int height = image.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);
        // recreate the new Bitmap
        Bitmap resizedBitmap = Bitmap.createBitmap(image, 0, 0, width, height,
            matrix, false);
        return resizedBitmap;
    }

    private void savePicture() {
        requestForPermission();
    }

    private void requestForPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(),
                permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            CameraRuntimePermissionActivity.startActivity(EditSavePhotoFragment.this,
                    REQUEST_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        else{
            processPicture();
        }
    }

    private void processPicture(){
        View view = getView();
        if(view==null)
            return;

        ImageView photoImageView = (ImageView) view.findViewById(R.id.photo);

        Bitmap bitmap = ((BitmapDrawable) photoImageView.getDrawable()).getBitmap();

                /*double ratio = bitmap.getWidth()/150;
                Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, 150, (int)(bitmap.getHeight()/ratio) , false);
*/
        SharedPreferences sh = getActivity().getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
        String entityId = sh.getString("entity_id", "");

        Log.d(TAG,"Saved bitmap width = " + bitmap.getWidth()+" , " + bitmap.getHeight());
        Uri photoUri = ImageUtility.savePicture(getActivity(), bitmap,entityId);
        //  Uri previewUri = ImageUtility.savePicture(getActivity(), previewBitmap);
        if(photoUri!=null)
        {
            Log.d(TAG,"Saved bitmap width = " + bitmap.getWidth()+" , " + bitmap.getHeight());

            String carboInput = ((EditText)view.findViewById(R.id.edit_value)).getText().toString();
            String remarks = ((EditText)view.findViewById(R.id.edit_remarks)).getText().toString();
            try {
                remarks = URLEncoder.encode(remarks,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

            final Photo photo = new Photo();
            photo.setImage(photoUri.getPath());
            photo.setDate(setDate.getTime());
            photo.setRemark(remarks);
            photo.setEntityId(entityId);

            if(!carboInput.equals("")) {
                Double carboValue = Double.parseDouble(carboInput);
                photo.setValue(carboValue);
            }

            PatientData.getInstance().addPhoto(realm, photo);

            if(NetworkChangeReceiver.isInternetAvailable(getContext())) {
                uploadImageToLocalServer(photo);
            }
            else{
                SyncHandler.registerNetworkBroadcast();
                UnsyncedData.foodList.add(photo.getDate());
                getActivity().finish();
            }

            realm.close();

        }else{
            Timber.e("ERROR LOCATING PATH TO SAVE PHOTO");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Activity.RESULT_OK != resultCode) return;

        if (REQUEST_STORAGE == requestCode && data != null) {
            final boolean isGranted = data.getBooleanExtra(CameraRuntimePermissionActivity.REQUESTED_PERMISSION, false);
            //final View view = getView();
            if (isGranted) {
                /* new AlertDialog.Builder(getContext())
                        .setCancelable(false)
                        .setMessage("Uploading food to server...")
                        .show();*/
                processPicture();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private CustomToolbar.OnToolbarClickListener mToolbarListener = new CustomToolbar.OnToolbarClickListener() {
        @Override
        public void leftButtonClick() {
            getActivity().onBackPressed();
        }

        @Override public void rightButtonClick() {
            savePicture();
        }

        @Override public void secondRightButtonClick() {

        }
    };

    private void uploadImageToLocalServer(final Photo photo){
        dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getString(R.string.uploading_data));
        dialog.show();

        Future uploading = Ion.with(getActivity())
                .load(VitalConfigs.URL + "mobileUploadMedia")
                .setMultipartFile("file", "image/jpeg", new File(photo.getImage()))
                .asString()
                .withResponse()
                .setCallback(new FutureCallback<Response<String>>(){
                    @Override
                    public void onCompleted(Exception e, Response<String> result) {
                        if (result == null) {
                            Timber.e("Result is null");
                            return;
                        }
                        Timber.d("RAW RESULT = " + result.getResult());
                        try {
                            JSONObject json = new JSONObject(result.getResult());
                            //JSONObject json = new result.getResult();
                            if (json != null) {
                                if (json.has("Data")) {

                                    //String data = result.getResult().get("Data").toString();
                                    JSONObject dataObj = (JSONObject) json.get("Data");
                                    String data = dataObj.toString();
                                    Timber.d("Data = " + data);

                                    if (data != null && !data.equals("")) {
                                        new UploadImageToCloudTask(dataObj, photo).execute();

                                    }
                                }
                            }
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }});
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
            SharedPreferences sh = getActivity().getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
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
                }
            }

            return null;
        }

        private void uploadFood(String mediaId) throws JSONException, IOException {
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
            data.put("WriteToSocket",true);

            String jsonString = data.toString();

            Timber.w("Uploading Food Data To Server : " + jsonString);

            Request request = new Request.Builder()
                    .url(VitalConfigs.URL + "mlifecare/event/addEvent")
                    .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                    .build();

            okhttp3.Response response = LifeCareHandler.getInstance().okclient.newCall(request).execute();

            JSONObject json = new JSONObject(response.body().string());
            success = !json.getBoolean("Error"); //Error = true if contains error
            //success = response.isSuccessful();
            Timber.d("Successful food upload: " + success + " , " + json.getString("ErrorDesc"));
        }

        @Override
        protected void onPostExecute(Void paramVoid)
        {
            dialog.cancel();

            if(success) {
                Toast.makeText(getContext(),"Successfully uploaded!",Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(getContext(),"Failed to upload!",Toast.LENGTH_SHORT).show();
            }

            getActivity().finish();
        }
    }
}

