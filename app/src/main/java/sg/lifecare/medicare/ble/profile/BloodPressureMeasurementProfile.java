package sg.lifecare.medicare.ble.profile;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import timber.log.Timber;

/**
 * Blood pressure measurement (0x2135)
 */
public class BloodPressureMeasurementProfile extends AbstractProfile {

    private static final String TAG = "BPMProfile";
    private static final boolean DBG = true;

    public static final int UNIT_SI = 0;
    public static final int UNIT_IMPERIAL = 1;

    public static final int BODY_MOVEMENT_NOT_DETECTED = 0;
    public static final int BODY_MOVEMENT_DETECTED = 1;
    public static final int CUFF_FIT_PROPERLY = 2;
    public static final int CUFF_TOO_LOOSE = 3;
    public static final int IRREGULAR_PULSE_NOT_DETECTED = 4;
    public static final int IRREGULAR_PULSE_DETECTED = 5;
    public static final int PULSE_RATE_IS_WITHIN_THE_RANGE = 6;
    public static final int PULSE_RATE_IS_EXCEEDS_UPPER_LIMIT = 7;
    public static final int PULSE_RATE_IS_LEES_THAN_LOWER_LIMIT = 8;
    public static final int PULSE_RATE_RESERVED = 9;
    public static final int MEASUREMENT_POSITION_PROPER = 10;
    public static final int MEASUREMENT_POSITION_IMPROPER = 11;

    private final int mUnit;
    private final float mSystolic;
    private final float mDistolic;
    private final float mArterialPressure;
    private final Calendar mCalendar;
    private final float mPulseRate;
    private final int mMeasurementStatus;

    public BloodPressureMeasurementProfile(BluetoothGattCharacteristic cha) {
        Log.d(TAG,"new BPM!");
        byte[] values = cha.getValue();
        int offset = 1;

        if (values == null) {
            throw new InvalidParameterException("Invalid data");
        }

        // measurement unit
        if ((values[0] & 0x01) == 1) {
            mUnit = UNIT_IMPERIAL;
        } else {
            mUnit = UNIT_SI;
        }

        mSystolic = cha.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset).floatValue();
        offset += 2;

