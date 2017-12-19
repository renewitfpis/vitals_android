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
 * Temperature data
 */
public class Temperature extends RealmObject {

    @PrimaryKey
    private long id;
    private double value;
    private int unit;
    private Date date;
    private String remark;
    private String entityId;
    private String eventId;

    public Temperature() {
    }

    public long getId() {
        return id;
    }

    public int getUnit() { return unit; }

    public Date getDate() {
        return date;
    }

    public String getStringDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_DISPLAY_FORMAT, Locale.ENGLISH);
        return sdf.format(date);
    }

    public double getValue() {
        return value;
    }

    public String getStringUnit() {
        if(unit == 0) return MediCareApplication.getContext().getResources().getString(R.string.display_unit_temperature_celsius);
        else return MediCareApplication.getContext().getResources().getString(R.string.display_unit_temperature_fahrenheit);
    }

    public String getRemark() {
        return remark;
    }

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

    public void setStringUnit(String unit){
        if(unit.equalsIgnoreCase(MediCareApplication.getContext().
                getResources().getString(R.string.display_unit_temperature_celsius))) {
            this.unit = 0;
        }
        else {
            this.unit = 1;
        }
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}
