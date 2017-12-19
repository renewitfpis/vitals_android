package sg.lifecare.medicare.database.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import sg.lifecare.medicare.database.PatientData;
import timber.log.Timber;

/**
 * SpO2 data
 */
public class SpO2 extends RealmObject {

    @PrimaryKey
    private long id;
    private int unit;
    private double value;
    private double pulseRate;
    private Date date;
    private String entityId;
    private String eventId;

    public SpO2() {
    }

    public SpO2(double value, double pulseRate, Date date, int unit) {
        this.value = value;
        this.pulseRate = pulseRate;
        this.date = date;
        this.unit = unit;
    }

    public long getId() {
        return id;
    }

    public int getUnit() { return unit; }

    public double getValue() {
        return value;
    }

    public Date getDate() {
        return date;
    }

    public String getStringDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_DISPLAY_FORMAT,Locale.ENGLISH);
        return sdf.format(date);
    }

    public String getStringUnit() {
        return "%";
    }

    public double getPulseRate() { return pulseRate; }

    public String getEntityId() { return entityId; }

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

    public void setValue(double value) { this.value = value; }

    public void setPulseRate(double pulseRate) { this.pulseRate = pulseRate; }

    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}
