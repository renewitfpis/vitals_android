package sg.lifecare.medicare.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.nfc.NfcAdapter;

import sg.lifecare.medicare.R;

/**
 * Utility for NFC
 */
public class NfcUtil {

    public static boolean isNfcEnabled(Context context) {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(context);

        if (adapter == null) {
            return false;
        }

        return adapter.isEnabled();
    }

    public static AlertDialog.Builder enableNfcDialogBuilder(Context context,
                                                             DialogInterface.OnClickListener positive,
                                                             DialogInterface.OnClickListener negative) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.dialog);

        builder.setTitle(R.string.dialog_nfc_permission_request_title);
        builder.setMessage(R.string.dialog_nfc_permission_request_message);
        builder.setPositiveButton(R.string.button_settings, positive);
        builder.setNegativeButton(R.string.button_cancel, negative);
        
        return builder;

    }
}
