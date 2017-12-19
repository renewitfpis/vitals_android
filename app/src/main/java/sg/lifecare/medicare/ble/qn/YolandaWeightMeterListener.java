package sg.lifecare.medicare.ble.qn;

import com.kitnew.ble.QNBleDevice;

public interface YolandaWeightMeterListener {

    void onDeviceScan(QNBleDevice device);

    void onReadResult(double weigh);

    void onConnectionStateChanged(int status);
}
