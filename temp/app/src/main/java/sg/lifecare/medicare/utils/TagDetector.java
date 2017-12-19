package sg.lifecare.medicare.utils;

import android.app.Activity;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Log;

import com.sony.nfc.AndroidNfcTagManager;
import com.sony.nfc.DeviceInfo;
import com.sony.nfc.NfcDynamicTag;
import com.sony.nfc.NfcDynamicTagDetector;
import com.sony.nfc.NfcTag;
import com.sony.nfc.NfcTagDetector;
import com.sony.nfc.NfcTagDetectorUtil;
import com.sony.nfc.NfcTagListener;
import com.sony.nfc.NfcTagManager;
import com.sony.nfc.bpmonitor.BpMonitorEsH700DDetector;
import com.sony.nfc.err.NfcTagException;
import com.sony.nfc.err.NfcTagIllegalModeException;
import com.sony.nfc.glucosemeter.GlucoseMeterMsFr201;
import com.sony.nfc.glucosemeter.GlucoseMeterMsFr201Data;
import com.sony.nfc.glucosemeter.GlucoseMeterMsFr201Detector;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import sg.lifecare.medicare.database.model.Terumo;
import timber.log.Timber;

/**
 * Created by sweelai on 12/15/15.
 */
public class TagDetector
{
    private static final String TAG = "TagDetector";

    private Activity mActivity;
    private boolean mIsDetecting;

    private String mDeviceId;
    private String mDeviceName;
    private String devId;
    private String nfcId;
    private String entityId;

    private NfcTagManager mNfcTagManager;
    private NfcTagDetector[] mNfcTagDetectors;

    public TagDetectorListener tagListener;
    public interface TagDetectorListener
    {
        void onDataDetectionStarted();

        void onDataDetected(final Terumo terumo);

        void onDataDetectionCompleted();

        void onTagDetected(String nfcId);
    }

    private final NfcTagListener mListener = new NfcTagListener() {
        @Override
        public void errorOccurred(NfcTagException e) {
            Timber.e(e.getMessage(), e);
        }

        @Override
        public void started() {
            Timber.d("started");
        }

        @Override
        public void stopped() {
            Timber.d("stopped");
        }

        @Override
        public void tagDetected(NfcTag nfcTag) {
            Timber.d("TagDetector: tagDetected -> " +
                    HexConverter.getHexStringFromByteArray(nfcTag.getId()));
            nfcId =  HexConverter.getHexStringFromByteArray(nfcTag.getId());
            tagListener.onTagDetected(nfcId);
        }

        @Override
        public void dataRead(NfcTag nfcTag)
        {
            Timber.d("TagDetector: dataRead");
            TagDetector.this.resolveTag(nfcTag);
        }
    };

    public TagDetector(Activity activity, String deviceId, String entityId)
    {
        mActivity = activity;
        devId = deviceId;
        this.entityId = entityId;
        mIsDetecting = false;
        createTagDetector();
        createTagManager();
        Timber.tag(TAG);
    }

    public void setOnTagDetectedListener(TagDetectorListener listener)
    {
        this.tagListener = listener;
    }

    private void createTagDetector() {
        BpMonitorEsH700DDetector bp =
                new BpMonitorEsH700DDetector(BpMonitorEsH700DDetector.READ_DATA);
        GlucoseMeterMsFr201Detector glucose =
                new GlucoseMeterMsFr201Detector(GlucoseMeterMsFr201Detector.READ_DATA);
        NfcTagDetector regular[] = NfcTagDetectorUtil.getRegularDetectors();
        NfcTagDetector dynamic[] = {new NfcDynamicTagDetector()};
        NfcTagDetector[][] list = new NfcTagDetector[][] {
                new NfcTagDetector[] {bp, glucose}, regular, dynamic
        };

        mNfcTagDetectors = NfcTagDetectorUtil.append(list);
    }

    private void createTagManager() {
        try {
            mNfcTagManager = new AndroidNfcTagManager(mActivity);
        } catch (NfcTagException ex) {
            Timber.e(ex.getMessage(), ex);
        }
    }

    private void resolveTag(NfcTag tag)
    {
        try
        {
            if (DeviceInfo.class.isAssignableFrom(tag.getClass()))
            {
                DeviceInfo deviceInfo = (DeviceInfo) tag;
                mDeviceId = deviceInfo.getDeviceId();
                mDeviceName = deviceInfo.getDeviceName();
                tagListener.onTagDetected(mDeviceId);
                Timber.d("TagDetector: mDeviceId=" + mDeviceId + "  mDeviceName=" + mDeviceName);
            }

            if (GlucoseMeterMsFr201.class.isAssignableFrom(tag.getClass()))
            {
                Timber.d("TagDetection (glucose meter): start reading");
                Thread.sleep(300);
                GlucoseMeterMsFr201Data[] data
                        = (GlucoseMeterMsFr201Data[]) ((GlucoseMeterMsFr201)tag).getGlucoseData();

                if(data != null)
                {
                    Timber.d("TagDetector (glucose meter): finish reading (" + data.length + ")");

                    this.tagListener.onDataDetectionStarted();

                    for (int i = 0; i < data.length; i++)
                    {
                        double value = data[i].getBloodGlucose()/1000d/18d;
                        //trick to round up to 2 decimal places
                        value = value*100d;
                        value = Math.round(value);
                        value = value/100d;

                        Terumo terumo = new Terumo();
                        terumo.setEntityId(entityId);
                        terumo.setDate(data[i].getDate().getTime());
                        terumo.setValue(value);
                        terumo.setUnit(0); //fixed to be mmol/L
                        terumo.setBeforeMeal(true);

                        if(data[i].getFlags()==2)
                            terumo.setBeforeMeal(true);
                        else if(data[i].getFlags()==4)
                            terumo.setBeforeMeal(false);

                        Timber.d("TagDetector (glucose meter): index=" + i +
                                " value=" + value);
                        Timber.d("date=" + data[i].getDate().toString());
                        Timber.d("isBeforeMeal=" + data[i].getFlags());
                        Timber.d("unit=" + data[i].getUnit());
                        Timber.d("entityId=" + entityId);

                        this.tagListener.onDataDetected(terumo);
                    }

                    this.tagListener.onDataDetectionCompleted();
                }
            }
            else if (NfcDynamicTag.class.isAssignableFrom(tag.getClass()))
            {
                Timber.e("Unknown device");
                return;
            } else {
                Timber.e("Error. Unresolved");
            }
        }
        catch (InterruptedException ex)
        {
            Timber.e(ex.getMessage());
        }
    }

