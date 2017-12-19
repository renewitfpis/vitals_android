package sg.lifecare.medicare.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
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

import io.realm.Realm;
import io.realm.RealmResults;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.PrimaryKeyFactory;
import sg.lifecare.medicare.database.model.PendingReminder;
import sg.lifecare.medicare.database.model.Reminder;
import sg.lifecare.medicare.ui.alarm.AlarmHandler;
import sg.lifecare.medicare.ui.alarm.ReminderHandler;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.ui.view.CustomToolbar.OnToolbarClickListener;
import timber.log.Timber;


public class EditReminderActivity extends Activity {
    String TAG = "EditReminderActivity";

    final int EVERYDAY = 0, CUSTOM = 1;

    private final static int REQ_DRAW_OVERLAY = 237;

    AlertDialog reqPermissionDialog;

    Context context;

    Spinner spinner;

    boolean displayAnimation = false;

    boolean[] isDaySelected = new boolean[7];
    LinearLayout daySelector = null;
    int[] imgDaySelected = new int[]{
            R.drawable.s_selected,R.drawable.m_selected,R.drawable.t_selected,R.drawable.w_selected,
            R.drawable.t_selected,R.drawable.f_selected,R.drawable.s_selected
    };

    int[] imgDayDeselected = new int[]{
            R.drawable.s,R.drawable.m,R.drawable.t,R.drawable.w,R.drawable.t,
            R.drawable.f,R.drawable.s
    };

    ImageView[] ivDay = new ImageView[7];

    int hour, min, today;

    Reminder reminder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        final int position = getIntent().getIntExtra("reminder_pos",-1);

        context = getApplicationContext();

        Calendar cal = Calendar.getInstance();
        today = cal.get(Calendar.DAY_OF_WEEK) - 1;

        String entityId = getSharedPreferences("lifecare_pref",MODE_PRIVATE).getString("entity_id","");
        Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
        RealmResults<Reminder> reminders = PatientData.getInstance().getReminders(realm,entityId);
        realm.close();
        
        reminder = reminders.get(position);

        EditText etReminderTitle = (EditText) findViewById(R.id.etTitle);
        etReminderTitle.setText(reminder.getTitle());

        boolean everyday = true;
        for(int i = 0; i < 7; i++) {
            if(!reminder.checkReminderDay(i)){
                everyday = false;
                break;
            }
        }

        CustomToolbar mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_activity_edit_reminder);
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
                updateReminder();
            }

            @Override
            public void secondRightButtonClick() {

            }
        });

        for(int i = 0; i < isDaySelected.length; i++) {
            isDaySelected[i] = reminder.checkReminderDay(i);
            //isDaySelected[i] = false;
        }

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

        spinner = (Spinner)findViewById(R.id.spinner);
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

                if(selectedItem.equalsIgnoreCase("Custom")) {

                    for (int i = 0; i < isDaySelected.length; i++) {
                        if (!isDaySelected[i])
                            ivDay[i].setImageResource(imgDayDeselected[i]);
                        else
                            ivDay[i].setImageResource(imgDaySelected[i]);
                    }

                    if (displayAnimation) {
                        daySelector.setVisibility(View.VISIBLE);
                        daySelector.animate()
                                .translationY(daySelector.getHeight())
                                .setListener(null);
                    }
                }

                else if(selectedItem.equalsIgnoreCase("Everyday")){

                    daySelector.animate().
                            translationY(0)
                            .setListener(hideViewListener);

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        if(!everyday) {
            spinner.setSelection(1);
            for (int i = 0; i < isDaySelected.length; i++) {
                if (!isDaySelected[i])
                    ivDay[i].setImageResource(imgDayDeselected[i]);
                else
                    ivDay[i].setImageResource(imgDaySelected[i]);
            }
        }
        else{
            displayAnimation = true;
        }

        hour = reminder.getHour();
        min = reminder.getMin();

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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if(hasFocus&&!displayAnimation){
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(spinner.getSelectedItemPosition()==1){
                        daySelector.setVisibility(View.VISIBLE);
                        int y = daySelector.getHeight();
                        daySelector.animate().translationY(y)
                                .setListener(null);
                    }
                }
            },200);

        }
    }

    private void updateReminder(){
        EditText etReminderTitle = (EditText) findViewById(R.id.etTitle);
        String reminderTitle = etReminderTitle.getText().toString();
        String reminderType = "Default";

        if(reminderTitle.isEmpty()){
            new AlertDialog.Builder(EditReminderActivity.this,R.style.dialog)
                    .setTitle(getResources().getString(R.string.error_title_incomplete_form))
                    .setMessage(getResources().getString(R.string.error_msg_incomplete_form))
                    .setCancelable(true)
                    .setPositiveButton("ok", null)
                    .create().show();
        }
        else {

            boolean activated = true;

            if(spinner.getSelectedItemPosition() == EVERYDAY) {
                for(int i = 0; i < isDaySelected.length; i++)
                    isDaySelected[i]=true;
            }

            long id = reminder.getId();
            Reminder editedReminder = new Reminder(
                    id, hour, min, isDaySelected, reminderTitle, reminderType, activated
            );

            editedReminder.setEntityId(reminder.getEntityId());

            final long pendingReminderId = PrimaryKeyFactory.getInstance().nextKey(PendingReminder.class);
            PendingReminder pendingReminder = new PendingReminder(pendingReminderId,hour,min,id);
            pendingReminder.setEntityId(editedReminder.getEntityId());

            AlarmHandler.getInstance().cancelAlarm(reminder);
            AlarmHandler.getInstance().setAlarm(editedReminder,pendingReminderId);

            ReminderHandler.getInstance().removePendingWithParentId(reminder.getId());
            ReminderHandler.getInstance().removeFromList(reminder);
            ReminderHandler.getInstance().addToList(editedReminder);
            //ReminderHandler.getInstance().editPending(pendingReminder);
            ReminderHandler.getInstance().addToActiveList(pendingReminder);

            checkDrawOverlayPermission();
        }

    }
    public void checkDrawOverlayPermission() {
        Timber.d("CHECK PERMISSION");
        /** check if we already  have permission to draw over other apps */
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(EditReminderActivity.this)) {
                Timber.d("CANT DRAW OVERLAYS");
                /** if not construct intent to request permission */
                reqPermissionDialog = new AlertDialog.Builder(EditReminderActivity.this)
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
                    new AlertDialog.Builder(EditReminderActivity.this,R.style.dialog)
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
