package sg.lifecare.medicare.ui.measuring;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sony.nfc.DeviceInfo;
import com.sony.nfc.NfcDynamicTag;
import com.sony.nfc.NfcTag;
import com.sony.nfc.NfcTagDetector;
import com.sony.nfc.NfcTagListener;
import com.sony.nfc.err.NfcTagException;
import com.sony.nfc.glucosemeter.GlucoseData;
import com.sony.nfc.glucosemeter.GlucoseMeterMsFr201;
import com.sony.nfc.glucosemeter.GlucoseMeterMsFr201Data;
import com.sony.nfc.glucosemeter.GlucoseMeterMsFr201Detector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.utils.HexUtil;
import sg.lifecare.medicare.utils.LifeCareHandler;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.SonyNfc;
import timber.log.Timber;


/**
 * Created by ct on 13/1/16.
 */
public class PreparationReadingActivity extends Activity
{
    private final String TAG = "ReadingPrep";

    //upper bar navigation
    private TextView backTitle, title, nextTitle;
    private ImageView backImage, nextImage;
    private ImageButton backButton, nextButton;

    private RelativeLayout loadingView;
    private ProgressBar loading;

    private RelativeLayout readingView, preparationView;
    private TextView value, dateTaken;
    private Button finishButton, cancelButton;

    private TextView deviceName, deviceDesc;
    private ImageView deviceIcon;

    private MedicalDevice device;

    private SonyNfc mSonyNfc;
    private DeviceInfo mDeviceInfo;
    private GlucoseMeterMsFr201Data[] mGlucoseData;

    private SharedPreferences sh;

