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
import sg.lifecare.medicare.database.model.Weight;

public class WeightReadingAdapter extends ArrayAdapter<Weight> implements ListAdapter
{   private String TAG = "BloodPressureReadingAdapter";
    private Context context;
    private int layout;
    LayoutInflater inflater;
    private ArrayList<Weight> list;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    public boolean[] selected;

    public WeightReadingAdapter(Context context, int layout, ArrayList<Weight> list)
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

        final Weight weight = getItem(position);
        final TextView tvDate = (TextView) convertView.findViewById(R.id.date);
        final TextView tvMeasuredTime = (TextView) convertView.findViewById(R.id.measured_time);
        final TextView tvReading = (TextView) convertView.findViewById(R.id.value);
        final TextView tvUnit = (TextView) convertView.findViewById(R.id.unit);
        final RelativeLayout checkBoxLayout = (RelativeLayout) convertView.findViewById(R.id.check_box_layout);
        final ImageView checkBox = (ImageView) convertView.findViewById(R.id.check_box);

        tvDate.setText(dateFormat.format(weight.getDate()));
        tvMeasuredTime.setText(timeFormat.format(weight.getDate()));
        tvReading.setText(String.format(Locale.getDefault(), "%.2f", weight.getWeight()));
        tvUnit.setText(weight.getStringUnit());
        if(selected[position]){
            checkBox.setImageResource(R.drawable.select);
        }else{
            checkBox.setImageResource(R.drawable.unselect);
        }
        checkBoxLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                selected[position] = !selected[position];

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
