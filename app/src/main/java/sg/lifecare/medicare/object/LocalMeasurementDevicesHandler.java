package sg.lifecare.medicare.object;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Set;

import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import timber.log.Timber;

/**
 * Created by wanping on 31/8/16.
 */
public class LocalMeasurementDevicesHandler {
    private static ArrayList<MedicalDevice> medicalDeviceList;
    private static Context context;
    private static String entityId;
    private static String prefix;

    private static LocalMeasurementDevicesHandler instance;

    public static LocalMeasurementDevicesHandler getInstance() {
        if (instance == null) {
            instance = new LocalMeasurementDevicesHandler(context);
        }

        return instance;
    }

    public static void initialize(Context ctx, String EntityId){
        context = ctx;
        entityId = EntityId;
        prefix = "";
    }

    private LocalMeasurementDevicesHandler(Context context){
        retrieveCurrentLocalDevices(context,entityId);
    }

    public ArrayList<MedicalDevice> getConnectedMedicalDeviceList(){
        if(medicalDeviceList==null) {
            Timber.d("Medical Device List is null. retrieving");
            retrieveCurrentLocalDevices(context, entityId);
        }

        try {
            Timber.d("Returning Medical Device Size = " + medicalDeviceList.size());
            checkDeviceBluetoothStatus();
        }catch(Exception e){
            e.printStackTrace();
        }
        return medicalDeviceList;
    }

