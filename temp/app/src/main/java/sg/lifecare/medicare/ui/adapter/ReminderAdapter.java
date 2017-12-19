package sg.lifecare.medicare.ui.adapter;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import io.realm.Realm;
import io.realm.RealmResults;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.PrimaryKeyFactory;
import sg.lifecare.medicare.database.model.PendingReminder;
import sg.lifecare.medicare.database.model.Reminder;
import sg.lifecare.medicare.ui.ReminderFragment;
import sg.lifecare.medicare.ui.alarm.AlarmHandler;
import sg.lifecare.medicare.ui.alarm.ReminderHandler;

public class ReminderAdapter extends ArrayAdapter<Reminder> implements ListAdapter
{   private String TAG = "ReminderAdapter";
    private Context context;
    private int layout;
    LayoutInflater inflater;
    private final RealmResults<Reminder> reminderList;
    private Fragment fragment;

    public ReminderAdapter(Context context, int layout, RealmResults<Reminder> list)
    {
        super(context, layout, list);
        this.context = context;
        this.layout = layout;
        this.reminderList = list;
    }

    public ReminderAdapter(Context context, int layout, RealmResults<Reminder> list, Fragment fragment)
    {
        super(context, layout, list);
        this.context = context;
        this.layout = layout;
        this.reminderList = list;
        this.fragment = fragment;
    }

