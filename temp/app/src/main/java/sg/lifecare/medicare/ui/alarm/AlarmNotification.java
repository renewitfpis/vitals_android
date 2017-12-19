package sg.lifecare.medicare.ui.alarm;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;

import headsUp.HeadsUp;
import headsUp.HeadsUpManager;
import sg.lifecare.medicare.R;
import timber.log.Timber;

/**
 * Created by wanping on 16/6/16.
 */
public class AlarmNotification extends Activity {
    private static String TAG = "AlarmDialogPopUp";
    public static final String NOTIFICATION_ID = "NOTIFICATION_ID";

    private String title = "";
    private String time = "";
    private String type = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get reminder information
        Bundle extras = getIntent().getExtras();
        int hour = -1, min = -1;
        long prId = -1;

        if (extras != null) {
            title = extras.getString("title", "");
            type = extras.getString("type", "");
            hour = extras.getInt("hour", -1);
            min = extras.getInt("min", -1);
            prId = extras.getLong("prId", -1);
        }

        Log.d("Alarm", "On Notification : prId = " + prId);

        int notificationId = 1081;
        Intent intent = new Intent(this, UserActionBroadcast.class);
        intent.putExtra("prId", prId);
        intent.putExtra(NOTIFICATION_ID, notificationId);
        intent.putExtra(UserActionBroadcast.ACTION, UserActionBroadcast.CONFIRM);
        //dummy action to prevent extras being dropped off
        //solution referred to: http://stackoverflow.com/questions/3168484/
        intent.setAction(Long.toString(System.currentTimeMillis()));
        final PendingIntent confirmIntent = PendingIntent.getBroadcast(
                AlarmNotification.this,
                11,
                intent,
                PendingIntent.FLAG_ONE_SHOT
        );

        Intent intent2 = new Intent(this, UserActionBroadcast.class);
        intent2.putExtra("prId", prId);
        intent2.putExtra(NOTIFICATION_ID, notificationId);
        intent2.putExtra(UserActionBroadcast.ACTION, UserActionBroadcast.CANCEL);
        intent2.setAction(Long.toString(System.currentTimeMillis()));
        final PendingIntent cancelIntent = PendingIntent.getBroadcast(
                AlarmNotification.this,
                12,
                intent2,
                PendingIntent.FLAG_ONE_SHOT
        );

        Intent intent3 = new Intent(this, UserActionBroadcast.class);
        intent3.putExtra("prId", prId);
        intent3.putExtra(NOTIFICATION_ID, notificationId);
        intent3.putExtra(UserActionBroadcast.ACTION, UserActionBroadcast.SNOOZE);
        intent3.setAction(Long.toString(System.currentTimeMillis()));
        final PendingIntent snoozeIntent = PendingIntent.getBroadcast(
                AlarmNotification.this,
                13,
                intent3,
                PendingIntent.FLAG_ONE_SHOT
        );

