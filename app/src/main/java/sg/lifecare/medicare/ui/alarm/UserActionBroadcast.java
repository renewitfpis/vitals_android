package sg.lifecare.medicare.ui.alarm;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import io.realm.Realm;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.PendingReminder;
import sg.lifecare.medicare.database.model.Reminder;

/**
 * Created by wanping on 21/6/16.
 */
public class UserActionBroadcast extends BroadcastReceiver {
    final private static String TAG = "UserActionBroadcast";
    final public static String ACTION = "action";
    final public static int CANCEL = 0;
    final public static int CONFIRM = 1;
    final public static int SNOOZE = 2;


    @Override
    public void onReceive(Context context, Intent intent) {

        long prId = intent.getExtras().getLong("prId",-1);
        int action = intent.getExtras().getInt(UserActionBroadcast.ACTION, -1);
        String actionStr = "";

        switch(action){
            case CANCEL:
                ReminderHandler.getInstance().setAction(prId,UserActionBroadcast.CANCEL);
                actionStr = "Cancel";
                break;

            case CONFIRM:
                ReminderHandler.getInstance().setAction(prId,UserActionBroadcast.CONFIRM);
                actionStr = "Confirm";
                break;

            case SNOOZE:
                ReminderHandler.getInstance().setAction(prId,UserActionBroadcast.SNOOZE);
                actionStr = "Snooze";

                //Retrieve parent reminder from pending reminder ID
                //and set alarm to display after 5 min
                Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
                PendingReminder pendingReminder = PatientData.getInstance().getPendingReminder(realm, prId);
                long parentId = pendingReminder.getParentId();
                Reminder reminder = PatientData.getInstance().getReminder(realm, parentId, pendingReminder.getEntityId());
                AlarmHandler.getInstance().snoozeAlarm(context,reminder,prId);
                realm.close();
                break;

        }

        NotificationManager manager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        manager.cancel(intent.getIntExtra(AlarmNotification.NOTIFICATION_ID, -1));

        Log.d(TAG,"Alarm ON RECEIVE and RESPONDED! " + "\n"
                + "PendingReminder ID : " + prId + ", "
                + "Notification ID : " + intent.getIntExtra(AlarmNotification.NOTIFICATION_ID, -1) + ", "
                + "User Action : " + actionStr + " (Action ID: " + action + ")" );

    }
}
