package sg.lifecare.medicare.ui.alarm;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;

import sg.lifecare.medicare.R;

public class AlarmDialogPopUp extends Activity
{
    private static String TAG = "AlarmDialogPopUp";
    private String title = "";
    private String time = "";
    private MediaPlayer mediaPlayer = new MediaPlayer();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //Play sound
        playSound();

        //Disable keyguard & turn screen on
        Window win = this.getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        win.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        win.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        //Get reminder information
        Bundle extras = getIntent().getExtras();
        int hour = -1, min = -1;

        if (extras != null) {
            title = extras.getString("title", "");
            hour = extras.getInt("hour", -1);
            min = extras.getInt("min", -1);
        }
        setupTimeDisplay(hour, min);

        showDialog(0);
    }

    @Override
    protected Dialog onCreateDialog(int id)
    {
        super.onCreateDialog(id);

        final Dialog dlg = new Dialog(this);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.alarm_dialog, null);
        ImageView closeBtn = (ImageView) view.findViewById(R.id.close_button);
        TextView tvTime = (TextView) view.findViewById(R.id.tvReminderTime);
        TextView tvTitle = (TextView) view.findViewById(R.id.tvReminderTitle);
        tvTime.setText(time);
        tvTitle.setText(title);

        closeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dlg.cancel();
            }
        });

        dlg.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
                WakeLocker.release();
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        });

        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlg.setCanceledOnTouchOutside(true);
        dlg.setCancelable(true);
        dlg.setContentView(view);
        dlg.show();

        return dlg;
    }

    private void setupTimeDisplay(int hour, int min){
        String period;
        if(hour > 12){
            period = "PM";
            hour = hour - 12;
        }
        else{
            period = "AM";
        }

        time = (hour < 10 ? "0" : "") + hour + ":" +
                (min < 10 ? "0" : "") + min + " " + period;
    }

    private void playSound(){

        //vibrate for 1 second
        Vibrator myVib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        myVib.vibrate(1000);

        //get default alarm ringtone & play it
        Uri defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        try {
            mediaPlayer.setDataSource(getApplicationContext(), defaultRingtoneUri);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    mp.release();
                }
            });
            mediaPlayer.start();
        } catch (IllegalArgumentException e) {
            Log.e(TAG,"Problem playing sound: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
