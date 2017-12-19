package sg.lifecare.medicare.ble;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.ble.aandd.AAndDMeterListener;
import sg.lifecare.medicare.ble.profile.AbstractProfile;
import sg.lifecare.medicare.database.model.SpO2;
import sg.lifecare.medicare.utils.DevDecode_X6;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import timber.log.Timber;

/**
 * General device meter
 */
public class DeviceMeter extends Ble implements ServiceConnection {

    private static final String TAG = "DeviceMeter";

    public UUID SERVICE, CHARACTERISTIC;
    public static final int MODE_PAIRING = 0;
    public static final int MODE_READING = 1;
    public static final int TYPE_SPO2 = 0;
    public static final int TYPE_STEP = 1;
    public static final int PAIRING_MODE_RANDOM = 0;
    public static final int PAIRING_MODE_ID = 1;

    private Model deviceModel;
    private String deviceId;
    private ArrayList<String> deviceIds = null;
    private int currSpO2, currPulseRate;
    protected int mMode = MODE_READING;
    protected WeakReference<Activity> mActivity;
    private int mSetDateTimeDelay;
    private int mSetIndicationDelay;
    protected List<AbstractProfile> mRecords;
    protected AAndDMeterListener mListener;
    private CountDownTimer countDownTimer;
    private int type;
    private int pairingMode = 0;

    public DeviceMeter(Activity activity, AAndDMeterListener listener, Model deviceModel, String deviceId) throws BleException {
        super(activity);

        mActivity = new WeakReference<>(activity);
        mListener = listener;
        this.deviceModel = deviceModel;
        this.deviceId = deviceId;
        this.pairingMode = PAIRING_MODE_ID;
        initService();
    }

    public DeviceMeter(Activity activity, AAndDMeterListener listener, Model deviceModel, ArrayList<String> deviceIds) throws BleException {
        super(activity);

        mActivity = new WeakReference<>(activity);
        mListener = listener;
        this.deviceModel = deviceModel;
        this.deviceIds = deviceIds;
        this.pairingMode = PAIRING_MODE_ID;
        initService();
    }

    public DeviceMeter(Activity activity, AAndDMeterListener listener, Model deviceModel) throws BleException {
        super(activity);

        mActivity = new WeakReference<>(activity);
        mListener = listener;
        this.deviceModel = deviceModel;
        this.deviceId = "";
        this.pairingMode = PAIRING_MODE_RANDOM;
        initService();
    }

    public DeviceMeter(Activity activity, AAndDMeterListener listener, int type) throws BleException {
        super(activity);

        mActivity = new WeakReference<>(activity);
        mListener = listener;
        this.deviceModel = null;
        this.deviceId = null;
        this.pairingMode = PAIRING_MODE_RANDOM;
        this.type = type;
    }

    public void initService(){
        Timber.d("Init service = " + deviceModel.name());
        if(deviceModel==Model.BERRY_BM1000B){
            SERVICE = BERRY_MED_SPO2_SERVICE;
            CHARACTERISTIC = BERRY_MED_SPO2_CHARACTERISTIC;
        }else if(deviceModel==Model.NONIN_3230){
            SERVICE = NONIN_SPO2_SERVICE;
            CHARACTERISTIC = NONIN_SPO2_CHARACTERISTIC;
        }else if(deviceModel==Model.ZENCRO_X6){
            SERVICE = ZENCRO_STEPS_SERVICE;
            CHARACTERISTIC = ZENCRO_STEPS_CHARACTERISTIC;
        }
    }

    public void setMode(int mode) {
        mMode = mode;
    }

