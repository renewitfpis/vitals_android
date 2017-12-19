package sg.lifecare.medicare.utils;

import android.app.Activity;
import android.nfc.Tag;
import android.util.Log;

import com.sony.nfc.AndroidNfcTagManager;
import com.sony.nfc.NfcTagDetector;
import com.sony.nfc.NfcTagListener;
import com.sony.nfc.NfcTagManager;
import com.sony.nfc.err.NfcTagException;
import com.sony.nfc.err.NfcTagIllegalModeException;

/**
 * Created by sweelai on 1/12/16.
 */
public class SonyNfc {

    private static final String TAG = "SonyNfc";

    private boolean mIsDetecting;
    private NfcTagListener mListener;
    private NfcTagManager mNfcTagManager;
    private NfcTagDetector[] mNfcTagDetectors;

    public SonyNfc(Activity activity, NfcTagDetector[] detectors, NfcTagListener listener) throws NfcTagException {
        mIsDetecting = false;
        mNfcTagManager = new AndroidNfcTagManager(activity);
        mListener = listener;
        mNfcTagDetectors = detectors;
    }

    public void startDetect() {
        Log.d(TAG, "startDetect");
        if (mIsDetecting) {
            Log.d(TAG, "Start dectect failed. Detection started");
        } else {
            if (mNfcTagManager != null) {
                try {
                    mNfcTagManager.startDetect(mNfcTagDetectors, mListener);
                    mIsDetecting = true;
                    Log.d(TAG, "Start detect success");
                } catch (NfcTagIllegalModeException ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                } catch (NfcTagException ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                }
            }
        }
    }

    public void stopDetect() {
        if (!mIsDetecting) {
            Log.d(TAG, "Not started yet");
        } else if (mNfcTagManager != null) {
            try {
                mNfcTagManager.stopDetect();
                mIsDetecting = false;
            } catch (NfcTagException nfcTagException) {
                Log.e(TAG, nfcTagException.getMessage(), nfcTagException);
            }
        }
    }

    public void startReadThread(Tag tag) {
        if (mNfcTagManager != null) {
            new ReadThread(tag).start();
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
                    ((AndroidNfcTagManager)mNfcTagManager).readTag(this.mTag, mNfcTagDetectors,
                            mListener);
                }
            } catch (NfcTagException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }
}
