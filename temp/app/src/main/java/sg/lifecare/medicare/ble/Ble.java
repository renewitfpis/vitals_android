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
import timber.log.Timber;

public abstract class Ble {

	private static final String TAG = "Ble";

    public static final int FORCED_CANCEL = 231;

    public final static UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    public static final UUID WEIGHT_SCALE_SERVICE = UUID.fromString("0000181d-0000-1000-8000-00805f9b34fb");
    public static final UUID DATE_TIME_CHARACTERISTIC = UUID.fromString("00002a08-0000-1000-8000-00805f9b34fb");
    public static final UUID WEIGHT_MEASUREMENT_CHARACTERISTIC = UUID.fromString("00002a9d-0000-1000-8000-00805f9b34fb");

    public static final UUID BLOOD_PRESSURE_SERVICE = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb");
    public static final UUID BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC = UUID.fromString("00002a35-0000-1000-8000-00805f9b34fb");

    public static final UUID HEALTH_THERMOMETER_SERVICE = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
    public static final UUID TEMPERATURE_MEASUREMENT_CHARACTERISTIC = UUID.fromString("00002a1c-0000-1000-8000-00805f9b34fb");

    public static final UUID PULSE_OXIMETER_SERVICE = UUID.fromString("00001822-0000-1000-8000-00805f9b34fb");
    public static final UUID BERRY_MED_SPO2_SERVICE = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455");
    public static final UUID BERRY_MED_SPO2_CHARACTERISTIC = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616");

    public static final UUID NONIN_SPO2_SERVICE = UUID.fromString("46a970e0-0d5f-11e2-8b5e-0002a5d5c51b");
    public static final UUID NONIN_SPO2_CHARACTERISTIC = UUID.fromString("0AAD7EA0-0D60-11E2-8E3C-0002A5D5C51B");

    public static final UUID ZENCRO_STEPS_SERVICE = UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb");
    public static final UUID ZENCRO_STEPS_CHARACTERISTIC = UUID.fromString("0000ffe9-0000-1000-8000-00805f9b34fb");

    public static final UUID ZENCRO_STEPS_MANUAL_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    public static final UUID ZENCRO_STEPS_MANUAL_CHARACTERISTIC = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    public static final UUID GLUCOSE_SERVICE = UUID.fromString("00001808-0000-1000-8000-00805f9b34fb");
    public static final UUID GLUCOSE_CHARACTERISTIC = UUID.fromString("00002a18-0000-1000-8000-00805f9b34fb");
    public static final UUID GLUCOSE_CONTEXT_CHARACTERISTIC = UUID.fromString("00002a34-0000-1000-8000-00805f9b34fb");
    public static final UUID GLUCOSE_RECORD_ACCESS_CHARACTERISTIC = UUID.fromString("00002a52-0000-1000-8000-00805f9b34fb");

    public static final UUID DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID MANUFACTURER_NAME_CHARACTERISTIC = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    public static final UUID MODEL_NUMBER_CHARACTERISTIC = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
    public static final UUID SERIAL_NUMBER_CHARACTERISTIC = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
    public static final UUID HARDWARE_REVISION_CHARACTERISTIC = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");
    public static final UUID FIRMWARE_REVISION_CHARACTERISTIC = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID SOFTWARE_REVISION_CHARACTERISTIC = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb");
    public static final UUID SYSTEM_ID_CHARACTERISTIC = UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb");


    private boolean isConnecting = false;
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
			Log.d(TAG, "onConnectionStateChange: status=" + getStatus(status) + " newState=" + getState(newState));
            isConnecting = false;
            Log.d(TAG, "Set IsConnecting to false");

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

	public Ble(Context context) throws BleException {
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

    protected abstract void onBleStopped();

    protected abstract void startCountDown();

    protected abstract void stopCountDown();

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
		    mBluetoothAdapter.startLeScan(mLeScanCallback);
            mIsScanning = true;
            startCountDown();
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
    		mIsScanning = false;
            onBleStopped();
            stopCountDown();
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
            //TODO:
            Timber.d("DISCONNECTED");
            isConnecting = false;
            mBluetoothGatt.disconnect();
        }
    }

    public void close() {
        if (mBluetoothGatt != null) {
            Timber.d("CLOSE");
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

        if(isConnecting){
            Log.e(TAG,"IS CONNECTING.");
            return false;
        }

		/*if (mBluetoothGatt != null && deviceAddress.equalsIgnoreCase(mBluetoothGatt.getDevice().getAddress())) {
		    boolean reconnect = mBluetoothGatt.connect();
		    
		    Log.d(TAG, "connectDevice: reconnect=" + reconnect);
		    
			return mBluetoothGatt.connect();
		}*/
		/*if (mBluetoothGatt != null) {
		    mBluetoothGatt.close();
		}*/

		mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);
		if (mBluetoothDevice == null) {
			Log.w(TAG, "Device not available");
			return false;
		}

        Log.d(TAG, "Connected to GATT - " + mBluetoothDevice.getName());
        try {
            boolean autoConnect = false;
            if(mBluetoothDevice.getName().contains("Berry")||
                    mBluetoothDevice.getName().contains("X6")//
                    ){
                //||
               // mBluetoothDevice.getName().contains("Nonin")
                autoConnect = true;
                if (VERSION.SDK_INT >= VERSION_CODES.M) {
                    Log.d(TAG, "Connected to GATT the special way");
                    mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, autoConnect, mBluetoothGattCallback,BluetoothDevice.TRANSPORT_LE);
                } else{
                    Log.d(TAG, "Connected to GATT the normal way");
                    mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, autoConnect, mBluetoothGattCallback);
                }
            }else if (mBluetoothDevice.getName().toLowerCase().contains("accu-chek") || "Bluetooth BP".equalsIgnoreCase(mBluetoothDevice.getName())){
                if (VERSION.SDK_INT >= VERSION_CODES.M) {
                    Log.d(TAG, "Connected to GATT the special way");
                    mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, autoConnect, mBluetoothGattCallback,BluetoothDevice.TRANSPORT_LE);
                } else{
                    Log.d(TAG, "Connected to GATT the normal way");
                    mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, autoConnect, mBluetoothGattCallback);
                }
            }
            else {
                Log.d(TAG, "Connected to GATT the normal way");
                mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, autoConnect, mBluetoothGattCallback);
            }

            isConnecting = true;
            /*if (VERSION.SDK_INT >= VERSION_CODES.KITKAT
                    && !mBluetoothDevice.getName().toLowerCase().contains("accu-chek")
                    && !mBluetoothDevice.getName().toLowerCase().contains("My Thermometer")
                    && !mBluetoothDevice.getName().toLowerCase().contains("My Oximeter")) {
                try {
                    mBluetoothDevice.setPairingConfirmation(true);
                }catch(SecurityException e){
                    e.printStackTrace();
                    mBluetoothDevice.createBond();
                }
            }*/

            mBluetoothGatt.discoverServices();
        }
        catch(IllegalArgumentException e){
            Log.e(TAG,e.getMessage());
        }
        Log.d(TAG, "Connected to GATT - callback222");
        //mBluetoothGatt.discoverServices();
        //mBluetoothGatt.setCharacteristicNotification(new BluetoothGattCharacteristic(BERRY_MED_SPO2_CHARACTERISTIC,
          //      BluetoothGattCharacteristic.PERMISSION_READ,BluetoothGattCharacteristic.PROPERTY_READ),true);
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

    protected void setNotificationForCharacteristic(BluetoothGattCharacteristic ch, boolean enabled) {
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

        Log.e(TAG, "Set characteristic descriptor value!!!");

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
