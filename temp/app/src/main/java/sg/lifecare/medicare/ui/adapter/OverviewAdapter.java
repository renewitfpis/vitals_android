package sg.lifecare.medicare.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmObject;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.SpO2;
import sg.lifecare.medicare.database.model.SpO2Set;
import sg.lifecare.medicare.database.model.Step;
import sg.lifecare.medicare.database.model.Temperature;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.ui.BloodPressureReadingActivity;
import sg.lifecare.medicare.ui.DashboardActivity;
import sg.lifecare.medicare.ui.GeneralGlucoseReadingActivity;
import sg.lifecare.medicare.ui.SpO2ReadingActivity;
import sg.lifecare.medicare.ui.TemperatureReadingActivity;
import sg.lifecare.medicare.ui.WeightReadingActivity;
import timber.log.Timber;

public class OverviewAdapter extends ArrayAdapter<RealmObject> implements ListAdapter
{
    private Context context;
    private LayoutInflater inflater;
    private int layout;
    private final ArrayList<RealmObject> list;
    private Activity activity;

    public OverviewAdapter(Context context, int layout, ArrayList<RealmObject> list, Activity activity)
    {
        super(context, layout, list);
        this.context = context;
        this.layout = layout;
        this.list = list;
        this.activity = activity;
    }

    @Override
    public RealmObject getItem(int position){
       /* if(isTerumoUser) {
            if (list.get(position) instanceof Terumo)
                return list.get(position);
            else
                return null;
        }*/
        return list.get(position);
    }

    @Override
    public int getCount() {
        /*if(isTerumoUser) {
            return 1;
        }*/
        return list.size();
    }

