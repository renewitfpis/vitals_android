package sg.lifecare.medicare.database.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import sg.lifecare.medicare.database.PatientData;
import timber.log.Timber;

/**
 * SpO2 data
 */
public class SpO2Set extends RealmObject {

    @PrimaryKey
    private long id;
    private Date startDate;
    private Date endDate;
    private String entityId;
    private String eventId;

    public SpO2Set() {
    }

    public SpO2Set(Date startDate, Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public long getId() {
        return id;
    }

    public Date getStartTime() {
        return startDate;
    }

    public String getStringStartDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_DISPLAY_FORMAT,Locale.ENGLISH);
        return sdf.format(startDate);
    }
    public String getStringEndDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_DISPLAY_FORMAT,Locale.ENGLISH);
        return sdf.format(startDate);
    }

    public Date getStartDate() {
        return startDate;
    }
    public Date getEndDate() {
        return endDate;
    }

    public String getEntityId() { return entityId; }

    public void setId(long id) {
        this.id = id;
    }

    public void setStartDate(Date date) {
        this.startDate = date;
    }

    public void setEndDate(Date date) {
        this.endDate = date;
    }

    public void setStringStartDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT, Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));

        try {
            Date d = sdf.parse(date);
            setStartDate(d);
        } catch (ParseException e) {
            Timber.e(e.getMessage(), e);
        }
    }

    public void setStringEndDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FULL_FORMAT, Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));

        try {
            Date d = sdf.parse(date);
            setEndDate(d);
        } catch (ParseException e) {
            Timber.e(e.getMessage(), e);
        }
    }

    public void setEntityId(String entityId) { this.entityId = entityId; }

    public SpO2 getLastSpO2(Realm realm){
        //TODO: get last spo2 data
        return PatientData.getInstance().getSpO2BySpecificDate(realm,endDate);
    }

    public SpO2 getHighestSpO2(Realm realm){
        //TODO: get highest spo2 data
        return PatientData.getInstance().getHighestSpO2ByDateRange(realm,startDate,endDate);
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}
