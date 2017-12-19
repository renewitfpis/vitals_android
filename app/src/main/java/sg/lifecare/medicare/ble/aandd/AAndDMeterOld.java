package sg.lifecare.medicare.ble.aandd;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.ble.Ble2;
import sg.lifecare.medicare.ble.BleException;
import sg.lifecare.medicare.ble.profile.AbstractProfile;

/**
 * A&D meter
 */
public abstract class AAndDMeterOld extends Ble2 {

    private static final String TAG = "AAndDMeter";

    public static final int MODE_PAIRING = 0;
    public static final int MODE_READING = 1;

    protected int mMode = MODE_READING;
    protected WeakReference<Activity> mActivity;
    private int mSetDateTimeDelay;
    private int mSetIndicationDelay;
    protected List<AbstractProfile> mRecords;
    protected AAndDMeterListener mListener;

    public AAndDMeterOld(Activity activity, AAndDMeterListener listener) throws BleException {
        super(activity);

        mActivity = new WeakReference<>(activity);
        mListener = listener;
    }

    public void setMode(int mode) {
        mMode = mode;
    }

    protected abstract boolean isValidDevice(BluetoothDevice device);
    protected abstract void parseProfile(BluetoothGattCharacteristic cha);
    protected abstract void getProfile(BluetoothGatt gatt);

