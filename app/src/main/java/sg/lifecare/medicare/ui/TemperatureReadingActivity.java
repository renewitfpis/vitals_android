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
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.calendardatepicker.MonthAdapter;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import sg.lifecare.medicare.ble.BleException;
import sg.lifecare.medicare.ble.aandd.AAndD201Thermometer;
import sg.lifecare.medicare.ble.aandd.AAndDMeter;
import sg.lifecare.medicare.ble.aandd.AAndDMeterListener;
import sg.lifecare.medicare.ble.jumper.JumperThermometer;
import sg.lifecare.medicare.ble.jumper.JumperThermometerListener;
import sg.lifecare.medicare.ble.profile.AbstractProfile;
import sg.lifecare.medicare.ble.profile.TemperatureMeasurementProfile;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Temperature;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.database.sync.NetworkChangeReceiver;
import sg.lifecare.medicare.database.sync.SyncHandler;
import sg.lifecare.medicare.database.sync.UnsyncedData;
import sg.lifecare.medicare.object.LocalMeasurementDevicesHandler;
import sg.lifecare.medicare.ui.adapter.TempReadingAdapter;
import sg.lifecare.medicare.ui.pairing.ConvertGatewayActivity;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.ui.view.PairingIndicatorView;
import sg.lifecare.medicare.utils.BleUtil;
import sg.lifecare.medicare.utils.LifeCareHandler;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import timber.log.Timber;

public class TemperatureReadingActivity extends AppCompatActivity implements CalendarDatePickerDialogFragment.OnDateSetListener, RadialTimePickerDialogFragment.OnTimeSetListener {

    private static final String TAG = "TempReadingActivity";
    private static final String FRAG_TAG_DATE_PICKER = "fragment_date_picker_name";
    private static final String FRAG_TAG_TIME_PICKER = "fragment_time_picker_name";
    private static final String DATE_FORMAT = "dd MMM yyyy";
    private static final String TIME_FORMAT = "HH:mm";
    private static final int REQUEST_ENABLE_BT = 123;

