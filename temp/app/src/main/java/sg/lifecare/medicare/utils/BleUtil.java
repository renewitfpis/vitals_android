package sg.lifecare.medicare.utils;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import sg.lifecare.medicare.R;

/**
 * Utility for BLE
 */
public class BleUtil {

    private static final String TAG = "BleUtil";

    public static boolean isBleEnabled() {

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Log.w(TAG, "Fail to get BluetoothAdapter");
            return false;
        }

        return adapter.isEnabled();
    }

    public static AlertDialog.Builder enableBleDialogBuilder(Context context,
                                                             DialogInterface.OnClickListener positive,
                                                             DialogInterface.OnClickListener negative) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.dialog);

        builder.setTitle(R.string.dialog_ble_permission_request_title);
        builder.setMessage(R.string.dialog_ble_permission_request_message);
        builder.setPositiveButton(R.string.button_settings, positive);
        builder.setNegativeButton(R.string.button_cancel, negative);

        return builder;

    }
}
