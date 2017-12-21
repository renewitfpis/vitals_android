package sg.lifecare.medicare.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment.OnDateSetListener;
import com.codetroopers.betterpickers.calendardatepicker.MonthAdapter;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.getbase.floatingactionbutton.FloatingActionsMenu.OnFloatingActionsMenuUpdateListener;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import sg.lifecare.medicare.BuildConfig;
import sg.lifecare.medicare.MediCareApplication;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.LifecareAssisted;
import sg.lifecare.medicare.database.model.LifecareSharedPreference;
import sg.lifecare.medicare.database.model.Medication;
import sg.lifecare.medicare.database.model.Photo;
import sg.lifecare.medicare.database.model.SpO2;
import sg.lifecare.medicare.database.model.SpO2Set;
import sg.lifecare.medicare.database.model.Symptom;
import sg.lifecare.medicare.database.model.Temperature;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.database.model.User;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.database.sync.NetworkChangeReceiver;
import sg.lifecare.medicare.database.sync.SyncHandler;
import sg.lifecare.medicare.object.LocalMeasurementDevicesHandler;
import sg.lifecare.medicare.ui.adapter.PatientAdapter;
import sg.lifecare.medicare.ui.alarm.AlarmHandler;
import sg.lifecare.medicare.ui.alarm.AutoStartAlarm;
import sg.lifecare.medicare.ui.alarm.ReminderHandler;
import sg.lifecare.medicare.ui.fragment.OverviewFragment;
import sg.lifecare.medicare.ui.fragment.ProfileFragment;
import sg.lifecare.medicare.ui.fragment.TerumoOverviewFragment;
import sg.lifecare.medicare.ui.fragment.TimelineFragment;
import sg.lifecare.medicare.ui.fragment.TimelineFragment.TimelineFragmentListener;
import sg.lifecare.medicare.ui.measuring.MeasurementDeviceListActivity;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.utils.CircleTransform;
import sg.lifecare.medicare.utils.EnterpriseHandler;
import sg.lifecare.medicare.utils.LifeCareHandler;
import timber.log.Timber;


/**
 * Dashboard activity
 */
