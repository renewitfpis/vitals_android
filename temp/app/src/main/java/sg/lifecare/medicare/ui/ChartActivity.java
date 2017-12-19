package sg.lifecare.medicare.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.ResolveInfo;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.support.v7.widget.RecyclerView;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment.OnDateSetListener;
import com.codetroopers.betterpickers.calendardatepicker.MonthAdapter;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend.LegendPosition;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.XAxis.XAxisPosition;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Medication;
import sg.lifecare.medicare.database.model.Note;
import sg.lifecare.medicare.database.model.Photo;
import sg.lifecare.medicare.database.model.SpO2;
import sg.lifecare.medicare.database.model.Symptom;
import sg.lifecare.medicare.database.model.Temperature;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.database.model.User;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.ui.adapter.ActivityAdapter;
import sg.lifecare.medicare.ui.view.ChartToolbar;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.ui.view.CustomToolbar.OnToolbarClickListener;
import sg.lifecare.medicare.utils.CSVUtils;
import sg.lifecare.medicare.utils.ChartValueFormatter;
import sg.lifecare.medicare.utils.EnterpriseHandler;
import sg.lifecare.medicare.utils.HtmlUtil;
import sg.lifecare.medicare.utils.WrapContentLinearLayoutManager;
import timber.log.Timber;

import static android.R.attr.type;

public class ChartActivity extends AppCompatActivity implements  OnChartValueSelectedListener {
    String TAG = "ChartActivity";
    Context context;

    private static final int
            DAY_MODE = 0,
            WEEK_MODE = 1,
            MONTH_MODE = 2;

    public static final int
            GLUCOSE_TYPE = 0,
            BLOOD_PRESSURE_TYPE = 1,
            WEIGHT_TYPE = 2,
            TEMPERATURE_TYPE = 3,
            SPO2_TYPE = 4;

    private final float GLUCOSE_DEFAULT_MAX_VALUE = 8f;
    private final float GLUCOSE_DEFAULT_MIN_VALUE = 0f;
    private final float BP_DEFAULT_MAX_VALUE = 160;
    private final float BP_DEFAULT_MIN_VALUE = 50;
    private final float WEIGHT_DEFAULT_MAX_VALUE = 100;
    private final float WEIGHT_DEFAULT_MIN_VALUE = 10;
    private final float TEMP_DEFAULT_MAX_VALUE = 41;
    private final float TEMP_DEFAULT_MIN_VALUE = 34;
    private final float SPO2_DEFAULT_MAX_VALUE = 100;
    private final float SPO2_DEFAULT_MIN_VALUE = 0;

    int[] icons = new int[]{
            R.drawable.blood_glucose,
            R.drawable.blood_pressure,
            R.drawable.weight,
            R.drawable.temp,
            R.drawable.spo2};

    int[] chartColours = new int[] {
            R.color.overview_glucose,
            R.color.overview_pressure,
            R.color.overview_weight,
            R.color.overview_temperature,
            R.color.overview_spo2};

    public static int currentMode = DAY_MODE;

    public static int currentType = GLUCOSE_TYPE;

    public static final String[] markerTimeFormat = new String[]{
            "HH:mm", "dd MMM, HH:mm","dd MMM, HH:mm"
    };

    private static String[] typeNames = new String[]{ "blood_glucose","blood_pressure","weight","temperature"};

    int circle_radius = 7;
    int circle_hole_radius = 5;

    LineChart chart;
    LineDataSet normalDataset;
    LineDataSet circleDataset;
    LineDataSet foodDataset;
    LineDataSet circleFoodDataset;
    LineDataSet medicDataset;
    LineDataSet circleMedicDataset;
    LineDataSet diastolicDataset;
    LineDataSet diastolicCircleDataset;

    ArrayList<String> xVals = new ArrayList<>();
    ArrayList<Entry> values = new ArrayList<>();
    ArrayList<Entry> values2 = new ArrayList<>();
    ArrayList<Entry> foodValues = new ArrayList<>();
    ArrayList<Entry> medicValues = new ArrayList<>();
    ArrayList<Entry> foodValues2 = new ArrayList<>();
    ArrayList<Entry> medicValues2 = new ArrayList<>();
    ArrayList<ILineDataSet> dataSets = new ArrayList<>();

    ImageView nextDateBtn, prevDateBtn;
    RelativeLayout[] rlTimeSelectors;

    TextView tvDate, tvLabelX, tvLabelY;
    TextView[] tvTimeSelectors;

    Calendar cal;
    Calendar weekStartCal, weekEndCal;
    Calendar monthStartCal, monthEndCal;
    SimpleDateFormat dailyDateFormat;

    String chartDate;
    String currentDate;
    String[] day;
    boolean hasFoodOrMedic = false;

    LinearLayout foodMedicPanels;
    RelativeLayout foodPanel;
    RelativeLayout medicPanel;
    RecyclerView foodMedicListView;
    ArrayList<RealmObject> foodMedicList;
    ActivityAdapter foodMedicAdapter;

    ImageView chartValueIcon;
    TextView chartValue, chartUnit, chartTime;
    Realm mRealm;

    boolean firstTime = true;
    public static boolean active = false;