    /**
     * This method retrieves locally stored devices list from sharedpref
     * and checks if the devices are still active in pairing status
     */
    public void retrieveCurrentLocalDevices(Context context, String entityId){
        if(context==null){
            Timber.d("context is null. returning");
            return;
        }

        Timber.d("context is not null. continuing");
        medicalDeviceList = new ArrayList<>();

        SharedPreferences sh = context.getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
        int size = sh.getInt(prefix+"medical_devices_array_size", -1);

        if (size == -1) {
            Timber.e("Medical Devices Array Size data is not found in shared pref!");
            return;
        }

        Timber.d("Retrieve from Shared Pref : Medical Devices Array Size = " + size);
        for(int i = 0; i < size; i++){
            String jsonStr = sh.getString(prefix+"medical_devices_array_" + i, "");
            try{
                JSONObject jsonObject = new JSONObject(jsonStr);
                String productId = "", name = "", deviceId = "";
                if(jsonObject.has("ProductId")) {
                    productId = jsonObject.getString("ProductId");
                }
                if(jsonObject.has("Name")) {
                    name = jsonObject.getString("Name");
                }
                if(jsonObject.has("DeviceId")) {
                    deviceId = jsonObject.getString("DeviceId");
                }

                if(!productId.equalsIgnoreCase("")){
                    MedicalDevice dummyMd = MedicalDevice.findByProductId(productId);
                    if(dummyMd!=null) {
                        MedicalDevice md = (MedicalDevice)dummyMd.clone();
                        md.setAssignedName(name);
                        md.setDeviceId(deviceId);
                        medicalDeviceList.add(md);
                    }
                }
            }catch(JSONException e){
                Timber.e(e.getMessage());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        for(int i = 0 ; i < medicalDeviceList.size(); i++){
            Timber.d("Connected Medical Devices " + i + ": " + medicalDeviceList.get(i).getDeviceId());
        }

        checkDeviceBluetoothStatus();
    }

    /**
     * This method checks the bluetooth connectivity of each local device,
     * if found disconnected, the device will be removed from local device list
     */
    public void checkDeviceBluetoothStatus(){
        Timber.d("Checking Device Bluetooth Status");

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(mBluetoothAdapter==null) {
            Timber.e("Bluetooth is not supported in user's device");
            return;
        }

        if(!mBluetoothAdapter.isEnabled()) {
            Timber.e("Bluetooth is not enabled in user's device");
            return;
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        ArrayList<MedicalDevice> disconnectedDeviceList = new ArrayList<>();
        for(MedicalDevice md : medicalDeviceList){
            Timber.d("checking if its ble : " + md.getDeviceId());

            if(!md.getMode().equalsIgnoreCase(MedicalDevice.WCONFIG_CODE_NFC)) {

                if(md.getModel() != Model.BERRY_BM1000B
                        && md.getModel() != Model.NONIN_3230
                        && md.getModel() != Model.ZENCRO_X6
                        && md.getModel() != Model.JUMPER_FR302
                        && md.getModel() != Model.JUMPER_JPD500E
                        && md.getModel() != Model.YOLANDA_LITE
                        && md.getModel() != Model.VIVACHEK_INO_SMART
                        && md.getModel() != Model.URION_BP_U80E) {

                    boolean isConnected = false;
                    for (BluetoothDevice bt : pairedDevices) {
                        Timber.d("Comparing local device = " + md.getDeviceId() + " with paired device = " + bt.getAddress() );

                        if (md.getDeviceId().equalsIgnoreCase(bt.getAddress())) {
                            isConnected = true;
                            Timber.d("Device " + md.getDeviceId() + " " + md.getAssignedName() + " is connected");
                        }
                    }

                    if (!isConnected) {
                        disconnectedDeviceList.add(md);
                        Timber.e("Device " + md.getDeviceId() + " " + md.getAssignedName() + " is not connected and hence removed");
                    }
                }
            }
        }

        for(MedicalDevice disconnectedMd : disconnectedDeviceList){
            removeMedicalDevice(disconnectedMd);
        }
    }

    /**
     * This method adds newly connected devices (if exists) to local device list
     * and then update the list to sharedpref
     */
    public void storeMedicalDevice(MedicalDevice md){

        Timber.d("Store Medical Devices, md = " + md.getDeviceId());
        boolean isDuplicate = false;
        // if there is duplicate, terminate action
        for(int i = 0; i < medicalDeviceList.size(); i++){
            if(medicalDeviceList.get(i).getDeviceId().
                    equalsIgnoreCase(md.getDeviceId())){
                Timber.e("Medical Device " + md.getDeviceId() + " already exists!");
                medicalDeviceList.get(i).setAssignedName(md.getAssignedName());
                isDuplicate = true;
            }
        }
        if(!isDuplicate) {
            Timber.d("there's no duplicate!!");
            medicalDeviceList.add(md);
        }

        updateSharedPref();
    }

    /**
     * This method removes connected devices from the list and sharedpref,
     * called when a device is unpaired using the app
     */
    public void removeMedicalDevice(MedicalDevice md){
        Timber.d("Remove Medical Devices");
        medicalDeviceList.remove(md);
        updateSharedPref();
    }

    /**
     * This method converts each device information to json string and store in sharedpref
     */
    public void updateSharedPref(){
        SharedPreferences sh = context.getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
        sh.edit().putInt(prefix+"medical_devices_array_size", medicalDeviceList.size()).apply();
        Timber.d("Update Shared Pref : Medical Devices Array Size = " + medicalDeviceList.size());
        Timber.d(prefix+"medical_devices_array_size" + " = " + medicalDeviceList.size());
        for(int i = 0; i < medicalDeviceList.size(); i++){
            String productId = medicalDeviceList.get(i).getProductId();
            String deviceId = medicalDeviceList.get(i).getDeviceId();
            String name = medicalDeviceList.get(i).getAssignedName();
            Timber.d(" UPDATE SHARED PREF " + i + " = " + medicalDeviceList.get(i).getDeviceId());

            JSONObject data = new JSONObject();
            try {
                data.put("ProductId",productId);
                data.put("DeviceId",deviceId);
                data.put("Name",name);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            sh.edit().putString(prefix+"medical_devices_array_"+i,data.toString()).apply();
        }
    }

    public MedicalDevice getDeviceByMacAddress(String macAddress){
        //macAddress = macAddress.replace(":","");

        for(int i = 0; i < medicalDeviceList.size(); i++){
            Timber.d("Medical Device List " + i + " : " + medicalDeviceList.get(i).getDeviceId());
            if(medicalDeviceList.get(i).getDeviceId().contains(macAddress)){
                return medicalDeviceList.get(i);
            }
        }
        return null;
    }

    public boolean hasConnectedModel(Model model) {

        for (int i = 0; i < medicalDeviceList.size(); i++) {
            if(medicalDeviceList.get(i).getModel() == model){
                return true;
            }
        }

        return false;
    }
}
