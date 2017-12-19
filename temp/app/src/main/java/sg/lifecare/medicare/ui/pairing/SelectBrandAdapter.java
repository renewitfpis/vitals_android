package sg.lifecare.medicare.ui.pairing;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.utils.MedicalDevice;

/**
 * Created by ct on 13/1/16.
 */
public class SelectBrandAdapter extends ArrayAdapter<String>
{
    private Context context;
    private final String[] brandList;

    public SelectBrandAdapter(Context context, int layout)
    {
        super(context, layout);
        this.context = context;
        this.brandList = MedicalDevice.getBrandList(context);;
    }

    @Override
    public int getCount() {
        return brandList.length;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent)
    {
        String dev = brandList[position];

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.brand_row, parent, false);

        ImageView brandIcon = (ImageView) view.findViewById(R.id.brand_img);
        TextView brandName = (TextView) view.findViewById(R.id.brand_name);

        brandIcon.setImageResource(MedicalDevice.getBrandImage(MedicalDevice.getBrandByPosition(position)));
        brandName.setText(dev);

        return view;
    }
}
