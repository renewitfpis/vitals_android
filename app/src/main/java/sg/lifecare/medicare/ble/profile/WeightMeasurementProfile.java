package sg.lifecare.medicare.ble.profile;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Weight measurement (0x2A9D)
 */
public class WeightMeasurementProfile extends AbstractProfile {

    private static final String TAG = "WeightMeasurement";
    private static final boolean DBG = true;

    public static final int UNIT_SI = 0;
    public static final int UNIT_IMPERIAL = 1;

    private final int mUnit;
    private final double mWeight;
    private final Calendar mCalendar;

    public WeightMeasurementProfile(BluetoothGattCharacteristic cha) {
        byte[] values = cha.getValue();
        int offset = 1;

        if (values == null) {
            throw new InvalidParameterException("Invalid data");
        }

        // measurement unit
        double resolution;
        if ((values[0] & 0x01) == 1) {
            mUnit = UNIT_IMPERIAL;
            resolution = 0.01d;
        } else {
            mUnit = UNIT_SI;
            resolution = 0.005d;
        }
        double rawWeight = ((double)cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset)) * resolution;

        DecimalFormat df = new DecimalFormat("#.##");
        mWeight = Double.parseDouble(df.format(rawWeight));

        offset += 2;

        // timestamp
        if (((values[0] & 0x02) >> 1) == 1) {
            int year = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset).intValue();
            int month = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 2).intValue();
            int day = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 3).intValue();
            int hour = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 4).intValue();
            int minute = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 5).intValue();
            int second = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 6).intValue();

            Log.d("WeightMeasurement","CALENDAR0 =  year : " + year + " mth : " + month + " day : " + day);
            mCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, month - 1);
            mCalendar.set(Calendar.DAY_OF_MONTH, day);
            mCalendar.set(Calendar.HOUR_OF_DAY, hour);
            mCalendar.set(Calendar.MINUTE, minute);
            mCalendar.set(Calendar.SECOND, second);

            Log.d("WeightMeasurement","CALENDAR = " + mCalendar.getTime().toString() + " , year : " + year);
            //offset += 7;
        } else {
            mCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        }

        if (DBG) {
            getTimeStamp();
            getWeight();
        }

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

    public String getStringUnit() {
        if (UNIT_IMPERIAL == mUnit) {
            return "lb";
        }

        return "kg";
    }

    public int getUnit() {
        return mUnit;
    }

    public double getWeight() {
        if (DBG) {
            Log.d(TAG, "getWeight: " + String.format("%.1f", mWeight) + " " + getUnit());
        }

        return mWeight;
    }


}