        mDistolic = cha.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset).floatValue();
        offset += 2;

        mArterialPressure = cha.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset).floatValue();
        offset += 2;

        // timestamp
        if (((values[0] & 0x02) >> 1) == 1) {
            int year = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset).intValue();
            int month = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 2).intValue();
            int day = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 3).intValue();
            int hour = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 4).intValue();
            int minute = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 5).intValue();
            int second = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 6).intValue();


            Log.d(TAG,"BPM getting cal from bytes = year : " + year + ", month = " + month + ", day = " + day
                    + ", hour = " + hour + ", min = " +minute + ", sec = " + second);

            mCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, month - 1);
            mCalendar.set(Calendar.DAY_OF_MONTH, day);
            mCalendar.set(Calendar.HOUR_OF_DAY, hour);
            mCalendar.set(Calendar.MINUTE, minute);
            mCalendar.set(Calendar.SECOND, second);
            offset += 7;
            Log.d("BPM","CALENDAR = " + mCalendar.getTime().toString() + " , year : " + year);
        } else {
            mCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        }

        // pulse rate
        if (((values[0] & 0x04) >> 2) == 1) {
            mPulseRate = cha.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset);
            offset += 2;
        } else {
            mPulseRate = 0f;
        }

        // measurement status flag
        if (((values[0] & 0x10) >> 4) == 1) {
            mMeasurementStatus = cha.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset).intValue();
        } else {
            mMeasurementStatus = 0;
        }

        if (DBG) {
            getSystolic();
            getDistolic();
            getArterialPressure();
            getPulseRate();
            getTimeStamp();
            bodyMovementDetection();
            cuffFitDetection();
            irregularPulseDetection();
            pulseRateRangeDetection();
        }
    }

    public String getStringUnit() {
        if (UNIT_IMPERIAL == mUnit) {
            return "kPa";
        }

        return "mmHg";
    }

    public int getUnit() {
        return mUnit;
    }

    public float getSystolic() {
        if (DBG) {
            Timber.d( "getSystolic: " + String.format("%.0f", mSystolic) + " " + getUnit());
        }
        return mSystolic;
    }

    public float getDistolic() {
        if (DBG) {
            Timber.d( "getDistolic: " + String.format("%.0f", mDistolic) + " " + getUnit());
        }
        return mDistolic;
    }

    public float getArterialPressure() {
        if (DBG) {
            Timber.d( "getArterialPressure: " + String.format("%.0f", mArterialPressure) + " " + getUnit());
        }
        return mArterialPressure;
    }

    public Calendar getTimeStamp() {
        if (DBG) {
            String ts = String.format("%d-%d-%d %d:%d:%d",
                    mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH) + 1,
                    mCalendar.get(Calendar.DAY_OF_MONTH), mCalendar.get(Calendar.HOUR_OF_DAY),
                    mCalendar.get(Calendar.MINUTE), mCalendar.get(Calendar.SECOND));
            Timber.d( "getTimeStamp: " + ts);
        }
        return mCalendar;
    }
    public Date getDate() {
        return mCalendar.getTime();
    }

    public float getPulseRate() {
        if (DBG) {
            Timber.d( "getmPulseRate: " + String.format("%.0f", mPulseRate));
        }
        return mPulseRate;
    }


    public int bodyMovementDetection() {
        if ((mMeasurementStatus & 0x01) == 1) {
            if (DBG) {
                Timber.d( "bodyMovementDetection: BODY_MOVEMENT_DETECTED");
            }
            return BODY_MOVEMENT_DETECTED;
        }

        if (DBG) {
            Timber.d( "bodyMovementDetection: BODY_MOVEMENT_NOT_DETECTED");
        }

        return BODY_MOVEMENT_NOT_DETECTED;
    }

    public int cuffFitDetection() {
        if (((mMeasurementStatus & 0x02) >> 1) == 1) {
            if (DBG) {
                Timber.d( "cuffFitDetection: CUFF_TOO_LOOSE");
            }
            return CUFF_TOO_LOOSE;
        }
        if (DBG) {
            Timber.d( "cuffFitDetection: CUFF_FIT_PROPERLY");
        }
        return CUFF_FIT_PROPERLY;
    }

    public int irregularPulseDetection() {

        if (((mMeasurementStatus & 0x04) >> 3) == 1) {
            if (DBG) {
                Timber.d( "irregularPulseDetection: IRREGULAR_PULSE_DETECTED");
            }
            return IRREGULAR_PULSE_DETECTED;
        }
        if (DBG) {
            Timber.d( "irregularPulseDetection: IRREGULAR_PULSE_NOT_DETECTED");
        }
        return IRREGULAR_PULSE_NOT_DETECTED;
    }

    public int pulseRateRangeDetection() {
        if (((mMeasurementStatus & 0x05) >> 4) == 1) {
            if (((mMeasurementStatus & 0x06) >> 5) == 1) {
                if (DBG) {
                    Timber.d( "pulseRateRangeDetection: PULSE_RATE_RESERVED");
                }
                return PULSE_RATE_RESERVED;
            } else {
                if (DBG) {
                    Timber.d( "pulseRateRangeDetection: PULSE_RATE_IS_EXCEEDS_UPPER_LIMIT");
                }
                return PULSE_RATE_IS_EXCEEDS_UPPER_LIMIT;
            }
        } else {
            if (((mMeasurementStatus & 0x06) >> 5) == 1) {
                if (DBG) {
                    Timber.d( "pulseRateRangeDetection: PULSE_RATE_IS_LEES_THAN_LOWER_LIMIT");
                }
                return PULSE_RATE_IS_LEES_THAN_LOWER_LIMIT;
            } else {
                if (DBG) {
                    Timber.d( "pulseRateRangeDetection: PULSE_RATE_IS_WITHIN_THE_RANGE");
                }
                return PULSE_RATE_IS_WITHIN_THE_RANGE;
            }
        }
    }
}
