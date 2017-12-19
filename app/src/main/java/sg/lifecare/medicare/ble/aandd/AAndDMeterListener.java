package sg.lifecare.medicare.ble.aandd;

/**
 * Created by sweelai on 1/7/16.
 */

import android.bluetooth.BluetoothDevice;

import java.util.List;

import sg.lifecare.medicare.ble.profile.AbstractProfile;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Weight;

public interface AAndDMeterListener {
    void onResult(List<AbstractProfile> records);
    void onDeviceBonded(BluetoothDevice device);
    void onDataRetrieved(BloodPressure bp);
    void onDataRetrieved(Weight weight);
    void onDataRetrieved(Object object);
    void onInvalidDataReturned();
    void onConnectionStateChanged(int status);
}
