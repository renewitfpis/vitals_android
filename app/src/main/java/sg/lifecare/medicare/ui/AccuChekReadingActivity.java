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
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import sg.lifecare.medicare.ble.AccuChekDeviceMeter;
import sg.lifecare.medicare.ble.BleException;
import sg.lifecare.medicare.ble.aandd.AAndDMeter;
import sg.lifecare.medicare.ble.aandd.AAndDMeterListener;
import sg.lifecare.medicare.ble.profile.AbstractProfile;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Temperature;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.database.sync.UnsyncedData;
import sg.lifecare.medicare.object.LocalMeasurementDevicesHandler;
import sg.lifecare.medicare.ui.adapter.GlucoseReadingAdapter;
import sg.lifecare.medicare.ui.pairing.ConvertGatewayActivity;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.ui.view.PairingIndicatorView;
import sg.lifecare.medicare.utils.BleUtil;
import sg.lifecare.medicare.utils.LifeCareHandler;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import timber.log.Timber;

public class AccuChekReadingActivity extends AppCompatActivity implements CalendarDatePickerDialogFragment.OnDateSetListener, RadialTimePickerDialogFragment.OnTimeSetListener {

    private static final String TAG = "AccuChekReading";
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
    private AccuChekDeviceMeter deviceMeter;
    private AlertDialog mBleEnableDialog;
    private boolean isDevicePaired = false;
    private String entityId;
    private PairingIndicatorView pairingIndicator;
    private Button addDeviceBtn, retryBtn;
    private ArrayList<Temperature> tempList;
    private ArrayList<Terumo> terumoList;
    private GlucoseReadingAdapter adapter;
    private ProgressDialog progress;

