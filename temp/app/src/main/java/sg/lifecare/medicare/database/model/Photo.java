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
 * Food photo
 */
public class Photo extends RealmObject {

    @PrimaryKey
    private long id;
    private Double value;
    private Date date;
    private String remark;
    private String image;
    private String entityId;
    private String eventId;

    public Photo() {
        value = 0d;
        remark = "";
    }

    public long getId() {
        return id;
    }

    public Double getValue() {
        return value;
    }

    public Date getDate() {
        return date;
    }

    public String getRemark() {
        return remark;
    }

    public String getImage() {
        return this.image;
    }

    public String getEntityId(){
        return entityId;
    }

    public String getStringDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_DISPLAY_FORMAT, Locale.ENGLISH);
        return sdf.format(date);
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setValue(Double value) {
        this.value = value;
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
            Timber.d("DATE SET");
        } catch (ParseException e) {

            Timber.d("DATE NOT SET");
            Timber.e("PROB PARSING DATE = " + e.getMessage());
        }
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public void setEntityId(String entityId) { this.entityId = entityId; }

    public boolean isUrl() {
        return image.startsWith("http://") || image.startsWith("https://");
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}
