package sg.lifecare.medicare.ble.urion;

import android.bluetooth.BluetoothDevice;

public interface UrionBloodPressureMeterListener {

    void onDeviceScan(BluetoothDevice device);

    void onReadResult(int systolic, int diastopic, int pulse);

    void onReadPulse(int pulse);

    void onConnectionStateChanged(int status);
}
