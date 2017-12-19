package sg.lifecare.medicare.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
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
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.database.sync.NetworkChangeReceiver;
import sg.lifecare.medicare.database.sync.SyncHandler;
import sg.lifecare.medicare.database.sync.UnsyncedData;
import sg.lifecare.medicare.object.LocalMeasurementDevicesHandler;
import sg.lifecare.medicare.ui.adapter.GlucoseReadingAdapter;
import sg.lifecare.medicare.ui.pairing.ConvertGatewayActivity;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.ui.view.PairingIndicatorView;
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
public class BloodGlucoseReadingActivity extends AppCompatActivity implements CalendarDatePickerDialogFragment.OnDateSetListener, RadialTimePickerDialogFragment.OnTimeSetListener {

    int REQUEST_ENABLE_NFC = 125;

    private static final String TAG = "GlucoseReadingActivity";
    private static final String FRAG_TAG_DATE_PICKER = "fragment_date_picker_name";
    private static final String FRAG_TAG_TIME_PICKER = "fragment_time_picker_name";
    private static final String DATE_FORMAT = "dd MMM yyyy";
    private static final String TIME_FORMAT = "HH:mm";

    private SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    private SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);

    private EditText etRemarks, etSugarCon;
    private TextView selectedDate, selectedTime;
    private ImageView dateBtn, timeBtn, autoDataEntryImage;
    private Spinner unitSpinner, measuredSpinner;
    private Calendar setDate;
    private TagDetector mTagDetector;
    private ArrayList<Terumo> terumoList = new ArrayList<>();
    private Realm mRealm;
    boolean isDevicePaired = false;
    private TextView tvDesc;
    private String nfcId;
    private boolean hasDisplayed;
    private MedicalDevice medicalDevice;
    private String entityId;
    private AlertDialog mNfcEnableDialog;
    private PairingIndicatorView pairingIndicator;
    private CustomToolbar mToolbar;
    private GlucoseReadingAdapter adapter;
    private Button addDeviceBtn;
    private ProgressDialog progress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_device_reading);
        ViewStub stub = (ViewStub) findViewById(R.id.manual_entry_view);
        stub.setLayoutResource(R.layout.view_manual_entry_bg);

        final View inflated = stub.inflate();
        inflated.setVisibility(View.INVISIBLE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.uploading_data));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);

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

        //timeBtn = (ImageView) findViewById(R.id.time_btn);
        //dateBtn = (ImageView) findViewById(R.id.calendar_btn);
        autoDataEntryImage = (ImageView) findViewById(R.id.automatic_data_entry_image);

        findViewById(R.id.auto_entry_view).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!NfcUtil.isNfcEnabled(BloodGlucoseReadingActivity.this)) {
                    requestNfc();
                }
            }
        });
        /*RelativeLayout topLayout = (RelativeLayout) findViewById(R.id.top_layout);
        topLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isDevicePaired){
                    if(NfcUtil.isNfcEnabled(BloodGlucoseReadingActivity.this)) {
                        startPairingActivity();
                    }else{
                        requestNfc();
                    }
                }
            }
        });*/

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
                if(!isDevicePaired){
                    if(NfcUtil.isNfcEnabled(BloodGlucoseReadingActivity.this)) {
                        startPairingActivity();
                    }else{
                        requestNfc();
                    }
                }
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
        selectedDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: onCalendarDatePickerPressed();
            }
        });
        selectedTime.setOnClickListener(new View.OnClickListener() {
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

        checkIsPaired();

        CalendarDatePickerDialogFragment datePickerFrag =
                (CalendarDatePickerDialogFragment) getSupportFragmentManager()
                        .findFragmentByTag(FRAG_TAG_DATE_PICKER);

        if (datePickerFrag != null) {
            datePickerFrag.setOnDateSetListener(BloodGlucoseReadingActivity.this);
        }

        RadialTimePickerDialogFragment timePickerFrag =
                (RadialTimePickerDialogFragment) getSupportFragmentManager().
                        findFragmentByTag(FRAG_TAG_TIME_PICKER);

        if (timePickerFrag != null) {
            timePickerFrag.setOnTimeSetListener(BloodGlucoseReadingActivity.this);
        }
    }

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

    private void startPairingActivity(){
        Intent intent = new Intent(BloodGlucoseReadingActivity.this,
                ConvertGatewayActivity.class);
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
                .setOnDateSetListener(BloodGlucoseReadingActivity.this);
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
                .setOnTimeSetListener(BloodGlucoseReadingActivity.this)
                .setCancelText(getString(R.string.dialog_cancel))
                .setDoneText(getString(R.string.dialog_ok))
                .setStartTime(setDate.get(Calendar.HOUR_OF_DAY),setDate.get(Calendar.MINUTE))
                .setThemeCustom(R.style.MyCustomBetterPickersDialogs);
        rtpd.show(getSupportFragmentManager(), FRAG_TAG_TIME_PICKER);
    }

    public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
        String time = String.format(Locale.ENGLISH, "%02d:%02d", hourOfDay, minute);
        selectedTime.setText(time);
        setDate.set(
                setDate.get(Calendar.YEAR),
                setDate.get(Calendar.MONTH),
                setDate.get(Calendar.DAY_OF_MONTH),
                hourOfDay,
                minute
        );
    }

    private void checkIsPaired(){
        ArrayList<MedicalDevice> mds = LocalMeasurementDevicesHandler.getInstance().getConnectedMedicalDeviceList();
        for (MedicalDevice device : mds) {
            if(device.getModel()==Model.TERUMO_MEDISAFE_FIT ||
                    device.getModel()==Model.ACCU_CHEK_AVIVA_CONNECT ||
                    device.getModel() == Model.VIVACHEK_INO_SMART ){
                isDevicePaired = true;
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

            if(!NfcUtil.isNfcEnabled(BloodGlucoseReadingActivity.this)) {
                mNfcEnableDialog.show();
            }else{
                startTagDetect();
            }

            if (LocalMeasurementDevicesHandler.getInstance().hasConnectedModel(Model.VIVACHEK_INO_SMART)) {
                tvDesc.setText(R.string.device_info_retrieve_data);
                pairingIndicator.setPairingMode(PairingIndicatorView.READING_BLE);
                addDeviceBtn.setVisibility(View.INVISIBLE);
            }
        }
        else {
            if(!NfcUtil.isNfcEnabled(BloodGlucoseReadingActivity.this)) {
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

        if (mRealm == null)
            mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

        checkIsPaired();
    }

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

                            new uploadDataToServer(terumoList.get(i), false, isLastData).execute();

                        } else {
                            Timber.d("Found existing record: (val) " + terumoList.get(i).getValue()
                                    + " (date) " + sdf.format(terumoList.get(i).getDate()));
                        }
                    }

                }
                terumoList.clear();
            }
        });

        adapter = new GlucoseReadingAdapter(
                BloodGlucoseReadingActivity.this, R.layout.glucose_result_item, terumoList
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
        mTagDetector.stopDetect();
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

                data.put("DeviceId", LifeCareHandler.getInstance().getMyDeviceID(BloodGlucoseReadingActivity.this));
                data.put("EntityId", entityId);
                data.put("ExtraData", extraData);
                data.put("EventTypeName", "Gluco Update Data");
                data.put("EventTypeId", "20015"); //blood glucose
                data.put("Zone", "56398aa0e4b00e308ce460ec");
                data.put("ZoneCode", "OT");
                data.put("CreateDate", sdf.format(terumo.getDate()));
                data.put("WriteToSocket",true);

                if(!isManualKeyIn) {
                    String gatewayId =  LifeCareHandler.getInstance().getMyDeviceID(BloodGlucoseReadingActivity.this);
                    SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
                    entityId = sh.getString("entity_id", "");
                    String serial = entityId + "-" + gatewayId + "-" + nfcId.replace(":","");

                    data.put("NodeId", serial);
                    data.put("NodeName", device.getAssignedName());
                    data.put("WConfigCode", "NFC");
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
                Toast.makeText(BloodGlucoseReadingActivity.this, message, Toast.LENGTH_LONG).show();

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

}
