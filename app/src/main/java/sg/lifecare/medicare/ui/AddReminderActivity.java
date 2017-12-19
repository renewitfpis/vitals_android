package sg.lifecare.medicare.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

import java.util.Calendar;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.PrimaryKeyFactory;
import sg.lifecare.medicare.database.model.PendingReminder;
import sg.lifecare.medicare.database.model.Reminder;
import sg.lifecare.medicare.ui.alarm.AlarmHandler;
import sg.lifecare.medicare.ui.alarm.ReminderHandler;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.ui.view.CustomToolbar.OnToolbarClickListener;
import timber.log.Timber;

import static sg.lifecare.medicare.R.string.dialog_ok;


public class AddReminderActivity extends Activity {

    String TAG = "AddReminderActivity";

    private static final int REQ_DRAW_OVERLAY = 237;
    Context context;

    boolean[] isDaySelected = new boolean[7];
    
    int[] imgDaySelected = new int[7];

    int[] imgDayDeselected = new int[7];

    ImageView[] ivDay = new ImageView[7];

    int hour, min;

    LinearLayout daySelector = null;
    AlertDialog reqPermissionDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        context = this;

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        CustomToolbar mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_activity_add_reminder);
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.setRightButtonImage(R.drawable.ic_toolbar_tick);
        mToolbar.hideSecondRightButton();
        mToolbar.setListener(new OnToolbarClickListener() {
            @Override
            public void leftButtonClick() {
                onBackPressed();
            }

            @Override
            public void rightButtonClick() {
                addReminder();
            }

            @Override
            public void secondRightButtonClick() {

            }
        });

        //Set up resources
        imgDayDeselected = new int[]{
                R.drawable.s,R.drawable.m,R.drawable.t,R.drawable.w,R.drawable.t,
                R.drawable.f,R.drawable.s
        };

        imgDaySelected = new int[]{
                R.drawable.s_selected,R.drawable.m_selected,R.drawable.t_selected,R.drawable.w_selected,
                R.drawable.t_selected,R.drawable.f_selected,R.drawable.s_selected
        };

        for(int i = 0; i < isDaySelected.length; i++)
            isDaySelected[i] = false;

        //Day
        daySelector = (LinearLayout) findViewById(R.id.daySelector);
        daySelector.setVisibility(View.INVISIBLE);

        int count = daySelector.getChildCount();
        for(int i = 0; i < count; i++) {
            ivDay[i] = (ImageView)daySelector.getChildAt(i);
            ivDay[i].setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    String layoutResName = getResources().getResourceName(view.getId());
                    if(layoutResName!=null) {
                        Log.d(TAG, "Clicked on " + layoutResName);
                        int dayId = layoutResName.charAt(layoutResName.length()-1) - '0';

                        isDaySelected[dayId] = !isDaySelected[dayId];

                        if(isDaySelected[dayId])
                            ivDay[dayId].setImageResource(imgDaySelected[dayId]);
                        else
                            ivDay[dayId].setImageResource(imgDayDeselected[dayId]);
                    }

                }
            });
        }

        Spinner spinner = (Spinner)findViewById(R.id.spinner);
        String[] repeatOptions = new String[]{"Everyday", "Custom"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, repeatOptions);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {

                Log.d(TAG,"Item selected : "+adapterView.getItemAtPosition(pos).toString());

                String selectedItem = adapterView.getItemAtPosition(pos).toString();

                if(selectedItem.equalsIgnoreCase("Custom")){
                    daySelector.setVisibility(View.VISIBLE);

                    for(int i = 0; i <isDaySelected.length; i++) {
                        //isDaySelected[i] = false;
                        if(isDaySelected[i])
                            ivDay[i].setImageResource(imgDaySelected[i]);
                        else
                            ivDay[i].setImageResource(imgDayDeselected[i]);
                    }

                    daySelector.animate().translationY(daySelector.getHeight()).setListener(null);
                }
                else if(selectedItem.equalsIgnoreCase("Everyday")){
                    daySelector.animate().
                            translationY(0)
                            .setListener(hideViewListener);
                    //daySelector.setVisibility(View.INVISIBLE);

                    for(int i = 0; i <isDaySelected.length; i++) {
                        isDaySelected[i] = true;
                    }

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Calendar cal = Calendar.getInstance();
        hour = cal.get(Calendar.HOUR_OF_DAY);
        min = cal.get(Calendar.MINUTE);

        TimePicker timePicker = (TimePicker) findViewById(R.id.timePicker);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setHour(hour);
            timePicker.setMinute(min);
        }
        else{
            timePicker.setCurrentHour(hour);
            timePicker.setCurrentMinute(min);
        }

        timePicker.setOnTimeChangedListener(new OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker timePicker, int hourInput, int minInput) {
                hour = hourInput;
                min = minInput;
            }
        });

    }

    AnimatorListener hideViewListener = new AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            daySelector.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };
    private void addReminder(){
        EditText etReminderTitle = (EditText) findViewById(R.id.etTitle);
        String reminderTitle = etReminderTitle.getText().toString();
        String reminderType = "Default";

        if(reminderTitle.isEmpty()){
            new AlertDialog.Builder(AddReminderActivity.this,R.style.dialog)
                    .setTitle(getResources().getString(R.string.error_title_incomplete_form))
                    .setMessage(getResources().getString(R.string.error_msg_incomplete_form))
                    .setCancelable(true)
                    .setPositiveButton(dialog_ok, null)
                    .create().show();
        }
        else {

            boolean activated = true;

            final long reminderId = PrimaryKeyFactory.getInstance().nextKey(Reminder.class);

            Reminder reminder = new Reminder(
                    reminderId, hour, min, isDaySelected,reminderTitle, reminderType, activated
            );

            SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
            String entityId = sh.getString("entity_id", "");
            reminder.setEntityId(entityId);

            ReminderHandler.getInstance().addToList(reminder);

            Calendar cal = Calendar.getInstance();
            int today = cal.get(Calendar.DAY_OF_WEEK) - 1;
            int currentHr = cal.get(Calendar.HOUR_OF_DAY);
            int currentMin = cal.get(Calendar.MINUTE);

            if(reminder.checkReminderDay(today))
            {
                if(reminder.getHour() > currentHr ||
                        reminder.getHour() == currentHr && reminder.getMin() >= currentMin)
                {
                    PendingReminder pendingReminder = new PendingReminder(reminder);

                    pendingReminder.setEntityId(entityId);

                    ReminderHandler.getInstance().addToActiveList(pendingReminder);

                    AlarmHandler.getInstance().setAlarm(reminder,pendingReminder.getId());
                }
            }

            //check permission

            checkDrawOverlayPermission();
            //finish();
        }
    }
    public void checkDrawOverlayPermission() {
        Timber.d("CHECK PERMISSION");
        /** check if we already  have permission to draw over other apps */
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(AddReminderActivity.this)) {
                Timber.d("CANT DRAW OVERLAYS");
                /** if not construct intent to request permission */
                reqPermissionDialog = new AlertDialog.Builder(AddReminderActivity.this,R.style.dialog)
                    .setTitle("Overlay Permission Required")
                    .setMessage("Overlay Permission is needed for showing reminder. Press OK to go to the settings.")
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            /** request permission via start activity for result */
                            startActivityForResult(intent, REQ_DRAW_OVERLAY);
                        }
                    })
                    .show();
            }else{
                Timber.d("!!!!!CAN DRAW OVERLAYS");
                finish();
            }
        }else{
            //no problem with overlay permission
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        /** check if received result code
         is equal our requested code for draw permission  */
        if (requestCode == REQ_DRAW_OVERLAY) {
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // continue here - permission was granted
                    Timber.d("Overlay Permission is granted!");
                    new AlertDialog.Builder(AddReminderActivity.this,R.style.dialog)
                            .setTitle("Overlay Permission is granted!")
                            .setMessage("However, you might need to restart your app or clear app settings for reminders to work correctly.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    finish();
                                }
                            })
                            .show();

                }else{
                    Timber.d("Overlay Permission is still not granted! :(");
                    if(reqPermissionDialog!=null && !reqPermissionDialog.isShowing()){
                        reqPermissionDialog.show();
                    }
                }

            }
        }
    }
}