public class DashboardActivity extends AppCompatActivity implements
        OnNavigationItemSelectedListener,
        OnDateSetListener,
        TimelineFragmentListener,
        Callback {

    private NavigationView mNavigationView;
    private NavigationView mNavigationView2;
    private DrawerLayout mDrawer;

    private CustomToolbar mToolbar;
    private TimelineFragment mTimelineFragment;
    private OverviewFragment mOverviewFragment;
    private TerumoOverviewFragment mTerumoOverviewFrag;

    private Calendar mTodayDate;
    public Calendar mTimelineDate;

    private boolean mIsRequestRunning;

    private Realm mRealm;
    private User mUser;
    private String userEntityId; //user entity id
    private String entityId; //display entity id (can be user or caregiver)

    private int prevId = 0;
    boolean drawerClosed = false;
    boolean makingSelection = false;

    private ProgressBar progressBar;

    private FloatingActionsMenu floatingMenu, measureMenu;
    private LinearLayout navOverviewActLayout, navOverview, navActivity;
    private TextView textActivity, textOverview;
    private ImageView imageActivity, imageOverview;
    private RelativeLayout dimBackground;

    public static final int REQ_PHOTO = 3;
    public static final int REQ_SYMPTOM = 4;
    public static final int REQ_MEDIC = 5;
    public static final int REQ_NOTES = 6;

    public static boolean isSyncing = false;
    public static boolean isSyncingProfile = false;

    private static boolean isCaregiver = false;
    private ArrayList<LifecareAssisted> assisteds;
    private int currentAssistedPos = 0;
    private PatientAdapter adapter;
    private SharedPreferences sh;
    private SharedPreferences.Editor editor;

    private TextView tvSipId, tvSipStatus;

    private final Handler handler = new Handler(this);
    public class MSG_TYPE
    {
        public final static int INCOMING_CALL = 1;
        public final static int CALL_STATE = 2;
        public final static int REG_STATE = 3;
        public final static int BUDDY_STATE = 4;
        public final static int CALL_MEDIA_STATE = 5;
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.tag("DashboardActivity");

        setContentView(R.layout.activity_dashboard);

        sh = getSharedPreferences("lifecare_pref",Context.MODE_PRIVATE);
        editor = sh.edit();
        userEntityId = sh.getString("entity_id", "");
        isCaregiver = sh.getBoolean("is_caregiver"+userEntityId,false);
        if(isCaregiver) {
            entityId = sh.getString("assisted_entity_id",userEntityId);
        }else{
            entityId = userEntityId;
        }

        if(entityId.isEmpty()){
            Timber.d("IS EMPTY");
            Intent intent = new Intent(DashboardActivity.this,LoginActivity2.class);
            startActivity(intent);
            finish();
            return;
        }else{
            Timber.d("IS NOOOOT EMPTY");
        }

        Timber.d("isCaregiver = " + isCaregiver());
        Timber.d("UserEntityID = " + userEntityId);
        Timber.d("DisplayEntityID = " + entityId);
        mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

        //mRealm = Realm.getDefaultInstance();
        SyncHandler.retrieveUnsyncedData(this);
        SyncHandler.uploadDataToServer(this);
        EnterpriseHandler.initialize(this);
        ReminderHandler.initialize(this);
        AlarmHandler.initialize(this);

        LocalMeasurementDevicesHandler.initialize(this,entityId);

        PatientData.getInstance().setEntityId(entityId);
        AutoStartAlarm.autoStartAlarm(this);

        mTimelineFragment = TimelineFragment.newInstance();
        mOverviewFragment = OverviewFragment.newInstance();
        mTerumoOverviewFrag = TerumoOverviewFragment.newInstance();

        if(BuildConfig.PRODUCT_FLAVOR == MediCareApplication.MEDISAFE
                || EnterpriseHandler.getCurrentEnterprise()==EnterpriseHandler.TERUMO){
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, mTerumoOverviewFrag)
                    .commit();
        }else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, mOverviewFragment)
                    .commit();
        }

        getSupportFragmentManager().addOnBackStackChangedListener(getListener());
        mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView2 = (NavigationView) findViewById(R.id.nav_view2);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        textOverview = (TextView) findViewById(R.id.text_overview);
        textActivity = (TextView) findViewById(R.id.text_activity);
        imageOverview = (ImageView) findViewById(R.id.image_overview);
        imageActivity = (ImageView) findViewById(R.id.image_activity);
        navOverview = (LinearLayout) findViewById(R.id.nav_bar_overview);
        navActivity = (LinearLayout) findViewById(R.id.nav_bar_activity);
        dimBackground = (RelativeLayout) findViewById(R.id.dim_background);
        navOverviewActLayout = (LinearLayout) findViewById(R.id.overview_activity_nav_bar);

        if(EnterpriseHandler.getCurrentEnterprise()==EnterpriseHandler.TERUMO){
            navOverviewActLayout.setVisibility(View.VISIBLE);
        }else{
            navOverviewActLayout.setVisibility(View.GONE);
        }

        progressBar.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(R.color.progress_bar),
                android.graphics.PorterDuff.Mode.SRC_IN
        );

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawer, null,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
        };

        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView.setNavigationItemSelectedListener(this);
        mNavigationView.setItemIconTintList(null);
        mNavigationView.getMenu().findItem(R.id.nav_overview).setChecked(true);
        TextView tvVersion = (TextView) mNavigationView.findViewById(R.id.text_version);
        tvVersion.setText("ver. " + BuildConfig.VERSION_NAME);

        assisteds = LifecareSharedPreference.getInstance().getLifecareAssisted();
        adapter = new PatientAdapter(this,R.layout.assisted_list_item,assisteds);
        ListView listViewAssisted = (ListView) findViewById(R.id.list_view_inside_nav);
        listViewAssisted.setAdapter(adapter);

        final EditText etName = (EditText) findViewById(R.id.edit_name);
        etName.addTextChangedListener(filterTextWatcher);
        etName.setOnFocusChangeListener(hideKeyboardListener);
        listViewAssisted.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                int position = (int)id;

                if(currentAssistedPos == position) {
                    return;
                }else {
                    currentAssistedPos = position;
                    adapter.selectedPos = position;
                    adapter.notifyDataSetChanged();
                    etName.setText("");
                    etName.clearFocus();
                }

                mTimelineDate = Calendar.getInstance();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mDrawer.closeDrawer(GravityCompat.END);
                    }
                },150);

                entityId = assisteds.get(position).getId();
                PatientData.getInstance().setEntityId(entityId);
                if(getCurrentFragment() instanceof OverviewFragment){
                    ((OverviewFragment)getCurrentFragment()).initData();
                    Timber.d("CURR FRAG IS OVERVIEW");
                }else if(getCurrentFragment() instanceof TimelineFragment){
                    ((TimelineFragment)getCurrentFragment()).clearData();
                    drawerClosed = true;
                    onBackPressed();
                    if(getCurrentFragment() instanceof TerumoOverviewFragment) {
                        ((TerumoOverviewFragment) getCurrentFragment()).clearData();
                    }else if(getCurrentFragment() instanceof OverviewFragment){
                        Timber.d("INIT DATA");
                        ((OverviewFragment) getCurrentFragment()).initData();
                    }
                    Timber.d("BACKPRESS, CURR FRAG IS OVERVIEW, CLEAR DATA");
                }
                syncFromServer(true);
                //new GetLatestVitalsDataTask().execute();
            }
        });

        prevId = R.id.nav_overview;

        // create new user data if it's not been created
        RealmResults<User> users = mRealm.where(User.class).equalTo("entityId",userEntityId).findAll();
        RealmResults<User> allUsers = mRealm.where(User.class).findAll();
        RealmResults<Weight> weights = mRealm.where(Weight.class).equalTo("entityId",userEntityId).findAllSorted("date", Sort.DESCENDING);

        for(int i = 0 ; i < allUsers.size(); i++){
            Timber.d("User " + i + " : " + allUsers.get(i).getEntityId() + ", " + allUsers.get(i).getName());
        }

        if(users.size()>0){
            mUser = users.first();
            PatientData.getInstance().updateUserLatestWeightProfile(mRealm,userEntityId);
        } else {
            mUser = new User();
            mUser.setMale();
            mUser.setAge("18");
            mUser.setHeight("150");
        }

        if(NetworkChangeReceiver.isInternetAvailable(this)) {
            new GetUserDetailsTask().execute();
        }else {
            setupDrawerProfile();
        }

        mTodayDate = Calendar.getInstance();
        mTimelineDate = Calendar.getInstance();

        dimBackground.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                floatingMenu.collapse();
            }
        });
        textOverview.setTypeface(textOverview.getTypeface(), Typeface.BOLD);

        OnClickListener onNavBarClickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

                switch(view.getId()){
                    case R.id.nav_bar_activity:
                        if(!(currentFragment instanceof TimelineFragment)){
                            selectTab(R.id.nav_bar_activity);
                            showTimelineFragment();
                            toggleProgressDialog();
                        }
                        break;

                    case R.id.nav_bar_overview:
                        if(!(currentFragment instanceof TerumoOverviewFragment)&&
                                !(currentFragment instanceof OverviewFragment)){
                            selectTab(R.id.nav_bar_overview);
                            onBackPressed();
                            toggleProgressDialog();
                        }
                        break;
                }
            }
        };
        navActivity.setOnClickListener(onNavBarClickListener);
        navOverview.setOnClickListener(onNavBarClickListener);
        navOverview.setSelected(true);
        textOverview.setSelected(true);
        imageOverview.setSelected(true);

        initFabs();

        //do caregiver only & non-caregiver only stuffs here
        if(!isCaregiver) {
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mNavigationView2);
        }else{
            floatingMenu.setVisibility(View.GONE);
            mNavigationView.getMenu().findItem(R.id.nav_measure).setVisible(false);
            mNavigationView.getMenu().findItem(R.id.nav_reminder).setVisible(false);
        }

        if(!isSyncing) {
            /*if(isCaregiver) {
                new GetLatestVitalsDataTask().execute();
            }else{
                syncFromServer(true);
            }*/
            syncFromServer(true);
        }
    }

    private TextWatcher filterTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            adapter.getFilter().filter(charSequence);
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private OnFocusChangeListener hideKeyboardListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
    };

    private void initFabs(){
        floatingMenu = (FloatingActionsMenu) findViewById(R.id.multiple_actions);
        if(EnterpriseHandler.getCurrentEnterprise()==EnterpriseHandler.GENERAL
                && (BuildConfig.PRODUCT_FLAVOR != MediCareApplication.MEDISAFE)) {
            floatingMenu.setVisibility(View.GONE);
        }
        floatingMenu.setOnFloatingActionsMenuUpdateListener(new OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded() {
                dimBackground.setVisibility(View.VISIBLE);
                mToolbar.setDimmer(true);
                //dim system bar
                /*View decorView = getWindow().getDecorView();
                int uiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE;
                decorView.setSystemUiVisibility(uiOptions);*/
            }

            @Override
            public void onMenuCollapsed() {
                dimBackground.setVisibility(View.INVISIBLE);
                mToolbar.setDimmer(false);
                //undim system bar
                /*View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(0);*/
            }
        });
        measureMenu = (FloatingActionsMenu) findViewById(R.id.measure_actions_menu);
        measureMenu.setOnFloatingActionsMenuUpdateListener(new OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded() {

            }

            @Override
            public void onMenuCollapsed() {
                floatingMenu.setVisibility(View.VISIBLE);
                //floatingMenu.expand();
                measureMenu.setVisibility(View.GONE);
            }
        });

        FloatingActionButton mButtonReading    = ((FloatingActionButton) findViewById(R.id.button_reading));
        FloatingActionButton mButtonPressure   = ((FloatingActionButton) findViewById(R.id.button_bp));
        FloatingActionButton mButtonWeight     = ((FloatingActionButton) findViewById(R.id.button_weight));
        FloatingActionButton mButtonTemp       = ((FloatingActionButton) findViewById(R.id.button_temp));
        FloatingActionButton mButtonMedication = ((FloatingActionButton) findViewById(R.id.button_medication));
        FloatingActionButton mButtonPhoto      = ((FloatingActionButton) findViewById(R.id.button_photo));
        FloatingActionButton mButtonSymptom    = ((FloatingActionButton) findViewById(R.id.button_symptom));
        FloatingActionButton mButtonMeasure    = ((FloatingActionButton) findViewById(R.id.button_measure));
        FloatingActionButton mButtonNotes      = ((FloatingActionButton) findViewById(R.id.button_notes));

        mButtonReading.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DashboardActivity.this, BloodGlucoseReadingActivity.class);
                startActivity(intent);
            }
        });

        mButtonPressure.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DashboardActivity.this, BloodPressureReadingActivity.class);
                startActivity(intent);
            }
        });

        mButtonWeight.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DashboardActivity.this, WeightReadingActivity.class);
                startActivity(intent);
            }
        });

        mButtonTemp.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DashboardActivity.this, TemperatureReadingActivity.class);
                startActivity(intent);
            }
        });

        mButtonMedication.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, MedicationTakenActivity.class);
                startActivityForResult(intent, REQ_MEDIC);
                floatingMenu.collapse();
            }
        });

        mButtonPhoto.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, CameraActivity.class);
                startActivityForResult(intent, REQ_PHOTO);
                floatingMenu.collapse();
            }
        });

        mButtonSymptom.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, SignsAndSymptomsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(intent, REQ_SYMPTOM);
                floatingMenu.collapse();
            }
        });

        mButtonNotes.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DashboardActivity.this, TakeNotesActivity.class);
                startActivityForResult(intent, REQ_NOTES);
                floatingMenu.collapse();
            }
        });

        mButtonMeasure.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(EnterpriseHandler.getCurrentEnterprise() == EnterpriseHandler.GENERAL) {
                    Intent intent = new Intent(DashboardActivity.this, MeasureActivity.class);
                    startActivity(intent);
                }else{
                    Intent intent = new Intent(DashboardActivity.this, BloodGlucoseReadingActivity.class);
                    startActivity(intent);
                }
                floatingMenu.collapse();

               /* floatingMenu.collapse();
                floatingMenu.setVisibility(View.GONE);
                measureMenu.setVisibility(View.VISIBLE);
                measureMenu.expand();*/
            }
        });
    }

    public void collapseFloatingMenu(){
        if(floatingMenu!=null && floatingMenu.isExpanded())
            floatingMenu.collapse();
    }
    public void showFloatingMenu(){
        if(floatingMenu!=null && measureMenu!= null) {
            FloatingActionsMenu menu = floatingMenu.getVisibility() == View.VISIBLE? floatingMenu : measureMenu;
            if(menu.isExpanded()) {
                menu.collapse();
            }
            menu.animate().translationY(0)
                    .setDuration(250)
                    .start();
        }

        if(navOverviewActLayout!=null && navOverviewActLayout.getVisibility()==View.GONE){
            navOverviewActLayout.setVisibility(View.VISIBLE);

            navOverviewActLayout.animate().translationY(0)
                    .setDuration(250)
                    .start();
        }
    }

    public void hideFloatingMenu(){
        if(floatingMenu!=null && measureMenu!=null) {
            FloatingActionsMenu menu = floatingMenu.getVisibility() == View.VISIBLE? floatingMenu : measureMenu;

            /*if(menu.getVisibility()==View.VISIBLE) {
                menu.setVisibility(View.GONE);
               */
            menu.animate().translationY(menu.getHeight() +
                    ((MarginLayoutParams) menu.getLayoutParams()).bottomMargin)
                    .setDuration(500)
                    .start();
            // }

        }

        if(navOverviewActLayout!=null){
            navOverviewActLayout.setVisibility(View.GONE);
            navOverviewActLayout.animate().translationY(navOverviewActLayout.getHeight() +
                    ((MarginLayoutParams) navOverviewActLayout.getLayoutParams()).bottomMargin)
                    .setDuration(500)
                    .start();
        }
    }

    public void setupDrawerProfile(){
        View headerLayout = mNavigationView.getHeaderView(0);
        ImageView drawerProfileImg = (ImageView) headerLayout.findViewById(R.id.nav_header_image);
        TextView drawerProfileName = (TextView) headerLayout.findViewById(R.id.nav_header_name);
        TextView drawerProfileEmail = (TextView) headerLayout.findViewById(R.id.nav_header_email);

        //tvSipStatus.setVisibility(View.GONE);
        //tvSipId.setVisibility(View.GONE);

        if(mUser.isFemale()){
            drawerProfileImg.setImageResource(R.drawable.ic_female_large);
        }
        else{
            drawerProfileImg.setImageResource(R.drawable.ic_male_large);
        }
        if(mUser.getImgUrl()!=null && !mUser.getImgUrl().isEmpty()){
            Timber.d("User profile url = " + mUser.getImgUrl());
            Picasso.with(this).load(mUser.getImgUrl())
                    .transform(new CircleTransform())
                    .placeholder(mUser.isFemale()?R.drawable.ic_female_large:R.drawable.ic_male_large)
                    .into(drawerProfileImg);
        }
        drawerProfileName.setText(mUser.getName());
        drawerProfileEmail.setText(mUser.getEmail());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Timber.d("On activity Result!! " + requestCode + ", result code = " + (resultCode == RESULT_OK));
        if(requestCode == REQ_PHOTO || requestCode == REQ_SYMPTOM
                || requestCode == REQ_MEDIC || requestCode == REQ_NOTES){
            if(resultCode == RESULT_OK){
                Timber.d("ShowTImeline!!");
                navActivity.performClick();
                /*selectTab(R.id.nav_bar_activity);
                showTimelineFragment();*/
            }
        }
    }

    private void toggleProgressDialog(){
        if(progressBar==null)
            return;

        if(isSyncing || isSyncingProfile) {
            progressBar.setVisibility(View.VISIBLE);
        }else{
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    /*
     *  Find last sync date from SharedPref and sync data from server with the date as beginning date.
     *  If last sync date is not found (i.e. user changed device and the device contains no data),
     *  sync from the very beginning (pre set date: 2015/1/1).
     *  Then update the SharedPref with new last sync (which is today).
     */
    public void syncFromServer(boolean onAppLaunched){
        int year = 2016, mth = 1, day = 1;

        if(isCaregiver()){
            year = 2016;
            mth = 8;
            day = 1;
        }

        Calendar startCal = Calendar.getInstance();
        startCal.set(year, mth, day);

        Calendar today = Calendar.getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT,Locale.ENGLISH);
        SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
        String lastSync = sh.getString("last_sync_"+entityId,"NOT_FOUND");
        Timber.d("LAST SYNC " + entityId +" = " + lastSync);

        if(!lastSync.equalsIgnoreCase("NOT_FOUND")) {
            try {
                startCal.setTime(sdf.parse(lastSync));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if(onAppLaunched) {
            if (startCal.get(Calendar.YEAR) != today.get(Calendar.YEAR)
                    || startCal.get(Calendar.MONTH) != today.get(Calendar.MONTH)
                    || (startCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && startCal.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR))
                    ) {
                Timber.d("SYNCED 111 ");
                new SyncFromServerTask(startCal,true).execute();
            }else{
                Timber.d("SYNCED 222 ");

                if(getCurrentFragment() != null){
                    if(getCurrentFragment() instanceof TimelineFragment){
                        drawerClosed = true;
                        onBackPressed();
                    }
                    getCurrentFragment().onResume();
                }
            }
        }else{
            Timber.d("SYNCED 333 ");
            new SyncFromServerTask(startCal,true).execute();
            String newLastSync = sdf.format(today.getTime());
            sh.edit().putString("last_sync_" + entityId, newLastSync).apply();
            Timber.d("NEW LAST SYNC = " + newLastSync);
        }
        sh.edit().apply();
    }

    public String getUserEntityId(){
        return userEntityId;
    }

    public String getAssistedEntityId(){
        return entityId;
    }

    public static boolean isCaregiver(){
        return isCaregiver;
    }

    public void syncSpecificDateFromServer(Calendar cal){
        new SyncFromServerTask(cal,false).execute();
    }

    private class GetUserDetailsTask extends AsyncTask<Void, Void, Void>{
        String name = "", firstName = "", lastName = "";
        String age = "", gender = "", height = "", email = "", imgUrl = "";
        String sipId, sipPort, sipHost, sipPass, sipUsername;

        @Override
        protected void onPreExecute(){
            isSyncingProfile = true;
            toggleProgressDialog();
            Timber.d("GetUserDetailsTask");
        }
        @Override
        protected Void doInBackground(Void...voids){

            try {
                JSONArray arr = LifeCareHandler.getInstance().getCurrentCaregiverProfile(userEntityId);

                if (arr != null) {
                    Timber.e("Response from getting profile : " + arr.toString());
                    if (arr.length() > 0) {
                        JSONObject json = arr.getJSONObject(0);
                        Timber.e("Response from getting profile22");
                        if (json.has("name")) {
                            if (json.getString("name") != null)
                                name = json.getString("name");
                            Timber.e("Response from getting profile23");
                        }

                        if (json.has("first_name")) {
                            if (json.getString("first_name") != null)
                                firstName = json.getString("first_name");
                        }

                        if (json.has("last_name")) {
                            if (json.getString("last_name") != null)
                                lastName = json.getString("last_name");
                        }

                        if (json.has("type")) {
                            if (json.getString("type") != null)
                                gender = json.getString("type");
                        }

                        if (json.has("height")) {
                            if (json.getString("height") != null)
                                height = json.getString("height");
                        }

                        if (json.has("authentication_string_lower")) {
                            Timber.e("Response from getting profile24");
                            if (json.getString("authentication_string_lower") != null) {
                                email = json.getString("authentication_string_lower");
                                Timber.d("EMAIL = " + email);
                                Timber.e("Response from getting profile25");
                            }
                        }

                        if (json.has("medias")) {
                            JSONArray mediasArr = json.getJSONArray("medias");
                            if (mediasArr.length() > 0) {
                                JSONObject medias = (JSONObject) mediasArr.get(0);
                                if (medias.has("img_url3")) {
                                    imgUrl = medias.getString("img_url3");
                                }
                            }
                        }


                        if (json.has("date_established")) {
                            if (json.getString("date_established") != null) {
                                String dateOfBirth = json.getString("date_established");
                                if (!dateOfBirth.isEmpty() && !dateOfBirth.equalsIgnoreCase(
                                        "null")) {
                                    String yearOfBirth = dateOfBirth.substring(0, 4);
                                    Timber.d("YEAR OF BIRTH = " + yearOfBirth);
                                    int year = Integer.valueOf(yearOfBirth);
                                    age = String.valueOf(
                                            Calendar.getInstance().get(Calendar.YEAR) - year);
                                }
                            }
                        }

                    }
                }
            }catch(JSONException e){
                Timber.e(e.getMessage());
            }catch(Exception e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void results){
            //mRealm = Realm.getDefaultInstance();

            isSyncingProfile = false;
            toggleProgressDialog();

            if(mUser.getEntityId()==null
                    || mUser.getEntityId().equalsIgnoreCase("")) {
                setProfile();
                mUser.setEntityId(userEntityId);
                PatientData.getInstance().addUser(mRealm,mUser);
            }
            else {
                mRealm.beginTransaction();
                setProfile();
                mRealm.commitTransaction();
            }
            mUser = mRealm.where(User.class).equalTo("entityId",userEntityId).findFirst();
            PatientData.getInstance().updateUserLatestWeightProfile(mRealm,userEntityId);

            setupDrawerProfile();
        }

        private void setProfile(){
            if (!name.equalsIgnoreCase("")) {
                mUser.setName(name);
            }

            if (!firstName.equalsIgnoreCase("")) {
                mUser.setFirstName(firstName);
            }

            if (!lastName.equalsIgnoreCase("")) {
                mUser.setLastName(lastName);
            }

            if (!age.equalsIgnoreCase("")){
                mUser.setAge(age);
            }

            if (!height.equalsIgnoreCase("")
                    && !height.equalsIgnoreCase("null")) {
                mUser.setHeight(height);
            }

            if (!gender.equalsIgnoreCase("")
                    && !gender.equalsIgnoreCase("null")) {
                if(gender.equalsIgnoreCase("F"))
                    mUser.setFemale();
                else
                    mUser.setMale();
            }

            if (!email.equalsIgnoreCase("")
                    && !email.equalsIgnoreCase("null")) {
                mUser.setEmail(email);
            }

            if (!imgUrl.equalsIgnoreCase("")
                    && !imgUrl.equalsIgnoreCase("null")) {
                mUser.setImgUrl(imgUrl);
            }
        }
    }

    private class GetLatestVitalsDataTask extends AsyncTask<Void, Void, Void>{
        boolean isSyncNeeded = false;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isSyncing = true;
            SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT,Locale.ENGLISH);
            SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
            String lastSync = sh.getString("last_sync_"+entityId,"NOT_FOUND");

            Timber.d("LAST SYNC = " + lastSync);

            Calendar startCal = Calendar.getInstance();
            Calendar today = Calendar.getInstance();

            if(!lastSync.equalsIgnoreCase("NOT_FOUND")){
                try {
                    startCal.setTime(sdf.parse(lastSync));
                } catch (ParseException e) {
                    e.printStackTrace();
                    startCal.set(2016,1,1);
                }
            }
            else{
                startCal.set(2016,1,1);
            }

            if (startCal.get(Calendar.YEAR) != today.get(Calendar.YEAR)
                    || startCal.get(Calendar.MONTH) != today.get(Calendar.MONTH)
                    || (startCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && startCal.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR))
                    )
            {
                isSyncNeeded = true;
                String newLastSync = sdf.format(today.getTime());
                sh.edit().putString("last_sync_" + entityId, newLastSync).apply();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {

            if(!isSyncNeeded)
                return null;
            //mRealm = Realm.getDefaultInstance();

            Timber.d("GetLatestVitalsDataTask Sync doinbg");

            JSONArray glucoseResult = LifeCareHandler.getInstance().getGlucoseReading(entityId, 1);

            JSONArray bloodPressureResult = LifeCareHandler.getInstance().getBloodPressureReading(entityId, 1);

            JSONArray weightResult = LifeCareHandler.getInstance().getWeighingScaleReading(entityId, 1);

            JSONArray tempResult = LifeCareHandler.getInstance().getBodyTemperatureReading(entityId, 1);

            JSONArray spo2Result = LifeCareHandler.getInstance().getSPO2Reading(entityId, 1);

            Realm realm = null;
            try {
                realm = Realm.getDefaultInstance();
                Timber.d("Created realm in bg thread");
                saveGlucoseData(glucoseResult,realm);
                saveBloodPressureData(bloodPressureResult,realm);
                saveWeightData(weightResult,realm);
                saveTempData(tempResult,realm);
                saveSpo2Data(spo2Result,realm);
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if(realm!=null) {
                    realm.close();
                    Timber.d("Closed realm in bg thread");
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            isSyncing = false;
            toggleProgressDialog();
            //mRealm = Realm.getDefaultInstance();

            if(getCurrentFragment()!=null){
                getCurrentFragment().onResume();
            }
        }
    }

    /*
     *  Find last sync date from SharedPref and sync data from server with the date as beginning date.
     *  If last sync date is not found (i.e. user changed device and the device contains no data),
     *  sync from the very beginning (pre set date: 2015/1/1).
     *  Then update the SharedPref with new last sync (which is today).
     */
    private class SyncFromServerTask extends AsyncTask<Void, Void, Void>{
        String startDate;
        String endDate;
        Calendar startCal, endCal;
        boolean isSyncTillToday;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Realm realm;
        int realmCount = 0;

        public SyncFromServerTask(Calendar cal, boolean isSyncTillToday){
            this.isSyncTillToday = isSyncTillToday;
            if(isSyncTillToday) {
                startCal = (Calendar)cal.clone();
                endCal = Calendar.getInstance();
            }else{
                startCal = (Calendar)cal.clone();
                startCal.set(Calendar.HOUR_OF_DAY,00);
                startCal.set(Calendar.MINUTE,00);

                endCal = (Calendar)cal.clone();
                endCal.set(Calendar.HOUR_OF_DAY,23);
                endCal.set(Calendar.MINUTE,59);
            }

            startDate = sdf.format(cal.getTime());
            endDate = sdf.format(endCal.getTime()); //current time
            Timber.d("StartDate = " + startDate + ", EndDate = " + endDate);
        }

        @Override
        protected void onPreExecute(){
            if(isSyncTillToday) {
                isSyncing = true;
                toggleProgressDialog();
            }

            if(getCurrentFragment() instanceof OverviewFragment) {
                ((OverviewFragment) getCurrentFragment()).initData();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {


            //mRealm = Realm.getDefaultInstance();

            Timber.d("Sync doinbg");
            Calendar tempStart = (Calendar)startCal.clone();
            Calendar tempEnd = (Calendar)startCal.clone();

            Timber.d("Sync doinbg tempstart = " + tempStart.get(Calendar.HOUR_OF_DAY));
            Timber.d("Sync doinbg tempend = " + tempEnd.get(Calendar.HOUR_OF_DAY));
            Timber.d("Sync doinbg endCal = " + endCal.get(Calendar.HOUR_OF_DAY));
            while(tempStart.before(endCal) && tempEnd.before(endCal)){

                Timber.d("Sync doinbg2");
                tempEnd.add(Calendar.MONTH,1);

                String tempStartDate = sdf.format(tempStart.getTime());
                String tempEndDate;

                if(tempEnd.before(endCal))
                    tempEndDate = sdf.format(tempEnd.getTime());
                else { //tempEnd is after TODAY
                    Calendar end = Calendar.getInstance();
                    tempEndDate = sdf.format(end.getTime());
                }

                Timber.d("Temp Start Date = " + tempStartDate);
                Timber.d("Temp End Date = " + tempEndDate);

                JSONArray glucoseResult = LifeCareHandler.getInstance()
                        .getGlucoseReadingByDateRange(entityId, tempStartDate, tempEndDate);

                JSONArray foodResult = LifeCareHandler.getInstance()
                        .getFoodByDateRange(entityId, tempStartDate, tempEndDate);

                JSONArray medicationResult = LifeCareHandler.getInstance()
                        .getMedicationByDateRange(entityId, tempStartDate, tempEndDate);

                JSONArray symptomsResult = LifeCareHandler.getInstance()
                        .getSymptomsByDateRange(entityId, tempStartDate, tempEndDate);

                JSONArray bloodPressureResult = LifeCareHandler.getInstance()
                        .getBloodPressureReadingByDateRange(entityId, tempStartDate, tempEndDate);

                JSONArray weightResult = LifeCareHandler.getInstance()
                        .getWeighingScaleReadingByDateRange(entityId, tempStartDate, tempEndDate);

                JSONArray tempResult = LifeCareHandler.getInstance()
                        .getBodyTemperatureReadingByDateRange(entityId, tempStartDate, tempEndDate);

                JSONArray spo2Result = LifeCareHandler.getInstance()
                        .getSpo2ReadingByDateRange(entityId, tempStartDate, tempEndDate);

                tempStart.add(Calendar.MONTH,1);

                try {
                    realm = Realm.getDefaultInstance();
                    Timber.d("Created realm in bg " + realmCount);
                    saveGlucoseData(glucoseResult,realm);
                    saveFoodData(foodResult,realm);
                    saveMedicationData(medicationResult,realm);
                    saveSymptomsData(symptomsResult,realm);
                    saveBloodPressureData(bloodPressureResult,realm);
                    saveWeightData(weightResult,realm);
                    saveTempData(tempResult,realm);
                    saveSpo2Data(spo2Result,realm);
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    if(realm!=null){
                        realm.close();
                        Timber.d("Closed realm in bg " + realmCount);
                        realmCount++;
                    }
                }
            }

            //TODO: reminder
            // JSONArray result3 = LifeCareHandler.getInstance()
            //    .getBloodPressureReading(entityId, 1);

            return null;

        }

        @Override
        protected void onPostExecute(Void results){
            isSyncing = false;
            toggleProgressDialog();
            //mTimelineFragment.getPatientData(mTimelineDate);
            //mRealm = Realm.getDefaultInstance();

            //update new last sync date
            SimpleDateFormat syncFormat = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT,Locale.ENGLISH);
            String newLastSync = syncFormat.format(Calendar.getInstance().getTime());
            sh.edit().putString("last_sync_" + entityId, newLastSync).apply();
            Timber.d("NEW LAST SYNC " + entityId + " = " + newLastSync);

            if(DashboardActivity.this.isFinishing()){
                if(mRealm!=null){
                    mRealm.close();
                    mRealm = null;
                }

                removeAssistedRealmData();
            }else {

                if (getCurrentFragment() instanceof TimelineFragment) {
                    //refresh timeline
                    TimelineFragment frag = (TimelineFragment) getCurrentFragment();
                    frag.setRefreshing(false);
                    frag.getPatientData(frag.getCurrentDate());
                }/*else if(getCurrentFragment() instanceof OverviewFragment){
                    mOverviewFragment.onResume();
                }else if(getCurrentFragment() instanceof TerumoOverviewFragment){
                    mTerumoOverviewFrag.onResume();
                }*/
                else {
                    getCurrentFragment().onResume();
                }
            }
        }
    }

    public void selectTab(int id){
        switch(id){
            case R.id.nav_bar_activity:
                navActivity.setSelected(true);
                navOverview.setSelected(false);
                imageActivity.setSelected(true);
                imageOverview.setSelected(false);
                textActivity.setSelected(true);
                textOverview.setSelected(false);
                textActivity.setTypeface(textActivity.getTypeface(), Typeface.BOLD);
                textOverview.setTypeface(null, Typeface.NORMAL);
                break;

            case R.id.nav_bar_overview:
                navActivity.setSelected(false);
                navOverview.setSelected(true);
                imageActivity.setSelected(false);
                imageOverview.setSelected(true);
                textActivity.setSelected(false);
                textActivity.setTypeface(null, Typeface.NORMAL);
                textOverview.setSelected(true);
                textOverview.setTypeface(textActivity.getTypeface(), Typeface.BOLD);
        }
    }


    private void saveGlucoseData(JSONArray array, Realm realm) throws JSONException {
        if(array==null)
            return;

        for(int n = 0; n < array.length(); n++)
        {
            String eventId = "", concentration = "", unit = "", date = "", remarks = "";

            JSONObject data = array.getJSONObject(n);

            if(data.has("_id")){
                eventId = data.getString("_id");
            }

            if(data.has("concentration")){
                concentration = data.getString("concentration");
            }

            if(data.has("concentrationUnit")){
                unit = data.getString("concentrationUnit");
            }

            if(data.has("create_date")){
                date = data.getString("create_date");
            }

            if(data.has("remarks")){
                remarks = data.getString("remarks");
            }
            if(!concentration.equals("")&&!concentration.equals("null")){
                final Terumo terumo = new Terumo();
                if(!concentration.isEmpty()) {
                    terumo.setValue(Double.parseDouble(concentration));
                }
                terumo.setRemark(remarks);
                terumo.setDate(date);
                terumo.setStringUnit(unit);
                terumo.setBeforeMeal(true);
                terumo.setEntityId(entityId);
                terumo.setEventId(eventId);
                Timber.d("EventId = " + eventId);
                PatientData.getInstance().addTerumo(realm,terumo);
            }
        }
    }

    private void saveFoodData(JSONArray array, Realm realm) throws JSONException {
        if(array==null)
            return;

        for(int n = 0; n < array.length(); n++)
        {
            double carbo = -1;
            String eventId = "", remarks = "", imgUrl = "", date = "";

            JSONObject data = array.getJSONObject(n);

            if(data.has("_id")){
                eventId = data.getString("_id");
            }
            if(data.has("carbohydrates")){
                String carboStr = data.getString("carbohydrates");
                if(!carboStr.isEmpty()) {
                    carbo = Double.parseDouble(carboStr);
                }
            }
            if(data.has("remarks")){
                remarks = data.getString("remarks");
            }
            if(data.has("create_date")){
                date = data.getString("create_date");
            }
            if(data.has("medias")){
                JSONArray mediasArr = data.getJSONArray("medias");
                if(mediasArr.length()>0) {
                    JSONObject medias = (JSONObject) mediasArr.get(0);
                    if (medias.has("img_url")) {
                        imgUrl = medias.getString("img_url");
                    }
                }
            }

            Timber.d("Save Photo - In Progress: " + carbo + ", " + remarks +", " + date.toString());
            if(!imgUrl.equals("")&&!imgUrl.equals("null")){
                final Photo photo = new Photo();
                if(carbo != -1) {
                    photo.setValue(carbo);
                }
                photo.setImage(imgUrl);
                photo.setRemark(remarks);
                photo.setDate(date);
                photo.setEventId(eventId);
                photo.setEntityId(entityId);
                PatientData.getInstance().addPhoto(realm,photo);
                Timber.d("Added Photo - In Progress");
            }
            else{
                Timber.d("Added Photo - In Progress - Fail (IMG URL='')");
            }
        }
    }

    private void saveMedicationData(JSONArray array, Realm realm) throws JSONException {
        if(array==null)
            return;

        Timber.d("MEDIC - array size " + array.length());

        for(int n = 0; n < array.length(); n++)
        {
            String eventId = "", type = "", unit = "", dosage = "", remarks = "", date = "";

            JSONObject data = array.getJSONObject(n);

            if(data.has("_id")){
                eventId = data.getString("_id");
            }
            if(data.has("medication_type")){
                type = data.getString("medication_type");
            }
            if(data.has("unit")){
                unit = data.getString("unit");
            }
            if(data.has("dosage")){
                dosage = data.getString("dosage");
            }
            if(data.has("remarks")){
                remarks = data.getString("remarks");
            }
            if(data.has("create_date")){
                date = data.getString("create_date");
            }

            if(!type.equals("")&&!type.equals("null")){
                Timber.d("MEDIC - new");

                final Medication medication = new Medication();
                medication.setStringType(type);
                if(!dosage.isEmpty()) {
                    medication.setDosage(Double.parseDouble(dosage));
                }
                medication.setRemark(remarks);
                medication.setDate(date);
                medication.setUnit(0);
                medication.setEntityId(entityId);
                medication.setEventId(eventId);
                Timber.d("Event id medic = " + eventId);
                PatientData.getInstance().addMedication(realm,medication);
            }
        }
    }

    private void saveBloodPressureData(JSONArray array, Realm realm) throws JSONException {
        if(array==null)
            return;

        Timber.d("BP - array size " + array.length());

        for(int n = 0; n < array.length(); n++)
        {
            String eventId = "", systolic = "", diastolic = "", unit = "", pulseRate = "", date = "";

            JSONObject data = array.getJSONObject(n);

            if(data.has("_id")){
                eventId = data.getString("_id");
            }
            if(data.has("systolic")){
                systolic = data.getString("systolic");
            }
            if(data.has("unit")){
                unit = data.getString("unit");
            }
            if(data.has("diastolic")){
                diastolic = data.getString("diastolic");
            }
            if(data.has("heart_rate")){
                pulseRate = data.getString("heart_rate");
            }
            if(data.has("create_date")){
                date = data.getString("create_date");
            }

            if(!systolic.equals("")&&!systolic.equals("null")){
                Timber.d("BP - new");

                final BloodPressure bp = new BloodPressure();
                bp.setSystolic(Float.parseFloat(systolic));
                bp.setDistolic(Float.parseFloat(diastolic));
                bp.setPulseRate(Float.parseFloat(pulseRate));
                bp.setDate(date);
                bp.setUnit(0);
                bp.setEntityId(entityId);
                bp.setEventId(eventId);
                PatientData.getInstance().addBloodPressure(realm,bp);
                Timber.d("Event id bp = " + eventId);
                Timber.d("NEW BP - entityId =  " + entityId);
            }
        }
    }

    private void saveWeightData(JSONArray array, Realm realm) throws JSONException {
        if(array==null)
            return;

        Timber.d("Weight - array size " + array.length());

        for(int n = 0; n < array.length(); n++)
        {
            String eventId = "", weightData = "", unit = "", date = "";

            JSONObject data = array.getJSONObject(n);
            if(data.has("_id")){
                eventId = data.getString("_id");
            }
            if(data.has("weight")){
                weightData = data.getString("weight");
            }
            if(data.has("weight_reading_type")){
                unit = data.getString("weight_reading_type");
            }
            if(data.has("create_date")){
                date = data.getString("create_date");
            }

            if(!weightData.equals("")&&!weightData.equals("null")){
                Timber.d("WEIGHT - new");

                final Weight weight = new Weight();
                if(!weightData.isEmpty()) {
                    weight.setWeight(Double.parseDouble(weightData));
                }
                weight.setUnit(unit.equalsIgnoreCase("kg")?0:1);
                weight.setDate(date);
                weight.setEntityId(entityId);
                weight.setEventId(eventId);

                Timber.d("Event id weight = " + eventId);
                PatientData.getInstance().addWeight(realm,weight);
            }
        }
    }
    private void saveTempData(JSONArray array, Realm realm) throws JSONException {
        if(array==null)
            return;

        Timber.d("Temp - array size " + array.length());

        for(int n = 0; n < array.length(); n++)
        {
            String eventId = "", tempData = "", unit = "", date = "";

            JSONObject data = array.getJSONObject(n);

            if(data.has("_id")){
                eventId = data.getString("_id");
            }
            if(data.has("temperature")){
                tempData = data.getString("temperature");
            }
            if(data.has("unit")){
                unit = data.getString("unit");
            }
            if(data.has("create_date")){
                date = data.getString("create_date");
            }

            if(!tempData.equals("")&&!tempData.equals("null")){
                Timber.d("Temperature - new");

                final Temperature temp = new Temperature();
                if(!tempData.isEmpty()) {
                    temp.setValue(Double.parseDouble(tempData));
                }
                temp.setUnit(unit.contains("C") ? 0 : 1);
                temp.setDate(date);
                temp.setEntityId(entityId);
                temp.setEventId(eventId);
                PatientData.getInstance().addTemperature(realm,temp);
            }
        }
    }
    //TODO
    private void saveSpo2Data(JSONArray array, Realm realm) throws JSONException {
        if(array==null)
            return;

        Timber.d("SpO2 - array size " + array.length());

        for(int n = 0; n < array.length(); n++)
        {
            String eventId = "", pulseRate = "", spo2Data = "", unit = "", date = "";

            JSONObject data = array.getJSONObject(n);

            if(data.has("_id")){
                eventId = data.getString("_id");
            }
            if(data.has("pulse")){
                pulseRate = data.getString("pulse");
            }
            if(data.has("oxygen")){
                spo2Data = data.getString("oxygen");
            }
            if(data.has("create_date")){
                date = data.getString("create_date");
            }

            if(!spo2Data.equals("")&&!spo2Data.equals("null")){
                Timber.d("SPO2 - new: spo2val - " + spo2Data + ", pulse - " + pulseRate);

                final SpO2 spo2 = new SpO2();
                if(!spo2Data.isEmpty()) {
                    spo2.setValue(Double.parseDouble(spo2Data));
                }
                if(!pulseRate.isEmpty()) {
                    spo2.setPulseRate(Double.parseDouble(pulseRate));
                }
                spo2.setDate(date);
                spo2.setEntityId(entityId);
                PatientData.getInstance().addSpO2(realm,spo2);

                final SpO2Set spo2set = new SpO2Set(spo2.getDate(),spo2.getDate());
                spo2set.setEntityId(entityId);
                spo2set.setEventId(eventId);
                PatientData.getInstance().addSpO2Set(realm,spo2set);
            }
        }
    }

    private void saveSymptomsData(JSONArray array, Realm realm) throws JSONException {
        if(array==null) {
            return;
        }

        for(int n = 0; n < array.length(); n++)
        {
            String eventId = "", types = "", remarks = "", date = "";

            JSONObject data = array.getJSONObject(n);

            if(data.has("_id")){
                eventId = data.getString("_id");
            }
            if(data.has("type")){
                Timber.d("SYMPTOMS - has types");

                types = data.getString("type");
            }
            if(data.has("remarks")){
                remarks = data.getString("remarks");
            }
            if(data.has("create_date")){
                date = data.getString("create_date");
            }

            if(!types.equals("")&&!types.equals("null")){
                Timber.d("SYMPTOM - new");

                final Symptom symptom = new Symptom();
                symptom.setSymptoms(types);
                symptom.setRemark(remarks);
                symptom.setDate(date);
                symptom.setEntityId(entityId);
                symptom.setEventId(eventId);
                PatientData.getInstance().addSymptoms(realm,symptom);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //checkDrawOverlayPermission();
        Timber.d("Dashboard onResume");
        //if(mRealm==null)
        //  mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

        if(progressBar!=null){
            toggleProgressDialog();
        }
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if(currentFragment instanceof OverviewFragment)
        {
            // mOverviewFragment.onResume();
        }
        else if(currentFragment instanceof TimelineFragment)
        {
            mTimelineFragment.setListener(this);
            mTimelineFragment.setDate(mTimelineDate, isToday());
            mTimelineFragment.getPatientData(mTimelineDate);
            mTimelineFragment.updateReminder();
            initDrawerPos(R.id.nav_overview);
        }
        else if(currentFragment instanceof ReminderFragment)
        {
            initDrawerPos(R.id.nav_reminder);
        }
        else if(currentFragment instanceof PrivacyFragment)
        {
            initDrawerPos(R.id.nav_privacy);
        }
        else if(currentFragment instanceof ProfileFragment)
        {
            initDrawerPos(R.id.nav_setting);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        SyncHandler.unregisterNetworkBroadcast();
        SyncHandler.storeUnsyncedData(this);

        if(mTimelineFragment!=null)
            mTimelineFragment.setListener(null);
    }

    @Override
    public void onDestroy() {

        if (mRealm != null) {
            mRealm.close();
            mRealm = null;
            Timber.d("Realm closed!");
        }

        removeAssistedRealmData();
        super.onDestroy();
    }

    //TODO: caregiver problem
    private void removeAssistedRealmData(){
        if(isCaregiver()) {
            try {
                Timber.d("Removed realm database1");
                editor.clear().commit();
                Timber.d("Removed realm database2");
                Realm.deleteRealm(PatientData.getInstance().getRealmConfig());
                Timber.d("Removed realm database3");
            }catch(Exception e){
                Timber.e("Failed to remove realm database:");
                e.printStackTrace();
            }
            Timber.d("CLEARING SHARED PREF1");
            Timber.d("CLEARING SHARED PREF2");
            //editor.apply();
            Timber.d("CLEARED SHARED PREF3");
        }
    }
    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void selectDrawerItem(int id){
        onNavigationItemSelected(mNavigationView.getMenu().findItem(id));
    }

    @Override
    public boolean onNavigationItemSelected(final MenuItem item) {
        Timber.d("onNavigationItemSelected");

        if(prevId == item.getItemId())
        {
            mDrawer.closeDrawers();
            return false;
        }

        mNavigationView.getMenu().findItem(prevId).setChecked(false);
        mNavigationView.getMenu().findItem(item.getItemId()).setChecked(true);
        prevId = item.getItemId();
        mDrawer.closeDrawers();

        makingSelection = true;
        //delay to avoid close drawer animation lag
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                switch(item.getItemId())
                {
                    case R.id.nav_overview:
                        onBackPressed();
                        break;

                    case R.id.nav_setting:
                        if (mUser == null) {
                            Timber.w("no user");
                            break;
                        }
                        showProfileFragment();
                        hideFloatingMenu();
                        break;

                    case R.id.nav_measure:
                        initDrawerPos();
                        showTakeMeasurementActivity();
                        break;

                    case R.id.nav_reminder:
                        showReminderFragment();
                        hideFloatingMenu();
                        break;

                    case R.id.nav_privacy:
                        showPrivacyFragment();
                        hideFloatingMenu();
                        break;

                    case R.id.nav_logout:
                        initDrawerPos();
                        promptLogoutDialog();
                        break;
                }
            }
        },250);


        return false;
    }

    private void showTakeMeasurementActivity(){
        Intent intent1 = new Intent(DashboardActivity.this, MeasurementDeviceListActivity.class);
        startActivity(intent1);
    }

    private void promptLogoutDialog(){
        new AlertDialog.Builder(DashboardActivity.this,R.style.dialog)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_confirmation)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.logout_title, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        Logout task = new Logout();
                        task.execute();
                    }
                })
                .show();
    }

    public void showTimelineFragment(){
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        getSupportFragmentManager().popBackStack();
        if(mTimelineFragment==null)
            mTimelineFragment = TimelineFragment.newInstance();
        ft.replace(R.id.fragment_container, mTimelineFragment);
        ft.addToBackStack("timeline");
        ft.commit();

        mTimelineDate = Calendar.getInstance();
    }

    private void showProfileFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        getSupportFragmentManager().popBackStack();
        Fragment fragment = ProfileFragment.newInstance();
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    private Fragment getCurrentFragment(){
        return getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    private void showReminderFragment(){
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        getSupportFragmentManager().popBackStack();
        Fragment fragment = new ReminderFragment();
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    private void showPrivacyFragment(){
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        getSupportFragmentManager().popBackStack();
        Fragment fragment = new PrivacyFragment();
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    public void setToolbar(int titleRes, int leftRes) {
        mToolbar.setTitle(titleRes);
        mToolbar.setLeftButtonImage(leftRes);
        mToolbar.hideRightButton();
        mToolbar.hideSecondRightButton();
    }

    public void setToolbar(int titleRes, int leftRes, int rightRes) {
        mToolbar.setTitle(titleRes);
        mToolbar.setLeftButtonImage(leftRes);
        mToolbar.setRightButtonImage(rightRes);
        mToolbar.hideSecondRightButton();
    }

    public void setToolbar(int titleRes, int leftRes, int rightRes, int rightRes2) {
        mToolbar.setTitle(titleRes);
        mToolbar.setLeftButtonImage(leftRes);
        mToolbar.setRightButtonImage(rightRes);
        mToolbar.setSecondRightButtonImage(rightRes2);
    }

    public void setToolbarListener(CustomToolbar.OnToolbarClickListener listener) {
        mToolbar.setListener(listener);
    }

    public void setToolbarTitle(int resId) {
        mToolbar.setTitle(resId);
        //centerToolbarTitle(mToolbar);
    }

    private OnBackStackChangedListener getListener()
    {
        OnBackStackChangedListener result = new OnBackStackChangedListener()
        {
            public void onBackStackChanged()
            {
                FragmentManager manager = getSupportFragmentManager();

                if (manager != null)
                {
                    Fragment currFrag = (Fragment) manager.findFragmentById(R.id.fragment_container);

                    if(BuildConfig.PRODUCT_FLAVOR == MediCareApplication.MEDISAFE) {
                        if (currFrag instanceof TimelineFragment
                                || currFrag instanceof OverviewFragment
                                || currFrag instanceof TerumoOverviewFragment) {
                            showFloatingMenu();
                        } else {
                            hideFloatingMenu();
                        }
                    }
                    if(BuildConfig.PRODUCT_FLAVOR == MediCareApplication.GENERAL
                            && EnterpriseHandler.getCurrentEnterprise() == EnterpriseHandler.TERUMO) {
                        if (currFrag instanceof TerumoOverviewFragment ||
                                currFrag instanceof TimelineFragment) {
                            showFloatingMenu();
                        } else {
                            hideFloatingMenu();
                        }
                    }
                    if(currFrag instanceof ReminderFragment){
                        Timber.d("REMINDER");
                        mNavigationView.getMenu().findItem(prevId).setChecked(false);
                        mNavigationView.getMenu().findItem(R.id.nav_reminder).setChecked(true);
                        prevId = R.id.nav_reminder;
                    } else if(currFrag instanceof PrivacyFragment){
                        Timber.d("PRIVACY");
                        mNavigationView.getMenu().findItem(prevId).setChecked(false);
                        mNavigationView.getMenu().findItem(R.id.nav_privacy).setChecked(true);
                        prevId = R.id.nav_privacy;
                    }
                }
            }
        };

        return result;
    }

    @Override
    public void onNextClick() {
        Timber.d("onNextClick");
        if (!isToday() && !mIsRequestRunning) {
            mTimelineDate.add(Calendar.DATE, 1);
            mTimelineFragment.setDate(mTimelineDate, isToday());
            mTimelineFragment.getPatientData(mTimelineDate);
//            if(!isCaregiver)
//                mTimelineFragment.getPatientData(mTimelineDate);
//            else
//                syncSpecificDateFromServer(mTimelineDate);
            mTimelineFragment.scrollToTop();
        }
    }

    public void scrollToTop(){
        if(getCurrentFragment()!=null) {
            if (getCurrentFragment() instanceof OverviewFragment) {
                ((OverviewFragment) getCurrentFragment()).scrollToTop();
            } else if (getCurrentFragment() instanceof TimelineFragment) {
                ((TimelineFragment) getCurrentFragment()).scrollToTop();
            }
        }
    }

    @Override
    public void onPreviousClick() {
        Timber.d("onPreviousClick");
        if (!mIsRequestRunning) {
            mTimelineDate.add(Calendar.DATE, -1);
            mTimelineFragment.setDate(mTimelineDate, false);
            mTimelineFragment.getPatientData(mTimelineDate);
//            if(!isCaregiver)
//                mTimelineFragment.getPatientData(mTimelineDate);
//            else
//                syncSpecificDateFromServer(mTimelineDate);
            mTimelineFragment.scrollToTop();
        }
    }

    public boolean isToday() {
        if ((mTimelineDate.get(Calendar.DAY_OF_MONTH) != mTodayDate.get(Calendar.DAY_OF_MONTH))
                || (mTimelineDate.get(Calendar.MONTH) != mTodayDate.get(Calendar.MONTH))
                || (mTimelineDate.get(Calendar.YEAR) != mTodayDate.get(Calendar.YEAR))) {
            return false;
        }

        return true;
    }

    public Calendar getTimelineDate() {
        return mTimelineDate;
    }

    public Realm getRealm() {
        return mRealm;
    }

    public User getUser() {
        if (mUser != null) {
            return  mRealm.copyFromRealm(mUser);
        }
        return null;
    }

    public void openLeftDrawer() {
        mDrawer.openDrawer(GravityCompat.START);
    }

    public void openRightDrawer() {
        mDrawer.openDrawer(GravityCompat.END);
    }

    @Override public void onRequestStart() {
        mIsRequestRunning = true;
    }

    @Override public void onRequestFinish() {
        mIsRequestRunning = false;
    }

    public void onCalendarClick(){
        Timber.d("OnCalendarClick");
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
                .setPreselectedDate(mTimelineDate.get(Calendar.YEAR),mTimelineDate.get(Calendar.MONTH),mTimelineDate.get(Calendar.DAY_OF_MONTH))
                .setOnDateSetListener(DashboardActivity.this);
        cdp.show(getSupportFragmentManager(), "fragment_date_picker_name");
    }

    public void onDateSet(CalendarDatePickerDialogFragment dialog, int year, int monthOfYear, int dayOfMonth) {
        mTimelineDate.set(year,monthOfYear,dayOfMonth);

        if (!mIsRequestRunning) {
            mTimelineFragment.setDate(mTimelineDate, isToday());
            mTimelineFragment.getPatientData(mTimelineDate);
            //if(!isCaregiver)
            //  mTimelineFragment.getPatientData(mTimelineDate);
            //else
            //  syncSpecificDateFromServer(mTimelineDate);
            Log.d("Check", "Calendar" + mTimelineDate);
        }
    }

    public void onBackPressed(){
        if(floatingMenu.isExpanded()){
            Timber.d("FLOATING MENU IS EXPANDED");
            floatingMenu.collapse();
        }else if(mDrawer.isDrawerOpen(GravityCompat.START)&&!makingSelection){
            mDrawer.closeDrawer(GravityCompat.START);
        }else if(mDrawer.isDrawerOpen(GravityCompat.END)&&!drawerClosed){
            Timber.d("Drawer is open");
            mDrawer.closeDrawer(GravityCompat.END);
        }else {
            Timber.d("SUPER ON BACK PRESS");
            initDrawerPos();
            super.onBackPressed();
            scrollToTop();
        }

        if(drawerClosed){
            drawerClosed=false;
        }

        if(makingSelection){
            makingSelection=false;
        }
    }

    private void initDrawerPos(){

        if(getCurrentFragment() instanceof ReminderFragment
                || getCurrentFragment() instanceof ProfileFragment
                || getCurrentFragment() instanceof PrivacyFragment
                ){
            navOverview.setSelected(true);
            navActivity.setSelected(false);
            selectTab(R.id.nav_bar_overview);
        }

        mNavigationView.getMenu().findItem(prevId).setChecked(false);
        mNavigationView.getMenu().findItem(R.id.nav_overview).setChecked(true);
        prevId = R.id.nav_overview;
    }

    private void initDrawerPos(int currId){
        mNavigationView.getMenu().findItem(prevId).setChecked(false);
        mNavigationView.getMenu().findItem(currId).setChecked(true);
        prevId = currId;
    }

    private class Logout extends AsyncTask<Void, Void, Void> {
        boolean success = false;
        SharedPreferences sh;

        @Override
        protected Void doInBackground(Void... paramVarArgs) {
            sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
            String deviceId = LifeCareHandler.getInstance()
                    .getMyDeviceID(DashboardActivity.this);
            Timber.d("Logout! DEVICE ID = " + deviceId);

            success = LifeCareHandler.getInstance().logout(deviceId);
            LifeCareHandler.getInstance().clearPrefCookies();

            Timber.d("Successfully logged out from server? = " + success);

            return null;
        }

        @Override
        protected void onPostExecute(Void paramVoid) {

            new AlertDialog.Builder(DashboardActivity.this, R.style.dialog)
                    .setTitle(R.string.logout_success_title)
                    .setMessage(R.string.logout_success_message)
                    .setNegativeButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            Intent intent = new Intent(DashboardActivity.this, LoginActivity2.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String permissions[], int[] grantResults) {
        switch (requestCode) {
            case OverviewFragment.MY_PERMISSIONS_REQUEST_BLE_AND_POP_UP: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    // ((CameraFragment)fragment).restartPreview();
                    Log.d("Permission","Location Permission granted!");

                } else {
                    Log.d("Permission","Location Permission denied!");
                }
            }
        }
    }
}
