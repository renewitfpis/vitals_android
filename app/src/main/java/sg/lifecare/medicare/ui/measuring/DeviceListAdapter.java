package sg.lifecare.medicare.ui.measuring;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.utils.MedicalDevice;

/**
 * Created by ct on 13/1/16.
 */
public class DeviceListAdapter extends ArrayAdapter<MedicalDevice>
{
    private final String TAG = "BrandAdapter";

    private Context context;
    private int layout;
    private ArrayList<MedicalDevice> deviceList;

    public DeviceListAdapter(Context context, int layout)
    {
        super(context, layout);
        this.context = context;
        this.layout = layout;
        this.deviceList = new ArrayList<>();
    }

    @Override
    public MedicalDevice getItem(int position) {
        try {
            return deviceList.get(position);
        } catch (IndexOutOfBoundsException ex) {
            Log.w(TAG, ex.getMessage());
        }
        return null;
    }

    @Override
    public int getCount() {
        return deviceList.size();
    }

    public View getView(final int position, View convertView, ViewGroup parent)
    {
        MedicalDevice dev = deviceList.get(position);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.device_list_row, parent, false);

        ImageView deviceIcon = (ImageView) view.findViewById(R.id.device_img);
        TextView deviceName = (TextView) view.findViewById(R.id.device_name);
        TextView deviceDesc = (TextView) view.findViewById(R.id.device_desc);

        deviceName.setText(dev.getAssignedName());
       // deviceIcon.setImageResource(dev.getImage());
        //deviceDesc.setVisibility(View.INVISIBLE);
        deviceDesc.setText(dev.getModel().toString());
        if(dev.getMediaImageURL()!=null && !dev.getMediaImageURL().equals("")) {
            Picasso.with(context).load(dev.getMediaImageURL()).into(deviceIcon);
        }else{
            /*if(dev.getModel() == Model.AANDD_UA_651){
                Picasso.with(context).load(R.drawable.automatic_pair_bp).into(deviceIcon);
            }else if(dev.getModel() == Model.AANDD_UC_352){
                Picasso.with(context).load(R.drawable.weighscale).into(deviceIcon);
            }else if(dev.getModel() == Model.AANDD_UT_201){
                Picasso.with(context).load(R.drawable.thermometer).into(deviceIcon);
            }else if(dev.getModel() == Model.TERUMO_MEDISAFE_FIT){
                Picasso.with(context).load(R.drawable.glucose_icon).into(deviceIcon);
            }*/
            deviceIcon.setImageResource(dev.getImage());
            //Picasso.with(context).load(dev.getImage()).into(deviceIcon);
        }
        //deviceDesc.setText(dev.deviceName);

        //if(dev.deviceName.contains("MediSave"))
        //    deviceIcon.setImageResource(R.drawable.glucose_icon);

        return view;
    }

    public void update(ArrayList<MedicalDevice> deviceList) {
        this.deviceList = deviceList;
    }
}