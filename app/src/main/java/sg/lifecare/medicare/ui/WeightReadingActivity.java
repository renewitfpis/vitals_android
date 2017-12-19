package sg.lifecare.medicare.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.calendardatepicker.MonthAdapter;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.kitnew.ble.QNBleDevice;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.realm.Realm;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.VitalConfigs;
import sg.lifecare.medicare.ble.Ble;
import sg.lifecare.medicare.ble.BleException;
import sg.lifecare.medicare.ble.aandd.AAndD352WeighScale;
import sg.lifecare.medicare.ble.aandd.AAndDMeter;
import sg.lifecare.medicare.ble.aandd.AAndDMeterListener;
import sg.lifecare.medicare.ble.profile.AbstractProfile;
import sg.lifecare.medicare.ble.profile.TemperatureMeasurementProfile;
import sg.lifecare.medicare.ble.profile.WeightMeasurementProfile;
import sg.lifecare.medicare.ble.qn.YolandaWeightMeter;
import sg.lifecare.medicare.ble.qn.YolandaWeightMeterListener;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Temperature;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.database.sync.NetworkChangeReceiver;
import sg.lifecare.medicare.database.sync.SyncHandler;
import sg.lifecare.medicare.database.sync.UnsyncedData;
import sg.lifecare.medicare.object.LocalMeasurementDevicesHandler;
import sg.lifecare.medicare.ui.adapter.WeightReadingAdapter;
import sg.lifecare.medicare.ui.pairing.ConvertGatewayActivity;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.ui.view.PairingIndicatorView;
import sg.lifecare.medicare.utils.BleUtil;
import sg.lifecare.medicare.utils.LifeCareHandler;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import timber.log.Timber;

public class WeightReadingActivity extends AppCompatActivity implements CalendarDatePickerDialogFragment.OnDateSetListener, RadialTimePickerDialogFragment.OnTimeSetListener {

    private static final String TAG = "WeightReadingActivity";
    private static final String FRAG_TAG_DATE_PICKER = "fragment_date_picker_name";
    private static final String FRAG_TAG_TIME_PICKER = "fragment_time_picker_name";
    private static final String DATE_FORMAT = "dd MMM yyyy";
    private static final String TIME_FORMAT = "HH:mm";
    private static final int REQUEST_ENABLE_BT = 123;

