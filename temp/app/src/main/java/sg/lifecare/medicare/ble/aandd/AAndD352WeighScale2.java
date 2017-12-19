package sg.lifecare.medicare.ble.aandd;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import io.realm.Realm;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sg.lifecare.medicare.VitalConfigs;
import sg.lifecare.medicare.ble.BleException;
import sg.lifecare.medicare.ble.profile.WeightMeasurementProfile;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.database.sync.NetworkChangeReceiver;
import sg.lifecare.medicare.database.sync.UnsyncedData;
import sg.lifecare.medicare.utils.LifeCareHandler;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;

/**
 * A&D UC-352BLE weight scale
 */
public class AAndD352WeighScale2 extends AAndDMeter {

    private static final String TAG = "AAndD352WeighScale";

    private static final String DEVICE_NAME = "A&D_UC-352";

    private Activity activity;

    public AAndD352WeighScale2(Activity activity, AAndDMeterListener listener) throws BleException {
        super(activity, listener);
        this.activity = activity;
    }

    protected boolean isValidDevice(BluetoothDevice device) {
        String name  = device.getName();
        if (!TextUtils.isEmpty(name)) {
            if (name.startsWith(DEVICE_NAME)) {
                return true;
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

            Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
            PatientData.getInstance().addWeight(realm,weight);
            realm.close();

            if(NetworkChangeReceiver.isInternetAvailable(activity.getApplicationContext())) {
                uploadWeightDataToServer task = new uploadWeightDataToServer(weight);
                task.execute();
            }
            else{
                UnsyncedData.weightList.add(wm.getDate());
            }

            mListener.onDataRetrieved(weight);
        }
    }

    protected void getProfile(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(WEIGHT_SCALE_SERVICE);

        if (service == null) {
            Log.e(TAG, "Could not find WeightScale service");
            disconnect();
            return;
        }

        BluetoothGattCharacteristic cha = service.getCharacteristic(WEIGHT_MEASUREMENT_CHARACTERISTIC);

        if (cha == null) {
            Log.e(TAG, "Could not find WeightMeasurement characteristic");
            disconnect();
            return;
        }

        queueSetIndicationForCharacteristic(cha, true);
    }

    @Override
    public void clear() {

    }

    @Override
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

    @Override
    protected void onBleServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.d(TAG, "onBleServicesDiscovered: bondState=" + getBondState(mBluetoothDevice.getBondState()));
        if (BluetoothGatt.GATT_SUCCESS == status) {
            if ((MODE_PAIRING == mMode) && (BluetoothDevice.BOND_BONDED != mBluetoothDevice.getBondState())){
                // wait for bonded broadcast
                return;
            }

            if (MODE_READING == mMode) {
                Log.d(TAG,"Weight- Reading mode, getFirmwareRevision");
                getFirmwareRevision(gatt);
                boolean isReading = gatt.readCharacteristic(gatt.getService(WEIGHT_SCALE_SERVICE).getCharacteristic
                        (WEIGHT_MEASUREMENT_CHARACTERISTIC));
                Log.d(TAG,"Reading mode - Is reading = " + isReading);
            } else {
                Log.d(TAG,"Weight- Pairing mode, setDateTime");
                setDateTime(gatt);
                boolean isReading = gatt.readCharacteristic(gatt.getService(WEIGHT_SCALE_SERVICE).getCharacteristic
                        (WEIGHT_MEASUREMENT_CHARACTERISTIC));
                Log.d(TAG,"SetDateTime - Is reading = " + isReading);
            }
        }
    }

    @Override
    protected void onBleStopped() {

    }

    private class uploadWeightDataToServer extends AsyncTask<Void, Void, Void>
    {
        private Weight weight;
        boolean success;
        Date weightDate;

        public uploadWeightDataToServer(Weight weight)
        {
            this.weight = weight;
            this.weightDate = weight.getDate();
        }

        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {

            Log.d(TAG, "do in background!");

            JSONObject data = new JSONObject();
            String jsonString = "";
            String result = "";
            try
            {
                String extraData = "Weight:" + weight.getWeight()
                        + "&Height:" + ""
                        + "&Bmi:" + ""
                        + "&Type:" + weight.getStringUnit();

                MedicalDevice device = MedicalDevice.findModel(Model.AANDD_UC_352);

                String serial = device.getDeviceId();
                Log.d(TAG,"SERIAL = " + serial);
                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT);

                data.put("DeviceId", LifeCareHandler.getInstance().getMyDeviceID(activity));
                data.put("EntityId", weight.getEntityId());
                data.put("ExtraData", extraData);
                data.put("EventTypeName", "Weight Update Data");
                data.put("EventTypeId", "20014"); //weight
                data.put("Zone", "56398aa0e4b00e308ce460ec");
                data.put("ZoneCode", "OT");
                data.put("NodeId", serial);
                data.put("NodeName", device.getAssignedName());
                data.put("WConfigCode", "BLE");
                data.put("SystemManufacturerCode", device.getManufacturerCode());
                data.put("SystemProductCode", device.getProductCode());
                data.put("SystemProductTypeCode", device.getProductTypeCode());
                data.put("SmartDeviceId", serial);
                data.put("CreateDate", sdf.format(weight.getDate()));
                data.put("WriteToSocket",true);

                jsonString = data.toString();

                Log.w(TAG, jsonString);

                Request request = new Request.Builder()
                        .url(VitalConfigs.URL + "mlifecare/event/addEvent")
                        .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                        .build();
                //"https://eyecare.eyeorcas.com/eyeCare/eyeCareEventDetected.php"
                Response response = LifeCareHandler.getInstance().okclient.newCall(request).execute();

                JSONObject json = new JSONObject(response.body().string());
                success = !json.getBoolean("Error"); //Error = true if contains error
                Log.d(TAG, "Successful upload weight data: " + success
                        + " , Error desc (if exist): " + json.getString("ErrorDesc"));

            } catch(JSONException ex) {
                Log.e(TAG, ex.getMessage(), ex);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            } catch(Exception e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void res){
            if(success){
                Toast.makeText(activity, "Successfully updated weight data!",
                        Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(activity, "Update weight data fail!",
                        Toast.LENGTH_LONG).show();

                UnsyncedData.weightList.add(weightDate);
            }
        }
    }

}
