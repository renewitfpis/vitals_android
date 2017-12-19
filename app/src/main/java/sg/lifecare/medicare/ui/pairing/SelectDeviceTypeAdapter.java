package sg.lifecare.medicare.ui.pairing;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.object.MeasurementDevice;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Brand;
import sg.lifecare.medicare.utils.MedicalDevice.Model;


/**
 * Created by ct on 13/1/16.
 */
public class SelectDeviceTypeAdapter extends ArrayAdapter<MeasurementDevice>
{
    private final String TAG = "BrandAdapter";

    private Context context;
    private Brand brand;
    private String[] deviceList;

    public SelectDeviceTypeAdapter(Context context, int layout, Brand brand)
    {
        super(context, layout);
        this.context = context;
        this.brand = brand;
        this.deviceList = MedicalDevice.getModelList(context, brand);
    }

    @Override
    public int getCount() {
        return deviceList.length;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent)
    {
        String dev = deviceList[position];

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.brand_device_row, parent, false);

        ImageView deviceIcon = (ImageView) view.findViewById(R.id.brand_device_img);
        TextView deviceName = (TextView) view.findViewById(R.id.brand_device_name);

        deviceName.setText(dev);
        Model model = MedicalDevice.getModelByPosition(brand, position);
        deviceIcon.setImageResource(MedicalDevice.getModelImage(model));

        return view;
    }

    public void setDeviceTypeList(Brand brand) {
        this.brand = brand;
        this.deviceList = MedicalDevice.getModelList(context, brand);
    }
}