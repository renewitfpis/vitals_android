package sg.lifecare.medicare.ui.pairing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.sony.nfc.DeviceInfo;
import com.sony.nfc.NfcDynamicTag;
import com.sony.nfc.NfcTag;
import com.sony.nfc.NfcTagDetector;
import com.sony.nfc.NfcTagListener;
import com.sony.nfc.err.NfcTagException;
import com.sony.nfc.glucosemeter.GlucoseMeterMsFr201;
import com.sony.nfc.glucosemeter.GlucoseMeterMsFr201Data;
import com.sony.nfc.glucosemeter.GlucoseMeterMsFr201Detector;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.object.MeasurementDevice;
import sg.lifecare.medicare.utils.HexUtil;
import sg.lifecare.medicare.utils.SonyNfc;

/**
 * Created by ct on 13/1/16.
 */
public class AddDeviceStepFragment extends Fragment
{
    private final String TAG = "AddDeviceStep";

    private Activity myActivity;

    private RelativeLayout loadingView;
    private ProgressBar loading;

    private MeasurementDevice device;

    private SharedPreferences sh;

    private SonyNfc mSonyNfc;
    private DeviceInfo mDeviceInfo;
    private GlucoseMeterMsFr201Data[] mGlucoseData;

    private boolean mFinishScanning = false;

    private onPairingDetected mCallback;
    public interface onPairingDetected
    {
        void onPairSuccess(String devId);
        void onPairFailed();
    }

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater,
                             ViewGroup paramViewGroup, Bundle paramBundle)
    {
        View view = paramLayoutInflater.inflate(R.layout.add_device_step, paramViewGroup, false);

        sh = myActivity.getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);

        loadingView = (RelativeLayout) view.findViewById(R.id.loading_view);
        loading = (ProgressBar) view.findViewById(R.id.loading);
//        loading.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.loading_prog_bar),
//                android.graphics.PorterDuff.Mode.SRC_IN);

        loadingView.setVisibility(View.GONE);

        device = new MeasurementDevice();

        GlucoseMeterMsFr201Detector glucose =
                new GlucoseMeterMsFr201Detector(GlucoseMeterMsFr201Detector.READ_DATA);
        NfcTagDetector[] detectors = new NfcTagDetector[]{glucose};

        try {
            mSonyNfc = new SonyNfc(getActivity(), detectors, mNfcTagListener);
        } catch (NfcTagException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }

        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mSonyNfc.startDetect();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mSonyNfc.stopDetect();
    }

    @Override
    public void onAttach(Activity paramActivity)
    {
        super.onAttach(paramActivity);
        myActivity = paramActivity;

        try
        {
            mCallback = ((onPairingDetected)paramActivity);
            return;
        }
        catch (ClassCastException localClassCastException)
        {
            throw new ClassCastException(paramActivity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    public void resolveIntent(Intent intent)
    {
        String s = intent.getAction();

        Log.d(TAG, "resolveIntent: action=" + s);

        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(s) ||
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(s))
        {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            if (tag != null && mSonyNfc != null)
            {
                mSonyNfc.startReadThread(tag);
            }
        }
    }

    private void resolveTag(NfcTag tag)
    {
        try
        {
            if (DeviceInfo.class.isAssignableFrom(tag.getClass()))
            {
                mDeviceInfo = (DeviceInfo) tag;

                Log.d(TAG, "resolveTag: id=" + mDeviceInfo.getDeviceId() +
                        "  name=" + mDeviceInfo.getDeviceName());
            }

            if (GlucoseMeterMsFr201.class.isAssignableFrom(tag.getClass()))
            {
                Log.d(TAG, "resolveTag: start reading");
                Thread.sleep(300);
                mGlucoseData = (GlucoseMeterMsFr201Data[]) ((GlucoseMeterMsFr201)tag).getGlucoseData();
                if (mGlucoseData != null) {
                    Log.d(TAG, "resolveTag: finish reading (" + mGlucoseData.length + ")");
                    for (int i = 0; i < mGlucoseData.length; i++) {
                        Log.d(TAG, "TagDetector (glucose meter): index=" + i +
                                " value=" + mGlucoseData[i].getBloodGlucose());
                    }
                }
            }
            else if (NfcDynamicTag.class.isAssignableFrom(tag.getClass()))
            {
                Log.e(TAG, "Unknown device");
                return;
            }
            else
            {
                Log.e(TAG, "Error. Unresolved");
            }

            mSonyNfc.stopDetect();
            addDeviceSucccess();
        }
        catch (InterruptedException ex)
        {
            Log.e(TAG, ex.getMessage());
            mCallback.onPairFailed();
        }
    }

    public void addDeviceSucccess()
    {
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mCallback.onPairSuccess(mDeviceInfo.getDeviceId());
                //((MedicalDeviceAdderActivity) getActivity())
                // .setActionBarTitle(R.string.action_bar_added_successful);
                //mConfirmButton.setVisibility(View.VISIBLE);
                Log.w(TAG, "add device success");
            }
        });

        //mFinishScanning = true;
    }

    private NfcTagListener mNfcTagListener = new NfcTagListener()
    {
        @Override
        public void errorOccurred(NfcTagException e)
        {
            Log.e(TAG, e.getMessage(), e);
        }

        @Override
        public void started() {
            Log.d(TAG, "NfcTagListener: started");
        }

        @Override
        public void stopped() {
            Log.d(TAG, "NfcTagListener: stopped");
        }

        @Override
        public void tagDetected(NfcTag nfcTag) {
            Log.d(TAG, "NfcTagListener: tagDetected -> " +
                    HexUtil.toHexString(nfcTag.getId()));
        }

        @Override
        public void dataRead(NfcTag nfcTag) {
            Log.d(TAG, "NfcTagListener: dataRead");
            resolveTag(nfcTag);
        }
    };

    private void addToDatabase()
    {
        /*
        String deviceId = mDeviceInfo.getDeviceId();
        String deviceName = device.deviceName;
        String vendorId = ServerUtil.TERUMO_ID;
        String modelId = ServerUtil.TERUMO_MEDISAFE_FIT;
        String addedTime = DateUtil.getCurrentDate();


        MedicalDeviceInfo info = new MedicalDeviceInfo(0, deviceId, addedTime, vendorId, modelId,
                MedicalDeviceProvider.TYPE_NFC, deviceName);

        MedicalDeviceDatabase db = MedicalDeviceDatabase.getInstance(getActivity());
        db.addMedicalDevice(info);*/
    }

    public void getMeasurementDeviceData(MeasurementDevice device)
    {
        this.device = device;
        mSonyNfc.startDetect();
    }
}