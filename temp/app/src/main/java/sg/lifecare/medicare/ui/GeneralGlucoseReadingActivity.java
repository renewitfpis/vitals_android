package sg.lifecare.medicare.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.calendardatepicker.MonthAdapter;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.VitalConfigs;
import sg.lifecare.medicare.ble.AccuChekDeviceMeter;
import sg.lifecare.medicare.ble.BleException;
import sg.lifecare.medicare.ble.aandd.AAndDMeter;
import sg.lifecare.medicare.ble.aandd.AAndDMeterListener;
import sg.lifecare.medicare.ble.profile.AbstractProfile;
import sg.lifecare.medicare.ble.vivachek.VivaChekBloodGlucoseMeter;
import sg.lifecare.medicare.ble.vivachek.VivaChekBloodGlucoseMeterListener;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.database.sync.NetworkChangeReceiver;
import sg.lifecare.medicare.database.sync.SyncHandler;
import sg.lifecare.medicare.database.sync.UnsyncedData;
import sg.lifecare.medicare.object.LocalMeasurementDevicesHandler;
import sg.lifecare.medicare.ui.adapter.GlucoseReadingAdapter;
import sg.lifecare.medicare.ui.pairing.DevicePairingMenuActivity;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.ui.view.PairingIndicatorView;
import sg.lifecare.medicare.utils.BleUtil;
import sg.lifecare.medicare.utils.HtmlUtil;
import sg.lifecare.medicare.utils.LifeCareHandler;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import sg.lifecare.medicare.utils.NfcUtil;
import sg.lifecare.medicare.utils.TagDetector;
import timber.log.Timber;


/**
 * Created by janice on 20/6/16.
 */
public class GeneralGlucoseReadingActivity extends AppCompatActivity implements CalendarDatePickerDialogFragment.OnDateSetListener, RadialTimePickerDialogFragment.OnTimeSetListener {

    int REQUEST_ENABLE_NFC = 125;
    int REQUEST_ENABLE_BT = 126;

    private static final String TAG = "GlucoseReadingActivity";
    private static final String FRAG_TAG_DATE_PICKER = "fragment_date_picker_name";
    private static final String FRAG_TAG_TIME_PICKER = "fragment_time_picker_name";
    private static final String DATE_FORMAT = "dd MMM yyyy";
    private static final String TIME_FORMAT = "HH:mm";

