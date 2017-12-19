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

import java.util.List;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.ble.BleException;
import sg.lifecare.medicare.ble.aandd.AAndD201Thermometer;
import sg.lifecare.medicare.ble.aandd.AAndD352WeighScale;
import sg.lifecare.medicare.ble.aandd.AAndD651BloodPressure;
import sg.lifecare.medicare.ble.aandd.AAndDMeter;
import sg.lifecare.medicare.ble.aandd.AAndDMeterListener;
import sg.lifecare.medicare.ble.profile.AbstractProfile;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.ui.view.PairingIndicatorView;
import sg.lifecare.medicare.utils.BleUtil;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;

/**
 * Add A&D device
 */
public class AddAAndDDeviceFragment extends AddDeviceFragment {

    public static AddAAndDDeviceFragment newInstance() {
        return new AddAAndDDeviceFragment();
    }

    private static final String TAG = "AddAAndDDeviceFragment";

    private AAndDMeter mAAndDMeter;

    private PairingIndicatorView pairingIndicator;
    private AlertDialog mBleEnableDialog;
    private static int REQUEST_ENABLE_BT = 251;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            /*if(resultCode==RESULT_OK) {
                startPairingProcess();
            }else{
                Timber.d("CANCELED");
                pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_FAIL_BLE_OFF);
            }*/
        }
    }

    private void startPairingProcess(){
        try {
            if (myActivity.getDevice() == Model.AANDD_UA_651) {
                mAAndDMeter = new AAndD651BloodPressure(getActivity(), mMeterListener, "");
            } else if (myActivity.getDevice() == Model.AANDD_UC_352) {
                mAAndDMeter = new AAndD352WeighScale(getActivity(), mMeterListener, "");
            } else if (myActivity.getDevice() == Model.AANDD_UT_201) {
                mAAndDMeter = new AAndD201Thermometer(getActivity(), mMeterListener, "");
            }
            mAAndDMeter.setMode(AAndDMeter.MODE_PAIRING);
            mAAndDMeter.startScanning();

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
        }
        if (myActivity.getDevice() == Model.AANDD_UA_651) {
            pairingIndicator.setPairingDescription(R.string.blood_pressure_info_pair_device_step);
            pairingIndicator.setPairingDescriptionImage(R.drawable.bp_start);
        } else if (myActivity.getDevice() == Model.AANDD_UC_352) {
            pairingIndicator.setPairingDescription(R.string.weight_info_pair_device_step);
            pairingIndicator.setPairingDescriptionImage(R.drawable.weighscale_start);
        } else if (myActivity.getDevice() == Model.AANDD_UT_201) {
            pairingIndicator.setPairingDescription(R.string.thermo_info_pair_device_step);
            pairingIndicator.setPairingDescriptionImage(R.drawable.thermometer_start);
        }
        pairingIndicator.showPairingDescription();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        //TODO
        //mAAndDMeter.startScanning();

        if(BleUtil.isBleEnabled()) {
            startPairingProcess();
        }else{
            pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_FAIL_BLE_OFF);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mAAndDMeter!=null) {
            mAAndDMeter.disconnect();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mAAndDMeter!=null) {
            mAAndDMeter.stopScanning();
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
            if(status==AAndDMeter.FORCED_CANCEL
                    || status == BluetoothGatt.STATE_DISCONNECTED){
                mCallback.onPairFailed();
            }
        }
    };
}
