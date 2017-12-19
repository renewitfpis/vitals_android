package sg.lifecare.medicare.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.calendardatepicker.MonthAdapter;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.realm.Realm;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.VitalConfigs;
import sg.lifecare.medicare.ble.BleException;
import sg.lifecare.medicare.ble.aandd.AAndD651BloodPressure;
import sg.lifecare.medicare.ble.aandd.AAndDMeter;
import sg.lifecare.medicare.ble.aandd.AAndDMeterListener;
import sg.lifecare.medicare.ble.profile.AbstractProfile;
import sg.lifecare.medicare.ble.profile.BloodPressureMeasurementProfile;
import sg.lifecare.medicare.ble.urion.UrionBloodPressureMeter;
import sg.lifecare.medicare.ble.urion.UrionBloodPressureMeterListener;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.database.sync.NetworkChangeReceiver;
import sg.lifecare.medicare.database.sync.SyncHandler;
import sg.lifecare.medicare.database.sync.UnsyncedData;
import sg.lifecare.medicare.object.LocalMeasurementDevicesHandler;
import sg.lifecare.medicare.ui.adapter.BloodPressureReadingAdapter;
import sg.lifecare.medicare.ui.pairing.ConvertGatewayActivity;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.ui.view.PairingIndicatorView;
import sg.lifecare.medicare.utils.BleUtil;
import sg.lifecare.medicare.utils.LifeCareHandler;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import timber.log.Timber;

/**
 * Created by janice on 20/6/16.
 */
public class BloodPressureReadingActivity extends AppCompatActivity implements CalendarDatePickerDialogFragment.OnDateSetListener, RadialTimePickerDialogFragment.OnTimeSetListener {

    int REQUEST_ENABLE_BT = 123;

    private static final String TAG = "BPReadingActivity";
    private static final String FRAG_TAG_DATE_PICKER = "fragment_date_picker_name";
    private static final String FRAG_TAG_TIME_PICKER = "fragment_time_picker_name";
    private static final String DATE_FORMAT = "dd MMM yyyy";
    private static final String TIME_FORMAT = "HH:mm";

