package sg.lifecare.medicare.database.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import sg.lifecare.medicare.MediCareApplication;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.PatientData;
import timber.log.Timber;

/**
 * Weight data
 */
public class Weight extends RealmObject {

    @PrimaryKey
    private long id;
    private int unit;
    private double weight;
    private Date date;
    private String entityId;
    private String eventId;

    public Weight() {
    }

    public Weight(double weight, Date date, int unit) {
        this.weight = weight;
        this.date = date;
        this.unit = unit;
    }

    public long getId() {
        return id;
    }

    public int getUnit() { return unit; }

    public double getWeight() {
        return weight;
    }

    public Date getDate() {
        return date;
    }

    public String getStringDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_DISPLAY_FORMAT);
        return sdf.format(date);
    }

    public String getStringUnit() {
        if(unit == 0)
            return MediCareApplication.getContext().getResources().getString(R.string.display_unit_weight_si);
        else
            return MediCareApplication.getContext().getResources().getString(R.string.display_unit_weight_imperial);
    }

    public String getEntityId() { return entityId; }

    public void setId(long id) {
        this.id = id;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));

        try {
            Date d = sdf.parse(date);
            setDate(d);
        } catch (ParseException e) {
            Timber.e(e.getMessage(), e);
        }
    }

    public void setUnit(int unit) { this.unit = unit; }

    public void setWeight(double weight) { this.weight = weight; }

    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}
