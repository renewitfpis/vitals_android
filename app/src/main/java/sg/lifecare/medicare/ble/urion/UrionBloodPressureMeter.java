package sg.lifecare.medicare.ble.urion;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.UUID;

import sg.lifecare.medicare.ble.Ble;
import sg.lifecare.medicare.ble.BleException;
import sg.lifecare.medicare.ble.ParserUtils;
import timber.log.Timber;

public class UrionBloodPressureMeter extends Ble {

    private static final String TAG = "UrionBloodPressureMeter";

    private static final String DEVICE_NAME = "Bluetooth BP";

    private static final UUID PROPRIETARY_SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static final UUID NOTIFY_CHARACTERISTIC = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    private static final UUID WRITE_CHARACTERISTIC = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");

    private static final int MSG_WRITE_CHECK = 1;

    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;

    private UrionBloodPressureMeterListener mListener;
    private WeakReference<Activity> mActivity;

    private byte[] START_CMD =  new byte[] {(byte)0xfd, (byte)0xfd, (byte)0xfa, (byte)0x05, (byte)0x0d, (byte)0x0a};

    private boolean mIsPairing = false;

    public UrionBloodPressureMeter(Activity activity, UrionBloodPressureMeterListener listener) throws
            BleException {
        super(activity);

        mListener = listener;

        mActivity = new WeakReference<>(activity);
    }

    public void startPairing() {
        mIsPairing = true;
        startScanning();
    }

    @Override
    protected void onBleScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (DEVICE_NAME.equalsIgnoreCase(device.getName())) {

            if (mIsPairing) {
                mIsPairing = false;

                stopScanning();
            }

            mListener.onDeviceScan(device);
        }
    }

    public void connectBluetoothDevice(BluetoothDevice device) {

        Activity act = mActivity.get();
        if (act != null) {
            mBluetoothDevice = device;

            connectDevice(mBluetoothDevice.getAddress());
        }
    }

    @Override
    protected void onBleConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        Log.d(TAG, "onBleConnectionStateChange: bondState=" + getBondState(mBluetoothDevice.getBondState()));
        Log.d(TAG, "onBleConnectionStateChange: status=" + status);
        Log.d(TAG, "onBleConnectionStateChange: newState=" + newState);

        mListener.onConnectionStateChanged(newState);
    }

    @Override
    protected void onBleServicesDiscovered(BluetoothGatt gatt, int status) {
        if (BluetoothGatt.GATT_SUCCESS == status) {

            BluetoothGattService service = gatt.getService(PROPRIETARY_SERVICE);

            if (service != null) {
                mNotifyCharacteristic = service.getCharacteristic(NOTIFY_CHARACTERISTIC);
                mWriteCharacteristic = service.getCharacteristic(WRITE_CHARACTERISTIC);

                queueSetNotificationForCharacteristic(mNotifyCharacteristic,true);
                queueWriteDataToCharacteristic(mWriteCharacteristic, START_CMD);
            }
        }
    }

    @Override
    protected void onBleReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

    }

    @Override
    protected void onBleCharacteristicRead(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic, int status) {

    }

    @Override
    protected void onBleCharacteristicChanged(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic) {

        final BluetoothGattDescriptor cccd = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
        final boolean notifications = cccd == null || cccd.getValue() == null || cccd.getValue().length != 2 || cccd.getValue()[0] == 0x01;

        if (notifications) {
            Timber.i( "Notification received from " + characteristic.getUuid() + ", value: " + ParserUtils.parse(characteristic));

            byte[] data = characteristic.getValue();

            if (data != null) {
                if ((data.length == 1) && (data[0] == (byte)0xA5)) {
                    //mWriteCharacteristic.setValue(START_CMD);
                    //writeCharacteristic(mWriteCharacteristic);
                    //enqueue(Request.newWriteRequest(mWriteCharacteristic, START_CMD));
                    queueWriteDataToCharacteristic(mWriteCharacteristic, START_CMD);
                } else if (data.length == 5) {
                    if (data[2] == (byte)0x06) {
                        //mCallbacks.onStartMeasure();
                    }
                } else if (data.length == 7) {
                    if (data[2] == (byte)0xfb) {
                        int pulse = (data[3] & 0xff) * 256 + (data[4] & 0xff);
                        mListener.onReadPulse(pulse);
                    }
                } else if (data.length == 8) {
                    if (data[2] == (byte)0xfc) {
                        int sys = (data[3] & 0xff);
                        int dia = (data[4] & 0xff);
                        int pulse = (data[5] & 0xff);

                        Timber.d("systolic: %d, diastolic: %d, pulse: %d", sys, dia, pulse);

                        mListener.onReadResult(sys, dia, pulse);
                    }
                }
            }


        } else { // indications
            Timber.i( "Indication received from " + characteristic.getUuid() + ", value: " + ParserUtils.parse(characteristic));
        }


    }

    @Override
    protected void onBleCharacteristicWrite(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic, int status) {

    }

    @Override
    protected void onBleDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
            int status) {

    }

    @Override
    protected void onBleStopped() {

    }

    @Override
    protected void startCountDown() {

    }

    @Override
    protected void stopCountDown() {

    }
}