    private String pairedDeviceMacAdd;
    private MedicalDevice pairedDevice;
    CountDownTimer timer;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_reading);

        tempList = new ArrayList<>();
        terumoList = new ArrayList<>();
        //setContentView(R.layout.activity_temp_reading);
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
                            displayList();
                        }else{
                            pairingIndicator.setPairingMode(PairingIndicatorView.READING_FAIL_ERROR);
                            retryBtn.setVisibility(View.VISIBLE);
                            Timber.d("DISCONNECTED!!!! display res TEMP LIST smaller than 0");
                        }
                    }
                });
            }
        };
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.uploading_data));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCancelable(false);

        final CustomToolbar mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_activity_glucose_reading);
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.setRightButtonImage(R.drawable.ic_toolbar_tick);
        mToolbar.hideRightButton();
        mToolbar.hideSecondRightButton();
        mToolbar.setListener(mToolbarListener);

        ViewStub stub = (ViewStub) findViewById(R.id.manual_entry_view);
        stub.setLayoutResource(R.layout.view_manual_entry_temp);
        final View inflated = stub.inflate();
        inflated.setVisibility(View.INVISIBLE);

        findViewById(R.id.entry_mode_selector).setVisibility(View.GONE);

        pairingIndicator = (PairingIndicatorView) findViewById(R.id.pairing_indicator);
        pairingIndicator.setProductImage(R.drawable.accu_chek_ac);
        pairingIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!BleUtil.isBleEnabled()){
                    mBleEnableDialog.show();
                }
            }
        });

        if(getIntent().hasExtra("device")) {
            MedicalDevice device = (MedicalDevice) getIntent().getSerializableExtra("device");
            pairedDevice = device;
        }

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
                        Intent intent = new Intent(AccuChekReadingActivity.this,
                                ConvertGatewayActivity.class);
                        intent.putExtra("medical_device",MedicalDevice.findModel(Model.ACCU_CHEK_AVIVA_CONNECT));
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
            datePickerFrag.setOnDateSetListener(AccuChekReadingActivity.this);
        }

        RadialTimePickerDialogFragment timePickerFrag =
                (RadialTimePickerDialogFragment) getSupportFragmentManager().
                        findFragmentByTag(FRAG_TAG_TIME_PICKER);

        if (timePickerFrag != null) {
            timePickerFrag.setOnTimeSetListener(AccuChekReadingActivity.this);
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
                .setOnDateSetListener(AccuChekReadingActivity.this);
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
            if(device.getModel()==Model.ACCU_CHEK_AVIVA_CONNECT){
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
                .setOnTimeSetListener(AccuChekReadingActivity.this)
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

        ArrayAdapter adapter = new ArrayAdapter(AccuChekReadingActivity.this,R.layout.spinner_item,weightUnit);
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
            TextView text = new TextView(AccuChekReadingActivity.this);
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
            TextView text = new TextView(AccuChekReadingActivity.this);
            text.setGravity(Gravity.CENTER_VERTICAL);
            text.setPadding(12, 10, 30, 16);
            text.setTextSize(18);
            text.setText(list.get(i));
            text.setTextColor(Color.DKGRAY);
            return text;
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
            if(deviceMeter==null) {
                deviceMeter = new AccuChekDeviceMeter(this, mMeterListener, Model.ACCU_CHEK_AVIVA_CONNECT, "");
                deviceMeter.setMode(AAndDMeter.MODE_READING);
            }
            if(pairedDevice!=null && !pairedDevice.getDeviceId().isEmpty()) {
                deviceMeter.connectDevice(pairedDevice.getDeviceId());
            }
        } catch (BleException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void stopDetect() {

        if(deviceMeter!=null) {
            deviceMeter.clear();
            deviceMeter.stopScanning();
            deviceMeter.disconnect();
            deviceMeter.close();
            deviceMeter = null;
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
            }else if(object instanceof Terumo){
                ((Terumo)object).setEntityId(entityId);
                final Terumo terumo = (Terumo) object;
                Log.d(TAG, "ON GLUCOSE DATA RETRIEVED: " + terumo.getValue() + " " + terumo.getStringUnit());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Timber.d("TERUMO EntityId = " + terumo.getEntityId());
                        terumoList.add(terumo);
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
            if(status==BluetoothAdapter.STATE_DISCONNECTED){
                Timber.d("DISCONNECTED!!!! display res");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Timber.d("DISCONNECTED!!!! display res b");
                        if (terumoList.size() > 0) {
                            Timber.d("DISCONNECTED!!!! display res TEMP LIST > 0");
                            //pairingIndicator.setPairingMode(PairingIndicatorView.READING_SUCCESS);
                            //tvDesc.setText("Results Retrieved");
                            displayList();
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

    private CustomToolbar.OnToolbarClickListener mToolbarListener = new CustomToolbar.OnToolbarClickListener() {
        @Override
        public void leftButtonClick() {
            onBackPressed();
        }

        @Override public void rightButtonClick() {

        }

        @Override public void secondRightButtonClick() {

        }
    };

    public void displayList() {
        final Dialog dlg = new Dialog(this);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.dialog_bp, null);
        TextView done = (TextView) view.findViewById(R.id.done_button);
        TextView cancel = (TextView) view.findViewById(R.id.cancel_button);

        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dlg.cancel();
                terumoList.clear();
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

                            //TODO:new uploadDataToServer(terumoList.get(i), false, isLastData).execute();

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
                AccuChekReadingActivity.this, R.layout.glucose_result_item, terumoList
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

                MedicalDevice device = LocalMeasurementDevicesHandler.getInstance().getDeviceByMacAddress(pairedDeviceMacAdd); //MedicalDevice.findModel(Model.ACCU_CHEK_AVIVA_CONNECT);

                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT_UPLOAD, Locale.ENGLISH);

                data.put("DeviceId", LifeCareHandler.getInstance().getMyDeviceID(AccuChekReadingActivity.this));
                data.put("EntityId", entityId);
                data.put("ExtraData", extraData);
                data.put("EventTypeName", "Gluco Update Data");
                data.put("EventTypeId", "20015"); //blood glucose
                data.put("Zone", "56398aa0e4b00e308ce460ec");
                data.put("ZoneCode", "OT");
                data.put("CreateDate", sdf.format(terumo.getDate()));
                data.put("WriteToSocket",true);

                if(!isManualKeyIn) {
                    String gatewayId =  LifeCareHandler.getInstance().getMyDeviceID(AccuChekReadingActivity.this);
                    SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
                    entityId = sh.getString("entity_id", "");
                    String serial = entityId + "-" + gatewayId + "-" + pairedDevice.getDeviceId().replace(":","");

                    data.put("NodeId", serial);
                    data.put("NodeName", device.getAssignedName());
                    data.put("WConfigCode", "BLE");
                    data.put("SystemManufacturerCode", device.getManufacturerCode());
                    data.put("SystemProductCode", device.getProductCode());
                    data.put("SystemProductTypeCode", device.getProductTypeCode());
                    data.put("SmartDeviceId", serial);
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
                Toast.makeText(AccuChekReadingActivity.this, message, Toast.LENGTH_LONG).show();

                onDataUpdated();
            }
        }
    }

    private void onDataUpdated(){
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }

}
