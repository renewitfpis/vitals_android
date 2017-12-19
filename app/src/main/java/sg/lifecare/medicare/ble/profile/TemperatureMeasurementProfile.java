package sg.lifecare.medicare.ble.profile;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Weight measurement (0x2A9D)
 */
public class TemperatureMeasurementProfile extends AbstractProfile {

    private static final String TAG = "TempMeasurement";
    private static final boolean DBG = true;

    public static final int UNIT_CELSIUS = 0;
    public static final int UNIT_FAHRENHEIT = 1;

    private final int mUnit;
    private double mTemp = 0d;
    private final Calendar mCalendar;

    public TemperatureMeasurementProfile(BluetoothGattCharacteristic cha) {
        byte[] values = cha.getValue();
        int offset = 1;

        if (values == null) {
            throw new InvalidParameterException("Invalid data");
        }

        double temperature;  //= ((double)cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset));

        // measurement unit
        if ((values[0] & 0x01) == 1) {
            mUnit = UNIT_FAHRENHEIT;
            mTemp = (cha.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, offset)).doubleValue();
            offset += 4;
        } else {
            mUnit = UNIT_CELSIUS;
            mTemp = (cha.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, offset)).doubleValue();
            offset += 4;
        }

        //round to 2 decimal places
        mTemp = mTemp*100d;
        mTemp = Math.round(mTemp);
        mTemp = mTemp/100d;

        // timestamp
        if (((values[0] & 0x02) >> 1) == 1) {
            int year = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset).intValue();
            int month = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 2).intValue();
            int day = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 3).intValue();
            int hour = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 4).intValue();
            int minute = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 5).intValue();
            int second = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 6).intValue();

            Log.d(TAG,"CALENDAR0 =  year : " + year + " mth : " + month + " day : " + day);
            mCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            mCalendar.clear(Calendar.MILLISECOND);
            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, month - 1);
            mCalendar.set(Calendar.DAY_OF_MONTH, day);
            mCalendar.set(Calendar.HOUR_OF_DAY, hour);
            mCalendar.set(Calendar.MINUTE, minute);
            mCalendar.set(Calendar.SECOND, second);

            Log.d(TAG,"CALENDAR = " + mCalendar.getTime().toString() + " , year : " + year);
            //offset += 7;
        } else {
            mCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        }

        if (DBG) {
            getTimeStamp();
            getTemperature();
        }

        Log.d(TAG,"Temp: " + mTemp);
        Log.d(TAG,"Unit: " + mUnit);
        Log.d(TAG,"Date: " + getDate());


    }

    public Calendar getTimeStamp() {
        if (DBG) {
            String ts = String.format("%d-%d-%d %d:%d:%d",
                    mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH) + 1,
                    mCalendar.get(Calendar.DAY_OF_MONTH), mCalendar.get(Calendar.HOUR_OF_DAY),
                    mCalendar.get(Calendar.MINUTE), mCalendar.get(Calendar.SECOND));
            Log.d(TAG, "getTimeStamp: " + ts);
        }
        return mCalendar;
    }

    public Date getDate(){
        return mCalendar.getTime();
    }

    /*public String getStringUnit() {
        if (UNIT_CELSIUS == mUnit) {
            return "lb";
        }

        return "kg";
    }*/

    public int getUnit() {
        return mUnit;
    }

    public double getTemperature() {
        if (DBG) {
            Log.d(TAG, "getTemperature: " + String.format("%.1f", mTemp) + " " + getUnit());
        }

        return mTemp;
    }


}
