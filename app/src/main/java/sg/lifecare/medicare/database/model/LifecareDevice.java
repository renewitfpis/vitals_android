package sg.lifecare.medicare.database.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import timber.log.Timber;

/**
 * Lifecare Device
 */
public class LifecareDevice {

    private String mId;
    private String mName;
    private String mType;
    private Calendar mLastUpdate;
    private String mStatus;
    private LifecareMedia mMedia;
    private boolean mIsGateway;
    private String mValue;

    public LifecareDevice(String id) {
        mId = id;
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public String getType() {
        return mType;
    }

    public Calendar getLastUpdate() {
        return mLastUpdate;
    }

    public String getStatus() {
        return mStatus;
    }

    public LifecareMedia getMedia() {
        return mMedia;
    }

    public String getValue() {
        return mValue;
    }

    public boolean isGateway() {
        return mIsGateway;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setType(String type) {
        mType = type;
    }

    public void setLastUpdate(String lastUpdate) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        Calendar cal = Calendar.getInstance();

        try {
            cal.setTime(df.parse(lastUpdate));
            mLastUpdate = cal;
        } catch(ParseException e) {
            Timber.e( e.getMessage(), e);
        }
    }

    public void setStatus(String status) {
        mStatus = status;
    }

    public void setMedia(LifecareMedia media) {
        mMedia = media;
    }

    public void setGateway(boolean isGateway) {
        mIsGateway = isGateway;
    }

    public void setValue(String value) {
        mValue = value;
    }

}
