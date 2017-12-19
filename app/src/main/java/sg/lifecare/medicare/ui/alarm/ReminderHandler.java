package sg.lifecare.medicare.ui.alarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import io.realm.Realm;
import io.realm.RealmResults;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.PendingReminder;
import sg.lifecare.medicare.database.model.Reminder;

/**
 * Created by wanping on 20/6/16.
 */
public class ReminderHandler {

    private static String TAG = "ReminderHandler";

    private static Context context;

    private static ReminderHandler instance;


    private static SharedPreferences prefs;
    private static SharedPreferences.Editor editor;


    public static ReminderHandler getInstance() {
        if (instance == null) {
            instance = new ReminderHandler(context);
        }

        return instance;
    }

    public static void initialize(Context ctx){
        context = ctx;
    }

    private ReminderHandler(Context context){

    }

    public void addToActiveList(PendingReminder pendingReminder){
        Log.d(TAG,"addToActiveList ");
        Realm realm = Realm.getInstance(PatientData.getInstance().getInstance().getRealmConfig());
        PatientData.getInstance().addPendingReminder(realm, pendingReminder);
        realm.close();
    }

    public void removePendingWithParentId(long parentId){
        Log.d(TAG,"removeFromActiveList");
        Realm realm = Realm.getInstance(PatientData.getInstance().getInstance().getRealmConfig());
        PatientData.getInstance().removePendingWithParentId(realm, parentId);
        realm.close();
    }

    public void editPending(PendingReminder pendingReminder){
        Log.d(TAG,"removeFromActiveList");
        Realm realm = Realm.getInstance(PatientData.getInstance().getInstance().getRealmConfig());
        PatientData.getInstance().editPending(realm, pendingReminder);
        realm.close();
    }

    public static void setShown(long prId, boolean shown){
        Realm realm = Realm.getInstance(PatientData.getInstance().getInstance().getRealmConfig());
        PatientData.getInstance().setReminderShown(realm,prId,shown);
        realm.close();

    }

    public static void setAction(long parentId, int action) {
        Realm realm = Realm.getInstance(PatientData.getInstance().getInstance().getRealmConfig());
        PatientData.getInstance().setUserAction(realm, parentId, action);
        realm.close();
    }

    public void addToList(Reminder reminder){

        Realm realm = Realm.getInstance(PatientData.getInstance().getInstance().getRealmConfig());
        PatientData.getInstance().addReminder(realm,reminder);
        realm.close();
    }

    public void removeFromList(Reminder reminder){

        Realm realm = Realm.getInstance(PatientData.getInstance().getInstance().getRealmConfig());
        PatientData.getInstance().removeReminder(realm,reminder);
        realm.close();
    }

    public void removeFromList(RealmResults<Reminder> reminders, int position){

        Realm realm = Realm.getInstance(PatientData.getInstance().getInstance().getRealmConfig());
        PatientData.getInstance().removeReminder(realm,reminders,position);
        realm.close();
    }

    public PendingReminder getFirstUnshownPendingReminder(){
        Realm realm = Realm.getInstance(PatientData.getInstance().getInstance().getRealmConfig());
        PendingReminder pr = PatientData.getInstance().getFirstUnshownPendingReminder(realm);
        realm.close();
        return pr;
    }

    public Reminder getReminderFromPendingId(long prId){
        Realm realm = Realm.getInstance(PatientData.getInstance().getInstance().getRealmConfig());
        Reminder reminder = PatientData.getInstance().getReminderFromPendingId(realm,prId);
        realm.close();
        return reminder;
    }
}
