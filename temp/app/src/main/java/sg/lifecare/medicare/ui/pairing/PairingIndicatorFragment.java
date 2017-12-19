package sg.lifecare.medicare.ui.pairing;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.ui.view.PairingIndicatorView;
import sg.lifecare.medicare.utils.MedicalDevice;


/**
 * Created by ct on 13/1/16.
 */
public class PairingIndicatorFragment extends Fragment
{
    public static PairingIndicatorFragment newInstance() {
        return new PairingIndicatorFragment();
    }

    private EditText deviceName;
    private TextView deviceNameTitle, pairingIndicatorDesc;
    private Button finishButton, cancelButton, tryAgainButton, deviceListingButton;
    private ImageView productImage, resultImage;

    private ConvertGatewayActivity myActivity;
    private PairingIndicatorView pairingIndicator;
    private OnButtonSelected mCallback;
    public interface OnButtonSelected
    {
        void onCancelSelected();
        void onFinishSelected(String name);
        void onTryAgainSelected();
        void onDeviceListingSelected();
    }

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater,
                             ViewGroup paramViewGroup, Bundle paramBundle)
    {
        View view = paramLayoutInflater.inflate(R.layout.pairing_success_fail_view,
                paramViewGroup, false);

        pairingIndicator = (PairingIndicatorView) view.findViewById(R.id.pairing_indicator);
        pairingIndicator.setProductImage(MedicalDevice.getModelImage(myActivity.getDevice()));

        finishButton = (Button) view.findViewById(R.id.finish_button);
        finishButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finishPair();
            }
        });

        cancelButton = (Button) view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mCallback.onCancelSelected();
            }
        });

        deviceNameTitle = (TextView) view.findViewById(R.id.device_title);
        deviceName = (EditText) view.findViewById(R.id.device_name_field);
        deviceName.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {

                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager)
                            getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                    finishButton.performClick();
                    return true;
                }

                return false;
            }
        });

        pairingIndicatorDesc = (TextView) view.findViewById(R.id.pairing_indicator_desc);

        tryAgainButton = (Button) view.findViewById(R.id.try_again_button);
        tryAgainButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mCallback.onTryAgainSelected();
            }
        });

        /*deviceListingButton = (Button) view.findViewById(R.id.device_listing_button);
        deviceListingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onDeviceListingSelected();
            }
        });*/

        return view;
    }

    private void finishPair(){
        String name = deviceName.getText().toString();
        Log.d("PairingIndicator","finish btn clicked! name = " + name);
        mCallback.onFinishSelected(name);
    }
    @Override
    public void onViewCreated (View view, Bundle savedInstanceState)
    {
        String pairedDeviceId = myActivity.getPairedDeviceId();

        if(!TextUtils.isEmpty(pairedDeviceId))
        {
            String name = myActivity.getDeviceName();
            String displayName = "";
            if(name.contains("Blood Pressure")){
                displayName = "Blood Pressure Meter";
            } else if(name.contains("Weight Scale")){
                displayName = "Weighing Scale";
            } else if(name.contains("Pulse Oximeter")){
                displayName = "Pulse Oximeter";
            } else if(name.contains("Smart Bracelet")){
                displayName = "Smart Bracelet";
            } else {
                displayName = name;
            }
            deviceName.setVisibility(View.VISIBLE);
            deviceName.setText(displayName);
            deviceName.setSelection(deviceName.getText().length());
            deviceNameTitle.setVisibility(View.VISIBLE);

            finishButton.setVisibility(View.VISIBLE);
            //cancelButton.setVisibility(View.VISIBLE);

            tryAgainButton.setVisibility(View.GONE);
            pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_SUCCESS);
            pairingIndicatorDesc.setText("Pairing Success");
            //deviceListingButton.setVisibility(View.GONE);
        }
        else
        {
            deviceName.setVisibility(View.GONE);
            deviceNameTitle.setVisibility(View.GONE);

            finishButton.setVisibility(View.GONE);
           // cancelButton.setVisibility(View.GONE);

            tryAgainButton.setVisibility(View.VISIBLE);
            pairingIndicator.setPairingMode(PairingIndicatorView.PAIRING_FAIL_TIMEOUT);
            pairingIndicatorDesc.setText("No Device Found");
            //deviceListingButton.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onAttach(Activity paramActivity)
    {
        super.onAttach(paramActivity);

        try
        {
            mCallback = ((OnButtonSelected)paramActivity);
            myActivity = (ConvertGatewayActivity)paramActivity;
        }
        catch (ClassCastException localClassCastException)
        {
            throw new ClassCastException(paramActivity.toString()
                    + " must implement OnButtonSelected");
        }
    }
}
