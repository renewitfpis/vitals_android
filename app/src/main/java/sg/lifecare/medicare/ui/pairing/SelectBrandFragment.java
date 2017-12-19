package sg.lifecare.medicare.ui.pairing;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Brand;


/**
 * Created by ct on 13/1/16.
 */
public class SelectBrandFragment extends Fragment
{
    public static SelectBrandFragment newInstance() {
        SelectBrandFragment fragment = new SelectBrandFragment();
        return fragment;
    }

    private final String TAG = "SelectBrand";

    private ListView brandListView;
    private SelectBrandAdapter adapter;

    private Activity myActivity;

    private OnBrandSelected mCallback;
    public interface OnBrandSelected
    {
        void onBrandSelected(Brand brand);
    }

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater,
                             ViewGroup paramViewGroup, Bundle paramBundle)
    {
        View view = paramLayoutInflater.inflate(R.layout.select_brand_view, paramViewGroup, false);

        RelativeLayout loadingView = (RelativeLayout) view.findViewById(R.id.loading_view);
        loadingView.setVisibility(View.GONE);

        brandListView = (ListView) view.findViewById(R.id.brand_listview);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new SelectBrandAdapter(myActivity, R.layout.brand_row);
        brandListView.setAdapter(adapter);
        brandListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mCallback.onBrandSelected(MedicalDevice.getBrandByPosition(position));
            }
        });

    }

    @Override
    public void onAttach(Activity paramActivity)
    {
        super.onAttach(paramActivity);
        myActivity = paramActivity;

        try
        {
            mCallback = ((OnBrandSelected)paramActivity);
        }
        catch (ClassCastException localClassCastException)
        {
            throw new ClassCastException(paramActivity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }
}