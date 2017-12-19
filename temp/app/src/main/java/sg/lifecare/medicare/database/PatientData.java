package sg.lifecare.medicare.database;

import android.content.Context;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import io.realm.DynamicRealm;
import io.realm.FieldAttribute;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmMigration;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.RealmSchema;
import io.realm.Sort;
import io.realm.exceptions.RealmMigrationNeededException;
import sg.lifecare.medicare.MediCareApplication;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Medication;
import sg.lifecare.medicare.database.model.Note;
import sg.lifecare.medicare.database.model.PendingReminder;
import sg.lifecare.medicare.database.model.Photo;
import sg.lifecare.medicare.database.model.Reminder;
import sg.lifecare.medicare.database.model.SpO2;
import sg.lifecare.medicare.database.model.SpO2Set;
import sg.lifecare.medicare.database.model.Symptom;
import sg.lifecare.medicare.database.model.Temperature;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.database.model.User;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.database.module.PatientModule;
import timber.log.Timber;

/**
 * Patient data
 */
public class PatientData {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    public static final String DATE_FULL_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String DATE_FULL_FORMAT_UPLOAD = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    public static final String DATE_DISPLAY_FORMAT = "yyyy-MM-dd, hh:mm a";

    public static final String DAY_FORMAT = "EEEE"; //output = Monday, Tuesday...

    public static final String DATE_DAY_FORMAT = "dd MMM, EEE";

    public static final String SIMPLE_DATE_FORMAT = "dd MMM";

    private static String entityId;

    private static PatientData sInstance;

    private final RealmConfiguration mRealmConfig;

    public static PatientData getInstance() {
        if (sInstance == null) {
            sInstance = new PatientData(MediCareApplication.getContext());
        }

        return sInstance;
    }

    private PatientData(Context context) {
        Timber.tag("PatientData");
        Timber.d("PatientData!!!");

        Log.d("Patientdata1","PatientData");
        Realm realm = null;

        mRealmConfig = new RealmConfiguration.Builder()
            .name("patient")
            .modules(new PatientModule())
            .schemaVersion(6)
            .migration(myMigration)
            .build();

        Timber.d("GET SCHEMA VER: " + mRealmConfig.getSchemaVersion());
        Log.d("Patientdata1","GET SCHEMA VER: " + mRealmConfig.getSchemaVersion());
        Realm.setDefaultConfiguration(mRealmConfig);

        // reset realm
        //Realm.deleteRealm(mRealmConfig);

        try {
            realm = Realm.getInstance(mRealmConfig);
            Log.d("Patientdata1","GET REALM VER: " + realm.getVersion());
            PrimaryKeyFactory.getInstance().initialize(realm);
            sample(realm);

            Timber.d("PatientData-Sample!!!");

        } catch (RealmMigrationNeededException e) {
            Timber.e(e.getMessage(), e);
        }

        if(realm!=null){
            realm.close();
        }

        //get Unsynced Data

    }

