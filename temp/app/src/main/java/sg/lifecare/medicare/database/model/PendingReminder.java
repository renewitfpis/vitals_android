package sg.lifecare.medicare.database.model;

import java.util.Calendar;
import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import sg.lifecare.medicare.database.PrimaryKeyFactory;

public class PendingReminder extends RealmObject {

    @PrimaryKey
    private long id;
    private int hour;
    private int min;
    private long parentId;
    private String type;
    private boolean hasShown;
    private Date shownTime;
    private int userAction;
    private String entityId;

    public PendingReminder() {}

    public PendingReminder(Reminder reminder){
        this.id = PrimaryKeyFactory.getInstance().nextKey(PendingReminder.class);
        this.hour = reminder.getHour();
        this.min = reminder.getMin();
        this.parentId = reminder.getId();
        this.hasShown = false;

        Calendar reminderCal = Calendar.getInstance();
        reminderCal.set(Calendar.HOUR_OF_DAY,hour);
        reminderCal.set(Calendar.MINUTE,min);
        this.shownTime =reminderCal.getTime();
    }

    public PendingReminder(long id, int hour, int min, long parentId){
        this.id = id;
        this.hour = hour;
        this.min = min;
        this.parentId = parentId;
        this.hasShown = false;

        Calendar reminderCal = Calendar.getInstance();
        reminderCal.set(Calendar.HOUR_OF_DAY,hour);
        reminderCal.set(Calendar.MINUTE,min);
        this.shownTime =reminderCal.getTime();
    }

    public void setId(long id){ this.id= id; }
    public void setHour(int hour) { this.hour = hour; }
    public void setMin(int min) { this.min = min; }
    public void setParentId(long parentId) {this.parentId = parentId; }
    public void setType(String type){ this.type = type; }
    public void setShown(boolean shown) { this.hasShown = shown;}
    public void setShownTime(Date shownTime) { this.shownTime = shownTime;}
    public void setAction(int action) {this.userAction = action;}
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public int getHour(){ return hour; }
    public int getMin(){ return min; }
    public int getUserAction() { return userAction; }
    public long getId(){ return id; }
    public long getParentId(){ return parentId; }
    public boolean getShown() { return hasShown; }
    public String getType(){ return type; }
    public Date getShownTime() { return shownTime; }
    public String getEntityId(){ return entityId; }

}
