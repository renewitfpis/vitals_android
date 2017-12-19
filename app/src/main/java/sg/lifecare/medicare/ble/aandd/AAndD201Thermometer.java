package sg.lifecare.medicare.ble.aandd;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import sg.lifecare.medicare.ble.BleException;
import sg.lifecare.medicare.ble.profile.TemperatureMeasurementProfile;
import sg.lifecare.medicare.database.model.Temperature;
import timber.log.Timber;

/**
 * A&D UT201 weight scale
 */
public class AAndD201Thermometer extends AAndDMeter {

    private static final String TAG = "AAndD201Thermometer";

    private static final String DEVICE_NAME = "A&D_UT201";

    private String pairedDeviceAddress;

    private Activity activity;

    public AAndD201Thermometer(Activity activity, AAndDMeterListener listener, String deviceId) throws BleException {
        super(activity, listener);
        this.activity = activity;
        this.pairedDeviceAddress = deviceId;
    }

    protected boolean isValidDevice(BluetoothDevice device) {
     /*   String name  = device.getName();
        if (!TextUtils.isEmpty(name)) {
            if (name.startsWith(DEVICE_NAME)) {
                return true;
            }
        }*/
        //TODO: compare the address

        if (pairedDeviceAddress != null && !pairedDeviceAddress.equalsIgnoreCase("")) {
            String address = device.getAddress();

            Timber.e("Address found : " + address);
            Timber.e("Paired Device : " + pairedDeviceAddress);

            if (!TextUtils.isEmpty(address)) {
                if (address.equalsIgnoreCase(pairedDeviceAddress)) {
                    return true;
                }
            }
            return false;
        }else{
            String name  = device.getName();
            if (!TextUtils.isEmpty(name)) {
                if (name.startsWith(DEVICE_NAME)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void parseProfile(BluetoothGattCharacteristic cha) {
        Log.e(TAG, "PARSE PROFILE - Received cha! ");
        byte[] values = cha.getValue();
        if (values != null) {

            Log.e(TAG, "PARSE PROFILE - cha values size  = " + values.length);
            TemperatureMeasurementProfile profile = new TemperatureMeasurementProfile(cha);
            mRecords.add(profile);

            SharedPreferences sh = activity.getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
            String entityId = sh.getString("entity_id", "");

            Temperature temp = new Temperature();
            temp.setValue(profile.getTemperature());
            temp.setUnit(profile.getUnit());
            temp.setDate(profile.getDate());
            temp.setEntityId(entityId);

            Log.e(TAG, "Received temp data! " + temp.getValue() + " " + temp.getUnit() + " " + temp.getDate().toString());

            if(temp.getValue()>=100){
                return;
            }
            mListener.onDataRetrieved(temp);
        }
    }

    protected void getProfile(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(HEALTH_THERMOMETER_SERVICE);

        if (service == null) {
            Log.e(TAG, "Could not find Thermometer service");
            //disconnect();
            return;
        }

        BluetoothGattCharacteristic cha = service.getCharacteristic(TEMPERATURE_MEASUREMENT_CHARACTERISTIC);

        if (cha == null) {
            Log.e(TAG, "Could not find TemperatureMeasurement characteristic");
            //disconnect();
            return;
        }

        queueSetIndicationForCharacteristic(cha, true);
    }

    @Override
    public void clear() {
        if(mBluetoothGatt != null) {
            /*setNotificationForCharacteristic(mBluetoothGatt.getService(SERVICE).getCharacteristic
                    (CHARACTERISTIC), false);*/
            BluetoothGattService service = mBluetoothGatt.getService(HEALTH_THERMOMETER_SERVICE);
            if(service!=null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(TEMPERATURE_MEASUREMENT_CHARACTERISTIC);

                if (characteristic != null) {
                    queueSetNotificationForCharacteristic(characteristic, false);
                }
            }
        }
    }

    @Override
    protected void setDateTime(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(HEALTH_THERMOMETER_SERVICE);

        if (service == null) {
            Log.e(TAG, "Could not find Thermometer service");
            //disconnect();
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(DATE_TIME_CHARACTERISTIC);

        if (characteristic == null) {
            Log.e(TAG, "Could not find DateTime characteristic (TemperatureMeasurement)");
            //disconnect();
            return;
        }
        queueWriteDataToCharacteristic(characteristic, getDateTime());

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
                if(mListener!=null){
                    mListener.onDeviceBonded(mBluetoothDevice);
                }
                Log.d(TAG,"Weight- Reading mode, getFirmwareRevision");
                getFirmwareRevision(gatt);
                //boolean isReading = gatt.readCharacteristic(gatt.getService(HEALTH_THERMOMETER_SERVICE).getCharacteristic
                  //      (TEMPERATURE_MEASUREMENT_CHARACTERISTIC));
                BluetoothGattService service = gatt.getService(HEALTH_THERMOMETER_SERVICE);

                if (service == null) {
                    Log.e(TAG, "Could not find Temperature service");
                    return;
                }

                BluetoothGattCharacteristic characteristic = service.getCharacteristic(TEMPERATURE_MEASUREMENT_CHARACTERISTIC);

                if (characteristic == null) {
                    Log.e(TAG, "Could not find Temperature Measurement characteristic");
                    return;
                }
                queueSetNotificationForCharacteristic(characteristic,true);
                //Log.d(TAG,"Reading mode - Is reading = " + isReading);
            } else {
                Log.d(TAG,"Weight- Pairing mode, setDateTime");
                setDateTime(gatt);
                boolean isReading = gatt.readCharacteristic(gatt.getService(HEALTH_THERMOMETER_SERVICE).getCharacteristic
                        (TEMPERATURE_MEASUREMENT_CHARACTERISTIC));
                Log.d(TAG,"SetDateTime - Is reading = " + isReading);
            }
        }
    }

    @Override
    protected void onBleStopped() {

    }

}