    @Override
    protected void onBleScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (isValidDevice(device)) {

            mBluetoothDevice = device;

            Log.d(TAG, "onBleScan: bondState=" + getBondState(mBluetoothDevice.getBondState()));

            if (MODE_PAIRING == mMode) {
                if (BluetoothDevice.BOND_NONE == mBluetoothDevice.getBondState()) {
                    stopScanning();
                    connectToDevice();
                } else if (BluetoothDevice.BOND_BONDED == mBluetoothDevice.getBondState()) {
                    if (mListener != null) {
                        mListener.onDeviceBonded(mBluetoothDevice);
                    }
                    stopScanning();
                    connectToDevice();
                }
            } else if ((MODE_READING == mMode) && (BluetoothDevice.BOND_BONDED == mBluetoothDevice.getBondState())) {
                stopScanning();
                connectToDevice();
            } else {
                disconnect();
            }
        }
    }

    private void connectToDevice() {
        mRecords = new ArrayList<>();

        Activity act = mActivity.get();
        if (act != null) {
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectDevice(mBluetoothDevice.getAddress());
                }
            });
        }
    }

    @Override
    protected void onBleConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        Log.d(TAG, "onBleConnectionStateChange: bondState=" + getBondState(mBluetoothDevice.getBondState()));
        Log.d(TAG, "onBleConnectionStateChange: status=" + status);
        Log.d(TAG, "onBleConnectionStateChange: newState=" + newState);
        //setDateTime(gatt);
        if (GATT_ERROR == status) {

            Log.d(TAG, "onBleConnectionStateChange: ERROR");
            if (BluetoothDevice.BOND_NONE == mBluetoothDevice.getBondState()) {
                Activity act = mActivity.get();
                if (act != null) {
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                    act.registerReceiver(mBondingBroadcastReceiver, filter);
                }
            }
        } else if (BluetoothGatt.GATT_SUCCESS == status || status == 19) {
            Log.d(TAG, "onBleConnectionStateChange: SUCCESS");
            Log.d(TAG, "onBleConnectionStateChange: mode " + mMode);
            if (MODE_READING == mMode) {

                Log.d(TAG, "onBleConnectionStateChange: READING");
                if (BluetoothProfile.STATE_DISCONNECTED == newState) {
                    if (mListener != null) {
                        Log.d(TAG, "mListener is not null! <on result>");
                        mListener.onResult(mRecords);
                    } else {
                        Log.d(TAG, "mListener is null!");
                    }
                }
            }
            else{
                if (BluetoothDevice.BOND_NONE == mBluetoothDevice.getBondState()||
                        BluetoothDevice.BOND_BONDING == mBluetoothDevice.getBondState()) {
                    Activity act = mActivity.get();
                    if (act != null) {
                        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                        act.registerReceiver(mBondingBroadcastReceiver, filter);//TODO: unreg
                    }
                }
            }
        }
    }

    @Override
    protected void onBleServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.d(TAG, "onBleServicesDiscovered: bondState=" + getBondState(mBluetoothDevice.getBondState()));

        if (BluetoothGatt.GATT_SUCCESS == status) {
            if ((MODE_PAIRING == mMode) && (BluetoothDevice.BOND_BONDED != mBluetoothDevice.getBondState())){
                // wait for bonded broadcast
                return;
            }

            if (MODE_READING == mMode) {
                Log.d(TAG,"Reading mode, getFirmwareRevision");
                getFirmwareRevision(gatt);
                boolean isReading = gatt.readCharacteristic(gatt.getService(BLOOD_PRESSURE_SERVICE).getCharacteristic
                        (BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC));
                Log.d(TAG,"Reading mode - Is reading = " + isReading);
            } else if (MODE_PAIRING == mMode){
                Log.d(TAG,"Pairing mode, setDateTime");
                //mBluetoothDevice.cancelP(true);
                setDateTime(gatt);
                boolean isReading = gatt.readCharacteristic(gatt.getService(BLOOD_PRESSURE_SERVICE).getCharacteristic
                        (BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC));
                Log.d(TAG,"SetDateTime - Is reading = " + isReading);
            }
        }
    }

    public void unpairDevice(String deviceId) {
        super.unpairDevice(deviceId);
    }

    @Override
    protected void onBleReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

    }

    @Override
    protected void onBleCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.d(TAG,"onBleCharacteristicRead");
        //parseProfile(characteristic);
        if (FIRMWARE_REVISION_CHARACTERISTIC.equals(characteristic.getUuid())) {
            byte[] values = characteristic.getValue();
            Log.d(TAG,"onBleCharacteristicRead2");

            if (values != null) {
                Log.d(TAG,"onBleCharacteristicRead3");

                String firmwareRevision = new String(values);
                if (firmwareRevision != null) {
                    Log.d(TAG,"onBleCharacteristicRead4");

                    Activity act = mActivity.get();
                    if (act != null) {
                        String[] firmRevisionArray = act.getResources().getStringArray(
                                R.array.aandd_firmware_revision_group1);
                        boolean isGroup1 = false;

                        Log.d(TAG, "onBleCharacteristicRead: firmwareRevision=" + firmwareRevision);

                        for (String revision : firmRevisionArray) {
                            if (revision.contains(firmwareRevision)) {
                                isGroup1 = true;
                                break;
                            }
                        }

                        if (isGroup1) {
                            mSetDateTimeDelay = 40;
                            mSetIndicationDelay = 40;
                        } else {
                            mSetDateTimeDelay = 5000;
                            mSetIndicationDelay = 5000;
                        }

                        setDateTime(gatt);
                        return;
                    }
                }
            }

            disconnect();
        }
    }

    @Override
    protected void onBleCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Log.d(TAG,"onBleCharacteristicChanged");
        parseProfile(characteristic);
    }

    @Override
    protected void onBleCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.d(TAG,"onBleCharacteristicWrite");
        if (DATE_TIME_CHARACTERISTIC.equals(characteristic.getUuid())) {
            Log.d(TAG,"onBleCharacteristicWrite - FOUND DATE TIME");
            if (MODE_PAIRING == mMode) {
                if (mListener != null) {
                    mListener.onDeviceBonded(mBluetoothDevice);
                }
                Log.d(TAG,"onBleCharacteristicWrite - PAIRING");

                disconnect();
            } else {
                getProfile(gatt);
                Log.d(TAG,"onBleCharacteristicWrite - READING");
                Log.d(TAG,"called getProfile()");

            }
        } else if (FIRMWARE_REVISION_CHARACTERISTIC.equals(characteristic.getUuid())) {
            setDateTime(gatt);
        }
    }

    @Override
    protected void onBleDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

    }

    protected void setDateTime(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(WEIGHT_SCALE_SERVICE);

        if (service == null) {
            Log.e(TAG, "Could not find WeightScale service");
            disconnect();
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(DATE_TIME_CHARACTERISTIC);

        if (characteristic == null) {
            Log.e(TAG, "Could not find DateTime characteristic");
            disconnect();
            return;
        }
        queueWriteDataToCharacteristic(characteristic, getDateTime());

    }

    protected void getFirmwareRevision(BluetoothGatt gatt) {
        Log.d(TAG,"getFirmwareRevision");
        BluetoothGattService service = gatt.getService(DEVICE_INFORMATION_SERVICE);

        if (service == null) {
            Log.e(TAG, "Could not find DeviceInformation service");
            disconnect();
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(FIRMWARE_REVISION_CHARACTERISTIC);

        if (characteristic == null) {
            Log.e(TAG, "Could not find FirmwareRevision characteristic");
            disconnect();
            return;
        }
        Log.d(TAG,"Found Characteristics!");
        queueReadDataToCharacteristic(characteristic);

    }

    protected byte[] getDateTime() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        Log.d(TAG,"getDateTime() = year : " + year + ", month = " + month + ", day = " + day
        + ", hour = " + hour + ", min = " +minute + ", sec = " + second);

        byte[] dateTime = new byte[7];

        dateTime[0] = (byte)(year & 0xff);
        dateTime[1] = (byte)((year>>8) & 0xff);
        dateTime[2] = (byte)(month & 0xff);
        dateTime[3] = (byte)(day & 0xff);
        dateTime[4] = (byte)(hour & 0xff);
        dateTime[5] = (byte)(minute & 0xff);
        dateTime[6] = (byte)(second & 0xff);

        for(int i = 0 ; i < 7; i++){
            Log.d(TAG,"[BYTE DATA] getDateTime() = " + dateTime[i]);
        }
        return dateTime;

    }

    private BroadcastReceiver mBondingBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
            int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

            Log.d(TAG, "BroadcastReceiver: Bond state changed for device " + device.getAddress() +
                    " new state: " + bondState + " previous:" + previousBondState);

            String name  = device.getName();
            if (!TextUtils.isEmpty(name)) {
                if (BluetoothDevice.BOND_BONDED == bondState) {

                    context.unregisterReceiver(mBondingBroadcastReceiver);
                    //TODO: handle here
                    mBluetoothDevice = device;
                    mListener.onDeviceBonded(device);
                    //connectToDevice();
                }
            }
        }
    };
}
