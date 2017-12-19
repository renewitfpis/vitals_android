package sg.lifecare.medicare.ble.qn;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;

import com.kitnew.ble.QNApiManager;
import com.kitnew.ble.QNBleApi;
import com.kitnew.ble.QNBleCallback;
import com.kitnew.ble.QNBleDevice;
import com.kitnew.ble.QNBleScanCallback;
import com.kitnew.ble.QNData;
import com.kitnew.ble.QNItemData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class YolandaWeightMeter implements QNBleCallback, QNBleScanCallback {

    public static final String DEVICE_NAME = "QN-Scale";

    private YolandaWeightMeterListener mListener;

    private QNBleApi mQNBleApi;

    public YolandaWeightMeter(Activity activity, YolandaWeightMeterListener listener) {
        mListener = listener;

        mQNBleApi = QNApiManager.getApi(activity);
        mQNBleApi.setWeightUnit(QNBleApi.WEIGHT_UNIT_KG);
    }

    public void startScanning() {
        mQNBleApi.setScanMode(QNBleApi.SCAN_MODE_FIRST);
        mQNBleApi.startLeScan(null, null, this);
    }

    public void stopScanning() {
        mQNBleApi.stopScan();
    }

    public void connectBluetoothDevice(QNBleDevice qnBleDevice) {
        stopScanning();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-M-d", Locale.getDefault());

        try {
            final Date birthday = df.parse("1980-11-11");
            Timber.d("Birthday " + birthday.getYear());

            mQNBleApi.setUser(qnBleDevice.getMac(), "2", 170, 1, birthday);

            mQNBleApi.connectDevice(qnBleDevice, "2"  , 170, 1, birthday, this);
        } catch (ParseException e) {
            Timber.e(e);
        }


    }

    public void disconnect() {
        mQNBleApi.disconnectAll();
    }

    @Override
    public void onConnectStart(QNBleDevice qnBleDevice) {

    }

    @Override
    public void onConnected(QNBleDevice qnBleDevice) {

    }

    @Override
    public void onDisconnected(QNBleDevice qnBleDevice, int i) {

        mListener.onConnectionStateChanged(BluetoothAdapter.STATE_DISCONNECTED);

    }

    @Override
    public void onUnsteadyWeight(QNBleDevice qnBleDevice, float v) {

    }

    @Override
    public void onReceivedData(QNBleDevice qnBleDevice, QNData qnData) {
        List<QNItemData> items = qnData.getAll();

        for (QNItemData item : items) {
            Timber.d("name: %s, type=%d, value=%f, valueStr=%s", item.name, item.type,
                    item.value, item.valueStr);
        }

        mListener.onReadResult(qnData.getWeight());
    }

    @Override
    public void onReceivedStoreData(QNBleDevice qnBleDevice, List<QNData> list) {

    }

    @Override
    public void onDeviceModelUpdate(QNBleDevice qnBleDevice) {

    }

    @Override
    public void onLowPower() {

    }

    @Override
    public void onCompete(int i) {
        Timber.d("onComplete: %d", i);
    }

    @Override
    public void onScan(QNBleDevice qnBleDevice) {
        stopScanning();
        mListener.onDeviceScan(qnBleDevice);
    }
}
