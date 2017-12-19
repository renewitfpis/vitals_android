package sg.lifecare.medicare.ui.pairing;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

/**
 * Created by ct on 13/1/16.
 */
public class GatewayPairingPagerAdapter extends FragmentStatePagerAdapter
{
    private static final String TAG = "SetupPagerAdapter";
    private static final int PAGES = 4;

    public static final int PAGE_SELECT_BRAND = 0;
    public static final int PAGE_SELECT_TYPE = 1;
    public static final int PAGE_ADD_DEVICE = 2;
    public static final int PAGE_PAIRING_INDICATOR = 3;

    private Fragment fragment1;
    private Fragment fragment2;
    private Fragment fragment3;
    private Fragment fragment4;

    public GatewayPairingPagerAdapter(FragmentManager paramFragmentManager)
    {
        super(paramFragmentManager);
    }

    public int getCount()
    {
        return PAGES;
    }

    public Fragment getItem(int paramInt)
    {
        Log.d("sweelai", "getItem" + paramInt);
        if (paramInt == PAGE_SELECT_BRAND)
        {
            if (this.fragment1 == null)
            {
                this.fragment1 = new SelectBrandFragment();
            }
            return this.fragment1;
        }
        else if (paramInt == PAGE_SELECT_TYPE)
        {
            if (this.fragment2 == null)
            {
                this.fragment2 = new SelectDeviceTypeFragment();
            }
            return this.fragment2;
        }
        else if (paramInt == PAGE_ADD_DEVICE)
        {
            if (this.fragment3 == null)
            {
                this.fragment3 = new AddDeviceStepFragment();
            }
            return this.fragment3;
        }
        else if (paramInt == PAGE_PAIRING_INDICATOR)
        {
            if (this.fragment4 == null)
            {
                this.fragment4 = new PairingIndicatorFragment();
            }
            return this.fragment4;
        }

        return null;
    }
}