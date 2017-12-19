package sg.lifecare.medicare.ui.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import sg.lifecare.medicare.R;
import timber.log.Timber;

public class SymptomsAdapter extends BaseAdapter
{
    private Context context;
    private LayoutInflater inflater;
    private int layout;
    private String[] text;
    private TypedArray icons;
    public static int globalInc = 0;
    private boolean[] selected = new boolean[17];

    public SymptomsAdapter(Context context, String[] text, TypedArray icons, boolean[] isSelected)
    {
        this.context = context;
        this.text = text;
        this.icons = icons;
        selected = isSelected;

        globalInc = 0;
        for(int i = 0; i < isSelected.length; i++) {
            if(isSelected[i]) {
                globalInc++;
            }
        }
    }

    @Override
    public int getCount() {
        return text.length;
    }

    @Override
    public Object getItem(int i) {
        return text[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public class Holder{
        TextView textView;
        ImageView image,orangeTick;
        int position;
        boolean isSelected=false;

        Holder(int position){
            this.position = position;
            Timber.d("Created holder at position = " + position);
        }
    }

    public View getView(final int position, View convertView, final ViewGroup parent)
    {

        Holder holder;
        if(convertView == null) {
             inflater = (LayoutInflater) context
                     .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.symptom_selection_item, parent, false);
             Timber.d("convert view is null" );
             holder = new Holder(position);
            convertView.setTag(holder);
         }
         else{
             holder = (Holder) convertView.getTag();
         }

        holder.textView = (TextView) convertView.findViewById(R.id.symptom_text);
        holder.textView.setText(text[position]);

        holder.image = (ImageView) convertView.findViewById(R.id.symptom_img);
        holder.image.setImageDrawable(icons.getDrawable(position));

        holder.orangeTick = (ImageView) convertView.findViewById(R.id.image_selection);
        if(selected[position]){
            holder.orangeTick.setSelected(true);
            holder.orangeTick.setImageResource(R.drawable.select);
        }else{
            Timber.d("set selected = FALSE");
            holder.orangeTick.setSelected(false);
            holder.orangeTick.setImageResource(R.drawable.unselect);
        }
        MyOnClickListener myOnClickListener = new MyOnClickListener(position, holder);
        convertView.setOnClickListener(myOnClickListener);

        return convertView;
    }


    public class MyOnClickListener implements View.OnClickListener {
        int position;
        Holder holder;

        public MyOnClickListener(int position, Holder holder) {
            this.position = position;
            this.holder = holder;
        }

        @Override
        public void onClick(View v) {
            Timber.d("Clicked on position = " + this.position);

            if (selected[position]) {
                selected[position] = false;
                globalInc--;
            }
            else{
                if(globalInc>3){
                    Log.d("Check","Return");
                    return;
                }
                else{
                    selected[position] = true;
                    globalInc++;
                }
            }
            if(selected[position]){
                holder.orangeTick.setImageResource(R.drawable.select);
                holder.orangeTick.setSelected(true);
            }else{
                holder.orangeTick.setImageResource(R.drawable.unselect);
                holder.orangeTick.setSelected(false);
            }

        }

    }

}
