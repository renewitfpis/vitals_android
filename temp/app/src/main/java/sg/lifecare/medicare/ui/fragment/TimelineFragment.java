package sg.lifecare.medicare.ui.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.balysv.materialripple.MaterialRippleLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmObject;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Medication;
import sg.lifecare.medicare.database.model.PendingReminder;
import sg.lifecare.medicare.database.model.Photo;
import sg.lifecare.medicare.database.model.Reminder;
import sg.lifecare.medicare.database.model.SpO2;
import sg.lifecare.medicare.database.model.Symptom;
import sg.lifecare.medicare.database.model.Temperature;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.database.sync.NetworkChangeReceiver;
import sg.lifecare.medicare.ui.ChartActivity;
import sg.lifecare.medicare.ui.DashboardActivity;
import sg.lifecare.medicare.ui.EnlargeImageActivity;
import sg.lifecare.medicare.ui.adapter.TimelineAdapter;
import sg.lifecare.medicare.ui.alarm.ReminderHandler;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.utils.EnterpriseHandler;
import sg.lifecare.medicare.utils.LifeCareHandler;
import sg.lifecare.medicare.utils.WrapContentLinearLayoutManager;
import timber.log.Timber;

/**
 * Main fragment for dashboard
 */
public class TimelineFragment extends Fragment implements View.OnClickListener {

    public static TimelineFragment newInstance() {
        return new TimelineFragment();
    }

    public interface TimelineFragmentListener {
        void onRequestStart();
        void onRequestFinish();
        void onNextClick();
        void onPreviousClick();
        void onCalendarClick();
    }

    private ImageView mImageNoActivity, mImagePrevious, mImageNext;
    private TextView mTextNoActivity, mTextDate;
    private LinearLayout mViewNoActivity;

    private NestedScrollView mScrollView;
    private SwipeRefreshLayout swipeContainer;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private TimelineAdapter mAdapter;
    private PatientDataTask mPatientDataTask;
    private TimelineFragmentListener mListener;
    private ArrayList<RealmObject> mDataList = new ArrayList<>();

    private Calendar selectedDate;

    private View view;

    enum Direction {
        UP, DOWN
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Timber.tag("TimelineFragment");

        mDataList = new ArrayList<>();
        mAdapter = new TimelineAdapter(mDataList, getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_timeline, container, false);

        Timber.d("onCreateView");

