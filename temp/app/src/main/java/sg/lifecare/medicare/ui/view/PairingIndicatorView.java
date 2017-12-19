package sg.lifecare.medicare.ui.view;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import sg.lifecare.medicare.R;

public class PairingIndicatorView extends RelativeLayout {

    private static final String TAG = "PairingResultView";
    public static final int PAIRING_FAIL_BLE_OFF = 0;
    public static final int PAIRING_FAIL_NFC_OFF = 1;
    public static final int PAIRING_FAIL_TIMEOUT = 2;
    public static final int PAIRING_SUCCESS = 3;
    public static final int PAIRING_BLE = 4;
    public static final int PAIRING_NFC = 5;

    public static final int READING_BLE = 6;
    public static final int READING_NFC = 7;
    public static final int READING_FAIL_BLE_OFF = 8;
    public static final int READING_FAIL_NFC_OFF = 9;
    public static final int READING_FAIL_NO_DEVICE = 10;
    public static final int READING_FAIL_ERROR = 11;
    public static final int READING_SUCCESS = 12;
    public static final int READING_FAIL_NFC_BLE_OFF = 13;

    private ImageView mPhoneImage;
    private ImageView mConnectImage;
    private ImageView mProductImage;
    private TextView mPairingTitle;
    private TextView mPairingDesc;
    private ImageView mPairingDescImage;

    private String mPairingDescText = "";
    private int mode = -1;

