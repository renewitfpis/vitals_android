package sg.lifecare.medicare.ble.vivachek;

import android.bluetooth.BluetoothDevice;

import java.util.Date;
import java.util.List;

public interface VivaChekBloodGlucoseMeterListener {

    void onDeviceScan(BluetoothDevice device);

    void onReadResult(List<VivaChekBloodGlucoseMeter.CustomerHistory> histories);

    void onConnectionStateChanged(int status);
}
