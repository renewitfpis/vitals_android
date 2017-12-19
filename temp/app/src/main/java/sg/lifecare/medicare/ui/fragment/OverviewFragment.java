package sg.lifecare.medicare.ui.fragment;

import android.Manifest;
import android.Manifest.permission;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmObject;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.ble.BleException;
import sg.lifecare.medicare.ble.DeviceMeter;
import sg.lifecare.medicare.ble.aandd.AAndDMeterListener;
import sg.lifecare.medicare.ble.profile.AbstractProfile;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.SpO2;
import sg.lifecare.medicare.database.model.Step;
import sg.lifecare.medicare.database.model.Temperature;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.object.LocalMeasurementDevicesHandler;
import sg.lifecare.medicare.object.OverviewItem;
import sg.lifecare.medicare.ui.BloodPressureReadingActivity;
import sg.lifecare.medicare.ui.ChartActivity;
import sg.lifecare.medicare.ui.DashboardActivity;
import sg.lifecare.medicare.ui.GeneralGlucoseReadingActivity;
import sg.lifecare.medicare.ui.MedicationTakenActivity;
import sg.lifecare.medicare.ui.SpO2ReadingActivity;
import sg.lifecare.medicare.ui.TakeNotesActivity;
import sg.lifecare.medicare.ui.TemperatureReadingActivity;
import sg.lifecare.medicare.ui.WeightReadingActivity;
import sg.lifecare.medicare.ui.adapter.OverviewAdapter3;
import sg.lifecare.medicare.ui.pairing.ConvertGatewayActivity;
import sg.lifecare.medicare.ui.pairing.DevicePairingMenuActivity;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.ui.view.MarginDecoration;
import sg.lifecare.medicare.utils.BleUtil;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static sg.lifecare.medicare.ui.adapter.OverviewAdapter3.LAST_ITEM;
import static sg.lifecare.medicare.ui.adapter.OverviewAdapter3.LAST_ITEM_CAREGIVER;
import static sg.lifecare.medicare.ui.adapter.OverviewAdapter3.TYPE_SEPARATOR;
import static sg.lifecare.medicare.ui.adapter.OverviewAdapter3.TYPE_STEP;



/**
 * Main fragment for dashboard
 */
public class OverviewFragment extends Fragment {

    private static String TAG = "OverviewFragment";

    public final static int REQUEST_ENABLE_BT = 251;

    public final static int MY_PERMISSIONS_REQUEST_BLE_AND_POP_UP = 102;

    private final static int REQ_MEDIC = 456;
    private final static int REQ_NOTE = 789;

    public static OverviewFragment newInstance() {
        return new OverviewFragment();
    }

    private OverviewAdapter3 adapter;
    private ArrayList<RealmObject> dataList;
    private ArrayList<OverviewItem> mDataList;

    private DeviceMeter deviceMeter = null;
    private View view;
    ListView listView;
    RecyclerView recyclerView;
    private Step step;

    private android.app.AlertDialog mBleEnableDialog;

