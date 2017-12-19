package sg.lifecare.medicare.ui.pairing;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.ble.BleException;
import sg.lifecare.medicare.ble.aandd.AAndD651BloodPressure;
import sg.lifecare.medicare.ble.aandd.AAndDMeter;
import sg.lifecare.medicare.ble.aandd.AAndDMeterListener;
import sg.lifecare.medicare.ble.profile.AbstractProfile;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.ui.pairing.AddDeviceFragment.OnPairingDetected;
import sg.lifecare.medicare.utils.MedicalDevice.Model;

/**
 * Get reading from A&D device
 */
public class GetReadingFragment extends Fragment {

    protected OnPairingDetected mCallback;
    public static GetReadingFragment newInstance() {
        return new GetReadingFragment();
    }

    private static final String TAG = "GetReadingFragment";

    private AAndDMeter mAAndDMeter;

    protected ConvertGatewayActivity myActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"GetReadingFragment");
        myActivity = (ConvertGatewayActivity)this.getActivity();

        try {
            if (myActivity.getDevice() == Model.AANDD_UA_651) {
                mAAndDMeter = new AAndD651BloodPressure(getActivity(), mMeterListener,"");
            } else if (myActivity.getDevice() == Model.AANDD_UC_352) {
               // mAAndDMeter = new AAndD352WeighScale(getActivity(), mMeterListener,"");
            }
            mAAndDMeter.setMode(AAndDMeter.MODE_READING);
        } catch (BleException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater,
                             ViewGroup paramViewGroup, Bundle paramBundle) {
        View view = paramLayoutInflater.inflate(R.layout.add_device_step, paramViewGroup, false);

        view.findViewById(R.id.loading_view).setVisibility(View.GONE);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG,"Start Scanning");
        mAAndDMeter.startScanning();
    }

    @Override
    public void onPause() {
        super.onPause();

        mAAndDMeter.stopScanning();
    }

    private AAndDMeterListener mMeterListener = new AAndDMeterListener() {
        @Override
        public void onResult(List<AbstractProfile> records) {
            Log.d(TAG,"RESULTS RETRIEVED! " + records.size());
        }

        @Override
        public void onDeviceBonded(final BluetoothDevice device) {
            Log.d(TAG,"ON DEVICE BONDED! ");
            myActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCallback.onPairSuccess(device.getAddress());
                }
            });
        }

        @Override
        public void onDataRetrieved(BloodPressure bp) {

        }

        @Override
        public void onDataRetrieved(Weight weight) {

        }

        @Override
        public void onDataRetrieved(Object object) {

        }

        @Override
        public void onInvalidDataReturned() {

        }

        @Override
        public void onConnectionStateChanged(int status) {

        }
    };
}
