package sg.lifecare.medicare.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

import sg.lifecare.medicare.utils.HexUtil;

public abstract class Ble2 {

	private static final String TAG = "Ble";

    public static final UUID WEIGHT_SCALE_SERVICE = UUID.fromString("0000181d-0000-1000-8000-00805f9b34fb");
    public static final UUID DATE_TIME_CHARACTERISTIC = UUID.fromString("00002a08-0000-1000-8000-00805f9b34fb");
    public static final UUID WEIGHT_MEASUREMENT_CHARACTERISTIC = UUID.fromString("00002a9d-0000-1000-8000-00805f9b34fb");

    public static final UUID BLOOD_PRESSURE_SERVICE = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb");
    public static final UUID BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC = UUID.fromString("00002a35-0000-1000-8000-00805f9b34fb");

    public static final UUID DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID MANUFACTURER_NAME_CHARACTERISTIC = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    public static final UUID MODEL_NUMBER_CHARACTERISTIC = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
    public static final UUID SERIAL_NUMBER_CHARACTERISTIC = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
    public static final UUID HARDWARE_REVISION_CHARACTERISTIC = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");
    public static final UUID FIRMWARE_REVISION_CHARACTERISTIC = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID SOFTWARE_REVISION_CHARACTERISTIC = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb");
    public static final UUID SYSTEM_ID_CHARACTERISTIC = UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb");


    // some error codes not defined in BluetoothGatt
    public static final int GATT_ERROR = 133;

	private Context mContext;

	private BluetoothManager mBluetoothManager;
	protected BluetoothAdapter mBluetoothAdapter;
	protected BluetoothDevice mBluetoothDevice;
	protected BluetoothGatt mBluetoothGatt;
	protected boolean mConnected;
	protected boolean mIsScanning;

	protected static final UUID DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			Log.d(TAG, "=====================================================");
			Log.d(TAG, "name       : " + device.getName());
			Log.d(TAG, "address    : " + device.getAddress());
			Log.d(TAG, "bond state : " + device.getBondState());
			Log.d(TAG, "type       : " + device.getType());
			Log.d(TAG, "rssi       : " + rssi);
			Log.d(TAG, "scanRecord : " + HexUtil.toHexString(scanRecord));
            Log.d(TAG, "RawScanInfo: " + Arrays.toString(scanRecord));
			Log.d(TAG, "=====================================================");

