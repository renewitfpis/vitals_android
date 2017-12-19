package sg.lifecare.medicare.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmList;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.VitalConfigs;
import sg.lifecare.medicare.ble.BleException;
import sg.lifecare.medicare.ble.DeviceMeter;
import sg.lifecare.medicare.ble.aandd.AAndDMeterListener;
import sg.lifecare.medicare.ble.jumper.JumperOximeter;
import sg.lifecare.medicare.ble.jumper.JumperOximeterListener;
import sg.lifecare.medicare.ble.profile.AbstractProfile;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.SpO2;
import sg.lifecare.medicare.database.model.SpO2Set;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.database.sync.UnsyncedData;
import sg.lifecare.medicare.object.LocalMeasurementDevicesHandler;
import sg.lifecare.medicare.ui.adapter.WeightReadingAdapter;
import sg.lifecare.medicare.ui.pairing.DevicePairingMenuActivity;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.ui.view.PairingIndicatorView;
import sg.lifecare.medicare.utils.BleUtil;
import sg.lifecare.medicare.utils.LifeCareHandler;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import timber.log.Timber;

public class SpO2ReadingActivity extends AppCompatActivity {

    private static final String TAG = "SpO2ReadingActivity";
    private static final int REQUEST_ENABLE_BT = 123;
    private Realm mRealm;
    private DeviceMeter deviceMeter = null;
    private AlertDialog mBleEnableDialog;
    private boolean isDevicePaired = false;
    private String entityId;

    private PairingIndicatorView pairingIndicator;
    private Button addDeviceBtn;
    WeightReadingAdapter adapter;

    private TextView tvSpo2Value;
    private TextView tvPulseValue;
    private TextView tvDescription;

    private ImageView startEndBtn;
    private boolean started = false;
    private boolean hasData = false;
    private MedicalDevice device;
    private Model model;
    private RealmList<SpO2> spo2List;
    private ProgressBar progBar;
    private CountDownTimer cdt;
    private boolean isCdtRunning = false;
    private TextView tvReading;
    RelativeLayout readingLayout;
    RelativeLayout resultLayout;
    private boolean resultRetrieved = false;
    double dTotal = 0;
    private ProgressDialog progress;

    private String pairedDeviceMacAdd;
    private MedicalDevice pairedDevice;
    private CustomToolbar mToolbar;