    private SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    private SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);

    private EditText glucose_remarks, etSystolic, etDiastolic, etArterialPressure, etPulseRate;
    private TextView selectedDate, selectedTime;
    private TextView tvDesc;
    private TextView tvReading, tvDate;
    private Button addDeviceBtn, retryBtn;
    private ImageView dateBtn, timeBtn, autoDataEntryImage;
    private Spinner unitSpinner;
    private Calendar setDate;

    private ArrayList<Terumo> terumoList = new ArrayList<>();
    private Realm mRealm;
    boolean isDevicePaired = false;
    private AAndDMeter mAAndDMeter;
    private AlertDialog mBleEnableDialog;
    private PairingIndicatorView pairingIndicator;
    private CustomToolbar mToolbar;
    private BloodPressureReadingAdapter adapter;
    private ProgressDialog progress;

    private ArrayList<BloodPressure> bpList;
    private String pairedDeviceMacAdd;
    private MedicalDevice pairedDevice;

    private String entityId;

    private UrionBloodPressureMeter mUrionBloodPressureMeter;
    private BloodPressure mUrionBloodPressure;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_device_reading);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.uploading_data));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCancelable(false);

        bpList = new ArrayList<>();
        ViewStub stub = (ViewStub) findViewById(R.id.manual_entry_view);
        stub.setLayoutResource(R.layout.view_manual_entry_bp);
        final View inflated = stub.inflate();
        inflated.setVisibility(View.INVISIBLE);

        SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
        entityId = sh.getString("entity_id", "");

        mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

        unitSpinner = (Spinner) findViewById(R.id.blood_glucose_spinner);

        etSystolic = (EditText) findViewById(R.id.systolic);
        etDiastolic = (EditText) findViewById(R.id.diastolic);
        etPulseRate = (EditText) findViewById(R.id.pulse_rate);
        etArterialPressure = (EditText) findViewById(R.id.arterial_pressure);
        ((TextView) findViewById(R.id.sugar_con_subtitle)).setText("Blood Pressure Reading");

        tvReading = (TextView) findViewById(R.id.reading_value);
        tvDate = (TextView) findViewById(R.id.taken_value);

        tvDesc = (TextView) findViewById(R.id.description);
        selectedDate = (TextView) findViewById(R.id.date_selection);
        selectedTime = (TextView) findViewById(R.id.time_selection);

       // timeBtn = (ImageView) findViewById(R.id.time_btn);
       // dateBtn = (ImageView) findViewById(R.id.calendar_btn);
        autoDataEntryImage = (ImageView) findViewById(R.id.automatic_data_entry_image);

        pairingIndicator = (PairingIndicatorView) findViewById(R.id.pairing_indicator);
        pairingIndicator.setProductImage(R.drawable.automatic_pair_bp);
        pairingIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!BleUtil.isBleEnabled()){
                    mBleEnableDialog.show();
                }
            }
        });
        final RelativeLayout autoEntryBtn = (RelativeLayout) findViewById(R.id.auto_entry_button);
        autoEntryBtn.setSelected(true);

        final RelativeLayout manualEntryBtn = (RelativeLayout) findViewById(R.id.manual_entry_button);
        autoEntryBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                inflated.setVisibility(View.INVISIBLE);
                findViewById(R.id.auto_entry_view).setVisibility(View.VISIBLE);
                autoEntryBtn.setSelected(true);
                manualEntryBtn.setSelected(false);
                mToolbar.hideRightButton();
            }
        });
        manualEntryBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.auto_entry_view).setVisibility(View.INVISIBLE);
                inflated.setVisibility(View.VISIBLE);
                manualEntryBtn.setSelected(true);
                autoEntryBtn.setSelected(false);
                mToolbar.showRightButton();
            }
        });
        addDeviceBtn = (Button) findViewById(R.id.add_device_button);
        addDeviceBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!BleUtil.isBleEnabled()){
                    mBleEnableDialog.show();
                }else {
                    if (!isDevicePaired) {
                        Intent intent = new Intent(BloodPressureReadingActivity.this,
                                ConvertGatewayActivity.class);
                        intent.putExtra("medical_device",MedicalDevice.findModel(Model.AANDD_UA_651));
                        startActivity(intent);
                    }
                }
            }
        });

        retryBtn = (Button) findViewById(R.id.retry_button);
        retryBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //tvDesc.setText(R.string.device_info_retrieve_data);
                //autoDataEntryImage.setImageResource(R.drawable.automatic_read_bp_4);
                pairingIndicator.setPairingMode(PairingIndicatorView.READING_BLE);
                retryBtn.setVisibility(View.INVISIBLE);
                startDetect();
            }
        });

        selectedDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO:onCalendarDatePickerPressed();
            }
        });
        selectedTime.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onTimePressed();
            }
        });

        mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_activity_blood_pressure_reading);
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.setRightButtonImage(R.drawable.ic_toolbar_tick);
        mToolbar.hideRightButton();
        mToolbar.hideSecondRightButton();
        mToolbar.setListener(mToolbarListener);


        mBleEnableDialog = BleUtil.enableBleDialogBuilder(this,
                mBleSettingsButtonListener, mBleCancelButtonListener).create();

        currentTimeAndDate();
        BloodSugarSpinner();
        //MeasuredSpinner();

        //Check if device is paired
        //new getConnectedDeviceList().execute();
        getLocalConnectedDevices();

        CalendarDatePickerDialogFragment datePickerFrag =
                (CalendarDatePickerDialogFragment) getSupportFragmentManager()
                        .findFragmentByTag(FRAG_TAG_DATE_PICKER);

        if (datePickerFrag != null) {
            datePickerFrag.setOnDateSetListener(BloodPressureReadingActivity.this);
        }

        RadialTimePickerDialogFragment timePickerFrag =
                (RadialTimePickerDialogFragment) getSupportFragmentManager().
                        findFragmentByTag(FRAG_TAG_TIME_PICKER);

        if (timePickerFrag != null) {
            timePickerFrag.setOnTimeSetListener(BloodPressureReadingActivity.this);
        }
    }

    public void currentTimeAndDate() {
        setDate = Calendar.getInstance(TimeZone.getDefault());
        selectedDate.setText(dateFormat.format(setDate.getTime()));
        selectedTime.setText(timeFormat.format(setDate.getTime()));
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
            getLocalConnectedDevices();
        }
    }

    private void getLocalConnectedDevices(){
        if (!BleUtil.isBleEnabled()) {
            tvDesc.setText(R.string.pairing_bluetooth_not_connected);
            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_BLE_OFF);
            addDeviceBtn.setVisibility(View.VISIBLE);
            return;
        }

        ArrayList<MedicalDevice> list =
                LocalMeasurementDevicesHandler.getInstance().getConnectedMedicalDeviceList();

        for (MedicalDevice device : list) {
            if(device.getModel()==Model.AANDD_UA_651 || device.getModel() == Model.URION_BP_U80E){
                isDevicePaired = true;
            }
        }

        if(isDevicePaired){
            tvDesc.setText(R.string.device_info_retrieve_data);
            autoDataEntryImage.setImageResource(R.drawable.automatic_read_bp_4);
            pairingIndicator.setPairingMode(PairingIndicatorView.READING_BLE);
            addDeviceBtn.setVisibility(View.INVISIBLE);
            startDetect();
        }
        else {
            tvDesc.setText(R.string.device_info_no_pair_device);
            autoDataEntryImage.setImageResource(R.drawable.automatic_pair_bp);
            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_NO_DEVICE);
            addDeviceBtn.setVisibility(View.VISIBLE);
        }
    }

    public void onCalendarDatePickerPressed() {
        final Calendar c = Calendar.getInstance();
        int currentDate = c.get(Calendar.DAY_OF_MONTH);
        int currentMonth = c.get(Calendar.MONTH);
        int currentYear = c.get(Calendar.YEAR);

        MonthAdapter.CalendarDay maxDate = new MonthAdapter.CalendarDay(currentYear, currentMonth, currentDate);
        CalendarDatePickerDialogFragment cdp = new CalendarDatePickerDialogFragment()
                .setDoneText(getString(R.string.dialog_ok))
                .setCancelText(getString(R.string.dialog_cancel))
                .setThemeCustom(R.style.MyCustomBetterPickersDialogs)
                .setDateRange(null, maxDate)
                .setPreselectedDate(setDate.get(Calendar.YEAR),
                        setDate.get(Calendar.MONTH),setDate.get(Calendar.DAY_OF_MONTH))
                .setOnDateSetListener(BloodPressureReadingActivity.this);
        cdp.show(getSupportFragmentManager(), FRAG_TAG_DATE_PICKER);
    }

    public void onDateSet(CalendarDatePickerDialogFragment dialog, int year, int monthOfYear, int dayOfMonth) {
        String date;
        final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        String mon = MONTHS[monthOfYear];
        if (dayOfMonth < 10)
            date = "0" + dayOfMonth + " " + mon + " " + year;
        else
            date = dayOfMonth + " " + mon + " " + year;
        selectedDate.setText(date);

        setDate.set(year, monthOfYear, dayOfMonth);
    }

    private class getConnectedDeviceList extends AsyncTask<Void, Void, Void>
    {
        private ArrayList<MedicalDevice> list = new ArrayList<>();

        @Override
        protected void onPreExecute()
        {
            list.clear();
        }

        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {

            Log.d(TAG, "enter doinbg");

            String gatewayId = LifeCareHandler.getInstance().getMyDeviceID(BloodPressureReadingActivity.this);
            String myId = entityId + gatewayId;

            Log.d(TAG, "myId=" + myId);
            Log.d(TAG, "entity ID=" + entityId);
            Log.d(TAG, "gateway ID=" + gatewayId);

            JSONArray result = LifeCareHandler.getInstance().getConnectedDeviceList(entityId, gatewayId);

            if(result != null && result.length() > 0)
            {
                Log.w(TAG, "result device list: " + result.length());
                Log.w(TAG, result.toString());
                try
                {
                    for(int i = 0; i < result.length(); i++)
                    {
                        JSONObject data = result.getJSONObject(i);
                        String deviceName;
                        String productId = "";
                        String deviceId;
                        String imgUrl ="";

                        deviceName = data.getString("name");
                        deviceId = data.getString("_id");

                        if (!TextUtils.isEmpty(deviceId)) {
                            if (deviceId.startsWith(myId)) {
                                deviceId = deviceId.substring(myId.length(), deviceId.length());
                            }
                        }

                        JSONObject product = data.getJSONObject("product");
                        if (product != null) {
                            productId = product.getString("_id");
                            if(product.has("medias")){
                                Object mediaObj = product.get("medias");
                                if(mediaObj instanceof JSONArray)
                                {
                                    JSONArray media = product.getJSONArray("medias");
                                    if(media.length() > 0)
                                    {
                                        JSONObject module = media.getJSONObject(0);

                                        if(module.getString("media_url") != null
                                                && !module.getString("media_url").isEmpty()
                                                && !module.getString("media_url").equalsIgnoreCase("null"))
                                        {
                                            imgUrl = module.getString("media_url");
                                        }
                                    }
                                }
                            }
                        }

                        Log.d(TAG, "deviceName=" + deviceName + ", productId=" + productId + ", deviceId=" + deviceId);

                        if (!TextUtils.isEmpty(deviceName) && !TextUtils.isEmpty(productId) && !TextUtils.isEmpty(deviceId)) {
                            MedicalDevice md = MedicalDevice.findByProductId(productId);
                            if (md  != null) {
                                md.setAssignedName(deviceName);
                                md.setDeviceId(deviceId);
                                md.setMediaImageURL(imgUrl);
                                list.add(md);
                            } else {
                                Log.w(TAG, "cannot find product " + productId);
                            }
                        }
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
        public void onPostExecute(Void result) {

            for (MedicalDevice device : list) {
                if(device.getModel()==Model.AANDD_UA_651){
                    isDevicePaired = true;
                }
            }

            if(isDevicePaired){
                tvDesc.setText(R.string.blood_pressure_info_press_button);
                autoDataEntryImage.setImageResource(R.drawable.automatic_read_bp_4);
                startDetect();
            }
            else {
                tvDesc.setText(R.string.blood_pressure_info_pair_device);
                autoDataEntryImage.setImageResource(R.drawable.automatic_pair_bp);
            }
        }
    }

    public void onResume() {
        super.onResume();

        if (mRealm == null)
            mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

        getLocalConnectedDevices();
        //new getConnectedDeviceList().execute();
    }

    public void onTimePressed() {
        RadialTimePickerDialogFragment rtpd = new RadialTimePickerDialogFragment()
                .setOnTimeSetListener(BloodPressureReadingActivity.this)
                .setCancelText(getString(R.string.dialog_cancel))
                .setDoneText(getString(R.string.dialog_ok))
                .setThemeCustom(R.style.MyCustomBetterPickersDialogs)
                .setStartTime(setDate.get(Calendar.HOUR_OF_DAY),setDate.get(Calendar.MINUTE));
        rtpd.show(getSupportFragmentManager(), FRAG_TAG_TIME_PICKER);
    }

    public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
        String time = String.format("%02d:%02d", hourOfDay, minute);
        selectedTime.setText(time);
        setDate.set(
                setDate.get(Calendar.YEAR),
                setDate.get(Calendar.MONTH),
                setDate.get(Calendar.DAY_OF_MONTH),
                hourOfDay,
                minute
        );
    }

    private void BloodSugarSpinner() {
        List<String> l = Arrays.<String>asList(getResources().getStringArray(R.array.blood_pressure_units));
        ArrayList<String> bloodPressureUnit = new ArrayList<>(l);

        ArrayAdapter adapter = new ArrayAdapter(BloodPressureReadingActivity.this,
                R.layout.spinner_item,bloodPressureUnit);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        unitSpinner.setAdapter(adapter);
        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Level: " + item);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void getData() {
        String systolic = etSystolic.getText().toString().trim();
        String diastolic = etDiastolic.getText().toString().trim();
        String pulseRate = etPulseRate.getText().toString().trim();
        String arterialPressure = etArterialPressure.getText().toString().trim();
        if (systolic.isEmpty()) {
            etSystolic.setError("Please enter systolic reading");
            return;
        }

        if (diastolic.isEmpty()) {
            etDiastolic.setError("Please enter diastolic reading");
            return;
        }

        /*String remarks = glucose_remarks.getText().toString().trim();
        try {
            remarks = URLEncoder.encode(remarks,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }*/

        float systolicVal = Float.parseFloat(systolic);
        float diastolicVal = Float.parseFloat(diastolic);
        float pulseRateVal = Float.parseFloat(pulseRate);
        //float arterialPressureVal = Float.parseFloat(arterialPressure);

        BloodPressure bp = new BloodPressure();
        bp.setSystolic(systolicVal);
        bp.setDistolic(diastolicVal);
        bp.setPulseRate(pulseRateVal);
        //bp.setArterialPressure(arterialPressureVal);
        bp.setUnit(unitSpinner.getSelectedItemPosition());
        bp.setDate(setDate.getTime());
        bp.setEntityId(entityId);

        Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
        PatientData.getInstance().addBloodPressure(realm, bp);
        realm.close();

        if(NetworkChangeReceiver.isInternetAvailable(this)){
            new uploadDataToServer(bp,true,true).execute();
            Timber.d("blood pressure - Internet is available");
        }
        else{
            SyncHandler.registerNetworkBroadcast();
            UnsyncedData.bloodPressureList.add(setDate.getTime());
            Timber.d("Added blood pressure reading to unsyc");

            onDataUpdated();
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
        pairedDevice = null;

        try{
            if (LocalMeasurementDevicesHandler.getInstance().hasConnectedModel(Model.AANDD_UA_651)) {

                if (mAAndDMeter == null) {
                    mAAndDMeter = new AAndD651BloodPressure(this, mMeterListener, "");
                    mAAndDMeter.setMode(AAndDMeter.MODE_READING);
                }
                mAAndDMeter.startScanning();
            }

            if (LocalMeasurementDevicesHandler.getInstance().hasConnectedModel(Model.URION_BP_U80E)) {
                if (mUrionBloodPressureMeter == null) {
                    mUrionBloodPressureMeter = new UrionBloodPressureMeter(this, mUrionBloodPressureMeterListener);
                }

                mUrionBloodPressureMeter.startScanning();
            }
        } catch (BleException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void stopDetect() {

        if(mAAndDMeter!=null) {
            mAAndDMeter.stopScanning();
            mAAndDMeter.disconnect();
            mAAndDMeter.close();
        }

        if (mUrionBloodPressureMeter != null) {
            mUrionBloodPressureMeter.stopScanning();
            mUrionBloodPressureMeter.disconnect();
            mUrionBloodPressureMeter.close();
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
        }

        @Override
        public void onDataRetrieved(final BloodPressure bp) {
            Log.d(TAG,"ON BP DATA RETRIEVED: " + bp.getSystolic() + "/" + bp.getDistolic());

            //mAAndDMeter.startScanning();

            //TODO add blood pressure data here
            bpList.add(bp);
            /*runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pairingLayout.setVisibility(View.GONE);
                    readingLayout.setVisibility(View.VISIBLE);
                    tvReading.setText(bp.getSystolic() + "/" + bp.getDistolic() + " " + bp.getStringUnit() );
                    tvDate.setText(bp.getStringDate());
                    tvReading.invalidate();
                    tvDate.invalidate();
                }
            });*/
        }

        @Override
        public void onDataRetrieved(final Weight weight) {

        }

        @Override
        public void onDataRetrieved(Object object) {

        }

        @Override
        public void onInvalidDataReturned() {
            mAAndDMeter.startScanning();
        }

        @Override
        public void onConnectionStateChanged(int status) {
            if(status == BluetoothProfile.STATE_DISCONNECTED){
                stopDetect();
                if(bpList.size()>0){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            displayResults();
                        }
                    });
                }else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_ERROR);
                            retryBtn.setVisibility(View.VISIBLE);

                        }
                    });
                }
            }
        }
    };

    private void displayResults(){
        final Dialog dlg = new Dialog(this);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.dialog_bp, null);

        TextView done = (TextView) view.findViewById(R.id.done_button);
        TextView cancel = (TextView) view.findViewById(R.id.cancel_button);

        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dlg.cancel();
                bpList.clear();
                startDetect();
            }
        });
        done.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                Timber.d("BP list DONE ONCLICK = " + bpList.size());
                dlg.cancel();

                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.getInstance().DATE_DISPLAY_FORMAT, Locale.ENGLISH);
                //find latest date
                Timber.d("BP list size = " + bpList.size());
                for (int i = 0; i < bpList.size(); i++) {

                    Timber.d("BP list pos " + i + " = " + adapter.selected[i]);
                    if(adapter.selected[i]) {
                        if (!PatientData.getInstance().isBloodPressureExistByDate(mRealm, bpList.get(i).getDate())) {
                            PatientData.getInstance().addBloodPressure(mRealm, bpList.get(i));

                             Timber.d("Added new record: (val) " + bpList.get(i).getSystolic()+"/"+
                                     bpList.get(i).getDistolic()
                                   + " (date) " + sdf.format(bpList.get(i).getDate()) + "(entityid)" +
                             bpList.get(i).getEntityId());

                            boolean isLastData = false;
                            //push data to server
                            if (i == bpList.size() - 1) {
                                isLastData = true;
                            }

                            new uploadDataToServer(bpList.get(i),isLastData, false).execute();

                        } else {
                            Timber.d("Found existing record: (val) " + terumoList.get(i).getValue()
                                    + " (date) " + sdf.format(terumoList.get(i).getDate()));
                        }
                    }
                }

                bpList.clear();
            }
        });

         adapter = new BloodPressureReadingAdapter(
                BloodPressureReadingActivity.this, R.layout.bp_result_item, bpList
        );
        ListView listView = (ListView) view.findViewById(R.id.list_view);
        listView.setAdapter(adapter);

        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlg.setCanceledOnTouchOutside(true);
        dlg.setCancelable(true);
        dlg.setContentView(view);
        dlg.show();
    }

    private class uploadDataToServer extends AsyncTask<Void, Void, Void>
    {
        private BloodPressure bp;
        //private boolean isManualKeyIn;
        private boolean success, isLastData, isManual;

        public uploadDataToServer(BloodPressure bp, boolean isLastData, boolean isManual)
        {
            this.bp = bp;
            this.isLastData = isLastData;
            this.isManual = isManual;
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
            try {
                JSONObject data = new JSONObject();

                String extraData = "HighBlood:" + bp.getSystolic()
                        + "&LowBlood:" + bp.getDistolic()
                        + "&HeartBeat:" + bp.getPulseRate()
                        + "&Remarks:" + bp.getRemark();

                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.getInstance().DATE_FULL_FORMAT_UPLOAD,Locale.ENGLISH);

                data.put("DeviceId", LifeCareHandler.getInstance().getMyDeviceID(BloodPressureReadingActivity.this));
                data.put("EntityId", entityId);
                data.put("ExtraData", extraData);
                data.put("EventTypeName", "BloodPressure Update Data");
                data.put("EventTypeId", "20013"); //blood pressure
                data.put("Zone", "56398aa0e4b00e308ce460ec");
                data.put("ZoneCode", "OT");
                data.put("CreateDate", sdf.format(bp.getDate()));
                data.put("WriteToSocket",true);

                if(!isManual && pairedDevice!=null) {

                    String gatewayId =  LifeCareHandler.getInstance().getMyDeviceID(BloodPressureReadingActivity.this);
                    SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
                    String serial = bp.getEntityId() + "-" + gatewayId + "-" + pairedDevice.getDeviceId().replace(":","");
                    Log.d(TAG,"SERIAL = " + serial);

                    data.put("NodeId", serial);
                    data.put("NodeName", pairedDevice.getAssignedName());
                    data.put("WConfigCode", "BLE");
                    data.put("SystemManufacturerCode", pairedDevice.getManufacturerCode());
                    data.put("SystemProductCode", pairedDevice.getProductCode());
                    data.put("SystemProductTypeCode", pairedDevice.getProductTypeCode());
                    data.put("SmartDeviceId", serial);
                }

                String jsonString = data.toString();

                Log.w(TAG, "Uploading BG Data To Server : " + jsonString);

                Request request = new Request.Builder()
                        .url(VitalConfigs.URL + "mlifecare/event/addEvent")
                        .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                        .build();

                Response response = LifeCareHandler.getInstance().okclient.newCall(request).execute();

                JSONObject json = new JSONObject(response.body().string());
                success = !json.getBoolean("Error"); //Error = true if contains error
                //success = response.isSuccessful();
                Log.d(TAG, "Successful upload: " + success + " , " + json.getString("ErrorDesc"));

                response.body().close();
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
                UnsyncedData.bloodPressureList.add(setDate.getTime());
            }

            if(isLastData){
                if(progress!=null && progress.isShowing()){
                    progress.dismiss();
                }
                String message = "";
                if(success){
                    message = "Successfully updated blood pressure data!";
                }else{
                    message = "Problem uploading data! Data have been saved to cache!";
                }
                Toast.makeText(BloodPressureReadingActivity.this, message, Toast.LENGTH_LONG).show();

                onDataUpdated();
            }
        }
    }

    private void onDataUpdated(){
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }

    private CustomToolbar.OnToolbarClickListener mToolbarListener = new CustomToolbar.OnToolbarClickListener() {
        @Override
        public void leftButtonClick() {
            onBackPressed();
        }

        @Override public void rightButtonClick() {
            getData();
        }

        @Override public void secondRightButtonClick() {

        }
    };

    private UrionBloodPressureMeterListener mUrionBloodPressureMeterListener = new UrionBloodPressureMeterListener() {


        @Override
        public void onDeviceScan(final BluetoothDevice device) {
            Timber.d("onDeviceScan");

            pairedDeviceMacAdd = device.getAddress();

            if (pairedDevice == null) {
                pairedDevice = LocalMeasurementDevicesHandler.getInstance().getDeviceByMacAddress(pairedDeviceMacAdd);

                if (pairedDevice != null) {
                    bpList.clear();

                    mUrionBloodPressureMeter.stopScanning();


                    final Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            mUrionBloodPressureMeter.connectBluetoothDevice(device);
                        }
                    };

                    mHandler.postDelayed(r, 1000);
                }
            }
        }

        @Override
        public void onReadResult(int systolic, int diastolic, int pulse) {
            if (mUrionBloodPressure == null) {
                mUrionBloodPressure = new BloodPressure();
            }

            mUrionBloodPressure.setSystolic(systolic);
            mUrionBloodPressure.setDistolic(diastolic);
            mUrionBloodPressure.setPulseRate(pulse);
            mUrionBloodPressure.setDate(Calendar.getInstance().getTime());
            mUrionBloodPressure.setUnit(BloodPressureMeasurementProfile.UNIT_SI);
            mUrionBloodPressure.setEntityId(entityId);
        }

        @Override
        public void onReadPulse(int pulse) {

        }

        @Override
        public void onConnectionStateChanged(int status) {
            if(status == BluetoothProfile.STATE_DISCONNECTED){
                stopDetect();
                if(mUrionBloodPressure != null){
                    bpList.add(mUrionBloodPressure);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            displayResults();
                        }
                    });
                }else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_ERROR);
                            retryBtn.setVisibility(View.VISIBLE);

                        }
                    });
                }
            }
        }
    };
}
