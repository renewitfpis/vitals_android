package sg.lifecare.medicare.ui.pairing;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import sg.lifecare.medicare.R;

/**
 * Created by wanping on 13/10/16.
 */

public class DevicePairingMenuFragment extends Fragment {
    ViewGroup rootView;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                R.layout.device_pairing_menu_view, container, false);

        return rootView;
    }

    public View getView(){
        return rootView;
    }


}
