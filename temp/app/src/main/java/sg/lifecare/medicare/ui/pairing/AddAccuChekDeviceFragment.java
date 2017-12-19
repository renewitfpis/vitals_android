package sg.lifecare.medicare.ui.pairing;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.util.List;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.ble.AccuChekDeviceMeter;
import sg.lifecare.medicare.ble.BleException;
import sg.lifecare.medicare.ble.aandd.AAndDMeter;
import sg.lifecare.medicare.ble.aandd.AAndDMeterListener;
import sg.lifecare.medicare.ble.profile.AbstractProfile;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.ui.view.PairingIndicatorView;
import sg.lifecare.medicare.utils.BleUtil;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static sg.lifecare.medicare.ui.fragment.OverviewFragment.REQUEST_ENABLE_BT;

public class AddAccuChekDeviceFragment extends AddDeviceFragment {

    public static AddAccuChekDeviceFragment newInstance() {
        return new AddAccuChekDeviceFragment();
    }

    private static final String TAG = "AddGenDeviceFragment";

    private AccuChekDeviceMeter deviceMeter;

    private PairingIndicatorView pairingIndicator;
    private AlertDialog mBleEnableDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Timber.d("ON CREATE");
        mBleEnableDialog = BleUtil.enableBleDialogBuilder(getActivity(),
                mBleSettingsButtonListener, mBleCancelButtonListener).create();
    }

    private DialogInterface.OnClickListener mBleCancelButtonListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            mBleEnableDialog.dismiss();
            pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_FAIL_BLE_OFF);
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
            if(resultCode==RESULT_OK) {
                //startPairingProcess();
            }else{
                Timber.d("CANCELED");
                pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_FAIL_BLE_OFF);
            }
        }
    }

    private  void startPairingProcess(){
        Timber.d("START PAIRING");
        try {
            if (myActivity.getDevice() == Model.ACCU_CHEK_AVIVA_CONNECT) {
                deviceMeter = new AccuChekDeviceMeter(getActivity(), mMeterListener, myActivity.getDevice());//,myActivity.getPairedDeviceId()
            }
            deviceMeter.setMode(AAndDMeter.MODE_PAIRING);
            deviceMeter.startScanning();

            if(pairingIndicator!=null){
                pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_BLE);
            }

        } catch (BleException e) {
            Log.e(TAG, e.getMessage());
        }
    }
    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater,
                             ViewGroup paramViewGroup, Bundle paramBundle) {
        View view = paramLayoutInflater.inflate(R.layout.add_aandd_device_step, paramViewGroup, false);

        pairingIndicator = (PairingIndicatorView) view.findViewById(R.id.pairing_indicator);
        pairingIndicator.setProductImage(MedicalDevice.getModelImage(myActivity.getDevice()));
        pairingIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!BleUtil.isBleEnabled()){
                    mBleEnableDialog.show();
                }
            }
        });
        if(BleUtil.isBleEnabled()) {
            pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_BLE);
        }else{
            pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_FAIL_BLE_OFF);
            return view;
        }

       if (myActivity.getDevice() == Model.ZENCRO_X6){
            pairingIndicator.setPairingDescription(R.string.zencro_x6_info_pair_device_step);
            pairingIndicator.setPairingDescriptionImage(R.drawable.x6_start);
        } else if (myActivity.getDevice() == Model.BERRY_BM1000B){
            pairingIndicator.setPairingDescription(R.string.berry_oximeter_info_pair_device_step);
            pairingIndicator.setPairingDescriptionImage(R.drawable.ic_pairing_berry1);
        } else if (myActivity.getDevice() == Model.NONIN_3230){
            pairingIndicator.setPairingDescription(R.string.nonin_oximeter_info_pair_device_step);
            pairingIndicator.setPairingDescriptionImage(R.drawable.ic_pairing_nonin);
        } else if (myActivity.getDevice() == Model.ACCU_CHEK_AVIVA_CONNECT){
            pairingIndicator.setPairingDescription(R.string.accu_chek_info_pair_device_step);
            pairingIndicator.setPairingDescriptionImage(R.drawable.accu_chek_ac);
       }
        pairingIndicator.showPairingDescription();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(BleUtil.isBleEnabled()){
            startPairingProcess();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopDetect();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopDetect();
    }

    private void stopDetect() {
        Timber.d("STOP DETECT");
        if(deviceMeter!=null) {
            Timber.d("STOP DETECT2");
            deviceMeter.clear();
            deviceMeter.stopScanning();
            deviceMeter.disconnect();
            deviceMeter.close();
            deviceMeter=null;
        }
    }

    private AAndDMeterListener mMeterListener = new AAndDMeterListener() {
        @Override
        public void onResult(List<AbstractProfile> records) {
            Log.d(TAG,"RESULTS RETRIEVED! " + records.size());
        }

        @Override
        public void onDeviceBonded(final BluetoothDevice device) {
            Log.d(TAG,"ON DEVICE BONDED! ");
            myActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCallback.onPairSuccess(device.getAddress());

                    Log.d(TAG,"addandd - PAIR DEVICE ID = " + device.getAddress());
                }
            });
        }

        @Override
        public void onDataRetrieved(BloodPressure bp) {

        }

        @Override
        public void onDataRetrieved(Weight weight) {

        }

        @Override
        public void onDataRetrieved(Object object) {

        }

        @Override
        public void onInvalidDataReturned() {

        }

        @Override
        public void onConnectionStateChanged(int status) {
            if(status == BluetoothGatt.STATE_DISCONNECTED){
                mCallback.onPairFailed();
            }
        }

    };
}
