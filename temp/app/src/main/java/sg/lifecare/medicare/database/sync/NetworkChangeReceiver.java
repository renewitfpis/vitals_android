package sg.lifecare.medicare.database.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import timber.log.Timber;

/**
 * Created by wanping on 11/8/16.
 */
public class NetworkChangeReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        Timber.d("Network connectivity change!");
        if (intent.getExtras() != null) {
            NetworkInfo ni = (NetworkInfo) intent.getExtras().get(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (ni != null && ni.getState() == NetworkInfo.State.CONNECTED) {
                SyncHandler.uploadDataToServer(context);
                Log.i("app", "Network " + ni.getTypeName() + " connected");
            } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                Log.d("app", "There's no network connectivity");
            }
        }
    }

    public static boolean isInternetAvailable(Context context){
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
    }

}
