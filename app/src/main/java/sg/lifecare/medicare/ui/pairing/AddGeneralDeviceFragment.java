package sg.lifecare.medicare.ui.pairing;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.util.Date;
import java.util.List;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.ble.Ble;
import sg.lifecare.medicare.ble.BleException;
import sg.lifecare.medicare.ble.DeviceMeter;
import sg.lifecare.medicare.ble.aandd.AAndDMeter;
import sg.lifecare.medicare.ble.aandd.AAndDMeterListener;
import sg.lifecare.medicare.ble.jumper.JumperOximeter;
import sg.lifecare.medicare.ble.jumper.JumperOximeterListener;
import sg.lifecare.medicare.ble.jumper.JumperThermometer;
import sg.lifecare.medicare.ble.jumper.JumperThermometerListener;
import sg.lifecare.medicare.ble.profile.AbstractProfile;
import sg.lifecare.medicare.ble.qn.YolandaWeightMeter;
import sg.lifecare.medicare.ble.qn.YolandaWeightMeterListener;
import sg.lifecare.medicare.ble.urion.UrionBloodPressureMeter;
import sg.lifecare.medicare.ble.urion.UrionBloodPressureMeterListener;
import sg.lifecare.medicare.ble.vivachek.VivaChekBloodGlucoseMeter;
import sg.lifecare.medicare.ble.vivachek.VivaChekBloodGlucoseMeterListener;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.ui.view.PairingIndicatorView;
import sg.lifecare.medicare.utils.BleUtil;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static sg.lifecare.medicare.ui.fragment.OverviewFragment.REQUEST_ENABLE_BT;

import com.kitnew.ble.QNBleDevice;

public class AddGeneralDeviceFragment extends AddDeviceFragment {

    public static AddGeneralDeviceFragment newInstance() {
        return new AddGeneralDeviceFragment();
    }

    private static final String TAG = "AddGenDeviceFragment";

    private DeviceMeter deviceMeter;

    private JumperThermometer mJumperThermometer;
    private JumperOximeter mJumperOximeter;
    private UrionBloodPressureMeter mUrionBloodPressureMeter;
    private YolandaWeightMeter mYolandaWeightMeter;
    private VivaChekBloodGlucoseMeter mVivaChekBloodGlucoseMeter;

    private PairingIndicatorView pairingIndicator;
    private AlertDialog mBleEnableDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Timber.d("ON CREAATE");
        mBleEnableDialog = BleUtil.enableBleDialogBuilder(getActivity(),
                mBleSettingsButtonListener, mBleCancelButtonListener).create();

        /*if(BleUtil.isBleEnabled()) {
            startPairingProcess();
        }else{
            mBleEnableDialog.show();
        }*/
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