    private SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
    private SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT, Locale.ENGLISH);

    private AlertDialog mBleEnableDialog;
    private EditText etRemarks, etSugarCon;
    private Button addDeviceBtn, retryBtn;
    private TextView selectedDate, selectedTime;
    private TextView tvDesc;
    private ImageView autoDataEntryImage;
    private Spinner unitSpinner, measuredSpinner;
    private Calendar setDate;
    private PairingIndicatorView pairingIndicator;
    private CustomToolbar mToolbar;
    private ProgressDialog progress;

    private AccuChekDeviceMeter deviceMeter;
    private ArrayList<Terumo> terumoList = new ArrayList<>();
    private TagDetector mTagDetector;
    private Realm mRealm;
    private boolean isDevicePaired = false;
    private String nfcId;
    private boolean hasDisplayed;
    private String entityId;
    private AlertDialog mNfcEnableDialog;
    private GlucoseReadingAdapter adapter;
    private MedicalDevice pairedDevice;
    private MedicalDevice pairedDeviceNfc;
    private MedicalDevice pairedDeviceBle;
    private boolean isModeSet = false;

    boolean hasTerumo = false;
    boolean hasAccuChek = false;
    boolean isListDisplayed = false;
    enum Mode{
        NFC, BLE, NFC_AND_BLE
    }
    Mode mode = Mode.NFC;
    CountDownTimer timer;

    private VivaChekBloodGlucoseMeter mVivaChekBloodGlucoseMeter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_device_reading);
        ViewStub stub = (ViewStub) findViewById(R.id.manual_entry_view);
        stub.setLayoutResource(R.layout.view_manual_entry_bg);

        timer = new CountDownTimer(2000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Timber.d("DISCONNECTED!!!! display res b");
                        if (terumoList.size() > 0) {
                            Timber.d("DISCONNECTED!!!! display res TEMP LIST > 0");
                            if(!isListDisplayed) {
                                displayList();
                                isListDisplayed = true;
                            }
                        }else{
                            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_ERROR);
                            retryBtn.setVisibility(View.VISIBLE);
                            Timber.d("DISCONNECTED!!!! display res TEMP LIST smaller than 0");
                        }
                    }
                });
            }
        };
        mBleEnableDialog = BleUtil.enableBleDialogBuilder(this,
                mBleSettingsButtonListener, mBleCancelButtonListener).create();

        final View inflated = stub.inflate();
        inflated.setVisibility(View.INVISIBLE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = getIntent();
        if(intent.hasExtra("device")) {
            pairedDevice = (MedicalDevice) intent.getSerializableExtra("device");
        }

        if(pairedDevice==null) {
           /* new AlertDialog.Builder(this)
                    .setTitle("Select Device")
                    .setMessage("Read glucose data from NFC or BLE Device?")
                    .setNegativeButton("NFC", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mode = Mode.NFC;
                            isModeSet = true;
                            onResume();
                        }
                    })
                    .setPositiveButton("BLE", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mode = Mode.BLE;
                            isModeSet = true;
                            onResume();
                        }
                    })
                    .setCancelable(false)
                    .show();*/
        }else{
            if(pairedDevice.getModel() == Model.ACCU_CHEK_AVIVA_CONNECT || pairedDevice.getModel() == Model.VIVACHEK_INO_SMART){
                mode = Mode.BLE;
                isModeSet = true;
            }else if(pairedDevice.getModel() == Model.TERUMO_MEDISAFE_FIT){
                mode = Mode.NFC;
                isModeSet = true;
            }
        }

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.uploading_data));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCancelable(false);

        SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
        entityId = sh.getString("entity_id", "");

        mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

        measuredSpinner = (Spinner) findViewById(R.id.measured_spinner);
        unitSpinner = (Spinner) findViewById(R.id.blood_glucose_spinner);

        etSugarCon = (EditText) findViewById(R.id.edit_blood_sugar);
        etRemarks = (EditText) findViewById(R.id.edit_remarks);
        etRemarks.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                ((ScrollView)findViewById(R.id.scrollView)).requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        tvDesc = (TextView) findViewById(R.id.description);
        selectedDate = (TextView) findViewById(R.id.date_selection);
        selectedTime = (TextView) findViewById(R.id.time_selection);

        autoDataEntryImage = (ImageView) findViewById(R.id.automatic_data_entry_image);

        findViewById(R.id.auto_entry_view).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mode == Mode.NFC) {
                    if (!NfcUtil.isNfcEnabled(GeneralGlucoseReadingActivity.this)) {
                        requestNfc();
                    }
                }else{
                    if (!BleUtil.isBleEnabled()){
                        requestBle();
                    }
                }
            }
        });

        retryBtn = (Button) findViewById(R.id.retry_button);
        retryBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                //if(mode == Mode.BLE) {
                    getLocalConnectedDevices();
                //}

                retryBtn.setVisibility(View.GONE);
            }
        });

        pairingIndicator = (PairingIndicatorView) findViewById(R.id.pairing_indicator);
        pairingIndicator.setProductImage(R.drawable.glucose_icon);

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
                //if(!isDevicePaired){
                    startPairingActivity();
                    /*if(mode == Mode.NFC) {
                        if (NfcUtil.isNfcEnabled(GeneralGlucoseReadingActivity.this)) {
                            startPairingActivity();
                        } else {
                            requestNfc();
                        }
                    }else{
                        if (BleUtil.isBleEnabled()) {
                            startPairingActivity();
                        } else {
                            requestBle();
                        }
                    }*/
                //}
            }
        });
        mNfcEnableDialog = NfcUtil.enableNfcDialogBuilder(this,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestNfc();
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mNfcEnableDialog.dismiss();
                    }
                }).create();
        selectedDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: onCalendarDatePickerPressed();
            }
        });
        selectedTime.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onTimePressed();
            }
        });

        mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_activity_glucose_reading);
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.setRightButtonImage(R.drawable.ic_toolbar_tick);
        mToolbar.hideRightButton();
        mToolbar.hideSecondRightButton();
        mToolbar.setListener(mToolbarListener);

        currentTimeAndDate();
        BloodSugarSpinner();
        MeasuredSpinner();

        //Check if device is paired
        //new getConnectedDeviceList().execute();

        mTagDetector = new TagDetector(this, "0", entityId);
        mTagDetector.setOnTagDetectedListener(new TagDetector.TagDetectorListener() {
            @Override
            public void onDataDetectionStarted() {
                Log.e(TAG, "onDataDetectionStarted!! clearing terumo list");
                terumoList.clear();
            }

            @Override
            public void onDataDetected(final Terumo terumo) {
                if(pairedDeviceNfc!=null) {
                    pairedDevice = pairedDeviceNfc;
                }
                terumoList.add(terumo);
                Log.d(TAG, "Detected data!! " + terumo.getValue() + ", " + terumo.isBeforeMeal());
            }

            @Override
            public void onDataDetectionCompleted() {
                Log.d(TAG, "Finish detection!");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayList();
                    }
                });
            }

            @Override
            public void onTagDetected(String id) {
                Log.d(TAG,"On Tag Detected!");
                nfcId = id;
                //new commissionBLEDevice().execute();
            }
        });

        //checkIsTerumoPaired();

        CalendarDatePickerDialogFragment datePickerFrag =
                (CalendarDatePickerDialogFragment) getSupportFragmentManager()
                        .findFragmentByTag(FRAG_TAG_DATE_PICKER);

        if (datePickerFrag != null) {
            datePickerFrag.setOnDateSetListener(GeneralGlucoseReadingActivity.this);
        }

        RadialTimePickerDialogFragment timePickerFrag =
                (RadialTimePickerDialogFragment) getSupportFragmentManager().
                        findFragmentByTag(FRAG_TAG_TIME_PICKER);

        if (timePickerFrag != null) {
            timePickerFrag.setOnTimeSetListener(GeneralGlucoseReadingActivity.this);
        }
    }

    private void getLocalConnectedDevices(){

        pairingIndicator.setProductImage(R.drawable.glucose_icon);

        ArrayList<MedicalDevice> list =
                LocalMeasurementDevicesHandler.getInstance().getConnectedMedicalDeviceList();

        for (MedicalDevice device : list) {
            if(device.getModel()==Model.ACCU_CHEK_AVIVA_CONNECT || device.getModel() == Model.VIVACHEK_INO_SMART) {
                hasAccuChek = true;
                pairingIndicator.setProductImage(R.drawable.accu_chek_ac);
                isDevicePaired = true;
                isModeSet = true;
                mode = Mode.BLE;
                try {
                    pairedDeviceBle = (MedicalDevice) device.clone();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
            if(device.getModel()==Model.TERUMO_MEDISAFE_FIT){
                hasTerumo = true;
                isDevicePaired = true;
                isModeSet = true;
                mode = Mode.NFC;
                try {
                    pairedDeviceNfc = (MedicalDevice) device.clone();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }

        if(isDevicePaired){
            if(hasTerumo && hasAccuChek){
                mode = Mode.NFC_AND_BLE;
                pairingIndicator.setProductImage(R.drawable.glucose_icon);
                if(!NfcUtil.isNfcEnabled(GeneralGlucoseReadingActivity.this)
                        && !BleUtil.isBleEnabled()) {
                    Timber.d("NFC & BLE is not turned on");

                    pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_NFC_BLE_OFF);
                    //pairingIndicator.setPairingDescription(R.string.pairing_nfc_ble_not_connected);
                    pairingIndicator.showPairingDescriptionText();
                    return;
                }else if(!NfcUtil.isNfcEnabled(GeneralGlucoseReadingActivity.this)){
                    pairingIndicator.setProductImage(R.drawable.accu_chek_ac);
                    startDetect();
                }else if(!BleUtil.isBleEnabled()) {
                    pairingIndicator.setProductImage(R.drawable.glucose_icon);
                    startTagDetect();
                }else if(NfcUtil.isNfcEnabled(GeneralGlucoseReadingActivity.this) && BleUtil.isBleEnabled()){
                    startDetect();
                    startTagDetect();
                }
            }else if(hasTerumo){
                pairedDevice = pairedDeviceNfc;
                pairingIndicator.setProductImage(R.drawable.glucose_icon);
                if(!NfcUtil.isNfcEnabled(GeneralGlucoseReadingActivity.this)) {
                    tvDesc.setText(R.string.pairing_nfc_not_connected);
                    pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_NFC_OFF);
                    pairingIndicator.showPairingDescriptionText();
                    return;
                }
                startTagDetect();
            }else if(hasAccuChek){
                pairedDevice = pairedDeviceBle;
                pairingIndicator.setProductImage(R.drawable.accu_chek_ac);

                if (!BleUtil.isBleEnabled()) {
                    tvDesc.setText(R.string.pairing_bluetooth_not_connected);
                    pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_BLE_OFF);
                    pairingIndicator.showPairingDescriptionText();
                    addDeviceBtn.setVisibility(View.VISIBLE);
                    retryBtn.setVisibility(View.GONE);
                    return;
                }

                startDetect();
            }
            tvDesc.setText(R.string.device_info_retrieve_data);
            pairingIndicator.setPairingMode(PairingIndicatorView.READING_BLE);
            addDeviceBtn.setVisibility(View.INVISIBLE);
        }
        else {
            tvDesc.setText(R.string.device_info_no_pair_device);
            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_NO_DEVICE);
            addDeviceBtn.setVisibility(View.VISIBLE);
            retryBtn.setVisibility(View.GONE);
        }
    }

    public void startDetect() {
        try{
            if (LocalMeasurementDevicesHandler.getInstance().hasConnectedModel(Model.ACCU_CHEK_AVIVA_CONNECT)) {

                if (deviceMeter == null) {
                    deviceMeter = new AccuChekDeviceMeter(this, mMeterListener,
                            Model.ACCU_CHEK_AVIVA_CONNECT, "");
                    deviceMeter.setMode(AAndDMeter.MODE_READING);
                }

                if (pairedDeviceBle != null) {
                    deviceMeter.connectDevice(pairedDeviceBle.getDeviceId());
                } else {
                    Timber.e("Problem getting connected ble device!");
                }

                Timber.d("111 DEVICE ID = " + pairedDeviceBle.getDeviceId());
            }

            if (LocalMeasurementDevicesHandler.getInstance().hasConnectedModel(
                    Model.VIVACHEK_INO_SMART)) {
                if (mVivaChekBloodGlucoseMeter == null) {
                    mVivaChekBloodGlucoseMeter = new VivaChekBloodGlucoseMeter(this, mVivaChekBloodGlucoseMeterListener);
                }

                mVivaChekBloodGlucoseMeter.startScanning();
            }

        } catch (BleException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void stopDetect() {
        Timber.d("Stop detect ac!");
        if(deviceMeter!=null) {
            deviceMeter.clear();
            deviceMeter.stopScanning();
            deviceMeter.disconnect();
            deviceMeter.close();
            deviceMeter = null;
        }

        if (mVivaChekBloodGlucoseMeter != null) {
            mVivaChekBloodGlucoseMeter.stopScanning();
            mVivaChekBloodGlucoseMeter.disconnect();
            mVivaChekBloodGlucoseMeter.close();
        }
        //mTagDetector.stopDetect();
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
        if(requestCode==REQUEST_ENABLE_NFC){
            if(resultCode==RESULT_OK) {
                Log.d(TAG, "NFC ONRESULT !");
                if (isDevicePaired) {
                    startTagDetect();
                } else {
                    startPairingActivity();
                }
            }
        } else if(requestCode==REQUEST_ENABLE_BT){
            Log.d(TAG,"BT ONRESULT !");
            if(resultCode==RESULT_OK) {
                getLocalConnectedDevices();
            }
        }
    }

    private void requestNfc(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
            startActivityForResult(intent,REQUEST_ENABLE_NFC);
        } else{
            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            startActivityForResult(intent,REQUEST_ENABLE_NFC);
        }
    }

    private void requestBle(){
        mBleEnableDialog.show();
    }

    private void startPairingActivity(){
        /*if(mode == Mode.NFC) {
            Intent intent = new Intent(GeneralGlucoseReadingActivity.this,
                    ConvertGatewayActivity.class);
            intent.putExtra("is_glucose", true);
            startActivity(intent);
        }else{
            Intent intent = new Intent(GeneralGlucoseReadingActivity.this,
                    ConvertGatewayActivity.class);
            intent.putExtra("medical_device", MedicalDevice.findModel(Model.ACCU_CHEK_AVIVA_CONNECT));
            startActivity(intent);
        }*/
        Intent intent = new Intent(GeneralGlucoseReadingActivity.this,
                DevicePairingMenuActivity.class);
        intent.putExtra("is_glucose", true);
        startActivity(intent);
    }

    public void currentTimeAndDate() {
        setDate = Calendar.getInstance();
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
                .setPreselectedDate(setDate.get(Calendar.YEAR),setDate.get(Calendar.MONTH),setDate.get(Calendar.DAY_OF_MONTH))
                .setOnDateSetListener(GeneralGlucoseReadingActivity.this);
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

    public void onTimePressed() {
        RadialTimePickerDialogFragment rtpd = new RadialTimePickerDialogFragment()
                .setOnTimeSetListener(GeneralGlucoseReadingActivity.this)
                .setCancelText(getString(R.string.dialog_cancel))
                .setDoneText(getString(R.string.dialog_ok))
                .setStartTime(setDate.get(Calendar.HOUR_OF_DAY),setDate.get(Calendar.MINUTE))
                .setThemeCustom(R.style.MyCustomBetterPickersDialogs);
        rtpd.show(getSupportFragmentManager(), FRAG_TAG_TIME_PICKER);
    }

    public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
        String time = String.format(Locale.ENGLISH, ("%02d:%02d"), hourOfDay, minute);
        selectedTime.setText(time);
        setDate.set(
                setDate.get(Calendar.YEAR),
                setDate.get(Calendar.MONTH),
                setDate.get(Calendar.DAY_OF_MONTH),
                hourOfDay,
                minute
        );
    }

    private void checkIsTerumoPaired(){

        pairingIndicator.setProductImage(R.drawable.glucose_icon);

        ArrayList<MedicalDevice> mds = LocalMeasurementDevicesHandler.getInstance().getConnectedMedicalDeviceList();
        for (MedicalDevice device : mds) {
            if(device.getModel()==Model.TERUMO_MEDISAFE_FIT){
                isDevicePaired = true;
                try {
                    pairedDevice = (MedicalDevice) device.clone();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }

        if(isDevicePaired){
            tvDesc.setText(R.string.glucose_info_tap_device);
            autoDataEntryImage.setImageResource(R.drawable.pairing_glucometer_reading);
            pairingIndicator.setPairingMode(PairingIndicatorView.READING_NFC);
            pairingIndicator.setPairingDescription(R.string.glucose_info_tap_device);
            pairingIndicator.setPairingDescriptionImage(R.drawable.pairing_glucometer_tap_phone);
            pairingIndicator.showPairingDescription();
            addDeviceBtn.setVisibility(View.INVISIBLE);

            if(!NfcUtil.isNfcEnabled(GeneralGlucoseReadingActivity.this)) {
                mNfcEnableDialog.show();
            }else{
                startTagDetect();
            }
        }
        else {
            if(!NfcUtil.isNfcEnabled(GeneralGlucoseReadingActivity.this)) {
                tvDesc.setText(R.string.pairing_nfc_not_connected);
                pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_NFC_OFF);
            }else {
                tvDesc.setText(R.string.device_info_no_pair_device);
                pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_NO_DEVICE);
            }
            autoDataEntryImage.setImageResource(R.drawable.pairing_glucometer);
            addDeviceBtn.setVisibility(View.VISIBLE);
        }
    }

    public void onResume() {
        super.onResume();
        Timber.d("On Resume");
        if (mRealm == null)
            mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

        getLocalConnectedDevices();
       /* if(isModeSet) {
            if (mode == Mode.NFC) {
                checkIsTerumoPaired();
            } else {
                getLocalConnectedDevices();
            }
        }*/
    }

    public void displayList() {
        final Dialog dlg = new Dialog(this);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.dialog_bp, null);
        TextView done = (TextView) view.findViewById(R.id.done_button);
        TextView cancel = (TextView) view.findViewById(R.id.cancel_button);

        dlg.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                isListDisplayed = false;
            }
        });
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dlg.cancel();
                terumoList.clear();
                stopDetect();
                stopTagDetect();
                onResume();
            }
        });
        done.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dlg.cancel();

                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_DISPLAY_FORMAT);
                //find latest date
                Timber.d("Terumo list size = " + terumoList.size());
                for (int i = 0; i < terumoList.size(); i++) {
                    if(adapter.selected[i]) {
                        if (!PatientData.getInstance().isTerumoExistsByDate(mRealm, terumoList.get(i).getDate())) {
                            PatientData.getInstance().addTerumo(mRealm, terumoList.get(i));

                            Timber.d("Added new record: (val) " + terumoList.get(i).getValue()
                                    + " (date) " + sdf.format(terumoList.get(i).getDate()));

                            boolean isLastData = false;
                            //push data to server
                            if (i == terumoList.size() - 1) {
                                isLastData = true;
                            }

                            new uploadDataToServer(terumoList.get(i), false, isLastData).execute();

                        } else {
                            Timber.d("Found existing record: (val) " + terumoList.get(i).getValue()
                                    + " (date) " + sdf.format(terumoList.get(i).getDate()));

                            if (i == terumoList.size() - 1) {
                                onDataUpdated();
                            }
                        }
                    }

                }
                terumoList.clear();
            }
        });

        adapter = new GlucoseReadingAdapter(
                GeneralGlucoseReadingActivity.this, R.layout.glucose_result_item, terumoList
        );
        ListView listView = (ListView) view.findViewById(R.id.list_view);
        listView.setAdapter(adapter);

        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlg.setCanceledOnTouchOutside(true);
        dlg.setCancelable(true);
        dlg.setContentView(view);
        if(!this.isFinishing()) {
            dlg.show();
        }
    }

    private void BloodSugarSpinner() {
        String[] glucoseLevel = new String[]{"mmol/L","mg/dL"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, glucoseLevel);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        unitSpinner.setAdapter(adapter);
    }

    private void MeasuredSpinner() {
        String[] measuredList = new String[]{
                getResources().getString(R.string.display_info_before_meal),
                getResources().getString(R.string.display_info_after_meal)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, measuredList);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        measuredSpinner.setAdapter(adapter);
    }

    public void getData() {
        String bloodGlucose = etSugarCon.getText().toString().trim();
        if (bloodGlucose.isEmpty()) {
            etSugarCon.setError("Please enter sugar concentration");
            return;
        }

        String remarks = HtmlUtil.encodeString(etRemarks.getText().toString().trim());

        double bloodGlucoseValue = Double.parseDouble(bloodGlucose);
        int selectedUnit = unitSpinner.getSelectedItemPosition();
        if(selectedUnit == 0){
            if(bloodGlucoseValue >= 50){
                etSugarCon.setError(getResources().getString(R.string.error_msg_overly_high_glucose));
                return;
            }
        }
        else if (selectedUnit == 1) {
            bloodGlucoseValue = bloodGlucoseValue / 18;
            //trick to round up to 2 decimal places
            bloodGlucoseValue = bloodGlucoseValue * 100;
            bloodGlucoseValue = Math.round(bloodGlucoseValue);
            bloodGlucoseValue = bloodGlucoseValue / 100;
        }

        Terumo terumo = new Terumo();
        terumo.setValue(bloodGlucoseValue);
        terumo.setUnit(0);
        terumo.setManual(true);
        terumo.setDate(setDate.getTime());
        terumo.setBeforeMeal(measuredSpinner.getSelectedItemPosition() == 0);
        terumo.setRemark(remarks);
        terumo.setEntityId(entityId);

        Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
        PatientData.getInstance().addTerumo(realm, terumo);
        realm.close();

        if(NetworkChangeReceiver.isInternetAvailable(this)){
            new uploadDataToServer(terumo,true,true).execute();
            Timber.d("glucose - Internet is available");
        }
        else{
            SyncHandler.registerNetworkBroadcast();
            UnsyncedData.glucoseList.add(setDate.getTime());
            onDataUpdated();
            Timber.d("Added glucose reading to unsyc");
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

        stopTagDetect();
        stopDetect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopTagDetect();
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        resolveIntent(intent);
    }

    private void resolveIntent(Intent intent) {
        String s = intent.getAction();

        Timber.d("resolveIntent: action=" + s);

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(s) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(s) ||
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(s)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            if (tag != null && mTagDetector != null) {
                Log.w("homeActivity", "tag is not null");
                mTagDetector.startReadThread(tag);
            }
        }
    }

    public void startTagDetect() {
        mTagDetector.startDetect();
    }

    public void stopTagDetect() {
        if(mTagDetector!=null) {
            mTagDetector.stopDetect();
        }
    }

    private class uploadDataToServer extends AsyncTask<Void, Void, Void>
    {
        private Terumo terumo;
        private boolean isManualKeyIn;
        private boolean success;
        private boolean isLastData;
        private Date date;

        public uploadDataToServer(Terumo terumo, boolean isManualKeyIn, boolean isLastData)
        {
            this.terumo = terumo;
            this.date = terumo.getDate();
            this.isManualKeyIn = isManualKeyIn;
            this.isLastData = isLastData;
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
                Log.d(TAG, "Trying to upload glucose data");
                JSONObject data = new JSONObject();

                String meal;
                if(terumo.isBeforeMeal()!=null){
                    meal = terumo.isBeforeMeal() ? "BeforeMeal" : "AfterMeal";
                }else {
                    meal = "BeforeMeal";
                }
                String extraData = "Concentration:" + terumo.getValue()
                        + "&Unit:"+ terumo.getStringUnit()
                        + "&SampleLocation:" + "Finger"
                        + "&Type:"+ meal
                        + "&StatusAnnunciation:" + "N/A"
                        + "&Remarks:" + terumo.getRemark();

                MedicalDevice device = MedicalDevice.findModel(Model.TERUMO_MEDISAFE_FIT);

                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT_UPLOAD, Locale.ENGLISH);

                data.put("DeviceId", LifeCareHandler.getInstance().getMyDeviceID(GeneralGlucoseReadingActivity.this));
                data.put("EntityId", entityId);
                data.put("ExtraData", extraData);
                data.put("EventTypeName", "Gluco Update Data");
                data.put("EventTypeId", "20015"); //blood glucose
                data.put("Zone", "56398aa0e4b00e308ce460ec");
                data.put("ZoneCode", "OT");
                data.put("CreateDate", sdf.format(terumo.getDate()));
                data.put("WriteToSocket",true);

                if(!isManualKeyIn) {
                    /*String gatewayId =  LifeCareHandler.getInstance().getMyDeviceID(GeneralGlucoseReadingActivity.this);
                    SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
                    entityId = sh.getString("entity_id", "");
                    String serial = entityId + "-" + gatewayId + "-" + nfcId.replace(":","");*/
                    //TODO: test nfc id /serial num

                    data.put("NodeId", pairedDevice.getDeviceId());
                    data.put("NodeName", pairedDevice.getAssignedName());
                    data.put("WConfigCode", pairedDevice.getMode());
                    data.put("SystemManufacturerCode", pairedDevice.getManufacturerCode());
                    data.put("SystemProductCode", pairedDevice.getProductCode());
                    data.put("SystemProductTypeCode", pairedDevice.getProductTypeCode());
                    data.put("SmartDeviceId", pairedDevice.getDeviceId());
                }

                String jsonString = data.toString();

                Log.w(TAG, "Uploading BG Data To Server : " + jsonString);

                Request request = new Request.Builder()
                        .url(VitalConfigs.URL + "mlifecare/event/addEvent")
                        .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                        .build();
                //https://www.lifecare.sg/mlifecare/event/addEvents

                Response response = LifeCareHandler.getInstance().okclient.newCall(request).execute();

                JSONObject json = new JSONObject(response.body().string());
                success = !json.getBoolean("Error"); //Error = true if contains error
                //success = response.isSuccessful();
                Log.d(TAG, "Successful upload: " + success + " , " + json.getString("ErrorDesc"));
            } catch(JSONException ex) {
                Log.e(TAG, ex.getMessage(), ex);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            } catch (Exception e2){
                e2.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void res){

            if(isLastData){
                if(progress!=null && progress.isShowing()){
                    progress.dismiss();
                }
                String message = "";
                if(success){
                    message = "Successfully updated glucose data!";
                }else{
                    message = "Problem uploading data! Data have been saved to cache!";
                    UnsyncedData.glucoseList.add(date);
                }
                Toast.makeText(GeneralGlucoseReadingActivity.this, message, Toast.LENGTH_LONG).show();

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


    private AAndDMeterListener mMeterListener = new AAndDMeterListener() {
        @Override
        public void onResult(List<AbstractProfile> records) {
            Log.d(TAG,"RESULTS RETRIEVED! " + records.size());
        }

        @Override
        public void onDeviceBonded(final BluetoothDevice device) {
            Log.d(TAG,"ON DEVICE BONDED! ");
            nfcId = device.getAddress();
            //pairedDevice = LocalMeasurementDevicesHandler.getInstance().getDeviceByMacAddress(pairedDeviceMacAdd);
        }

        @Override
        public void onDataRetrieved(final BloodPressure bp) {

        }

        @Override
        public void onDataRetrieved(final Weight weight) {

        }

        @Override
        public void onDataRetrieved(final Object object) {
            if(object instanceof Terumo){
                if(pairedDeviceBle!=null) {
                    pairedDevice = pairedDeviceBle;
                }
                ((Terumo)object).setEntityId(entityId);
                final Terumo terumo = (Terumo) object;
                Log.d(TAG, "ON GLUCOSE DATA RETRIEVED: " + terumo.getValue() + " " + terumo.getStringUnit());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Timber.d("TERUMO EntityId = " + terumo.getEntityId());
                        terumoList.add(terumo);
                        if(hasTerumo&&hasAccuChek){
                            pairingIndicator.setProductImage(R.drawable.accu_chek_ac);
                        }
                    }
                });
                timer.cancel();
                timer.start();
            }
        }

        @Override
        public void onInvalidDataReturned() {
            //deviceMeter.startScanning();
            Timber.e("INVALID DATA RETURNED");
        }

        @Override
        public void onConnectionStateChanged(int status) {
            if(status== BluetoothAdapter.STATE_DISCONNECTED){
                Timber.d("DISCONNECTED!!!! display res");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Timber.d("DISCONNECTED!!!! display res b");
                        if (terumoList.size() > 0) {
                            Timber.d("DISCONNECTED!!!! display res TEMP LIST > 0");
                            //pairingIndicator.setPairingMode(PairingIndicatorView.READING_SUCCESS);
                            //tvDesc.setText("Results Retrieved");
                            if(!isListDisplayed) {
                                displayList();
                                isListDisplayed = true;
                            }
                        }else{
                            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_ERROR);
                            retryBtn.setVisibility(View.VISIBLE);
                            Timber.d("DISCONNECTED!!!! display res TEMP LIST smaller than 0");
                            /*else {
                            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_ERROR);
                            retryBtn.setVisibility(View.VISIBLE);
                            addDeviceBtn.setVisibility(View.GONE);
                            tvDesc.setText("Error");
                        }*/
                        }
                    }
                });

            }
        }
    };

    private VivaChekBloodGlucoseMeterListener mVivaChekBloodGlucoseMeterListener = new VivaChekBloodGlucoseMeterListener() {

        @Override
        public void onDeviceScan(BluetoothDevice device) {
            pairedDeviceBle = LocalMeasurementDevicesHandler.getInstance().getDeviceByMacAddress(device.getAddress());

            if (pairedDeviceBle != null) {

                mVivaChekBloodGlucoseMeter.stopScanning();
                mVivaChekBloodGlucoseMeter.connectDevice(device.getAddress());
            }
        }

        @Override
        public void onReadResult(List<VivaChekBloodGlucoseMeter.CustomerHistory> histories) {
            terumoList.clear();

            if (histories.size() > 0) {
                Terumo terumo = new Terumo();
                terumo.setValue(histories.get(0).getResult());
                terumo.setUnit(0);
                terumo.setManual(true);
                terumo.setDate(histories.get(0).getTimestamp());
                terumo.setBeforeMeal(histories.get(0).isBeforeMeal());
                terumo.setEntityId(entityId);

                terumoList.add(terumo);
            }

            mVivaChekBloodGlucoseMeter.disconnect();
        }

        @Override
        public void onConnectionStateChanged(int status) {
            if(status == BluetoothAdapter.STATE_DISCONNECTED){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (terumoList.size() > 0) {
                            if(!isListDisplayed) {
                                displayList();
                                isListDisplayed = true;
                            }
                        } else {
                            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_ERROR);
                            retryBtn.setVisibility(View.VISIBLE);
                        }
                    }
                });

            }
        }
    };
}