    public View getView(final int position, View convertView, final ViewGroup parent)
    {
         if(convertView == null) {
             inflater = (LayoutInflater) context
                     .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
             convertView = inflater.inflate(layout, parent, false);
         }

        if(!reminderList.get(position).isValid())
            return convertView;

        final Reminder reminder = getItem(position);
        final TextView tvTitle = (TextView) convertView.findViewById(R.id.reminder_title);
        tvTitle.setText(reminder.getTitle());
        final TextView tvTime = (TextView) convertView.findViewById(R.id.tvTime);
        String period;
        int hourDisplay;
        if(reminder.getHour() >= 12){
            period = "PM";

            if(reminder.getHour()>12)
                hourDisplay = reminder.getHour() - 12;
            else
                hourDisplay = reminder.getHour();
        }
        else{
            period = "AM";
            hourDisplay = reminder.getHour();
        }
        tvTime.setText((hourDisplay < 10 ? "0" : "") + hourDisplay + ":" +
                (reminder.getMin() < 10 ? "0" : "") + reminder.getMin() + " " + period);

        final StringBuilder builder = new StringBuilder();
        //noinspection ResourceType
        final String color = "#" + parent.getResources().getString(R.color.colorPrimary).substring(3);
        //noinspection ResourceType
        final String deactivatedColor = "#" + parent.getResources().getString(R.color.colorDeactivatedDay).substring(3);

        char[] dayLetter = new char[]{'S','M','T','W','T','F','S'};
        //boolean[] isDaySelected = reminder.days;
        for(int i = 0; i < 7; i++){
            if(reminder.checkReminderDay(i) && reminder.isActivated()) {
                builder.append("<font color=\""+color+"\">");
                builder.append(dayLetter[i] + " ");
                builder.append("</font>");
            }
            else if(reminder.checkReminderDay(i) && !reminder.isActivated()){
                builder.append("<font color=\""+deactivatedColor+"\">");
                builder.append(dayLetter[i] + " ");
                builder.append("</font>");
            }
            else{
                builder.append(dayLetter[i] + " ");
            }
        }

        final TextView tvDays = (TextView) convertView.findViewById(R.id.tvDays);
        tvDays.setText(Html.fromHtml(builder.toString()));

        RelativeLayout deleteBtnLayout = (RelativeLayout) convertView.findViewById(R.id.deleteBtnLayout);

        deleteBtnLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                View dialogView = inflater.inflate(R.layout.confirm_dialog,null);
                final Dialog dialog = new Dialog(context);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(dialogView);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                dialog.show();

                TextView tvTitle = (TextView) dialogView.findViewById(R.id.text_title);
                tvTitle.setText("Delete this reminder, " + reminder.getTitle() +"?");

                TextView btnCancel = (TextView) dialogView.findViewById(R.id.cancel);
                btnCancel.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                    }
                });

                TextView btnConfirm = (TextView) dialogView.findViewById(R.id.confirm);
                btnConfirm.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();

                        AlarmHandler.getInstance().cancelAlarm(reminder);

                        ReminderHandler.getInstance().removePendingWithParentId(reminder.getId());
                        ReminderHandler.getInstance().removeFromList(reminderList,position);

                    }
                });



            }
        });

        LinearLayout ll = (LinearLayout) convertView.findViewById(R.id.reminderTitleLayout);
        ll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                if(fragment instanceof ReminderFragment){
                   ((ReminderFragment) fragment).editReminder(position);
                }
            }
        });

        final ToggleButton alarmBtn = (ToggleButton) convertView.findViewById(R.id.alarm);
        LinearLayout toggleReminderBtn = (LinearLayout) convertView.findViewById(R.id.toggleReminderLayout);

        ToggleButtonListener toggleReminderListener = new ToggleButtonListener(reminder,convertView,parent);
        toggleReminderBtn.setOnClickListener(toggleReminderListener);
        alarmBtn.setOnClickListener(toggleReminderListener);


        if(!reminder.isActivated()){
            alarmBtn.setChecked(false);
            tvTime.setTextColor(parent.getResources().getColor(android.R.color.darker_gray));
            tvTitle.setTextColor(parent.getResources().getColor(android.R.color.darker_gray));
            tvDays.setTextColor(parent.getResources().getColor(android.R.color.darker_gray));
        }
        else{
            alarmBtn.setChecked(true);
            tvTime.setTextColor(parent.getResources().getColor(R.color.black_lighter));
            tvTitle.setTextColor(parent.getResources().getColor(R.color.black_lighter));
            tvDays.setTextColor(parent.getResources().getColor(R.color.black_lighter));
        }

        return convertView;
    }

    public class ToggleButtonListener implements OnClickListener
    {

        Reminder reminder;
        View convertView;
        ViewGroup parent;
        public ToggleButtonListener(Reminder reminder, View convertView, ViewGroup parent) {
            this.reminder = reminder;
            this.convertView = convertView;
            this.parent = parent;
        }

        @Override
        public void onClick(View view) {
            final TextView tvTitle = (TextView) convertView.findViewById(R.id.reminder_title);
            final TextView tvTime = (TextView) convertView.findViewById(R.id.tvTime);
            ToggleButton alarmBtn = (ToggleButton) convertView.findViewById(R.id.alarm);
            TextView tvDays = (TextView) convertView.findViewById(R.id.tvDays);
            //noinspection ResourceType
            final String activatedColor = "#" + parent.getResources().getString(R.color.colorPrimary).substring(3);
            //noinspection ResourceType
            final String deactivatedColor = "#" + parent.getResources().getString(R.color.colorDeactivatedDay).substring(3);

            Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
            realm.beginTransaction();
            reminder.setActivated(!reminder.isActivated());
            realm.commitTransaction();

            if(!reminder.isActivated()){
                alarmBtn.setChecked(false);
                 tvTime.setTextColor(parent.getResources().getColor(android.R.color.darker_gray));
                tvTitle.setTextColor(parent.getResources().getColor(android.R.color.darker_gray));
                tvDays.setTextColor(parent.getResources().getColor(android.R.color.darker_gray));
                ReminderHandler.getInstance().removePendingWithParentId(reminder.getId());

                AlarmHandler.getInstance().cancelAlarm(reminder);
            }
            else{
                alarmBtn.setChecked(true);
                tvTime.setTextColor(parent.getResources().getColor(R.color.black_lighter));
                tvTitle.setTextColor(parent.getResources().getColor(R.color.black_lighter));
                tvDays.setTextColor(parent.getResources().getColor(R.color.black_lighter));

                long prId = PrimaryKeyFactory.getInstance().nextKey(PendingReminder.class);
                PendingReminder pr = new PendingReminder(prId,reminder.getHour(),reminder.getMin(),reminder.getId());
                pr.setEntityId(reminder.getEntityId());
                ReminderHandler.getInstance().addToActiveList(pr);

                AlarmHandler.getInstance().setAlarm(reminder,prId);
            }

            StringBuilder builder = new StringBuilder();
            char[] dayLetter = new char[]{'S','M','T','W','T','F','S'};
            //boolean[] isDaySelected = reminder.days;
            for(int i = 0; i < 7; i++){
                if(reminder.checkReminderDay(i) && reminder.isActivated()) {
                    builder.append("<font color=\""+activatedColor+"\">");
                    builder.append(dayLetter[i] + " ");
                    builder.append("</font>");
                }
                else if(reminder.checkReminderDay(i) && !reminder.isActivated()){
                    builder.append("<font color=\""+deactivatedColor+"\">");
                    builder.append(dayLetter[i] + " ");
                    builder.append("</font>");
                }
                else{
                    builder.append(dayLetter[i] + " ");
                }
            }

            tvDays.setText(Html.fromHtml(builder.toString()));


        }

    };
}
