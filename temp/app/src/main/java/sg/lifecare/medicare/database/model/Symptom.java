package sg.lifecare.medicare.database.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import sg.lifecare.medicare.database.PatientData;
import timber.log.Timber;

/**
 * Symptoms
 */
public class Symptom extends RealmObject {

    @PrimaryKey
    private long id;
    private Date date;
    private String symptoms;
    private String remark;
    private String entityId;
    private String eventId;

    public Symptom() {}

    public long getId() {
        return id;
    }

    public Date getDate() {
        return date;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public String getRemark() {
        return remark;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));

        try {
            Date d = sdf.parse(date);
            setDate(d);
            Timber.d("DATE SET");
        } catch (ParseException e) {
            Timber.e(e.getMessage(), e);
        }
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
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
