package sg.lifecare.medicare.ui.alarm;

import android.content.Context;

import java.util.Calendar;

import sg.lifecare.medicare.database.model.Reminder;

/*
 *   A singleton class that handles alarm creation & removal
 *
 */
public class AlarmHandler {
    String TAG = "AlarmHandler";

    Alarm alarm = new Alarm();

    private static Context context;

    private static AlarmHandler instance;

    public static AlarmHandler getInstance() {
        if (instance == null) {
            instance = new AlarmHandler(context);
        }

        return instance;
    }

    public static void initialize(Context ctx){
        context = ctx;
    }

    private AlarmHandler(Context context){

    }

    public static boolean isInitialized(){
        return (context != null);
    }

    public void setAlarm(Reminder reminder, long prId){

        Calendar cal = Calendar.getInstance();

        int today = cal.get(Calendar.DAY_OF_WEEK) - 1;

        int currentHr = cal.get(Calendar.HOUR_OF_DAY);

        int currentMin = cal.get(Calendar.MINUTE);

        int reqCode = Integer.parseInt(reminder.getId() + "" + today);

        if(reminder.checkReminderDay(today)){
            if(reminder.getHour() > currentHr ||
                    reminder.getHour() == currentHr && reminder.getMin() >= currentMin)
            {
                long triggerTime = getCalTrigger(reminder.getHour(),reminder.getMin());
                alarm.SetOneTimeAlarm(context, reqCode, triggerTime,
                        reminder.getHour(), reminder.getMin(), reminder.getTitle(), reminder.getType(), prId);
            }
        }
    }

    public void snoozeAlarm(Context context, Reminder reminder, long prId){

        Calendar cal = Calendar.getInstance();

        int today = cal.get(Calendar.DAY_OF_WEEK) - 1;

        int currentHr = cal.get(Calendar.HOUR_OF_DAY);

        int currentMin = cal.get(Calendar.MINUTE);

        int reqCode = Integer.parseInt(reminder.getId() + "" + today);

        int hour = currentHr;

        int min = currentMin+5;

        if(min > 59){
            min = min - 60;

            if(hour != 23)
                hour += 1;
            else
                hour = 0;
        }

        long triggerTime = getCalTrigger(hour,min);
        alarm.SetOneTimeAlarm(context, reqCode, triggerTime,
                hour, min, reminder.getTitle(), reminder.getType(), prId);

    }

    public void cancelAlarm(Reminder reminder){
        Calendar cal = Calendar.getInstance();

        int today = cal.get(Calendar.DAY_OF_WEEK) - 1;

        alarm.CancelAlarm(context,Integer.parseInt(reminder.getId()+""+today));
    }

    long getCalTrigger(int hour, int min){
        Calendar timeOff = Calendar.getInstance();
        timeOff.set(Calendar.HOUR_OF_DAY, hour);
        timeOff.set(Calendar.MINUTE, min);
        timeOff.set(Calendar.SECOND, 0);
        return timeOff.getTimeInMillis();
    }
}
