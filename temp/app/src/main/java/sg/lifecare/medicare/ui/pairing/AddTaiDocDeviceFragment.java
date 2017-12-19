/*
package sg.lifecare.medicare.ui.pairing;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.ble.BleException;

*/
/**
 * Add TaiDoc device
 *//*

public class AddTaiDocDeviceFragment extends AddDeviceFragment {

    public static AddTaiDocDeviceFragment newInstance() {
        return new AddTaiDocDeviceFragment();
    }

    private static final String TAG = "AddTaiDocDeviceFragment";

    private TD1261Termometer mTD1261Termometer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mTD1261Termometer = new TD1261Termometer(getActivity(), mMeterListener);
        } catch (BleException e) {
            LogUtil.e(TAG, e.getMessage());
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

        mTD1261Termometer.startScanning();
    }

    @Override
    public void onPause() {
        super.onPause();

        mTD1261Termometer.stopScanning();
    }

    private TDMeterListener mMeterListener = new TDMeterListener() {
        @Override
        public void onResult(List<AbstractRecord> records) {

        }

        @Override
        public void onDeviceFound(final BluetoothDevice device) {
            myActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCallback.onPairSuccess(device.getAddress());
                }
            });

        }
    };
}
*/