            onBleScan(device, rssi, scanRecord);
		}
	};

	protected BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {

	    private String getStatus(int status) {
	        switch (status) {
    	        case BluetoothGatt.GATT_SUCCESS:
                    return "GATT_SUCCESS";

    	        case BluetoothGatt.GATT_FAILURE:
    	            return "GATT_FAILURE";

    	        case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
                    return "GATT_INSUFFICIENT_AUTHENTICATION";

    	        case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
                    return "GATT_INSUFFICIENT_ENCRYPTION";

    	        case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH:
                    return "GATT_INVALID_ATTRIBUTE_LENGTH";

    	        case BluetoothGatt.GATT_INVALID_OFFSET:
                    return "GATT_INVALID_OFFSET";

    	        case BluetoothGatt.GATT_READ_NOT_PERMITTED:
                    return "GATT_READ_NOT_PERMITTED";

    	        case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
                    return "GATT_REQUEST_NOT_SUPPORTED";

    	        case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                    return "GATT_WRITE_NOT_PERMITTED";

                case GATT_ERROR:
                    return "GATT_ERROR";

    	        case 8:
    	            return "GATT_INSUFFICIENT_AUTHORIZATION";
	        }

	        return "STATUS_UNKNOWN (" + status + ")";
	    }

	    private String getState(int state) {
	        switch (state) {

	            case BluetoothProfile.STATE_CONNECTED:
	                return "STATE_CONNECTED";

	            case BluetoothProfile.STATE_DISCONNECTED:
                    return "STATE_DISCONNECTED";

	            case BluetoothProfile.STATE_CONNECTING:
                    return "STATE_CONNECTING";

	            case BluetoothProfile.STATE_DISCONNECTING:
                    return "STATE_DISCONNECTING";
	        }

	        return "STATE_UNKNOWN (" + state + ")";
	    }

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "123");
			Log.d(TAG, "onConnectionStateChange: status=" + getStatus(status) + " newState=" + getState(newState));

			if (newState == BluetoothProfile.STATE_CONNECTED) {

				mConnected = true;

                try {
                    Log.d(TAG, "sleep(500ms)");
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
				gatt.discoverServices();
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				mConnected = false;
				resetTxQueue();
			}

            onBleConnectionStateChange(gatt, status, newState);
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			Log.d(TAG, "onServicesDiscovered : status=" + getStatus(status));

            onBleServicesDiscovered(gatt, status);
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			Log.d(TAG, "onReadRemoteRssi: rssi=" + rssi + "  status=" + getStatus(status));

			if (status == BluetoothGatt.GATT_SUCCESS) {
                onBleReadRemoteRssi(gatt, rssi, status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
			Log.d(TAG, "onCharacteristicRead: status=" + getStatus(status));
            Log.d(TAG, "uuid  : " + characteristic.getUuid().toString());
            Log.d(TAG, "value : " + HexUtil.toHexString(characteristic.getValue()));

            onBleCharacteristicRead(gatt, characteristic, status);

			processTxQueue();
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
			Log.d(TAG, "onCharacteristicChanged");
			Log.d(TAG, "uuid  : " + characteristic.getUuid().toString());
			Log.d(TAG, "value : " + HexUtil.toHexString(characteristic.getValue()));

            onBleCharacteristicChanged(gatt, characteristic);

			//processTxQueue();
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
			Log.d(TAG, "onCharacteristicWrite : " + getStatus(status));
			Log.d(TAG, "uuid  : " + characteristic.getUuid().toString());
            Log.d(TAG, "value : " + HexUtil.toHexString(characteristic.getValue()));

            onBleCharacteristicWrite(gatt, characteristic, status);

			processTxQueue();
		}

		@Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            Log.d(TAG, "onDescriptorWrite : status=" + getStatus(status));

            onBleDescriptorWrite(gatt, descriptor, status);

            processTxQueue();
        }
	};

	public Ble2(Context context) throws BleException {
		mContext = context;

		// check if BLE supported
		mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
		if (mBluetoothManager == null) {
			throw new BleException("Bluetooth not supported on this device");
		}

		if (!mContext.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			throw new BleException("Bluetooth LE not supported on this device");
		}

		mBluetoothAdapter = mBluetoothManager.getAdapter();

		if (mBluetoothAdapter == null) {
			throw new BleException("Fail to get Bluetooth adpater");
		}
	}

    protected abstract void onBleScan(BluetoothDevice device, int rssi, byte[] scanRecord);

    protected abstract void onBleConnectionStateChange(BluetoothGatt gatt, int status, int newState);

    protected abstract void onBleServicesDiscovered(BluetoothGatt gatt, int status);

    protected abstract void onBleReadRemoteRssi(BluetoothGatt gatt, int rssi, int status);

    protected abstract void onBleCharacteristicRead(BluetoothGatt gatt,
                         BluetoothGattCharacteristic characteristic, int status);

    protected abstract void onBleCharacteristicChanged(BluetoothGatt gatt,
                            BluetoothGattCharacteristic characteristic);

    protected abstract void onBleCharacteristicWrite(BluetoothGatt gatt,
                          BluetoothGattCharacteristic characteristic, int status);

    protected abstract void onBleDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                     int status);

    public String getBondState(int state) {
        switch (state) {
            case BluetoothDevice.BOND_BONDED:
                return "BOND_BONDED";

            case BluetoothDevice.BOND_NONE:
                return "BOND_NONE";

            case BluetoothDevice.BOND_BONDING:
                return "BOND_BONDING";
        }

        return "UNKNOWN";
    }

	/**
	 * Check the Bluetooth is enabled
	 * 
	 * @return true if enabled, false otherwise
	 */
	public boolean isBleEnabled() {
		return mBluetoothAdapter.isEnabled();
	}

	/**
	 * Check the BLE device connected
	 * 
	 * @return true if connected, false otherwise
	 */
	public boolean isConnected() {
	    Log.d(TAG, "isConnected: " + mConnected);
		return mConnected;
	}

	/**
	 * Start scanning for BLE device
	 */
	@SuppressWarnings("deprecation")
    public void startScanning() {		
		if (!mIsScanning) {
		    Log.d(TAG, "start scanning");
		    mBluetoothAdapter.startLeScan(mLeScanCallback);
		    mIsScanning = true;
		}
	}

	/**
	 * Stop scanning for BLE device
	 */
	@SuppressWarnings("deprecation")
    public void stopScanning() {
	    if (mIsScanning) {
    		Log.d(TAG, "stop scanning");
    		mBluetoothAdapter.stopLeScan(mLeScanCallback);
    		
    		/*try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                
            }*/
    		mIsScanning = false;
	    }
	}

    public void unpairDevice(String deviceId){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for(BluetoothDevice bt : pairedDevices){
            if(bt.getAddress().equalsIgnoreCase(deviceId)){
                try {
                    Log.d(TAG,"Unpairing Device! @unpairDevice");
                    Method m = bt.getClass()
                            .getMethod("removeBond", (Class[]) null);
                    m.invoke(bt, (Object[]) null);
                    Log.d(TAG,"Unpaired Device! @unpairDevice");
                } catch (Exception e) {
                    Log.e(TAG, "UnpairDevice Error!\n" + e.getMessage());
                }
            }
        }
    }

	public boolean isScanning() {
	    return mIsScanning;
	}
	
	public void disconnect() {
        if (mBluetoothGatt != null) {
            if (mIsScanning) {
                startScanning();
            }

            mBluetoothGatt.disconnect();
            //mBluetoothGatt.close();
        }
    }

    public void close() {
        if (mBluetoothGatt != null) {
            //mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        mConnected = false;
    }

	/**
	 * Connect to BLE device
	 * 
	 * @param deviceAddress
	 *            BLE device address
	 * @return true if connected, false otherwise
	 */
	public boolean connectDevice(final String deviceAddress) {
		Log.d(TAG, "connectDevice: deviceAddress=" + deviceAddress);

		if (TextUtils.isEmpty(deviceAddress)) {
			return false;
		}

		if (!BluetoothAdapter.checkBluetoothAddress(deviceAddress.toUpperCase())) {
			Log.w(TAG, "Invalid address");
			return false;
		}

		/*if (mBluetoothGatt != null && deviceAddress.equalsIgnoreCase(mBluetoothGatt.getDevice().getAddress())) {
		    boolean reconnect = mBluetoothGatt.connect();
		    
		    Log.d(TAG, "connectDevice: reconnect=" + reconnect);
		    
			return mBluetoothGatt.connect();
		}*/
		if (mBluetoothGatt != null) {
		    mBluetoothGatt.close();
		}

		mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);
		if (mBluetoothDevice == null) {
			Log.w(TAG, "Device not available");
			return false;
		}

        Log.d(TAG, "Connected to GATT - callback");
        try {
            mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mBluetoothGattCallback);
            if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                mBluetoothDevice.setPairingConfirmation(true);
            }
        }
        catch(IllegalArgumentException e){
            Log.e(TAG,e.getMessage());
        }
        Log.d(TAG, "Connected to GATT - callback222");
		return true;
	}
	
	/**
     * Queue the write operation - notification subscription or characteristic
     * write
     */
    private class TxQueueItem {
        BluetoothGattCharacteristic characteristic;
        byte[] dataToWrite; // only used for characteristic write
        boolean enabled; // only used for characteristic subscription
        public TxQueueItemType type;
    }

    private LinkedList<TxQueueItem> mTxQueue = new LinkedList<TxQueueItem>();
    private boolean mTxQueueProcessing = false;
    private Object mQueueLock = new Object();

    private enum TxQueueItemType {
        SubscribeCharacteristic, ReadCharacteristic, WriteCharacteristic, IndicationCharacteristic
    }

    /**
     * Add enable/disable indication for characteristic to queue
     *
     * @param ch
     * @param enabled
     */
    protected void queueSetIndicationForCharacteristic(BluetoothGattCharacteristic ch, boolean enabled) {
        TxQueueItem item = new TxQueueItem();
        item.characteristic = ch;
        item.enabled = enabled;
        item.type = TxQueueItemType.IndicationCharacteristic;
        addToTxQueue(item);
    }

    /**
     * Add enable/disable notification for characteristic to queue
     * 
     * @param ch
     * @param enabled
     */
    protected void queueSetNotificationForCharacteristic(BluetoothGattCharacteristic ch, boolean enabled) {
        TxQueueItem item = new TxQueueItem();
        item.characteristic = ch;
        item.enabled = enabled;
        item.type = TxQueueItemType.SubscribeCharacteristic;
        addToTxQueue(item);
    }

    /**
     * Add write to characteristic to queue
     * 
     * @param ch
     * @param dataToWrite
     */
    public void queueWriteDataToCharacteristic(final BluetoothGattCharacteristic ch, final byte[] dataToWrite) {
        TxQueueItem item = new TxQueueItem();
        item.characteristic = ch;
        item.dataToWrite = dataToWrite;
        item.type = TxQueueItemType.WriteCharacteristic;
        addToTxQueue(item);
    }
    
    /**
     * Add read to characteristic to queue
     * 
     * @param ch
     */
    public void queueReadDataToCharacteristic(final BluetoothGattCharacteristic ch) {
        TxQueueItem item = new TxQueueItem();
        item.characteristic = ch;
        item.type = TxQueueItemType.ReadCharacteristic;
        addToTxQueue(item);
    }

    private void addToTxQueue(TxQueueItem item) {
        
        synchronized(mQueueLock) {
            mTxQueue.addLast(item);
            Log.d(TAG, "addToTxQueue: mTxQueueProcessing=" + mTxQueueProcessing);
            if (!mTxQueueProcessing) {
                processTxQueue();
            }
        }
    }

    protected void resetTxQueue() {
        synchronized(mQueueLock) {
            mTxQueue.clear();
            mTxQueueProcessing = false;
        }
    }

    private void processTxQueue() {
        Log.d(TAG, "processTxQueue: size=" + mTxQueue.size());

        TxQueueItem item = null;
        
        synchronized(mQueueLock) {
            if (mTxQueue.size() <= 0) {
                mTxQueueProcessing = false;
                return;
            }
            
            mTxQueueProcessing = true;
            item = mTxQueue.removeFirst();
        }


        switch (item.type) {
            case ReadCharacteristic:
                readDataFromCharacteristic(item.characteristic);
                break;
            case SubscribeCharacteristic:
                setNotificationForCharacteristic(item.characteristic, item.enabled);
                break;
            case WriteCharacteristic:
                writeDataToCharacteristic(item.characteristic, item.dataToWrite);
                break;
            case IndicationCharacteristic:
                setIndicationForCharacteristic(item.characteristic, item.enabled);
                break;

            default:
                break;

        }
    }
    
    private void readDataFromCharacteristic(BluetoothGattCharacteristic ch) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null || ch == null) {
            Log.e(TAG, "readDataFromCharacteristic: failed");
            return;
        }
        
        Log.d(TAG, "readDataFromCharacteristic: uuid=" + ch.getUuid().toString());

        mBluetoothGatt.readCharacteristic(ch);
    }

    private void writeDataToCharacteristic(BluetoothGattCharacteristic ch, byte[] dataToWrite) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null || ch == null || dataToWrite == null) {
            Log.e(TAG, "writeDataToCharacteristic: failed");
            return;
        }

        Log.d(TAG, "writeDataToCharacteristic: uuid=" + ch.getUuid().toString() 
                + "  values=" + HexUtil.toHexString(dataToWrite));

        ch.setValue(dataToWrite);
        boolean success = mBluetoothGatt.writeCharacteristic(ch);

        Log.d(TAG, "successfully written ? = " + success);
    }

    private void setNotificationForCharacteristic(BluetoothGattCharacteristic ch, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null || ch == null) {
            Log.e(TAG, "setNotificationForCharacteristic: failed");
            return;
        }

        boolean success = mBluetoothGatt.setCharacteristicNotification(ch, enabled);
        if (!success) {
            Log.e(TAG, "set notification to characteristic failed!!!");
            return;
        }

        BluetoothGattDescriptor descriptor = ch.getDescriptor(DESCRIPTOR);
        if (descriptor == null) {
            Log.e(TAG, "Get characteristic descriptor failed!!!");
            return;
        }

        if (enabled) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }

        mBluetoothGatt.writeDescriptor(descriptor);
    }

    private void setIndicationForCharacteristic(BluetoothGattCharacteristic ch, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null || ch == null) {
            Log.e(TAG, "setNotificationForCharacteristic: failed");
            return;
        }

        boolean success = mBluetoothGatt.setCharacteristicNotification(ch, enabled);
        if (!success) {
            Log.e(TAG, "set notification to characteristic failed!!!");
            return;
        }

        BluetoothGattDescriptor descriptor = ch.getDescriptor(DESCRIPTOR);
        if (descriptor == null) {
            Log.e(TAG, "Get characteristic descriptor failed!!!");
            return;
        }

        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);


    }

}
