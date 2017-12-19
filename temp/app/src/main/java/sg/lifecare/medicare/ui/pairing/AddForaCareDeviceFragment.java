/*
package sg.lifecare.medicare.ui.pairing;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import com.singtel.lifecare.ble.BleException;
import com.singtel.lifecare.ble.taidoc.TD3261BloodPressureGlucoseMeter;
import com.singtel.lifecare.ble.taidoc.TDMeter;
import com.singtel.lifecare.ble.taidoc.TDMeterListener;
import com.singtel.lifecare.ble.taidoc.record.AbstractRecord;
import com.singtel.lifecare.utils.LogUtil;

import java.util.List;

*/
/**
 * Add ForaCare devices
 *//*

public class AddForaCareDeviceFragment extends AddDeviceFragment {

    public static AddForaCareDeviceFragment newInstance() {
        return new AddForaCareDeviceFragment();
    }

    private static final String TAG = "AddForaCareDeviceFragment";

    private TDMeter mTDMeter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mTDMeter = new TD3261BloodPressureGlucoseMeter(getActivity(), mMeterListener);
        } catch (BleException e) {
            LogUtil.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mTDMeter.startScanning();
    }

    @Override
    public void onPause() {
        super.onPause();

        mTDMeter.stopScanning();
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