    private void startPairingProcess(){
        Timber.d("START PAIRING");
        try {
            if (myActivity.getDevice() == Model.NONIN_3230) {
                deviceMeter = new DeviceMeter(getActivity(), mMeterListener, myActivity.getDevice());//myActivity.getPairedDeviceId()
                deviceMeter.setMode(AAndDMeter.MODE_PAIRING);
                deviceMeter.startScanning();
            } else if (myActivity.getDevice() == Model.BERRY_BM1000B) {
                deviceMeter = new DeviceMeter(getActivity(), mMeterListener, myActivity.getDevice());//,myActivity.getPairedDeviceId()
                deviceMeter.setMode(AAndDMeter.MODE_PAIRING);
                deviceMeter.startScanning();
            } else if (myActivity.getDevice() == Model.ZENCRO_X6) {
                deviceMeter = new DeviceMeter(getActivity(), mMeterListener, myActivity.getDevice());//,myActivity.getPairedDeviceId()
                deviceMeter.setMode(AAndDMeter.MODE_PAIRING);
                deviceMeter.startScanning();
            } else if (myActivity.getDevice() == Model.JUMPER_FR302) {
                mJumperThermometer = new JumperThermometer(getActivity(), mJumperThermometerListener);
                mJumperThermometer.startPairing();
            } else if (myActivity.getDevice() == Model.JUMPER_JPD500E) {
                mJumperOximeter = new JumperOximeter(getActivity(), mJumperOximeterListener);
                mJumperOximeter.startPairing();
            } else if (myActivity.getDevice() == Model.URION_BP_U80E) {
                mUrionBloodPressureMeter = new UrionBloodPressureMeter(getActivity(), mUrionBloodPressureMeterListener);
                mUrionBloodPressureMeter.startPairing();
            } else if (myActivity.getDevice() == Model.YOLANDA_LITE) {
                mYolandaWeightMeter = new YolandaWeightMeter(getActivity(), mYolandaWeightMeterListener);
                mYolandaWeightMeter.startScanning();
            } else if (myActivity.getDevice() == Model.VIVACHEK_INO_SMART) {
                mVivaChekBloodGlucoseMeter = new VivaChekBloodGlucoseMeter(getActivity(), mVivaChekBloodGlucoseMeterListener);
                mVivaChekBloodGlucoseMeter.startScanning();
            }

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

       if (myActivity.getDevice() == Model.ZENCRO_X6){
            pairingIndicator.setPairingDescription(R.string.zencro_x6_info_pair_device_step);
            pairingIndicator.setPairingDescriptionImage(R.drawable.x6_start);
        } else if (myActivity.getDevice() == Model.BERRY_BM1000B){
            pairingIndicator.setPairingDescription(R.string.berry_oximeter_info_pair_device_step);
           pairingIndicator.setPairingDescriptionImage(R.drawable.ic_pairing_berry1);
        } else if (myActivity.getDevice() == Model.NONIN_3230){
            pairingIndicator.setPairingDescription(R.string.nonin_oximeter_info_pair_device_step);
           pairingIndicator.setPairingDescriptionImage(R.drawable.ic_pairing_nonin);
        } else if (myActivity.getDevice() == Model.JUMPER_FR302) {
           pairingIndicator.setPairingDescription(R.string.jumper_fr302_instruction);
           pairingIndicator.setPairingDescriptionImage(R.drawable.ic_jumper_fr302);
       } else if (myActivity.getDevice() == Model.JUMPER_JPD500E) {
           pairingIndicator.setPairingDescription(R.string.jumper_jpd500e_instruction);
           pairingIndicator.setPairingDescriptionImage(R.drawable.ic_jumper_jpd500e);
       } else if (myActivity.getDevice() == Model.URION_BP_U80E) {
           pairingIndicator.setPairingDescription(R.string.jumper_jpd500e_instruction);
           pairingIndicator.setPairingDescriptionImage(R.drawable.ic_urion_u80e);
       } else if (myActivity.getDevice() == Model.YOLANDA_LITE) {
           pairingIndicator.setPairingDescription(R.string.yolanda_lite_instruction);
           pairingIndicator.setPairingDescriptionImage(R.drawable.ic_yolanda);
       } else if (myActivity.getDevice() == Model.VIVACHEK_INO_SMART) {
           pairingIndicator.setPairingDescription(R.string.vivachek_ino_smart_instruction);
           pairingIndicator.setPairingDescriptionImage(R.drawable.ic_vivachek_ino_smart);
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

        if (mJumperThermometer != null) {
            mJumperThermometer.stopScanning();
        }

        if (mJumperOximeter != null) {
            mJumperOximeter.stopScanning();
        }

        if (mUrionBloodPressureMeter != null) {
            mUrionBloodPressureMeter.stopScanning();
        }

        if (mYolandaWeightMeter != null) {
            mYolandaWeightMeter.stopScanning();
        }

        if (mVivaChekBloodGlucoseMeter != null) {
            mVivaChekBloodGlucoseMeter.stopScanning();
        }
    }

    private JumperThermometerListener mJumperThermometerListener = new JumperThermometerListener() {
        @Override
        public void onDeviceScan(final BluetoothDevice device) {
            myActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"DeviceId = " + device.getAddress());

                    mCallback.onPairSuccess(device.getAddress());
                }
            });
        }

        @Override
        public void onReadResult(double temperature) {

        }

        @Override
        public void onConnectionStateChanged(int status) {
            if(status == BluetoothGatt.STATE_DISCONNECTED){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_FAIL_TIMEOUT);

                    }
                });
            }
        }
    };

    private JumperOximeterListener mJumperOximeterListener = new JumperOximeterListener() {
        @Override
        public void onDeviceScan(final BluetoothDevice device) {
            myActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"DeviceId = " + device.getAddress());

                    mCallback.onPairSuccess(device.getAddress());
                }
            });
        }

        @Override
        public void onReadResult(int sp02, int pulse, double pi) {

        }

        @Override
        public void onConnectionStateChanged(int status) {
            if(status == BluetoothGatt.STATE_DISCONNECTED){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_FAIL_TIMEOUT);

                    }
                });
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
            if(status == BluetoothGatt.STATE_DISCONNECTED
                    ||status== Ble.FORCED_CANCEL){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_FAIL_TIMEOUT);

                    }
                });
            }
        }

    };

    private UrionBloodPressureMeterListener mUrionBloodPressureMeterListener = new UrionBloodPressureMeterListener() {


        @Override
        public void onDeviceScan(final BluetoothDevice device) {
            myActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"DeviceId = " + device.getAddress());

                    mCallback.onPairSuccess(device.getAddress());
                }
            });
        }

        @Override
        public void onReadResult(int systolic, int diastopic, int pulse) {

        }

        @Override
        public void onReadPulse(int pulse) {

        }

        @Override
        public void onConnectionStateChanged(int status) {
            if(status == BluetoothGatt.STATE_DISCONNECTED){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_FAIL_TIMEOUT);

                    }
                });
            }
        }
    };

    private YolandaWeightMeterListener mYolandaWeightMeterListener =  new YolandaWeightMeterListener() {


        @Override
        public void onDeviceScan(final QNBleDevice device) {
            myActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"DeviceId = " + device.getMac());

                    mCallback.onPairSuccess(device.getMac());
                }
            });
        }

        @Override
        public void onReadResult(double weigh) {

        }

        @Override
        public void onConnectionStateChanged(int status) {
            if(status == BluetoothGatt.STATE_DISCONNECTED){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_FAIL_TIMEOUT);

                    }
                });
            }
        }
    };

    private VivaChekBloodGlucoseMeterListener mVivaChekBloodGlucoseMeterListener = new VivaChekBloodGlucoseMeterListener() {

        @Override
        public void onDeviceScan(final BluetoothDevice device) {
            myActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"DeviceId = " + device.getAddress());

                    mCallback.onPairSuccess(device.getAddress());
                }
            });
        }

        @Override
        public void onReadResult(List<VivaChekBloodGlucoseMeter.CustomerHistory> histories) {

        }

        @Override
        public void onConnectionStateChanged(int status) {
            if(status == BluetoothGatt.STATE_DISCONNECTED){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_FAIL_TIMEOUT);

                    }
                });
            }
        }
    };
}