    @Override
    public void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        active = false;
    }

    private void initToolbar(){
        final ChartToolbar mToolbar = (ChartToolbar) findViewById(R.id.chart_toolbar);

        if(EnterpriseHandler.getCurrentEnterprise()!=EnterpriseHandler.TERUMO) {
            ArrayList<String> spinnerArray = new ArrayList<>();
            spinnerArray.add("Blood Glucose Chart");
            spinnerArray.add("Blood Pressure Chart");
            spinnerArray.add("Weight Chart");
            spinnerArray.add("Temperature Chart");

            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.chart_spinner_item, spinnerArray); //selected item will look like a spinner set from XML
            spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

            OnItemSelectedListener onItemSelectedListener = new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                    Timber.d("onItemSelected: " + pos);
                    currentType = pos;
                    if(mRealm == null){
                        mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
                    }
                    createChart();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            };
            mToolbar.setSpinnerAdapter(spinnerArrayAdapter);
            mToolbar.setSpinnerListener(onItemSelectedListener);
            mToolbar.setSpinnerSelection(currentType);

            //TODO: hidden spinner
            mToolbar.setTitle(spinnerArray.get(currentType));
            mToolbar.setSpinnerVisibility(View.GONE);
        }else{
            mToolbar.setTitle(R.string.title_activity_chart);
            mToolbar.setSpinnerVisibility(View.GONE);
        }

        ArrayList<String> exportSpinnerArray = new ArrayList<>();
        exportSpinnerArray.add("Measure");
        exportSpinnerArray.add("Export Data");

        ArrayAdapter<String> exportArrayAdapter = new ArrayAdapter<>(this, R.layout.chart_spinner_item, exportSpinnerArray); //selected item will look like a spinner set from XML
        exportArrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        OnItemSelectedListener onOverflowItemSelectedListener = new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                Timber.d("onOverFlowItemSelected pos = " + pos) ;
                if(firstTime) {
                    Timber.d("is first time");

                    firstTime = !firstTime;
                    return;
                }
                //mToolbar.setOverflowMenuSelection(0);
                if(pos == 1){
                    try {
                        String path = exportToCSV();
                        if(path != null) {
                            sendCsvByEmail(path);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else if(pos == 0){
                    ArrayList<Class> readingClasses = new ArrayList<>();
                    readingClasses.add(GeneralGlucoseReadingActivity.class);
                    readingClasses.add(BloodPressureReadingActivity.class);
                    readingClasses.add(WeightReadingActivity.class);
                    readingClasses.add(TemperatureReadingActivity.class);
                    readingClasses.add(SpO2ReadingActivity.class);
                    Intent intent = new Intent(ChartActivity.this, readingClasses.get(type));
                    startActivity(intent);

                    Timber.d("POS is not 1, pos = " + pos);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        };
        mToolbar.setOverflowMenuAdapter(exportArrayAdapter);
        mToolbar.setOverflowMenuListener(onOverflowItemSelectedListener);
        mToolbar.setOverflowMenuSelection(0);

        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        //mToolbar.setRightButtonImage(R.drawable.ic_toolbar_tick);
        //mToolbar.hideRightButton();
        mToolbar.hideSecondRightButton();
        mToolbar.setListener(mToolbarListener);
    }

   /* private void getLocalConnectedDevices(){
        ArrayList<MedicalDevice> list =
                LocalMeasurementDevicesHandler.getInstance().getConnectedMedicalDeviceList();

        for (MedicalDevice device : list) {
            if(type == GLUCOSE_TYPE) {
                if (device.getModel() == Model.TERUMO_MEDISAFE_FIT || device.getModel() == Model.ACCU_CHEK_AVIVA_CONNECT) {
                    return true;
                }
            }else if(type == BLOOD_PRESSURE_TYPE) {
                if (device.getModel() == Model.AANDD_UC_352) {
                    mDataList.get(OverviewAdapter3.TYPE_WEIGHT).setDevicePaired(true);
                }
            }else if(device.getModel()==Model.AANDD_UA_651){
                mDataList.get(OverviewAdapter3.TYPE_BLOOD_PRESSURE).setDevicePaired(true);
            }else if(device.getModel()==Model.AANDD_UT_201){
                mDataList.get(OverviewAdapter3.TYPE_TEMPERATURE).setDevicePaired(true);
            }else if(device.getModel()==Model.BERRY_BM1000B || device.getModel()==Model.NONIN_3230){
                mDataList.get(OverviewAdapter3.TYPE_SPO2SET).setDevicePaired(true);
            }else if(device.getModel()==Model.ZENCRO_X6){
                mDataList.get(TYPE_STEP).setDevicePaired(true);
            }
        }
    }*/

    private void initToolbar2(){
        final CustomToolbar mToolbar = (CustomToolbar) findViewById(R.id.chart_toolbar);

        if(EnterpriseHandler.getCurrentEnterprise()!=EnterpriseHandler.TERUMO) {
            ArrayList<String> spinnerArray = new ArrayList<>();
            spinnerArray.add("Blood Glucose Chart");
            spinnerArray.add("Blood Pressure Chart");
            spinnerArray.add("Weight Chart");
            spinnerArray.add("Temperature Chart");
            spinnerArray.add("SpO2");

            /*ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.chart_spinner_item, spinnerArray); //selected item will look like a spinner set from XML
            spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

            OnItemSelectedListener onItemSelectedListener = new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                    Timber.d("onItemSelected: " + pos);
                    currentType = pos;
                    if(mRealm==null){
                        mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
                    }
                    createChart();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            };
            mToolbar.setSpinnerAdapter(spinnerArrayAdapter);
            mToolbar.setSpinnerListener(onItemSelectedListener);
            mToolbar.setSpinnerSelection(currentType);*/

            //TODO: hidden spinner
            mToolbar.setTitle(spinnerArray.get(currentType));
            // mToolbar.setSpinnerVisibility(View.GONE);
        }else{
            mToolbar.setTitle(R.string.title_activity_chart);
            //mToolbar.setSpinnerVisibility(View.GONE);
        }

        View view = mToolbar.getRightView();
        final PopupMenu popup = new PopupMenu(view.getContext(), view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.chart_right_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.measure){
                    Timber.d("CLicked on measure!");
                    ArrayList<Class> readingClasses = new ArrayList<>();
                    readingClasses.add(GeneralGlucoseReadingActivity.class);
                    readingClasses.add(BloodPressureReadingActivity.class);
                    readingClasses.add(WeightReadingActivity.class);
                    readingClasses.add(TemperatureReadingActivity.class);
                    readingClasses.add(SpO2ReadingActivity.class);
                    Intent intent = new Intent(ChartActivity.this, readingClasses.get(currentType));
                    startActivity(intent);

                }else{
                    Timber.d("CLicked on export data!");
                    try {
                        String path = exportToCSV();
                        if(path != null) {
                            sendCsvByEmail(path);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });

        mToolbar.setListener(new OnToolbarClickListener(){

            @Override
            public void leftButtonClick() {
                onBackPressed();
            }

            @Override
            public void rightButtonClick() {

                if(DashboardActivity.isCaregiver()){
                    return;
                }

                popup.show();
            }

            @Override
            public void secondRightButtonClick() {

            }
        });
        /*ArrayList<String> exportSpinnerArray = new ArrayList<>();
        exportSpinnerArray.add("Measure");
        exportSpinnerArray.add("Export Data");

        ArrayAdapter<String> exportArrayAdapter = new ArrayAdapter<>(this, R.layout.chart_spinner_item, exportSpinnerArray); //selected item will look like a spinner set from XML
        exportArrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        OnItemSelectedListener onOverflowItemSelectedListener = new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                Timber.d("onOverFlowItemSelected pos = " + pos) ;
                if(firstTime) {
                    Timber.d("is first time");

                    firstTime = !firstTime;
                    return;
                }
                //mToolbar.setOverflowMenuSelection(0);
                if(pos == 1){
                    try {
                        String path = exportToCSV();
                        if(path != null) {
                            sendCsvByEmail(path);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    Timber.d("POS is not 1, pos = " + pos);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        };
        mToolbar.setOverflowMenuAdapter(exportArrayAdapter);
        mToolbar.setOverflowMenuListener(onOverflowItemSelectedListener);
        mToolbar.setOverflowMenuSelection(0);*/

        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.setRightButtonImage(R.drawable.overflow_icon);
        mToolbar.hideSecondRightButton();
        //mToolbar.setListener(mToolbarListener);

        if(DashboardActivity.isCaregiver()){
            mToolbar.hideRightButton();
        }
    }

    private void initFoodMedicPanels() {
       /* foodMedicPanels = (LinearLayout) findViewById(R.id.food_medic_panels);
        foodPanel = (RelativeLayout) findViewById(R.id.food_panel);
        medicPanel = (RelativeLayout) findViewById(R.id.medic_panel);

        foodTitle = (TextView) findViewById(R.id.food_text);
        foodCarbo = (TextView) findViewById(R.id.food_carbo);
        foodTime = (TextView) findViewById(R.id.food_time);

        medicTitle = (TextView) findViewById(R.id.medic_text);
        medicDosage = (TextView) findViewById(R.id.medic_dosage);
        medicTime = (TextView) findViewById(R.id.medic_time);*/

        // mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        foodMedicListView = (RecyclerView) findViewById(R.id.food_medic_list);
        foodMedicList = new ArrayList<>();
        WrapContentLinearLayoutManager mLayoutManager = new WrapContentLinearLayoutManager(this);
        foodMedicListView.setLayoutManager(mLayoutManager);
        foodMedicListView.setNestedScrollingEnabled(false);

        foodMedicAdapter = new ActivityAdapter(foodMedicList,this);
        foodMedicAdapter.setOnItemClickListener(new ActivityAdapter.ClickListener() {
            @Override
            public void onItemClick(int position, int type, View view) {
                switch(type){
                    case ActivityAdapter.TYPE_PHOTO:
                        String url = (String)view.findViewById(R.id.image_photo).getTag();
                        String remarks = ((TextView)view.findViewById(R.id.text_remark)).getText().toString();
                        Intent intent3 = new Intent(ChartActivity.this, EnlargeImageActivity.class);
                        intent3.putExtra("Image",url);
                        intent3.putExtra("Remarks",remarks);
                        startActivity(intent3);
                        break;
                }
            }
        });
        foodMedicListView.setAdapter(foodMedicAdapter);
        foodMedicAdapter.notifyDataSetChanged();
        /*foodMedicListView.setOnTouchListener(new OnTouchListener() {
            // Setting on Touch Listener for handling the touch inside ScrollView
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Disallow the touch request for parent scroll on touch of child view
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });*/
        /*foodMedicListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                Object object = adapterView.getAdapter().getItem(pos);
                if (object instanceof Photo) {
                    Photo photo = (Photo) object;
                    Intent intent = new Intent(ChartActivity.this, EnlargeImageActivity.class);
                    intent.putExtra("Image", photo.getImage());
                    intent.putExtra("Remarks", HtmlUtil.decodeString(photo.getRemark()));
                    startActivity(intent);
                }
            }
        });*/
    }

    private void initChartTypes(){
        ImageView glucoseBtn = (ImageView) findViewById(R.id.glucose_btn);
        ImageView weightBtn = (ImageView) findViewById(R.id.weight_btn);
        ImageView bpBtn = (ImageView) findViewById(R.id.bp_btn);

        glucoseBtn.setOnClickListener(onTypeClickListener);
        weightBtn.setOnClickListener(onTypeClickListener);
        bpBtn.setOnClickListener(onTypeClickListener);

        if(getIntent().hasExtra("type")) {
            int type = getIntent().getIntExtra("type",-1);
            Timber.d("Current type = " + type);
            if(type!=-1) {
                currentType = type;

                if(type==BLOOD_PRESSURE_TYPE){
                   /* ((ImageView)findViewById(R.id.bp_btn)).setImageResource(R.drawable.ic_blood_pressure);
                    ((ImageView)findViewById(R.id.glucose_btn)).setImageResource(R.drawable.ic_blood_glucose_disabled);
                    ((ImageView)findViewById(R.id.weight_btn)).setImageResource(R.drawable.ic_weight_disabled);*/
                }
                else if(type == WEIGHT_TYPE){
                   /* ((ImageView)findViewById(R.id.weight_btn)).setImageResource(R.drawable.ic_weight);
                    ((ImageView)findViewById(R.id.bp_btn)).setImageResource(R.drawable.ic_blood_pressure_disabled);
                    ((ImageView)findViewById(R.id.glucose_btn)).setImageResource(R.drawable.ic_blood_glucose_disabled);*/
                }
            }
            else
                currentType = GLUCOSE_TYPE;
        }
        else {
            currentType = GLUCOSE_TYPE;
        }
    }

    private void initChartModes(){
        currentMode = DAY_MODE;

        //Mode Selector
        RelativeLayout dailyBtn = (RelativeLayout) findViewById(R.id.timeLeftBg);
        RelativeLayout weeklyBtn = (RelativeLayout) findViewById(R.id.timeMiddleBg);
        RelativeLayout monthlyBtn = (RelativeLayout) findViewById(R.id.timeRightBg);
        TextView tvDaily = (TextView) findViewById(R.id.tvDaily);
        TextView tvWeekly = (TextView) findViewById(R.id.tvWeekly);
        TextView tvMonthly = (TextView) findViewById(R.id.tvMonthly);

        dailyBtn.setSelected(true);

        //dailyBtn.getBackground().setColorFilter(ContextCompat.getColor(
        //      context,R.color.colorPrimaryGreen), Mode.MULTIPLY);
        //tvDaily.setTextColor(ContextCompat.getColor(context,android.R.color.white));

        rlTimeSelectors = new RelativeLayout[]{dailyBtn,weeklyBtn,monthlyBtn};
        tvTimeSelectors = new TextView[]{tvDaily,tvWeekly,tvMonthly};

        dailyBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                selectTab(0);
                setDailyDate();
                createDayChart();
            }
        });

        weeklyBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                selectTab(1);
                setWeeklyDate();
                createWeekChart();
            }
        });

        monthlyBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                selectTab(2);
                setMonthlyDate();
                createMonthChart();
            }
        });
    }

    private void initChartDate(){
        //Handles date
        cal = Calendar.getInstance();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(PatientData.getInstance().DATE_FORMAT, Locale.ENGLISH);
            String date = getIntent().getStringExtra("date");
            if(date!=null)
                cal.setTime(sdf.parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
            Timber.e("Problem parsing date! " + e.getMessage());
        }

        weekStartCal = (Calendar)cal.clone();
        weekEndCal = (Calendar)cal.clone();
        monthStartCal = (Calendar)cal.clone();
        monthEndCal = (Calendar)cal.clone();

        dailyDateFormat = new SimpleDateFormat(PatientData.getInstance().DATE_DAY_FORMAT, Locale.ENGLISH);
        chartDate = currentDate = dailyDateFormat.format(cal.getTime());
        tvDate = (TextView) findViewById(R.id.text_date);
        tvDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCalendarPressed();
            }
        });
        tvDate.setText(chartDate);
    }

    private void initDateNavigation(){
        prevDateBtn = (ImageView) findViewById(R.id.image_previous);
        prevDateBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentMode == DAY_MODE)
                    navigateToPrevDay();
                else if(currentMode == WEEK_MODE)
                    navigateToPrevWeek();
                else
                    navigateToPrevMonth();
            }
        });
        nextDateBtn = (ImageView) findViewById(R.id.image_next);
        nextDateBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentMode == DAY_MODE)
                    navigateToNextDay();
                else if(currentMode == WEEK_MODE)
                    navigateToNextWeek();
                else
                    navigateToNextMonth();
            }
        });
        if(isToday(cal)){
            nextDateBtn.setVisibility(View.INVISIBLE);
        }
    }

    private void initChartValues(){
        Timber.d("INIT CHART");
        hasFoodOrMedic = false;

        //chart.clear();
        chart.getXAxis().resetAxisMaxValue();
        chart.getXAxis().setAxisMinValue(0);
        chart.fitScreen();

        chart.invalidate();

        chartValueIcon.setImageResource(icons[currentType]);
        chartValue.setText("--");
        chartUnit.setText("");
        chartTime.setText("--");

        tvLabelX.setVisibility(View.VISIBLE);
        tvLabelY.setVisibility(View.VISIBLE);
        if(currentMode==DAY_MODE){
            tvLabelX.setText(R.string.hour);
        }else if(currentMode==WEEK_MODE){
            tvLabelX.setText(R.string.day);
        }else{
            tvLabelX.setText(R.string.day);
        }

        if(currentType==GLUCOSE_TYPE){
            tvLabelY.setText(R.string.display_unit_mmol_per_l);
        }else if(currentType==BLOOD_PRESSURE_TYPE){
            tvLabelY.setText(R.string.display_unit_blood_pressure_si);
        }else if(currentType==WEIGHT_TYPE){
            tvLabelY.setText(R.string.display_unit_weight_si);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chart_view);
        context = this;
        /*if(mRealm==null) {
            Timber.d("mRealm is nulll");
            mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
        }else{
            Timber.d("mRealm is not nulll");
        }*/
        Timber.d("Init mRealm");
        mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
        initFoodMedicPanels();
        initChartTypes();
        initChartModes();
        initChartDate();
        initDateNavigation();
        initToolbar2();

        chartValueIcon = (ImageView) findViewById(R.id.chart_icon);
        chartValue = (TextView) findViewById(R.id.chart_value);
        chartUnit = (TextView) findViewById(R.id.chart_unit);
        chartTime = (TextView) findViewById(R.id.chart_time);

        final NestedScrollView nestedScrollView = (NestedScrollView)findViewById(R.id.scroll_view);
        nestedScrollView.setSmoothScrollingEnabled(true);
        // Set up Chart
        tvLabelX = (TextView) findViewById(R.id.text_label_x);
        tvLabelY = (TextView) findViewById(R.id.text_label_y);
        chart = (LineChart) findViewById(R.id.chart);
        //chart.setTouchEnabled(true); //display pop up
        //chart.setMarkerView(new ValueMarkerView (context));
        chart.setOnChartValueSelectedListener(this);
        chart.setNoDataText("No data");
        chart.getAxisLeft().setValueFormatter(new ChartValueFormatter());
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);

        int[] legendColors = new int[]{
                getResources().getColor(chartColours[BLOOD_PRESSURE_TYPE]),
                getResources().getColor(R.color.color_orange_dark) };

        String[] legendTexts = new String[]{ getResources().getString(R.string.systolic),
                getResources().getString(R.string.diastolic) };

        chart.getLegend().setCustom(legendColors,legendTexts);
        chart.getLegend().setPosition(LegendPosition.ABOVE_CHART_RIGHT);

        /*chart.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //nestedScrollView.requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });*/

        Paint p = chart.getPaint(Chart.PAINT_INFO);
        p.setColor(getResources().getColor(R.color.gray));

        chart.invalidate();

        createDayChart();
    }

    private void initFoodMedic(){
        foodMedicList.clear();
        foodMedicAdapter.notifyDataSetChanged();
    }

    private void displayFoodMedic(Date terumoDate){

        Calendar terumoCal = Calendar.getInstance();
        terumoCal.setTime(terumoDate);
        Timber.d("Display food medic of date = " + terumoDate.toString());

        initFoodMedic();
        retrieveFoodMedic(terumoCal);
        //SimpleDateFormat hourMinSdf = new SimpleDateFormat("HH:mm",Locale.ENGLISH);

        // boolean hasFood = false, hasMedic = false;
    }

    private void showFoodMedicPanel(){
       /* if(hasFoodOrMedic)
            foodMedicPanels.setVisibility(View.INVISIBLE);
        else
            foodMedicPanels.setVisibility(View.GONE);
        foodPanel.setVisibility(View.GONE);
        medicPanel.setVisibility(View.GONE);*/
        foodMedicListView.setVisibility(View.VISIBLE);
    }

    private void hideFoodMedicPanel(){
        if(hasFoodOrMedic)
            foodMedicPanels.setVisibility(View.INVISIBLE);
        else
            foodMedicPanels.setVisibility(View.GONE);
        foodPanel.setVisibility(View.GONE);
        medicPanel.setVisibility(View.GONE);
        foodMedicListView.setVisibility(View.GONE);
    }

    private void navigateToCurrentDay(){
        cal.set(Calendar.DAY_OF_MONTH, -1);
        setDailyDate();
        createDayChart();
    }

    private void navigateToPrevDay(){
        cal.add(Calendar.DAY_OF_MONTH, -1);
        weekStartCal = (Calendar)cal.clone();
        weekEndCal = (Calendar)cal.clone();
        monthStartCal = (Calendar)cal.clone();
        monthEndCal = (Calendar)cal.clone();

        setDailyDate();
        createDayChart();
    }

    private void navigateToNextDay(){
        cal.add(Calendar.DAY_OF_MONTH, 1);
        weekStartCal = (Calendar)cal.clone();
        weekEndCal = (Calendar)cal.clone();
        monthStartCal = (Calendar)cal.clone();
        monthEndCal = (Calendar)cal.clone();

        setDailyDate();
        createDayChart();
    }

    private void navigateToPrevWeek(){
        weekStartCal.add(Calendar.DAY_OF_MONTH, -7);
        weekEndCal.add(Calendar.DAY_OF_MONTH, -7);
        setWeeklyDate();
        createWeekChart();
    }

    private void navigateToNextWeek(){
        weekStartCal.add(Calendar.DAY_OF_MONTH, 7);
        weekEndCal.add(Calendar.DAY_OF_MONTH, 7);
        setWeeklyDate();
        createWeekChart();
    }

    private void navigateToPrevMonth(){
        monthStartCal.add(Calendar.MONTH, -1);
        monthEndCal.add(Calendar.MONTH, -1);
        setMonthlyDate();
        createMonthChart();
    }

    private void navigateToNextMonth(){
        monthStartCal.add(Calendar.MONTH, 1);
        monthEndCal.add(Calendar.MONTH, 1);
        setMonthlyDate();
        createMonthChart();
    }

    private void selectTab(int tabId){
        currentMode = tabId;

        for(int i = 0; i < 3; i++){
            if(i == tabId){
                //rlTimeSelectors[i].setBackgroundResource(R.drawable.chart_tab_selected);
                //tvTimeSelectors[i].setTextColor(nonSelectedColor);
                rlTimeSelectors[i].setSelected(true);
            }
            else{
                //rlTimeSelectors[i].setBackgroundResource(R.drawable.chart_tab_deselected);
                //rlTimeSelectors[i].getBackground().setColorFilter(nonSelectedColor,mode);
                //tvTimeSelectors[i].setTextColor(selectedColor);
                rlTimeSelectors[i].setSelected(false);
            }
        }
    }

    private void createDayChart(){
        initChartValues();
        initFoodMedic();
        Timber.d("CURR TYPE @ DAY CHART = " + currentType);
        switch(currentType) {
            case GLUCOSE_TYPE:
                showFoodMedicPanel();
                createGlucoseDayChart();
                break;

            case BLOOD_PRESSURE_TYPE:
                createBPDayChart();
                break;

            case WEIGHT_TYPE:
                createWeightDayChart();
                break;

            case TEMPERATURE_TYPE:
                createTempDayChart();
                break;

            case SPO2_TYPE:
                createSpo2DayChart();
                break;
        }

        // hideFoodMedicPanel();
    }

    private void retrieveFoodMedic(Calendar terumoDate){
        foodValues.clear();
        medicValues.clear();
        foodMedicList.clear();
        foodMedicAdapter.notifyDataSetChanged();

        Timber.d("retrieveFoodMedic");

        Calendar myDate = (Calendar)terumoDate.clone();
        RealmResults<Photo> photoList = PatientData.getInstance().getPhotoByDate(mRealm,terumoDate);
        RealmResults<Medication> medicList = PatientData.getInstance().getMedicationByDate(mRealm,terumoDate);
        RealmResults<Symptom> symptomList = PatientData.getInstance().getSymtomByDate(mRealm,terumoDate);
        RealmResults<Terumo> terumoList = PatientData.getInstance().getTerumoByDate(mRealm,terumoDate);
        RealmResults<BloodPressure> bpList = PatientData.getInstance().getBloodPressureByDate(mRealm,terumoDate);
        RealmResults<Weight> weightList = PatientData.getInstance().getWeightByDate(mRealm,terumoDate);
        RealmResults<Temperature> tempList = PatientData.getInstance().getTemperatureByDate(mRealm,terumoDate);
        RealmResults<Note> noteList = PatientData.getInstance().getNoteByDate(mRealm,terumoDate);
        RealmResults<SpO2> spO2List = PatientData.getInstance().getSpO2ByDate(mRealm, terumoDate);

        Timber.d("sp0List size %d", spO2List.size());

        Map<Date, RealmObject> map;

        //if (descend) {
        map = new TreeMap<>(Collections.reverseOrder());
        //} else {
        //  map = new TreeMap<>();
        //}

        for (int i = 0; i < photoList.size(); i++) {
            map.put(photoList.get(i).getDate(), photoList.get(i));
        }

        for (int i = 0; i < symptomList.size(); i++) {
            map.put(symptomList.get(i).getDate(), symptomList.get(i));
        }

        for (int i = 0; i < medicList.size(); i++) {
            map.put(medicList.get(i).getDate(), medicList.get(i));
        }

        for (int i = 0; i < terumoList.size(); i++) {
            map.put(terumoList.get(i).getDate(), terumoList.get(i));
        }

        for (int i = 0; i < bpList.size(); i++) {
            map.put(bpList.get(i).getDate(), bpList.get(i));
        }

        for (int i = 0; i < weightList.size(); i++) {
            map.put(weightList.get(i).getDate(), weightList.get(i));
        }

        for (int i = 0; i < tempList.size(); i++) {
            map.put(tempList.get(i).getDate(), tempList.get(i));
        }

        for (int i = 0; i < noteList.size(); i++) {
            map.put(noteList.get(i).getDate(), noteList.get(i));
        }

        for (int i = 0; i < spO2List.size(); i++) {
            map.put(spO2List.get(i).getDate(), spO2List.get(i));
        }

        if(currentMode==DAY_MODE) {
            for (Map.Entry<Date, RealmObject> entry : map.entrySet()) {
                RealmObject ro = entry.getValue();
                Date dataDate = null;
                if(ro instanceof Photo){
                    dataDate = ((Photo) ro).getDate();
                }else if(ro instanceof Medication){
                    dataDate = ((Medication) ro).getDate();
                }else if(ro instanceof Symptom){
                    dataDate = ((Symptom) ro).getDate();
                }else if(ro instanceof Note){
                    dataDate = ((Note) ro).getDate();
                }
                if(dataDate!=null) {
                    if (isDataWithinOneHour(dataDate, myDate.getTime())) {
                        foodMedicList.add(ro);
                    }
                }
            }
        }else{
            for (Map.Entry<Date, RealmObject> entry : map.entrySet()) {
                RealmObject ro = entry.getValue();
                if(currentType == GLUCOSE_TYPE){
                    if(!(ro instanceof BloodPressure) && !(ro instanceof Weight)
                            && !(ro instanceof Temperature) && !(ro instanceof SpO2)){
                        foodMedicList.add(ro);
                    }
                }else if(currentType == BLOOD_PRESSURE_TYPE){
                    if(ro instanceof BloodPressure){
                        foodMedicList.add(ro);
                    }
                }else if(currentType == WEIGHT_TYPE){
                    if(ro instanceof Weight){
                        foodMedicList.add(ro);
                    }
                }else if(currentType == TEMPERATURE_TYPE){
                    if(ro instanceof Temperature){
                        foodMedicList.add(ro);
                    }
                } else if (currentType == SPO2_TYPE) {
                    if (ro instanceof SpO2) {
                        foodMedicList.add(ro);
                    }
                }
            }
        }

        foodMedicAdapter.notifyDataSetChanged();

    }

    private boolean isDataWithinOneHour(Date dataDate, Date glucoseDate){
        long secs = Math.abs((dataDate.getTime() - glucoseDate.getTime()) / 1000);
        double hourDiff = secs / 3600d;
        Timber.d("Date 1 = " + dataDate.toString());
        Timber.d("Date 2 = " + glucoseDate.toString());
        Timber.d("Is Data Within One Hour = " + (hourDiff));
        return hourDiff <= 1d;
    }

    private void setChartNoData(){
        chart.clear();
        chart.invalidate();
        tvLabelX.setVisibility(View.INVISIBLE);
        tvLabelY.setVisibility(View.INVISIBLE);
    }

    private void createGlucoseDayChart(){
        //clear the lists every time a chart is created
        xVals.clear();
        dataSets.clear();
        values.clear();
        values2.clear();
        foodValues.clear();
        foodValues2.clear();
        medicValues.clear();
        medicValues2.clear();

        /*TODO:if(mRealm==null){
            mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
        }*/

        RealmResults terumoList = PatientData.getInstance().getTerumoByDate(mRealm,cal);
        RealmResults photoList = PatientData.getInstance().getPhotoByDate(mRealm,cal);
        RealmResults medicList = PatientData.getInstance().getMedicationByDate(mRealm,cal);

        if(terumoList.size()<= 0){
            setChartNoData();
            //
            return;
        }else{
            //TODO: check
            for(int i = 0; i < terumoList.size(); i++){
                Terumo terumo = (Terumo)terumoList.get(i);
                Timber.d("Terumo " + i + " : " + terumo.getEntityId() + ", " + terumo.getValue() + ", " + terumo.getDate().toString());

                long count = PatientData.getInstance().getTerumoBySpecificDateCount(mRealm,((Terumo) terumoList.get(i)).getDate());
                Timber.d("Terumo count = " + count);
            }
        }

        Terumo terumo1 = (Terumo) terumoList.get(0);
        Date terumoDate1 = terumo1.getDate();
        SimpleDateFormat sdf1 = new SimpleDateFormat("HH", Locale.ENGLISH);
        String timeStr1 = sdf1.format(terumoDate1);
        int time1 = Integer.parseInt(timeStr1);
        //int offset = 0;
        if(time1 > 6) {
            chart.getXAxis().setAxisMinValue(6);
        }else{
            chart.getXAxis().setAxisMinValue(0);
        }

        for (int i = 0; i < 24; i++) {
            if (i % 2 == 0)
                xVals.add(i + "");
            else
                xVals.add("");
        }
        int size = 0;

        float maxVal = GLUCOSE_DEFAULT_MAX_VALUE;
        float minVal = GLUCOSE_DEFAULT_MIN_VALUE;
        size = terumoList.size() < 24 ? terumoList.size() : 24;
        for(int i = 0; i < size; i++){
            Terumo terumo = (Terumo) terumoList.get(i);
            Date terumoDate = terumo.getDate();
            SimpleDateFormat sdf = new SimpleDateFormat("HH", Locale.ENGLISH);
            String timeStr = sdf.format(terumoDate);
            int time = Integer.parseInt(timeStr); // - offset
            //xVals.add(time);
            values.add(new Entry(Float.parseFloat(""+terumo.getMmolValue()),time,terumo));
            values2.add(new Entry(Float.parseFloat(""+terumo.getMmolValue()),time,terumo));

            if(terumo.getMmolValue()>maxVal ){
                maxVal = (float)terumo.getMmolValue();
            }
            if(terumo.getMmolValue()<minVal ){
                minVal = (float)terumo.getMmolValue();
            }
        }

        chart.getAxisLeft().setAxisMaxValue(maxVal);
        chart.getAxisLeft().setAxisMinValue(minVal);

        int prevTime = -1;
        for(int i = 0; i < photoList.size(); i++){
            Photo photo = (Photo) photoList.get(i);
            Date photoDate = photo.getDate();
            SimpleDateFormat sdf = new SimpleDateFormat("HH");
            String timeStr = sdf.format(photoDate);
            int time = Integer.parseInt(timeStr);
            if(prevTime==time) {
                time++;
            }
            foodValues.add(new Entry(0.5f, time, photo));
            foodValues2.add(new Entry(0.5f, time, photo));
            prevTime = time;
        }

        prevTime = -1;

        for(int i = 0; i < medicList.size(); i++){
            Medication medic = (Medication) medicList.get(i);
            Date photoDate = medic.getDate();
            SimpleDateFormat sdf = new SimpleDateFormat("HH",Locale.ENGLISH);
            String timeStr = sdf.format(photoDate);
            int time = Integer.parseInt(timeStr);
            if(prevTime==time) {
                time++;
            }
            medicValues.add(new Entry(1.5f,time,medic));
            medicValues2.add(new Entry(1.5f,time,medic));

            prevTime = time;
        }

        if(values.size()> 0 && (foodValues.size() > 0 || medicValues.size() > 0)){
            hasFoodOrMedic = true;
        }
        else{
            hasFoodOrMedic = false;
        }
        Timber.d("HAS FOOD OR MEDIC = " + hasFoodOrMedic);

        normalDataset = new LineDataSet(values, "DataSet ");
        normalDataset.setDrawValues(false);
        normalDataset.setColor(ContextCompat.getColor(context,chartColours[currentType]));
        normalDataset.setCircleColor(ContextCompat.getColor(context,chartColours[currentType]));
        normalDataset.setCircleRadius(circle_radius);
        normalDataset.setCircleHoleRadius(circle_hole_radius);
        //normalDataset.setDrawCircleHole(false);

        circleDataset = new LineDataSet(values2,"DataSet ");
        circleDataset.setDrawValues(false);
        circleDataset.setColor(ContextCompat.getColor(context,chartColours[currentType]));
        circleDataset.setCircleColor(ContextCompat.getColor(context,chartColours[currentType]));
        circleDataset.setCircleColorHole(ContextCompat.getColor(context,chartColours[currentType]));
        circleDataset.setCircleRadius(circle_radius);
        circleDataset.setCircleHoleRadius(circle_hole_radius);
        circleDataset.clear();

        int chartColor = chartColours[currentType];

        foodDataset = new LineDataSet(foodValues, "Food ");
        foodDataset.setDrawValues(false);
        foodDataset.setColor(ContextCompat.getColor(context,chartColor));
        foodDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        foodDataset.setCircleRadius(circle_radius);
        foodDataset.setCircleHoleRadius(circle_hole_radius);

        circleFoodDataset = new LineDataSet(foodValues2, "Food ");
        circleFoodDataset.setDrawValues(false);
        circleFoodDataset.setColor(ContextCompat.getColor(context,chartColor));
        circleFoodDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        circleFoodDataset.setCircleColorHole(ContextCompat.getColor(context,chartColor));
        circleFoodDataset.setCircleRadius(circle_radius);
        circleFoodDataset.setCircleHoleRadius(circle_hole_radius);
        circleFoodDataset.clear();

        chartColor = chartColours[currentType];

        medicDataset = new LineDataSet(medicValues, "Medic ");
        medicDataset.setDrawValues(false);
        medicDataset.setColor(ContextCompat.getColor(context,chartColor));
        medicDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        medicDataset.setCircleRadius(circle_radius);
        medicDataset.setCircleHoleRadius(circle_hole_radius);

        circleMedicDataset = new LineDataSet(medicValues2, "Food ");
        circleMedicDataset.setDrawValues(false);
        circleMedicDataset.setColor(ContextCompat.getColor(context,chartColor));
        circleMedicDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        circleMedicDataset.setCircleColorHole(ContextCompat.getColor(context,chartColor));
        circleMedicDataset.setCircleRadius(circle_radius);
        circleMedicDataset.setCircleHoleRadius(circle_hole_radius);
        circleMedicDataset.clear();

        dataSets.add(normalDataset);
        dataSets.add(circleDataset);
        /*dataSets.add(foodDataset);
        dataSets.add(circleFoodDataset);
        dataSets.add(medicDataset);
        dataSets.add(circleMedicDataset);*/

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
        chart.setDescription("");
        //chart.setExtraOffsets(0,120,0,0);
        chart.getXAxis().setDrawGridLines(false);

        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setAvoidFirstLastClipping(true);
        chart.getXAxis().setLabelsToSkip(0);
        chart.getXAxis().setEnabled(true);

        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);
        //chart.setOnChartGestureListener(new CustomChartGestureListener());
        chart.invalidate();
    }

    private void createBPDayChart(){
        //clear the lists every time a chart is created
        xVals.clear();
        dataSets.clear();
        values.clear();
        values2.clear();

        ArrayList<Entry> values3 = new ArrayList<>();
        ArrayList<Entry> values4 = new ArrayList<>();
        /*TODO:if(mRealm==null){
            Timber.d("mRealm here is null!!!!");
            mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
        }*/

        RealmResults bpList = PatientData.getInstance().getBloodPressureByDate(mRealm,cal);

        if(bpList.size()<= 0){
            setChartNoData();
            //
            return;
        }

        for(int i = 0; i < 24; i++){
            if(i%2==0)
                xVals.add(i+"");
            else
                xVals.add("");
        }

        float maxVal = BP_DEFAULT_MAX_VALUE;
        float minVal = BP_DEFAULT_MIN_VALUE;
        int size = bpList.size() < 24 ? bpList.size() : 24;
        for(int i = 0; i < size; i++){
            BloodPressure bp = (BloodPressure) bpList.get(i);
            Date terumoDate = bp.getDate();
            SimpleDateFormat sdf = new SimpleDateFormat("HH");
            String timeStr = sdf.format(terumoDate);
            int time = Integer.parseInt(timeStr);
            //xVals.add(time);
            values.add(new Entry(bp.getSystolic(),time,bp));
            values2.add(new Entry(bp.getSystolic(),time,bp));
            values3.add(new Entry(bp.getDistolic(),time,bp));
            values4.add(new Entry(bp.getDistolic(),time,bp));

            if(bp.getSystolic()>maxVal ){
                maxVal = bp.getSystolic();
            }
            if(bp.getSystolic()<minVal ){
                minVal = bp.getSystolic();
            }
            if(bp.getDistolic()>maxVal ){
                maxVal = bp.getDistolic();
            }
            if(bp.getDistolic()<minVal ){
                minVal = bp.getDistolic();
            }
        }
        //chart.getAxisLeft().setAxisMaxValue(maxVal);
        //chart.getAxisLeft().setAxisMinValue(minVal);

        BloodPressure bp = (BloodPressure) bpList.get(0);
        Date terumoDate1 = bp.getDate();
        SimpleDateFormat sdf1 = new SimpleDateFormat("HH");
        String timeStr1 = sdf1.format(terumoDate1);
        int time1 = Integer.parseInt(timeStr1);
        //int offset = 0;
        if(time1 > 6) {
            chart.getXAxis().setAxisMinValue(6);
        }else{
            chart.getXAxis().setAxisMinValue(0);
        }

        int chartColor = chartColours[currentType];
        int chartColor2 = R.color.color_orange_dark;
        normalDataset = new LineDataSet(values, "DataSet ");
        normalDataset.setDrawValues(false);
        normalDataset.setColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleRadius(circle_radius);
        normalDataset.setCircleHoleRadius(circle_hole_radius);
        //normalDataset.setDrawCircleHole(false);

        circleDataset = new LineDataSet(values2,"DataSet ");
        circleDataset.setDrawValues(false);
        circleDataset.setColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColorHole(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleRadius(circle_radius);
        circleDataset.setCircleHoleRadius(circle_hole_radius);
        circleDataset.clear();

        diastolicDataset = new LineDataSet(values3,"Diastolic ");
        diastolicDataset.setDrawValues(false);
        diastolicDataset.setColor(ContextCompat.getColor(context,chartColor2));
        diastolicDataset.setCircleColor(ContextCompat.getColor(context,chartColor2));
        diastolicDataset.setCircleRadius(circle_radius);
        diastolicDataset.setCircleHoleRadius(circle_hole_radius);

        diastolicCircleDataset = new LineDataSet(values4,"Diastolic ");
        diastolicCircleDataset.setDrawValues(false);
        diastolicCircleDataset.setColor(ContextCompat.getColor(context,R.color.transparent));
        diastolicCircleDataset.setCircleColor(ContextCompat.getColor(context,R.color.transparent));
        diastolicCircleDataset.setCircleColorHole(ContextCompat.getColor(context,R.color.color_orange_dark));
        diastolicCircleDataset.setCircleRadius(circle_radius);
        diastolicCircleDataset.setCircleHoleRadius(circle_hole_radius);
        diastolicCircleDataset.clear();

        dataSets.add(normalDataset);
        dataSets.add(circleDataset);
        dataSets.add(diastolicDataset);
        dataSets.add(diastolicCircleDataset);

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
        chart.setDescription("");
        //chart.setExtraOffsets(0,120,0,0);
        chart.getXAxis().setDrawGridLines(false);

        chart.getXAxis().setPosition(XAxisPosition.BOTTOM);
        chart.getXAxis().setAvoidFirstLastClipping(true);
        chart.getXAxis().setLabelsToSkip(0);
        chart.getXAxis().setEnabled(true);

        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(true);
        chart.setDragEnabled(true);

        //chart.setOnChartGestureListener(new CustomChartGestureListener());
        chart.invalidate();
    }

    private void createWeightDayChart(){
        //clear the lists every time a chart is created
        xVals.clear();
        dataSets.clear();
        values.clear();
        values2.clear();

        RealmResults weightList = PatientData.getInstance().getWeightByDate(mRealm,cal);

        if(weightList.size()<= 0){
            setChartNoData();
            //
            return;
        }

        for(int i = 0; i < 24; i++){
            if(i%2==0)
                xVals.add(i+"");
            else
                xVals.add("");
        }

        float maxVal = WEIGHT_DEFAULT_MAX_VALUE;
        float minVal = WEIGHT_DEFAULT_MIN_VALUE;
        int size = weightList.size() < 24 ? weightList.size() : 24;
        for(int i = 0; i < size; i++){
            Weight weight = (Weight) weightList.get(i);
            Date terumoDate = weight.getDate();
            SimpleDateFormat sdf = new SimpleDateFormat("HH");
            String timeStr = sdf.format(terumoDate);
            int time = Integer.parseInt(timeStr);
            //xVals.add(time);
            values.add(new Entry(Float.parseFloat(""+weight.getWeight()),time,weight));
            values2.add(new Entry(Float.parseFloat(""+weight.getWeight()),time,weight));

            if(weight.getWeight()>maxVal ){
                maxVal = (float)weight.getWeight();
            }
            if(weight.getWeight()<minVal ){
                maxVal = (float)weight.getWeight();
            }
        }

        chart.getAxisLeft().setAxisMaxValue(maxVal);
        chart.getAxisLeft().setAxisMinValue(minVal);

        Weight weight = (Weight) weightList.get(0);
        Date terumoDate1 = weight.getDate();
        SimpleDateFormat sdf1 = new SimpleDateFormat("HH", Locale.ENGLISH);
        String timeStr1 = sdf1.format(terumoDate1);
        int time1 = Integer.parseInt(timeStr1);
        //int offset = 0;
        if(time1 > 6) {
            chart.getXAxis().setAxisMinValue(6);
        }else{
            chart.getXAxis().setAxisMinValue(0);
        }

        int chartColor = chartColours[currentType];
        normalDataset = new LineDataSet(values, "DataSet ");
        normalDataset.setDrawValues(false);
        normalDataset.setColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleRadius(circle_radius);
        normalDataset.setCircleHoleRadius(circle_hole_radius);
        //normalDataset.setDrawCircleHole(false);

        circleDataset = new LineDataSet(values2,"DataSet ");
        circleDataset.setDrawValues(false);
        circleDataset.setColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColorHole(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleRadius(circle_radius);
        circleDataset.setCircleHoleRadius(circle_hole_radius);
        circleDataset.clear();

        dataSets.add(normalDataset);
        dataSets.add(circleDataset);

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
        chart.setDescription("");
        //chart.setExtraOffsets(0,120,0,0);
        chart.getXAxis().setDrawGridLines(false);
        chart.setDragEnabled(true);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setAvoidFirstLastClipping(true);
        chart.getXAxis().setLabelsToSkip(0);
        chart.getXAxis().setEnabled(true);

        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);
        //chart.setOnChartGestureListener(new CustomChartGestureListener());
        chart.invalidate();
    }

    private void createTempDayChart(){
        //clear the lists every time a chart is created
        xVals.clear();
        dataSets.clear();
        values.clear();
        values2.clear();

        RealmResults tempList = PatientData.getInstance().getTemperatureByDate(mRealm,cal);

        if(tempList.size()<= 0){
            setChartNoData();
            //
            return;
        }

        for(int i = 0; i < 24; i++){
            if(i%2==0)
                xVals.add(i+"");
            else
                xVals.add("");
        }

        float maxVal = TEMP_DEFAULT_MAX_VALUE;
        float minVal = TEMP_DEFAULT_MIN_VALUE;
        int size = tempList.size() < 24 ? tempList.size() : 24;
        for(int i = 0; i < size; i++){
            Temperature temp = (Temperature) tempList.get(i);
            Date terumoDate = temp.getDate();
            SimpleDateFormat sdf = new SimpleDateFormat("HH");
            String timeStr = sdf.format(terumoDate);
            int time = Integer.parseInt(timeStr);
            //xVals.add(time);
            values.add(new Entry(Float.parseFloat(""+temp.getValue()),time,temp));
            values2.add(new Entry(Float.parseFloat(""+temp.getValue()),time,temp));

            if(temp.getValue()>maxVal ){
                maxVal = (float)temp.getValue();
            }
            if(temp.getValue()<minVal ){
                minVal = (float)temp.getValue();
            }
        }

        chart.getAxisLeft().setAxisMaxValue(maxVal);
        chart.getAxisLeft().setAxisMinValue(minVal);

        Temperature temp = (Temperature) tempList.get(0);
        Date terumoDate1 = temp.getDate();
        SimpleDateFormat sdf1 = new SimpleDateFormat("HH");
        String timeStr1 = sdf1.format(terumoDate1);
        int time1 = Integer.parseInt(timeStr1);
        //int offset = 0;
        if(time1 > 6) {
            chart.getXAxis().setAxisMinValue(6);
        }else{
            chart.getXAxis().setAxisMinValue(0);
        }

        int chartColor = chartColours[currentType];
        normalDataset = new LineDataSet(values, "DataSet ");
        normalDataset.setDrawValues(false);
        normalDataset.setColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleRadius(circle_radius);
        normalDataset.setCircleHoleRadius(circle_hole_radius);
        //normalDataset.setDrawCircleHole(false);

        circleDataset = new LineDataSet(values2,"DataSet ");
        circleDataset.setDrawValues(false);
        circleDataset.setColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColorHole(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleRadius(circle_radius);
        circleDataset.setCircleHoleRadius(circle_hole_radius);
        circleDataset.clear();

        dataSets.add(normalDataset);
        dataSets.add(circleDataset);

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
        chart.setDescription("");
        //chart.setExtraOffsets(0,120,0,0);
        chart.getXAxis().setDrawGridLines(false);

        chart.getXAxis().setPosition(XAxisPosition.BOTTOM);
        chart.getXAxis().setAvoidFirstLastClipping(true);
        chart.getXAxis().setLabelsToSkip(0);
        chart.getXAxis().setEnabled(true);

        //chart.setOnChartGestureListener(new CustomChartGestureListener());
        chart.invalidate();
    }

    private void createSpo2DayChart() {
        //clear the lists every time a chart is created
        xVals.clear();
        dataSets.clear();
        values.clear();
        values2.clear();

        RealmResults spO2List = PatientData.getInstance().getSpO2ByDate(mRealm,cal);

        if(spO2List.size()<= 0){
            setChartNoData();
            //
            return;
        }

        for(int i = 0; i < 24; i++){
            if(i%2==0)
                xVals.add(i+"");
            else
                xVals.add("");
        }

        float maxVal = SPO2_DEFAULT_MAX_VALUE;
        float minVal = SPO2_DEFAULT_MIN_VALUE;
        int size = spO2List.size() < 24 ? spO2List.size() : 24;
        for(int i = 0; i < size; i++){
            SpO2 spO2 = (SpO2) spO2List.get(i);
            Date terumoDate = spO2.getDate();
            SimpleDateFormat sdf = new SimpleDateFormat("HH");
            String timeStr = sdf.format(terumoDate);
            int time = Integer.parseInt(timeStr);
            //xVals.add(time);
            values.add(new Entry(Float.parseFloat(""+spO2.getPulseRate()),time,spO2));
            values2.add(new Entry(Float.parseFloat(""+spO2.getPulseRate()),time,spO2));

            if(spO2.getPulseRate()>maxVal ){
                maxVal = (float)spO2.getPulseRate();
            }
            if(spO2.getPulseRate()<minVal ){
                maxVal = (float)spO2.getPulseRate();
            }
        }

        chart.getAxisLeft().setAxisMaxValue(maxVal);
        chart.getAxisLeft().setAxisMinValue(minVal);

        SpO2 spO2 = (SpO2) spO2List.get(0);
        Date terumoDate1 = spO2.getDate();
        SimpleDateFormat sdf1 = new SimpleDateFormat("HH", Locale.ENGLISH);
        String timeStr1 = sdf1.format(terumoDate1);
        int time1 = Integer.parseInt(timeStr1);
        //int offset = 0;
        if(time1 > 6) {
            chart.getXAxis().setAxisMinValue(6);
        }else{
            chart.getXAxis().setAxisMinValue(0);
        }

        int chartColor = chartColours[currentType];
        normalDataset = new LineDataSet(values, "DataSet ");
        normalDataset.setDrawValues(false);
        normalDataset.setColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleRadius(circle_radius);
        normalDataset.setCircleHoleRadius(circle_hole_radius);
        //normalDataset.setDrawCircleHole(false);

        circleDataset = new LineDataSet(values2,"DataSet ");
        circleDataset.setDrawValues(false);
        circleDataset.setColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColorHole(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleRadius(circle_radius);
        circleDataset.setCircleHoleRadius(circle_hole_radius);
        circleDataset.clear();

        dataSets.add(normalDataset);
        dataSets.add(circleDataset);

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
        chart.setDescription("");
        //chart.setExtraOffsets(0,120,0,0);
        chart.getXAxis().setDrawGridLines(false);
        chart.setDragEnabled(true);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setAvoidFirstLastClipping(true);
        chart.getXAxis().setLabelsToSkip(0);
        chart.getXAxis().setEnabled(true);

        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);
        //chart.setOnChartGestureListener(new CustomChartGestureListener());
        chart.invalidate();
    }

    private void createWeekChart(){
        initChartValues();
        initFoodMedic();
        switch(currentType) {
            case GLUCOSE_TYPE:
                createGlucoseWeekChart();
                break;

            case BLOOD_PRESSURE_TYPE:
                createBPWeekChart();
                break;

            case WEIGHT_TYPE:
                createWeightWeekChart();
                break;

            case TEMPERATURE_TYPE:
                createTempWeekChart();
                break;

            case SPO2_TYPE:
                createSpO2WeekChart();
                break;
        }
        //hideFoodMedicPanel();
    }

    private void createGlucoseWeekChart(){
        //clear the lists every time a chart is created
        xVals.clear();
        dataSets.clear();
        values.clear();
        values2.clear();
        foodValues.clear();
        medicValues.clear();

        String[] days = { "Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        int[] count = new int[7];

        for(int i =0; i < 7; i++){

            xVals.add("");
            xVals.add(days[i]);
            xVals.add("");
            count[i] = i*3;
        }

        RealmList<Terumo> terumoList =
                PatientData.getInstance().getThreeTerumoPerDayByDateRange(mRealm,weekStartCal,weekEndCal);

        if(terumoList.size()<= 0){
            setChartNoData();
            //
            return;
        }

        float maxVal = GLUCOSE_DEFAULT_MAX_VALUE;
        float minVal = GLUCOSE_DEFAULT_MIN_VALUE;
        for(int i = 0; i < terumoList.size(); i++){
            Terumo terumo = terumoList.get(i);
            Date terumoDate = terumo.getDate();
            Calendar cal = Calendar.getInstance();
            cal.setTime(terumoDate);
            int day =  cal.get(Calendar.DAY_OF_WEEK);
            int index = day - 1;
            values.add(new Entry(Float.parseFloat(""+terumo.getMmolValue()),count[index],terumo));
            values2.add(new Entry(Float.parseFloat(""+terumo.getMmolValue()),count[index],terumo));
            count[index]++;
            if(terumo.getMmolValue()>maxVal ){
                maxVal = (float)terumo.getMmolValue();
            }
            if(terumo.getMmolValue()<minVal ){
                minVal = (float)terumo.getMmolValue();
            }
        }

        chart.getAxisLeft().setAxisMaxValue(maxVal);
        chart.getAxisLeft().setAxisMinValue(minVal);

        int chartColor = chartColours[currentType];
        normalDataset = new LineDataSet(values, "DataSet ");
        normalDataset.setDrawValues(false);
        normalDataset.setColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleRadius(circle_radius);
        normalDataset.setCircleHoleRadius(circle_hole_radius);

        circleDataset = new LineDataSet(values2,"DataSet ");
        circleDataset.setDrawValues(false);
        circleDataset.setColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColorHole(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleRadius(circle_radius);
        circleDataset.setCircleHoleRadius(circle_hole_radius);
        circleDataset.clear();

        dataSets.add(normalDataset);
        dataSets.add(circleDataset);

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
        chart.setDescription("");
        //chart.setExtraOffsets(0,120,0,0);
        chart.getXAxis().setDrawGridLines(false);

        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setAvoidFirstLastClipping(true);
        chart.getXAxis().setLabelsToSkip(0);
        chart.getXAxis().setEnabled(true);

        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);
        //chart.setOnChartGestureListener(new CustomChartGestureListener());
        chart.invalidate();
    }

    private void createBPWeekChart(){
        //clear the lists every time a chart is created
        xVals.clear();
        dataSets.clear();
        values.clear();
        values2.clear();
        ArrayList<Entry> values3 = new ArrayList<>();
        ArrayList<Entry> values4 = new ArrayList<>();
        RealmList<BloodPressure> bpList =
                PatientData.getInstance().getThreeBloodPressurePerDayByDateRange(mRealm,weekStartCal,weekEndCal);

        if(bpList.size()<= 0){
            setChartNoData();
            //
            return;
        }

        String[] days = { "Sun","Mon","Tue","Wed","Thu","Fri","Sat" };
        int[] count = new int[7];

        for(int i =0; i < 7; i++){
            xVals.add("");
            xVals.add(days[i]);
            xVals.add("");
            count[i] = i*3;
        }

        for(int i = 0; i < bpList.size(); i++){
            BloodPressure bp = bpList.get(i);
            Date terumoDate = bp.getDate();
            Calendar cal = Calendar.getInstance();
            cal.setTime(terumoDate);
            int day =  cal.get(Calendar.DAY_OF_WEEK);
            int index = day - 1;
        }

        float maxVal = BP_DEFAULT_MAX_VALUE;
        float minVal = BP_DEFAULT_MIN_VALUE;
        for(int i = 0; i < bpList.size(); i++){
            BloodPressure bp = bpList.get(i);
            Date terumoDate = bp.getDate();
            Calendar cal = Calendar.getInstance();
            cal.setTime(terumoDate);
            int day =  cal.get(Calendar.DAY_OF_WEEK);
            int index = day - 1;
            values.add(new Entry(bp.getSystolic(),count[index],bp));
            values2.add(new Entry(bp.getSystolic(),count[index],bp));
            values3.add(new Entry(bp.getDistolic(),count[index],bp));
            values4.add(new Entry(bp.getDistolic(),count[index],bp));

            count[index]++;

            if(bp.getSystolic()>maxVal ){
                maxVal = bp.getSystolic();
            }
            if(bp.getSystolic()<minVal ){
                minVal = bp.getSystolic();
            }
            if(bp.getDistolic()>maxVal ){
                maxVal = bp.getDistolic();
            }
            if(bp.getDistolic()<minVal ){
                minVal = bp.getDistolic();
            }
        }

        /*chart.getAxisLeft().setAxisMaxValue(maxVal);
        chart.getAxisLeft().setAxisMinValue(minVal);*/

        int chartColor = chartColours[currentType];
        int chartColor2 = R.color.color_orange_dark;
        normalDataset = new LineDataSet(values, "DataSet ");
        normalDataset.setDrawValues(false);
        normalDataset.setColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleRadius(circle_radius);
        normalDataset.setCircleHoleRadius(circle_hole_radius);

        circleDataset = new LineDataSet(values2,"DataSet ");
        circleDataset.setDrawValues(false);
        circleDataset.setColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColorHole(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleRadius(circle_radius);
        circleDataset.setCircleHoleRadius(circle_hole_radius);
        circleDataset.clear();

        diastolicDataset = new LineDataSet(values3,"Diastolic ");
        diastolicDataset.setDrawValues(false);
        diastolicDataset.setColor(ContextCompat.getColor(context,chartColor2));
        diastolicDataset.setCircleColor(ContextCompat.getColor(context,chartColor2));
        diastolicDataset.setCircleRadius(circle_radius);
        diastolicDataset.setCircleHoleRadius(circle_hole_radius);

        diastolicCircleDataset = new LineDataSet(values4,"Diastolic ");
        diastolicCircleDataset.setDrawValues(false);
        diastolicCircleDataset.setColor(ContextCompat.getColor(context,R.color.transparent));
        diastolicCircleDataset.setCircleColor(ContextCompat.getColor(context,R.color.transparent));
        diastolicCircleDataset.setCircleColorHole(ContextCompat.getColor(context,R.color.color_orange_dark));
        diastolicCircleDataset.setCircleRadius(circle_radius);
        diastolicCircleDataset.setCircleHoleRadius(circle_hole_radius);
        diastolicCircleDataset.clear();

        dataSets.add(normalDataset);
        dataSets.add(circleDataset);
        dataSets.add(diastolicDataset);
        dataSets.add(diastolicCircleDataset);

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
        chart.setDescription("");
        //chart.setExtraOffsets(0,120,0,0);
        chart.getXAxis().setDrawGridLines(false);

        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setAvoidFirstLastClipping(true);
        chart.getXAxis().setLabelsToSkip(0);
        chart.getXAxis().setEnabled(true);

        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(true);
        //chart.setOnChartGestureListener(new CustomChartGestureListener());
        chart.invalidate();
    }

    private void createWeightWeekChart(){
        //clear the lists every time a chart is created
        xVals.clear();
        dataSets.clear();
        values.clear();
        values2.clear();

        RealmList<Weight> weightList = PatientData.getInstance().getThreeWeightPerDayByDateRange(mRealm,weekStartCal,weekEndCal);

        if(weightList.size()<= 0){
            setChartNoData();
            //
            return;
        }

        String[] days = { "Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        int[] count = new int[7];

        for(int i =0; i < 7; i++){

            xVals.add("");
            xVals.add(days[i]);
            xVals.add("");
            count[i] = i*3;
        }

        for(int i = 0; i < weightList.size(); i++){
            Weight weight = weightList.get(i);
            Date terumoDate = weight.getDate();
            Calendar cal = Calendar.getInstance();
            cal.setTime(terumoDate);
            int day =  cal.get(Calendar.DAY_OF_WEEK);
            int index = day - 1;
        }

        float maxVal = WEIGHT_DEFAULT_MAX_VALUE;
        float minVal = WEIGHT_DEFAULT_MIN_VALUE;
        for(int i = 0; i < weightList.size(); i++){
            Weight weight = weightList.get(i);
            Date terumoDate = weight.getDate();
            Calendar cal = Calendar.getInstance();
            cal.setTime(terumoDate);
            int day =  cal.get(Calendar.DAY_OF_WEEK);
            int index = day - 1;
            values.add(new Entry(Float.parseFloat(""+weight.getWeight()),count[index],weight));
            values2.add(new Entry(Float.parseFloat(""+weight.getWeight()),count[index],weight));
            count[index]++;

            if(weight.getWeight()>maxVal ){
                maxVal = (float)weight.getWeight();
            }
            if(weight.getWeight()<minVal ){
                minVal = (float)weight.getWeight();
            }
        }

        chart.getAxisLeft().setAxisMaxValue(maxVal);
        chart.getAxisLeft().setAxisMinValue(minVal);

        int chartColor = chartColours[currentType];
        normalDataset = new LineDataSet(values, "DataSet ");
        normalDataset.setDrawValues(false);
        normalDataset.setColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleRadius(circle_radius);
        normalDataset.setCircleHoleRadius(circle_hole_radius);

        circleDataset = new LineDataSet(values2,"DataSet ");
        circleDataset.setDrawValues(false);
        circleDataset.setColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColorHole(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleRadius(circle_radius);
        circleDataset.setCircleHoleRadius(circle_hole_radius);
        circleDataset.clear();

        dataSets.add(normalDataset);
        dataSets.add(circleDataset);

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
        chart.setDescription("");
        //chart.setExtraOffsets(0,120,0,0);
        chart.getXAxis().setDrawGridLines(false);

        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setAvoidFirstLastClipping(true);
        chart.getXAxis().setLabelsToSkip(0);
        chart.getXAxis().setEnabled(true);

        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);
        //chart.setOnChartGestureListener(new CustomChartGestureListener());
        chart.invalidate();
    }

    private void createTempWeekChart(){
        //clear the lists every time a chart is created
        xVals.clear();
        dataSets.clear();
        values.clear();
        values2.clear();

        RealmList<Temperature> tempList = PatientData.getInstance().getThreeTempPerDayByDateRange(mRealm,weekStartCal,weekEndCal);

        if(tempList.size()<= 0){
            setChartNoData();
            //
            return;
        }

        String[] days = { "Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        int[] count = new int[7];

        for(int i =0; i < 7; i++){

            xVals.add("");
            xVals.add(days[i]);
            xVals.add("");
            count[i] = i*3;
        }

        for(int i = 0; i < tempList.size(); i++){
            Temperature temp = tempList.get(i);
            Date terumoDate = temp.getDate();
            Calendar cal = Calendar.getInstance();
            cal.setTime(terumoDate);
            int day =  cal.get(Calendar.DAY_OF_WEEK);
            int index = day - 1;

        }
        float maxVal = TEMP_DEFAULT_MAX_VALUE;
        float minVal = TEMP_DEFAULT_MIN_VALUE;
        for(int i = 0; i < tempList.size(); i++){
            Temperature temp = tempList.get(i);
            Date terumoDate = temp.getDate();
            Calendar cal = Calendar.getInstance();
            cal.setTime(terumoDate);
            int day =  cal.get(Calendar.DAY_OF_WEEK);
            int index = day - 1;
            values.add(new Entry(Float.parseFloat(""+temp.getValue()),count[index],temp));
            values2.add(new Entry(Float.parseFloat(""+temp.getValue()),count[index],temp));
            count[index]++;

            if(temp.getValue()>maxVal ){
                maxVal = (float)temp.getValue();
            }
            if(temp.getValue()<minVal ){
                minVal = (float)temp.getValue();
            }
        }

        chart.getAxisLeft().setAxisMaxValue(maxVal);
        chart.getAxisLeft().setAxisMinValue(minVal);

        int chartColor = chartColours[currentType];
        normalDataset = new LineDataSet(values, "DataSet ");
        normalDataset.setDrawValues(false);
        normalDataset.setColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleRadius(circle_radius);
        normalDataset.setCircleHoleRadius(circle_hole_radius);

        circleDataset = new LineDataSet(values2,"DataSet ");
        circleDataset.setDrawValues(false);
        circleDataset.setColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColorHole(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleRadius(circle_radius);
        circleDataset.setCircleHoleRadius(circle_hole_radius);
        circleDataset.clear();

        dataSets.add(normalDataset);
        dataSets.add(circleDataset);

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
        chart.setDescription("");
        //chart.setExtraOffsets(0,120,0,0);
        chart.getXAxis().setDrawGridLines(false);

        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setAvoidFirstLastClipping(true);
        chart.getXAxis().setLabelsToSkip(0);
        chart.getXAxis().setEnabled(true);

        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);
        //chart.setOnChartGestureListener(new CustomChartGestureListener());
        chart.invalidate();
    }

    private void createSpO2WeekChart(){
        //clear the lists every time a chart is created
        xVals.clear();
        dataSets.clear();
        values.clear();
        values2.clear();

        RealmList<SpO2> spO2List = PatientData.getInstance().getThreeSpO2PerDayByDateRange(mRealm,weekStartCal,weekEndCal);

        if(spO2List.size()<= 0){
            setChartNoData();
            //
            return;
        }

        String[] days = { "Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        int[] count = new int[7];

        for(int i =0; i < 7; i++){

            xVals.add("");
            xVals.add(days[i]);
            xVals.add("");
            count[i] = i*3;
        }

        for(int i = 0; i < spO2List.size(); i++){
            SpO2 spO2 = spO2List.get(i);
            Date terumoDate = spO2.getDate();
            Calendar cal = Calendar.getInstance();
            cal.setTime(terumoDate);
            int day =  cal.get(Calendar.DAY_OF_WEEK);
            int index = day - 1;
        }

        float maxVal = SPO2_DEFAULT_MAX_VALUE;
        float minVal = SPO2_DEFAULT_MIN_VALUE;
        for(int i = 0; i < spO2List.size(); i++){
            SpO2 spO2 = spO2List.get(i);
            Date terumoDate = spO2.getDate();
            Calendar cal = Calendar.getInstance();
            cal.setTime(terumoDate);
            int day =  cal.get(Calendar.DAY_OF_WEEK);
            int index = day - 1;
            values.add(new Entry(Float.parseFloat(""+spO2.getPulseRate()),count[index],spO2));
            values2.add(new Entry(Float.parseFloat(""+spO2.getPulseRate()),count[index],spO2));
            count[index]++;

            if(spO2.getPulseRate()>maxVal ){
                maxVal = (float)spO2.getPulseRate();
            }
            if(spO2.getPulseRate()<minVal ){
                minVal = (float)spO2.getPulseRate();
            }
        }

        chart.getAxisLeft().setAxisMaxValue(maxVal);
        chart.getAxisLeft().setAxisMinValue(minVal);

        int chartColor = chartColours[currentType];
        normalDataset = new LineDataSet(values, "DataSet ");
        normalDataset.setDrawValues(false);
        normalDataset.setColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleRadius(circle_radius);
        normalDataset.setCircleHoleRadius(circle_hole_radius);

        circleDataset = new LineDataSet(values2,"DataSet ");
        circleDataset.setDrawValues(false);
        circleDataset.setColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColorHole(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleRadius(circle_radius);
        circleDataset.setCircleHoleRadius(circle_hole_radius);
        circleDataset.clear();

        dataSets.add(normalDataset);
        dataSets.add(circleDataset);

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
        chart.setDescription("");
        //chart.setExtraOffsets(0,120,0,0);
        chart.getXAxis().setDrawGridLines(false);

        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setAvoidFirstLastClipping(true);
        chart.getXAxis().setLabelsToSkip(0);
        chart.getXAxis().setEnabled(true);

        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);
        //chart.setOnChartGestureListener(new CustomChartGestureListener());
        chart.invalidate();
    }


    private void createMonthChart(){
        initChartValues();
        initFoodMedic();
        Timber.d("Current month = " + monthStartCal.getTime().toString() + " , " + monthEndCal.getTime().toString());
        switch(currentType) {
            case GLUCOSE_TYPE:
                createGlucoseMonthChart();
                break;

            case BLOOD_PRESSURE_TYPE:
                createBPMonthChart();
                break;

            case WEIGHT_TYPE:
                createWeightMonthChart();
                break;

            case TEMPERATURE_TYPE:
                createTempMonthChart();
                break;
        }
        //hideFoodMedicPanel();

    }

    private void createGlucoseMonthChart(){
        //clear the lists every time a chart is created
        xVals.clear();
        dataSets.clear();
        values.clear();
        values2.clear();
        foodValues.clear();
        medicValues.clear();

        RealmList<Terumo> terumoList = PatientData.getInstance().getOneTerumoPerDayByDateRange(mRealm,monthStartCal,monthEndCal);

        if(terumoList.size()<= 0){
            setChartNoData();
            //
            return;
        }

        for(int i =0; i < monthStartCal.getActualMaximum(Calendar.DAY_OF_MONTH); i++){
            if((i+1)%5 == 0)
                xVals.add(i+1+"");
            else
                xVals.add("");
        }
        float maxVal = GLUCOSE_DEFAULT_MAX_VALUE;
        float minVal = GLUCOSE_DEFAULT_MIN_VALUE;
        for(int i = 0; i < terumoList.size(); i++){
            Terumo terumo = (Terumo) terumoList.get(i);
            Date terumoDate = terumo.getDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd");
            String dayStr = sdf.format(terumoDate);
            int day = Integer.parseInt(dayStr);
            values.add(new Entry(Float.parseFloat(""+terumo.getMmolValue()),day-1,terumo));
            values2.add(new Entry(Float.parseFloat(""+terumo.getMmolValue()),day-1,terumo));

            if(terumo.getMmolValue()>maxVal ){
                maxVal = (float)terumo.getMmolValue();
            }
            if(terumo.getMmolValue()<minVal ){
                minVal = (float)terumo.getMmolValue();
            }
        }

        chart.getAxisLeft().setAxisMaxValue(maxVal);
        chart.getAxisLeft().setAxisMinValue(minVal);

        int chartColor = chartColours[currentType];
        normalDataset = new LineDataSet(values, "DataSet ");
        normalDataset.setDrawValues(false);
        normalDataset.setColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleRadius(circle_radius);
        normalDataset.setCircleHoleRadius(circle_hole_radius);

        circleDataset = new LineDataSet(values2,"DataSet ");
        circleDataset.setDrawValues(false);
        circleDataset.setColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColorHole(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleRadius(circle_radius);
        circleDataset.setCircleHoleRadius(circle_hole_radius);
        circleDataset.clear();

        dataSets.add(normalDataset);
        dataSets.add(circleDataset);

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
        chart.setDescription("");
        //chart.setExtraOffsets(0,120,0,0);
        chart.getXAxis().setDrawGridLines(false);

        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setAvoidFirstLastClipping(true);
        chart.getXAxis().setLabelsToSkip(0);
        chart.getXAxis().setEnabled(true);

        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);
        //chart.setOnChartGestureListener(new CustomChartGestureListener());
        chart.invalidate();
    }

    private void createBPMonthChart(){
        //clear the lists every time a chart is created
        xVals.clear();
        dataSets.clear();
        values.clear();
        values2.clear();

        ArrayList<Entry> values3 = new ArrayList<>();
        ArrayList<Entry> values4 = new ArrayList<>();

        RealmList<BloodPressure> bpList =
                PatientData.getInstance().getOneBloodPressurePerDayByDateRange(mRealm,monthStartCal,monthEndCal);

        if(bpList.size()<= 0){
            setChartNoData();
            //
            return;
        }

        for(int i =0; i < monthStartCal.getActualMaximum(Calendar.DAY_OF_MONTH); i++){
            if((i+1)%5==0)
                xVals.add(i+1+"");
            else
                xVals.add("");
        }
        float maxVal = BP_DEFAULT_MAX_VALUE;
        float minVal = BP_DEFAULT_MIN_VALUE;
        for(int i = 0; i < bpList.size(); i++){
            BloodPressure bp = bpList.get(i);
            Date terumoDate = bp.getDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd", Locale.ENGLISH);
            String dayStr = sdf.format(terumoDate);
            int day = Integer.parseInt(dayStr);
            values.add(new Entry(Float.parseFloat(""+bp.getSystolic()),day-1,bp));
            values2.add(new Entry(Float.parseFloat(""+bp.getSystolic()),day-1,bp));
            values3.add(new Entry(Float.parseFloat(""+bp.getDistolic()),day-1,bp));
            values4.add(new Entry(Float.parseFloat(""+bp.getDistolic()),day-1,bp));

            if(bp.getSystolic()>maxVal ){
                maxVal = bp.getSystolic();
            }
            if(bp.getSystolic()<minVal ){
                minVal = bp.getSystolic();
            }
            if(bp.getDistolic()>maxVal ){
                maxVal = bp.getDistolic();
            }
            if(bp.getDistolic()<minVal ){
                minVal = bp.getDistolic();
            }
        }

        /*chart.getAxisLeft().setAxisMaxValue(maxVal);
        chart.getAxisLeft().setAxisMinValue(minVal);
*/
        int chartColor = chartColours[currentType];
        int chartColor2 = R.color.color_orange_dark;
        normalDataset = new LineDataSet(values, "DataSet ");
        normalDataset.setDrawValues(false);
        normalDataset.setColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleRadius(circle_radius);
        normalDataset.setCircleHoleRadius(circle_hole_radius);

        circleDataset = new LineDataSet(values2,"DataSet ");
        circleDataset.setDrawValues(false);
        circleDataset.setColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColorHole(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleRadius(circle_radius);
        circleDataset.setCircleHoleRadius(circle_hole_radius);
        circleDataset.clear();

        diastolicDataset = new LineDataSet(values3,"Diastolic ");
        diastolicDataset.setDrawValues(false);
        diastolicDataset.setColor(ContextCompat.getColor(context,chartColor2));
        diastolicDataset.setCircleColor(ContextCompat.getColor(context,chartColor2));
        diastolicDataset.setCircleRadius(circle_radius);
        diastolicDataset.setCircleHoleRadius(circle_hole_radius);

        diastolicCircleDataset = new LineDataSet(values4,"Diastolic ");
        diastolicCircleDataset.setDrawValues(false);
        diastolicCircleDataset.setColor(ContextCompat.getColor(context,R.color.transparent));
        diastolicCircleDataset.setCircleColor(ContextCompat.getColor(context,R.color.transparent));
        diastolicCircleDataset.setCircleColorHole(ContextCompat.getColor(context,R.color.color_orange_dark));
        diastolicCircleDataset.setCircleRadius(circle_radius);
        diastolicCircleDataset.setCircleHoleRadius(circle_hole_radius);
        diastolicCircleDataset.clear();

        dataSets.add(normalDataset);
        dataSets.add(circleDataset);
        dataSets.add(diastolicDataset);
        dataSets.add(diastolicCircleDataset);

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
        chart.setDescription("");
        //chart.setExtraOffsets(0,120,0,0);
        chart.getXAxis().setDrawGridLines(false);

        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setAvoidFirstLastClipping(true);
        chart.getXAxis().setLabelsToSkip(0);
        chart.getXAxis().setEnabled(true);

        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(true);
        //chart.setOnChartGestureListener(new CustomChartGestureListener());
        chart.invalidate();
    }

    private void createWeightMonthChart(){
        //clear the lists every time a chart is created
        xVals.clear();
        dataSets.clear();
        values.clear();
        values2.clear();

        RealmList<Weight> weightList = PatientData.getInstance().getOneWeightPerDayByDateRange(mRealm,monthStartCal,monthEndCal);

        if(weightList.size()<= 0){
            setChartNoData();
            //
            return;
        }
        for(int i =0; i < monthStartCal.getActualMaximum(Calendar.DAY_OF_MONTH); i++){
            if((i+1)%5==0)
                xVals.add(i+1+"");
            else
                xVals.add("");
        }
        float maxVal = WEIGHT_DEFAULT_MAX_VALUE;
        float minVal = WEIGHT_DEFAULT_MIN_VALUE;
        for(int i = 0; i < weightList.size(); i++){
            Weight weight = (Weight) weightList.get(i);
            Date terumoDate = weight.getDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd");
            String dayStr = sdf.format(terumoDate);
            int day = Integer.parseInt(dayStr);
            values.add(new Entry(Float.parseFloat(""+weight.getWeight()),day-1,weight));
            values2.add(new Entry(Float.parseFloat(""+weight.getWeight()),day-1,weight));

            if(weight.getWeight()>maxVal ){
                maxVal = (float)weight.getWeight();
            }

            if(weight.getWeight()<minVal ){
                minVal = (float)weight.getWeight();
            }
        }

        chart.getAxisLeft().setAxisMaxValue(maxVal);
        chart.getAxisLeft().setAxisMinValue(minVal);

        int chartColor = chartColours[currentType];
        normalDataset = new LineDataSet(values, "DataSet ");
        normalDataset.setDrawValues(false);
        normalDataset.setColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleRadius(circle_radius);
        normalDataset.setCircleHoleRadius(circle_hole_radius);

        circleDataset = new LineDataSet(values2,"DataSet ");
        circleDataset.setDrawValues(false);
        circleDataset.setColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColorHole(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleRadius(circle_radius);
        circleDataset.setCircleHoleRadius(circle_hole_radius);
        circleDataset.clear();

        dataSets.add(normalDataset);
        dataSets.add(circleDataset);

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
        chart.setDescription("");
        //chart.setExtraOffsets(0,120,0,0);
        chart.getXAxis().setDrawGridLines(false);

        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setAvoidFirstLastClipping(true);
        chart.getXAxis().setLabelsToSkip(0);
        chart.getXAxis().setEnabled(true);

        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);
        //chart.setOnChartGestureListener(new CustomChartGestureListener());
        chart.invalidate();
    }

    private void createTempMonthChart(){
        //clear the lists every time a chart is created
        xVals.clear();
        dataSets.clear();
        values.clear();
        values2.clear();

        RealmList<Temperature> tempList = PatientData.getInstance().getOneTempPerDayByDateRange(mRealm,monthStartCal,monthEndCal);

        if(tempList.size()<= 0){
            setChartNoData();
            //
            return;
        }
        for(int i =0; i < monthStartCal.getActualMaximum(Calendar.DAY_OF_MONTH); i++){
            if((i+1)%5==0)
                xVals.add(i+1+"");
            else
                xVals.add("");
        }
        float maxVal = TEMP_DEFAULT_MAX_VALUE;
        float minVal = TEMP_DEFAULT_MIN_VALUE;
        for(int i = 0; i < tempList.size(); i++){
            Temperature temp = (Temperature) tempList.get(i);
            Date terumoDate = temp.getDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd");
            String dayStr = sdf.format(terumoDate);
            int day = Integer.parseInt(dayStr);
            values.add(new Entry(Float.parseFloat(""+temp.getValue()),day-1,temp));
            values2.add(new Entry(Float.parseFloat(""+temp.getValue()),day-1,temp));

            if(temp.getValue()>maxVal ){
                maxVal = (float)temp.getValue();
            }
            if(temp.getValue()<minVal ){
                minVal = (float)temp.getValue();
            }
        }

        chart.getAxisLeft().setAxisMaxValue(maxVal);
        chart.getAxisLeft().setAxisMinValue(minVal);

        int chartColor = chartColours[currentType];
        normalDataset = new LineDataSet(values, "DataSet ");
        normalDataset.setDrawValues(false);
        normalDataset.setColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleRadius(circle_radius);
        normalDataset.setCircleHoleRadius(circle_hole_radius);

        circleDataset = new LineDataSet(values2,"DataSet ");
        circleDataset.setDrawValues(false);
        circleDataset.setColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColorHole(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleRadius(circle_radius);
        circleDataset.setCircleHoleRadius(circle_hole_radius);
        circleDataset.clear();

        dataSets.add(normalDataset);
        dataSets.add(circleDataset);

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
        chart.setDescription("");
        //chart.setExtraOffsets(0,120,0,0);
        //chart.setOnChartGestureListener(new CustomChartGestureListener());
        chart.invalidate();
    }

    private void createSpO2MonthChart(){
        //clear the lists every time a chart is created
        xVals.clear();
        dataSets.clear();
        values.clear();
        values2.clear();

        RealmList<SpO2> spO2List = PatientData.getInstance().getOneSpO2PerDayByDateRange(mRealm,monthStartCal,monthEndCal);

        if(spO2List.size()<= 0){
            setChartNoData();
            //
            return;
        }
        for(int i =0; i < monthStartCal.getActualMaximum(Calendar.DAY_OF_MONTH); i++){
            if((i+1)%5==0)
                xVals.add(i+1+"");
            else
                xVals.add("");
        }
        float maxVal = WEIGHT_DEFAULT_MAX_VALUE;
        float minVal = WEIGHT_DEFAULT_MIN_VALUE;
        for(int i = 0; i < spO2List.size(); i++){
            SpO2 spO2 = (SpO2) spO2List.get(i);
            Date terumoDate = spO2.getDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd");
            String dayStr = sdf.format(terumoDate);
            int day = Integer.parseInt(dayStr);
            values.add(new Entry(Float.parseFloat(""+spO2.getPulseRate()),day-1,spO2));
            values2.add(new Entry(Float.parseFloat(""+spO2.getPulseRate()),day-1,spO2));

            if(spO2.getPulseRate()>maxVal ){
                maxVal = (float)spO2.getPulseRate();
            }

            if(spO2.getPulseRate()<minVal ){
                minVal = (float)spO2.getPulseRate();
            }
        }

        chart.getAxisLeft().setAxisMaxValue(maxVal);
        chart.getAxisLeft().setAxisMinValue(minVal);

        int chartColor = chartColours[currentType];
        normalDataset = new LineDataSet(values, "DataSet ");
        normalDataset.setDrawValues(false);
        normalDataset.setColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        normalDataset.setCircleRadius(circle_radius);
        normalDataset.setCircleHoleRadius(circle_hole_radius);

        circleDataset = new LineDataSet(values2,"DataSet ");
        circleDataset.setDrawValues(false);
        circleDataset.setColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColor(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleColorHole(ContextCompat.getColor(context,chartColor));
        circleDataset.setCircleRadius(circle_radius);
        circleDataset.setCircleHoleRadius(circle_hole_radius);
        circleDataset.clear();

        dataSets.add(normalDataset);
        dataSets.add(circleDataset);

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
        chart.setDescription("");
        //chart.setExtraOffsets(0,120,0,0);
        chart.getXAxis().setDrawGridLines(false);

        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setAvoidFirstLastClipping(true);
        chart.getXAxis().setLabelsToSkip(0);
        chart.getXAxis().setEnabled(true);

        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);
        //chart.setOnChartGestureListener(new CustomChartGestureListener());
        chart.invalidate();
    }

    private void setDailyDate(){
        currentDate = dailyDateFormat.format(cal.getTime());
        tvDate.setText(currentDate);
        if(!currentDate.equalsIgnoreCase(chartDate)){
            nextDateBtn.setVisibility(View.VISIBLE);
        }

        if(isToday(cal))
            nextDateBtn.setVisibility(View.INVISIBLE);
        else
            nextDateBtn.setVisibility(View.VISIBLE);
    }

    private void setWeeklyDate(){
        if(weekStartCal==null)
            weekStartCal = Calendar.getInstance();

        if(weekEndCal==null)
            weekEndCal = Calendar.getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.getInstance().SIMPLE_DATE_FORMAT);

        weekStartCal.add(Calendar.DAY_OF_WEEK, -(weekStartCal.get(Calendar.DAY_OF_WEEK)-1));
        String startDateOfWeek = sdf.format(weekStartCal.getTime());

        weekEndCal.add(Calendar.DAY_OF_WEEK,-(weekEndCal.get(Calendar.DAY_OF_WEEK)-7));
        String endDateOfWeek = sdf.format(weekEndCal.getTime());

        tvDate.setText(startDateOfWeek + " - " + endDateOfWeek);

        if(weekEndCal.getTime().after(Calendar.getInstance().getTime())){
            nextDateBtn.setVisibility(View.INVISIBLE);
        }
        else{
            nextDateBtn.setVisibility(View.VISIBLE);
        }
    }

    private void setMonthlyDate(){
        if(monthStartCal==null)
            monthStartCal = Calendar.getInstance();

        if(monthEndCal==null)
            monthEndCal = Calendar.getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat("MMM, yyyy");

        monthStartCal.set(
                monthStartCal.get(Calendar.YEAR),
                monthStartCal.get(Calendar.MONTH),
                monthStartCal.getActualMinimum(Calendar.DAY_OF_MONTH)
        );
        Timber.d("Start mth = " + monthStartCal.getTime().toString());
        monthEndCal.set(
                monthEndCal.get(Calendar.YEAR),
                monthEndCal.get(Calendar.MONTH),
                monthEndCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        );
        Timber.d("End mth = " + monthEndCal.getTime().toString());

        String month = sdf.format(monthEndCal.getTime());

        tvDate.setText(month);

        if(monthEndCal.getTime().after(Calendar.getInstance().getTime())){
            nextDateBtn.setVisibility(View.INVISIBLE);
        }
        else{
            nextDateBtn.setVisibility(View.VISIBLE);
        }
    }

    boolean isToday(Calendar cal){
        Calendar today = Calendar.getInstance();
        return (cal.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
                && cal.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                && cal.get(Calendar.YEAR) == today.get(Calendar.YEAR));
    }
    @Override
    public void onResume(){
        super.onResume();
        if(mRealm==null)
            mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
    }
    @Override
    public void onPause(){
        super.onPause();
        mRealm.close();
        mRealm=null;
    }

    private void createChart(){
        chart.clear();
        switch (currentMode){
            case DAY_MODE:
                createDayChart();
                break;

            case WEEK_MODE:
                createWeekChart();
                break;

            case MONTH_MODE:
                createMonthChart();
                break;
        }
    }

    private View.OnClickListener onTypeClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View view) {
            switch(view.getId()){
                case R.id.glucose_btn:
                    currentType = GLUCOSE_TYPE;
                    ((ImageView)findViewById(R.id.glucose_btn)).setImageResource(R.drawable.ic_blood_glucose);
                    ((ImageView)findViewById(R.id.bp_btn)).setImageResource(R.drawable.ic_blood_pressure_disabled);
                    ((ImageView)findViewById(R.id.weight_btn)).setImageResource(R.drawable.ic_weight_disabled);

                    break;
                case R.id.bp_btn:
                    currentType = BLOOD_PRESSURE_TYPE;
                    ((ImageView)findViewById(R.id.bp_btn)).setImageResource(R.drawable.ic_blood_pressure);
                    ((ImageView)findViewById(R.id.glucose_btn)).setImageResource(R.drawable.ic_blood_glucose_disabled);
                    ((ImageView)findViewById(R.id.weight_btn)).setImageResource(R.drawable.ic_weight_disabled);
                    break;
                case R.id.weight_btn:
                    currentType = WEIGHT_TYPE;
                    ((ImageView)findViewById(R.id.weight_btn)).setImageResource(R.drawable.ic_weight);
                    ((ImageView)findViewById(R.id.bp_btn)).setImageResource(R.drawable.ic_blood_pressure_disabled);
                    ((ImageView)findViewById(R.id.glucose_btn)).setImageResource(R.drawable.ic_blood_glucose_disabled);
                    break;
            }
            createChart();
        }

    };

    private void onCalendarPressed(){
        Calendar c = Calendar.getInstance();
        int currentDate = c.get(Calendar.DAY_OF_MONTH);
        int currentMonth = c.get(Calendar.MONTH);
        int currentYear = c.get(Calendar.YEAR);

        MonthAdapter.CalendarDay maxDate = new MonthAdapter.CalendarDay(currentYear, currentMonth,currentDate);
        CalendarDatePickerDialogFragment cdp = new CalendarDatePickerDialogFragment()
                .setDoneText(getString(R.string.dialog_ok))
                .setCancelText(getString(R.string.dialog_cancel))
                .setThemeCustom(R.style.MyCustomBetterPickersDialogs)
                .setDateRange(null,maxDate)
                .setPreselectedDate(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH))
                .setOnDateSetListener(onDateSetListener);

        cdp.show(getSupportFragmentManager(), "fragment_date_picker_name");
    }

    private CalendarDatePickerDialogFragment.OnDateSetListener onDateSetListener = new OnDateSetListener() {

        @Override
        public void onDateSet(CalendarDatePickerDialogFragment dialog, int year, int monthOfYear, int dayOfMonth) {
            cal.set(year,monthOfYear,dayOfMonth);
            weekStartCal = (Calendar)cal.clone();
            weekEndCal = (Calendar)cal.clone();
            monthStartCal = (Calendar)cal.clone();
            monthEndCal = (Calendar)cal.clone();

            if(currentMode==DAY_MODE) {
                setDailyDate();
                createDayChart();
            }
            else if(currentMode==WEEK_MODE){
                setWeeklyDate();
                createWeekChart();
            }
            else{
                setMonthlyDate();
                createMonthChart();
            }
        }
    };

    private void onSpinnerClick(){

    }

    private String getName(){
        String entityId = getSharedPreferences("lifecare_pref",Context.MODE_PRIVATE).getString("entity_id","");
        RealmResults<User> users = mRealm.where(User.class).equalTo("entityId",entityId).findAll();
        if(users.size()>0){
            User mUser = users.first();
            return mUser.getName();
        }
        return "";
    }

    private String exportToCSV() throws IOException {
        File dlStorageDir;
        // if there is external storage (sd card)
        if(Environment.getExternalStorageState() != null) {
            Timber.d("EXTERNAL DIRECTORY AVAIL. MAKING PATH TO EXTERNAL");
            dlStorageDir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Vitals"
            );
        }
        else {

            Timber.d("EXTERNAL DIRECTORY NOT AVAIL. MAKING PATH TO INTERNAL");
            dlStorageDir = new File(Environment.getDataDirectory()
                    + "/Vitals/");
        }

        if (!dlStorageDir.exists()) {
            if (!dlStorageDir.mkdirs()) {
                Timber.d("PROBLEM CREATING DIRECTORY");
                return null;
            }
        }

        Timber.d("DONE SETTING PATH FOR SAVING FILE ");
        Timber.d(" PATH FOR SAVING FILE =  " + dlStorageDir);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.ENGLISH);
        String timeStamp = sdf.format(new Date());

        String name = getName().replace(" ","_").toLowerCase();
        String type = typeNames[currentType];
        String csvFilePath = dlStorageDir.getPath() + File.separator + name + "_" + timeStamp + "_" + type + ".csv";
        FileWriter writer = new FileWriter(csvFilePath);

        switch(currentType){
            case GLUCOSE_TYPE:
                CSVUtils.writeLine(writer, Arrays.asList("Date", "Glucose Reading", "Unit", "Is Before Meal","Is Manual","Remark"));

                RealmResults<Terumo> terumoList =
                        PatientData.getInstance().getAllTerumo(mRealm);

                for (Terumo terumo : terumoList) {
                    List<String> list = new ArrayList<>();
                    list.add(sdf.format(terumo.getDate()));
                    list.add(terumo.getValue()+"");
                    list.add(terumo.getStringUnit());
                    list.add(terumo.isBeforeMeal()?"1":"0");
                    list.add(terumo.isManual()?"1":"0");
                    list.add(HtmlUtil.decodeString(terumo.getRemark()));

                    CSVUtils.writeLine(writer, list);
                }
                break;

            case BLOOD_PRESSURE_TYPE:
                CSVUtils.writeLine(writer, Arrays.asList("Date", "Systolic", "Diastolic", "Unit", "Pulse Rate", "Remark"));

                RealmResults<BloodPressure> bpList =
                        PatientData.getInstance().getAllBloodPressure(mRealm);

                for (BloodPressure bp : bpList) {

                    List<String> list = new ArrayList<>();
                    list.add(sdf.format(bp.getDate()));
                    list.add(bp.getSystolic()+"");
                    list.add(bp.getDistolic()+"");
                    list.add(bp.getStringUnit());
                    list.add(bp.getPulseRate()+"");
                    list.add(HtmlUtil.decodeString(bp.getRemark())); //TODO: remark

                    CSVUtils.writeLine(writer, list);
                }
                break;

            case WEIGHT_TYPE:
                CSVUtils.writeLine(writer, Arrays.asList("Date", "Weight", "Unit"));

                RealmResults<Weight> weightList =
                        PatientData.getInstance().getAllWeight(mRealm);

                for (Weight weight : weightList) {

                    List<String> list = new ArrayList<>();
                    list.add(sdf.format(weight.getDate()));
                    list.add(weight.getWeight()+"");
                    list.add(weight.getStringUnit());
                    //list.add(HtmlUtil.decodeString(weight.getRemark())); //TODO: remark

                    CSVUtils.writeLine(writer, list);
                }
                break;

            case TEMPERATURE_TYPE:
                CSVUtils.writeLine(writer, Arrays.asList("Date", "Temperature", "Unit"));

                RealmResults<Temperature> tempList =
                        PatientData.getInstance().getAllTemperature(mRealm);

                for (Temperature temp : tempList) {

                    List<String> list = new ArrayList<>();
                    list.add(sdf.format(temp.getDate()));
                    list.add(temp.getValue()+"");
                    list.add(temp.getStringUnit());
                    //list.add(HtmlUtil.decodeString(weight.getRemark())); //TODO: remark

                    CSVUtils.writeLine(writer, list);
                }
                break;
        }

        writer.flush();
        writer.close();
        Timber.d("CSV FILE PATH = " + csvFilePath);

        return csvFilePath;
    }

    private void sendCsvByEmail(String path){
        String bodyMessage = getName()+ "\n"
                + (typeNames[currentType].substring(0,1).toUpperCase() +
                typeNames[currentType].substring(1).replace("_", " ") + " readings");
        Uri uriPath = Uri.fromFile(new File(path));
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "example@gmail.com", null));
        List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(emailIntent, 0);
        List<LabeledIntent> intents = new ArrayList<>();
        for (ResolveInfo info : resolveInfos) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Vitals");
            intent.putExtra(Intent.EXTRA_TEXT,bodyMessage);
            intent.putExtra(Intent.EXTRA_STREAM,uriPath);
            intents.add(new LabeledIntent(intent, info.activityInfo.packageName, info.loadLabel(getPackageManager()), info.icon));
        }
        try {
            Intent chooser = Intent.createChooser(intents.remove(intents.size() - 1), "Send data via email...");
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new LabeledIntent[intents.size()]));
            startActivity(chooser);
        }catch(ClassCastException e){
            e.printStackTrace();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private ChartToolbar.OnToolbarClickListener mToolbarListener = new ChartToolbar.OnToolbarClickListener() {
        @Override
        public void leftButtonClick() {
            onBackPressed();
        }

        @Override public void rightButtonClick() {
           /* try {
                String path = exportToCSV();
                sendCsvByEmail(path);
            } catch (IOException e) {
                e.printStackTrace();
            }*/
        }

        @Override public void secondRightButtonClick() {

        }

        @Override
        public void spinnerClick() {
            onSpinnerClick();
        }
    };

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {

        //http://stackoverflow.com/questions/30011615/mpandroidchart-drawing-a-circle-in-the-last-entry-only

        SimpleDateFormat sdf = new SimpleDateFormat(markerTimeFormat[currentMode], Locale.ENGLISH);

        switch(currentType){
            case GLUCOSE_TYPE:
                if(e.getData() instanceof Terumo) {
                    Terumo terumo = (Terumo) e.getData();
                    Timber.d("Terumo clicked! = " + terumo.getEventId());
                    if(circleFoodDataset!=null)
                        circleFoodDataset.clear();
                    if(circleMedicDataset!=null)
                        circleMedicDataset.clear();

                    circleDataset.clear();
                    circleDataset.addEntry(e);

                    Date date = terumo.getDate();
                    displayFoodMedic(date);

                    //chartValueIcon.setImageResource(R.drawable.blood_glucose);
                    chartValue.setText((terumo.getMmolValue()+" ").replace(".0 ","").replace(" ",""));
                    chartUnit.setText(getResources().getString(R.string.display_unit_mmol_per_l));
                    chartTime.setText(sdf.format(terumo.getDate()));

                    long count = mRealm.where(Terumo.class)
                            .equalTo("date", terumo.getDate())
                            .equalTo("entityId",terumo.getEntityId())
                            .count();
                    Timber.d("count = " + count);
                }else if(e.getData() instanceof Photo) {
                    circleDataset.clear();
                    circleMedicDataset.clear();

                    circleFoodDataset.clear();
                    circleFoodDataset.addEntry(e);

                }else {
                    circleDataset.clear();
                    circleFoodDataset.clear();

                    circleMedicDataset.clear();
                    circleMedicDataset.addEntry(e);
                }
                break;

            case BLOOD_PRESSURE_TYPE:
                BloodPressure bp = (BloodPressure)e.getData();

                Timber.d("BP clicked! = " + bp.getEventId());
                if(e.getVal() == bp.getSystolic()){
                    circleDataset.clear();
                    circleDataset.addEntry(e);
                    diastolicCircleDataset.clear();
                    diastolicCircleDataset.addEntry(diastolicDataset.getEntryForIndex(normalDataset.getEntryIndex(e)));
                }else{
                    diastolicCircleDataset.clear();
                    diastolicCircleDataset.addEntry(e);
                    circleDataset.clear();
                    circleDataset.addEntry(normalDataset.getEntryForIndex(diastolicDataset.getEntryIndex(e)));
                }
                //chartValueIcon.setImageResource(R.drawable.blood_pressure);
                chartValue.setText((bp.getSystolic()+"/"+bp.getDistolic()+" ").replace(".0/","/").replace(".0 ","").replace(" ",""));
                chartUnit.setText(bp.getStringUnit());
                chartTime.setText(sdf.format(bp.getDate()));
                displayFoodMedic(bp.getDate());
                break;

            case WEIGHT_TYPE:
                Weight weight = (Weight)e.getData();

                Timber.d("Weight clicked! = " + weight.getEventId());
                circleDataset.clear();
                circleDataset.addEntry(e);
                //chartValueIcon.setImageResource(R.drawable.weight);
                chartValue.setText(weight.getWeight()+"");
                chartUnit.setText(weight.getStringUnit());
                chartTime.setText(sdf.format(weight.getDate()));
                displayFoodMedic(weight.getDate());
                break;

            case TEMPERATURE_TYPE:
                Temperature temp = (Temperature) e.getData();
                Timber.d("Temp clicked! = " + temp.getEventId());
                circleDataset.clear();
                circleDataset.addEntry(e);
                //chartValueIcon.setImageResource(R.drawable.weight);
                chartValue.setText(temp.getValue()+"");
                chartUnit.setText(temp.getStringUnit());
                chartTime.setText(sdf.format(temp.getDate()));
                displayFoodMedic(temp.getDate());
                break;

            case SPO2_TYPE:
                SpO2 spO2 = (SpO2) e.getData();
                Timber.d("Temp clicked! = " + spO2.getEventId());
                circleDataset.clear();
                circleDataset.addEntry(e);
                //chartValueIcon.setImageResource(R.drawable.weight);
                chartValue.setText((int)spO2.getValue()+"");
                chartUnit.setText(spO2.getStringUnit());
                chartTime.setText(sdf.format(spO2.getDate()));
                displayFoodMedic(spO2.getDate());
                break;
        }
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    @Override
    public void onNothingSelected() {
        if (circleDataset != null) circleDataset.clear();
        if (circleFoodDataset != null) circleFoodDataset.clear();
        if (circleMedicDataset != null) circleMedicDataset.clear();
        if(diastolicCircleDataset!=null) diastolicCircleDataset.clear();

        chart.notifyDataSetChanged();
        chart.invalidate();

        foodMedicList.clear();
        foodMedicAdapter.notifyDataSetChanged();

        //initChartValues();
    }
}

