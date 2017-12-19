package sg.lifecare.medicare.ui.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.model.LifecareAssisted;
import sg.lifecare.medicare.utils.CircleTransform;
import timber.log.Timber;

public class PatientAdapter extends ArrayAdapter<LifecareAssisted>
{
    private Context context;
    private LayoutInflater inflater;
    private int layout;
    private ArrayList<LifecareAssisted> assisteds;
    private ArrayList<LifecareAssisted> filters;
    public static int selectedPos = 0;

    public PatientAdapter(Context context, int layout, ArrayList<LifecareAssisted> list)
    {
        super(context,0,list);
        Timber.d("PatientAdapter");
        this.context = context;
        this.layout = layout;
        this.assisteds = list;
        this.filters = list;
        selectedPos = 0;
    }

    @Override
    public int getCount() {
        return filters.size();
    }

    @Override
    public LifecareAssisted getItem(int position) {
        return filters.get(position);
    }

    @Override
    public long getItemId(int position) {
        //if filtered results are the original results,
        //return the position as it is

        Timber.d("Position = " + position);
        if(filters.size() == assisteds.size())
            return position;

        //else, find the ACTUAL position by
        //looping the original data and matching it with filtered item
        long actualPos = -1;
        for(int i = 0; i < assisteds.size(); i++){
            if(assisteds.get(i) == filters.get(position)){
                actualPos = i;
                Timber.d("Return actual pos " + actualPos);
            }
        }
        return actualPos;
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

        String title = filters.get(position).getName();
        String imgUrl =  filters.get(position).getProfileImgUrl();

        TextView tvTitle  = (TextView) view.findViewById(R.id.text_name);
        ImageView ivIcon = (ImageView) view.findViewById(R.id.image_profile);

        tvTitle.setText(title);
        if(imgUrl!=null && !imgUrl.isEmpty()){
            Picasso.with(context).load(imgUrl).transform(new CircleTransform()).into(ivIcon);
        }else {
            if (filters.get(position).isFemale()) {
                ivIcon.setImageResource(R.drawable.ic_female_large);
            }else{
                ivIcon.setImageResource(R.drawable.ic_male_large);
            }
        }

        if(getItemId(position) == selectedPos){
            tvTitle.setTextColor(ContextCompat.getColor(context,R.color.colorPrimaryGreen));
        }else{
            tvTitle.setTextColor(ContextCompat.getColor(context,R.color.black_lighter));
        }

        return view;
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint,FilterResults results) {

                filters = (ArrayList<LifecareAssisted>) results.values; // has the filtered values
                notifyDataSetChanged();  // notifies the data with new filtered values
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();        // Holds the results of a filtering operation in values
                ArrayList<LifecareAssisted> FilteredArrList = new ArrayList<>();

                if (assisteds == null) {
                    assisteds = new ArrayList<>(filters); // saves the original data in mOriginalValues
                }

                /********
                 *
                 *  If constraint(CharSequence that is received) is null returns the mOriginalValues(Original) values
                 *  else does the Filtering and returns FilteredArrList(Filtered)
                 *
                 ********/
                if (constraint == null || constraint.length() == 0) {

                    // set the Original result to return
                    results.count = assisteds.size();
                    results.values = assisteds;
                } else {
                    constraint = constraint.toString().toLowerCase();
                    for (int i = 0; i < assisteds.size(); i++) {
                        String data = assisteds.get(i).getName();
                        if (data.toLowerCase().contains(constraint.toString())) {
                            FilteredArrList.add(assisteds.get(i));
                        }
                    }
                    // set the Filtered result to return
                    results.count = FilteredArrList.size();
                    results.values = FilteredArrList;
                }
                return results;
            }
        };
        return filter;
    }
}
