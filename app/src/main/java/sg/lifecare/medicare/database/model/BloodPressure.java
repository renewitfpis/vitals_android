package sg.lifecare.medicare.database.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import sg.lifecare.medicare.MediCareApplication;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.PatientData;
import timber.log.Timber;

/**
 * Medication data
 */
public class BloodPressure extends RealmObject {

    @PrimaryKey
    private long id;
    private int unit;
    private float systolic;
    private float distolic;
    private float arterialPressure;
    private float pulseRate;
    private Date date;
    private String entityId;
    private String eventId;

    public BloodPressure() {
    }

    public BloodPressure(float systolic, float distolic, float arterialPressure,
                         float pulseRate, Date date, int unit) {
        this.systolic = systolic;
        this.distolic = distolic;
        this.arterialPressure = arterialPressure;
        this.pulseRate = pulseRate;
        this.date = date;
        this.unit = unit;
    }

    public long getId() {
        return id;
    }

    public int getUnit() { return unit; }

    public float getSystolic() {
        return systolic;
    }

    public float getDistolic() { return distolic; }

    public float getArterialPressure() { return arterialPressure; }

    public float getPulseRate() { return pulseRate; }

    public Date getDate() {
        return date;
    }

    public String getStringDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_DISPLAY_FORMAT);
        return sdf.format(date);
    }

    public String getRemark() {
        return "";
    }

    public String getStringUnit() {
        if(unit == 0)
            return MediCareApplication.getContext().getResources().getString(R.string.display_unit_blood_pressure_si);
        else
            return MediCareApplication.getContext().getResources().getString(R.string.display_unit_blood_pressure_imperial);
    }

    public String getEntityId(){
        return entityId;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT, Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));

        try {
            Date d = sdf.parse(date);
            setDate(d);
        } catch (ParseException e) {
            Timber.e(e.getMessage(), e);
        }
    }

    public void setUnit(int unit) { this.unit = unit; }

    public void setSystolic(float systolic) { this.systolic = systolic; }

    public void setDistolic(float distolic) { this.distolic = distolic; }

    public void setArterialPressure(float arterialPressure) {
        this.arterialPressure = arterialPressure;
    }

    public void setPulseRate(float pulseRate) {
        this.pulseRate = pulseRate;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}
