package sg.lifecare.medicare.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.calendardatepicker.MonthAdapter;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import io.realm.Realm;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.VitalConfigs;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.Medication;
import sg.lifecare.medicare.database.sync.NetworkChangeReceiver;
import sg.lifecare.medicare.database.sync.SyncHandler;
import sg.lifecare.medicare.database.sync.UnsyncedData;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.utils.HtmlUtil;
import sg.lifecare.medicare.utils.LifeCareHandler;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import timber.log.Timber;

/**
 * Created by sweelai on 13/6/16.
 */
public class MedicationTakenActivity extends AppCompatActivity implements CalendarDatePickerDialogFragment.OnDateSetListener, RadialTimePickerDialogFragment.OnTimeSetListener {

    private static final String TAG = "MedicationTakenActivity";
    private static final String FRAG_TAG_TIME_PICKER = "fragment_time_picker_name";
    private static final String FRAG_TAG_DATE_PICKER = "fragment_date_picker_name";
    private EditText remarks, dosageEntry;
    private TextView mediTitle, dateText, timeText;
    private ImageView mediBtn, DateBtn, TimeBtn, backBtn;
    private Spinner typeSpinner, unitSpinner;
    private Calendar setDate;
    Realm mRealm;

    public MedicationTakenActivity() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.medication_taken);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
        typeSpinner = (Spinner) findViewById(R.id.medi_type_spinner);
        unitSpinner = (Spinner) findViewById(R.id.unit_spinner);
        dateText = (TextView) findViewById(R.id.date_selection);
        timeText = (TextView) findViewById(R.id.time_selection);
        dosageEntry = (EditText) findViewById(R.id.edit_dosage);
        remarks = (EditText) findViewById(R.id.edit_remarks);
        remarks.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                ((ScrollView)findViewById(R.id.scroll_view)).requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });
        DateBtn = (ImageView) findViewById(R.id.calendar_btn);
        TimeBtn = (ImageView) findViewById(R.id.time_btn);

        currentTimeAndDate();
        MediTypeSpinner();
        UnitSpinner();

        dateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO:onCalendarDatePickerPressed();
            }
        });
        timeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTimePressed();
            }
        });

        CustomToolbar mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_activity_medication_taken);
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.setRightButtonImage(R.drawable.ic_toolbar_tick);
        mToolbar.hideSecondRightButton();
        mToolbar.setListener(mToolbarListener);
    }

    public void currentTimeAndDate() {

        final Calendar c = Calendar.getInstance();
        int currentMin = c.get(Calendar.MINUTE);
        int currentHour = c.get(Calendar.HOUR_OF_DAY);
        int currentDate = c.get(Calendar.DAY_OF_MONTH);
        int currentMonth = c.get(Calendar.MONTH);
        int currentYear = c.get(Calendar.YEAR);
        String date;

        final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        String mon = MONTHS[currentMonth];
        if (currentDate < 10)
            date = "0" + currentDate + " " + mon + " " + currentYear;
        else
            date = currentDate + " " + mon + " " + currentYear;
        dateText.setText(date);

        String time = String.format("%02d:%02d", currentHour, currentMin);
        timeText.setText(time);

        setDate = Calendar.getInstance();
    }

    public void onCalendarDatePickerPressed() {
        final Calendar c = Calendar.getInstance();
        int currentDate = c.get(Calendar.DAY_OF_MONTH);
        int currentMonth = c.get(Calendar.MONTH);
        int currentYear = c.get(Calendar.YEAR);

        MonthAdapter.CalendarDay maxDate = new MonthAdapter.CalendarDay(currentYear, currentMonth,currentDate);
        CalendarDatePickerDialogFragment cdp = new CalendarDatePickerDialogFragment()
                .setDoneText(getString(R.string.dialog_ok))
                .setCancelText(getString(R.string.dialog_cancel))
                .setThemeCustom(R.style.MyCustomBetterPickersDialogs)
                .setDateRange(null,maxDate)
                .setPreselectedDate(setDate.get(Calendar.YEAR),
                        setDate.get(Calendar.MONTH),setDate.get(Calendar.DAY_OF_MONTH))
                .setOnDateSetListener(MedicationTakenActivity.this);
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
        dateText.setText(date);
        setDate.set(year,monthOfYear,dayOfMonth);
    }

    @Override
    public void onResume() {
        super.onResume();

        if(mRealm==null)
            mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

        CalendarDatePickerDialogFragment calendarDatePickerDialogFragment = (CalendarDatePickerDialogFragment) getSupportFragmentManager()
                .findFragmentByTag(FRAG_TAG_DATE_PICKER);
        if (calendarDatePickerDialogFragment != null) {
            calendarDatePickerDialogFragment.setOnDateSetListener(MedicationTakenActivity.this);
        }

        RadialTimePickerDialogFragment rtpd = (RadialTimePickerDialogFragment) getSupportFragmentManager().findFragmentByTag(FRAG_TAG_TIME_PICKER);
        if (rtpd != null) {
            rtpd.setOnTimeSetListener(MedicationTakenActivity.this);
        }
    }

    public void onTimePressed() {
        RadialTimePickerDialogFragment rtpd = new RadialTimePickerDialogFragment()
                .setOnTimeSetListener(MedicationTakenActivity.this)
                .setCancelText(getString(R.string.dialog_cancel))
                .setDoneText(getString(R.string.dialog_ok))
                .setStartTime(setDate.get(Calendar.HOUR_OF_DAY),setDate.get(Calendar.MINUTE))
                .setThemeCustom(R.style.MyCustomBetterPickersDialogs);
            rtpd.show(getSupportFragmentManager(), "Time picker");
    }

    public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
        String time = String.format("%02d:%02d", hourOfDay, minute);
        timeText.setText(time);
        setDate.set(setDate.get(Calendar.YEAR),setDate.get(Calendar.MONTH),setDate.get(Calendar.DAY_OF_MONTH),
                hourOfDay,minute);
    }

    private void MediTypeSpinner() {
        List<String> medicList = Arrays.asList(getResources().getStringArray(R.array.medication));
        ArrayAdapter adapter = new ArrayAdapter(MedicationTakenActivity.this,R.layout.spinner_item,medicList);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

    }

    private void UnitSpinner() {
        List<String> unitList = new ArrayList<>();
        unitList.add("Unit");
        ArrayAdapter adapter = new ArrayAdapter(MedicationTakenActivity.this,R.layout.spinner_item,unitList);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        unitSpinner.setPrompt("Unit");
        unitSpinner.setAdapter(adapter);
        unitSpinner.setEnabled(false);
    }

    public void getMedicationTakenData() {

        String mediType = typeSpinner.getSelectedItem().toString();
        String unit = unitSpinner.getSelectedItem().toString();
        String dosage = dosageEntry.getText().toString().trim();
        String remark = remarks.getText().toString().trim();
        String date = dateText.getText().toString().trim();
        String time = timeText.getText().toString().trim();

       if (dosage.isEmpty()) {
            dosageEntry.setError("Please enter number of doses.");
        } else {
           double dosageValue = Double.parseDouble(dosage);

           SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
           String entityId = sh.getString("entity_id", "");

           Medication medication = new Medication();
           medication.setDosage(dosageValue);
           medication.setDate(setDate.getTime());
           medication.setType(typeSpinner.getSelectedItemPosition());
           medication.setUnit(unitSpinner.getSelectedItemPosition());
           medication.setRemark(HtmlUtil.encodeString(remark));
           medication.setEntityId(entityId);

          // Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
           PatientData.getInstance().addMedication(mRealm,medication);
          // realm.close();

           if(NetworkChangeReceiver.isInternetAvailable(this)) {
               new UploadMedicationToServer(medication).execute();
           }
           else{
               SyncHandler.registerNetworkBroadcast();
               UnsyncedData.medicationList.add(setDate.getTime());
           }

           finish();
        }


    }

    private class UploadMedicationToServer extends AsyncTask<Void, Void, Void>
    {
        private Medication medication;
        private boolean success;

        public UploadMedicationToServer(Medication medication)
        {
            this.medication = medication;
        }

        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {
            try {
                JSONObject data = new JSONObject();

                String extraData = "MedicationType:" + medication.getStringType()
                        + "&Dosage:" + medication.getDosage()
                        + "&Unit:"+ medication.getStringUnit()
                        + "&Remarks:"+ medication.getRemark();

                Timber.d("Extra Data= " + extraData);

                MedicalDevice device = MedicalDevice.findModel(Model.TERUMO_MEDISAFE_FIT);

                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT_UPLOAD);

                String serial = device.getDeviceId();

                data.put("DeviceId", LifeCareHandler.getInstance().getMyDeviceID(MedicationTakenActivity.this));
                data.put("EntityId", medication.getEntityId());
                data.put("ExtraData", extraData);
                data.put("EventTypeName", "Medication Intake");
                data.put("EventTypeId", "20060"); //medication
                data.put("Zone", "56398aa0e4b00e308ce460ec");
                data.put("ZoneCode", "OT");
                data.put("CreateDate", sdf.format(medication.getDate()));
                data.put("WriteToSocket",true);
                data.put("SmartDeviceId", serial);

                String jsonString = data.toString();

                Log.w(TAG, "Uploading Medication Data To Server : " + jsonString);

                Request request = new Request.Builder()
                        .url(VitalConfigs.URL + "mlifecare/event/addEvent")
                        .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                        .build();

                Response response = LifeCareHandler.getInstance().okclient.newCall(request).execute();

                JSONObject json = new JSONObject(response.body().string());
                success = !json.getBoolean("Error"); //Error = true if contains error
                //success = response.isSuccessful();
                Log.d(TAG, "Successful upload: " + success + " , " + json.getString("ErrorDesc"));
            } catch(JSONException ex) {
                Log.e(TAG, ex.getMessage(), ex);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void res){
            if(!success) {
                UnsyncedData.medicationList.add(setDate.getTime());
            }
        }

    }

    @Override
    public void onPause(){
        super.onPause();
        Log.d("TESTT","medicationtaken = pause!");
        mRealm.close();
        mRealm=null;
    }

    private CustomToolbar.OnToolbarClickListener mToolbarListener = new CustomToolbar.OnToolbarClickListener() {
        @Override
        public void leftButtonClick() {
            onBackPressed();
        }

        @Override public void rightButtonClick() {
            getMedicationTakenData();
        }

        @Override public void secondRightButtonClick() {

        }
    };
}
