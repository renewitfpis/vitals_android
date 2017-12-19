package sg.lifecare.medicare.object;

import java.util.Date;

import sg.lifecare.medicare.database.model.Step;

public class OverviewItem {
    private String value;
    private int unit;
    private int type;
    private int state;
    private Date date;
    private boolean isDevicePaired;

    public int getType(){
        return type;
    }

    public String getValue(){
        return value;
    }

    public int getUnit(){
        return unit;
    }

    public Date getDate(){
        return date;
    }

    public boolean isDevicePaired(){
        return isDevicePaired;
    }

    public void setType(int type){
        this.type = type;
    }

    public void setValue(String value){
        this.value = value;
    }

    public void setUnit(int unit){
        this.unit = unit;
    }

    public void setDate(Date date){
        this.date = date;
    }

    public void setDevicePaired(boolean isDevicePaired){
        this.isDevicePaired = isDevicePaired;
    }

    public int getState() {
        return state;
    }

    public String getStringState() {
        if(state == Step.CONNECTED)
            return "Connected";
        else if(state == Step.DISCONNECTED)
            return "Disconnected";
        else if(state == Step.CONNECTING)
            return "Connecting";
        return "";
    }

    public void setState(int state) {
        this.state = state;
    }
}