    private Dialog dlg = null;
    private SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
    private SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT, Locale.ENGLISH);

    private RelativeLayout readingLayout;
    private LinearLayout pairingLayout;
    private ImageView dateBtn, timeBtn, autoDataEntryImage;
    private TextView selectedDate, selectedTime;
    private TextView tvReading, tvDate, tvDesc;
    private EditText etTemp;
    private Spinner unitSpinner;
    private Calendar setDate;

    private Realm mRealm;
    private AAndDMeter mAAndDMeter;
    private AlertDialog mBleEnableDialog;
    private boolean isDevicePaired = false;
    private String entityId;
    private PairingIndicatorView pairingIndicator;
    private Button addDeviceBtn, retryBtn;
    private ArrayList<Temperature> tempList;
    private TempReadingAdapter adapter;
    private ProgressDialog progress;

    private String pairedDeviceMacAdd;
    private MedicalDevice pairedDevice;

    private JumperThermometer mJumperThermometer;
    private double mTemperature = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_reading);

        tempList = new ArrayList<>();
        //setContentView(R.layout.activity_temp_reading);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.uploading_data));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCancelable(false);

        final CustomToolbar mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_activity_temperature_reading);
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.setRightButtonImage(R.drawable.ic_toolbar_tick);
        mToolbar.hideSecondRightButton();
        mToolbar.setListener(mToolbarListener);

        ViewStub stub = (ViewStub) findViewById(R.id.manual_entry_view);
        stub.setLayoutResource(R.layout.view_manual_entry_temp);
        final View inflated = stub.inflate();
        inflated.setVisibility(View.INVISIBLE);

        pairingIndicator = (PairingIndicatorView) findViewById(R.id.pairing_indicator);
        pairingIndicator.setProductImage(R.drawable.thermometer);
        pairingIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!BleUtil.isBleEnabled()){
                    mBleEnableDialog.show();
                }
            }
        });

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
        timeBtn             = (ImageView) findViewById(R.id.time_btn);
        dateBtn             = (ImageView) findViewById(R.id.calendar_btn);
        retryBtn            = (Button) findViewById(R.id.retry_button);
        autoDataEntryImage  = (ImageView) findViewById(R.id.auto_data_entry_image);
        unitSpinner         = (Spinner) findViewById(R.id.unit_spinner);
        etTemp              = (EditText) findViewById(R.id.edit_temp);

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

        retryBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getLocalConnectedDevices();
                retryBtn.setVisibility(View.GONE);
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
                        Intent intent = new Intent(TemperatureReadingActivity.this,
                                ConvertGatewayActivity.class);
                        intent.putExtra("medical_device",MedicalDevice.findModel(Model.AANDD_UT_201));
                        //intent.putExtra("is_glucose",true);
                        startActivity(intent);
                    }
                }
            }
        });


        currentTimeAndDate();
        BloodSugarSpinner();
        //Check if device is paired
        //new getConnectedDeviceList().execute();
        //getLocalConnectedDevices();

        CalendarDatePickerDialogFragment datePickerFrag =
                (CalendarDatePickerDialogFragment) getSupportFragmentManager()
                        .findFragmentByTag(FRAG_TAG_DATE_PICKER);

        if (datePickerFrag != null) {
            datePickerFrag.setOnDateSetListener(TemperatureReadingActivity.this);
        }

        RadialTimePickerDialogFragment timePickerFrag =
                (RadialTimePickerDialogFragment) getSupportFragmentManager().
                        findFragmentByTag(FRAG_TAG_TIME_PICKER);

        if (timePickerFrag != null) {
            timePickerFrag.setOnTimeSetListener(TemperatureReadingActivity.this);
        }
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
                .setPreselectedDate(setDate.get(Calendar.YEAR),
                        setDate.get(Calendar.MONTH),setDate.get(Calendar.DAY_OF_MONTH))
                .setOnDateSetListener(TemperatureReadingActivity.this);
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

        if (mRealm == null)
            mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

        getLocalConnectedDevices();
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
            //getLocalConnectedDevices();
        }
    }

    private void getLocalConnectedDevices(){
        if (!BleUtil.isBleEnabled()) {
            tvDesc.setText(R.string.pairing_bluetooth_not_connected);
            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_BLE_OFF);
            addDeviceBtn.setVisibility(View.VISIBLE);
            retryBtn.setVisibility(View.GONE);
            return;
        }

        ArrayList<MedicalDevice> list =
                LocalMeasurementDevicesHandler.getInstance().getConnectedMedicalDeviceList();

        for (MedicalDevice device : list) {
            if(device.getModel()==Model.AANDD_UT_201 || device.getModel() == Model.JUMPER_FR302){
                isDevicePaired = true;
            }
        }

        if(isDevicePaired){
            tvDesc.setText(R.string.device_info_retrieve_data);
            pairingIndicator.setPairingMode(PairingIndicatorView.READING_BLE);
            addDeviceBtn.setVisibility(View.INVISIBLE);
            startDetect();
        }
        else {
            tvDesc.setText(R.string.device_info_no_pair_device);
            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_NO_DEVICE);
            addDeviceBtn.setVisibility(View.VISIBLE);
            retryBtn.setVisibility(View.GONE);
        }
    }

    public void onTimePressed() {
        RadialTimePickerDialogFragment rtpd = new RadialTimePickerDialogFragment()
                .setOnTimeSetListener(TemperatureReadingActivity.this)
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
        List<String> l = Arrays.<String>asList(getResources().getStringArray(R.array.temperature_units));
        ArrayList<String> weightUnit = new ArrayList<>(l);

        ArrayAdapter adapter = new ArrayAdapter(TemperatureReadingActivity.this,R.layout.spinner_item,weightUnit);
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

    public class CustomSpinnerAdapter extends BaseAdapter implements SpinnerAdapter {
        private final Context activity;
        private ArrayList<String> list;

        public CustomSpinnerAdapter(Context context, ArrayList<String> list) {
            this.list = list;
            activity = context;
        }

        public int getCount() {
            return list.size();
        }

        public Object getItem(int i) {
            return list.get(i);
        }

        public long getItemId(int i) {
            return (long) i;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView text = new TextView(TemperatureReadingActivity.this);
            text.setPadding(16, 16, 16, 16);
            text.setTextSize(18);
            text.setTypeface(null, Typeface.BOLD);
            text.setBackgroundColor(Color.WHITE);
            text.setGravity(Gravity.CENTER_VERTICAL);
            text.setText(list.get(position));
            text.setTextColor(Color.DKGRAY);
            return text;
        }

        public View getView(int i, View view, ViewGroup viewgroup) {
            TextView text = new TextView(TemperatureReadingActivity.this);
            text.setGravity(Gravity.CENTER_VERTICAL);
            text.setPadding(12, 10, 30, 16);
            text.setTextSize(18);
            text.setText(list.get(i));
            text.setTextColor(Color.DKGRAY);
            return text;
        }
    }

    public void getData() {
        String tempReading = etTemp.getText().toString().trim();
         if (tempReading.isEmpty()) {
             etTemp.setError("Please enter temperature reading");
             return;
        }

        double tempVal = Double.parseDouble(tempReading);

        int selectedUnit = unitSpinner.getSelectedItemPosition();

        Temperature temp = new Temperature();
        temp.setValue(tempVal);
        temp.setUnit(unitSpinner.getSelectedItemPosition());
        temp.setDate(setDate.getTime());
        temp.setEntityId(entityId);

        Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
        PatientData.getInstance().addTemperature(realm, temp);
        realm.close();

        if(NetworkChangeReceiver.isInternetAvailable(this)){
            new uploadTempDataToServer(temp,true,true).execute();
            Timber.d("temp - Internet is available");
        }
        else{
            SyncHandler.registerNetworkBroadcast();
            UnsyncedData.tempList.add(setDate.getTime());
            Timber.d("Added temp reading to unsyc");
        }
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK,returnIntent);
        finish();

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
            if(LocalMeasurementDevicesHandler.getInstance().hasConnectedModel(Model.AANDD_UT_201)) {
                if (mAAndDMeter == null) {
                    mAAndDMeter = new AAndD201Thermometer(this, mMeterListener, "");
                    mAAndDMeter.setMode(AAndDMeter.MODE_READING);
                }

                mAAndDMeter.startScanning();
            }

            if (LocalMeasurementDevicesHandler.getInstance().hasConnectedModel(Model.JUMPER_FR302)) {
                if (mJumperThermometer == null) {
                    mJumperThermometer = new JumperThermometer(this, mJumperThermometerListener);
                }
                mJumperThermometer.startScanning();
            }

        } catch (BleException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void stopDetect() {

        if(mAAndDMeter!=null) {
            mAAndDMeter.clear();
            mAAndDMeter.stopScanning();
            mAAndDMeter.disconnect();
            mAAndDMeter.close();
            mAAndDMeter = null;
        }

        if (mJumperThermometer != null) {
            mJumperThermometer.stopScanning();
            mJumperThermometer.disconnect();
            mJumperThermometer.close();
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
            Log.d(TAG,"ON DEVICE BONDED! ");
            pairedDeviceMacAdd = device.getAddress();
            pairedDevice = LocalMeasurementDevicesHandler.getInstance().getDeviceByMacAddress(pairedDeviceMacAdd);
        }

        @Override
        public void onDataRetrieved(final BloodPressure bp) {
            Log.d(TAG,"ON BP DATA RETRIEVED: " + bp.getSystolic() + "/" + bp.getDistolic());

            startDetect();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pairingLayout.setVisibility(View.GONE);
                    readingLayout.setVisibility(View.VISIBLE);
                    tvReading.setText(bp.getSystolic() + "/" + bp.getDistolic() + " " + bp.getStringUnit() );
                    tvDate.setText(bp.getStringDate());
                }
            });
        }

        @Override
        public void onDataRetrieved(final Weight weight) {
            Log.d(TAG,"ON WEIGHT DATA RETRIEVED: " + weight.getWeight() + " " + weight.getStringUnit());

            startDetect();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pairingLayout.setVisibility(View.GONE);
                    readingLayout.setVisibility(View.VISIBLE);
                    tvReading.setText(weight.getWeight() + " " + weight.getStringUnit() );
                    tvDate.setText(weight.getStringDate());
                }
            });
        }

        @Override
        public void onDataRetrieved(final Object object) {
            if(object instanceof Temperature) {
                final Temperature temp = (Temperature)object;
                Log.d(TAG, "ON TEMP DATA RETRIEVED: " + temp.getValue() + " " + temp.getStringUnit());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tempList.add(temp);
                    }
                });
            }
        }

        @Override
        public void onInvalidDataReturned() {
            //mAAndDMeter.startScanning();
            Timber.e("INVALID DATA RETURNED");
        }

        @Override
        public void onConnectionStateChanged(int status) {
            if(status==BluetoothAdapter.STATE_DISCONNECTED){
                Timber.d("DISCONNECTED!!!! display res");
                    runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Timber.d("DISCONNECTED!!!! display res b");
                        if (tempList.size() > 0) {
                            Timber.d("DISCONNECTED!!!! display res TEMP LIST > 0");
                            //pairingIndicator.setPairingMode(PairingIndicatorView.READING_SUCCESS);
                            //tvDesc.setText("Results Retrieved");
                            displayResults();
                        }else{
                            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_ERROR);
                            retryBtn.setVisibility(View.VISIBLE);
                            addDeviceBtn.setVisibility(View.GONE);
                            tvDesc.setText("Error");

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


    private void displayResults(){

        Timber.d("DISCONNECTED!!!! display res2");

        dlg = new Dialog(TemperatureReadingActivity.this);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.dialog_bp, null);
        TextView done = (TextView) view.findViewById(R.id.done_button);
        TextView cancel = (TextView) view.findViewById(R.id.cancel_button);

        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Timber.d("ON CLICK CANCEL!!!");
                dlg.cancel();
                tempList.clear();
            }
        });
        done.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dlg.cancel();
                Timber.d("ON CLICK DONE!!!");

                //hasDisplayed = false;

                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_DISPLAY_FORMAT, Locale.ENGLISH);
                //find latest date
                Timber.d("weight list size = " + tempList.size());
                for (int i = 0; i < tempList.size(); i++) {
                    if(adapter.selected[i]) {
                        if (!PatientData.getInstance().isTempExistByDate(mRealm, tempList.get(i).getDate())) {
                            PatientData.getInstance().addTemperature(mRealm, tempList.get(i));

                            Timber.d("Added new record: (val) " + tempList.get(i).getValue()
                                    + " (date) " + sdf.format(tempList.get(i).getDate()));

                            boolean isLastData = false;
                            //push data to server
                            if (i == tempList.size() - 1) {
                                isLastData = true;
                            }

                            new uploadTempDataToServer(tempList.get(i),isLastData,false).execute();

                        } else {
                            Timber.d("Found existing record: (val) " + tempList.get(i).getValue()
                                    + " (date) " + sdf.format(tempList.get(i).getDate()));
                        }
                    }
                }

                tempList.clear();
                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_OK,returnIntent);
                finish();

            }
        });

        adapter = new TempReadingAdapter(
                TemperatureReadingActivity.this, R.layout.weight_result_item, tempList
        );

        ListView listView = (ListView) view.findViewById(R.id.list_view);
        listView.setAdapter(adapter);
        tvDesc.setText(tempList.get(0).getValue()+"");
        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlg.setCanceledOnTouchOutside(true);
        dlg.setCancelable(true);
        dlg.setContentView(view);
        if(!isFinishing()) {
            Timber.e("SHOW DIALOG");
            dlg.show();
        }else{
            Timber.e("NOT SHOWING DIALOG -  ACT IS FINISHING");
        }
    }


    private class uploadTempDataToServer extends AsyncTask<Void, Void, Void>
    {
        private Temperature temp;
        boolean success = false, isLastData, isManual;
        Date date;

        public uploadTempDataToServer(Temperature temp, boolean isLastData, boolean isManual)
        {
            this.temp = temp;
            this.date = temp.getDate();
            this.isLastData = isLastData;
            this.isManual = isManual;
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
                String extraData = "Temperature:" + temp.getValue()
                        + "&Unit:" + temp.getStringUnit()
                        + "&Location:" + "armpit";

                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT_UPLOAD, Locale.ENGLISH);

                data.put("DeviceId", LifeCareHandler.getInstance().getMyDeviceID(TemperatureReadingActivity.this));
                data.put("EntityId", entityId);
                data.put("ExtraData", extraData);
                data.put("EventTypeName", "Temperature Update Data");
                data.put("EventTypeId", "20051");
                data.put("Zone", "56398aa0e4b00e308ce460ec");
                data.put("ZoneCode", "OT");
                data.put("CreateDate", sdf.format(temp.getDate()));
                data.put("WriteToSocket",true);
                data.put("WConfigCode", "BLE");

                if(!isManual && pairedDevice!=null) {

                    String gatewayId =  LifeCareHandler.getInstance().getMyDeviceID(TemperatureReadingActivity.this);
                    SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
                    String serial = temp.getEntityId() + "-" + gatewayId + "-" + pairedDevice.getDeviceId().replace(":","");
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
            } /*catch (java.io.IOException e) {
                e.printStackTrace();
            } */catch(Exception e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void res){
            if(!success){
                UnsyncedData.tempList.add(date);
            }

            if(isLastData){
                if(progress!=null && progress.isShowing()){
                    progress.dismiss();
                }
                String message = "";
                if(success){
                    message = "Successfully updated temperature data!";
                }else{
                    message = "Problem uploading data! Data have been saved to cache!";
                }
                Toast.makeText(TemperatureReadingActivity.this, message, Toast.LENGTH_LONG).show();

                onDataUpdated();
            }
        }
    }

    private void onDataUpdated(){
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }

    private JumperThermometerListener mJumperThermometerListener = new JumperThermometerListener() {
        @Override
        public void onDeviceScan(BluetoothDevice device) {

            pairedDeviceMacAdd = device.getAddress();
            pairedDevice = LocalMeasurementDevicesHandler.getInstance().getDeviceByMacAddress(pairedDeviceMacAdd);

            if (pairedDevice != null) {
                mTemperature = -1;

                mJumperThermometer.stopScanning();
                mJumperThermometer.connectBluetoothDevice(device);
            }
        }

        @Override
        public void onReadResult(double temperature) {
            mTemperature = temperature;
        }

        @Override
        public void onConnectionStateChanged(int state) {
            if (state == BluetoothProfile.STATE_DISCONNECTED) {
                if (mTemperature > 0) {
                    Temperature temp = new Temperature();
                    temp.setValue(mTemperature);
                    temp.setUnit(TemperatureMeasurementProfile.UNIT_CELSIUS);
                    temp.setDate(Calendar.getInstance().getTime());
                    temp.setEntityId(entityId);

                    tempList.add(temp);
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
        }
    };

}
