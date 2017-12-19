package sg.lifecare.medicare.ui.pairing;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.utils.BleUtil;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import sg.lifecare.medicare.utils.NfcUtil;


/**
 * Created by ct on 13/1/16.
 */
public class SelectDeviceTypeFragment extends Fragment
{
    public static SelectDeviceTypeFragment newInstance() {
        return new SelectDeviceTypeFragment();
    }

    private final String TAG = "SelectDevice";

    private static final int REQUEST_ENABLE_BT = 123;
;
    private ListView deviceListView;
    private SelectDeviceTypeAdapter adapter;
    private Model model;

    private ConvertGatewayActivity myActivity;
    private AlertDialog mNfcEnableDialog;
    private AlertDialog mBleEnableDialog;

    private OnDeviceSelected mCallback;

    public interface OnDeviceSelected
    {
        void onDeviceSelected(Model device);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNfcEnableDialog = NfcUtil.enableNfcDialogBuilder(getActivity(),
                mNfcSettingsButtonListener, mNfcCancelButtonListener).create();
        mBleEnableDialog = BleUtil.enableBleDialogBuilder(getActivity(),
                mBleSettingsButtonListener, mBleCancelButtonListener).create();
    }

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater,
                             ViewGroup paramViewGroup, Bundle paramBundle)
    {
        View view = paramLayoutInflater.inflate(R.layout.select_device_view, paramViewGroup, false);

        view.findViewById(R.id.loading_view).setVisibility(View.GONE);
        deviceListView = (ListView) view.findViewById(R.id.device_listview);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new SelectDeviceTypeAdapter(myActivity, R.layout.brand_row, myActivity.getBrand());
        deviceListView.setAdapter(adapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                model = MedicalDevice.getModelByPosition(myActivity.getBrand(), position);
                MedicalDevice md = MedicalDevice.findModel(model);

                if (md != null) {
                    if (MedicalDevice.WCONFIG_CODE_NFC.equalsIgnoreCase(md.getMode())) {
                        if (NfcUtil.isNfcEnabled(getActivity())){
                            mCallback.onDeviceSelected(model);
                        } else {
                            mNfcEnableDialog.show();
                        }
                    } else if (MedicalDevice.WCONFIG_CODE_BLE.equalsIgnoreCase(md.getMode())) {
                        if (BleUtil.isBleEnabled()) {
                            mCallback.onDeviceSelected(model);
                        } else {
                            mBleEnableDialog.show();
                        }
                    }
                }


            }
        });
    }

    @Override
    public void onAttach(Activity paramActivity) {
        super.onAttach(paramActivity);

        try
        {
            mCallback = ((OnDeviceSelected)paramActivity);
            myActivity = (ConvertGatewayActivity)paramActivity;
        }
        catch (ClassCastException localClassCastException)
        {
            throw new ClassCastException(paramActivity.toString()
                    + " must implement OnDeviceSelected");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_ENABLE_BT){
            Log.d(TAG,"BT ONRESULT ! " + resultCode);
            if(resultCode == Activity.RESULT_OK) {
                mCallback.onDeviceSelected(model);
            }
        }
    }

    public void updateDeviceList() {
        adapter.setDeviceTypeList(myActivity.getBrand());
        adapter.notifyDataSetChanged();
    }

    private DialogInterface.OnClickListener mNfcCancelButtonListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            mNfcEnableDialog.dismiss();
        }
    };
    private DialogInterface.OnClickListener mNfcSettingsButtonListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        }
    };

    private DialogInterface.OnClickListener mBleCancelButtonListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            mBleEnableDialog.dismiss();
        }
    };
    private DialogInterface.OnClickListener mBleSettingsButtonListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }
    };
}