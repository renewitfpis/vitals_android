package sg.lifecare.medicare.ui.fragment;

import android.Manifest;
import android.Manifest.permission;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmResults;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.ui.ChartActivity;
import sg.lifecare.medicare.ui.DashboardActivity;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import timber.log.Timber;

/**
 * Terumo fragment for dashboard
 */
public class TerumoOverviewFragment extends Fragment {

    private static String TAG = "TerumoOverviewFragment";

    public final static int MY_PERMISSIONS_REQUEST_BLE_AND_POP_UP = 102;

    public static TerumoOverviewFragment newInstance() {
        return new TerumoOverviewFragment();
    }

    private PieChart mChart;
    private TextView textHigh, textLow, textNormal, textDate;
    private int countHigh, countLow, countNormal;

    private Realm realm;
    private Terumo latestTerumo;
    private View view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Timber.tag(TAG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_overview_terumo, container, false);

        textNormal = (TextView) view.findViewById(R.id.text_normal);
        textHigh = (TextView) view.findViewById(R.id.text_high);
        textLow = (TextView) view.findViewById(R.id.text_low);
        textDate = (TextView) view.findViewById(R.id.text_date);

        mChart = (PieChart) view.findViewById(R.id.chart);
        mChart.setDescription("");
        mChart.setOnTouchListener(null);
        mChart.setClickable(true);
        mChart.setOnClickListener(navToChartListener);
        view.findViewById(R.id.chart_layout).setOnClickListener(navToChartListener);
        view.findViewById(R.id.image_navigate).setOnClickListener(navToChartListener);
        view.findViewById(R.id.latest_terumo_date_layout).setOnClickListener(navToChartListener);