    public View getView(final int position, View convertView, final ViewGroup parent)
    {
        RealmObject ro = getItem(position);
        View view = null;
         if(convertView == null) {
             inflater = (LayoutInflater) context
                     .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
             view = inflater.inflate(layout, parent, false);
             /*if(ro instanceof SpO2Set){
                 view = inflater.inflate(R.layout.overview_item_spo2,parent,false);
             }else {
                 view = inflater.inflate(layout, parent, false);
             }*/
             Timber.d("convert view is null" );
         }
         else{
             view = convertView;
         }

        if(list.isEmpty() || list.size()==0)
            return view;

        TextView tvDate  = (TextView) view.findViewById(R.id.text_date);
        TextView tvValue = (TextView) view.findViewById(R.id.text_value);
        TextView tvUnit  = (TextView) view.findViewById(R.id.text_unit);
        TextView tvValue2 = (TextView) view.findViewById(R.id.text_value2);
        TextView tvUnit2  = (TextView) view.findViewById(R.id.text_unit2);
        ImageView ivType = (ImageView) view.findViewById(R.id.image_item_type);
        tvValue2.setVisibility(View.GONE);
        tvUnit2.setVisibility(View.GONE);
        String value = "", unit = "", date = "";
        Drawable type = null;
        //TODO

        Intent i = null;

        view.setTag(ro);


        if(ro instanceof Terumo){
            value = String.valueOf(((Terumo)ro).getValue()) + " ";
            unit = ((Terumo)ro).getStringUnit();
            date = ((Terumo)ro).getStringDate();
            type = ResourcesCompat.getDrawable(context.getResources(), R.drawable.blood_glucose, null);
            i = new Intent(context, GeneralGlucoseReadingActivity.class);
        }
        else if(ro instanceof BloodPressure){
          /*  if(isTerumoUser){
                view.setLayoutParams(new AbsListView.LayoutParams(-1,1));
                view.setVisibility(View.GONE);
                return view;
            }*/
            value = ((BloodPressure)ro).getSystolic() + "/"
                    + ((BloodPressure)ro).getDistolic() + " ";
            unit = ((BloodPressure)ro).getStringUnit();
            date = ((BloodPressure)ro).getStringDate();
            type = ResourcesCompat.getDrawable(context.getResources(), R.drawable.blood_pressure, null);
            i = new Intent(context, BloodPressureReadingActivity.class);
        }
        else if(ro instanceof Weight){
          /*  if(isTerumoUser){
                view.setLayoutParams(new AbsListView.LayoutParams(-1,1));
                view.setVisibility(View.GONE);
                return view;
            }*/
            value = ((Weight)ro).getWeight() + " ";
            unit = ((Weight)ro).getStringUnit();
            date = ((Weight)ro).getStringDate();
            type = ResourcesCompat.getDrawable(context.getResources(), R.drawable.weight, null);
            i = new Intent(context, WeightReadingActivity.class);
        }
        else if(ro instanceof Temperature){
            /*if(isTerumoUser){
                view.setLayoutParams(new AbsListView.LayoutParams(-1,1));
                view.setVisibility(View.GONE);
                return view;
            }*/
            value = ((Temperature)ro).getValue() + " ";
            unit = ((Temperature)ro).getStringUnit();
            date = ((Temperature)ro).getStringDate();
            type = ResourcesCompat.getDrawable(context.getResources(), R.drawable.temp, null);
            i = new Intent(context, TemperatureReadingActivity.class);
        }
        else if(ro instanceof SpO2Set){
            /*if(isTerumoUser){
                view.setLayoutParams(new AbsListView.LayoutParams(-1,1));
                view.setVisibility(View.GONE);
                return view;
            }*/
            //TODO: value = ((SpO2Set)ro).getLastSpO2() + " ";
            Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
            SpO2 spo2 = ((SpO2Set)ro).getHighestSpO2(realm);
            value = (spo2.getValue()+" ").replace(".0 ","").replace(" ","");
            unit = "%";
            date = ((SpO2Set)ro).getStringEndDate();
            type = ResourcesCompat.getDrawable(context.getResources(), R.drawable.spo2, null);
            i = new Intent(context, SpO2ReadingActivity.class);

            tvValue2.setText((spo2.getPulseRate()+" ").replace(".0 ","").replace(" ",""));
            tvUnit2.setText("bpm");
            tvValue2.setVisibility(View.VISIBLE);
            tvUnit2.setVisibility(View.VISIBLE);
            realm.close();
        } else if(ro instanceof Step){
            /*if(isTerumoUser){
                view.setLayoutParams(new AbsListView.LayoutParams(-1,1));
                view.setVisibility(View.GONE);
                return view;
            }*/
            //TODO: value = ((SpO2Set)ro).getLastSpO2() + " ";
            Realm realm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
            Step step = ((Step)ro);
            int state = step.getState();
            if(state==Step.DISCONNECTED){
                type = ResourcesCompat.getDrawable(context.getResources(), R.drawable.steps_disconnected, null);
            }else if(state==Step.CONNECTING){
                type = ResourcesCompat.getDrawable(context.getResources(), R.drawable.connecting_steps_anim, null);
                ivType.setTag("anim");
            }else{
                type = ResourcesCompat.getDrawable(context.getResources(), R.drawable.steps_connected, null);
            }
            value = ((Step)ro).getSteps()+"";
            unit = "steps";
            i = null;
            realm.close();
        }

        value = value.replace(".0 "," ").replace(".0/","/").trim();

        tvDate.setText(date);
        tvUnit.setText(unit);
        tvValue.setText(value);
        if(type!=null) {
            ivType.setImageDrawable(type);
            if(ivType.getTag()!=null && ivType.getTag().equals("anim")){
                ((AnimationDrawable) ivType.getDrawable()).start();
            }
        }

        ivType.setTag(i);
        if(activity instanceof DashboardActivity) {
            if(!((DashboardActivity) activity).isCaregiver()) {
                ivType.setOnClickListener(onClickListener);
            }
        }
        return view;
    }

    OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            Object object = view.getTag();
            if(object instanceof Intent) {
                Intent i = (Intent)object;
                if(i!=null) {
                    context.startActivity(i);
                }
            }
        }
    };
}

