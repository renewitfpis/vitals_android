package sg.lifecare.medicare.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.Reminder;
import sg.lifecare.medicare.ui.adapter.ReminderAdapter;
import sg.lifecare.medicare.ui.view.CustomToolbar;

public class ReminderFragment extends Fragment {

    ReminderAdapter adapter;
    ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_reminder, container, false);
        //Toolbar
        RelativeLayout toolbarLayout = (RelativeLayout) view.findViewById(R.id.toolbar);
        toolbarLayout.setVisibility(View.GONE);

        String entityId = "";
        FragmentActivity parent = getActivity();
        if (parent instanceof DashboardActivity) {
            ((DashboardActivity)parent).setToolbar(R.string.title_reminder, R.drawable.ic_toolbar_back,
                    R.drawable.ic_toolbar_add);
            ((DashboardActivity)parent).setToolbarListener(mToolbarListener);
            entityId = ((DashboardActivity)parent).getUserEntityId();
        }

        Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
        RealmResults<Reminder> reminders = PatientData.getInstance().getReminders(realm,entityId);

        reminders.addChangeListener(new RealmChangeListener() {
            @Override
            public void onChange(Object element) {
                adapter.notifyDataSetChanged();
            }
        });
        adapter = new ReminderAdapter(getActivity(),
                R.layout.reminder_view, reminders, ReminderFragment.this);
        listView = (ListView) view.findViewById(R.id.reminder_listview);
        listView.setAdapter(adapter);
        listView.setEmptyView(view.findViewById(R.id.empty_list));

        realm.close();
        return view;
    }

    public void editReminder(int position){
        Intent intent = new Intent(getActivity(),EditReminderActivity.class);
        intent.putExtra("reminder_pos",position);
        startActivity(intent);
    }

    private CustomToolbar.OnToolbarClickListener mToolbarListener = new CustomToolbar.OnToolbarClickListener() {
        @Override
        public void leftButtonClick() {
            getActivity().onBackPressed();
        }

        @Override public void rightButtonClick() {
            Intent intent = new Intent(getActivity(),AddReminderActivity.class);
            startActivity(intent);
        }

        @Override public void secondRightButtonClick() {

        }
    };

}
