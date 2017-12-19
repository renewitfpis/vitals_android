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
import sg.lifecare.medicare.ble.profile.WeightMeasurementProfile;
import sg.lifecare.medicare.database.model.Weight;
import timber.log.Timber;

/**
 * A&D UC-352BLE weight scale
 */
public class AAndD352WeighScale extends AAndDMeter {

    private static final String TAG = "AAndD352WeighScale";

    private static final String DEVICE_NAME = "A&D_UC-352";
    private String pairedDeviceAddress;
    private Activity activity;

    public AAndD352WeighScale(Activity activity, AAndDMeterListener listener, String deviceId) throws BleException {
        super(activity, listener);
        this.activity = activity;
        this.pairedDeviceAddress = deviceId;
    }

    public void clear() {
        if(mBluetoothGatt != null) {
            /*setNotificationForCharacteristic(mBluetoothGatt.getService(SERVICE).getCharacteristic
                    (CHARACTERISTIC), false);*/
            BluetoothGattService service = mBluetoothGatt.getService(WEIGHT_SCALE_SERVICE);
            if(service!=null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(WEIGHT_MEASUREMENT_CHARACTERISTIC);

                if (characteristic != null) {
                    queueSetNotificationForCharacteristic(characteristic, false);
                }
            }
        }
    }

    protected boolean isValidDevice(BluetoothDevice device) {
      /*  String name  = device.getName();
        if (!TextUtils.isEmpty(name)) {
            if (name.startsWith(DEVICE_NAME)) {
                return true;
            }
        }*/
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
        byte[] values = cha.getValue();
        if (values != null && values.length == 10) {
            WeightMeasurementProfile wm = new WeightMeasurementProfile(cha);
            mRecords.add(wm);

            SharedPreferences sh = activity.getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
            String entityId = sh.getString("entity_id", "");

            //Add weight data to realm database
            Weight weight = new Weight(
                    wm.getWeight(), wm.getDate(), wm.getUnit()
            );
            weight.setEntityId(entityId);

            mListener.onDataRetrieved(weight);
        }
    }

    protected void getProfile(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(WEIGHT_SCALE_SERVICE);

        if (service == null) {
            Log.e(TAG, "Could not find WeightScale service");
            //disconnect();
            return;
        }

        BluetoothGattCharacteristic cha = service.getCharacteristic(WEIGHT_MEASUREMENT_CHARACTERISTIC);

        if (cha == null) {
            Log.e(TAG, "Could not find WeightMeasurement characteristic");
            //disconnect();
            return;
        }

        queueSetIndicationForCharacteristic(cha, true);
    }

    @Override
    protected void setDateTime(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(WEIGHT_SCALE_SERVICE);

        if (service == null) {
            Log.e(TAG, "Could not find WeightScale service");
            //disconnect();
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(DATE_TIME_CHARACTERISTIC);

        if (characteristic == null) {
            Log.e(TAG, "Could not find DateTime characteristic");
            //disconnect();
            return;
        }
        queueWriteDataToCharacteristic(characteristic, getDateTime());
        Log.e(TAG, "SET DATE TIME!");

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
                /*getFirmwareRevision(gatt);
                setDateTime(gatt);*/

                BluetoothGattService service = gatt.getService(WEIGHT_SCALE_SERVICE);

                if (service == null) {
                    Log.e(TAG, "Could not find WeightScale service");
                    //disconnect();
                    return;
                }

                BluetoothGattCharacteristic characteristic = service.getCharacteristic(WEIGHT_MEASUREMENT_CHARACTERISTIC);

                if (characteristic == null) {
                    Log.e(TAG, "Could not find DateTime characteristic");
                    //disconnect();
                    return;
                }

                Log.d(TAG,"Weight- Set notification");
                //queueReadDataToCharacteristic(characteristic);
                //queueSetNotificationForCharacteristic(characteristic,true);
                //Log.d(TAG,"Reading mode - Is reading = " + isReading);
                //important
                getFirmwareRevision(gatt);
                //queueReadDataToCharacteristic(characteristic);
                //queueSetNotificationForCharacteristic(characteristic,true);
                boolean isReading = gatt.readCharacteristic(gatt.getService(WEIGHT_SCALE_SERVICE).getCharacteristic
                        (WEIGHT_MEASUREMENT_CHARACTERISTIC));
                /*gatt.readCharacteristic(gatt.getService(WEIGHT_SCALE_SERVICE).getCharacteristic
                        (WEIGHT_MEASUREMENT_CHARACTERISTIC));*/

            } else {
                Log.d(TAG,"Weight- Pairing mode, setDateTime");
                setDateTime(gatt);
                //boolean isReading = gatt.readCharacteristic(gatt.getService(WEIGHT_SCALE_SERVICE).getCharacteristic
                  //      (WEIGHT_MEASUREMENT_CHARACTERISTIC));
                //Log.d(TAG,"SetDateTime - Is reading = " + isReading);
            }
        }
    }

    @Override
    protected void onBleStopped() {

    }


}
