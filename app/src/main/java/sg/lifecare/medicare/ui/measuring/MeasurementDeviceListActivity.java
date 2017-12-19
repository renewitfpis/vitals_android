package sg.lifecare.medicare.ui.measuring;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

import sg.lifecare.medicare.BuildConfig;
import sg.lifecare.medicare.MediCareApplication;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.object.LocalMeasurementDevicesHandler;
import sg.lifecare.medicare.ui.AccuChekReadingActivity;
import sg.lifecare.medicare.ui.BloodGlucoseReadingActivity;
import sg.lifecare.medicare.ui.BloodPressureReadingActivity;
import sg.lifecare.medicare.ui.DeviceReadingActivity;
import sg.lifecare.medicare.ui.SpO2ReadingActivity;
import sg.lifecare.medicare.ui.TemperatureReadingActivity;
import sg.lifecare.medicare.ui.WeightReadingActivity;
import sg.lifecare.medicare.ui.pairing.AddDeviceFragment.OnPairingDetected;
import sg.lifecare.medicare.ui.pairing.ConvertGatewayActivity;
import sg.lifecare.medicare.ui.pairing.DevicePairingMenuActivity;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.utils.BleUtil;
import sg.lifecare.medicare.utils.LifeCareHandler;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Brand;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import sg.lifecare.medicare.utils.NfcUtil;
import timber.log.Timber;

/**
 * Created by ct on 13/1/16.
 */
public class MeasurementDeviceListActivity extends Activity
{
    private final String TAG = "DeviceList";

    int REQUEST_ENABLE_BT = 123;
    int REQ_READING_STATUS = 124;

    private MedicalDevice device;
    private ListView deviceListView;
    private DeviceListAdapter adapter;

    private RelativeLayout loadingView;
    private ProgressBar loading;

    private Model model;

    protected OnPairingDetected mCallback;

    private ArrayList<MedicalDevice> list = new ArrayList<>();

    private String entityId;

