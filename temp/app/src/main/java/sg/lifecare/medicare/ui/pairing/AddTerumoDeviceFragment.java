package sg.lifecare.medicare.ui.pairing;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import java.util.ArrayList;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.ui.view.PairingIndicatorView;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.NfcUtil;
import sg.lifecare.medicare.utils.TagDetector;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;

public class AddTerumoDeviceFragment extends AddDeviceFragment {

    public static AddTerumoDeviceFragment newInstance() {
        return new AddTerumoDeviceFragment();
    }

    private static final String TAG = "AddTerumoDeviceFragment";
    private static final int REQUEST_ENABLE_NFC = 765;

    private TagDetector mTagDetector;
    private ArrayList<Terumo> terumoList = new ArrayList<>();
    private PairingIndicatorView pairingIndicator;
    private AlertDialog mNfcDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sh = getActivity().getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
        String entityId = sh.getString("entity_id", "");

        mNfcDialog = NfcUtil.enableNfcDialogBuilder(getActivity(),
                mNfcSettingButtonListener, mNfcCancelButtonListener).create();

        mTagDetector = new TagDetector(getActivity(), "0", entityId);
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
                mCallback.onPairSuccess(id);
            }
        });
    }


    private DialogInterface.OnClickListener mNfcCancelButtonListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            mNfcDialog.dismiss();
            pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_FAIL_NFC_OFF);
        }
    };
    private DialogInterface.OnClickListener mNfcSettingButtonListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            requestNfc();
        }
    };

    private void requestNfc(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
            startActivityForResult(intent,REQUEST_ENABLE_NFC);
        } else{
            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            startActivityForResult(intent,REQUEST_ENABLE_NFC);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_ENABLE_NFC){
            if(resultCode==RESULT_OK) {
                Log.d(TAG, "NFC ONRESULT !");
            }
            else{
                //CANCELED
                pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_FAIL_NFC_OFF);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater,
                             ViewGroup paramViewGroup, Bundle paramBundle) {
        View view = paramLayoutInflater.inflate(R.layout.fragment_add_glucometer, paramViewGroup, false);
        pairingIndicator = (PairingIndicatorView) view.findViewById(R.id.pairing_indicator);
        pairingIndicator.setProductImage(MedicalDevice.getModelImage(myActivity.getDevice()));
        pairingIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!NfcUtil.isNfcEnabled(getActivity())){
                    mNfcDialog.show();
                }
            }
        });

        pairingIndicator.setPairingDescription(R.string.glucose_info_pair_device_step2);
        pairingIndicator.setPairingDescriptionImage(R.drawable.pairing_glucometer_tap_phone);
        pairingIndicator.showPairingDescription();

        if(NfcUtil.isNfcEnabled(getActivity())) {
            pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_NFC);
        }else{
            pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_FAIL_NFC_OFF);
        }

       /* if (myActivity.getDevice() == Model.ZENCRO_X6){
            pairingIndicator.setPairingDescription(R.string.zencro_x6_info_pair_device_step);
            pairingIndicator.setPairingDescriptionImage(R.drawable.x6_start);
        } else if (myActivity.getDevice() == Model.BERRY_BM1000B){
            pairingIndicator.setPairingDescription(R.string.berry_oximeter_info_pair_device_step);
            pairingIndicator.setPairingDescriptionImage(R.drawable.ic_pairing_berry1);
        } else if (myActivity.getDevice() == Model.NONIN_3230){
            pairingIndicator.setPairingDescription(R.string.nonin_oximeter_info_pair_device_step);
            pairingIndicator.setPairingDescriptionImage(R.drawable.ic_pairing_nonin);
        }*/
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(NfcUtil.isNfcEnabled(getActivity())) {
            mTagDetector.startDetect();
            pairingIndicator.showPairingDescription();
            pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_NFC);
        }
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
                Timber.w("Tag is not null, start read thread");
                mTagDetector.startReadThread(tag);
            }
        }
    }

}
