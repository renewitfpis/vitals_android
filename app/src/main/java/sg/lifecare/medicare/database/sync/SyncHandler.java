package sg.lifecare.medicare.database.sync;

import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sg.lifecare.medicare.MediCareApplication;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Medication;
import sg.lifecare.medicare.database.model.Photo;
import sg.lifecare.medicare.database.model.SpO2;
import sg.lifecare.medicare.database.model.Symptom;
import sg.lifecare.medicare.database.model.Temperature;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.utils.LifeCareHandler;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import timber.log.Timber;

import static sg.lifecare.medicare.database.sync.UnsyncedData.spo2List;

/**
 * Created by wanping on 11/8/16.
 */
public class SyncHandler {

    private static NetworkChangeReceiver receiver = null;


    public static void registerNetworkBroadcast(){
        if(receiver==null) {
            Timber.d("Registered receiver");
            receiver = new NetworkChangeReceiver();
            MediCareApplication.getContext().registerReceiver(receiver,
                    new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        }
    }

    public static void unregisterNetworkBroadcast(){
        if(receiver!=null) {
            MediCareApplication.getContext().unregisterReceiver(receiver);
            Timber.d("Unregistered receiver");
            receiver = null;
        }
    }

    public static void retrieveUnsyncedData(Context context){
        UnsyncedData.glucoseList = SyncHandler.getStringArrayPref(context,"unsynced_glucose_list");
        UnsyncedData.medicationList = SyncHandler.getStringArrayPref(context,"unsynced_medication_list");
        UnsyncedData.foodList = SyncHandler.getStringArrayPref(context,"unsynced_food_list");
        UnsyncedData.symptomsList = SyncHandler.getStringArrayPref(context,"unsynced_symptom_list");
        UnsyncedData.bloodPressureList = SyncHandler.getStringArrayPref(context,"unsynced_blood_pressure_list");
        UnsyncedData.weightList = SyncHandler.getStringArrayPref(context,"unsynced_weight_list");
        spo2List = SyncHandler.getStringArrayPref(context,"unsynced_spo2_list");
        UnsyncedData.tempList = SyncHandler.getStringArrayPref(context,"unsynced_temp_list");
        //TODO: add other vitals data like temp

        Timber.d("Retrieved Unsynced Data, " + "\n" + "GlucoseList = " + UnsyncedData.glucoseList.size()
        + "\n" + "MedicList = " + UnsyncedData.medicationList.size()
        + "\n" + "FoodList = " + UnsyncedData.foodList.size()
        + "\n" + "SymptomsList = " + UnsyncedData.symptomsList.size()
        + "\n" + "BloodPressureList = " + UnsyncedData.bloodPressureList.size()
        + "\n" + "WeightList = " + UnsyncedData.weightList.size()
        + "\n" + "spo2List = " + spo2List.size()
        + "\n" + "tempList = " + UnsyncedData.tempList.size());
    }

    public static void storeUnsyncedData(Context context){
        SyncHandler.setStringArrayPref(context,"unsynced_glucose_list", UnsyncedData.glucoseList);
        SyncHandler.setStringArrayPref(context,"unsynced_medication_list",UnsyncedData.medicationList);
        SyncHandler.setStringArrayPref(context,"unsynced_food_list",UnsyncedData.foodList);
        SyncHandler.setStringArrayPref(context,"unsynced_symptom_list",UnsyncedData.symptomsList);
        SyncHandler.setStringArrayPref(context,"unsynced_blood_pressure_list",UnsyncedData.bloodPressureList);
        SyncHandler.setStringArrayPref(context,"unsynced_weight_list",UnsyncedData.weightList);
        SyncHandler.setStringArrayPref(context,"unsynced_spo2_list", spo2List);
        SyncHandler.setStringArrayPref(context,"unsynced_temp_list",UnsyncedData.tempList);
        //TODO: SyncHandler.setStringArrayPref(context,"unsynced_temp_list",UnsyncedData.tempList);

        Timber.d("Store Unsynced Data, " + "\n" + "GlucoseList = " + UnsyncedData.glucoseList.size()
                + "\n" + "MedicList = " + UnsyncedData.medicationList.size()
                + "\n" + "FoodList = " + UnsyncedData.foodList.size()
                + "\n" + "SymptomsList = " + UnsyncedData.symptomsList.size()
                + "\n" + "BloodPressureList = " + UnsyncedData.bloodPressureList.size()
                + "\n" + "WeightList = " + UnsyncedData.weightList.size()
                + "\n" + "TempList = " + UnsyncedData.tempList.size()
                + "\n" + "Spo2List = " + spo2List.size());
    }

    public static void uploadDataToServer(Context context){
        if(!NetworkChangeReceiver.isInternetAvailable(context)) {
            registerNetworkBroadcast();
            return;
        }

        Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

        for(int i = UnsyncedData.glucoseList.size()-1; i >= 0; i--){
            Date date = UnsyncedData.glucoseList.get(i);
            Terumo terumo = PatientData.getInstance().getTerumoBySpecificDate(realm,date);
            if(terumo!=null)
                new uploadGlucoseData(terumo,true).execute();
        }

        for(int i = UnsyncedData.medicationList.size()-1; i >= 0; i--){
            Timber.d("Found Medication Unsynced = " + i);
            Date date = UnsyncedData.medicationList.get(i);
            Medication medication = PatientData.getInstance().getMedicationBySpecificDate(realm,date);
            if(medication!=null)
                new UploadMedicationData(context, medication).execute();
        }

        for(int i = UnsyncedData.symptomsList.size() - 1; i >= 0; i--){
            Timber.d("Found Symptoms Unsynced = " + i);
            Date date = UnsyncedData.symptomsList.get(i);
            Symptom symptom = PatientData.getInstance().getSymptomBySpecificDate(realm,date);
            if(symptom!=null)
                new UploadSymptomsData(symptom).execute();
        }

        for(int i = UnsyncedData.bloodPressureList.size() - 1; i >= 0; i--){
           // TODO: complete BP data
            Timber.d("Found BP Unsynced = " + i);
            Date date = UnsyncedData.bloodPressureList.get(i);
            BloodPressure bp = PatientData.getInstance().getBloodPressureBySpecificDate(realm,date);
            if(bp!=null)
                new UploadBloodPressureData(bp).execute();
        }

        for(int i = UnsyncedData.weightList.size() - 1; i >= 0; i--){
            Timber.d("Found Weight Unsynced = " + i);
            Date date = UnsyncedData.weightList.get(i);
            Weight weight = PatientData.getInstance().getWeightBySpecificDate(realm,date);
            if(weight!=null)
                new UploadWeightData(weight).execute();
        }

        for(int i = UnsyncedData.tempList.size() - 1; i >= 0; i--){
            Timber.d("Found Temp Unsynced = " + i);
            Date date = UnsyncedData.tempList.get(i);
            Temperature temp = PatientData.getInstance().getTempBySpecificDate(realm,date);
            if(temp!=null) {
                new UploadTempData(temp).execute();
            }
        }

        for(int i = UnsyncedData.spo2List.size() - 1; i >= 0; i--){
            Timber.d("Found SpO2 Unsynced = " + i);
            Date date = UnsyncedData.spo2List.get(i);
            SpO2 spo2 = PatientData.getInstance().getSpO2BySpecificDate(realm,date);
            if(spo2!=null) {
                Timber.d("IS NOT NULLLLLLLL");
                new UploadSpO2Data(spo2).execute();
            }else{
                Timber.d("IS NULLLLLLLL");
            }
        }

        for(int i = UnsyncedData.foodList.size() - 1; i >= 0; i--){
            Timber.d("Found Food Unsynced = " + i);
            Date date = UnsyncedData.foodList.get(i);
            Photo photo = PatientData.getInstance().getFoodBySpecificDate(realm,date);
            if(photo!=null)
                SyncHandler.uploadImageToLocalServer(context, photo);
        }

        realm.close();

        SyncHandler.unregisterNetworkBroadcast();
    }

    public static void setStringArrayPref(Context context, String key, ArrayList<Date> values) {
        SharedPreferences prefs = context.getSharedPreferences("lifecare_pref",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray a = new JSONArray();
        for (int i = 0; i < values.size(); i++) {
            a.put(values.get(i).getTime());
        }
        if (!values.isEmpty()) {
            editor.putString(key, a.toString());
        } else {
            editor.putString(key, null);
        }
        editor.apply();
    }

    public static ArrayList<Date> getStringArrayPref(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences("lifecare_pref",Context.MODE_PRIVATE);
        String json = prefs.getString(key, null);
        ArrayList<Date> urls = new ArrayList<>();
        if (json != null) {
            try {
                JSONArray a = new JSONArray(json);
                for (int i = 0; i < a.length(); i++) {
                    Long millis = a.optLong(i);
                    Date date = new Date(millis);
                    urls.add(date);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return urls;
    }

    public static class uploadGlucoseData extends AsyncTask<Void, Void, Void>
    {
        private Terumo terumo;
        private boolean isManualKeyIn;
        private boolean success;
        private JSONObject data = new JSONObject();
        private Date date;

        public uploadGlucoseData(Terumo terumo, boolean isManualKeyIn)
        {
            this.terumo = terumo;
            this.isManualKeyIn = isManualKeyIn;
        }

        @Override
        protected void onPreExecute(){
            try {

                date = terumo.getDate();
                String meal = terumo.isBeforeMeal() ? "Before" : "After";
                String extraData = "Concentration:" + terumo.getValue()
                        + "&Unit:" + terumo.getStringUnit()
                        + "&SampleLocation:" + "Finger"
                        + "&Type:"+ meal
                        + "&StatusAnnunciation:" + "N/A"
                        + "&Remarks:" + terumo.getRemark();

                MedicalDevice device = MedicalDevice.findModel(Model.TERUMO_MEDISAFE_FIT);

                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT_UPLOAD, Locale.ENGLISH);

                //SharedPreferences sh = context.getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
                //String entityId = sh.getString("entity_id", "");

                //data.put("DeviceId", LifeCareHandler.getInstance().getMyDeviceID(context));
                data.put("EntityId", terumo.getEntityId());
                data.put("ExtraData", extraData);
                data.put("EventTypeName", "Gluco Update Data");
                data.put("EventTypeId", "20015"); //blood glucose
                data.put("Zone", "56398aa0e4b00e308ce460ec");
                data.put("ZoneCode", "OT");
                data.put("CreateDate", sdf.format(terumo.getDate()));
                data.put("WriteToSocket",false);

                if(!isManualKeyIn) {
                    String serial = device.getDeviceId();
                    data.put("NodeId", serial);
                    data.put("NodeName", device.getAssignedName());
                    data.put("WConfigCode", "NFC");
                    data.put("SystemManufacturerCode", device.getManufacturerCode());
                    data.put("SystemProductCode", device.getProductCode());
                    data.put("SystemProductTypeCode", device.getProductTypeCode());
                    data.put("SmartDeviceId", serial);
                }
            }
            catch(JSONException ex) {
                Timber.e(ex.getMessage(), ex);
            }
        }

        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {

                String jsonString = data.toString();

                Timber.w("Uploading BG Data To Server : " + jsonString);

                Request request = new Request.Builder()
                        .url("https://www.lifecare.sg/mlifecare/event/addEvent")
                        .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                        .build();
                //https://www.lifecare.sg/mlifecare/event/addEvents

            Response response = null;
            try {
                response = LifeCareHandler.getInstance().okclient.newCall(request).execute();


                JSONObject json = new JSONObject(response.body().string());
                success = !json.getBoolean("Error"); //Error = true if contains error
                Timber.d("Successful upload BG " + date.toString() + " ? : " + success + " , " + json.getString("ErrorDesc"));
                if(success){
                    UnsyncedData.glucoseList.remove(date);
                    Timber.d("Successfully removed " + date.toString() + " from unsynced");
                }

                }
            catch(JSONException ex) {
                Timber.e(ex.getMessage(), ex);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (NullPointerException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void res){

        }

    }

    public static class UploadSymptomsData extends AsyncTask<Void, Void, Void>
    {
        private Symptom symptom;
        private boolean success;
        private String jsonString;
        private Date date;

        public UploadSymptomsData(Symptom symptom)
        {
            this.symptom = symptom;
            this.date = symptom.getDate();
        }

        @Override
        protected void onPreExecute(){
            JSONObject data = new JSONObject();

            //Types format = 1,3,5,...
            String extraData = "Types:" + symptom.getSymptoms()
                    + "&Remarks:"+ symptom.getRemark();

            Timber.d("Extra Data= " + extraData);

            //MedicalDevice device = MedicalDevice.findModel(Model.TERUMO_MEDISAFE_FIT);

            SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT_UPLOAD);

            //String serial = device.getDeviceId();

            //data.put("DeviceId", LifeCareHandler.getInstance().getMyDeviceID(SignsAndSymptomsActivity.this));
            try {
                data.put("EntityId", symptom.getEntityId());
                data.put("ExtraData", extraData);
                data.put("EventTypeName", "Symptoms");
                data.put("EventTypeId", "20063"); //symptom
                data.put("Zone", "56398aa0e4b00e308ce460ec");
                data.put("ZoneCode", "OT");
                data.put("CreateDate", sdf.format(date));
                data.put("WriteToSocket",false);
                //data.put("SmartDeviceId", serial);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            jsonString = data.toString();
        }
        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {
            try {


                Timber.w("Uploading Symptoms Data To Server : " + jsonString);

                Request request = new Request.Builder()
                        .url("https://www.lifecare.sg/mlifecare/event/addEvent")
                        .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                        .build();

                Response response = LifeCareHandler.getInstance().okclient.newCall(request).execute();

                JSONObject json = new JSONObject(response.body().string());
                success = !json.getBoolean("Error"); //Error = true if contains error
                //success = response.isSuccessful();
                Timber.d("Successful upload: " + success + " , " + json.getString("ErrorDesc"));
                if(success){
                    UnsyncedData.symptomsList.remove(date);
                    Timber.d("Successfully removed symptoms " + date.toString() + " from unsynced");
                }

                response.body().close();
            } catch(JSONException ex) {
                Timber.e(ex.getMessage(), ex);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void res){

        }

    }

    private static class UploadBloodPressureData extends AsyncTask<Void, Void, Void>
    {
        private BloodPressure bp;
        //private boolean isManualKeyIn;
        private boolean success;
        private Date bpDate;
        private String jsonString;

        public UploadBloodPressureData(BloodPressure bp)
        {
            this.bp = bp;
            this.bpDate = bp.getDate();
            //this.isManualKeyIn = isManualKeyIn;
        }

        @Override
        protected void onPreExecute(){
            try {
                JSONObject data = new JSONObject();

                String extraData = "HighBlood:" + bp.getSystolic()
                        + "&LowBlood:" + bp.getDistolic()
                        + "&HeartBeat:" + bp.getPulseRate()
                        + "&Remarks:" + bp.getRemark();

                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT_UPLOAD);

                data.put("DeviceId", LifeCareHandler.getInstance().getMyDeviceID(MediCareApplication.getContext()));
                data.put("EntityId", bp.getEntityId());
                data.put("ExtraData", extraData);
                data.put("EventTypeName", "BloodPressure Update Data");
                data.put("EventTypeId", "20013"); //blood pressure
                data.put("Zone", "56398aa0e4b00e308ce460ec");
                data.put("ZoneCode", "OT");
                data.put("CreateDate", sdf.format(bpDate));
                data.put("WriteToSocket", true);

                /*if(!isManualKeyIn) {
                    String serial = device.getDeviceId();
                    data.put("NodeId", serial);
                    data.put("NodeName", device.getAssignedName());
                    data.put("WConfigCode", "NFC");
                    data.put("SystemManufacturerCode", device.getManufacturerCode());
                    data.put("SystemProductCode", device.getProductCode());
                    data.put("SystemProductTypeCode", device.getProductTypeCode());
                    data.put("SmartDeviceId", serial);
                }*/

                jsonString = data.toString();
            }
            catch(JSONException e){
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {
            try {

                Timber.w("Uploading BG Data To Server : " + jsonString);

                Request request = new Request.Builder()
                        .url("https://www.lifecare.sg/mlifecare/event/addEvent")
                        .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                        .build();

                Response response = LifeCareHandler.getInstance().okclient.newCall(request).execute();

                JSONObject json = new JSONObject(response.body().string());
                success = !json.getBoolean("Error"); //Error = true if contains error
                Timber.d("Successful upload: " + success + " , " + json.getString("ErrorDesc"));

                response.body().close();

                if(success){
                    UnsyncedData.bloodPressureList.remove(bpDate);
                }
            } catch(JSONException ex) {
                Timber.e(ex.getMessage(), ex);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private static class UploadWeightData extends AsyncTask<Void, Void, Void>
    {
        private Weight weight;
        boolean success;
        Date weightDate;

        String jsonString = "";

        public UploadWeightData(Weight weight)
        {
            this.weight = weight;
            this.weightDate = weight.getDate();
        }

        @Override
        protected void onPreExecute() {
            JSONObject data = new JSONObject();
            String result = "";
            try {
                String extraData = "Weight:" + weight.getWeight()
                        + "&Height:" + ""
                        + "&Bmi:" + ""
                        + "&Type:" + weight.getStringUnit();

                MedicalDevice device = MedicalDevice.findModel(Model.AANDD_UC_352);

                String serial = device.getDeviceId();
                Timber.d("SERIAL = " + serial);
                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT_UPLOAD);

                data.put("DeviceId", LifeCareHandler.getInstance().getMyDeviceID(MediCareApplication.getContext()));
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
                data.put("WriteToSocket", true);

                jsonString = data.toString();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


            @Override
        protected Void doInBackground(Void... paramVarArgs)
        {
                Timber.w(jsonString);

                Request request = new Request.Builder()
                        .url("https://www.lifecare.sg/mlifecare/event/addEvent")
                        .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                        .build();
                //"https://eyecare.eyeorcas.com/eyeCare/eyeCareEventDetected.php"
                Response response = null;
                try {
                    response = LifeCareHandler.getInstance().okclient.newCall(request).execute();

                JSONObject json = new JSONObject(response.body().string());
                success = !json.getBoolean("Error"); //Error = true if contains error
                Timber.d("Successful upload weight data: " + success
                            + " , Error desc (if exist): " + json.getString("ErrorDesc"));

                if(success){
                    UnsyncedData.weightList.remove(weightDate);
                    Timber.d("Successfully removed weight " + weightDate.toString() + " from unsynced");
                }

                response.body().close();
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch(JSONException ex) {
                Timber.e(ex.getMessage(), ex);
            } catch(Exception e){
                e.printStackTrace();
            }

            return null;
        }
    }

    private static class UploadTempData extends AsyncTask<Void, Void, Void>
    {
        private Temperature temp;
        boolean success = false;
        Date date;
        String jsonString = "", result ="";

        public UploadTempData(Temperature temp)
        {
            this.temp = temp;
            this.date = temp.getDate();
        }

        @Override
        protected void onPreExecute() {
            JSONObject data = new JSONObject();
            try {

                String extraData = "Temperature:" + temp.getValue()
                        + "&Unit:" + temp.getStringUnit()
                        + "&Location:" + "armpit";

                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT_UPLOAD, Locale.ENGLISH);

                data.put("DeviceId", LifeCareHandler.getInstance().getMyDeviceID(MediCareApplication.getContext()));
                data.put("EntityId", temp.getEntityId());
                data.put("ExtraData", extraData);
                data.put("EventTypeName", "Temperature Update Data");
                data.put("EventTypeId", "20051");
                data.put("Zone", "56398aa0e4b00e308ce460ec");
                data.put("ZoneCode", "OT");
                data.put("CreateDate", sdf.format(temp.getDate()));
                data.put("WriteToSocket",false);
                data.put("WConfigCode", "BLE");

                jsonString = data.toString();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {
            try
            {
                Timber.d("JSON = " + jsonString);

                Request request = new Request.Builder()
                        .url("https://www.lifecare.sg/mlifecare/event/addEvent")
                        .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                        .build();
                //"https://eyecare.eyeorcas.com/eyeCare/eyeCareEventDetected.php"
                Response response = LifeCareHandler.getInstance().okclient.newCall(request).execute();

                JSONObject json = new JSONObject(response.body().string());
                success = !json.getBoolean("Error"); //Error = true if contains error
                if(success){
                    Timber.d("SUCCESS");
                    UnsyncedData.tempList.remove(date);
                }else{

                    Timber.d("FAIL");
                }
                response.body().close();
            } catch(JSONException ex) {
                Timber.e( ex.getMessage(), ex);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            } catch(Exception e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void res){

        }
    }

    private static class UploadSpO2Data extends AsyncTask<Void, Void, Void>
    {
        private SpO2 spo2;
        boolean success;
        Date spo2Date;
        String jsonString = "";
        public UploadSpO2Data(SpO2 spo2)
        {
            this.spo2 = spo2;
            this.spo2Date = spo2.getDate();
        }

        @Override
        protected void onPreExecute(){
            JSONObject data = new JSONObject();

            try {
                String extraData = "Pulse:" + spo2.getPulseRate()
                        + "&SpO2:" + spo2.getValue()
                        + "&LastReading:" + "true";

                //TODO: change to local measurement list
                /*String gatewayId = LifeCareHandler.getInstance().getMyDeviceID(SpO2ReadingActivity.this);
                SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
                String serial = spo2.getEntityId() + "-" + gatewayId + "-" + pairedDevice.getDeviceId().replace(":", "");
                Timber.d("SERIAL = " + serial);*/
                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT_UPLOAD, Locale.ENGLISH);

                data.put("DeviceId", LifeCareHandler.getInstance().getMyDeviceID(MediCareApplication.getContext()));
                data.put("EntityId", spo2.getEntityId());
                data.put("ExtraData", extraData);
                data.put("EventTypeName", "SpO2 Update Data");
                data.put("EventTypeId", "20050"); //spo2
                data.put("Zone", "56398aa0e4b00e308ce460ec");
                data.put("ZoneCode", "OT");
                data.put("WConfigCode", "BLE");
                data.put("CreateDate", sdf.format(spo2.getDate()));
                data.put("WriteToSocket", true);

                jsonString = data.toString();
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {

            Timber.d("do in background!");

           try{
                Request request = new Request.Builder()
                        .url("https://www.lifecare.sg/mlifecare/event/addEvent")
                        .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                        .build();
                //"https://eyecare.eyeorcas.com/eyeCare/eyeCareEventDetected.php"
                Response response = LifeCareHandler.getInstance().okclient.newCall(request).execute();

                JSONObject json = new JSONObject(response.body().string());
                success = !json.getBoolean("Error"); //Error = true if contains error
                Timber.d("Successful upload spo2 data: " + success
                        + " , Error desc (if exist): " + json.getString("ErrorDesc"));
                   if(success){
                       Timber.d("SUCCESS");
                       UnsyncedData.spo2List.remove(spo2Date);
                   }else{
                       Timber.d("FAIL");
                   }

            } catch(JSONException ex) {
                Timber.e(ex.getMessage(), ex);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            } catch(Exception e){
                e.printStackTrace();
            }

            return null;
        }
    }


    public static void uploadImageToLocalServer(final Context context, final Photo photo){
        Future uploading = Ion.with(context)
                .load("https://www.lifecare.sg/mobileUploadMedia")
                .setMultipartFile("file", "image/jpeg", new File(photo.getImage()))
                .asString()
                .withResponse()
                .setCallback(new FutureCallback<com.koushikdutta.ion.Response<String>>(){
                    @Override
                    public void onCompleted(Exception e, com.koushikdutta.ion.Response<String> result) {
                        if (result == null) {
                            Timber.e("Result is null");
                            return;
                        }
                        Timber.d("RAW RESULT = " + result.getResult());
                        try {
                            JSONObject json = new JSONObject(result.getResult());
                            //JSONObject json = new result.getResult();
                            if (json != null) {
                                if (json.has("Data")) {

                                    //String data = result.getResult().get("Data").toString();
                                    JSONObject dataObj = (JSONObject) json.get("Data");
                                    String data = dataObj.toString();
                                    Timber.d("Data = " + data);

                                    if (data != null && !data.equals("")) {
                                        new UploadImageToCloudTask(context, dataObj, photo).execute();

                                    }
                                }
                            }
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }});
    }
    //cloudinary
    public static  class UploadImageToCloudTask extends AsyncTask<Void, Void, Void>
    {
        JSONObject dataObj;
        Photo photo;
        boolean success;
        Context context;
        String jsonString;
        String extraData, entityId;
        Date date;

        public UploadImageToCloudTask(Context context, JSONObject dataObj, Photo photo){
            this.dataObj = dataObj;
            this.photo = photo;
            this.context = context;
            this.extraData = "Carbohydrates:" + photo.getValue()
                    + "&Remarks:"+ photo.getRemark();
            this.entityId = photo.getEntityId();
            this.date = photo.getDate();
        }

        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {
            //TODO: complete the uploading task
            SharedPreferences sh = context.getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
            String entityId = sh.getString("entity_id", "");
            String res = "";
            String mediaId = "";

            if(dataObj!=null)
                res = LifeCareHandler.getInstance().uploadObjectPicture(dataObj,entityId);

            if(res!=null){
                Timber.d("RESPONSE FROM UPLOAD IMG = " + res);
                try {
                    JSONObject object = new JSONObject(res);
                    Timber.d("converted response to obj");

                    if(object.has("Data")){
                        Timber.d("Data = " + object.get("Data").toString());
                        JSONObject data = (JSONObject) object.get("Data");

                        if(data.has("_id")){
                            mediaId = data.getString("_id");
                        }
                    }

                    if(!mediaId.equals("")){
                        //upload food intake
                        generateJsonString(mediaId);
                        uploadFood();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }catch (IOException e) {
                    e.printStackTrace();
                } catch(ClassCastException e){
                    e.printStackTrace();
                }

            }

            return null;
        }

        private void generateJsonString(String mediaId) throws JSONException {
            JSONObject data = new JSONObject();

            Timber.d("Extra Data= " + extraData);

            SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT_UPLOAD);

            //String serial = device.getDeviceId();
            data.put("EntityId", entityId);
            data.put("ExtraData", extraData);
            data.put("EventTypeName", "Food Intake");
            data.put("EventTypeId", "20061"); //food intake
            data.put("CreateDate", sdf.format(date));

            JSONArray mediaIdArr = new JSONArray();
            mediaIdArr.put(mediaId);

            data.put("MediaIds",mediaIdArr);
            data.put("WriteToSocket",false);

            jsonString = data.toString();
        }

        private void uploadFood() throws JSONException, IOException {

            Timber.w("Uploading Food Data To Server : " + jsonString);

            Request request = new Request.Builder()
                    .url("https://www.lifecare.sg/mlifecare/event/addEvent")
                    .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                    .build();

            okhttp3.Response response = LifeCareHandler.getInstance().okclient.newCall(request).execute();

            JSONObject json = new JSONObject(response.body().string());
            success = !json.getBoolean("Error"); //Error = true if contains error
            //success = response.isSuccessful();
            if(success){
                UnsyncedData.foodList.remove(date);
            }
            Timber.d("Successful food upload: " + success + " , " + json.getString("ErrorDesc"));

            response.body().close();
        }

        @Override
        protected void onPostExecute(Void paramVoid)
        {
          /*  if(success) {
                Toast.makeText(context(),"Successfully uploaded!",Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(getContext(),"Failed to upload!",Toast.LENGTH_SHORT).show();
            }*/
        }
    }

    public static class UploadMedicationData extends AsyncTask<Void, Void, Void>
    {
        private Medication medication;
        private boolean success;
        private String jsonString = "";
        private Date date;

        public UploadMedicationData(Context context, Medication medication)
        {
            this.medication = medication;
            this.date = medication.getDate();
        }

        @Override
        protected void onPreExecute(){
            JSONObject data = new JSONObject();

            String extraData = "MedicationType:" + medication.getStringType()
                    + "&Dosage:" + medication.getDosage()
                    + "&Unit:"+ medication.getStringUnit()
                    + "&Remarks:"+ medication.getRemark();

            Timber.d("Extra Data= " + extraData);

            MedicalDevice device = MedicalDevice.findModel(Model.TERUMO_MEDISAFE_FIT);

            SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT_UPLOAD);

            String serial = device.getDeviceId();

            try {
                //data.put("DeviceId", LifeCareHandler.getInstance().getMyDeviceID(context));
                data.put("EntityId", medication.getEntityId());
                data.put("ExtraData", extraData);
                data.put("EventTypeName", "Medication Intake");
                data.put("EventTypeId", "20060"); //medication
                data.put("Zone", "56398aa0e4b00e308ce460ec");
                data.put("ZoneCode", "OT");
                data.put("CreateDate", sdf.format(medication.getDate()));
                data.put("WriteToSocket",false);
                data.put("SmartDeviceId", serial);

                jsonString = data.toString();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {
            try {
                Timber.d("Uploading Medication Data To Server : " + jsonString);

                Request request = new Request.Builder()
                        .url("https://www.lifecare.sg/mlifecare/event/addEvent")
                        .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, jsonString))
                        .build();

                Response response = LifeCareHandler.getInstance().okclient.newCall(request).execute();

                JSONObject json = new JSONObject(response.body().string());
                success = !json.getBoolean("Error"); //Error = true if contains error
                //success = response.isSuccessful();
                Timber.d("Successful upload: " + success + " , " + json.getString("ErrorDesc"));
                if(success){
                    Timber.d("Successfully removed medic " + date.toString() + " from unsynced");
                    UnsyncedData.medicationList.remove(date);
                }

                response.body().close();
            } catch(JSONException ex) {
                Timber.e(ex.getMessage(), ex);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            } catch (NullPointerException ne){
                ne.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void res){

        }

    }
}