    public void clear() {
        if(mBluetoothGatt != null && deviceModel != null
                && SERVICE != null && CHARACTERISTIC != null) {
            /*setNotificationForCharacteristic(mBluetoothGatt.getService(SERVICE).getCharacteristic
                    (CHARACTERISTIC), false);*/
            BluetoothGattService service = mBluetoothGatt.getService(SERVICE);
            if(service!=null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC);

                if (characteristic != null) {
                    queueSetNotificationForCharacteristic(characteristic, false);
                }

                deviceModel = null;
                SERVICE = null;
                CHARACTERISTIC = null;
            }
        }
    }

    public boolean isValidDevice(BluetoothDevice device){
        if(pairingMode == PAIRING_MODE_ID){
            if (deviceIds != null && deviceIds.size()>0){
                String address = device.getAddress();

                Timber.e("Address found : " + address);

                if (!TextUtils.isEmpty(address)) {
                    for (int i = 0; i < deviceIds.size(); i++) {
                        if (address.equalsIgnoreCase(deviceIds.get(i))) {
                            return true;
                        }
                    }
                }
            }
            else if (deviceId != null && !deviceId.equalsIgnoreCase("")) {
                String address = device.getAddress();

                Timber.e("Address found : " + address);
                Timber.e("Paired Device : " + deviceId);

                if (!TextUtils.isEmpty(address)) {
                    if (address.equalsIgnoreCase(deviceId)) {
                        return true;
                    }
                }
            }
        }else if(pairingMode == PAIRING_MODE_RANDOM){
            if(deviceModel!=null) {
                String nameToPair = MedicalDevice.getDeviceNameByModel(deviceModel);
                String name = device.getName();

                if (!TextUtils.isEmpty(name)) {

                    if (name.startsWith(nameToPair)) {
                        return true;
                    }
                }
            }else{
                String name = device.getName();

                if (!TextUtils.isEmpty(name)) {

                    if(type==TYPE_SPO2) {
                        if (name.startsWith("Berry")) {
                            deviceModel = Model.BERRY_BM1000B;
                            initService();
                            return true;
                        } else if (name.startsWith("Nonin")) {
                            deviceModel = Model.NONIN_3230;
                            initService();
                            return true;
                        }
                    }else if(type==TYPE_STEP){
                        if (name.startsWith("X6"))
                        {
                            deviceModel = Model.ZENCRO_X6;
                            initService();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void parseProfile(BluetoothGattCharacteristic cha){
        Log.d(TAG,"Parse profile");

        if(deviceModel==Model.BERRY_BM1000B||deviceModel==Model.NONIN_3230) {

            SpO2 spo2 = new SpO2();
            int value = 127, pulseRate = 255;
            if (deviceModel == Model.BERRY_BM1000B) {
                if (cha != null && cha.getValue().length == 5) {
                    value = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 4);
                    pulseRate = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 3)
                            | ((cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 2) & 0x40) << 1);
                    //int pi = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) & 0x0f;
                    Log.d(TAG, "11111 SPO2 = " + value + " , PulseRate = " + pulseRate);
                }else{
                    return;
                }
            } else if (deviceModel == Model.NONIN_3230) {
                if (cha != null && cha.getValue().length >= 10) {
                    int isValid = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 3);
                   // if(isValid==1) {
                        value = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 7);
                        pulseRate = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 9);
                    //}
                }
            }

            if (value != 127 && value != 0 && (value != currSpO2 || pulseRate != currPulseRate)) {
                currSpO2 = value;
                currPulseRate = pulseRate;
                spo2.setValue(value);
                spo2.setPulseRate(pulseRate);
                spo2.setDate(Calendar.getInstance().getTime());
                mListener.onDataRetrieved(spo2);
                Log.d(TAG, "SPO2 = " + value + " , PulseRate = " + pulseRate);
            }else if(value == 127){
                mListener.onInvalidDataReturned();
            }
        }else if(deviceModel == Model.ZENCRO_X6){
            Log.d(TAG, "X6!!");
            DevDecode_X6 decodeX6 = new DevDecode_X6();
            int parsedSteps = decodeX6.decode_CurrentValue_Auto(cha.getValue());
            if(parsedSteps != 0){
                Log.d(TAG, "X6 STEPS = " + parsedSteps);
                Integer steps = parsedSteps;
                mListener.onDataRetrieved(steps);
            }
        }

    }
    public void getProfile(BluetoothGatt gatt){

    }



    @Override
    protected void onBleScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (isValidDevice(device)) {

            mBluetoothDevice = device;

            Log.d(TAG, "onBleScan: bondState=" + getBondState(mBluetoothDevice.getBondState()));

            if (MODE_PAIRING == mMode) {
                if (BluetoothDevice.BOND_NONE == mBluetoothDevice.getBondState()) {
                    stopScanning();
                    Log.d(TAG, "isConnected() = " + isConnected());

                    if(!isConnected()) {
                        connectToDevice();
                        Log.d(TAG, "connecting to device");

                    }
                } else if (BluetoothDevice.BOND_BONDED == mBluetoothDevice.getBondState()) {
                    /*if (mListener != null) {
                        mListener.onDeviceBonded(mBluetoothDevice);
                    }*/

                    stopScanning();

                    connectToDevice();
                }
            } else if ((MODE_READING == mMode)) { //&& (BluetoothDevice.BOND_BONDED == mBluetoothDevice.getBondState())
                stopScanning();
                connectToDevice();
            } else {
                //disconnect();
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
        if(mBluetoothDevice.getName().contains("Berry")){

            mListener.onConnectionStateChanged(newState);
            Log.d(TAG,"Found berry! discovering services");
            if(mBluetoothGatt.STATE_DISCONNECTED == newState){
                //DO NOTHING todo
                //startScanning();
            }else {
                //mBluetoothGatt.discoverServices();
                BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                        CHARACTERISTIC, BluetoothGattCharacteristic.PERMISSION_READ, BluetoothGattCharacteristic.PROPERTY_READ);
                //mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                queueSetNotificationForCharacteristic(characteristic,true);
            }
            return;
        }
        //setDateTime(gatt);
        if (GATT_ERROR == status) {

            Log.d(TAG, "onBleConnectionStateChange: ERROR");
            if (BluetoothDevice.BOND_NONE == mBluetoothDevice.getBondState()) {
                Log.d(TAG, "onBleConnectionStateChange: BOND NONE");

               /* Activity act = mActivity.get();
                if (act != null) {
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                    act.registerReceiver(mBondingBroadcastReceiver, filter);
                }*//* Activity act = mActivity.get();
                if (act != null) {
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                    act.registerReceiver(mBondingBroadcastReceiver, filter);
                }*/
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
                        //TODO: added on 7/9
                         //startScanning();
                    }
                }
            }
            else{
                if (BluetoothDevice.BOND_NONE == mBluetoothDevice.getBondState()||
                        BluetoothDevice.BOND_BONDING == mBluetoothDevice.getBondState()) {
                   /* Activity act = mActivity.get();
                    if (act != null) {
                        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                        act.registerReceiver(mBondingBroadcastReceiver, filter);//TODO: unreg
                    }*/
                }else{

                }
            }
        }

        //TODO return new state!!

        Log.d(TAG, "Returning new state");
        mListener.onConnectionStateChanged(newState);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.v(TAG, "Connected Service: " + name);

    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    @Override
    protected void onBleServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.d(TAG, "onBleServicesDiscovered: bondState=" + getBondState(mBluetoothDevice.getBondState()));

        for(int i = 0; i < gatt.getServices().size(); i++){
            Log.e(TAG, "Found Service " + i + " : " + gatt.getServices().get(i).getUuid());

        }
        if (MODE_READING == mMode) {
             mListener.onDeviceBonded(gatt.getDevice());
              /*setNotificationForCharacteristic(gatt.getService(SERVICE).getCharacteristic
                    (CHARACTERISTIC), true);*/
            if(mBluetoothGatt.getService(SERVICE)!=null) {
                BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(SERVICE).getCharacteristic
                        (CHARACTERISTIC);
                queueSetNotificationForCharacteristic(characteristic, true);
                stopScanning();

            }
            if(deviceModel==Model.ZENCRO_X6) {
                queueReadDataToCharacteristic(gatt.getService(ZENCRO_STEPS_SERVICE)
                        .getCharacteristic(ZENCRO_STEPS_CHARACTERISTIC));
                stopScanning();

            }
        }else{
            stopScanning();
            Timber.d("BONDED1");
            mListener.onDeviceBonded(gatt.getDevice());
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            return;
        }

        if (BluetoothGatt.GATT_SUCCESS == status) {
            if ((MODE_PAIRING == mMode) && (BluetoothDevice.BOND_BONDED != mBluetoothDevice.getBondState())){
                // wait for bonded broadcast
                return;
            }

            if (MODE_READING == mMode) {
                Log.d(TAG,"Reading mode, getFirmwareRevision");
                getFirmwareRevision(gatt);
                setDateTime(gatt);
                setNotificationForCharacteristic(gatt.getService(SERVICE).getCharacteristic
                        (CHARACTERISTIC),true);
            } else if (MODE_PAIRING == mMode){
                Log.d(TAG,"Pairing mode, setDateTime");
                //mBluetoothDevice.cancelP(true);
                //getFirmwareRevision(gatt);
                //setDateTime(gatt);
                //setNotificationForCharacteristic(gatt.getService(SERVICE).getCharacteristic
                  //      (CHARACTERISTIC),true);
                mListener.onDeviceBonded(gatt.getDevice());
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

           // disconnect();
        } else if (characteristic.getUuid().equals(UUID.fromString("0aad7ea0-0d60-11e2-8e3c-0002a5d5c51b"))){
            Log.d(TAG,"onBleCharacteristicRead - found oximeter char");

            parseProfile(characteristic);
        } else if(characteristic.getUuid().equals(ZENCRO_STEPS_CHARACTERISTIC)){
            Timber.d("On Ble Char Read - found zencro manual");
            DevDecode_X6 devDecodeX6 = new DevDecode_X6();
            int steps = devDecodeX6.decode_CurrentValue_Auto(characteristic.getValue());
            if(steps!=0){
                Timber.d("On Ble Char Read - zencro manual  !=null");
                Timber.d("Val = " + steps);
                mListener.onDataRetrieved((Integer)steps);
            }else{
                Timber.d("On Ble Char Read - zencro manual  ==null");
            }
        }
    }

    @Override
    protected void onBleStopped(){
        if(mBluetoothGatt!=null) {

            BluetoothGattService service = mBluetoothGatt.getService(SERVICE);
            if(service!=null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC);
                if(characteristic!=null) {
                    setNotificationForCharacteristic(characteristic, false);
                }
            }
        }
    }

    @Override
    protected void startCountDown(){
        countDownTimer = new CountDownTimer(60000, 1000) {

            public void onTick(long millisUntilFinished) {
                Timber.d("seconds remaining: " + millisUntilFinished / 1000);
            }

            public void onFinish() {
                mListener.onConnectionStateChanged(Ble.FORCED_CANCEL);
                Timber.d("done!");
            }
        };
        countDownTimer.start();
    }

    @Override
    protected void stopCountDown() {
        if(countDownTimer!=null){
            countDownTimer.cancel();
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
                close();
                //disconnect();
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
        Log.d(TAG,"onBleDescriptorWrite = " + status );
    }

    protected void setDateTime(BluetoothGatt gatt) {
       /* BluetoothGattService service = gatt.getService(SERVICE);

        if (service == null) {
            Log.e(TAG, "Could not find main service");
            //disconnect();
            return;
        }*/
        boolean charFound = false;

        List<BluetoothGattService> serviceList = gatt.getServices();
        Log.e(TAG, "Services size = "+serviceList.size());
        for(int i = 0; i < serviceList.size(); i++) {

            BluetoothGattCharacteristic characteristic =
                    serviceList.get(i).getCharacteristic(DATE_TIME_CHARACTERISTIC);

            if (characteristic != null) {
                queueWriteDataToCharacteristic(characteristic, getDateTime());
                charFound = true;
                Log.e(TAG, "Found and Wrote Date Time!");
            }else{
                Log.e(TAG, "Date Time Characteristic is null!");
            }

            List<BluetoothGattCharacteristic> characteristicList =
                    serviceList.get(i).getCharacteristics();

            for(int j=0; j < characteristicList.size(); j++){
                Log.e(TAG,"Found Characteristic UUID : " + characteristicList.get(j).getUuid());
            }
        }

    }

    protected void getFirmwareRevision(BluetoothGatt gatt) {
        Log.d(TAG,"getFirmwareRevision");
        BluetoothGattService service = gatt.getService(DEVICE_INFORMATION_SERVICE);

        if (service == null) {
            Log.e(TAG, "Could not find DeviceInformation service");
            //disconnect();
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(FIRMWARE_REVISION_CHARACTERISTIC);

        if (characteristic == null) {
            Log.e(TAG, "Could not find FirmwareRevision characteristic");
            //disconnect();
            return;
        }
        Log.d(TAG,"Found Firmware characteristics!");
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
                if(!name.contains("Berry")) {
                    if (BluetoothDevice.BOND_BONDED == bondState) {
                        //setDateTime(mBluetoothGatt);
                        mBluetoothDevice = device;
                        mBluetoothGatt.discoverServices();
                        //BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(CHARACTERISTIC,BluetoothGattCharacteristic.PERMISSION_READ,BluetoothGattCharacteristic.PROPERTY_READ);
                        //mBluetoothGatt.setCharacteristicNotification(characteristic,true);
                        if(mListener!=null) {
                            mListener.onDeviceBonded(device);
                        }
                        Timber.d("Unregister Bonding Broadcast Receiver");
                        context.unregisterReceiver(mBondingBroadcastReceiver);
                    } else if (BluetoothDevice.BOND_BONDING == bondState) {
                        //setNotificationForCharacteristic(mBluetoothGatt.getService(SERVICE).getCharacteristic
                          //      (CHARACTERISTIC), true);
                    } else{

                        Timber.d("Unregister Bonding Broadcast Receiver");
                        context.unregisterReceiver(mBondingBroadcastReceiver);
                    }
                }
                else{
                    Log.d(TAG,"Berry found, discovering service");
                    mBluetoothDevice = device;
                    mBluetoothGatt.discoverServices();
                    Timber.d("Unregister Bonding Broadcast Receiver");
                    context.unregisterReceiver(mBondingBroadcastReceiver);
                }
            }

        }
    };
}
