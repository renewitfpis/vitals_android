package sg.lifecare.medicare.ui.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import sg.lifecare.medicare.R;
import timber.log.Timber;

public class MeasureAdapter extends BaseAdapter
{
    private Context context;
    private LayoutInflater inflater;
    private int layout;
    private String[] titles;
    private Drawable[] icons;

    public MeasureAdapter(Context context, int layout, String[] titles, Drawable[] icons)
    {

        //super();
        Timber.d("MeasureAdapter" );
        this.context = context;
        this.layout = layout;
        this.titles = titles;
        this.icons = icons;
    }

    @Override
    public int getCount() {
        return titles.length;
    }

    @Override
    public Object getItem(int i) {
        return titles[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public View getView(final int position, View convertView, final ViewGroup parent)
    {
        Timber.d("getview" );
        View view = null;
         if(convertView == null) {
             inflater = (LayoutInflater) context
                     .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
             view = inflater.inflate(layout, parent, false);
             Timber.d("convert view is null" );
         }
         else{
             view = convertView;
         }

        String title = titles[position];
        Drawable icon =  icons[position];

        TextView tvTitle  = (TextView) view.findViewById(R.id.text_title);
        ImageView ivIcon = (ImageView) view.findViewById(R.id.indicator_icon);

        tvTitle.setText(title);
        ivIcon.setImageDrawable(icon);

        return view;
    }

}
