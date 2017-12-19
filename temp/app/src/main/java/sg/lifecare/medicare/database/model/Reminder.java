package sg.lifecare.medicare.database.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Reminder extends RealmObject {

    @PrimaryKey
    private long id;
    private int hour;
    private int min;
    private String title;
    private String type;
    private boolean activated;
    private boolean Sunday = false;
    private boolean Monday = false;
    private boolean Tuesday = false;
    private boolean Wednesday = false;
    private boolean Thursday = false;
    private boolean Friday = false;
    private boolean Saturday = false;
    private String entityId;

    public Reminder() {}

    public Reminder(long id, int hour, int min, boolean[] days,
                    String title, String type, boolean activated){
        this.id = id;
        this.hour = hour;
        this.min = min;
        this.title = title;
        this.type = type;
        this.activated = activated;

        setDays(days);
    }

    public boolean checkReminderDay(int id){
        switch(id){
            case 0:
                return Sunday;
            case 1:
                return Monday;
            case 2:
                return Tuesday;
            case 3:
                return Wednesday;
            case 4:
                return Thursday;
            case 5:
                return Friday;
            case 6:
                return Saturday;
            default:
                return false;
        }
    }

    public void setDays(boolean[] days){
        if(days[0])
            Sunday = true;
        if(days[1])
            Monday = true;
        if(days[2])
            Tuesday = true;
        if(days[3])
            Wednesday = true;
        if(days[4])
            Thursday = true;
        if(days[5])
            Friday = true;
        if(days[6])
            Saturday = true;
    }

    public void setId(long id){ this.id = id; }
    public void setHour(int hour) { this.hour = hour; }
    public void setMin(int min) { this.min = min; }
    public void setTitle(String title){ this.title = title; }
    public void setType(String type){ this.type = type; }
    public void setActivated(boolean activated) { this.activated = activated;}
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public long getId(){ return id; }
    public int getHour(){ return hour; }
    public int getMin(){ return min; }
    public String getTitle(){ return title; }
    public String getType(){ return type; }
    public boolean isActivated(){ return activated; }
    public String getEntityId() { return entityId; }




}