        //hide timeline graph
        view.findViewById(R.id.graph_view).setVisibility(View.GONE);
        view.findViewById(R.id.full_chart_btn).setVisibility(View.GONE);

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        swipeContainer.setColorSchemeResources(R.color.colorPrimaryGreen,R.color.colorPrimaryGreen);
        swipeContainer.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(getActivity() instanceof DashboardActivity){
                    ((DashboardActivity) getActivity()).syncSpecificDateFromServer(getCurrentDate());
                }
            }
        });

        mScrollView = (NestedScrollView) view.findViewById(R.id.timeline_scroll_view);

        mImageNoActivity = (ImageView) view.findViewById(R.id.image_no_activity);
        mTextNoActivity = (TextView) view.findViewById(R.id.text_no_activity);
        mViewNoActivity = (LinearLayout) view.findViewById(R.id.empty);

        mImageNext = (ImageView) view.findViewById(R.id.image_next);
        MaterialRippleLayout.on(mImageNext)
                .rippleColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryTransparent))
                .rippleAlpha(0.03f)
                .rippleDuration(150)
                .create();

        mImageNext.setOnClickListener(this);

        mImagePrevious = (ImageView) view.findViewById(R.id.image_previous);
        MaterialRippleLayout.on(mImagePrevious)
                .rippleColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryTransparent))
                .rippleAlpha(0.03f)
                .rippleDuration(150)
                .create();
        mImagePrevious.setOnClickListener(this);

        mTextDate = (TextView) view.findViewById(R.id.text_date);
        mTextDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onCalendarClick();
            }
        });

        mAdapter.setOnItemClickListener(new TimelineAdapter.ClickListener() {
            @Override
            public void onItemClick(int position, int type, View view) {
                switch(type){
                    case TimelineAdapter.TYPE_TERUMO:
                        Intent intent = new Intent(getActivity(), ChartActivity.class);
                        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FORMAT, Locale.ENGLISH);
                        String date = sdf.format(selectedDate.getTime());
                        intent.putExtra("date", date);
                        if(!ChartActivity.active)
                            startActivity(intent);
                        break;

                    case TimelineAdapter.TYPE_BLOOD_PRESSURE:
                        Timber.d("ON CLICK BP!");
                        Intent intent2 = new Intent(getActivity(), ChartActivity.class);
                        SimpleDateFormat sdf2 = new SimpleDateFormat(PatientData.DATE_FORMAT, Locale.ENGLISH);
                        String date2 = sdf2.format(selectedDate.getTime());
                        intent2.putExtra("date", date2);
                        intent2.putExtra("type", ChartActivity.BLOOD_PRESSURE_TYPE);
                        if(!ChartActivity.active)
                            startActivity(intent2);
                        break;

                    case TimelineAdapter.TYPE_WEIGHT:
                        Timber.d("ON CLICK WEIGHT!");
                        Intent intentWeightChart = new Intent(getActivity(), ChartActivity.class);
                        SimpleDateFormat sdf3 = new SimpleDateFormat(PatientData.DATE_FORMAT, Locale.ENGLISH);
                        String date3 = sdf3.format(selectedDate.getTime());
                        intentWeightChart.putExtra("date", date3);
                        intentWeightChart.putExtra("type", ChartActivity.WEIGHT_TYPE);
                        if(!ChartActivity.active)
                            startActivity(intentWeightChart);
                        break;

                    case TimelineAdapter.TYPE_TEMPERATURE:
                        Timber.d("ON CLICK WEIGHT!");
                        Intent intentTempChart = new Intent(getActivity(), ChartActivity.class);
                        SimpleDateFormat sdf4 = new SimpleDateFormat(PatientData.DATE_FORMAT, Locale.ENGLISH);
                        String date4 = sdf4.format(selectedDate.getTime());
                        intentTempChart.putExtra("date", date4);
                        intentTempChart.putExtra("type", ChartActivity.TEMPERATURE_TYPE);
                        if(!ChartActivity.active)
                            startActivity(intentTempChart);
                        break;

                    case TimelineAdapter.TYPE_PHOTO:
                        String url = (String)view.findViewById(R.id.image_photo).getTag();
                        String remarks = ((TextView)view.findViewById(R.id.text_remark)).getText().toString();
                        Intent intent3 = new Intent(getActivity(), EnlargeImageActivity.class);
                        intent3.putExtra("Image",url);
                        intent3.putExtra("Remarks",remarks);
                        if(!ChartActivity.active)
                            startActivity(intent3);
                        break;
                }
            }

            @Override
            public void onItemLongClick(int position, final int type, final View v) {

                Timber.d("on item long click");
                if(DashboardActivity.isCaregiver()){
                    Timber.d("is caregiver");
                    return;
                }
                String eventId = "";
                Terumo terumo = null;
                BloodPressure bp = null;
                Weight weight = null;
                Temperature temp = null;
                Medication medic = null;
                Symptom symptom = null;
                Photo photo = null;
                SpO2 spo2 = null;
                switch(type) {
                    case TimelineAdapter.TYPE_TERUMO:
                        terumo = (Terumo) v.getTag();
                        Timber.d("Terumo is null? " + (terumo == null) + ", " + (terumo.getEventId() == null));
                        if(terumo!=null && terumo.getEventId()!=null && !terumo.getEventId().isEmpty()){
                            Timber.d("Event ID = " + terumo.getEventId());
                            eventId = terumo.getEventId();
                        }
                        break;

                    case TimelineAdapter.TYPE_BLOOD_PRESSURE:
                        bp = (BloodPressure) v.getTag();
                        if(bp!=null && bp.getEventId()!=null && !bp.getEventId().isEmpty()){
                            Timber.d("Event ID = " + bp.getEventId());
                            eventId = bp.getEventId();
                        }
                        break;

                    case TimelineAdapter.TYPE_WEIGHT:
                        Timber.d("ENTER WEIGHT!");
                        weight = (Weight) v.getTag();
                        if(weight!=null && weight.getEventId()!=null && !weight.getEventId().isEmpty()){
                            Timber.d("Event ID = " + weight.getEventId());
                            eventId = weight.getEventId();
                        }
                        break;

                    case TimelineAdapter.TYPE_TEMPERATURE:
                        temp = (Temperature) v.getTag();
                        if(temp!=null && temp.getEventId()!=null && !temp.getEventId().isEmpty()){
                            Timber.d("Event ID = " + temp.getEventId());
                            eventId = temp.getEventId();
                        }
                        break;

                    case TimelineAdapter.TYPE_SPO2:
                        spo2 = (SpO2) v.getTag();
                        if(spo2!=null && spo2.getEventId()!=null && !spo2.getEventId().isEmpty()){
                            Timber.d("Event ID = " + spo2.getEventId());
                            eventId = spo2.getEventId();
                        }
                        break;

                    case TimelineAdapter.TYPE_MEDICATION:
                        medic = (Medication) v.getTag();
                        Timber.d("medic!" + (medic == null) + (medic.getEventId() == null) );
                        if(medic!=null && medic.getEventId()!=null && !medic.getEventId().isEmpty()){
                            Timber.d("Event ID = " + medic.getEventId());
                            eventId = medic.getEventId();
                        }
                        break;

                    case TimelineAdapter.TYPE_SYMPTOM:
                        symptom = (Symptom) v.getTag();
                        if(symptom!=null && symptom.getEventId()!=null && !symptom.getEventId().isEmpty()){
                            Timber.d("Event ID = " + symptom.getEventId());
                            eventId = symptom.getEventId();
                        }
                        break;

                    case TimelineAdapter.TYPE_PHOTO:
                        photo = (Photo) v.getTag();
                        if(photo!=null && photo.getEventId()!=null && !photo.getEventId().isEmpty()){
                            Timber.d("Event ID = " + photo.getEventId());
                            eventId = photo.getEventId();
                        }
                        break;
                }

                final String evId = eventId;
                if(!evId.isEmpty()){
                    new AlertDialog.Builder(getActivity())
                            .setMessage("Are you sure you want to delete this data? ")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();

                                    if(NetworkChangeReceiver.isInternetAvailable(getActivity())) {
                                        new DeleteDataTask((RealmObject) v.getTag(), evId).execute();
                                    }else{
                                        Toast.makeText(getActivity(),"Please check your internet connection!",Toast.LENGTH_LONG).show();
                                    }
                                }
                            })
                            .show();
                }
            }
        });
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mLayoutManager = new WrapContentLinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setNestedScrollingEnabled(false);
        mRecyclerView.setFocusable(false);
        mRecyclerView.setVerticalScrollBarEnabled(false);
        mRecyclerView.setOnTouchListener(new OnTouchListener() {
            float initialY, finalY;
            Direction prevDirection, currDirection;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(!isRecyclerScrollable())
                    return false;

                switch (event.getActionMasked())
                {
                    case MotionEvent.ACTION_DOWN: {
                        initialY = event.getY();
                        break;
                    }

                    case MotionEvent.ACTION_MOVE:{
                        finalY = event.getY();

                        currDirection = (finalY > initialY )? Direction.UP : Direction.DOWN;
                       /* if(currDirection==Direction.DOWN)
                            if( initialY - finalY<200)
                                currDirection = Direction.UP;*/
                        if(prevDirection != currDirection) {
                            if(currDirection == Direction.UP) {
                                //TODO:((DashboardActivity) getActivity()).showFloatingMenu();
                            }
                            else {
                                //TODO:((DashboardActivity) getActivity()).hideFloatingMenu();
                            }
                        }

                        initialY = finalY;
                        prevDirection = currDirection;
                        break;
                    }

                    case MotionEvent.ACTION_UP: {
                        initialY = 0;
                        finalY = 0;
                        prevDirection = null;
                        currDirection = null;
                        break;
                    }
                }

                return false;
            }
        });
        view.findViewById(R.id.full_chart_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ChartActivity.class);
                SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_FORMAT, Locale.ENGLISH);
                String date = sdf.format(selectedDate.getTime());
                intent.putExtra("date", date);
                startActivity(intent);
            }
        });

        return view;
    }

    protected class DeleteDataTask extends AsyncTask<Void, Void, Boolean>
    {
        RealmObject ro;
        String eventId;

        protected DeleteDataTask(RealmObject ro, String eventId){
            this.ro = ro;
            this.eventId = eventId;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            JSONObject data = new JSONObject();

            Realm realm = null;
            boolean success = false;

            try {
                SharedPreferences sh = getActivity().getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
                String entityId = sh.getString("entity_id", "");
                data.put("EventId", eventId);
                data.put("EntityId", entityId);

                Request request = new Request.Builder()
                        .url("https://www.lifecare.sg/mlifecare/event/deleteEvent")
                        .post(RequestBody.create(LifeCareHandler.MEDIA_TYPE_JSON, data.toString()))
                        .build();

                Response response = LifeCareHandler.okclient.newCall(request).execute();
                JSONObject json = new JSONObject(response.body().string());
                Timber.d("JSON res = " + json.toString());

                success = !json.getBoolean("Error");
                //success
                if(success) {

                    mDataList.remove(ro);

                    realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

                    realm.beginTransaction();
                    if (ro != null) {
                        RealmObject roNew = realm.copyToRealmOrUpdate(ro);
                        roNew.deleteFromRealm();
                    }
                    realm.commitTransaction();

                    Timber.d("Deleted RO!");

                }else{
                    Timber.d("Did not delete RO!");
                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(realm!=null) {
                    realm.close();
                }
            }
            return success;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(success){
                Toast.makeText(getActivity(),"Successfully removed data!", Toast.LENGTH_SHORT).show();
                mAdapter.notifyDataSetChanged();

                if(mDataList.size()>0) {
                    mViewNoActivity.setVisibility(View.INVISIBLE);
                }
                else{
                    mViewNoActivity.setVisibility(View.VISIBLE);
                }

            }else{
                Toast.makeText(getActivity(),"Failed to remove data!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void setRefreshing(boolean refreshing){
        if(swipeContainer!=null){
            swipeContainer.setRefreshing(refreshing);
        }
    }

    public boolean isRecyclerScrollable() {
        return mRecyclerView.computeHorizontalScrollRange() > mRecyclerView.getWidth()
                || mRecyclerView.computeVerticalScrollRange() > mRecyclerView.getHeight();
    }

    public void updateReminder(){

        final LinearLayout reminderLayout = (LinearLayout)view.findViewById(R.id.reminder);

        PendingReminder pendingReminder = ReminderHandler.getInstance().getFirstUnshownPendingReminder();

        if(pendingReminder!=null) {

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mViewNoActivity.getLayoutParams();
            params.addRule(RelativeLayout.BELOW, R.id.reminder);
            mViewNoActivity.setLayoutParams(params);

            reminderLayout.setVisibility(View.VISIBLE);

            Reminder reminder = ReminderHandler.getInstance().getReminderFromPendingId(pendingReminder.getId());

            String title = reminder.getTitle();
            int hour = reminder.getHour();
            int min = reminder.getMin();
            String time = (hour < 10 ? "0" : "") + (hour > 12 ? hour-12 : hour) + ":"
                    + (min < 10 ? "0" : "") + min + " " + (hour >= 12 ? "PM" : "AM");

            ((TextView)view.findViewById(R.id.tvReminderTitle)).setText(title);
            ((TextView)view.findViewById(R.id.tvReminderTime)).setText(time);

            view.findViewById(R.id.close_button).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    reminderLayout.setVisibility(View.GONE);
                    RelativeLayout.LayoutParams params = ( RelativeLayout.LayoutParams) mViewNoActivity.getLayoutParams();
                    params.removeRule(RelativeLayout.BELOW);
                    mViewNoActivity.setLayoutParams(params);
                }
            });
        }
        else{
            reminderLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Timber.d("onActivityCreated");

        FragmentActivity parent = getActivity();
        if (parent instanceof DashboardActivity) {
            if(((DashboardActivity)parent).isCaregiver()) {
                if(EnterpriseHandler.getCurrentEnterprise()==EnterpriseHandler.GENERAL) {
                    ((DashboardActivity) parent).setToolbar(R.string.title_timeline, R.drawable.ic_toolbar_back, R.drawable.patient_list_icon);
                }else{
                    ((DashboardActivity) parent).setToolbar(R.string.title_timeline, R.drawable.ic_toolbar, R.drawable.patient_list_icon);
                }
                //((DashboardActivity) parent).setToolbar(R.string.title_timeline, R.drawable.ic_toolbar, R.drawable.patient_list_icon, R.drawable.dashboard_overview);
            }else {
                if(EnterpriseHandler.getCurrentEnterprise()==EnterpriseHandler.GENERAL) {
                    ((DashboardActivity) parent).setToolbar(R.string.title_timeline, R.drawable.ic_toolbar_back);
                }else{
                    ((DashboardActivity) parent).setToolbar(R.string.title_timeline, R.drawable.ic_toolbar);
                }
            }
            ((DashboardActivity)parent).setToolbarListener(mToolbarListener);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Timber.d("onDestroy");
    }

    @Override
    public void onClick(View v) {

        Timber.d("onClick");

        switch (v.getId()) {
            case R.id.image_next:
                if (mListener != null) {
                    mListener.onNextClick();
                }
                break;

            case R.id.image_previous:
                if (mListener != null) {
                    mListener.onPreviousClick();
                }
                break;
        }
    }

    public void setListener(TimelineFragmentListener listener) {
        mListener = listener;
    }

    public boolean getPatientData(Calendar date) {
        if (mPatientDataTask != null) {
            if (mPatientDataTask.getStatus() == AsyncTask.Status.RUNNING) {
                return false;
            }
            mPatientDataTask = null;
        }

        mPatientDataTask = new PatientDataTask(date);
        mPatientDataTask.execute();

        return true;
    }

    @Override
    public void onResume(){
        super.onResume();
        Timber.d("ON RESUME!");
        updateReminder();
        setListener(((DashboardActivity)getActivity()));
        //if(!getActivity().getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE).getBoolean("is_caregiver",false)) {
        getPatientData(((DashboardActivity) getActivity()).getTimelineDate());

        setDate(((DashboardActivity)getActivity()).mTimelineDate, ((DashboardActivity)getActivity()).isToday());
    }

    private void startAsyncTaskTransaction(Calendar date) {
        cancelAsyscTaskTransaction();

        mPatientDataTask = new PatientDataTask(date);
        mPatientDataTask.execute();

    }

    private void cancelAsyscTaskTransaction() {
        if (mPatientDataTask != null) {
            if (mPatientDataTask.getStatus() == AsyncTask.Status.RUNNING) {
                mPatientDataTask.cancel(true);
            }
            mPatientDataTask = null;
        }
    }

    public Calendar getCurrentDate(){
        return selectedDate;
    }

    public void setDate(Calendar date, boolean isToday) {
        SimpleDateFormat sdf = new SimpleDateFormat(PatientData.DATE_DAY_FORMAT);
        String s = sdf.format(date.getTime());
        selectedDate = date;
        mTextDate.setText(s);

        if (isToday) {
            mTextDate.setText(getText(R.string.today));
            mImageNext.setImageResource(R.drawable.calendar_next_disable);
            mImageNext.setClickable(false);
        } else {
            mImageNext.setImageResource(R.drawable.calendar_next);
            mImageNext.setClickable(true);
        }
    }

    private class PatientDataTask extends AsyncTask<Void, Void, Boolean> {

        private Calendar mDate;

        public PatientDataTask(Calendar date) {
            this.mDate = date;
        }

        @Override
        protected void onPreExecute() {

            if (mListener != null) {
                mListener.onRequestStart();
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

            Map<Date, RealmObject> results =
                    PatientData.getInstance().getAllByDate(realm, mDate, true);

            if(mDataList!=null) {
                Timber.d("clear mdatalist");
                mDataList.clear();
            }
            else {
                Timber.d("new mdatalist");
                mDataList = new ArrayList<>();
            }

            Timber.d("PatientDataTask: size=" + results.size());
            if ((results != null) && (results.size() > 0)) {

                for (Map.Entry<Date,RealmObject> entry :results.entrySet()) {
                    mDataList.add(realm.copyFromRealm(entry.getValue()));
                }

                Timber.d("PatientDataTask: list size=" + mDataList.size());

                realm.close();
                Timber.d("realm closed");

                return true;
            }

            realm.close();
            Log.d("TESTT","timeline = doinbg - close realm!");

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Timber.d("onPostExecute");
            //mDataList.notifyAll();
            //mAdapter.addAll(mDataList);

            mAdapter.notifyDataSetChanged();

            Timber.d("notifydatasetchanged");
            if(result) {
                mViewNoActivity.setVisibility(View.INVISIBLE);
            }
            else{
                mViewNoActivity.setVisibility(View.VISIBLE);
            }

            //updateChart();

            if (mListener != null) {
                mListener.onRequestFinish();
            }
        }
    }

    public void clearData(){
        if(mDataList!=null) {
            mDataList.clear();
        }
        selectedDate = Calendar.getInstance();
        if(mViewNoActivity!=null) {
            mViewNoActivity.setVisibility(View.GONE);
        }
    }

    private CustomToolbar.OnToolbarClickListener mToolbarListener = new CustomToolbar.OnToolbarClickListener() {
        @Override
        public void leftButtonClick() {
            if(EnterpriseHandler.getCurrentEnterprise()==EnterpriseHandler.GENERAL) {
                ((DashboardActivity) getActivity()).onBackPressed();
            }else {
                ((DashboardActivity)getActivity()).openLeftDrawer();
            }
        }

        @Override public void rightButtonClick() {
            if(((DashboardActivity) getActivity()).isCaregiver()) {
                ((DashboardActivity) getActivity()).openRightDrawer();
            }/*else{
                ((DashboardActivity) getActivity()).onBackPressed();
            }*/
        }

        @Override public void secondRightButtonClick() {
            //((DashboardActivity)getActivity()).collapseFloatingMenu();
            /*if(((DashboardActivity)getActivity()).isCaregiver()) {
                ((DashboardActivity)getActivity()).onBackPressed();
            }*/
        }
    };

    public void onPause(){
        super.onPause();
    }

    public void scrollToTop(){
        mScrollView.smoothScrollTo(0,0);
        //((DashboardActivity)getActivity()).showFloatingMenu();
    }
}
