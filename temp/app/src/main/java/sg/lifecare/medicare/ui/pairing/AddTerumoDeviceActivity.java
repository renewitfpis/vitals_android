package sg.lifecare.medicare.ui.pairing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.utils.TagDetector;
import timber.log.Timber;

public class AddTerumoDeviceActivity extends Activity {

    public static AddTerumoDeviceActivity newInstance() {
        return new AddTerumoDeviceActivity();
    }

    private static final String TAG = "AddTerumoDeviceActivity";

    private TagDetector mTagDetector;

    private ArrayList<Terumo> terumoList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_device_step);
        SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
        String entityId = sh.getString("entity_id", "");

        mTagDetector = new TagDetector(this, "0",entityId);
        mTagDetector.setOnTagDetectedListener(new TagDetector.TagDetectorListener() {
            @Override
            public void onDataDetectionStarted() {
                terumoList.clear();
            }

            @Override
            public void onDataDetected(final Terumo terumo) {
                terumoList.add(terumo);
                Log.d(TAG, "Detected data!! " + terumo.getValue() + ", " + terumo.isBeforeMeal());
            }

            @Override
            public void onDataDetectionCompleted() {
                Log.d(TAG, "Finish detection!");
            }

            @Override
            public void onTagDetected(String id) {
                Log.d(TAG,"On Tag Detected!");


            }
        });
    }

    @Override
    public void onNewIntent(Intent intent) {
        resolveIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        mTagDetector.startDetect();
    }

    @Override
    public void onPause() {
        super.onPause();

        mTagDetector.stopDetect();
    }

    protected void resolveIntent(Intent intent) {
        String s = intent.getAction();

        Timber.d("resolveIntent: action=" + s);

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(s) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(s) ||
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(s)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            if (tag != null && mTagDetector != null) {
                Log.w("homeActivity", "tag is not null");
                mTagDetector.startReadThread(tag);
            }
        }
    }

}
