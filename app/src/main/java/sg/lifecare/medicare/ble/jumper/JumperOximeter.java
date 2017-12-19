package sg.lifecare.medicare.ble.jumper;

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
import timber.log.Timber;

public class JumperOximeter extends Ble {


    private static final String TAG = "JumperOximeter";

    private static final String DEVICE_NAME = "My Oximeter";

    private static final UUID PROPRIETARY_SERVICE = UUID.fromString("cdeacb80-5235-4c07-8846-93a37ee6b86d");
    private static final UUID NOTIFY_CHARACTERISTIC = UUID.fromString("cdeacb81-5235-4c07-8846-93a37ee6b86d");
    private static final UUID WRITE_CHARACTERISTIC = UUID.fromString("cdeacb82-5235-4c07-8846-93a37ee6b86d");

    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;

    private JumperOximeterListener mListener;
    private WeakReference<Activity> mActivity;

    private BluetoothDevice mBluetoothDevice;

    private boolean mIsPairing = false;

    public JumperOximeter(Activity activity, JumperOximeterListener listener) throws
            BleException {
        super(activity);

        mListener = listener;

        mActivity = new WeakReference<>(activity);
    }

    public void startPairing() {
        mIsPairing = true;
        startScanning();
    }

    public void startReading() {
        mIsPairing = false;
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

            //act.runOnUiThread(new Runnable() {
            //    @Override
            //    public void run() {
            connectDevice(mBluetoothDevice.getAddress());
            //    }
            //});
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
        Log.d(TAG, "onBleServicesDiscovered: status=" + status);

        if (BluetoothGatt.GATT_SUCCESS == status) {

            BluetoothGattService service = gatt.getService(PROPRIETARY_SERVICE);

            if (service != null) {
                mNotifyCharacteristic = service.getCharacteristic(NOTIFY_CHARACTERISTIC);
                mWriteCharacteristic = service.getCharacteristic(WRITE_CHARACTERISTIC);

                queueSetNotificationForCharacteristic(mNotifyCharacteristic,true);
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

        Log.d(TAG,"onBleCharacteristicChanged");

        byte[] data = characteristic.getValue();

        if (data != null) {
            if ((data.length == 11) && (data[0] == (byte) 0x80)) {

            } else if ((data.length == 4) && (data[0] == (byte)0x81)) {
                int pulse = data[1] & 0xff;
                int spo2 = data[2] & 0xff;
                double pi = ((data[3] & 0xff) * 10d) / 100d;

                //mCallbacks.onSpo2PulsePiRead(spo2, pulse, pi);

                Timber.d("spo2: %d, pulse: %d, pi: %.1f", spo2, pulse, pi);

                mListener.onReadResult(spo2, pulse, pi);
            }
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
