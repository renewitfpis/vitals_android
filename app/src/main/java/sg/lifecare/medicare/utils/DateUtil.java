package sg.lifecare.medicare.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import timber.log.Timber;

/**
 * Created by sweelai on 1/12/16.
 */
public class DateUtil {

    private static final String TAG = "DateUtil";

    public static final String ISO8601_TIMESTAMP = "yyyy-MM-dd'T'HH:mm:ss";

    public static final String getCurrentDate() {
        DateFormat format = new SimpleDateFormat(ISO8601_TIMESTAMP);

        try {
            return format.format(new Date());
        } catch (Exception e) {
            Timber.e( e.getMessage());
        }
        return "";
    }

    public static String convertCalendarToString(Calendar calendar, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);

        return sdf.format(calendar.getTime());
    }

    public static final String UTC_ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String UTC_ISO8601_MS_FORMAT = UTC_ISO8601_FORMAT + ".SSS";

    public static final Calendar getCalendarFromUtcTime(String time) throws ParseException{
        return getCalendarFromUtcTime(time, UTC_ISO8601_MS_FORMAT);
    }

    public static final Calendar getCalendarFromUtcTime(String time, String format) throws ParseException {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        DateFormat df = new SimpleDateFormat(format);
        df.setTimeZone(TimeZone.getDefault());
        Date date = df.parse(time);
        calendar.setTime(date);

        return calendar;
    }
}