    private boolean isPedometerPaired = false;
    private boolean isSeparatorRemoved = false;
    private static boolean isRetrieving = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Timber.tag(TAG);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_overview_general, container, false);
        isSeparatorRemoved = false;
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mBleEnableDialog = BleUtil.enableBleDialogBuilder(getActivity(),
                mBleSettingsButtonListener, mBleCancelButtonListener).create();

        boolean isTerumoUser = getActivity().getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE)
                .getBoolean("is_terumo_user",false);
        Timber.d("onCreateView");
        dataList = new ArrayList<>();
        //TODO:adapter = new OverviewAdapter2(dataList,getContext());

        mDataList = new ArrayList<>();
        adapter = new OverviewAdapter3(mDataList,getContext(),getActivity());
        initData();
        //listView = (ListView) view.findViewById(R.id.list_view);
        //listView.setAdapter(adapter);
        step = new Step();

        final SwipeRefreshLayout swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        swipeContainer.setColorSchemeResources(R.color.colorPrimaryGreen,R.color.colorPrimaryGreen);
        swipeContainer.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeContainer.setRefreshing(false);
            }
        });
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.addItemDecoration(new MarginDecoration(getActivity()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new OverviewAdapter3.ClickListener() {
            @Override
            public void onItemClick(int position, int type, View v) {
                Timber.d("ON ITEM CLICK!");
                if(type == OverviewAdapter3.TYPE_MEDICATION){
                    Intent intent = new Intent(getActivity(), MedicationTakenActivity.class);
                    startActivityForResult(intent, REQ_MEDIC);
                }else if (type == OverviewAdapter3.TYPE_NOTE){
                    Intent intent = new Intent(getActivity(), TakeNotesActivity.class);
                    startActivityForResult(intent, REQ_NOTE);
                }else if (type == OverviewAdapter3.TYPE_SYMPTOM){
                    Intent intent = new Intent(getActivity(), TakeNotesActivity.class);
                    intent.putExtra(TakeNotesActivity.TYPE, TakeNotesActivity.REQ_SYMPTOM);
                    startActivityForResult(intent, REQ_NOTE);
                }else if (type == OverviewAdapter3.TYPE_PHOTO){
                    Intent intent = new Intent(getActivity(), TakeNotesActivity.class);
                    intent.putExtra(TakeNotesActivity.TYPE, TakeNotesActivity.REQ_PHOTO);
                    startActivityForResult(intent, REQ_NOTE);
                }
                else {
                    Timber.d("ON ITEM CLICK!2");
                    if(v.getTag() instanceof RealmObject) {
                        RealmObject ro = (RealmObject) v.getTag();
                        if (ro == null) {
                            Timber.d("ON ITEM CLICK!R");
                            return;
                        }
                        Timber.d("ON ITEM CLICK!3");
                        if (!(ro instanceof Step)) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat(PatientData.DATE_FORMAT, Locale.ENGLISH);

                            Intent intent = new Intent(getActivity(), ChartActivity.class);

                            Timber.d("Searching Current type ... is null ? " + (ro == null) + " type? = " + type);
                            if (ro instanceof Terumo) {
                                Timber.d("11Current type = " + "terumo");
                                intent.putExtra("date", (dateFormat.format(((Terumo) ro).getDate())));
                                startActivity(intent);
                            } else if (ro instanceof BloodPressure) {
                                Timber.d("11Current type = " + "bp");
                                intent.putExtra("date", (dateFormat.format(((BloodPressure) ro).getDate())));
                                intent.putExtra("type", ChartActivity.BLOOD_PRESSURE_TYPE);
                                startActivity(intent);
                            } else if (ro instanceof Weight) {
                                Timber.d("11Current type = " + "weight");
                                intent.putExtra("date", (dateFormat.format(((Weight) ro).getDate())));
                                intent.putExtra("type", ChartActivity.WEIGHT_TYPE);
                                startActivity(intent);
                            } else if (ro instanceof SpO2) {
                                Timber.d("11Current type = " + "spo2");
                                intent.putExtra("date", (dateFormat.format(((SpO2) ro).getDate())));
                                intent.putExtra("type", ChartActivity.SPO2_TYPE);
                                startActivity(intent);
                            } else if (ro instanceof Temperature) {
                                intent.putExtra("date", (dateFormat.format(((Temperature) ro).getDate())));
                                intent.putExtra("type", ChartActivity.TEMPERATURE_TYPE);
                                startActivity(intent);
                            } else if (ro instanceof Step) {

                            }
                        } else {
                            Timber.d("ENTER STEP, isDevicePaired? = " + mDataList.get(TYPE_STEP).isDevicePaired());
                            Timber.d("ENTER STEP, getState? = " + mDataList.get(TYPE_STEP).getStringState());

                            if(mDataList.get(TYPE_STEP).isDevicePaired()) {
                                if (mDataList.get(TYPE_STEP).getState() == Step.DISCONNECTED) {
                                    if (BleUtil.isBleEnabled()) {
                                        startDetect();
                                    } else {
                                        mBleEnableDialog.show();
                                    }
                                }
                            }else{
                                Intent intent = new Intent(getActivity(), ConvertGatewayActivity.class);
                                intent.putExtra("medical_device", MedicalDevice.findModel(Model.ZENCRO_X6));
                            }
                        }
                    }
                }
            }

            @Override
            public void onPairButtonClick(int position, int type, View v) {
                //RealmObject ro = (RealmObject)v.getTag();

                if(type != TYPE_STEP) {
                    Intent intent = null;
                    ArrayList<Class> readingClasses = new ArrayList<>();
                    readingClasses.add(GeneralGlucoseReadingActivity.class);
                    readingClasses.add(BloodPressureReadingActivity.class);
                    readingClasses.add(WeightReadingActivity.class);
                    readingClasses.add(TemperatureReadingActivity.class);
                    readingClasses.add(SpO2ReadingActivity.class);
                    readingClasses.add(TemperatureReadingActivity.class);

                    if(mDataList.get(type).getState()==Step.DISCONNECTED){
                        mBleEnableDialog.show();
                    } else if (mDataList.get(type).isDevicePaired()) {
                        intent = new Intent(getActivity(), readingClasses.get(type));
                    } else {
                        if (type == OverviewAdapter3.TYPE_TERUMO) {
                            intent = new Intent(getActivity(), DevicePairingMenuActivity.class);
                            intent.putExtra(DevicePairingMenuActivity.IS_GLUCOSE, true);
                        } else if (type == OverviewAdapter3.TYPE_SPO2) {
                            intent = new Intent(getActivity(), DevicePairingMenuActivity.class);
                            intent.putExtra(DevicePairingMenuActivity.IS_SPO2, true);
                        } else if (type == OverviewAdapter3.TYPE_BLOOD_PRESSURE) {
                            //intent = new Intent(getActivity(), ConvertGatewayActivity.class);
                            //intent.putExtra("medical_device", MedicalDevice.findModel(Model.AANDD_UA_651));
                            intent = new Intent(getActivity(), DevicePairingMenuActivity.class);
                            intent.putExtra(DevicePairingMenuActivity.IS_BLOOD_PRESSURE, true);
                        } else if (type == OverviewAdapter3.TYPE_WEIGHT) {
                            //intent = new Intent(getActivity(), ConvertGatewayActivity.class);
                            //intent.putExtra("medical_device", MedicalDevice.findModel(Model.AANDD_UC_352));
                            intent = new Intent(getActivity(), DevicePairingMenuActivity.class);
                            intent.putExtra(DevicePairingMenuActivity.IS_SCALE, true);
                        } else if (type == OverviewAdapter3.TYPE_TEMPERATURE) {
                            intent = new Intent(getActivity(), DevicePairingMenuActivity.class);
                            //intent.putExtra("medical_device", MedicalDevice.findModel(Model.AANDD_UT_201));
                            intent.putExtra(DevicePairingMenuActivity.IS_THERMOMETER, true);
                        }
                    }

                    if (intent != null) {
                        startActivity(intent);
                    }
                }else{
                    if(mDataList.get(TYPE_STEP).isDevicePaired()) {
                        if (mDataList.get(TYPE_STEP).getState() == Step.DISCONNECTED) {
                            if (BleUtil.isBleEnabled()) {
                                startDetect();
                            } else {
                                mBleEnableDialog.show();
                            }
                        }
                    }else{
                        Intent intent = new Intent(getActivity(), ConvertGatewayActivity.class);
                        intent.putExtra("medical_device", MedicalDevice.findModel(Model.ZENCRO_X6));
                        startActivity(intent);
                    }
                }
            }

            @Override
            public void onMenuItemClick(int position, int type, View v) {
                if(position == R.id.manual_entry){
                    Timber.d("Clicked on manual entry!");

                    if( type != OverviewAdapter3.TYPE_STEP)
                    {
                        Intent intent = null;

                        Timber.d("Searching type? = " + type);
                        if (type == OverviewAdapter3.TYPE_TERUMO) {
                            intent = new Intent(getActivity(), GeneralGlucoseReadingActivity.class);
                        } else if (type == OverviewAdapter3.TYPE_BLOOD_PRESSURE) {
                            Timber.d("11Current type = " + "bp");
                            intent = new Intent(getActivity(), BloodPressureReadingActivity.class);
                        } else if (type == OverviewAdapter3.TYPE_WEIGHT) {
                            Timber.d("11Current type = " + "weight");
                            intent = new Intent(getActivity(), WeightReadingActivity.class);
                        } else if (type == OverviewAdapter3.TYPE_SPO2) {
                            //TODO
                            intent = new Intent(getActivity(), SpO2ReadingActivity.class);
                        } else if (type == OverviewAdapter3.TYPE_TEMPERATURE) {
                            intent = new Intent(getActivity(), TemperatureReadingActivity.class);
                        }

                        if (intent != null) {
                            intent.putExtra("manual_entry", true);
                            startActivity(intent);
                        }

                    }
                }/*else if(position == R.id.hide){
                    Timber.d("Clicked on hide!");
                }else{
                    Timber.d("Clicked on unpair!");
                }*/
            }
        });

