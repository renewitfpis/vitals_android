package sg.lifecare.medicare.ble.jumper;

import android.bluetooth.BluetoothDevice;

public interface JumperThermometerListener {

    void onDeviceScan(BluetoothDevice device);

    void onReadResult(double temperature);

    void onConnectionStateChanged(int status);
}