    private SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
    private SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT, Locale.ENGLISH);

    private RelativeLayout readingLayout;
    private LinearLayout pairingLayout;
    private ImageView dateBtn, timeBtn, autoDataEntryImage;
    private TextView selectedDate, selectedTime;
    private TextView tvReading, tvDate, tvDesc;
    private EditText etWeight;
    private Spinner unitSpinner;
    private Calendar setDate;

    private Realm mRealm;
    private AAndDMeter mAAndDMeter;
    private AlertDialog mBleEnableDialog;
    private boolean isDevicePaired = false;
    private String entityId;

    private PairingIndicatorView pairingIndicator;
    private Button addDeviceBtn, retryBtn;
    private ArrayList<Weight> weightList;
    private WeightReadingAdapter adapter;
    private ProgressDialog progress;

    private String pairedDeviceMacAdd;
    private MedicalDevice pairedDevice;


    private YolandaWeightMeter mYolandaWeightMeter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_reading);

        weightList = new ArrayList<>();

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.uploading_data));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCancelable(false);

        ViewStub stub = (ViewStub) findViewById(R.id.manual_entry_view);
        stub.setLayoutResource(R.layout.view_manual_entry_weight);
        final View inflated = stub.inflate();
        inflated.setVisibility(View.INVISIBLE);

        SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
        entityId = sh.getString("entity_id", "");

        mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
        mBleEnableDialog = BleUtil.enableBleDialogBuilder(this,
                mBleSettingsButtonListener, mBleCancelButtonListener).create();

        readingLayout       = (RelativeLayout) findViewById(R.id.read_detail_view);
        pairingLayout       = (LinearLayout) findViewById(R.id.pairing_view);
        selectedDate        = (TextView) findViewById(R.id.date_selection);
        selectedTime        = (TextView) findViewById(R.id.time_selection);
        tvReading           = (TextView) findViewById(R.id.reading_value);
        tvDate              = (TextView) findViewById(R.id.taken_value);
        tvDesc              = (TextView) findViewById(R.id.description);
        autoDataEntryImage  = (ImageView) findViewById(R.id.auto_data_entry_image);
        unitSpinner         = (Spinner) findViewById(R.id.blood_glucose_spinner);
        etWeight            = (EditText) findViewById(R.id.weight_reading);


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

        final CustomToolbar mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_activity_weight_reading);
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.setRightButtonImage(R.drawable.ic_toolbar_tick);
        mToolbar.hideRightButton();
        mToolbar.hideSecondRightButton();
        mToolbar.setListener(mToolbarListener);

        pairingIndicator = (PairingIndicatorView) findViewById(R.id.pairing_indicator);
        pairingIndicator.setProductImage(R.drawable.weighscale);
        pairingIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!BleUtil.isBleEnabled()){
                    mBleEnableDialog.show();
                }else{
                    startDetect();
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
                        Intent intent = new Intent(WeightReadingActivity.this,
                                ConvertGatewayActivity.class);
                        intent.putExtra("medical_device",MedicalDevice.findModel(Model.AANDD_UC_352));
                        //intent.putExtra("is_glucose",true);
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
                retryBtn.setVisibility(View.GONE);
                stopDetect();
                startDetect();
            }
        });
        currentTimeAndDate();
        BloodSugarSpinner();
        //Check if device is paired
        //new getConnectedDeviceList().execute();
        getLocalConnectedDevices();

        CalendarDatePickerDialogFragment datePickerFrag =
                (CalendarDatePickerDialogFragment) getSupportFragmentManager()
                        .findFragmentByTag(FRAG_TAG_DATE_PICKER);

        if (datePickerFrag != null) {
            datePickerFrag.setOnDateSetListener(WeightReadingActivity.this);
        }

        RadialTimePickerDialogFragment timePickerFrag =
                (RadialTimePickerDialogFragment) getSupportFragmentManager().
                        findFragmentByTag(FRAG_TAG_TIME_PICKER);

        if (timePickerFrag != null) {
            timePickerFrag.setOnTimeSetListener(WeightReadingActivity.this);
        }
    }


    private void displayResults(){
        //stopDetect();

        final Dialog dlg = new Dialog(this);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.dialog_bp, null);
        TextView done = (TextView) view.findViewById(R.id.done_button);
        TextView cancel = (TextView) view.findViewById(R.id.cancel_button);

        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dlg.cancel();
                weightList.clear();

                stopDetect();
                startDetect();
            }
        });
        done.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dlg.cancel();

                //hasDisplayed = false;

                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_DISPLAY_FORMAT, Locale.ENGLISH);
                //find latest date
                Timber.d("weight list size = " + weightList.size());
                for (int i = 0; i < weightList.size(); i++) {
                    if(adapter.selected[i]) {
                        if (!PatientData.getInstance().isWeightExistByDate(mRealm, weightList.get(i).getDate())) {
                            PatientData.getInstance().addWeight(mRealm, weightList.get(i));
                            PatientData.getInstance().updateUserLatestWeightProfile(mRealm,entityId);
                            Timber.d("Added new record: (val) " + weightList.get(i).getWeight()
                                    + " (date) " + sdf.format(weightList.get(i).getDate()));

                            boolean isLastData = false;
                            //push data to server
                            if (i == weightList.size() - 1) {
                                isLastData = true;
                            }

                            new uploadWeightDataToServer(weightList.get(i),isLastData,false).execute();

                        } else {
                            Timber.d("Found existing record: (val) " + weightList.get(i).getWeight()
                                    + " (date) " + sdf.format(weightList.get(i).getDate()));
                        }
                    }
                }
                weightList.clear();
            }
        });

        adapter = new WeightReadingAdapter(
                WeightReadingActivity.this, R.layout.weight_result_item, weightList
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

    public void currentTimeAndDate() {
        setDate = Calendar.getInstance(TimeZone.getDefault());
        selectedDate.setText(dateFormat.format(setDate.getTime()));
        selectedTime.setText(timeFormat.format(setDate.getTime()));
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
                .setOnDateSetListener(WeightReadingActivity.this);
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

    public void onResume() {
        super.onResume();

        Timber.d("ON RESUME");
        if (mRealm == null)
            mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

        getLocalConnectedDevices();

        //new getConnectedDeviceList().execute();
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
            Timber.d("BLE NOY ENABLED");
            tvDesc.setText(R.string.pairing_bluetooth_not_connected);
            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_BLE_OFF);
            addDeviceBtn.setVisibility(View.VISIBLE);
            return;
        }

        ArrayList<MedicalDevice> list =
                LocalMeasurementDevicesHandler.getInstance().getConnectedMedicalDeviceList();

        for (MedicalDevice device : list) {
            if(device.getModel()==Model.AANDD_UC_352 || device.getModel() == Model.YOLANDA_LITE){
                isDevicePaired = true;
            }
        }

        if(isDevicePaired){
            tvDesc.setText(R.string.device_info_retrieve_data);
            pairingIndicator.setPairingMode(PairingIndicatorView.READING_BLE);
            addDeviceBtn.setVisibility(View.INVISIBLE);
            //autoDataEntryImage.setImageResource(R.drawable.weighscale);
            startDetect();
        }
        else {
            tvDesc.setText(R.string.device_info_no_pair_device);
            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_NO_DEVICE);
            addDeviceBtn.setVisibility(View.VISIBLE);
            //autoDataEntryImage.setImageResource(R.drawable.weighscale);
        }
    }

    public void onTimePressed() {
        RadialTimePickerDialogFragment rtpd = new RadialTimePickerDialogFragment()
                .setOnTimeSetListener(WeightReadingActivity.this)
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
        List<String> l = Arrays.<String>asList(getResources().getStringArray(R.array.weight_units));
        ArrayList<String> weightUnit = new ArrayList<>(l);

        ArrayAdapter adapter = new ArrayAdapter(WeightReadingActivity.this,R.layout.spinner_item,weightUnit);
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
        String weightReading = etWeight.getText().toString().trim();
         if (weightReading.isEmpty()) {
             etWeight.setError("Please enter weight reading");
            return;
        }

        double weightVal = Double.parseDouble(weightReading);

        int selectedUnit = unitSpinner.getSelectedItemPosition();

        Weight weight = new Weight();
        weight.setWeight(weightVal);
        weight.setUnit(unitSpinner.getSelectedItemPosition());
        weight.setDate(setDate.getTime());
        weight.setEntityId(entityId);

        Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
        PatientData.getInstance().addWeight(realm, weight);
        PatientData.getInstance().updateUserLatestWeightProfile(realm,entityId);
        realm.close();

        if(NetworkChangeReceiver.isInternetAvailable(this)){
            new uploadWeightDataToServer(weight,true,true).execute();
            Timber.d("weight - Internet is available");
        }
        else{
            SyncHandler.registerNetworkBroadcast();
            UnsyncedData.weightList.add(setDate.getTime());
            onDataUpdated();
            Timber.d("Added weight reading to unsyc");
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
        try{

            Timber.d("StartDetect!");
            if (LocalMeasurementDevicesHandler.getInstance().hasConnectedModel(Model.AANDD_UC_352)) {

                if (mAAndDMeter == null) {
                    Timber.d("StartDetect2!");
                    mAAndDMeter = new AAndD352WeighScale(this, mMeterListener, "");
                    mAAndDMeter.setMode(AAndDMeter.MODE_READING);
                }

                Timber.d("StartDetect3!");
                mAAndDMeter.startScanning();
            }

            if (LocalMeasurementDevicesHandler.getInstance().hasConnectedModel(Model.YOLANDA_LITE)) {
                if (mYolandaWeightMeter == null) {
                    mYolandaWeightMeter = new YolandaWeightMeter(this, mYolandaWeightMeterListener);
                }

                mYolandaWeightMeter.startScanning();
            }
        } catch (BleException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void stopDetect() {
        Timber.d("StopDetect!");
        if(mAAndDMeter!=null) {
            Timber.d("StopDetect2!");
            //mAAndDMeter.clear();
            mAAndDMeter.stopScanning();
            mAAndDMeter.disconnect();
            mAAndDMeter.close();
            //mAAndDMeter = null;
        }

        if (mYolandaWeightMeter != null) {
            mYolandaWeightMeter.disconnect();
            mYolandaWeightMeter.stopScanning();
        }

        //mTagDetector.stopDetect();
    }


    private AAndDMeterListener mMeterListener = new AAndDMeterListener() {
        @Override
        public void onResult(List<AbstractProfile> records) {
            Log.d(TAG,"RESULTS RETRIEVED! " + records.size());
        }

        @Override
        public void onDeviceBonded(final BluetoothDevice device) {
            Log.d(TAG,"ON DEVICE BONDED! " +device.getAddress());
            pairedDeviceMacAdd = device.getAddress();
            pairedDevice = LocalMeasurementDevicesHandler.getInstance().getDeviceByMacAddress(pairedDeviceMacAdd);
            if(pairedDevice!=null){
                Log.d(TAG,"ON DEVICE BONDED!222 " + pairedDevice.getDeviceId());
            }else
                Log.d(TAG,"ON DEVICE BONDED!null");
        }

        @Override
        public void onDataRetrieved(final BloodPressure bp) {

        }

        @Override
        public void onDataRetrieved(final Weight weight) {
            Log.d(TAG,"ON WEIGHT DATA RETRIEVED: " + weight.getWeight() + " " + weight.getStringUnit());
            weightList.add(weight);
            //TODOmAAndDMeter.startScanning();
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
            if(status==BluetoothAdapter.STATE_DISCONNECTED
                    ||status== Ble.FORCED_CANCEL){
                Timber.d("OnConnectionStateChanged- DC, weightList.size = " + weightList.size());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //stopDetect();
                        //mAAndDMeter.clear();
                        if(weightList.size() > 0){
                            displayResults();
                        }else{
                            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_ERROR);
                            retryBtn.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        }
    };

    private class uploadWeightDataToServer extends AsyncTask<Void, Void, Void>
    {
        private Weight weight;
        boolean success, isLastData, isManual;
        Date weightDate;

        public uploadWeightDataToServer(Weight weight, boolean isLastData, boolean isManual)
        {
            this.weight = weight;
            this.weightDate = weight.getDate();
            this.isLastData = isLastData;
            this.isManual = isManual;
            this.success = false;
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
                String extraData = "Weight:" + weight.getWeight()
                        + "&Height:" + ""
                        + "&Bmi:" + ""
                        + "&Type:" + weight.getStringUnit();

                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.getInstance().DATE_FULL_FORMAT_UPLOAD, Locale.ENGLISH);

                data.put("DeviceId", LifeCareHandler.getInstance().getMyDeviceID(WeightReadingActivity.this));
                data.put("EntityId", entityId);
                data.put("ExtraData", extraData);
                data.put("EventTypeName", "Weight Update Data");
                data.put("EventTypeId", "20014");
                data.put("Zone", "56398aa0e4b00e308ce460ec");
                data.put("ZoneCode", "OT");
                data.put("CreateDate", sdf.format(weight.getDate()));
                data.put("WriteToSocket",true);
                data.put("WConfigCode", "BLE");

                if(!isManual && pairedDevice!=null) {

                    String gatewayId =  LifeCareHandler.getInstance().getMyDeviceID(WeightReadingActivity.this);
                    SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
                    String serial = weight.getEntityId() + "-" + gatewayId + "-" + pairedDevice.getDeviceId().replace(":","");
                    Log.d(TAG,"SERIAL = " + serial);

                    data.put("NodeId", serial);
                    data.put("NodeName", pairedDevice.getAssignedName());
                    data.put("SystemManufacturerCode", pairedDevice.getManufacturerCode());
                    data.put("SystemProductCode", pairedDevice.getProductCode());
                    data.put("SystemProductTypeCode", pairedDevice.getProductTypeCode());
                    data.put("SmartDeviceId", serial);
                }

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
                Log.d(TAG, "Successful upload weight data: " + success
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
                UnsyncedData.weightList.add(weightDate);
            }

            if(isLastData){
                if(progress!=null && progress.isShowing()){
                    progress.dismiss();
                }
                String message = "";
                if(success){
                    message = "Successfully updated weight data!";
                }else{
                    message = "Problem uploading data! Data have been saved to cache!";
                }
                Toast.makeText(WeightReadingActivity.this, message, Toast.LENGTH_LONG).show();

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

    private YolandaWeightMeterListener mYolandaWeightMeterListener = new YolandaWeightMeterListener() {


        @Override
        public void onDeviceScan(QNBleDevice device) {
            pairedDeviceMacAdd = device.getMac();
            pairedDevice = LocalMeasurementDevicesHandler.getInstance().getDeviceByMacAddress(pairedDeviceMacAdd);

            if (pairedDevice != null) {
                mYolandaWeightMeter.stopScanning();
                mYolandaWeightMeter.connectBluetoothDevice(device);
            }
        }

        @Override
        public void onReadResult(double weigh) {
            Weight weight = new Weight(weigh, Calendar.getInstance().getTime(),
                    WeightMeasurementProfile.UNIT_SI);
            weight.setEntityId(entityId);

            weightList.add(weight);
        }

        @Override
        public void onConnectionStateChanged(int status) {
            if (weightList.size() > 0) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayResults();
                    }
                });


            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_ERROR);
                        retryBtn.setVisibility(View.VISIBLE);
                        addDeviceBtn.setVisibility(View.GONE);
                        tvDesc.setText("Error");
                    }
                });

            }
        }
    };
}