        PendingIntent defaultIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                new Intent(),
                PendingIntent.FLAG_ONE_SHOT);
        if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
            Timber.d("USING 3RD PARTY NOTIFICATION ");
            HeadsUpManager manager = HeadsUpManager.getInstant(getApplication());
            HeadsUp.Builder builder = new HeadsUp.Builder(AlarmNotification.this);
            builder.setContentTitle("Reminder").setDefaults(Notification.DEFAULT_SOUND |
                    Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentText(title)
                    .setUsesChronometer(false)
                    .setVibrate(new long[0])
                    .setPriority(Notification.PRIORITY_MAX)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .addAction(R.drawable.normal_cross_color_16dp, "Cancel", cancelIntent)
                    .addAction(R.drawable.icon_reminder_16dp, "Snooze", snoozeIntent)
                    .addAction(R.drawable.ic_normal_tick_16dp, "Confirm", confirmIntent);

            HeadsUp headsUp = builder.buildHeadUp();
            manager.notify(notificationId, headsUp);
        } else {
            Timber.d("USING ORIGINAL NOTIFICATION ");
           /* Builder mbuilder = new NotificationCompat.Builder(AlarmNotification.this)
                    .setDefaults(Notification.DEFAULT_SOUND |
                    Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentTitle(title)
                    .setContentText("You have a reminder!")
                    .setFullScreenIntent(defaultIntent, false)
                    .setUsesChronometer(false)
                    .setVibrate(new long[0])
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setOngoing(false)
                    .addAction(R.drawable.normal_cross_color_16dp, "Cancel", cancelIntent)
                    .addAction(R.drawable.icon_reminder_16dp, "Snooze", snoozeIntent)
                    .addAction(R.drawable.ic_normal_tick_16dp, "Confirm", confirmIntent)
                    .setSmallIcon(R.mipmap.ic_app);
            //Notification notification = builder.build();
            NotificationManager mNotifyManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotifyManager.notify(notificationId, mbuilder.build());*/


            /*HeadsUpManager manager = HeadsUpManager.getInstant(getApplication());
            HeadsUp.Builder builder = new HeadsUp.Builder(AlarmNotification.this);
            builder.setContentTitle("Reminder").setDefaults(Notification.DEFAULT_SOUND |
                    Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentText(title)
                    .setUsesChronometer(false)
                    .setVibrate(new long[0])
                    .setPriority(Notification.PRIORITY_HIGH)
                    .addAction(R.drawable.normal_cross_color_16dp, "Cancel", cancelIntent)
                    .addAction(R.drawable.icon_reminder_16dp, "Snooze", snoozeIntent)
                    .addAction(R.drawable.ic_normal_tick_16dp, "Confirm", confirmIntent);

            HeadsUp headsUp = builder.buildHeadUp();
            manager.notify(notificationId, headsUp);*/

            /*final HeadsUpManager manager = HeadsUpManager.getInstant(getApplication());
            View view=getLayoutInflater().inflate(R.layout.view_reminder_pop_up_notification, null);
            ((TextView)view.findViewById(R.id.text_title)).setText(title);
            ((TextView)view.findViewById(R.id.text_time)).setText(hour+":"+min);
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
            HeadsUp headsUp1 = new HeadsUp.Builder(AlarmNotification.this)
                    //要显示通知栏通知,这个一定要设置
                    .setSmallIcon(R.drawable.icon)
                    //2.3 一定要设置这个参数,负责会报错
                    .setContentIntent(defaultIntent)
                    .setContentTitle("Vitals Reminder")
                    .setContentText(title)
                    .addAction(R.drawable.normal_cross_color_16dp, "Skip", cancelIntent)
                    .addAction(R.drawable.ic_normal_tick_16dp, "Took", confirmIntent)
                    .addAction(R.drawable.icon_reminder_16dp, "Snooze", snoozeIntent)
                    .buildHeadUp();
            headsUp1.setCustomView(view);
            manager.notify(notificationId, headsUp1);*/

            PendingIntent pendingIntent=PendingIntent.getActivity(AlarmNotification.this,11,new Intent(AlarmNotification.this,AlarmNotification.class),PendingIntent.FLAG_UPDATE_CURRENT);

            final HeadsUpManager manage1 = HeadsUpManager.getInstant(getApplication());

            View view=getLayoutInflater().inflate(R.layout.custom_notification, null);

            view.findViewById(R.id.openSource).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/zzz40500/HeadsUp"));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    AlarmNotification.this.startActivity(intent);
                    manage1.cancel();
                }
            });

            HeadsUp headsUp1 = new HeadsUp.Builder(AlarmNotification.this)
                    .setContentTitle("标题")
                    //要显示通知栏通知,这个一定要设置
                    .setSmallIcon(R.drawable.icon)
                    //2.3 一定要设置这个参数,负责会报错
                    .setContentIntent(pendingIntent)
                    .setContentText("这个是自定义通知")
                    .buildHeadUp();
            headsUp1.setCustomView(view);
            manage1.notify(notificationId, headsUp1);
        }

        finish();


        //vibrate for 1 second
       /* Vibrator myVib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        myVib.vibrate(1000);*/
    }


}