    // Example migration adding a new class
    RealmMigration myMigration = new RealmMigration() {
        @Override
        public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
            Timber.d("RealmMigrating from oldVer. " + oldVersion + " to newVer. " + newVersion);
            Log.d("PatientData1","RealmMigrating from oldVer. " + oldVersion + " to newVer. " + newVersion);
            // DynamicRealm exposes an editable schema
            RealmSchema schema = realm.getSchema();

            // Migrate to version 1: Add a new class.
            // Example:
            // public Person extends RealmObject {
            //     private String name;
            //     private int age;
            //     // getters and setters left out for brevity
            // }
           /* if (oldVersion == 0) {
                schema.get("User")
                        .addField("entityId", String.class);
                oldVersion++;
            }else*/
            if(oldVersion == 0){
                schema.create("Temperature")
                        .addField("id",long.class, FieldAttribute.PRIMARY_KEY)
                        .addField("value",double.class)
                        .addField("unit",int.class)
                        .addField("date",Date.class)
                        .addField("remark",String.class)
                        .addField("entityId",String.class);
                oldVersion++;
            }
            if(oldVersion == 1){
                schema.create("Note")
                        .addField("id",long.class, FieldAttribute.PRIMARY_KEY)
                        .addField("note",String.class)
                        .addField("date",Date.class)
                        .addField("entityId",String.class);
                oldVersion++;
            }

            if(oldVersion == 2){
                schema.create("SpO2")
                        .addField("id",long.class, FieldAttribute.PRIMARY_KEY)
                        .addField("unit",int.class)
                        .addField("value",double.class)
                        .addField("pulseRate",double.class)
                        .addField("date",Date.class)
                        .addField("entityId",String.class);

                schema.create("SpO2Set")
                        .addField("id",long.class, FieldAttribute.PRIMARY_KEY)
                        .addField("startDate",Date.class)
                        .addField("endDate",Date.class)
                        .addField("entityId",String.class);
                oldVersion++;
            }

            if(oldVersion == 3){
                schema.get("User")
                        .addField("imgUrl", String.class);
                oldVersion++;
            }

            if(oldVersion == 4){
                schema.get("User")
                        .addField("firstName", String.class)
                        .addField("lastName", String.class);
                oldVersion++;
            }

            if(oldVersion == 5){
                schema.get("Terumo").addField("eventId", String.class);
                schema.get("BloodPressure").addField("eventId", String.class);
                schema.get("Weight").addField("eventId", String.class);
                schema.get("Temperature").addField("eventId", String.class);
                schema.get("SpO2").addField("eventId", String.class);
                schema.get("SpO2Set").addField("eventId", String.class);
                schema.get("Medication").addField("eventId", String.class);
                schema.get("Photo").addField("eventId", String.class);
                schema.get("Symptom").addField("eventId", String.class);
                schema.get("Note").addField("eventId", String.class);
                oldVersion++;
            }
        }
    };

    public RealmConfiguration getRealmConfig() {
        return mRealmConfig;
    }

    public void setEntityId(String entityId){ this.entityId = entityId; }

    public long getTerumoCount(Realm realm) {
        return realm.where(Terumo.class).count();
    }

    public long getSymptomCount(Realm realm) {
        return realm.where(Symptom.class).count();
    }

    public Terumo getTerumoBySpecificDate(Realm realm, Date date) {
        return realm.where(Terumo.class)
                        .equalTo("entityId",entityId)
                        .equalTo("date", date).findFirst();
    }

    public long getTerumoBySpecificDateCount(Realm realm, Date date) {
        return realm.where(Terumo.class)
                .equalTo("entityId",entityId)
                .equalTo("date", date).count();
    }

    public Medication getMedicationBySpecificDate(Realm realm, Date date) {
        return realm.where(Medication.class)
                .equalTo("entityId",entityId)
                .equalTo("date", date).findFirst();
    }

    public Photo getFoodBySpecificDate(Realm realm, Date date) {
        return realm.where(Photo.class)
                .equalTo("entityId",entityId)
                .equalTo("date", date).findFirst();
    }
    public Symptom getSymptomBySpecificDate(Realm realm, Date date) {
        return realm.where(Symptom.class)
                .equalTo("entityId",entityId)
                .equalTo("date", date).findFirst();
    }

    public BloodPressure getBloodPressureBySpecificDate(Realm realm, Date date) {
        return realm.where(BloodPressure.class)
                .equalTo("entityId",entityId)
                .equalTo("date", date).findFirst();
    }

    public Weight getWeightBySpecificDate(Realm realm, Date date) {
        return realm.where(Weight.class)
                .equalTo("entityId",entityId)
                .equalTo("date", date).findFirst();
    }

    public Temperature getTempBySpecificDate(Realm realm, Date date) {
        return realm.where(Temperature.class)
                .equalTo("entityId",entityId)
                .equalTo("date", date).findFirst();
    }

    public RealmResults<Terumo> getAllTerumo(Realm realm){
        RealmResults<Terumo> terumos = realm.where(Terumo.class)
                .equalTo("entityId",entityId)
                .findAllSorted("date");
        return terumos;
    }

    public RealmResults<Terumo> getLast30DaysTerumo(Realm realm){
        Calendar startDate = Calendar.getInstance();
        startDate.add(Calendar.DAY_OF_MONTH, -30);

        RealmResults<Terumo> terumos = realm.where(Terumo.class)
                .equalTo("entityId",entityId)
                .greaterThanOrEqualTo("date",startDate.getTime())
                .findAllSorted("date");
        return terumos;
    }


    public RealmResults<BloodPressure> getAllBloodPressure(Realm realm){
        RealmResults<BloodPressure> bps = realm.where(BloodPressure.class)
                .equalTo("entityId",entityId)
                .findAllSorted("date");
        return bps;
    }

    public RealmResults<Weight> getAllWeight(Realm realm){
        RealmResults<Weight> weights = realm.where(Weight.class)
                .equalTo("entityId",entityId)
                .findAllSorted("date");
        return weights;
    }

    public RealmResults<Temperature> getAllTemperature(Realm realm){
        RealmResults<Temperature> temps = realm.where(Temperature.class)
                .equalTo("entityId",entityId)
                .findAllSorted("date");
        return temps;
    }

    public RealmResults<Terumo> getTerumoByDate(Realm realm, Calendar date) {
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        Timber.d("EntityId = " + entityId);
        RealmResults<Terumo> terumos =
            realm.where(Terumo.class)
                    .equalTo("entityId",entityId)
                    .between("date", startDate, endDate).findAllSorted("date");
        return terumos;
    }

    public Terumo getLastTerumoByDate(Realm realm, Calendar date) {
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<Terumo> rr = realm.where(Terumo.class)
                .equalTo("entityId",entityId)
                .between("date", startDate, endDate)
                .findAllSorted("date",Sort.DESCENDING);
        if(rr.size()>0){
            return rr.first();
        }

        return null;

    }


    public RealmList<Terumo> getLastThreeTerumoByDate(Realm realm, Calendar date) {
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<Terumo> rr = realm.where(Terumo.class)
                .equalTo("entityId",entityId)
                .between("date", startDate, endDate)
                .findAllSorted("date",Sort.DESCENDING);

        RealmList<Terumo> rl = new RealmList<Terumo>();
        if(rr.size()>0){
            for(int i = 0; i < rr.size(); i++){
                if(i < 3){
                    rl.add(rr.get(i));
                }
            }
            return rl;
        }

        return null;
    }

    public RealmResults<Terumo> getTerumoByDateRange(Realm realm, Calendar startCal, Calendar endCal) {
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        Date startDate = startCal.getTime();
        //String start = sdf.format(date.getTime());

        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 0);

        Date endDate = endCal.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<Terumo> terumos =
                realm.where(Terumo.class)
                        .equalTo("entityId",entityId)
                        .between("date", startDate, endDate).findAllSorted("date");
        return terumos;
    }

    public RealmList<Terumo> getOneTerumoPerDayByDateRange(Realm realm, Calendar start, Calendar end) {

        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        startCal.setTime(start.getTime());
        endCal.setTime(end.getTime());

        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 0);

        RealmList<Terumo> terumos = new RealmList<>();
        while(startCal.before(endCal)) {
            Terumo terumo = getLastTerumoByDate(realm, startCal);
            if(terumo!=null) {
                terumos.add(terumo);
            }
            startCal.add(Calendar.DAY_OF_MONTH,1);
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
        }
        return terumos;
    }

    public RealmList<Terumo> getThreeTerumoPerDayByDateRange(Realm realm, Calendar start, Calendar end) {

        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        startCal.setTime(start.getTime());
        endCal.setTime(end.getTime());

        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 0);

        RealmList<Terumo> terumos = new RealmList<>();
        while(startCal.before(endCal)) {
            RealmList<Terumo> terumoListPerDay = getLastThreeTerumoByDate(realm, startCal);
            if(terumoListPerDay!=null){
                for(int i = terumoListPerDay.size()-1; i >= 0; i--)
                    terumos.add(terumoListPerDay.get(i));
            }
            startCal.add(Calendar.DAY_OF_MONTH,1);
        }

        return terumos;
    }

    public Boolean isTerumoExistsByDate(Realm realm, Date date) {

        Terumo terumo = realm.where(Terumo.class)
                .equalTo("entityId",entityId).equalTo("date", date).findFirst();

        if(terumo!=null)
            return true;

        return false;
    }

    public Boolean isBloodPressureExistByDate(Realm realm, Date date) {

        BloodPressure bp = realm.where(BloodPressure.class)
                .equalTo("entityId",entityId).equalTo("date", date).findFirst();

        if(bp!=null)
            return true;

        return false;
    }


    public Boolean isWeightExistByDate(Realm realm, Date date) {

        Weight w = realm.where(Weight.class)
                .equalTo("entityId",entityId).equalTo("date", date).findFirst();

        if(w!=null)
            return true;

        return false;
    }

    public Boolean isTempExistByDate(Realm realm, Date date) {

        Temperature w = realm.where(Temperature.class)
                .equalTo("entityId",entityId).equalTo("date", date).findFirst();

        if(w!=null)
            return true;

        return false;
    }


    public RealmResults<Symptom> getSymtomByDate(Realm realm, Calendar date) {
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<Symptom> symptoms =
            realm.where(Symptom.class)
                    .equalTo("entityId",entityId)
                    .between("date", startDate, endDate).findAllSorted("date");

        return symptoms;
    }

    public static RealmResults<Photo> getPhotoByDate(Realm realm, Calendar date) {
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<Photo> photos =
            realm.where(Photo.class)
                    .equalTo("entityId",entityId)
                    .between("date", startDate, endDate).findAllSorted("date");

        return photos;
    }

    public RealmResults<Medication> getMedicationByDate(Realm realm, Calendar date) {
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<Medication> medications =
                realm.where(Medication.class)
                        .equalTo("entityId",entityId)
                        .between("date", startDate, endDate).findAllSorted("date");

        return medications;
    }

    public RealmResults<BloodPressure> getBloodPressureByDate(Realm realm, Calendar date) {
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<BloodPressure> bps =
                realm.where(BloodPressure.class)
                        .equalTo("entityId",entityId)
                        .between("date", startDate, endDate).findAllSorted("date");

        return bps;
    }

    public BloodPressure getLastBloodPressureByDate(Realm realm, Calendar date) {
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<BloodPressure> rr = realm.where(BloodPressure.class)
                .equalTo("entityId",entityId)
                .between("date", startDate, endDate)
                .findAllSorted("date",Sort.DESCENDING);
        if(rr.size()>0){
            return rr.first();
        }

        return null;

    }

    public RealmList<BloodPressure> getOneBloodPressurePerDayByDateRange(Realm realm, Calendar start, Calendar end) {

        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        startCal.setTime(start.getTime());
        endCal.setTime(end.getTime());

        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 0);

        RealmList<BloodPressure> bps = new RealmList<>();
        while(startCal.before(endCal)) {
            BloodPressure bp = getLastBloodPressureByDate(realm, startCal);
            if(bp!=null) {
                bps.add(bp);
            }
            startCal.add(Calendar.DAY_OF_MONTH,1);
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
        }
        return bps;
    }

    public RealmList<BloodPressure> getLastThreeBloodPressureByDate(Realm realm, Calendar date){
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<BloodPressure> rr = realm.where(BloodPressure.class)
                .between("date", startDate, endDate)
                .equalTo("entityId",entityId)
                .findAllSorted("date",Sort.DESCENDING);
        RealmList<BloodPressure> rl = new RealmList<>();
        if(rr.size()>0){
            for(int i = 0; i < rr.size(); i++){
                if(i < 3){
                    rl.add(rr.get(i));
                }
            }
            return rl;
        }

        return null;
    }

    public RealmList<BloodPressure> getThreeBloodPressurePerDayByDateRange(Realm realm, Calendar start, Calendar end) {

        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        startCal.setTime(start.getTime());
        endCal.setTime(end.getTime());

        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 0);

        RealmList<BloodPressure> bps = new RealmList<>();
        while(startCal.before(endCal)) {
            RealmList<BloodPressure> bpListPerDay = getLastThreeBloodPressureByDate(realm, startCal);
            if(bpListPerDay!=null){
                for(int i = bpListPerDay.size()-1; i >= 0; i--)
                    bps.add(bpListPerDay.get(i));
            }
            startCal.add(Calendar.DAY_OF_MONTH,1);
        }

        return bps;
    }

    public RealmResults<Weight> getWeightByDate(Realm realm, Calendar date) {
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<Weight> weights =
                realm.where(Weight.class)
                        .equalTo("entityId",entityId)
                        .between("date", startDate, endDate).findAllSorted("date");

        return weights;
    }


    public RealmResults<Temperature> getTemperatureByDate(Realm realm, Calendar date) {
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<Temperature> temps =
                realm.where(Temperature.class)
                        .equalTo("entityId",entityId)
                        .between("date", startDate, endDate).findAllSorted("date");

        return temps;
    }


    public RealmResults<SpO2Set> getSpO2SetByDate(Realm realm, Calendar date) {
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<SpO2Set> spO2Sets =
                realm.where(SpO2Set.class)
                        .equalTo("entityId",entityId)
                        .between("startDate", startDate, endDate)
                        .findAllSorted("startDate");

        return spO2Sets;
    }

    public RealmResults<SpO2> getSpO2ByDate(Realm realm, Calendar date) {
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<SpO2> spO2s =
                realm.where(SpO2.class)
                        .equalTo("entityId",entityId)
                        .between("date", startDate, endDate)
                        .findAllSorted("date");

        return spO2s;
    }


    public SpO2Set getSpO2SetBySpecificDate(Realm realm, Date date) {
        RealmResults<SpO2Set> spO2Sets =
                realm.where(SpO2Set.class)
                        .equalTo("entityId",entityId)
                        .equalTo("startDate", date)
                        .findAllSorted("startDate");
        if(spO2Sets!=null && spO2Sets.size()>0)
            return spO2Sets.first();

        return null;
    }

    public RealmResults<Note> getNoteByDate(Realm realm, Calendar date) {
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<Note> notes =
                realm.where(Note.class)
                        .equalTo("entityId",entityId)
                        .between("date", startDate, endDate).findAllSorted("date");

        return notes;
    }

    public Weight getLastWeightByDate(Realm realm, Calendar date) {
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<Weight> rr = realm.where(Weight.class)
                .equalTo("entityId",entityId)
                .between("date", startDate, endDate).
                findAllSorted("date",Sort.DESCENDING);
        if(rr.size()>0){
            return rr.first();
        }

        return null;

    }


    public Temperature getLastTempByDate(Realm realm, Calendar date) {
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<Temperature> rr = realm.where(Temperature.class)
                .equalTo("entityId",entityId)
                .between("date", startDate, endDate).
                        findAllSorted("date",Sort.DESCENDING);
        if(rr.size()>0){
            return rr.first();
        }

        return null;

    }

    public RealmList<Weight> getOneWeightPerDayByDateRange(Realm realm, Calendar start, Calendar end) {

        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        startCal.setTime(start.getTime());
        endCal.setTime(end.getTime());

        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 0);

        RealmList<Weight> weights = new RealmList<>();
        while(startCal.before(endCal)) {
            Weight weight = getLastWeightByDate(realm, startCal);
            if(weight!=null) {
                weights.add(weight);
            }
            startCal.add(Calendar.DAY_OF_MONTH,1);
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
        }
        return weights;
    }

    public RealmList<Temperature> getOneTempPerDayByDateRange(Realm realm, Calendar start, Calendar end) {

        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        startCal.setTime(start.getTime());
        endCal.setTime(end.getTime());

        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 0);

        RealmList<Temperature> temps = new RealmList<>();
        while(startCal.before(endCal)) {
            Temperature temp = getLastTempByDate(realm, startCal);
            if(temp!=null) {
                temps.add(temp);
            }
            startCal.add(Calendar.DAY_OF_MONTH,1);
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
        }
        return temps;
    }

    public RealmList<Weight> getLastThreeWeightByDate(Realm realm, Calendar date){
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<Weight> rr = realm.where(Weight.class)
                .equalTo("entityId",entityId)
                .between("date", startDate, endDate)
                .findAllSorted("date",Sort.DESCENDING);
        RealmList<Weight> rl = new RealmList<>();
        if(rr.size()>0){
            for(int i = 0; i < rr.size(); i++){
                if(i < 3){
                    rl.add(rr.get(i));
                }
            }
            return rl;
        }

        return null;
    }

    public RealmList<Temperature> getLastThreeTempByDate(Realm realm, Calendar date){
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<Temperature> rr = realm.where(Temperature.class)
                .equalTo("entityId",entityId)
                .between("date", startDate, endDate)
                .findAllSorted("date",Sort.DESCENDING);
        RealmList<Temperature> rl = new RealmList<>();
        if(rr.size()>0){
            for(int i = 0; i < rr.size(); i++){
                if(i < 3){
                    rl.add(rr.get(i));
                }
            }
            return rl;
        }

        return null;
    }

    public RealmList<Weight> getThreeWeightPerDayByDateRange(Realm realm, Calendar start, Calendar end) {

        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        startCal.setTime(start.getTime());
        endCal.setTime(end.getTime());

        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 0);

        RealmList<Weight> weights = new RealmList<>();
        while(startCal.before(endCal)) {
            RealmList<Weight> bpListPerDay = getLastThreeWeightByDate(realm, startCal);
            if(bpListPerDay!=null){
                for(int i = bpListPerDay.size()-1; i >= 0; i--)
                    weights.add(bpListPerDay.get(i));
            }
            startCal.add(Calendar.DAY_OF_MONTH,1);
        }

        return weights;
    }

    public RealmList<Temperature> getThreeTempPerDayByDateRange(Realm realm, Calendar start, Calendar end) {

        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        startCal.setTime(start.getTime());
        endCal.setTime(end.getTime());

        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 0);

        RealmList<Temperature> weights = new RealmList<>();
        while(startCal.before(endCal)) {
            RealmList<Temperature> bpListPerDay = getLastThreeTempByDate(realm, startCal);
            if(bpListPerDay!=null){
                for(int i = bpListPerDay.size()-1; i >= 0; i--)
                    weights.add(bpListPerDay.get(i));
            }
            startCal.add(Calendar.DAY_OF_MONTH,1);
        }

        return weights;
    }

    public SpO2 getLastSpO2ByDate(Realm realm, Calendar date) {
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<SpO2> rr = realm.where(SpO2.class)
                .equalTo("entityId",entityId)
                .between("date", startDate, endDate).
                        findAllSorted("date",Sort.DESCENDING);
        if(rr.size()>0){
            return rr.first();
        }

        return null;

    }


    public RealmList<SpO2> getOneSpO2PerDayByDateRange(Realm realm, Calendar start, Calendar end) {

        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        startCal.setTime(start.getTime());
        endCal.setTime(end.getTime());

        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 0);

        RealmList<SpO2> spO2s = new RealmList<>();
        while(startCal.before(endCal)) {
            SpO2 spO2 = getLastSpO2ByDate(realm, startCal);
            if(spO2!=null) {
                spO2s.add(spO2);
            }
            startCal.add(Calendar.DAY_OF_MONTH,1);
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
        }
        return spO2s;
    }


    public RealmList<SpO2> getLastThreeSpO2ByDate(Realm realm, Calendar date){
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Date startDate = date.getTime();
        //String start = sdf.format(date.getTime());

        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 0);

        Date endDate = date.getTime();
        //String end = sdf.format(date.getTime());

        RealmResults<SpO2> rr = realm.where(SpO2.class)
                .equalTo("entityId",entityId)
                .between("date", startDate, endDate)
                .findAllSorted("date",Sort.DESCENDING);
        RealmList<SpO2> rl = new RealmList<>();
        if(rr.size()>0){
            for(int i = 0; i < rr.size(); i++){
                if(i < 3){
                    rl.add(rr.get(i));
                }
            }
            return rl;
        }

        return null;
    }

    public RealmList<SpO2> getThreeSpO2PerDayByDateRange(Realm realm, Calendar start, Calendar end) {

        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        startCal.setTime(start.getTime());
        endCal.setTime(end.getTime());

        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 0);

        RealmList<SpO2> spO2s = new RealmList<>();
        while(startCal.before(endCal)) {
            RealmList<SpO2> bpListPerDay = getLastThreeSpO2ByDate(realm, startCal);
            if(bpListPerDay!=null){
                for(int i = bpListPerDay.size()-1; i >= 0; i--)
                    spO2s.add(bpListPerDay.get(i));
            }
            startCal.add(Calendar.DAY_OF_MONTH,1);
        }

        return spO2s;
    }

    public SpO2 getSpO2BySpecificDate(Realm realm, Date date){
        RealmResults<SpO2> spO2List = realm.where(SpO2.class)
                .equalTo("entityId",entityId)
                .equalTo("date", date)
                .findAll();
        if(spO2List.size()>0){
            return realm.copyFromRealm(spO2List.first());
        }
        return null;
    }

    public SpO2 getHighestSpO2ByDateRange(Realm realm, Date startDate, Date endDate){
        RealmResults<SpO2> spO2List = realm.where(SpO2.class)
                .equalTo("entityId",entityId)
                .greaterThanOrEqualTo("date", startDate)
                .lessThanOrEqualTo("date", endDate)
                .findAllSorted("date",Sort.DESCENDING);//"value" for highest

        if(spO2List.size()>0){
            return realm.copyFromRealm(spO2List.first());
        }
        return null;
    }

    public Reminder getReminder(Realm realm, long id){
        return realm.where(Reminder.class)
                .equalTo("entityId",entityId)
                .equalTo("id",id).findFirst();
    }

    public Reminder getReminder(Realm realm, long id, String entityId){
        return realm.where(Reminder.class)
                .equalTo("entityId",entityId)
                .equalTo("id",id).findFirst();
    }

    public RealmResults<Reminder> getRemindersOfToday(Realm realm, Calendar date) {

        Calendar cal = Calendar.getInstance();

        SimpleDateFormat dateFormat = new SimpleDateFormat(DAY_FORMAT, Locale.ENGLISH);

        String today = dateFormat.format(cal.getTime());

        RealmResults<Reminder> reminders =
                realm.where(Reminder.class)
                        .equalTo("entityId",entityId)
                        .equalTo(today,true)
                        .findAllSorted("hour", Sort.ASCENDING,"min",Sort.ASCENDING);

        for(int i = 0; i < reminders.size(); i++){
            Timber.d("Reminder " + i + " : time = " + reminders.get(i).getHour()
                    + ":" + reminders.get(i).getMin() + " title: " + reminders.get(i).getHour()
            + " default entityid = " + entityId + " reminder entityid = "+ reminders.get(i).getEntityId());
        }

        return reminders;
    }

    public RealmResults<Reminder> getReminders(Realm realm, String entityId) {

        Calendar cal = Calendar.getInstance();

        SimpleDateFormat dateFormat = new SimpleDateFormat(DAY_FORMAT, Locale.ENGLISH);

        String today = dateFormat.format(cal.getTime());

        RealmResults<Reminder> reminders =
                realm.where(Reminder.class)
                        .equalTo("entityId",entityId)
                        .findAllSorted("hour", Sort.ASCENDING,"min",Sort.ASCENDING);

        return reminders;
    }

    public Reminder getReminderFromPendingId(Realm realm, long prId){
        long parentId = realm.where(PendingReminder.class).equalTo("id",prId).findFirst().getParentId();
        return realm.where(Reminder.class).equalTo("id",parentId).findFirst();
    }

    public PendingReminder getPendingReminder(Realm realm, long prId){
        return realm.where(PendingReminder.class).equalTo("id",prId).findFirst();
    }

    public RealmResults<PendingReminder> getPendingReminders(Realm realm) {

        Calendar cal = Calendar.getInstance();

        SimpleDateFormat dateFormat = new SimpleDateFormat(DAY_FORMAT, Locale.ENGLISH);

        String today = dateFormat.format(cal.getTime());

        RealmResults<PendingReminder> reminders =
                realm.where(PendingReminder.class).findAll();

        return reminders;
    }

    public PendingReminder getFirstUnshownPendingReminder(Realm realm) {
        Calendar cal = Calendar.getInstance();

        Log.d("pendingr", Calendar.getInstance().getTime().getHours() + ":"+Calendar.getInstance().getTime().getMinutes());
        RealmResults<PendingReminder> pendingReminders = realm.where(PendingReminder.class)
                .equalTo("entityId",entityId)
                .equalTo("hasShown",false)
                .greaterThan("shownTime",cal.getTime())
                .findAllSorted("shownTime",Sort.ASCENDING);

        if(pendingReminders.size()>0) {
            Log.d("pendingr", "time = " + pendingReminders.first().getShownTime().toString());
            return pendingReminders.first();
        }
        else
            return null;

    }

    public Map<Date, RealmObject> getAllByDate(Realm realm, Calendar date) {
        return getAllByDate(realm, date, false);

    }

    public Map<Date, RealmObject> getAllByDate(Realm realm, Calendar date, boolean descend) {
        RealmResults<Terumo> terumos = getTerumoByDate(realm, date);
        RealmResults<Symptom> symptoms = getSymtomByDate(realm, date);
        RealmResults<Photo> photos = getPhotoByDate(realm, date);
        RealmResults<Medication> medications = getMedicationByDate(realm, date);
        RealmResults<BloodPressure> bps = getBloodPressureByDate(realm, date);
        RealmResults<Weight> weights = getWeightByDate(realm, date);
        RealmResults<Temperature> temps = getTemperatureByDate(realm, date);
        RealmResults<Note> notes = getNoteByDate(realm, date);
        RealmResults<SpO2> spo2s = getSpO2ByDate(realm, date);

        Timber.d("getAllByDate: terumos=" + terumos.size() + ", symptoms=" + symptoms.size() +
                ", photos=" + photos.size() + ", medications="+ medications.size() + ", bps="+bps.size() +
                ", weights=" + weights.size(), ", notes=" + notes.size());

        Map<Date, RealmObject> map;

        if (descend) {
            map = new TreeMap<>(Collections.reverseOrder());
        } else {
            map = new TreeMap<>();
        }

        for (int i = 0; i < terumos.size(); i++) {
            map.put(terumos.get(i).getDate(), terumos.get(i));
        }

        for (int i = 0; i < symptoms.size(); i++) {
            map.put(symptoms.get(i).getDate(), symptoms.get(i));
        }

        for (int i = 0; i < photos.size(); i++) {
            //Timber.d(" Photo: path=" + photos.get(i).getImage());
            map.put(photos.get(i).getDate(), photos.get(i));
        }

        for (int i = 0; i < medications.size(); i++) {
            map.put(medications.get(i).getDate(), medications.get(i));
        }

        for (int i = 0; i < bps.size(); i++) {
            map.put(bps.get(i).getDate(), bps.get(i));
        }
        for (int i = 0; i < weights.size(); i++) {
            map.put(weights.get(i).getDate(), weights.get(i));
        }
        for (int i = 0; i < temps.size(); i++) {
            map.put(temps.get(i).getDate(), temps.get(i));
        }
        for (int i = 0; i < notes.size(); i++) {
            map.put(notes.get(i).getDate(), notes.get(i));
        }
        for (int i = 0; i < spo2s.size(); i++) {
            map.put(spo2s.get(i).getDate(), spo2s.get(i));
        }

        Timber.d("getAllByDate:");
        for (Map.Entry<Date, RealmObject> entry : map.entrySet()) {
            RealmObject ro = entry.getValue();

            if (ro instanceof Terumo) {
                Timber.d("    Terumo : date=" + ((Terumo)ro).getDate());
            } else if (ro instanceof Symptom) {
                Timber.d("   Symptom : date=" + ((Symptom)ro).getDate());
            } else if (ro instanceof Photo) {
                Timber.d("     Photo : date=" + ((Photo)ro).getDate());
            }else if (ro instanceof Medication) {
                Timber.d("Medication : date=" + ((Medication)ro).getDate());
            }else if (ro instanceof BloodPressure) {
                Timber.d("B Pressure : date=" + ((BloodPressure)ro).getDate());
            }else if (ro instanceof Weight) {
                Timber.d("    Weight : date=" + ((Weight)ro).getDate());
            }else if (ro instanceof Temperature) {
                Timber.d("      Temp : date=" + ((Temperature)ro).getDate());
            }else if (ro instanceof Note) {
                Timber.d("      Note : date=" + ((Note)ro).getDate());
            }
        }

        return map;
    }

    public Map<Date, RealmObject> getLatestVitalsData(Realm realm, boolean descend) {
        RealmResults<Terumo> terumoList = realm.where(Terumo.class)
                .equalTo("entityId",entityId)
                .findAllSorted("date",Sort.DESCENDING);

        RealmResults<BloodPressure> bpList = realm.where(BloodPressure.class)
                .equalTo("entityId",entityId)
                .findAllSorted("date",Sort.DESCENDING);

        RealmResults<Weight> weightList = realm.where(Weight.class)
                .equalTo("entityId",entityId)
                .findAllSorted("date",Sort.DESCENDING);

        RealmResults<Temperature> tempList = realm.where(Temperature.class)
                .equalTo("entityId",entityId)
                .findAllSorted("date",Sort.DESCENDING);

        RealmResults<SpO2> spo2List = realm.where(SpO2.class)
                .equalTo("entityId",entityId)
                .findAllSorted("date",Sort.DESCENDING);

        Map<Date, RealmObject> map;

        if (descend) {
            map = new TreeMap<>(Collections.reverseOrder());
        } else {
            map = new TreeMap<>();
        }

        if(terumoList.size()>0) {
            Terumo terumo = terumoList.first();
            map.put(terumo.getDate(), terumo);
        }
        if(bpList.size()>0){
            BloodPressure bp = bpList.first();
            map.put(bp.getDate(), bp);
        }
        if(weightList.size()>0) {
            Weight weight = weightList.first();
            map.put(weight.getDate(), weight);
        }
        if(tempList.size()>0) {
            Temperature temp = tempList.first();
            map.put(temp.getDate(), temp);
        }
        if(spo2List.size()>0) {
            SpO2 spO2 = spo2List.first();
            map.put(spO2.getDate(), spO2);
        }

        Timber.d("getAllByDate:");
        for (Map.Entry<Date, RealmObject> entry : map.entrySet()) {
            RealmObject ro = entry.getValue();

            if (ro instanceof Terumo) {
                Timber.d("    Terumo : date=" + ((Terumo)ro).getDate());
            } else if (ro instanceof BloodPressure) {
                Timber.d("B Pressure : date=" + ((BloodPressure)ro).getDate());
            } else if (ro instanceof Weight) {
                Timber.d("    Weight : date=" + ((Weight)ro).getDate());
            }
        }

        return map;
    }

    public void addTerumo(Realm realm, Terumo terumo) {
        if(terumo==null){
            Timber.w("TERUMO IS NULL");
            return;
        }

        long count = realm.where(Terumo.class)
                .equalTo("date", terumo.getDate())
                .equalTo("entityId",terumo.getEntityId())
                .count();

        if (count > 0) {
            Terumo existingTerumo = realm.where(Terumo.class)
                    .equalTo("date", terumo.getDate())
                    .equalTo("entityId",terumo.getEntityId()).findFirst();
            terumo.setId(existingTerumo.getId());
        }else {
            terumo.setId(PrimaryKeyFactory.getInstance().nextKey(Terumo.class));
        }
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(terumo);
        realm.commitTransaction();
    }

    public Date getTerumoLatestDate(Realm realm) {
        Terumo latestRecord =realm.where(Terumo.class)
                .equalTo("entityId",entityId).findAllSorted("date",Sort.DESCENDING).first();
        return latestRecord.getDate();
    }

    public void addSymptoms(Realm realm, Symptom symptom) {
        long count = realm.where(Symptom.class)
                .equalTo("date", symptom.getDate())
                .equalTo("entityId",symptom.getEntityId())
                .count();

        if (count > 0) {
            Symptom existingSymptom = realm.where(Symptom.class)
                    .equalTo("date", symptom.getDate())
                    .equalTo("entityId",symptom.getEntityId())
                    .findFirst();
            symptom.setId(existingSymptom.getId());
        }else {
            symptom.setId(PrimaryKeyFactory.getInstance().nextKey(Symptom.class));
        }
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(symptom);
        realm.commitTransaction();
    }

    public void addNote(Realm realm, Note note) {
        long count = realm.where(Note.class).equalTo("date", note.getDate()).count();

        if (count > 0) {
            Timber.d("addNote: duplicate data " + note.getDate());
            return;
        }

        realm.beginTransaction();
        note.setId(PrimaryKeyFactory.getInstance().nextKey(Note.class));
        realm.copyToRealm(note);
        realm.commitTransaction();
    }

    public void addPhoto(Realm realm, Photo photo) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FULL_FORMAT);
        long count = realm.where(Photo.class)
                .equalTo("date", photo.getDate())
                .equalTo("entityId",photo.getEntityId())
                .count();

        if (count > 0) {
            Photo existingPhoto = realm.where(Photo.class)
                    .equalTo("date", photo.getDate())
                    .equalTo("entityId",photo.getEntityId())
                    .findFirst();
            photo.setId(existingPhoto.getId());
        }else {
            photo.setId(PrimaryKeyFactory.getInstance().nextKey(Photo.class));
        }
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(photo);
        realm.commitTransaction();
    }

    public void addMedication(Realm realm, Medication medication) {
        long count = realm.where(Medication.class)
                .equalTo("date", medication.getDate())
                .equalTo("entityId",medication.getEntityId())
                .count();

        if (count > 0) {
            Medication existingPhoto = realm.where(Medication.class)
                    .equalTo("date", medication.getDate())
                    .equalTo("entityId",medication.getEntityId()).findFirst();
            medication.setId(existingPhoto.getId());
        }else {
            medication.setId(PrimaryKeyFactory.getInstance().nextKey(Medication.class));
        }
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(medication);
        realm.commitTransaction();
    }

    public void addReminder(Realm realm, Reminder reminder) {
        long count = realm.where(Reminder.class)
                .equalTo("hour", reminder.getHour())
                .equalTo("min", reminder.getMin())
                .equalTo("title", reminder.getTitle())
                .equalTo("entityId",reminder.getEntityId())
                .count();

        if (count > 0) {
            Timber.d("addReminder: duplicate data " + reminder.getTitle() + ", " + reminder.getHour()
                    +":" + reminder.getMin());
            return;
        }
        Timber.d("added reminder- " + reminder.getTitle() + ", " + reminder.getHour()
                +":" + reminder.getMin() + ", " + reminder.getEntityId());
        realm.beginTransaction();
        realm.copyToRealm(reminder);
        realm.commitTransaction();
    }

    public void addBloodPressure(Realm realm, BloodPressure bloodPressure) {
        long count = realm.where(BloodPressure.class)
                .equalTo("date", bloodPressure.getDate())
                .equalTo("entityId",bloodPressure.getEntityId())
                .count();

        if (count > 0) {
            BloodPressure existingBp = realm.where(BloodPressure.class)
                    .equalTo("date", bloodPressure.getDate())
                    .equalTo("entityId",bloodPressure.getEntityId()).findFirst();
            bloodPressure.setId(existingBp.getId());
        }else {
            bloodPressure.setId(PrimaryKeyFactory.getInstance().nextKey(BloodPressure.class));
        }
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(bloodPressure);
        realm.commitTransaction();
    }

    public void addWeight(Realm realm, Weight weight) {
        long count = realm.where(Weight.class)
                .equalTo("date", weight.getDate())
                .equalTo("entityId",weight.getEntityId())
                .count();

        if (count > 0) {
            Weight existingWeight = realm.where(Weight.class)
                    .equalTo("date", weight.getDate())
                    .equalTo("entityId",weight.getEntityId()).findFirst();
            weight.setId(existingWeight.getId());
        }else {
            weight.setId(PrimaryKeyFactory.getInstance().nextKey(Weight.class));
        }
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(weight);
        realm.commitTransaction();
    }

    public String updateUserLatestWeightProfile(Realm realm, String entityId){
        User user = realm.where(User.class).equalTo("entityId",entityId).findFirst();

        Timber.d("BEGIN CHANGE WEIGHT -entityid = " + entityId);
        RealmResults<Weight> weights = realm.where(Weight.class)
                .equalTo("entityId",entityId)
                .findAllSorted("date", Sort.DESCENDING);

        if(weights!=null && weights.size()>0) {
            Timber.d("BEGIN CHANGE WEIGHT " + weights.first().getWeight());
            realm.beginTransaction();
            user.setWeight(weights.first().getWeight()+"");
            realm.copyToRealmOrUpdate(user);
            realm.commitTransaction();
            return weights.first().getWeight()+"";
        }
        return "";
    }
    public void addTemperature(Realm realm, Temperature temperature) {
        long count = realm.where(Temperature.class)
                .equalTo("date", temperature.getDate())
                .equalTo("entityId",temperature.getEntityId())
                .count();

        if (count > 0) {
            Temperature existingTemp = realm.where(Temperature.class)
                    .equalTo("date", temperature.getDate())
                    .equalTo("entityId",temperature.getEntityId()).findFirst();
            temperature.setId(existingTemp.getId());
        }else {
            temperature.setId(PrimaryKeyFactory.getInstance().nextKey(Temperature.class));
        }
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(temperature);
        realm.commitTransaction();

    }

    public void addSpO2(Realm realm, SpO2 spO2) {
        long count = realm.where(SpO2.class)
                .equalTo("date", spO2.getDate())
                .equalTo("entityId",spO2.getEntityId())
                .count();

        if (count > 0) {
            SpO2 existingSpO2 = realm.where(SpO2.class)
                    .equalTo("date", spO2.getDate())
                    .equalTo("entityId",spO2.getEntityId()).findFirst();
            spO2.setId(existingSpO2.getId());
        }else {
            spO2.setId(PrimaryKeyFactory.getInstance().nextKey(SpO2.class));
        }
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(spO2);
        realm.commitTransaction();
    }

    public void addSpO2Set(Realm realm, SpO2Set spO2Set) {
        long count = realm.where(SpO2Set.class)
                .equalTo("startDate", spO2Set.getStartDate())
                .equalTo("entityId",spO2Set.getEntityId())
                .count();

        if (count > 0) {
            SpO2Set existingSpO2 = realm.where(SpO2Set.class)
                    .equalTo("startDate", spO2Set.getStartDate())
                    .equalTo("entityId",spO2Set.getEntityId())
                    .findFirst();
            spO2Set.setId(existingSpO2.getId());
        }else {
            spO2Set.setId(PrimaryKeyFactory.getInstance().nextKey(SpO2Set.class));
        }
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(spO2Set);
        realm.commitTransaction();
    }


    public RealmResults<PendingReminder> getAllPendingReminders(Realm realm){
        RealmResults<PendingReminder> row = realm.where(PendingReminder.class)
                .equalTo("entityId",entityId).findAll();
        return row;
    }

    public void removeReminder(Realm realm, Reminder reminder) {
        RealmResults<Reminder> row = realm.where(Reminder.class)
                .equalTo("id",reminder.getId()).findAll();
        for(int i = 0; i < row.size(); i++){
            Log.d("patientdata","going to delete = " + row.get(i).getHour() + ":" + row.get(i).getMin());
        }
        realm.beginTransaction();
        row.deleteAllFromRealm();
        realm.commitTransaction();
    }

    public void removeReminder(Realm realm, RealmResults<Reminder> reminderList, int position) {
        realm.beginTransaction();
        reminderList.get(position).deleteFromRealm();
        realm.commitTransaction();
    }

    public void addPendingReminder(Realm realm, PendingReminder pendingReminder) {
        Log.d("pending","add pending reminder1");
        realm.beginTransaction();
        realm.copyToRealm(pendingReminder);
        realm.commitTransaction();
        Log.d("pending","add pending reminder2");
    }

    public void removePendingReminder(Realm realm, PendingReminder pendingReminder) {
        RealmResults<PendingReminder> row = realm.where(PendingReminder.class).
                equalTo("id",pendingReminder.getId()).findAll();
        realm.beginTransaction();
        row.deleteAllFromRealm();
        realm.commitTransaction();
    }

    public void removePendingWithParentId(Realm realm, long parentId) {
        RealmResults<PendingReminder> row = realm.where(PendingReminder.class).
                equalTo("parentId",parentId).equalTo("hasShown",false).findAll();
        realm.beginTransaction();
        row.deleteAllFromRealm();
        realm.commitTransaction();
    }

    public void editPending(Realm realm, PendingReminder pendingReminder) {
       PendingReminder pr = realm.where(PendingReminder.class).
                equalTo("parentId",pendingReminder.getParentId()).equalTo("hasShown",false).findFirst();
        realm.beginTransaction();
        pr.setId(pendingReminder.getId());
        pr.setHour(pendingReminder.getHour());
        pr.setMin(pendingReminder.getMin());
        pr.setEntityId(pendingReminder.getEntityId());
        realm.commitTransaction();
    }

    public void setReminderShown(Realm realm, long prId, boolean shown) {
        PendingReminder pendingReminder = realm.where(PendingReminder.class)
                .equalTo("id",prId)
                .equalTo("hasShown",false).findFirst();

        if(pendingReminder!=null) {
            realm.beginTransaction();
            pendingReminder.setShown(shown);
            realm.commitTransaction();
        }
    }

    public void setUserAction(Realm realm, long prId, int action) {
        PendingReminder pendingReminder = realm.where(PendingReminder.class)
                .equalTo("id",prId).findFirst();
        if(pendingReminder!=null) {
            realm.beginTransaction();
            pendingReminder.setAction(action);
            realm.commitTransaction();
        }
    }

    /**
     * Add user information (only one user supported)
     * @param realm         realm
     * @param user          user
     */
    /*public void addUser(Realm realm, User user) {
        RealmResults<User> results = realm.where(User.class).findAll();

        realm.beginTransaction();
        if (results.size() > 0) {
            Timber.d("addUser: found " + results.size() + ", delete");
            for (int i = 0; i < results.size(); i++) {
                results.get(i).deleteFromRealm();
            }
        }

        user.setId(PrimaryKeyFactory.getInstance().nextKey(User.class));
        realm.copyToRealm(user);
        realm.commitTransaction();
    }*/

    /**
     * Add user information (multiple user)
     * @param realm         realm
     * @param user          user
     */
    public void addUser(Realm realm, User user) {
        RealmResults<User> results = realm.where(User.class)
                .equalTo("entityId",user.getEntityId()).findAll();

        realm.beginTransaction();
        if (results.size() > 0) {
            Timber.d("addUser: found existing record of " + user.getEntityId() + ", delete");
        }

        user.setId(PrimaryKeyFactory.getInstance().nextKey(User.class));
        realm.copyToRealm(user);
        realm.commitTransaction();
    }

    private void sample(Realm realm) {
   /*     Timber.d("SAMPLE");
        Photo photo = new Photo();
        photo.setValue(20.0);
        photo.setEntityId(entityId);
        photo.setRemark("Test123");
        photo.setDate("2016-08-09T09:53:48.097Z");
        photo.setImage("http://res.cloudinary.com/astralink-technology/image/upload/v1470304390/mobile_image/NI7ISP63-7KMK3V5F-1MMSB2LR_20160804_1753.jpg");
        addPhoto(realm, photo);


        Symptom symptom = new Symptom();
        symptom.setId(PrimaryKeyFactory.getInstance().nextKey(Symptom.class));
        symptom.setSymptoms("1,2,3");
        symptom.setRemark("Not feeling well suddenly");
        symptom.setDate("2016-08-20T10:50:48.097Z");
        symptom.setEntityId(entityId);
        addSymptoms(realm,symptom);*/
//
//        terumo = new Terumo();
//        terumo.setBeforeMeal(true);
//        terumo.setValue(4.8);
//        terumo.setDate("2016-06-06T05:14:23");
//        addTerumo(realm, terumo);
//
//        terumo = new Terumo();
//        terumo.setBeforeMeal(true);
//        terumo.setValue(4.8);
//        terumo.setDate("2016-06-05T06:19:23");
//        addTerumo(realm, terumo);
//
//        terumo = new Terumo();
//        terumo.setBeforeMeal(true);
//        terumo.setValue(4.8);
//        terumo.setDate("2016-06-07T07:19:23");
//        addTerumo(realm, terumo);
//
//        terumo = new Terumo();
//        terumo.setBeforeMeal(true);
//        terumo.setValue(4.4);
//        terumo.setDate("2016-06-05T13:22:23");
//        addTerumo(realm, terumo);
//
//        terumo = new Terumo();
//        terumo.setBeforeMeal(true);
//        terumo.setValue(4.4);
//        terumo.setDate("2016-06-0T08:02:23");
//        addTerumo(realm, terumo);
//
//        terumo = new Terumo();
//        terumo.setBeforeMeal(true);
//        terumo.setValue(4.4);
//        terumo.setDate("2016-06-10T11:18:23");
//        addTerumo(realm, terumo);
//
//        terumo = new Terumo();
//        terumo.setBeforeMeal(true);
//        terumo.setValue(4.4);
//        terumo.setDate("2016-06-10T15:11:23");
//        addTerumo(realm, terumo);
//
//        terumo = new Terumo();
//        terumo.setBeforeMeal(true);
//        terumo.setValue(4.0);
//        terumo.setDate("2016-06-13T10:12:23");
//        addTerumo(realm, terumo);
//
//        terumo = new Terumo();
//        terumo.setBeforeMeal(true);
//        terumo.setValue(3.8);
//        terumo.setDate("2016-06-13T14:12:23");
//        addTerumo(realm, terumo);
//
//        terumo = new Terumo();
//        terumo.setBeforeMeal(true);
//        terumo.setValue(4.5);
//        terumo.setDate("2016-06-13T19:12:23");
//        addTerumo(realm, terumo);
//
//        Symptom symptom = new Symptom();
//        symptom.setDate("2016-06-06T01:20:23");
//        symptom.setRemark("what la");
//        symptom.setSymptoms("t,f,t,f");
//        addSymptoms(realm, symptom);
//
//        Calendar cal = Calendar.getInstance();
//        BloodPressure bp = new BloodPressure(80,60,50,78,cal.getTime(),0);
//        bp.setDate("2016-07-15T13:33:23");
//        addBloodPressure(realm,bp);


       /* Calendar cal = Calendar.getInstance();
        BloodPressure bp = new BloodPressure(80,60,50,78,cal.getTime(),0);
        addBloodPressure(realm,bp);

        bp = new BloodPressure(90,72,10,40,cal.getTime(),0);
        bp.setDate("2016-07-06T01:22:23");
        addBloodPressure(realm,bp);

        Weight weight = new Weight(50.2,cal.getTime(),0);
        weight.setDate("2016-07-07T03:22:23");
        addWeight(realm,weight);

        weight = new Weight(60,cal.getTime(),0);
        weight.setDate("2016-07-08T04:21:23");
        addWeight(realm,weight);

        weight = new Weight(42.8,cal.getTime(),0);
        weight.setDate("2016-07-06T21:21:23");
        addWeight(realm,weight);*/
      /*  User user = new User();
        user.setName("Wong Fei Hong");
        user.setHeight("160");
        user.setMale();
        user.setWeight("100");
        user.setEmail("wongfeihong@astralink.com.sg");
        user.setYear("1970");
        addUser(realm, user);*/

    }

}