        return view;
    }

    OnClickListener navToChartListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if(latestTerumo!=null){
                SimpleDateFormat dateFormat = new SimpleDateFormat(PatientData.DATE_FORMAT, Locale.ENGLISH);
                Intent intent = new Intent(getActivity(), ChartActivity.class);
                intent.putExtra("date",(dateFormat.format(latestTerumo.getDate())));
                startActivity(intent);
            }
        }
    };

    private void createChart(){
        view.setVisibility(View.VISIBLE);
        textNormal.setText(String.valueOf(countNormal));
        textHigh.setText(String.valueOf(countHigh));
        textLow.setText(String.valueOf(countLow));

        if(latestTerumo!=null) {
            textDate.setText(latestTerumo.getStringDate());
        }else{
            textDate.setText("--");
        }

        int[] yData;
        if(countHigh == 0 && countNormal == 0 && countLow == 0){
            yData = new int[]{ 0, 1, 0};
        }else {
            yData = new int[]{ countHigh, countNormal, countLow};
        }
        String[] xData = { "High", "Normal", "Low" };
        ArrayList<Entry> yVals1 = new ArrayList<>();

        for (int i = 0; i < yData.length; i++)
            yVals1.add(new Entry(yData[i], i));

        ArrayList<String> xVals = new ArrayList<>();

        for (int i = 0; i < xData.length; i++)
            xVals.add(xData[i]);

        // create pie data set
        final PieDataSet dataSet = new PieDataSet(yVals1, "Range of Blood Glucose");
        dataSet.setSliceSpace(3);
        dataSet.setSelectionShift(0);

        // add colors
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(getResources().getColor(R.color.pie_chart_pink));
        colors.add(getResources().getColor(R.color.pie_chart_green));
        colors.add(getResources().getColor(R.color.pie_chart_yellow));
        dataSet.setColors(colors);

        PieData data = new PieData(xVals, dataSet);
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.BLACK);
        data.setDrawValues(false);
        mChart.setData(data);

        String latestReadingStr;
        //center text
        if(latestTerumo!=null) {
            latestReadingStr = latestTerumo.getValue() + " \n" + latestTerumo.getStringUnit();
        }else{
            latestReadingStr = "--" + " \n" + "mmol/L";
        }
        latestReadingStr = latestReadingStr.replace(".0 \n","\n").replace(" \n","\n");
        int index = latestReadingStr.indexOf("\n");
        SpannableString latestReadingText = new SpannableString(latestReadingStr);
        latestReadingText.setSpan(new RelativeSizeSpan(2f),0,index, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        latestReadingText.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.black_lighter)),0, index, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        mChart.setCenterText(latestReadingText);
        mChart.setCenterTextColor(getResources().getColor(R.color.gray));
        mChart.setCenterTextSize(18f);
        mChart.getLegend().setEnabled(false);
        mChart.setDrawSliceText(false);

        // undo all highlights
        //mChart.highlightValues(null);

        // update pie chart
        mChart.invalidate();
    }

    public void clearData(){
        view.setVisibility(View.GONE);

    }
    private void checkPermission(){
        Timber.d("Checking Permission!");
        // Here, thisActivity is the current activity
        if (
                ContextCompat.checkSelfPermission(getActivity(),
                        permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                        ||
                        ContextCompat.checkSelfPermission(getActivity(),
                                permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED
                        ||
                        ContextCompat.checkSelfPermission(getActivity(),
                                permission.RECEIVE_BOOT_COMPLETED)
                                != PackageManager.PERMISSION_GRANTED
                        ||
                        ContextCompat.checkSelfPermission(getActivity(),
                                permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED
                        ||
                        ContextCompat.checkSelfPermission(getActivity(),
                                permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED
                        ||
                        ContextCompat.checkSelfPermission(getActivity(),
                                permission.READ_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED
                )
        {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    ||
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.ACCESS_COARSE_LOCATION)
                    ||
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            permission.RECEIVE_BOOT_COMPLETED)
                    ||
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.CAMERA)
                    ||
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            permission.WRITE_EXTERNAL_STORAGE)
                    ||
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.READ_EXTERNAL_STORAGE)
                    )
            {
                Timber.d("Checking Permission!");
                showMessageOKCancel("You need to allow location access for bluetooth features.",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(getActivity(),
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                                permission.ACCESS_COARSE_LOCATION,
                                                permission.RECEIVE_BOOT_COMPLETED,
                                                Manifest.permission.CAMERA,
                                                permission.WRITE_EXTERNAL_STORAGE,
                                                permission.READ_EXTERNAL_STORAGE
                                                /*permission.SYSTEM_ALERT_WINDOW,
                                                permission.RECEIVE_BOOT_COMPLETED*/},
                                        MY_PERMISSIONS_REQUEST_BLE_AND_POP_UP);
                            }
                        });
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                Timber.d("Requesting Permission!");

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                permission.ACCESS_COARSE_LOCATION,
                                permission.RECEIVE_BOOT_COMPLETED,
                                Manifest.permission.CAMERA,
                                permission.WRITE_EXTERNAL_STORAGE,
                                permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_BLE_AND_POP_UP);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        else if(ContextCompat.checkSelfPermission(getActivity(),permission.RECEIVE_BOOT_COMPLETED)
                == PackageManager.PERMISSION_GRANTED){
            Timber.i("User granted start up permission");
        }
        else if(ContextCompat.checkSelfPermission(getActivity(),permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getActivity(),permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getActivity(),permission.RECEIVE_BOOT_COMPLETED)
                        == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getActivity(),permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getActivity(),permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getActivity(),permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED

                )
        {
            Timber.i("User granted some or all permissions");
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_BLE_AND_POP_UP: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    // ((CameraFragment)fragment).restartPreview();
                    Log.d(TAG,"Location Permission granted!");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    Log.d(TAG,"Location Permission denied!");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }

                if (grantResults.length > 0
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                    // ((CameraFragment)fragment).restartPreview();
                    Log.d(TAG,"POP UP Permission granted!");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    Log.d(TAG,"POP UP Permission denied!");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                if (grantResults.length > 0
                        && grantResults[3] == PackageManager.PERMISSION_GRANTED) {

                    // ((CameraFragment)fragment).restartPreview();
                    Log.d(TAG,"CAM Permission granted!");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    Log.d(TAG,"CAM Permission denied!");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }

                if (grantResults.length > 0
                        && grantResults[4] == PackageManager.PERMISSION_GRANTED) {

                    // ((CameraFragment)fragment).restartPreview();
                    Log.d(TAG,"External Storage Permission granted!");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    Log.d(TAG,"External Storage Permission denied!");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Timber.d("onActivityCreated");

        checkPermission();
        FragmentActivity parent = getActivity();
        if (parent instanceof DashboardActivity) {
            if(((DashboardActivity)parent).isCaregiver()) {
                ((DashboardActivity) parent).setToolbar(R.string.title_overview, R.drawable.ic_toolbar, R.drawable.patient_list_icon);
            }else{
                ((DashboardActivity) parent).setToolbar(R.string.title_overview, R.drawable.ic_toolbar);
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
    public void onResume(){
        super.onResume();
        Log.d(TAG,"ON RESUME!");
        if(getActivity() instanceof DashboardActivity) {
            ((DashboardActivity) getActivity()).selectTab(R.id.nav_bar_overview);
        }

        try {
            new RetrieveVitalsDataTask().execute();
        }
        catch(IllegalStateException e){
            Timber.e("Illegal!!\n" + e.getMessage());
        }
    }

    private class RetrieveVitalsDataTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());

            RealmResults<Terumo> results =
                    PatientData.getInstance().getAllTerumo(realm);

            Timber.d("RetrieveVitalsDataTask: size=" + results.size());
            latestTerumo = null;
            countHigh = countNormal = countLow = 0;
            if ((results != null) && (results.size() > 0)) {
                latestTerumo = realm.copyFromRealm(results.get(results.size()-1));

                for (int i = 0; i < results.size(); i++){
                    Terumo terumo = results.get(i);
                    if(terumo.isBeforeMeal()){
                        if(terumo.getValue() > 7.0){
                            countHigh++;
                        }else if(terumo.getValue() < 4.0){
                            countLow++;
                        }else{
                            countNormal++;
                        }
                    }else{
                        if(terumo.getValue() > 9.0){
                            countHigh++;
                        }else if(terumo.getValue() < 4.0){
                            countLow++;
                        }else{
                            countNormal++;
                        }
                    }
                }
                Timber.d("Count Low = " + countLow + ", Count High = " + countHigh + ", Count Normal = " + countNormal);

                realm.close();

                Timber.d("RetrieveVitalsDataTask closed realm");

                return true;
            }

            realm.close();
            Timber.d("RetrieveVitalsDataTask closed realm2");
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Timber.d("onPostExecute");
            if(getActivity()!=null) {
                createChart();
            }
        }
    }


    private CustomToolbar.OnToolbarClickListener mToolbarListener = new CustomToolbar.OnToolbarClickListener() {
        @Override
        public void leftButtonClick() {
            ((DashboardActivity)getActivity()).openLeftDrawer();
        }

        @Override public void rightButtonClick() {
            if(((DashboardActivity)getActivity()).isCaregiver()) {
                ((DashboardActivity) getActivity()).openRightDrawer();
            }
        }

        @Override public void secondRightButtonClick() {

        }
    };

    @Override
    public void onPause(){
        super.onPause();

       /* if(realm!=null) {
            realm.close();
            realm = null;
        }*/
    }

}