    private GlucoseMeterMsFr201Data glucoseData;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_reading);

        sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);

        readingView = (RelativeLayout) findViewById(R.id.reading_view);
        preparationView = (RelativeLayout) findViewById(R.id.preparation_view);

        readingView.setVisibility(View.GONE);
        preparationView.setVisibility(View.VISIBLE);

        Intent intent = getIntent();
        if(intent.hasExtra("device"))
        {
            device = (MedicalDevice) intent.getSerializableExtra("device");
        }

        loadingView = (RelativeLayout) findViewById(R.id.loading_view);
        loading = (ProgressBar) findViewById(R.id.loading);
        /*loading.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.loading_prog_bar),
                android.graphics.PorterDuff.Mode.SRC_IN);*/

        title = (TextView) findViewById(R.id.title);
        title.setText(device.getAssignedName());

        backImage = (ImageView) findViewById(R.id.left_button);

        //backButton = (ImageButton) findViewById(R.id.menu_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        deviceIcon = (ImageView) findViewById(R.id.device_img);
        deviceName = (TextView) findViewById(R.id.device_name);
        deviceDesc = (TextView) findViewById(R.id.device_desc);

        value = (TextView) findViewById(R.id.reading_value);
        dateTaken = (TextView) findViewById(R.id.taken_value);

        finishButton = (Button) findViewById(R.id.finish_button);
        finishButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                uploadDataToServer task = new uploadDataToServer(glucoseData);
                task.execute();
            }
        });

        cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });

        GlucoseMeterMsFr201Detector glucose =
                new GlucoseMeterMsFr201Detector(GlucoseMeterMsFr201Detector.READ_DATA);
        NfcTagDetector[] detectors = new NfcTagDetector[]{glucose};

        try {
            mSonyNfc = new SonyNfc(PreparationReadingActivity.this, detectors, mNfcTagListener);
        } catch (NfcTagException ex) {
            Timber.e(ex.getMessage());
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        mSonyNfc.startDetect();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        mSonyNfc.startDetect();
    }


    @Override
    public void onNewIntent(Intent intent)
    {
        resolveIntent(intent);
    }

    public void resolveIntent(Intent intent)
    {
        String s = intent.getAction();

        Timber.d("resolveIntent: action=" + s);

        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(s) ||
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(s)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            if (tag != null && mSonyNfc != null)
            {
                mSonyNfc.startReadThread(tag);
            }
        }
    }

    private void resolveTag(NfcTag tag) {
        try {
            if (DeviceInfo.class.isAssignableFrom(tag.getClass())) {
                mDeviceInfo = (DeviceInfo) tag;

                Timber.d("resolveTag: id=" + mDeviceInfo.getDeviceId() +
                        "  name=" + mDeviceInfo.getDeviceName());
            }

            if (GlucoseMeterMsFr201.class.isAssignableFrom(tag.getClass())) {
                Timber.d("resolveTag: start reading");
                Thread.sleep(300);
                mGlucoseData = (GlucoseMeterMsFr201Data[]) ((GlucoseMeterMsFr201)tag).getGlucoseData();

                //MedicalDeviceMainActivity parent = (MedicalDeviceMainActivity)getActivity();
                if (mGlucoseData != null)
                {
                    Timber.d("resolveTag: finish reading (" + mGlucoseData.length + ")");
                    for (int i = 0; i < mGlucoseData.length; i++) {
                        Timber.d("TagDetector (glucose meter): index=" + i +
                                " value=" + mGlucoseData[i].getBloodGlucose());
                    }

                    if (mGlucoseData.length > 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                readingView.setVisibility(View.VISIBLE);
                                preparationView.setVisibility(View.GONE);

                                GlucoseMeterMsFr201Data data = mGlucoseData[mGlucoseData.length - 1];

                                float glucose = data.getBloodGlucose() / 1000;
                                glucose = glucose / 18;

                                value.setText(String.format("%.1f mmol/L", glucose));
                                SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy, HH:mm a");
                                dateTaken.setText(sdf.format(data.getDate()));
                                //dateTaken.setText(DateUtil.convertCalendarToString(data.getDate(), "MM-dd-yyyy, HH:mm a"));

                                glucoseData = data;

                                /*

                                mTimeText.setText(DateUtil.convertCalendarToString(data.getDate(), "HH:mm  MM-dd-yyyy"));
                                mGlucoseText.setText(getBloodGlucose(data.getBloodGlucose(), data.getUnit()));
                                mUnitText.setText("mmol/L");
                                mMealText.setText(getMeal(data.getFlags()));

                                mDoneButton.setVisibility(View.VISIBLE);
                                mDoneButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        MedicalDeviceMainActivity parent = (MedicalDeviceMainActivity) getActivity();
                                        parent.finish();
                                    }
                                });*/
                            }
                        });
                    }
                }
                else
                {
                    /*
                    parent.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMessageText.setText(R.string.reading_failed);
                            mDoneButton.setVisibility(View.VISIBLE);
                            mDoneButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    MedicalDeviceMainActivity parent = (MedicalDeviceMainActivity)getActivity();
                                    parent.showPreviousFragment();
                                }
                            });
                        }
                    });*/

                }
            } else if (NfcDynamicTag.class.isAssignableFrom(tag.getClass())) {
                Timber.e("Unknown device");
                return;
            } else {
                Timber.e("Error. Unresolved");
            }

            mSonyNfc.stopDetect();
        } catch (InterruptedException ex) {
            Timber.e(ex.getMessage());
        }
    }

    private String getBloodGlucose(int glucose, int unit) {
        float value = glucose/1000f;

        if (unit == GlucoseData.UNIT_MG_PER_DL) {
            value = value/18f;
        }

        return String.format("%.1f", value);
    }

    private String getBloodGlucoseUnit(int unit) {
        if (unit == GlucoseData.UNIT_MMOL_PER_L) {
            return "mmol/L";
        }

        return "mg/dL";
    }

    private String getMeal(int meal) {
        Timber.d("getMeal=" + meal);
        if (meal == GlucoseData.MEAL_POSTPRANDIAL) {
            return "After Meal";
        }

        return "Before Meal";
    }

    private NfcTagListener mNfcTagListener = new NfcTagListener() {
        @Override
        public void errorOccurred(NfcTagException e) {
            Timber.e(e.getMessage(), e);
        }

        @Override
        public void started() {
            Timber.d("NfcTagListener: started");
        }

        @Override
        public void stopped() {
            Timber.d("NfcTagListener: stopped");
        }


        @Override
        public void tagDetected(NfcTag nfcTag) {
            Timber.d("NfcTagListener: tagDetected -> " +
                    HexUtil.toHexString(nfcTag.getId()));
        }

        @Override
        public void dataRead(NfcTag nfcTag) {
            Timber.d("NfcTagListener: dataRead");
            resolveTag(nfcTag);
        }
    };

    private class uploadDataToServer extends AsyncTask<Void, Void, Void>
    {
        private GlucoseMeterMsFr201Data data;

        public uploadDataToServer(GlucoseMeterMsFr201Data data)
        {
            this.data = data;
        }

        @Override
        protected void onPreExecute()
        {
            loadingView.setVisibility(View.VISIBLE);
            loading.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {
            float val = data.getBloodGlucose() / 1000;
            //val = val / 18;

            JSONObject data = new JSONObject();
            String jsonString = "";
            String result = "";
            try
            {
                String extraData = "Concentration:" + val
                        + "&SampleLocation:Finger"
                        + "&Type:Capillary Whole Blood"
                        + "&StatusAnnunciation:0"
                        + "&LastReading:true"
                        + "&Meal:After"
                        + "&Unit:mg/dL";

                String gatewayId =  LifeCareHandler.getInstance()
                        .getMyDeviceID(PreparationReadingActivity.this);
                String entityId = sh.getString("entity_id", "");
                String selectedEntityId = sh.getString("selected_profile_entityId", "");
                String serial = entityId + gatewayId + device.getDeviceId();

                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm:ss a");

                data.put("deviceId", LifeCareHandler.getInstance().getMyDeviceID(PreparationReadingActivity.this));
                data.put("smartDeviceId", serial);
                data.put("entityId", selectedEntityId);
                data.put("eventTypeName", "Gluco Update Data");
                data.put("eventTypeId", "20015");
                data.put("nodeName", device.getAssignedName());
                data.put("nodeId", serial);
                data.put("zone", "56398aa0e4b00e308ce460ec");
                data.put("zoneCode", "OT");
                data.put("extraData", extraData);
                data.put("wConfigCode", "NFC");
                data.put("manufacturerCode", device.getManufacturerCode());
                data.put("productCode", device.getProductCode());
                data.put("productTypeCode", device.getProductTypeCode());
                data.put("productSerialNumber", serial);
                data.put("time", sdf.format(this.data.getDate().getTime()));

                jsonString = data.toString();

                Log.w("TagDetector", jsonString);

                Request request = new Request.Builder()
                    .url("https://eyecare.eyeorcas.com/eyeCare/eyeCareEventDetected.php")
                    .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                    .build();

                Response response = LifeCareHandler.getInstance().okclient.newCall(request).execute();
                if (response.isSuccessful()) {
                    result = response.body().string();
                    Log.d(TAG, "result:\n" + result);
                }

            } catch(JSONException ex) {
                Log.e(TAG, ex.getMessage(), ex);
            } catch (IOException ex) {
                Log.e(TAG, ex.getMessage(), ex);
            }

            /*InputStream is = null;
            try
            {
                HttpClient client = new DefaultHttpClient();
                HttpPost request = new
                        HttpPost("https://eyecare.eyeorcas.com/eyeCare/eyeCareEventDetected.php");
                List<NameValuePair> value = new ArrayList<NameValuePair>();
                value.add(new BasicNameValuePair("json", jsonString));

                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(value);
                request.setEntity(entity);
                HttpResponse response = client.execute(request);
                // System.out.println("after sending :" + request.toString());
                HttpEntity entity1 = response.getEntity();
                is = entity1.getContent();
            }
            catch (Exception e)
            {
                Log.e(TAG, e.getMessage());
            }

            try
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null)
                {
                    sb.append(line + "\n");
                }
                is.close();
                Log.d(TAG, "Returning value of doInBackground :" + sb.toString());
                result = sb.toString();
            }
            catch (Exception e)
            {
                Log.e(TAG, "Error converting result " + e.toString());
            }*/

            return null;
        }

        @Override
        protected void onPostExecute(Void res)
        {
            loadingView.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
            finish();
        }
    }
}