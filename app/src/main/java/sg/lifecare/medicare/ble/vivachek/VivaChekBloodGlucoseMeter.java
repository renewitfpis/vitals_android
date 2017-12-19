package sg.lifecare.medicare.ble.vivachek;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import sg.lifecare.medicare.ble.Ble;
import sg.lifecare.medicare.ble.BleException;
import timber.log.Timber;

public class VivaChekBloodGlucoseMeter extends Ble {

    private static final String DEVICE_NAME = "BLE-Vivachek";

    private static final UUID SERVICE_1 = UUID.fromString("0003cdd0-0000-1000-8000-00805f9b0131");
    private static final UUID CHARACTERISTIC_1_NOTIFY = UUID.fromString("0003cdd1-0000-1000-8000-00805f9b0131");
    private static final UUID CHARACTERISTIC_1_WRITE = UUID.fromString("0003cdd2-0000-1000-8000-00805f9b0131");

    private static final UUID SERVICE_2 = UUID.fromString("0000fee7-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_2_WRITE = UUID.fromString("0000fec7-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_2_INDICATE = UUID.fromString("0000fec8-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_2_READ = UUID.fromString("0000fec9-0000-1000-8000-00805f9b34fb");

    private static final byte BEGIN = (byte)0x7b;
    private static final byte END = (byte)0x7d;
    private static final byte TARGET_CODE = (byte)0x20;
    private static final byte SOURCE_CODE = (byte)0x10;
    private static final byte COMMAND_READ_SERIAL_NUMBER = (byte)0x77;
    private static final byte COMMAND_READ_UNIT = (byte)0xaa;
    private static final byte COMMAND_TEST_SYNCHRONIZATION = (byte)0x12;
    private static final byte COMMAND_TURN_OFF_BLUETOOTH = (byte)0xd2;
    private static final byte COMMAND_READ_CUSTOMER_HISTORY = (byte)0xdd;
    private static final byte COMMAND_HISTORY_DONE = (byte)0xd1;
    private static final byte COMMAND_READ_SOFTWARE_VERSION = (byte)0x66;
    private static final byte COMMAND_DELETE_HISTORY = (byte)0x55;
    private static final byte COMMAND_DELETE_ONE_HHISTORY = (byte)0x56;
    private static final byte COMMAND_READ_SEVERAL_HISTORY = (byte)0x13;
    private static final byte COMMAND_TIME_SET_AND_READ = 0x44;
    private static final byte EXTENDED_CODE_READ = (byte)0x55;
    private static final byte EXTENDED_CODE_WRITE = (byte)0x66;
    private static final byte EXTENDED_CODE_READ_ANSWER = (byte)0xaa;
    private static final byte EXTENDED_CODE_WRITE_ANSWER = (byte)0x99;

    private static final int COMMAND_INDEX = 5;
    private static final int COMMAND_EXTENDED_CODE_INDEX = 6;
    private static final int DATA_SIZE_INDEX = 7;
    private static final int DATA_INDEX = 9;

    private static final byte UNIT_MG_DL = (byte) 0x11;
    private static final byte UNIT_MMOL_L = (byte) 0x22;

    private static final byte MEAL_NONE = (byte) 0x00;
    private static final byte MEAL_FPG = (byte) 0x11;
    private static final byte MEAL_PPG = (byte) 0x22;

    private static final byte SAMPLE_BLOOD = (byte) 0x11;
    private static final byte SAMPLE_CONTROL = (byte) 0x22;


    private static final byte[] READ_ONE_HISTORY_COMMAND = {
            BEGIN,
            0x01, TARGET_CODE, 0x01, SOURCE_CODE,
            COMMAND_READ_SEVERAL_HISTORY, EXTENDED_CODE_READ,
            0x00, 0x01,
            0x01,
            0x04, 0x03, 0x07, 0x07,
            END
    };

    private static final int DATA_LEN = 100;
    private byte[] mTempData = new byte[DATA_LEN];
    private int mTempDataLength = 0;
    private boolean mAppendHistory = false;

    private List<CustomerHistory> mCustomerHistories = new ArrayList<>();

    private BluetoothGattCharacteristic mCharacteristic1Notify;
    private BluetoothGattCharacteristic mCharacteristic1Write;

    private VivaChekBloodGlucoseMeterListener mListener;
    private WeakReference<Activity> mActivity;

    private boolean mIsPairing = false;


    public VivaChekBloodGlucoseMeter(Activity activity, VivaChekBloodGlucoseMeterListener listener) throws BleException {
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

    @Override
    protected void onBleConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        mListener.onConnectionStateChanged(status);
    }

    @Override
    protected void onBleServicesDiscovered(BluetoothGatt gatt, int status) {

        if (BluetoothGatt.GATT_SUCCESS == status) {
            BluetoothGattService service1 = gatt.getService(SERVICE_1);

            if (service1 != null) {

                mCharacteristic1Notify = service1.getCharacteristic(CHARACTERISTIC_1_NOTIFY);
                mCharacteristic1Write = service1.getCharacteristic(CHARACTERISTIC_1_WRITE);

                queueSetNotificationForCharacteristic(mCharacteristic1Notify,true);
                queueWriteDataToCharacteristic(mCharacteristic1Write, READ_ONE_HISTORY_COMMAND);
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

        byte[] data = characteristic.getValue();

        if (data.length == 0) {
            return;
        }

        if (data[0] == BEGIN) {
            switch (data[COMMAND_INDEX]) {

                case COMMAND_READ_SEVERAL_HISTORY:
                    Timber.d("COMMAND_READ_SEVERAL_HISTORY");

                    mCustomerHistories.clear();
                    mAppendHistory = true;

                    clearTempData();
                    appendTempData(data);
                    break;

                case COMMAND_HISTORY_DONE:
                    Timber.d("COMMAND_HISTORY_DONE");

                    clearTempData();
                    mAppendHistory = false;
                    break;
            }
        } else {
            if (mAppendHistory) {
                appendTempData(data);
                processCustomerHistory();
            }
        }

    }

    private void appendTempData(byte[] data) {
        System.arraycopy(data, 0, mTempData, mTempDataLength, data.length);
        mTempDataLength += data.length;
    }

    private void clearTempData() {
        Arrays.fill(mTempData, (byte)0x00);
        mTempDataLength = 0;
    }

    private void processCustomerHistory() {
        Timber.d("processCustomerHistory: data_length=%d", mTempDataLength);
        if (mTempDataLength >= 23) {
            byte[] temp = new byte[23];
            System.arraycopy(mTempData, 0, temp, 0, 23);

            CustomerHistory history = getCustomerHistory(temp);
            Timber.d("processCustomerHistory: %s", history.toString());
            mCustomerHistories.add(history);

            System.arraycopy(mTempData, 23, mTempData, 0, mTempDataLength - 23);

            mTempDataLength -= 23;

            if (mTempDataLength == 14 && mTempData[COMMAND_INDEX] == COMMAND_HISTORY_DONE) {
                mAppendHistory = false;
                clearTempData();

                mListener.onReadResult(mCustomerHistories);
            }
        }
    }

    private CustomerHistory getCustomerHistory(byte[] data) {
        CustomerHistory customerHistory = new CustomerHistory();

        int year = (0x00ff & data[DATA_INDEX]) + 2000;
        int month = (0x00ff & data[DATA_INDEX + 1]);
        int day = (0x00ff & data[DATA_INDEX + 2]);
        int hour = (0x00ff & data[DATA_INDEX + 3]);
        int minute = (0x00ff & data[DATA_INDEX + 4]);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);

        customerHistory.mTimestamp = cal.getTime();
        customerHistory.mResult = (((0x00ff & data[DATA_INDEX + 5]) * 100) + (0x00ff & data[DATA_INDEX + 6])) / 10f;

        //trick to round up to 2 decimal places
        customerHistory.mResult =  customerHistory.mResult*100f;
        customerHistory.mResult = Math.round( customerHistory.mResult);
        customerHistory.mResult =  customerHistory.mResult/100f;

        customerHistory.mMeal = data[DATA_INDEX + 7];
        customerHistory.mSample = data[DATA_INDEX + 8];

        return customerHistory;
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

    public class CustomerHistory {
        private Date mTimestamp;
        private float mResult;
        private byte mMeal;
        private byte mSample;

        public Date getTimestamp() {
            return mTimestamp;
        }

        public float getResult() {
            return mResult;
        }

        public byte getMeal() {
            return mMeal;
        }

        public byte getSample() {
            return mSample;
        }

        public boolean isBeforeMeal() {
            return mMeal == MEAL_FPG;
        }

        public boolean isAfterMeal() {
            return mMeal == MEAL_PPG;
        }

        @Override
        public String toString() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm", Locale.getDefault());

            return String.format(Locale.getDefault(), "Date: %s, Result: %.1f, Meal=%x, Sample: %x",
                    dateFormat.format(mTimestamp), mResult, mMeal, mSample);
        }
    }
}