    public void startDetect()
    {
        Timber.d("startDetect");
        if (mIsDetecting)
        {
            Timber.d("Start dectect failed. Detection started");
        }
        else
        {
            if (mNfcTagManager != null)
            {
                try
                {
                    mNfcTagManager.startDetect(mNfcTagDetectors, mListener);
                    mIsDetecting = true;

                    Timber.d("Start detect success");
                }
                catch (NfcTagIllegalModeException illegalModeException)
                {
                    Timber.e(illegalModeException.getMessage(), illegalModeException);
                }
                catch (NfcTagException ex)
                {
                    Timber.e(ex.getMessage(), ex);
                }
            }
        }
    }

    public void stopDetect() {
        if (!mIsDetecting) {
            Timber.d("Not started yet");
        } else if (mNfcTagManager != null) {
            try {
                mNfcTagManager.stopDetect();
                mIsDetecting = false;
            } catch (NfcTagException nfcTagException) {
                Timber.e(nfcTagException.getMessage(), nfcTagException);
            }
        }
    }

    public void startReadThread(Tag tag)
    {
        if (mNfcTagManager != null)
        {
            new ReadThread(tag).start();

            //this.tagListener.onDataDetected(9200, Calendar.getInstance());

            /*
            try
            {
                synchronized (mNfcTagManager)
                {
                    ((AndroidNfcTagManager)mNfcTagManager).readTag(tag, mNfcTagDetectors, mListener);
                    Timber.d("started OK");
                }
            }
            catch (NfcTagException e)
            {
                Timber.e(e.getMessage(), e);
            }*/
        }
    }

    private class ReadThread extends Thread {
        private Tag mTag;

        public ReadThread(Tag tag) {
            this.mTag = tag;
        }

        public void run() {
            try {
                synchronized (mNfcTagManager) {
                    ((AndroidNfcTagManager)mNfcTagManager).readTag(this.mTag, mNfcTagDetectors, mListener);
                }
            } catch (NfcTagException e) {
                Timber.e(e.getMessage(), e);
            }
        }
    }

    private class updateGlucometerReading extends AsyncTask<Void, Void, Void>
    {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm");

        private float value;
        private Calendar cal;

        public updateGlucometerReading(float value, Calendar cal)
        {
            this.value = value;
            this.cal = cal;
        }

        @Override
        public Void doInBackground(Void... params)
        {
            /*
            {
            "time":"04 Jan 2016 11:52:16 AM",
            "nodeId":"43276e4c99b4",
            "nodeName":"Weighing Scale",
            "extraData":"Weight:11.2&Height:&Bmi:&Type:kg&LastReading:true",
            "type":"kg",
            "homeId":"none",
            "zoneCode":"OT",
            "zone":"All zones",
            "eventTypeId":20014,
            "eventTypeName":"Scale Update Data",
            "deviceId":"z0a783010772"}
             */

            float val = value / 1000;

            JSONObject data = new JSONObject();
            String jsonString = "";
            String result = "";
            try
            {
                String extraData = "Concentration:" + val
                        + "&SampleLocation:Finger"
                        + "&Type:Capillary Whole Blood"
                        + "&StatusAnnunciation:0"
                        + "&LastReading:true";

                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss a");

                data.put("deviceId", devId);
                data.put("homeId", "none");
                data.put("extraData", extraData);
                data.put("eventTypeName", "Gluco Update Data");
                data.put("eventTypeId", "20015");
                data.put("nodeName", "Blood Glucose Meter");
                data.put("nodeId", mDeviceId);
                data.put("zone", "All Zones");
                data.put("time", sdf.format(cal.getTime()));
                data.put("zoneCode", "OT");
                data.put("type", "mg/dL");
                data.put("rfid", "none");

                Log.w("TagDetector", data.toString());
                jsonString = data.toString();
            }
            catch(JSONException e)
            {
                e.printStackTrace();
            }

            //TODO
            //result = LifeCareHandler.getInstance().updateGlucometerReading(data);

            return null;
        }

        @Override
        public void onPostExecute(Void result)
        {

        }
    }
}