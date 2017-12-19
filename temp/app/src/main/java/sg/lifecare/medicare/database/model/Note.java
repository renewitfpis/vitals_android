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

public class Note extends RealmObject {

    @PrimaryKey
    private long id;
    private String note;
    private Date date;
    private String entityId;
    private String eventId;

    public Note() {
    }

    public Note(String note, Date date) {
        this.note = note;
        this.date = date;
    }

    public long getId() {
        return id;
    }

    public String getNote() {
        return note;
    }

    public Date getDate() {
        return date;
    }

    public String getStringDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_DISPLAY_FORMAT, Locale.ENGLISH);
        return sdf.format(date);
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

    public void setNote(String note) { this.note = note; }

    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}
