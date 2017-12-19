package sg.lifecare.medicare.database.model;

import io.realm.RealmObject;

public class Step extends RealmObject {

    public static int DISCONNECTED = 0;
    public static int CONNECTING = 1;
    public static int CONNECTED = 2;

    private int state;
    private int steps;

    public Step() {
        this.state = 0;
        this.steps = 0;
    }

    public int getState(){
        return state;
    }

    public int getSteps(){
        return steps;
    }

    public void setState(int state){
        this.state = state;
    }

    public void setSteps(int steps){
        this.steps = steps;
    }

}
