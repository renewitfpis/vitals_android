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
import sg.lifecare.medicare.database.model.Terumo;

public class GlucoseReadingAdapter extends ArrayAdapter<Terumo> implements ListAdapter
{   private String TAG = "GlucoseReadingAdapter";
    private Context context;
    private int layout;
    LayoutInflater inflater;
    private final ArrayList<Terumo> list;
    public boolean[] selected;
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH);

    public GlucoseReadingAdapter(Context context, int layout, ArrayList<Terumo> list)
    {
        super(context, layout, list);
        this.context = context;
        this.layout = layout;
        this.list = list;
        selected = new boolean[list.size()];
        for(int i = 0; i < selected.length; i++){
            selected[i] = true;
        }
    }

    public View getView(final int position, View convertView, final ViewGroup parent) {
        if (convertView == null) {
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layout, parent, false);
        }

        final Terumo terumo = getItem(position);
        final TextView tvDate = (TextView) convertView.findViewById(R.id.date);
        final TextView tvMeasuredTime = (TextView) convertView.findViewById(R.id.measured_time);
        final TextView tvReading = (TextView) convertView.findViewById(R.id.value);
        final TextView tvUnit = (TextView) convertView.findViewById(R.id.unit);
        final RelativeLayout checkBoxLayout = (RelativeLayout) convertView.findViewById(R.id.check_box_layout);
        final ImageView checkBox = (ImageView) convertView.findViewById(R.id.check_box);
        tvDate.setText(sdf.format(terumo.getDate()));
        if(terumo.isBeforeMeal()!=null)
            tvMeasuredTime.setText(terumo.isBeforeMeal() ? "Before Meal" : "After Meal");
        else
            tvMeasuredTime.setVisibility(View.GONE);

        if(selected[position]){
            checkBox.setImageResource(R.drawable.select);
        }else{
            checkBox.setImageResource(R.drawable.unselect);
        }
        tvReading.setText(String.format(Locale.getDefault(), "%.1f", terumo.getValue()));
        tvUnit.setText(terumo.getStringUnit());
        checkBoxLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                selected[position] = !selected[position];

                if(selected[position]){
                    checkBox.setImageResource(R.drawable.select);
                }else{
                    checkBox.setImageResource(R.drawable.unselect);
                }
                //list.remove(terumo);
                //notifyDataSetChanged();
            }
        });

        return convertView;
    }
}
