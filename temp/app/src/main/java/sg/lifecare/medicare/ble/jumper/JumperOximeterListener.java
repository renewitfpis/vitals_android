package sg.lifecare.medicare.ble.jumper;

import android.bluetooth.BluetoothDevice;

public interface JumperOximeterListener {

    void onDeviceScan(BluetoothDevice device);

    void onReadResult(int sp02, int pulse, double pi);

    void onConnectionStateChanged(int status);
}