/*        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                RealmObject ro = (RealmObject)view.getTag();
                if(!(ro instanceof Step)) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(PatientData.DATE_FORMAT, Locale.ENGLISH);

                    Intent intent = new Intent(getActivity(), ChartActivity.class);

                    Timber.d("Searching Current type = " + "");
                    if (ro instanceof Terumo) {
                        Timber.d("11Current type = " + "terumo");
                        intent.putExtra("date", (dateFormat.format(((Terumo) ro).getDate())));
                        startActivity(intent);
                    } else if (ro instanceof BloodPressure) {
                        Timber.d("11Current type = " + "bp");
                        intent.putExtra("date", (dateFormat.format(((BloodPressure) ro).getDate())));
                        intent.putExtra("type", ChartActivity.BLOOD_PRESSURE_TYPE);
                        startActivity(intent);
                    } else if (ro instanceof Weight) {
                        Timber.d("11Current type = " + "weight");
                        intent.putExtra("date", (dateFormat.format(((Weight) ro).getDate())));
                        intent.putExtra("type", ChartActivity.WEIGHT_TYPE);
                        startActivity(intent);
                    } else if (ro instanceof SpO2) {
                        //TODO
                       *//* intent.putExtra("date", (dateFormat.format(((SpO2) ro).getDate())));
                        intent.putExtra("type", ChartActivity.WEIGHT_TYPE);
                        startActivity(intent);*//*
                    } else if (ro instanceof Temperature) {
                        intent.putExtra("date", (dateFormat.format(((Temperature) ro).getDate())));
                        intent.putExtra("type", ChartActivity.TEMPERATURE_TYPE);
                        startActivity(intent);
                    }
                }else{
                    if(step.getState()==Step.DISCONNECTED){
                        if(BleUtil.isBleEnabled()){
                            startDetect();
                        }else{
                            mBleEnableDialog.show();
                        }
                    }
                }
            }
        });*/

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQ_NOTE || requestCode == REQ_MEDIC){
            if(resultCode == RESULT_OK){
                if(getActivity() instanceof DashboardActivity){
                    ((DashboardActivity) getActivity()).showTimelineFragment();
                }
            }
        }
    }

    private void checkPermission(){
        Timber.d("Checking Permission!");
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(getActivity(),
                permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(getActivity(),
                        permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(getActivity(),
                        permission.RECEIVE_BOOT_COMPLETED)
                        != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(getActivity(),
                        permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(getActivity(),
                        permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(getActivity(),
                        permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED
                )
        {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    ||
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.ACCESS_COARSE_LOCATION)
                    ||
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            permission.RECEIVE_BOOT_COMPLETED)
                    ||
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.CAMERA)
                    ||
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            permission.WRITE_EXTERNAL_STORAGE)
                    ||
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.READ_EXTERNAL_STORAGE)
                    )
            {
                Timber.d("Checking Permission!");
                showMessageOKCancel("You need to allow location access for bluetooth features.",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(getActivity(),
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                                permission.ACCESS_COARSE_LOCATION,
                                                permission.RECEIVE_BOOT_COMPLETED,
                                                Manifest.permission.CAMERA,
                                                permission.WRITE_EXTERNAL_STORAGE,
                                                permission.READ_EXTERNAL_STORAGE
                                                /*permission.SYSTEM_ALERT_WINDOW,
                                                permission.RECEIVE_BOOT_COMPLETED*/},
                                        MY_PERMISSIONS_REQUEST_BLE_AND_POP_UP);
                            }
                        });
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                Timber.d("Requesting Permission!");

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                permission.ACCESS_COARSE_LOCATION,
                                permission.RECEIVE_BOOT_COMPLETED,
                                Manifest.permission.CAMERA,
                                permission.WRITE_EXTERNAL_STORAGE,
                                permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_BLE_AND_POP_UP);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        else if(ContextCompat.checkSelfPermission(getActivity(),permission.RECEIVE_BOOT_COMPLETED)
                == PackageManager.PERMISSION_GRANTED){
            Timber.i("User granted start up permission");
        }
        else if(ContextCompat.checkSelfPermission(getActivity(),permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getActivity(),permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getActivity(),permission.RECEIVE_BOOT_COMPLETED)
                        == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getActivity(),permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getActivity(),permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getActivity(),permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED

                )
        {
            Timber.i("User granted some or all permissions");
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
            String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_BLE_AND_POP_UP: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    // ((CameraFragment)fragment).restartPreview();
                    Log.d(TAG,"Location Permission granted!");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    Log.d(TAG,"Location Permission denied!");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }

                if (grantResults.length > 0
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                    // ((CameraFragment)fragment).restartPreview();
                    Log.d(TAG,"POP UP Permission granted!");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    Log.d(TAG,"POP UP Permission denied!");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                if (grantResults.length > 0
                        && grantResults[3] == PackageManager.PERMISSION_GRANTED) {

                    // ((CameraFragment)fragment).restartPreview();
                    Log.d(TAG,"CAM Permission granted!");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    Log.d(TAG,"CAM Permission denied!");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }

                if (grantResults.length > 0
                        && grantResults[4] == PackageManager.PERMISSION_GRANTED) {

                    // ((CameraFragment)fragment).restartPreview();
                    Log.d(TAG,"External Storage Permission granted!");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    Log.d(TAG,"External Storage Permission denied!");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Timber.d("onActivityCreated");

        checkPermission();
        FragmentActivity parent = getActivity();
        if (parent instanceof DashboardActivity) {
            if(((DashboardActivity)parent).isCaregiver()) {
                Timber.d("is caregiver!!");
                ((DashboardActivity) parent).setToolbar(R.string.title_overview, R.drawable.ic_toolbar, R.drawable.patient_list_icon, R.drawable.ic_timeline);
            }else{
                Timber.d("is not caregiver!!");
                ((DashboardActivity) parent).setToolbar(R.string.title_overview, R.drawable.ic_toolbar, R.drawable.ic_timeline);
            }
            ((DashboardActivity)parent).setToolbarListener(mToolbarListener);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopDetect();

        Timber.d("overview onDestroy");
    }

    @Override
    public void onResume(){
        super.onResume();
        getLocalConnectedDevices();
        Log.d(TAG,"ON RESUME!");
        if(getActivity() instanceof DashboardActivity) {
            ((DashboardActivity) getActivity()).selectTab(R.id.nav_bar_overview);
        }
        try {
            if(!isRetrieving) {
                new RetrieveVitalsDataTask().execute();
            }
        }
        catch(IllegalStateException e){
            Timber.e("Illegal!!\n" + e.getMessage());
        }
    }

    public void startDetect() {
        if(mDataList.get(TYPE_STEP).getState()==Step.CONNECTED
                || mDataList.get(TYPE_STEP).getState()==Step.CONNECTING)
            return;

        try{
            Timber.d("Started detecting");
            ArrayList<MedicalDevice> list =
                    LocalMeasurementDevicesHandler.getInstance().getConnectedMedicalDeviceList();

            ArrayList<String> ids = new ArrayList<>();
            //TODO: check
            for (MedicalDevice device : list) {
                if(device.getModel()== Model.ZENCRO_X6){
                    ids.add(device.getDeviceId());
                }
            }
            if(deviceMeter==null) {
                deviceMeter = new DeviceMeter(getActivity(), mMeterListener, Model.ZENCRO_X6,ids);//DeviceMeter.TYPE_STEP
                deviceMeter.setMode(DeviceMeter.MODE_READING);
            }
            deviceMeter.startScanning();
            mDataList.get(TYPE_STEP).setState(Step.CONNECTING);
            adapter.notifyDataSetChanged();
        } catch (BleException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void stopDetect() {
        if(deviceMeter!=null) {
            Timber.d("Stop detect @Overview");
            deviceMeter.clear();
            deviceMeter.stopScanning();
            deviceMeter.disconnect();
            deviceMeter.close();
            deviceMeter=null;
            mDataList.get(TYPE_STEP).setState(Step.DISCONNECTED);
            adapter.notifyDataSetChanged();
        }
    }

    //TODO
    private void getLocalConnectedDevices(){

        if(mDataList==null || mDataList.size()== 0){
            return;
        }
        int lastItem = ((DashboardActivity)getActivity()).isCaregiver() ? LAST_ITEM_CAREGIVER : LAST_ITEM ;
        if (!BleUtil.isBleEnabled()) {
            Timber.e("BLE NOT ENABLED");
            //mBleEnableDialog.show();
            for(int i = 0; i < lastItem; i++){
                mDataList.get(i).setState(Step.DISCONNECTED);
            }
            return;
        }else{
            for(int i = 0; i < lastItem; i++){
                if(i != OverviewAdapter3.TYPE_STEP)
                    mDataList.get(i).setState(Step.CONNECTED);
            }
        }

        ArrayList<MedicalDevice> list =
                LocalMeasurementDevicesHandler.getInstance().getConnectedMedicalDeviceList();

        for (MedicalDevice device : list) {
            Timber.d("device_model: " + device.getModel());
            if(device.getModel()==Model.TERUMO_MEDISAFE_FIT || device.getModel()==Model.ACCU_CHEK_AVIVA_CONNECT){
                mDataList.get(OverviewAdapter3.TYPE_TERUMO).setDevicePaired(true);
            }else if(device.getModel()==Model.AANDD_UC_352){
                mDataList.get(OverviewAdapter3.TYPE_WEIGHT).setDevicePaired(true);
            }else if(device.getModel()==Model.AANDD_UA_651){
                mDataList.get(OverviewAdapter3.TYPE_BLOOD_PRESSURE).setDevicePaired(true);
            }else if(device.getModel()==Model.AANDD_UT_201){
                mDataList.get(OverviewAdapter3.TYPE_TEMPERATURE).setDevicePaired(true);
            }else if(device.getModel()==Model.BERRY_BM1000B || device.getModel()==Model.NONIN_3230){
                mDataList.get(OverviewAdapter3.TYPE_SPO2).setDevicePaired(true);
            }else if(device.getModel()==Model.ZENCRO_X6){
                mDataList.get(OverviewAdapter3.TYPE_STEP).setDevicePaired(true);
            } else if (device.getModel() == Model.JUMPER_FR302) {
                mDataList.get(OverviewAdapter3.TYPE_TEMPERATURE).setDevicePaired(true);
            } else if (device.getModel() == Model.JUMPER_JPD500E) {
                mDataList.get(OverviewAdapter3.TYPE_SPO2).setDevicePaired(true);
            } else if (device.getModel() == Model.URION_BP_U80E) {
                mDataList.get(OverviewAdapter3.TYPE_BLOOD_PRESSURE).setDevicePaired(true);
            } else if (device.getModel() == Model.YOLANDA_LITE) {
                mDataList.get(OverviewAdapter3.TYPE_WEIGHT).setDevicePaired(true);
            } else if (device.getModel() == Model.VIVACHEK_INO_SMART) {
                mDataList.get(OverviewAdapter3.TYPE_TERUMO).setDevicePaired(true);
            }
        }
       /* if(isDevicePaired){
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
        }*/
    }

    private class RetrieveVitalsDataTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {

          /*  if (mListener != null) {
                mListener.onRequestStart();
            }*/
            isRetrieving = true;

            if(dataList!=null) {
                dataList.clear();
            }else{
                dataList = new ArrayList<>();
            }

            if(adapter!=null){
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

            final Map<Date, RealmObject> results =
                    PatientData.getInstance().getLatestVitalsData(realm, true);

            Timber.d("RetrieveVitalsDataTask: size=" + results.size());
            if (results.size() > 0) {

                for (final Map.Entry<Date,RealmObject> entry :results.entrySet()) {
                    final RealmObject obj = realm.copyFromRealm(entry.getValue());

                    if(obj instanceof Terumo){
                        OverviewItem item = mDataList.get(OverviewAdapter3.TYPE_TERUMO);
                        //item.setValue(((Terumo) obj).getValue()+"");
                        item.setValue(String.format(Locale.getDefault(), "%.1f", ((Terumo) obj).getValue()));
                        item.setDate(((Terumo) obj).getDate());
                    }else if(obj instanceof BloodPressure){
                        OverviewItem item = mDataList.get(OverviewAdapter3.TYPE_BLOOD_PRESSURE);
                        item.setValue(((BloodPressure) obj).getSystolic()+"/"+((BloodPressure) obj).getDistolic());
                        item.setDate(((BloodPressure) obj).getDate());
                    }else if(obj instanceof Weight){
                        OverviewItem item = mDataList.get(OverviewAdapter3.TYPE_WEIGHT);
                        item.setValue(((Weight) obj).getWeight()+"");
                        item.setDate(((Weight) obj).getDate());
                    }else if(obj instanceof Temperature){
                        OverviewItem item = mDataList.get(OverviewAdapter3.TYPE_TEMPERATURE);
                        item.setValue(((Temperature) obj).getValue()+"");
                        item.setDate(((Temperature) obj).getDate());
                    }else if(obj instanceof SpO2){
                        OverviewItem item = mDataList.get(OverviewAdapter3.TYPE_SPO2);
                        item.setValue(((SpO2) obj).getPulseRate()+"");
                        item.setDate(((SpO2) obj).getDate());
                    }
                    Timber.d("1111");
                    if(getActivity()!=null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                }

                if(getActivity()!=null){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            ArrayList<MedicalDevice> list =
                                    LocalMeasurementDevicesHandler.getInstance().getConnectedMedicalDeviceList();

                            //TODO: check
                            for (MedicalDevice device : list) {
                                if(device.getModel()== Model.ZENCRO_X6){
                                    //mDataList.add(step);
                                    adapter.notifyDataSetChanged();
                                    isPedometerPaired = true;
                                    break;
                                }
                            }
                        }
                    });
                }

                Timber.d("RetrieveVitalsDataTask: list size=" + dataList.size());

                realm.close();

                Timber.d("RetrieveVitalsDataTask closed realm");

                return true;
            }

            realm.close();
            Timber.d("RetrieveVitalsDataTask closed realm2");
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Timber.d("onPostExecute");
            isRetrieving = false;

            if(BleUtil.isBleEnabled() && isPedometerPaired ) {
                startDetect();
            }
            //            adapter.addAll(dataList);
            //            adapter.notifyDataSetChanged();
            /*TODO:if(getActivity()!=null) {
                if (dataList.isEmpty()) {
                    view.findViewById(R.id.empty).setVisibility(View.VISIBLE);
                } else {
                    view.findViewById(R.id.empty).setVisibility(View.INVISIBLE);
                }
            }*/
        }
    }


    private CustomToolbar.OnToolbarClickListener mToolbarListener = new CustomToolbar.OnToolbarClickListener() {
        @Override
        public void leftButtonClick() {
            ((DashboardActivity)getActivity()).openLeftDrawer();
        }

        @Override public void rightButtonClick() {
            if(((DashboardActivity)getActivity()).isCaregiver()) {
                ((DashboardActivity) getActivity()).openRightDrawer();
            }else{
                ((DashboardActivity)getActivity()).showTimelineFragment();
            }
        }

        @Override public void secondRightButtonClick() {
            //((DashboardActivity)getActivity()).collapseFloatingMenu();
            if(((DashboardActivity)getActivity()).isCaregiver()) {
                ((DashboardActivity)getActivity()).showTimelineFragment();
            }
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

        }

        @Override
        public void onDataRetrieved(final BloodPressure bp) {

        }

        @Override
        public void onDataRetrieved(final Weight weight) {
            Log.d(TAG,"ON WEIGHT DATA RETRIEVED: " + weight.getWeight() + " " + weight.getStringUnit());
        }

        @Override
        public void onDataRetrieved(Object object) {
            if(object instanceof Integer){
                if(mDataList.get(TYPE_STEP).getState()!=Step.CONNECTED) {
                    mDataList.get(TYPE_STEP).setState(Step.CONNECTED);
                    //mDataList.remove(step);
                    //mDataList.add(0,step);
                }
                final Integer steps = (Integer)object;

                Timber.d("ON DATA RETRIEVED steps =" + steps);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDataList.get(TYPE_STEP).setValue(steps+"");
                        //((Step)dataList.get(dataList.size()-1)).setSteps(steps);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }

        @Override
        public void onInvalidDataReturned() {
            deviceMeter.startScanning();
        }

        @Override
        public void onConnectionStateChanged(int status) {
            Timber.d("On Connection State Changed " + status);
            if(status== BluetoothAdapter.STATE_DISCONNECTED){
                Timber.d("On Connection State - DISCONNECTED");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        endReading();
                    }
                });
            }
        }
    };

    public void initData(){
        if(mDataList!=null) {
            mDataList.clear();
        }

        int totalItemsToDisplay = 0;
        boolean isCaregiver = ((DashboardActivity)getActivity()).isCaregiver();
        if(isCaregiver){
            totalItemsToDisplay = LAST_ITEM_CAREGIVER + 1;
        }else {
            totalItemsToDisplay = OverviewAdapter3.LAST_ITEM + 1;
        }
        for(int i = 0; i < totalItemsToDisplay; i++) {
            OverviewItem item = new OverviewItem();
            item.setValue("");
            item.setType(i);
            item.setDate(Calendar.getInstance().getTime());
            item.setDevicePaired(false);
            item.setUnit(-1);
            mDataList.add(item);
            Timber.d("Added item " + i );
        }
        if(!isCaregiver) {
            if (OverviewAdapter3.TYPE_SEPARATOR % 2 == 0) {
                if (mDataList.get(OverviewAdapter3.TYPE_SEPARATOR).getType() == TYPE_SEPARATOR) {
                    mDataList.remove(OverviewAdapter3.TYPE_SEPARATOR);
                }
            /*if(!isSeparatorRemoved) {
                mDataList.remove(OverviewAdapter3.TYPE_SEPARATOR);
                isSeparatorRemoved = true;
            }*/
            }
        }
        Timber.d("SET VISIBILITY GONE");
        view.findViewById(R.id.empty).setVisibility(View.GONE);
    }

    public void endReading(){
        Timber.d("Ended Reading");
        stopDetect();
    }

    @Override
    public void onPause(){
        super.onPause();

        stopDetect();

        Timber.d("overview onPause");

        /*if(realm!=null) {
            realm.close();
            realm = null;
            Timber.d("overview closed realm");
        }*/
    }


    public void scrollToTop(){

        recyclerView.scrollToPosition(View.SCROLL_INDICATOR_TOP);
    }

}
