package sg.lifecare.medicare.ui.alarm;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.BadTokenException;
import android.widget.TextView;

import headsUp.HeadsUp;
import headsUp.HeadsUpManager;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.ui.DashboardActivity;

import static sg.lifecare.medicare.ui.alarm.AlarmNotification.NOTIFICATION_ID;

public class Alarm extends BroadcastReceiver{

    String TAG = "Alarm";
    @Override
    public void onReceive(final Context context, Intent intent1)
    {

        WakeLocker.acquire(context);

        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("Alarm");
        keyguardLock.disableKeyguard();

        int alarmId = intent1.getExtras().getInt("alarmId");
        String title = intent1.getExtras().getString("title");
        String type = intent1.getExtras().getString("type");
        int hour = intent1.getExtras().getInt("hour");
        int min = intent1.getExtras().getInt("min");
        long prId = intent1.getExtras().getLong("prId");

        try {
            makeNotification(context, title, hour, min, prId);
        }catch(BadTokenException e){ //error that'd appear if user did not allow overdraw permission
            e.printStackTrace();
        }

        CancelAlarm(context,alarmId);

        ReminderHandler.getInstance().setShown(prId,true);

        Log.d(TAG,"On Receive Alarm : (Title) " + title + " (Time) " + hour + ":" + min + " (PR ID) " + prId);
    }

    public static void makeNotification(Context context, String title, int hour, int min, long prId){
        /*Intent alarmIntent = new Intent(context, AlarmNotification.class);
        alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        alarmIntent.putExtra("hour",hour);
        alarmIntent.putExtra("min",min);
        alarmIntent.putExtra("title",title);
        alarmIntent.putExtra("type",type);
        alarmIntent.putExtra("prId",prId);
        context.startActivity(alarmIntent);*/

        //setupNotification
        int notificationId = 1081;
        Intent intent = new Intent(context, UserActionBroadcast.class);
        intent.putExtra("prId", prId);
        intent.putExtra(NOTIFICATION_ID, notificationId);
        intent.putExtra(UserActionBroadcast.ACTION, UserActionBroadcast.CONFIRM);
        //dummy action to prevent extras being dropped off
        //solution referred to: http://stackoverflow.com/questions/3168484/
        intent.setAction(Long.toString(System.currentTimeMillis()));
        final PendingIntent confirmIntent = PendingIntent.getBroadcast(
                context,
                11,
                intent,
                PendingIntent.FLAG_ONE_SHOT
        );

        Intent intent2 = new Intent(context, UserActionBroadcast.class);
        intent2.putExtra("prId", prId);
        intent2.putExtra(NOTIFICATION_ID, notificationId);
        intent2.putExtra(UserActionBroadcast.ACTION, UserActionBroadcast.CANCEL);
        intent2.setAction(Long.toString(System.currentTimeMillis()));
        final PendingIntent cancelIntent = PendingIntent.getBroadcast(
                context,
                12,
                intent2,
                PendingIntent.FLAG_ONE_SHOT
        );

        Intent intent3 = new Intent(context, UserActionBroadcast.class);
        intent3.putExtra("prId", prId);
        intent3.putExtra(NOTIFICATION_ID, notificationId);
        intent3.putExtra(UserActionBroadcast.ACTION, UserActionBroadcast.SNOOZE);
        intent3.setAction(Long.toString(System.currentTimeMillis()));
        final PendingIntent snoozeIntent = PendingIntent.getBroadcast(
                context,
                13,
                intent3,
                PendingIntent.FLAG_ONE_SHOT
        );

        PendingIntent defaultIntent = PendingIntent.getActivity(
                context,
                0,
                new Intent(),
                PendingIntent.FLAG_ONE_SHOT);

        Intent dashboardIntent = new Intent(context, DashboardActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                dashboardIntent,
                PendingIntent.FLAG_ONE_SHOT);

        String time = (hour < 10 ? "0" : "") + hour + ":" + (min < 10 ? "0" : "") + min;
        final HeadsUpManager manager = HeadsUpManager.getInstant(context);
        View view= LayoutInflater.from(context).inflate(R.layout.view_reminder_pop_up_notification, null);
        ((TextView)view.findViewById(R.id.text_title)).setText(title);
        ((TextView)view.findViewById(R.id.text_time)).setText(time);
        view.findViewById(R.id.skip_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    cancelIntent.send();
                } catch (CanceledException e) {
                    e.printStackTrace();
                }
                manager.cancel();
            }
        });

        view.findViewById(R.id.took_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    confirmIntent.send();
                } catch (CanceledException e) {
                    e.printStackTrace();
                }
                manager.cancel();
            }
        });

        view.findViewById(R.id.snooze_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    snoozeIntent.send();
                } catch (CanceledException e) {
                    e.printStackTrace();
                }
                manager.cancel();
            }
        });
        HeadsUp headsUp1 = new HeadsUp.Builder(context)
                .setDefaults(Notification.DEFAULT_SOUND
                        | Notification.DEFAULT_VIBRATE
                        | Notification.DEFAULT_LIGHTS
                        | Notification.FLAG_NO_CLEAR
                        | Notification.FLAG_ONGOING_EVENT)
                .setSmallIcon(R.mipmap.ic_app)
                .setContentIntent(contentIntent)
                .setContentTitle("Vitals Reminder")
                .setContentText(title)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(context.getResources().getColor(R.color.colorPrimaryGreen))
                .addAction(R.drawable.skip, "Skip", cancelIntent)
                .addAction(R.drawable.took, "Took", confirmIntent)
                .addAction(R.drawable.snooze, "Snooze", snoozeIntent)
                .buildHeadUp();
        headsUp1.setCustomView(view);
        manager.notify(notificationId, headsUp1);

        /*NotificationManager manager2 =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setDefaults(Notification.DEFAULT_SOUND
                        | Notification.DEFAULT_VIBRATE
                        | Notification.DEFAULT_LIGHTS
                        | Notification.FLAG_NO_CLEAR
                        | Notification.FLAG_ONGOING_EVENT)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(contentIntent)
                .setContentTitle("Panic Button")
                .setContentText(title)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        manager2.notify(notificationId, builder.build());*/
    }

    public void SetOneTimeAlarm(Context context, int reqCode, long triggerTime,
                                int hour, int min, String title, String type, long prId)
    {
        Log.d(TAG,"Set One Time Alarm : (Title) " + title + " (Time) " + hour + ":" + min);

        Intent intent = new Intent(context, Alarm.class);
        intent.putExtra("alarmId",reqCode);
        intent.putExtra("hour",hour);
        intent.putExtra("min",min);
        intent.putExtra("title",title);
        intent.putExtra("type",type);
        intent.putExtra("prId",prId);

        PendingIntent pi = PendingIntent.getBroadcast(context, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT );

        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, triggerTime, pi);
    }

    public void CancelAlarm(Context context, int reqCode)
    {
        Log.d(TAG,"On Cancel Alarm : (reqCode) " + reqCode);

        Intent intent = new Intent(context, Alarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, reqCode, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}
