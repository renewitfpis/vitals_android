package sg.lifecare.medicare.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.model.BloodPressure;
import timber.log.Timber;

public class BloodPressureReadingAdapter extends ArrayAdapter<BloodPressure> implements ListAdapter
{   private String TAG = "BloodPressureReadingAdapter";
    private Context context;
    private int layout;
    LayoutInflater inflater;
    private ArrayList<BloodPressure> list;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    public boolean[] selected;

    public BloodPressureReadingAdapter(Context context, int layout, ArrayList<BloodPressure> list)
    {
        super(context, layout, list);
        this.context = context;
        this.layout = layout;
        this.list = list;
        selected = new boolean[list.size()];
        for(int i = 0; i<selected.length;i++){
            selected[i] = true;
        }
    }

    public View getView(final int position, View convertView, final ViewGroup parent) {
        if (convertView == null) {
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layout, parent, false);
        }

        final BloodPressure bp = getItem(position);
        final TextView tvDate = (TextView) convertView.findViewById(R.id.date);
        final TextView tvMeasuredTime = (TextView) convertView.findViewById(R.id.measured_time);
        final TextView tvBpReading = (TextView) convertView.findViewById(R.id.bp_value);
        final TextView tvBpUnit = (TextView) convertView.findViewById(R.id.bp_unit);
        final TextView tvPulseReading = (TextView) convertView.findViewById(R.id.pulse_rate_value);
        final TextView tvPulseUnit = (TextView) convertView.findViewById(R.id.pulse_rate_unit);
        final RelativeLayout checkBoxLayout = (RelativeLayout) convertView.findViewById(R.id.check_box_layout);
        final ImageView checkBox = (ImageView) convertView.findViewById(R.id.check_box);

        tvDate.setText(dateFormat.format(bp.getDate()));
        tvMeasuredTime.setText(timeFormat.format(bp.getDate()));
        tvBpReading.setText((int)bp.getSystolic()+"/"+ (int)bp.getDistolic());
        tvBpUnit.setText(bp.getStringUnit());
        tvPulseReading.setText((int)bp.getPulseRate()+"");
        if(selected[position]){
            checkBox.setImageResource(R.drawable.select);
        }else{
            checkBox.setImageResource(R.drawable.unselect);
        }
        checkBoxLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Timber.d("check box on click! ");
                selected[position] = !selected[position];

                Timber.d("new state =  " + selected[position]);
                if(selected[position]){
                    checkBox.setImageResource(R.drawable.select);
                }else{
                    checkBox.setImageResource(R.drawable.unselect);
                }
                //list.remove(bp);
                //notifyDataSetChanged();
            }
        });

        return convertView;
    }
}
