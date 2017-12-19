package sg.lifecare.medicare.ui.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

import io.realm.Realm;
import io.realm.RealmResults;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.PrimaryKeyFactory;
import sg.lifecare.medicare.database.model.PendingReminder;
import sg.lifecare.medicare.database.model.Reminder;

/*
    Automatically check available alarms of the day and activate them on boot
 */
public class AutoStartAlarm extends BroadcastReceiver
{
    Alarm alarm = new Alarm();

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){

            Log.d("AutoStartAlarm","onReceive");

            autoStartAlarm(context);
        }
    }

    public static void autoStartAlarm(Context context){
        Log.d("AutoStartAlarm","auto start alarm!");

        AlarmHandler.initialize(context);

        Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

        RealmResults<Reminder> reminders = PatientData.getInstance().getRemindersOfToday(realm,Calendar.getInstance());
        Log.d("AutoStartAlarm","reminder size  = " + reminders.size());


        if(reminders!=null && reminders.size()>0) {

            Calendar cal = Calendar.getInstance();
            int today = cal.get(Calendar.DAY_OF_WEEK) - 1;
            int currentHr = cal.get(Calendar.HOUR_OF_DAY);
            int currentMin = cal.get(Calendar.MINUTE);

            for (int i = 0; i < reminders.size(); i++) {

                Reminder reminder = reminders.get(i);
                Log.d("AutoStartAlarm","reminder is activated today? : " + reminder.checkReminderDay(today));
                Log.d("AutoStartAlarm","reminder hour : " + reminder.getHour() + ":" + reminder.getMin());

                if (reminder.checkReminderDay(today) && reminder.isActivated()) {

                    if (reminder.getHour() > currentHr ||
                            reminder.getHour() == currentHr && reminder.getMin() > currentMin) {

                        final long pendingReminderId = PrimaryKeyFactory.getInstance().nextKey(PendingReminder.class);

                        PendingReminder pendingReminder = new PendingReminder(
                                pendingReminderId,reminder.getHour(),reminder.getMin(),reminder.getId()
                        );

                        pendingReminder.setEntityId(reminder.getEntityId());

                        ReminderHandler.getInstance().addToActiveList(pendingReminder);

                        AlarmHandler.getInstance().setAlarm(reminder,pendingReminderId);
                        Log.d("AutoStartAlarm","set alarm!");


                    }
                }
            }
        }

        if(realm!=null){
            realm.close();
        }
    }


}
