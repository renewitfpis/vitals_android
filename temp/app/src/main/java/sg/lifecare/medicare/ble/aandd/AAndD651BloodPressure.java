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
import sg.lifecare.medicare.ble.profile.BloodPressureMeasurementProfile;
import sg.lifecare.medicare.database.model.BloodPressure;
import timber.log.Timber;


/**
 * A&D blood pressure metersleep
 */
public class AAndD651BloodPressure extends AAndDMeter {

    private static final String TAG = "AAndD651BloodPressure";

    private static final String DEVICE_NAME = "A&D_UA-651";

    private String pairedDeviceAddress;

    private Activity activity;

    public AAndD651BloodPressure(Activity activity, AAndDMeterListener listener, String deviceId) throws BleException {
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

    public void clear() {
        if(mBluetoothGatt != null) {
            /*setNotificationForCharacteristic(mBluetoothGatt.getService(SERVICE).getCharacteristic
                    (CHARACTERISTIC), false);*/
            BluetoothGattService service = mBluetoothGatt.getService(BLOOD_PRESSURE_SERVICE);
            if(service!=null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC);

                if (characteristic != null) {
                    queueSetNotificationForCharacteristic(characteristic, false);
                }
            }
        }
    }

    protected void parseProfile(BluetoothGattCharacteristic cha) {
        Log.d(TAG,"Parse Profile!");
        byte[] values = cha.getValue();

        if (values != null && values.length == 18) {

            Log.d(TAG,"Parsing...");
            BloodPressureMeasurementProfile bpm = new BloodPressureMeasurementProfile(cha);
            mRecords.add(bpm);

            if(bpm.getSystolic()>=2047){ //invalid data returned (error from bp measurement)
                Log.e(TAG,"Invalid data returned");
                mListener.onInvalidDataReturned();
                return;
            }

            SharedPreferences sh = activity.getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
            String entityId = sh.getString("entity_id", "");

            //Add blood pressure data to realm database
            BloodPressure bloodPressure = new BloodPressure(
                    bpm.getSystolic(), bpm.getDistolic(),
                    bpm.getArterialPressure(), bpm.getPulseRate(),
                    bpm.getDate(), bpm.getUnit()
            );
            bloodPressure.setEntityId(entityId);
            Timber.d("EntityId = " + entityId);

            //Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
            //PatientData.getInstance().addBloodPressure(realm,bloodPressure);
            //realm.close();

            //Push data to server
            //uploadDataToServer task = new uploadDataToServer(bloodPressure);
            //task.execute();

            mListener.onDataRetrieved(bloodPressure);

        }
    }

    protected void getProfile(BluetoothGatt gatt) {
        Log.d(TAG,"Get Profile!");
        BluetoothGattService service = gatt.getService(BLOOD_PRESSURE_SERVICE);

        if (service == null) {
            Log.e(TAG, "Could not find BloodPressure service");
            //disconnect();
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC);

        if (characteristic == null) {
            Log.e(TAG, "Could not find BloodPressure Measurement characteristic");
            //disconnect();
            return;
        }

        queueSetIndicationForCharacteristic(characteristic, true);
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
                Log.d(TAG,"BP- Reading mode, getFirmwareRevision");
                getFirmwareRevision(gatt);
                //boolean isReading = gatt.readCharacteristic(gatt.getService(BLOOD_PRESSURE_SERVICE).getCharacteristic
                 //       (BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC));
                //Log.d(TAG,"Reading mode - Is reading = " + isReading);
                BluetoothGattService service = gatt.getService(BLOOD_PRESSURE_SERVICE);

                if (service == null) {
                    Log.e(TAG, "Could not find BloodPressure service");
                    return;
                }

                BluetoothGattCharacteristic characteristic = service.getCharacteristic(BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC);

                if (characteristic == null) {
                    Log.e(TAG, "Could not find BloodPressure Measurement characteristic");
                    return;
                }
                //boolean isReading = gatt.readCharacteristic(gatt.getService(BLOOD_PRESSURE_SERVICE).getCharacteristic
                  //      (BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC));
                //Log.e(TAG, "Read BP char? = "+ isReading);
                //gatt.setCharacteristicNotification(gatt.getService(BLOOD_PRESSURE_SERVICE)
                  //      .getCharacteristic(BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC),true);
                queueSetNotificationForCharacteristic(characteristic,true);
            } else {
                Log.d(TAG,"BP- Pairing mode, setDateTime");
                setDateTime(gatt);
                //TODO: commented on 24oct:
                // boolean isReading = gatt.readCharacteristic(gatt.getService(BLOOD_PRESSURE_SERVICE).getCharacteristic
                  //      (BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC));
                //Log.d(TAG,"SetDateTime BP - Is reading = " + isReading);
            }
        }
    }

    @Override
    protected void onBleStopped() {

    }

    @Override
    protected void setDateTime(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(BLOOD_PRESSURE_SERVICE);

        if (service == null) {
            Log.e(TAG, "Could not find BloodPressure service");
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(DATE_TIME_CHARACTERISTIC);

        if (characteristic == null) {
            Log.e(TAG, "Could not find DateTime characteristic");
            return;
        }
        queueWriteDataToCharacteristic(characteristic, getDateTime());
        Log.e(TAG, "SET DATE TIME!");


    }

}
