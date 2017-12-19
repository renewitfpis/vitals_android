package sg.lifecare.medicare.ui.pairing;

import android.app.Activity;
import android.support.v4.app.Fragment;

/**
 * Created by sweelai on 1/18/16.
 */
public class AddDeviceFragment extends Fragment {

    public interface OnPairingDetected
    {
        void onPairSuccess(String devId);
        void onPairFailed();
    }

    protected OnPairingDetected mCallback;
    protected ConvertGatewayActivity myActivity;

    @Override
    public void onAttach(Activity paramActivity) {
        super.onAttach(paramActivity);

        try {
            mCallback = ((OnPairingDetected)paramActivity);
            myActivity = (ConvertGatewayActivity)paramActivity;
        }
        catch (ClassCastException localClassCastException)
        {
            throw new ClassCastException(paramActivity.toString()
                    + " must implement OnPairingDetected");
        }
    }
}
