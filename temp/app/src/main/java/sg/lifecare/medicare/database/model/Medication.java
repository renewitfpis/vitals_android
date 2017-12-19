package sg.lifecare.medicare.database.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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
public class Medication extends RealmObject {

    @PrimaryKey
    private long id;
    private double dosage;
    private int type;
    private int unit;
    private Date date;
    private String remark;
    private String entityId;
    private String eventId;

    public Medication() {
    }

    public long getId() {
        return id;
    }

    public double getDosage() {
        return dosage;
    }

    public int getUnit() { return unit; }

    public int getType() { return type; }

    public Date getDate() {
        return date;
    }

    public String getStringDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_DISPLAY_FORMAT);
        return sdf.format(date);
    }

    public String getStringUnit() {
        List<String> unitList = Arrays.asList(
                MediCareApplication.getContext().getResources().getStringArray(R.array.medication_unit_names)
        );
        return unitList.get(unit);
    }

    public String getStringType() {
        List<String> medicList = Arrays.asList(
                MediCareApplication.getContext().getResources().getStringArray(R.array.medication)
        );
        return medicList.get(type);
    }

    public String getRemark() {
        return remark;
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

    public void setType(int type) { this.type = type; }

    public void setStringType(String typeName){
        List<String> medicList = Arrays.asList(
                MediCareApplication.getContext().getResources().getStringArray(R.array.medication)
        );

        for(int i = 0; i < medicList.size(); i++){
            if(medicList.get(i).equalsIgnoreCase(typeName)){
                this.type = i;
            }
        }

    }

    public void setDosage(double dosage) {
        this.dosage = dosage;
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