    private android.app.AlertDialog mNfcEnableDialog;
    private android.app.AlertDialog mBleEnableDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_list_main_view);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        CustomToolbar mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_activity_measure);
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.hideRightButton();
        mToolbar.hideSecondRightButton();
        mToolbar.setListener(mToolbarListener);

        SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
        entityId = sh.getString("entity_id", "");
        LocalMeasurementDevicesHandler.initialize(this,entityId);

        mNfcEnableDialog = NfcUtil.enableNfcDialogBuilder(this,
                mNfcSettingsButtonListener, mNfcCancelButtonListener).create();
        mBleEnableDialog = BleUtil.enableBleDialogBuilder(this,
                mBleSettingsButtonListener, mBleCancelButtonListener).create();

        mBleEnableDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                loadingView.setVisibility(View.GONE);
                loading.setVisibility(View.GONE);
            }
        });
        loadingView = (RelativeLayout) findViewById(R.id.loading_view);
        loading = (ProgressBar) findViewById(R.id.loading);
        loading.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(R.color.progress_bar),
                android.graphics.PorterDuff.Mode.SRC_IN
        );

        ImageButton addDeviceButton = (ImageButton) findViewById(R.id.add_device_button);
        addDeviceButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                proceedToAddDevice();
            }
        });

        deviceListView = (ListView) findViewById(R.id.device_listview);

        adapter = new DeviceListAdapter(MeasurementDeviceListActivity.this, R.layout.device_list_row);

        deviceListView.setAdapter(adapter);
        deviceListView.setEmptyView(findViewById(R.id.empty_list));
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Log.d(TAG, "onItemClick=" + position);
                device = adapter.getItem(position);
                Brand brand = device.getBrand();
                model = device.getModel();
                Log.d(TAG, "Brand=" + brand);
                Log.d(TAG, "Model=" + device.getModel());
                if (brand == Brand.TERUMO) {
                    Intent intent = new Intent(MeasurementDeviceListActivity.this,
                            BloodGlucoseReadingActivity.class);
                    startActivityForResult(intent,REQ_READING_STATUS);
                }else if(model == Model.AANDD_UA_651){
                    Intent intent = new Intent(MeasurementDeviceListActivity.this,
                            BloodPressureReadingActivity.class);
                    startActivityForResult(intent,REQ_READING_STATUS);
                }else if(model == Model.AANDD_UC_352){
                    Intent intent = new Intent(MeasurementDeviceListActivity.this,
                            WeightReadingActivity.class);
                    startActivityForResult(intent,REQ_READING_STATUS);
                }else if (model == Model.BERRY_BM1000B || model == Model.NONIN_3230) {
                    MedicalDevice md = MedicalDevice.findModel(model);

                    if (md != null) {
                         if (MedicalDevice.WCONFIG_CODE_BLE.equalsIgnoreCase(md.getMode())) {
                            Intent intent = new Intent(MeasurementDeviceListActivity.this,
                                    SpO2ReadingActivity.class);
                            intent.putExtra("device", device);
                            startActivityForResult(intent,REQ_READING_STATUS);
                         }
                    }
                }else if (model == Model.AANDD_UT_201){
                    Intent intent = new Intent(MeasurementDeviceListActivity.this,
                            TemperatureReadingActivity.class);
                    startActivityForResult(intent,REQ_READING_STATUS);
                }else if (model == Model.ACCU_CHEK_AVIVA_CONNECT){
                    Intent intent = new Intent(MeasurementDeviceListActivity.this,
                            AccuChekReadingActivity.class);
                    intent.putExtra("device", device);
                    startActivityForResult(intent,REQ_READING_STATUS);
                }
            }
        });

        deviceListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {

            @Override
            public boolean onItemLongClick(final AdapterView<?> adapterView, View view, final int position, long l) {
                new AlertDialog.Builder(MeasurementDeviceListActivity.this)
                        .setTitle("Unpair Device")
                        .setMessage("Are you sure you want to unpair " + adapter.getItem(position).getAssignedName() + "?")
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .setPositiveButton(R.string.dialog_ok, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                device = adapter.getItem(position);
                                unpairDeviceFromSetting(device.getDeviceId());
                                new UnpairDeviceFromServerTask(device,position).execute();
                            }
                        })
                        .show();
                return true;
            }
        });

    }

    private void unpairDeviceFromSetting(String deviceId){

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for(BluetoothDevice bt : pairedDevices){
            if(bt.getAddress().equalsIgnoreCase(deviceId)){
                try {
                    Log.d(TAG,"Unpairing Device! @unpairDeviceFromSetting");
                    Method m = bt.getClass()
                            .getMethod("removeBond", (Class[]) null);
                    m.invoke(bt, (Object[]) null);
                    Log.d(TAG,"Unpaired Device! @unpairDeviceFromSetting");
                } catch (Exception e) {
                    Log.e(TAG, "UnpairDevice Error!\n" + e.getMessage());
                }
            }
        }
    }

    private void proceedToAddDevice(){
        if(!(BuildConfig.PRODUCT_FLAVOR == MediCareApplication.MEDISAFE)) {
            Intent intent = new Intent(MeasurementDeviceListActivity.this,
                    DevicePairingMenuActivity.class);//TODO: ConvertGatewayActivity
            startActivity(intent);
        }else{
            Intent intent = new Intent(MeasurementDeviceListActivity.this,
                    ConvertGatewayActivity.class);
            intent.putExtra("is_glucose", true);
            startActivity(intent);
        }
    }
    private void enterReadingActivity(MedicalDevice device){
        Intent intent = new Intent(MeasurementDeviceListActivity.this,
                DeviceReadingActivity.class);
        intent.putExtra("device", device);
        startActivity(intent);
    }

    private class UnpairDeviceFromServerTask extends AsyncTask<Void, Void, Void>
    {
        MedicalDevice medicalDevice;
        int position;
        boolean unpaired = false;

        protected UnpairDeviceFromServerTask(MedicalDevice medicalDevice, int position){
            this.medicalDevice = medicalDevice;
            this.position = position;
        }

        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {
            Log.e(TAG, "unpairing!");
            String pairedDeviceId = medicalDevice.getDeviceId();
            String gatewayId =  LifeCareHandler.getInstance().getMyDeviceID(MeasurementDeviceListActivity.this);
           String serial = entityId + "-" + gatewayId + "-" +pairedDeviceId.replace(":",""); //entityId + "-" + gatewayId + "-" + pairedDeviceId.replace(":","");
            JSONObject data = new JSONObject();

            if (medicalDevice == null) {
                Log.e(TAG, "cannot find medical device " + medicalDevice);
                return null;
            }

            try
            {
                data.put("GatewayId", gatewayId);
                data.put("EntityId", entityId);
                data.put("DeviceId", serial);
            }
            catch(JSONException e)
            {
                e.printStackTrace();
            }

            Log.w(TAG, "urlParameters: " + data.toString());

            LocalMeasurementDevicesHandler.getInstance().removeMedicalDevice(medicalDevice);
            unpaired = LifeCareHandler.getInstance().unpairSmartDevices(data);
            Log.w(TAG, "unpair res:" + unpaired);

            return null;
        }

        @Override
        public void onPostExecute(Void result)
        {
            Log.w(TAG, "unpaired: " + unpaired);

            Toast.makeText(MeasurementDeviceListActivity.this,
                    "Successfully unpaired!", Toast.LENGTH_SHORT).show();

            getLocalConnectedDevices();
        }

    }

    @Override
    public void onResume()
    {
        super.onResume();

       // getConnectedDeviceList task = new getConnectedDeviceList();
       // task.execute();

        Timber.d("On Resume");
        if(BuildConfig.PRODUCT_FLAVOR == MediCareApplication.GENERAL){
            if(BleUtil.isBleEnabled()) {
                getLocalConnectedDevices();
            }else{
                mBleEnableDialog.show();
            }
        }else if(BuildConfig.PRODUCT_FLAVOR == MediCareApplication.MEDISAFE) {
            getLocalConnectedDevices();
        }

    }

    @Override
    public void onPause() {
        super.onPause();

    }

    private void getLocalConnectedDevices(){
        Timber.d("Getting Local Connected Devices");
        loadingView.setVisibility(View.GONE);
        loading.setVisibility(View.GONE);
        //list.clear();
        list = LocalMeasurementDevicesHandler.getInstance().getConnectedMedicalDeviceList();
        Timber.d("Local Connected Devices Size = "+list.size());
        adapter.update(list);
        adapter.notifyDataSetChanged();

        if(list!=null){
            if(list.isEmpty()){
                proceedToAddDevice();
                finish();
            }
        }
    }

    private DialogInterface.OnClickListener mNfcCancelButtonListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
        }
    };
    private DialogInterface.OnClickListener mNfcSettingsButtonListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        }
    };

    private DialogInterface.OnClickListener mBleCancelButtonListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            loadingView.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
        }
    };
    private DialogInterface.OnClickListener mBleSettingsButtonListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_ENABLE_BT){
            Log.d(TAG,"BT ONRESULT !");
            if(resultCode==RESULT_OK) {
                Log.d(TAG,"BT ONRESULT OK!");
                //getLocalConnectedDevices();
                //enterReadingActivity(device);
            }else{
                Log.d(TAG,"BT ONRESULT CANCELED!");
                mBleEnableDialog.cancel();
                loadingView.setVisibility(View.GONE);
                loading.setVisibility(View.GONE);
            }
        }else if(requestCode == REQ_READING_STATUS){
            if(resultCode==RESULT_OK) {
                Log.d(TAG,"ONRESULT OK!");
               finish();
            }
        }
    }

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
}