    public PairingIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public PairingIndicatorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.view_medical_device_pairing_indicator, this);

        mProductImage = (ImageView) view.findViewById(R.id.image_device);
        mConnectImage = (ImageView) view.findViewById(R.id.image_connection);
        mPhoneImage = (ImageView) view.findViewById(R.id.image_phone);
        mPairingTitle = (TextView) view.findViewById(R.id.text_pairing_title);
        mPairingDesc = (TextView) view.findViewById(R.id.text_pairing_desc);
        mPairingDescImage = (ImageView) view.findViewById(R.id.image_pairing_desc);
        mPairingDescText = "";
        hidePairingDescription();
        mode = -1;
    }

    public void setPairingMode(int mode){
        this.mode = mode;

        switch(mode){
            case PAIRING_FAIL_BLE_OFF:
                mPhoneImage.setImageResource(R.drawable.phone_failed);
                mConnectImage.setImageResource(R.drawable.disconnected);
                mPairingTitle.setText(R.string.pairing_bluetooth_fail);
                mPairingDesc.setText(R.string.pairing_bluetooth_not_connected);
                mPairingDesc.setGravity(Gravity.CENTER);
                mPairingDesc.setVisibility(VISIBLE);
                mPairingDescImage.setVisibility(GONE);
                break;

            case PAIRING_FAIL_NFC_OFF:
                mPhoneImage.setImageResource(R.drawable.phone_failed);
                mConnectImage.setImageResource(R.drawable.disconnected);
                mPairingTitle.setText(R.string.pairing_bluetooth_fail);
                mPairingDesc.setText(R.string.pairing_nfc_not_connected);
                mPairingDesc.setGravity(Gravity.CENTER);
                mPairingDescImage.setVisibility(GONE);
                break;

            case PAIRING_FAIL_TIMEOUT:
                mPhoneImage.setImageResource(R.drawable.phone_failed);
                mConnectImage.setImageResource(R.drawable.disconnected);
                mPairingTitle.setText(R.string.pairing_bluetooth_fail);
                mPairingDesc.setText(R.string.pairing_timeout);
                mPairingDesc.setGravity(Gravity.CENTER);
                mPairingDescImage.setVisibility(GONE);
                break;

            case PAIRING_SUCCESS:
                mPhoneImage.setImageResource(R.drawable.phone_success);
                mConnectImage.setImageResource(R.drawable.connecting4);
                mPairingTitle.setText(R.string.pairing_bluetooth_success);
                mPairingDesc.setText("");
                break;

            case PAIRING_BLE:
                mPhoneImage.setImageResource(R.drawable.phone_ble);
                mConnectImage.setImageResource(R.drawable.connecting_anim);
                ((AnimationDrawable) mConnectImage.getDrawable()).start();
                mPairingTitle.setText(R.string.pairing_bluetooth_enter_pairing);
                mPairingDesc.setText(mPairingDescText);
                mPairingDesc.setGravity(Gravity.START);
                break;

            case PAIRING_NFC:
                mPhoneImage.setImageResource(R.drawable.phone_nfc);
                mConnectImage.setImageResource(R.drawable.connecting_anim);
                ((AnimationDrawable) mConnectImage.getDrawable()).start();
                mPairingTitle.setText(R.string.pairing_nfc_enter_pairing_mode);
                mPairingDesc.setText(R.string.glucose_info_pair_device_step2);
                mPairingDesc.setGravity(Gravity.START);
                mPairingDescImage.setImageResource(R.drawable.pairing_glucometer_tap_phone);
                mPairingDescImage.setVisibility(VISIBLE);
                break;

            case READING_BLE:
                mPhoneImage.setImageResource(R.drawable.phone_ble);
                mConnectImage.setImageResource(R.drawable.connecting_anim);
                ((AnimationDrawable) mConnectImage.getDrawable()).start();
                mPairingTitle.setText(R.string.pairing_bluetooth_enter_reading);
                mPairingDesc.setText(mPairingDescText);
                mPairingDesc.setGravity(Gravity.START);
                break;

            case READING_NFC:
                mPhoneImage.setImageResource(R.drawable.phone_nfc);
                mConnectImage.setImageResource(R.drawable.connecting_anim);
                ((AnimationDrawable) mConnectImage.getDrawable()).start();
                mPairingTitle.setText(R.string.pairing_nfc_enter_reading_mode);
                mPairingDesc.setText(mPairingDescText);
                mPairingDesc.setGravity(Gravity.START);
                break;

            case READING_FAIL_BLE_OFF:
                mPhoneImage.setImageResource(R.drawable.phone_failed);
                mConnectImage.setImageResource(R.drawable.disconnected);
                mPairingTitle.setText(R.string.pairing_reading_fail);
                mPairingDesc.setText(R.string.pairing_bluetooth_not_connected);
                mPairingDesc.setGravity(Gravity.CENTER);
                mPairingDescImage.setVisibility(GONE);
                break;

            case READING_FAIL_NFC_OFF:
                mPhoneImage.setImageResource(R.drawable.phone_failed);
                mConnectImage.setImageResource(R.drawable.disconnected);
                mPairingTitle.setText(R.string.pairing_reading_fail);
                mPairingDesc.setText(R.string.pairing_nfc_not_connected);
                mPairingDesc.setGravity(Gravity.CENTER);
                mPairingDescImage.setVisibility(GONE);
                break;

            case READING_FAIL_NO_DEVICE:
                mPhoneImage.setImageResource(R.drawable.phone_failed);
                mConnectImage.setImageResource(R.drawable.disconnected);
                mPairingTitle.setText(R.string.pairing_reading_fail_no_device);
                mPairingDesc.setText("");
                mPairingDescImage.setVisibility(GONE);
                break;

            case READING_FAIL_ERROR:
                mPhoneImage.setImageResource(R.drawable.phone_failed);
                mConnectImage.setImageResource(R.drawable.disconnected);
                mPairingTitle.setText(R.string.pairing_reading_fail);
                mPairingDesc.setText(R.string.pairing_reading_fail_error);
                mPairingDesc.setGravity(Gravity.CENTER);
                mPairingDesc.setVisibility(VISIBLE);
                mPairingDescImage.setVisibility(GONE);
                break;

            case READING_SUCCESS:
                mPhoneImage.setImageResource(R.drawable.phone_success);
                mConnectImage.setImageResource(R.drawable.connecting4);
                mPairingTitle.setText(R.string.pairing_reading_success);
                mPairingDesc.setText("");
                mPairingDescImage.setVisibility(GONE);
                break;

            case READING_FAIL_NFC_BLE_OFF:
                mPhoneImage.setImageResource(R.drawable.phone_failed);
                mConnectImage.setImageResource(R.drawable.disconnected);
                mPairingTitle.setText(R.string.pairing_reading_fail);
                mPairingDesc.setText(R.string.pairing_nfc_ble_not_connected);
                mPairingDesc.setGravity(Gravity.CENTER);
                mPairingDescImage.setVisibility(GONE);
                break;
        }
    }

    public void setPairingDescription(String desc){
        mPairingDescText = desc;
        setPairingMode(mode);
    }

    public void setPairingDescription(int resId){
        mPairingDescText = getResources().getString(resId);
        setPairingMode(mode);
    }

    public void setPairingDescriptionImage(int resId){
        mPairingDescImage.setImageResource(resId);
    }

    public void showPairingDescription(){
        mPairingDesc.setVisibility(VISIBLE);
        mPairingDescImage.setVisibility(VISIBLE);
    }

    public void showPairingDescriptionText(){
        mPairingDesc.setVisibility(VISIBLE);
        mPairingDescImage.setVisibility(GONE);
    }

    public void hidePairingDescription(){
        mPairingDesc.setVisibility(GONE);
        mPairingDescImage.setVisibility(GONE);
    }

    public void setProductImage(int resId) {
        mProductImage.setImageResource(resId);
    }

    public void setResultImage(int resId) {
        //mResultImage.setImageResource(resId);
    }

}
