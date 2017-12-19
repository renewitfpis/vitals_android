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
import sg.lifecare.medicare.ui.SignsAndSymptomsActivity;

/**
 * Sign and symptoms adapter
 */
public class SignsAndSymptomsAdapter extends BaseAdapter{

    private Context mContext;
    private final String[] text;
    private final TypedArray icons;
    public static boolean[] isSelected = new boolean[17];
    public static int globalInc = 0;

    public SignsAndSymptomsAdapter(Context con, String[] text, TypedArray icons ) {
        mContext = con;
        this.text = text;
        this.icons = icons;
        globalInc = 0;
        for(int i = 0; i < isSelected.length;i++)
            isSelected[i] = false;
    }

    @Override
    public int getCount() {
        return text.length;
    }

    @Override
    public Object getItem(int position) {
        return text[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public class Holder{
        TextView textView;
        ImageView image,orangeTick;
        int position;
        boolean isSelected=false;

        Holder(int position){
            this.position = position;
        }
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        Holder holder;

        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if(convertView == null){
            convertView = inflater.inflate(R.layout.symptom_selection_item,parent,false);
            holder = new Holder(position);
            holder.textView = (TextView) convertView.findViewById(R.id.symptom_text);
            holder.image = (ImageView) convertView.findViewById(R.id.symptom_img);
            holder.orangeTick = (ImageView) convertView.findViewById(R.id.image_selection);
            holder.orangeTick.setSelected(false);
            holder.textView.setText(text[position]);
            holder.image.setImageDrawable(icons.getDrawable(position));

            MyOnClickListener myOnClickListener = new MyOnClickListener(position, holder);
            convertView.setOnClickListener(myOnClickListener); //mOnClickListener
            convertView.setTag(holder);
        }
        else
        {
            holder = (Holder) convertView.getTag();
        }

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


            if (holder.isSelected) {
                holder.isSelected = false;
                ((SignsAndSymptomsActivity)mContext).isSelected[holder.position] = false;
                globalInc--;
            }
            else{
                if(globalInc>3){
                    Log.d("Check","Return");
                    return;
                }
                else{
                    holder.isSelected = true;
                    ((SignsAndSymptomsActivity)mContext).isSelected[holder.position] = true;
                    globalInc++;
                }
            }


                if (holder.isSelected) {

                    holder.orangeTick.setSelected(true);
                    //v.setBackground(mContext.getResources().getDrawable(R.drawable.orange_border));
                       // holder.orangeTick.setVisibility(View.VISIBLE);
    //                    globalInc++;
                } else {

                    holder.orangeTick.setSelected(false);
                    //v.setBackground(mContext.getResources().getDrawable(R.drawable.grey_border));
                   // holder.orangeTick.setVisibility(View.INVISIBLE);

                }

                if (holder.isSelected) {
                    Log.d("GridView", "isSelected = " + holder.position);
                }
            }

        }
    }