    private JumperOximeter mJumperOximeter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spo2_reading);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.uploading_data));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_activity_spo2_reading);
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.setRightButtonImage(R.drawable.ic_toolbar_tick);
        mToolbar.hideRightButton();
        mToolbar.hideSecondRightButton();
        mToolbar.setListener(mToolbarListener);

        entityId = getSharedPreferences("lifecare_pref",MODE_PRIVATE).getString("entity_id","");

        readingLayout = (RelativeLayout) findViewById(R.id.spo2_reading_view);
        resultLayout = (RelativeLayout) findViewById(R.id.spo2_result_view);

        final Button tryAgainBtn = (Button) findViewById(R.id.button_try_again);
        tryAgainBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                readingLayout.setVisibility(View.VISIBLE);
                resultLayout.setVisibility(View.GONE);
                resultRetrieved = false;
                dTotal = 0;
                mToolbar.hideRightButton();

                startDetect();
            }
        });

        tvReading = (TextView) findViewById(R.id.text_reading);
        progBar = (ProgressBar) findViewById(R.id.progress_bar);
        progBar.getIndeterminateDrawable().setColorFilter(
                ContextCompat.getColor(this, R.color.progress_bar),
                android.graphics.PorterDuff.Mode.SRC_IN);
        progBar.setProgress(0);
        int oneMin = 15 * 1000; // 15 secs in milli seconds
        /** CountDownTimer starts with 2 minutes and every onTick is 1 second */
        cdt = new CountDownTimer(oneMin, 100) {
            public void onTick(long millisUntilFinished) {
                dTotal++;
                int total = (int) ((dTotal)/150 * 100);
                progBar.setProgress(total);
                isCdtRunning = true;
            }

            public void onFinish() {
                progBar.setProgress(100);
                isCdtRunning = false;
                Timber.d("FINISH!");
                readingLayout.setVisibility(View.INVISIBLE);
                resultLayout.setVisibility(View.VISIBLE);
                mToolbar.showRightButton();
                resultRetrieved = true;
                dTotal = 0;
                progBar.setProgress(0);

                stopDetect();

                //endReading();
                // DO something when 2 minutes is up
                // TODO: show result

            }
        };

        pairingIndicator = (PairingIndicatorView) findViewById(R.id.pairing_indicator);
        spo2List = new RealmList<>();

        Intent intent = getIntent();
        if(intent.hasExtra("device"))
        {
            device = (MedicalDevice) intent.getSerializableExtra("device");
            pairedDevice = device;
            model = device.getModel();

            if(device != null){
                TextView tvDeviceName = (TextView) findViewById(R.id.text_device_name);
                tvDeviceName.setText(device.getAssignedName());
                tvDeviceName.setVisibility(View.VISIBLE);
                pairingIndicator.setProductImage(device.getImage());
            }else{
                pairingIndicator.setProductImage(R.drawable.ic_berry_oximeter);
            }
        }else{
            pairingIndicator.setProductImage(R.drawable.ic_jumper_jpd500e);
        }

        pairingIndicator.setPairingMode(PairingIndicatorView.READING_BLE);

        if(pairedDevice!=null && pairedDevice.getModel()==Model.NONIN_3230) {
            pairingIndicator.setPairingDescriptionImage(R.drawable.ic_pairing_nonin);
            pairingIndicator.setPairingDescription("Turn on your device by inserting your middle finger.");
        } else if (pairedDevice!=null && pairedDevice.getModel()==Model.JUMPER_JPD500E) {
            pairingIndicator.setPairingDescriptionImage(R.drawable.ic_jumper_jpd500e);
            pairingIndicator.setPairingDescription("Turn on your device by pressing the button.");
        } else{
            pairingIndicator.setPairingDescriptionImage(R.drawable.ic_pairing_berry1);
            pairingIndicator.setPairingDescription("Turn on your device by pressing the button.");
        }
        pairingIndicator.showPairingDescription();

        pairingIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!BleUtil.isBleEnabled()){
                    mBleEnableDialog.show();
                }
            }
        });

        tvDescription = (TextView)  findViewById(R.id.text_description);
        tvSpo2Value   = (TextView)  findViewById(R.id.text_spo2_value);
        tvPulseValue  = (TextView)  findViewById(R.id.text_pulse_value);
        startEndBtn   = (ImageView) findViewById(R.id.start_end_button);
        startEndBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!BleUtil.isBleEnabled()){
                    mBleEnableDialog.show();
                    return;
                }else{
                    handleStartEnd();
                }
            }
        });
        mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
        mBleEnableDialog = BleUtil.enableBleDialogBuilder(this,
                mBleSettingsButtonListener, mBleCancelButtonListener).create();

        addDeviceBtn = (Button) findViewById(R.id.add_device_button);
        addDeviceBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!BleUtil.isBleEnabled()){
                    mBleEnableDialog.show();
                }else {
                    if (!isDevicePaired) {
                        Intent intent = new Intent(SpO2ReadingActivity.this,
                                DevicePairingMenuActivity.class);
                        intent.putExtra("is_spo2",true);
                        startActivity(intent);
                    }
                }
            }
        });

        //Check if device is paired
        //new getConnectedDeviceList().execute();
        //getLocalConnectedDevices();


    }

    private void handleStartEnd(){
        if(!started){
            started = true;
            startDetect();
            startEndBtn.setImageResource(R.drawable.end);
            tvDescription.setText("Connecting to SpO2 device...");
        }else{
            endReading();
        }
    }

    public void endReading(){

        if(hasData) {
            new AlertDialog.Builder(SpO2ReadingActivity.this)
                    .setTitle("Upload Data")
                    .setMessage("Upload current SpO2 data?")
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            started = false;
                            hasData = false;
                            tvSpo2Value.setText("--");
                            tvPulseValue.setText("--");
                            tvDescription.setText("Press start to calculate your SpO2");
                            startEndBtn.setImageResource(R.drawable.start);
                            stopDetect();
                            spo2List.clear();
                        }
                    })
                    .setPositiveButton("Upload", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Timber.d(getString(R.string.uploading_data));
                            started = false;
                            hasData = false;
                            tvSpo2Value.setText("--");
                            tvPulseValue.setText("--");
                            tvDescription.setText("Press start to calculate your SpO2");
                            startEndBtn.setImageResource(R.drawable.start);
                            stopDetect();
                            //TODO: upload data
                            for(int i = 0; i<spo2List.size(); i++){
                                PatientData.getInstance().addSpO2(mRealm,spo2List.get(i));
                                Timber.d("Copying data " + i + " to realm");
                            }

                            Timber.d("Done Copying data");
                            SpO2Set spO2Set = new SpO2Set();
                            spO2Set.setStartDate(spo2List.get(0).getDate());
                            spO2Set.setEndDate(spo2List.get(spo2List.size()-1).getDate());
                            spO2Set.setEntityId(entityId);
                            PatientData.getInstance().addSpO2Set(mRealm,spO2Set);

                            Timber.d("added spo2set data");
                            Timber.d("spo2set data START DATE: " + spo2List.get(0).getDate());
                            Timber.d("spo2set data END DATE: " + spo2List.get(spo2List.size()-1).getDate());
                            Timber.d("spo2set data SIZE: " + spo2List.size());
                            dialog.dismiss();

                            spo2List.clear();
                        }
                    })
                    .show();
        }else{
            stopDetect();
            started = false;
            hasData = false;
            tvSpo2Value.setText("--");
            tvPulseValue.setText("--");
            tvDescription.setText("Press start to calculate your SpO2");
            startEndBtn.setImageResource(R.drawable.start);
            spo2List.clear();
        }
    }

    public void onResume() {
        super.onResume();

        if (mRealm == null)
            mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

        getLocalConnectedDevices();

        //startDetect();
        //if(deviceMeter!=null)
            //deviceMeter.startScanning();
        //new getConnectedDeviceList().execute();


       /* if(!BleUtil.isBleEnabled()) {
            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_BLE_OFF);
            addDeviceBtn.setVisibility(View.VISIBLE);
        }else{
            pairingIndicator.setPairingMode(PairingIndicatorView.READING_BLE);
            pairingIndicator.showPairingDescription();
            addDeviceBtn.setVisibility(View.GONE);
            handleStartEnd();
        }*/
    }

    private DialogInterface.OnClickListener mBleCancelButtonListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            mBleEnableDialog.dismiss();
        }
    };
    private DialogInterface.OnClickListener mBleSettingsButtonListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_ENABLE_BT){
            Log.d(TAG,"BT ONRESULT !");
            if(resultCode==RESULT_OK) {
                //handleStartEnd();
            }
        }
    }

    private void saveData(){
        Timber.d("UPLOAD DATA");
        started = false;
        hasData = false;
        /*tvSpo2Value.setText("--");
        tvPulseValue.setText("--");*/
        tvDescription.setText("Press start to calculate your SpO2");
        startEndBtn.setImageResource(R.drawable.start);
        stopDetect();
        //TODO: upload data
        for(int i = 0; i<spo2List.size(); i++){
            PatientData.getInstance().addSpO2(mRealm,spo2List.get(i));
            Timber.d("Copying data " + i + " to realm");
        }

        Timber.d("Done Copying data");
        SpO2Set spO2Set = new SpO2Set();
        spO2Set.setStartDate(spo2List.get(0).getDate());
        spO2Set.setEndDate(spo2List.get(spo2List.size()-1).getDate());
        spO2Set.setEntityId(entityId);
        PatientData.getInstance().addSpO2Set(mRealm,spO2Set);

        Timber.d("added spo2set data");
        Timber.d("spo2set data START DATE: " + spo2List.get(0).getDate());
        Timber.d("spo2set data END DATE: " + spo2List.get(spo2List.size()-1).getDate());
        Timber.d("spo2set data SIZE: " + spo2List.size());

        //TODO: upload to server
        new uploadSpO2DataToServer(spo2List.get(spo2List.size()-1)).execute();
    }

    private class uploadSpO2DataToServer extends AsyncTask<Void, Void, Void>
    {
        private SpO2 spo2;
        boolean success;
        Date spo2Date;

        public uploadSpO2DataToServer(SpO2 spo2)
        {
            this.spo2 = spo2;
            this.spo2Date = spo2.getDate();
        }

        @Override
        protected void onPreExecute(){
            if(progress!=null){
                progress.show();
            }
        }

        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {
            Log.d(TAG, "do in background!");

            JSONObject data = new JSONObject();
            String jsonString = "";
            String result = "";
            try
            {
                String extraData = "Pulse:" + spo2.getPulseRate()
                        + "&SpO2:" + spo2.getValue()
                        + "&LastReading:" + "true";

                //TODO: change to local measurement list
                String gatewayId =  LifeCareHandler.getInstance().getMyDeviceID(SpO2ReadingActivity.this);
                SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
                String serial = spo2.getEntityId() + "-" + gatewayId + "-" + pairedDevice.getDeviceId().replace(":","");

                Log.d(TAG,"SERIAL = " + serial);
                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT_UPLOAD, Locale.ENGLISH);

                data.put("DeviceId", LifeCareHandler.getInstance().getMyDeviceID(SpO2ReadingActivity.this));
                data.put("EntityId", spo2.getEntityId());
                data.put("ExtraData", extraData);
                data.put("EventTypeName", "SpO2 Update Data");
                data.put("EventTypeId", "20050"); //spo2
                data.put("Zone", "56398aa0e4b00e308ce460ec");
                data.put("ZoneCode", "OT");
                data.put("NodeId", serial);
                data.put("NodeName", pairedDevice.getAssignedName());
                data.put("WConfigCode", "BLE");
                data.put("SystemManufacturerCode", pairedDevice.getManufacturerCode());
                data.put("SystemProductCode", pairedDevice.getProductCode());
                data.put("SystemProductTypeCode", pairedDevice.getProductTypeCode());
                data.put("SmartDeviceId", serial);
                data.put("CreateDate", sdf.format(spo2.getDate()));
                data.put("WriteToSocket",true);

                jsonString = data.toString();

                Log.w(TAG, jsonString);

                Request request = new Request.Builder()
                        .url(VitalConfigs.URL + "mlifecare/event/addEvent")
                        .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                        .build();
                //"https://eyecare.eyeorcas.com/eyeCare/eyeCareEventDetected.php"
                Response response = LifeCareHandler.getInstance().okclient.newCall(request).execute();

                JSONObject json = new JSONObject(response.body().string());
                success = !json.getBoolean("Error"); //Error = true if contains error
                Log.d(TAG, "Successful upload spo2 data: " + success
                        + " , Error desc (if exist): " + json.getString("ErrorDesc"));

            } catch(JSONException ex) {
                Log.e(TAG, ex.getMessage(), ex);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            } catch(Exception e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void res){

            if(!success){
                UnsyncedData.spo2List.add(spo2Date);
            }

            spo2List.clear();
            //if(isLastData){
                if(progress!=null && progress.isShowing()){
                    progress.dismiss();
                }
                String message = "";
                if(success){
                    message = "Successfully updated spo2 data!";
                }else{
                    message = "Problem uploading data! Data have been saved to cache!";
                }
                Toast.makeText(SpO2ReadingActivity.this, message, Toast.LENGTH_LONG).show();

                onDataUpdated();
            //}
        }
    }

    private void onDataUpdated(){
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }

    private void getLocalConnectedDevices(){
        if (!BleUtil.isBleEnabled()) {
            Timber.d("BLE NOY ENABLED");
            //tvDesc.setText(R.string.pairing_bluetooth_not_connected);
            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_BLE_OFF);
            addDeviceBtn.setVisibility(View.VISIBLE);
            return;
        }

        ArrayList<MedicalDevice> list =
                LocalMeasurementDevicesHandler.getInstance().getConnectedMedicalDeviceList();

        for (MedicalDevice device : list) {
            if(device.getModel()==Model.BERRY_BM1000B
                    || device.getModel() == Model.NONIN_3230
                    || device.getModel() == Model.JUMPER_JPD500E){
                isDevicePaired = true;

                if(pairedDevice == null){
                    pairedDevice = device;
                }
            }
        }

        if(isDevicePaired){
           // tvDesc.setText(R.string.device_info_retrieve_data);
            pairingIndicator.setPairingMode(PairingIndicatorView.READING_BLE);
            pairingIndicator.showPairingDescription();
            addDeviceBtn.setVisibility(View.INVISIBLE);
            //autoDataEntryImage.setImageResource(R.drawable.weighscale);
            handleStartEnd();
        }
        else {
           // tvDesc.setText(R.string.device_info_no_pair_device);
            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_NO_DEVICE);
            addDeviceBtn.setVisibility(View.VISIBLE);
            //autoDataEntryImage.setImageResource(R.drawable.weighscale);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        Timber.d("PatientActivity: onPause");

        if(mRealm!=null){
            mRealm.close();
            mRealm = null;
        }
        stopDetect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopDetect();
    }

    public void startDetect() {
        if (LocalMeasurementDevicesHandler.getInstance().hasConnectedModel(Model.BERRY_BM1000B)
                || LocalMeasurementDevicesHandler.getInstance().hasConnectedModel(
                Model.NONIN_3230)) {
            try {
                Timber.d("Started detecting");
                if (deviceMeter == null) {
                    if (device != null) {
                        deviceMeter = new DeviceMeter(this, mMeterListener, device.getModel(),
                                device.getDeviceId());
                    } else {
                        deviceMeter = new DeviceMeter(this, mMeterListener, DeviceMeter.TYPE_SPO2);
                    }
                    deviceMeter.setMode(DeviceMeter.MODE_READING);
                }
                deviceMeter.startScanning();
            } catch (BleException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        if (LocalMeasurementDevicesHandler.getInstance().hasConnectedModel(Model.JUMPER_JPD500E)) {
            try {
                mJumperOximeter = new JumperOximeter(this, mJumperOximeterListener);
                mJumperOximeter.startScanning();
            } catch (BleException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    public void stopDetect() {
        if(deviceMeter!=null) {
            deviceMeter.clear();
            deviceMeter.stopScanning();
            deviceMeter.disconnect();
            deviceMeter.close();
            //deviceMeter = null;
        }

        if (mJumperOximeter != null) {
            mJumperOximeter.stopScanning();
            mJumperOximeter.disconnect();
            mJumperOximeter.close();
        }
    }

    @Override
    public void onBackPressed(){
        //super.onBackPressed();

        if(hasData) {
            new AlertDialog.Builder(SpO2ReadingActivity.this)
                    .setTitle("Discard Data")
                    .setMessage("Discard current SpO2 data?")
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //started = false;
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton("Discard", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            started = false;
                            hasData = false;
                            stopDetect();
                            finish();
                            //TODO: upload data
                        }
                    })
                    .show();
        }else{
            stopDetect();
            finish();
        }
    }

    private AAndDMeterListener mMeterListener = new AAndDMeterListener() {
        @Override
        public void onResult(List<AbstractProfile> records) {
            Log.d(TAG,"RESULTS RETRIEVED! " + records.size());
        }

        @Override
        public void onDeviceBonded(final BluetoothDevice device) {
            Log.d(TAG,"ON DEVICE BONDED! " + device.getAddress());
            pairedDeviceMacAdd = device.getAddress();

            pairedDevice = LocalMeasurementDevicesHandler.getInstance().getDeviceByMacAddress(pairedDeviceMacAdd);
            if(pairedDevice!=null){
                Timber.d("Local Device ID/serial = " + pairedDevice.getDeviceId());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pairingIndicator.setProductImage(pairedDevice.getImage());
                        if(pairedDevice.getModel()==Model.NONIN_3230)
                        pairingIndicator.setPairingDescriptionImage(R.drawable.ic_pairing_nonin);
                    }
                });
            }
        }

        @Override
        public void onDataRetrieved(final BloodPressure bp) {

        }

        @Override
        public void onDataRetrieved(final Weight spo2) {
            Log.d(TAG,"ON WEIGHT DATA RETRIEVED: " + spo2.getWeight() + " " + spo2.getStringUnit());

        }

        @Override
        public void onDataRetrieved(Object object) {
            if(object instanceof SpO2){

                if(resultRetrieved){
                    return;
                }
                if(!isCdtRunning){
                    cdt.start();
                }
                final SpO2 spo2 = (SpO2)object;
                spo2.setEntityId(entityId);
                spo2List.add(spo2);

                Log.d(TAG,"ON SPO2: " + spo2.getValue() + " " + spo2.getStringUnit());
                if(!hasData) {
                    hasData = true;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        tvDescription.setText("Calculating SpO2...");
                        tvSpo2Value.setText((spo2.getValue()+" ").replace(".0 ","").replace(" ",""));
                        tvPulseValue.setText((spo2.getPulseRate()+" ").replace(".0 ","").replace(" ",""));
                        tvSpo2Value.invalidate();
                        tvPulseValue.invalidate();
                        tvDescription.invalidate();
                        String spo2Val = (spo2.getValue()+" ").replace(".0 ","").replace(" ","");
                        String pulseVal = (spo2.getPulseRate()+" ").replace(".0 ","").replace(" ","");
                        tvReading.setText(spo2Val + " % | " + pulseVal + " bpm");
                        tvReading.invalidate();
                    }
                });
            }
        }

        @Override
        public void onInvalidDataReturned() {
            if(isCdtRunning){
                cdt.cancel();
                dTotal = 0;
                progBar.setProgress(0);
                isCdtRunning = false;
            }

            if(resultRetrieved) {
                return;
            }else{
                spo2List.clear();
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    String spo2Val = "--";
                    String pulseVal = "--";
                   /* tvDescription.setText("Calculating SpO2...");
                    tvSpo2Value.setText(spo2Val);
                    tvPulseValue.setText(pulseVal);
                    tvSpo2Value.invalidate();
                    tvPulseValue.invalidate();
                    tvDescription.invalidate();*/
                    tvReading.setText(spo2Val + " % | " + pulseVal + " bpm");
                    tvReading.invalidate();
                }
            });
            //deviceMeter.startScanning();
        }

        @Override
        public void onConnectionStateChanged(int status) {
            Timber.d("On Connection State Changed " + status);
            if(status==BluetoothAdapter.STATE_CONNECTED){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //change instruction
                        pairingIndicator.setPairingDescription("Please keep still while taking your measurements");
                        if(pairedDevice!=null && pairedDevice.getModel()==Model.NONIN_3230) {
                            pairingIndicator.setPairingDescriptionImage(R.drawable.ic_pairing_nonin);
                        }else{
                            pairingIndicator.setPairingDescriptionImage(R.drawable.ic_pairing_berry2);
                        }
                        pairingIndicator.showPairingDescription();
                    }
                });
            }
            if(status== BluetoothAdapter.STATE_DISCONNECTED){
                Timber.d("On Connection State - DISCONNECTED");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //startDetect();
                        //pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_FAIL_TIMEOUT);
                       // endReading();
                    }
                });
            }
        }
    };

    private CustomToolbar.OnToolbarClickListener mToolbarListener = new CustomToolbar.OnToolbarClickListener() {
        @Override
        public void leftButtonClick() {
            onBackPressed();
        }

        @Override public void rightButtonClick() {
            saveData();
        }

        @Override public void secondRightButtonClick() {

        }
    };


    private JumperOximeterListener mJumperOximeterListener = new JumperOximeterListener() {
        @Override
        public void onDeviceScan(BluetoothDevice device) {
            pairedDeviceMacAdd = device.getAddress();
            pairedDevice = LocalMeasurementDevicesHandler.getInstance().getDeviceByMacAddress(pairedDeviceMacAdd);

            if (pairedDevice != null) {
                //mTemperature = -1;

                mJumperOximeter.stopScanning();
                mJumperOximeter.connectBluetoothDevice(device);
            }
        }

        @Override
        public void onReadResult(int sp02, int pulse, double pi) {
            if(resultRetrieved){
                return;
            }
            if(!isCdtRunning){
                cdt.start();
            }

            final SpO2 spo2 = new SpO2(sp02, pulse, Calendar.getInstance().getTime(), 0);
            spo2.setEntityId(entityId);
            spo2List.add(spo2);

            Log.d(TAG,"ON SPO2: " + spo2.getValue() + " " + spo2.getStringUnit());
            if(!hasData) {
                hasData = true;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    tvDescription.setText("Calculating SpO2...");
                    tvSpo2Value.setText((spo2.getValue()+" ").replace(".0 ","").replace(" ",""));
                    tvPulseValue.setText((spo2.getPulseRate()+" ").replace(".0 ","").replace(" ",""));
                    tvSpo2Value.invalidate();
                    tvPulseValue.invalidate();
                    tvDescription.invalidate();
                    String spo2Val = (spo2.getValue()+" ").replace(".0 ","").replace(" ","");
                    String pulseVal = (spo2.getPulseRate()+" ").replace(".0 ","").replace(" ","");
                    tvReading.setText(spo2Val + " % | " + pulseVal + " bpm");
                    tvReading.invalidate();
                }
            });
        }

        @Override
        public void onConnectionStateChanged(int status) {

            if (status == BluetoothAdapter.STATE_CONNECTED) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //change instruction
                        pairingIndicator.setPairingDescription("Please keep still while taking your measurements");
                        if(pairedDevice!=null && pairedDevice.getModel()==Model.NONIN_3230) {
                            pairingIndicator.setPairingDescriptionImage(R.drawable.ic_pairing_nonin);
                        }else{
                            pairingIndicator.setPairingDescriptionImage(R.drawable.ic_pairing_berry2);
                        }
                        pairingIndicator.showPairingDescription();
                    }
                });
            }
        }
    };
}
