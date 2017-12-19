package sg.lifecare.medicare.ui;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.ble.BleException;
import sg.lifecare.medicare.ble.aandd.AAndD352WeighScale2;
import sg.lifecare.medicare.ble.aandd.AAndDMeter;
import sg.lifecare.medicare.ble.aandd.AAndDMeterListener;
import sg.lifecare.medicare.ble.profile.AbstractProfile;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import timber.log.Timber;

/**
 * Dashboard activity
 */
public class DeviceReadingActivity2 extends AppCompatActivity {

    private static String TAG = "DeviceReadingActivity";

    private AAndDMeter mAAndDMeter;

    private RelativeLayout loadingView;
    private ProgressBar loading;

    private RelativeLayout readingView, preparationView;
    private TextView tvValue, tvDate;
    private Button finishButton, cancelButton;

    private MedicalDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Timber.tag("DeviceReadingActivity");

        setContentView(R.layout.device_reading);

        readingView = (RelativeLayout) findViewById(R.id.reading_view);
        preparationView = (RelativeLayout) findViewById(R.id.preparation_view);

        readingView.setVisibility(View.VISIBLE);
        preparationView.setVisibility(View.INVISIBLE);

        Intent intent = getIntent();
        if(intent.hasExtra("device"))
        {
            device = (MedicalDevice) intent.getSerializableExtra("device");
        }

        loadingView = (RelativeLayout) findViewById(R.id.loading_view);
        loading = (ProgressBar) findViewById(R.id.loading);
        /*loading.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.loading_prog_bar),
                android.graphics.PorterDuff.Mode.SRC_IN);*/

        CustomToolbar mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_activity_measurement_reading);
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        //mToolbar.setRightButtonImage(R.drawable.ic_toolbar_tick);
        mToolbar.hideRightButton();
        mToolbar.hideSecondRightButton();
        mToolbar.setListener(mToolbarListener);

        ImageView deviceIcon = (ImageView) findViewById(R.id.device_img);
        TextView deviceName = (TextView) findViewById(R.id.device_name);
        TextView deviceDesc = (TextView) findViewById(R.id.device_desc);
        deviceName.setText(device.getAssignedName());
        deviceDesc.setText(device.getModel().toString());
        if(device.getMediaImageURL()!=null && !device.getMediaImageURL().isEmpty()) {
            Picasso.with(this).load(device.getMediaImageURL()).into(deviceIcon);
        }else {
            if(device.getModel() == Model.AANDD_UA_651){
                Picasso.with(this).load(R.drawable.automatic_pair_bp).into(deviceIcon);
            }else if(device.getModel() == Model.AANDD_UC_352){
                Picasso.with(this).load(R.drawable.weighscale).into(deviceIcon);
            }else if(device.getModel() == Model.TERUMO_MEDISAFE_FIT){
                Picasso.with(this).load(R.drawable.glucose_icon).into(deviceIcon);
            }
        }

        TextView tvReadingTitle = (TextView) findViewById(R.id.reading_title);
        tvValue = (TextView) findViewById(R.id.reading_value);
        tvDate = (TextView) findViewById(R.id.taken_value);

        finishButton = (Button) findViewById(R.id.finish_button);
        finishButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
               finish();
            }
        });

        cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
       // Model model = getIntent().getExtras();
        Model model = device.getModel();
        Log.d(TAG,"Model @DRA = " + model);

        try {
            if (model == Model.AANDD_UA_651) {
                tvReadingTitle.setText("Blood Pressure Level");
                mAAndDMeter = new AAndD352WeighScale2(this, mMeterListener);
            } else if (model == Model.AANDD_UC_352) {
                tvReadingTitle.setText("Weight");
                mAAndDMeter = new AAndD352WeighScale2(this, mMeterListener);
            }
            mAAndDMeter.setMode(AAndDMeter.MODE_READING);

        } catch (BleException e) {
            Log.e(TAG, e.getMessage());
        }

    }

    private AAndDMeterListener mMeterListener = new AAndDMeterListener() {
        @Override
        public void onResult(List<AbstractProfile> records) {
            Log.d(TAG,"RESULTS RETRIEVED! " + records.size());
        }

        @Override
        public void onDeviceBonded(final BluetoothDevice device) {
            Log.d(TAG,"ON DEVICE BONDED! ");

        }

        @Override
        public void onDataRetrieved(final BloodPressure bp) {
            Log.d(TAG,"ON BP DATA RETRIEVED: " + bp.getSystolic() + "/" + bp.getDistolic());

            mAAndDMeter.startScanning();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvValue.setText(bp.getSystolic() + "/" + bp.getDistolic() + " " + bp.getStringUnit() );
                    tvDate.setText(bp.getStringDate());
                }
            });
        }

        @Override
        public void onDataRetrieved(final Weight weight) {
            Log.d(TAG,"ON WEIGHT DATA RETRIEVED: " + weight.getWeight() + " " + weight.getStringUnit());

            mAAndDMeter.startScanning();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"ON WEIGHT: " + weight.getWeight() + " " + weight.getStringUnit());
                    tvValue.setText(weight.getWeight() + " " + weight.getStringUnit());
                    tvDate.setText(weight.getStringDate());
                    Log.d(TAG,"ON WEIGHT2: " + weight.getWeight() + " " + weight.getStringUnit());

                }
            });
        }

        @Override
        public void onDataRetrieved(Object object) {

        }

        @Override
        public void onInvalidDataReturned() {
            mAAndDMeter.startScanning();
        }

        @Override
        public void onConnectionStateChanged(int status) {

        }
    };

    protected void onResume(){
        super.onResume();

        mAAndDMeter.startScanning();
    }

    protected void onPause(){
        super.onPause();

        mAAndDMeter.stopScanning();
    }

    private CustomToolbar.OnToolbarClickListener mToolbarListener = new CustomToolbar.OnToolbarClickListener() {
        @Override
        public void leftButtonClick() {
            onBackPressed();
        }

        @Override public void rightButtonClick() {

        }

        @Override public void secondRightButtonClick() {

        }
    };
}
