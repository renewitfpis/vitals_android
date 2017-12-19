package sg.lifecare.medicare.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmObject;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Medication;
import sg.lifecare.medicare.database.model.Note;
import sg.lifecare.medicare.database.model.Photo;
import sg.lifecare.medicare.database.model.Symptom;
import sg.lifecare.medicare.database.model.Temperature;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.database.model.User;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.utils.HtmlUtil;
import timber.log.Timber;

/**
 * Timeline adapter
 */
public class ActivityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_TERUMO = 1;
    public static final int TYPE_SYMPTOM = 2;
    public static final int TYPE_PHOTO = 3;
    public static final int TYPE_MEDICATION = 4;
    public static final int TYPE_BLOOD_PRESSURE = 5;
    public static final int TYPE_WEIGHT = 6;
    public static final int TYPE_TEMPERATURE = 7;
    public static final int TYPE_NOTE = 8;

    private static ClickListener clickListener;
    private static Context mContext;
    private Realm mRealm;
    private User mUser;

    static class TerumoView extends RecyclerView.ViewHolder implements View.OnClickListener  {
        TextView timeText;
        TextView valueText;
        TextView unitText;
        TextView remarkText;
        TextView mealTakenText;
        ImageView expressionImg;

        public TerumoView(View view) {
            super(view);
            view.setOnClickListener(this);
            timeText = (TextView) view.findViewById(R.id.text_time);
            remarkText = (TextView) view.findViewById(R.id.text_remark);
            valueText = (TextView) view.findViewById(R.id.text_value);
            unitText = (TextView) view.findViewById(R.id.text_unit);
            mealTakenText = (TextView) view.findViewById(R.id.text_time_of_meal);
            expressionImg = (ImageView) view.findViewById(R.id.image_caution);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(),TYPE_TERUMO,v);
        }

    }

    static class SymptomView extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView timeText;
        TextView remarkText;
        TextView typeText;
        View greyLineView;
        ImageView[] imageList = new ImageView[4];
        TextView[] nameList = new TextView[4];

        public SymptomView(View view) {
            super(view);
            timeText = (TextView) view.findViewById(R.id.text_time);
            typeText = (TextView) view.findViewById(R.id.text_type);
            remarkText = (TextView) view.findViewById(R.id.text_remark);
           /*greyLineView = (View) view.findViewById(R.id.grey_line_view);

            nameList[0] = (TextView) view.findViewById(R.id.text1);
            nameList[1] = (TextView) view.findViewById(R.id.text2);
            nameList[2] = (TextView) view.findViewById(R.id.text3);
            nameList[3] = (TextView) view.findViewById(R.id.text4);
            imageList[0] = (ImageView) view.findViewById(R.id.Image_symptom);
            imageList[1] = (ImageView) view.findViewById(R.id.Image_symptom2);
            imageList[2] = (ImageView) view.findViewById(R.id.Image_symptom3);
            imageList[3] = (ImageView) view.findViewById(R.id.Image_symptom4);*/
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(),TYPE_SYMPTOM,v);
        }
    }

    static class PhotoView extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView timeText;
        TextView remarkText;
        ImageView photoImage;

        public PhotoView(View view) {
            super(view);
            view.setOnClickListener(this);
            timeText = (TextView) view.findViewById(R.id.text_time);
            remarkText = (TextView) view.findViewById(R.id.text_remark);
            photoImage = (ImageView) view.findViewById(R.id.image_photo);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(),TYPE_PHOTO,v);
        }
    }

    static class MedicationView extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView timeText;
        TextView typeText;
        TextView dosageText;

        public MedicationView(View view) {
            super(view);
            view.setOnClickListener(this);
            timeText = (TextView) view.findViewById(R.id.text_time);
            typeText = (TextView) view.findViewById(R.id.text_type);
            dosageText = (TextView) view.findViewById(R.id.text_dosage);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(),TYPE_MEDICATION,v);
        }
    }

    static class BPView extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView timeText;
        TextView bpText;
        TextView pulseText;
        TextView arterialText;
        TextView remarkText;
        ImageView cautionIcon;

        public BPView(View view) {
            super(view);
            view.setOnClickListener(this);
            bpText = (TextView) view.findViewById(R.id.text_bp);
            timeText = (TextView) view.findViewById(R.id.text_time);
            pulseText = (TextView) view.findViewById(R.id.text_pulse);
            //arterialText = (TextView) view.findViewById(R.id.text_arterial);
            remarkText = (TextView) view.findViewById(R.id.text_remark);
            cautionIcon = (ImageView) view.findViewById(R.id.image_caution);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(),TYPE_BLOOD_PRESSURE,v);
        }
    }

    static class WeightView extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView timeText;
        TextView weightText;

        public WeightView(View view) {
            super(view);
            view.setOnClickListener(this);
            timeText = (TextView) view.findViewById(R.id.text_time);
            weightText = (TextView) view.findViewById(R.id.text_weight);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(),TYPE_WEIGHT,v);
        }
    }

    static class TemperatureView extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView timeText;
        TextView weightText;

        public TemperatureView(View view) {
            super(view);
            view.setOnClickListener(this);
            timeText = (TextView) view.findViewById(R.id.text_time);
            weightText = (TextView) view.findViewById(R.id.text_weight);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(),TYPE_TEMPERATURE,v);
        }
    }

    static class NoteView extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView timeText;
        TextView noteText;

        public NoteView(View view) {
            super(view);
            view.setOnClickListener(this);
            timeText = (TextView) view.findViewById(R.id.text_time);
            noteText = (TextView) view.findViewById(R.id.text_weight);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(),TYPE_NOTE,v);
        }
    }

    private List<RealmObject> mList;

    public ActivityAdapter(List<RealmObject> list, Context context) {
        mList = list;
        mContext = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh = null;

        //Timber.d("onCreateViewHolder: viewType=" + viewType);

        if (viewType == TYPE_TERUMO) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.timeline_item_terumo_new, parent, false);
            vh = new TerumoView(v);
        } else if (viewType == TYPE_SYMPTOM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.timeline_item_symptom_new, parent, false);
            vh = new SymptomView(v);
        } else if (viewType == TYPE_PHOTO) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.timeline_item_photo_new, parent, false);
            vh = new PhotoView(v);
        } else if (viewType == TYPE_MEDICATION) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.timeline_item_medication_new, parent, false);
            vh = new MedicationView(v);
        } else if (viewType == TYPE_BLOOD_PRESSURE) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.timeline_item_bp_new, parent, false);
            vh = new BPView(v);
        } else if (viewType == TYPE_WEIGHT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.timeline_item_weight_new, parent, false);
            vh = new WeightView(v);
        } else if (viewType == TYPE_TEMPERATURE){
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.timeline_item_temp_new, parent, false);
            vh = new TemperatureView(v);
        } else if (viewType == TYPE_NOTE){
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.timeline_item_note, parent, false);
            vh = new NoteView(v);
        }

        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(mList.isEmpty())
            return;

        RealmObject ro = mList.get(position);

        switch (holder.getItemViewType()) {
            case TYPE_TERUMO:
                Terumo terumo = (Terumo) ro;
                boolean isHealthy = true;
                double glucoseLevel = terumo.getValue();
                if(terumo.getUnit() == 1) glucoseLevel = glucoseLevel/18d;

                glucoseLevel = glucoseLevel * 100d;
                glucoseLevel = Math.round(glucoseLevel);
                glucoseLevel = glucoseLevel / 100d;

                if(terumo.isBeforeMeal()==null){
                    mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
                    mRealm.beginTransaction();
                    terumo.setBeforeMeal(true);
                    mRealm.commitTransaction();
                    mRealm.close();
                }
                // refer to astralink slack for assessment info
                if(terumo.isBeforeMeal()) {
                    if (glucoseLevel < 4.0 || glucoseLevel > 7.0) {
                        isHealthy = false;
                    }
                }
                else{
                    if (glucoseLevel > 9.0) {
                        isHealthy = false;
                    }
                }
                String valueStr = String.valueOf(glucoseLevel);
                String unitStr = mContext.getResources().getString(R.string.display_unit_mmol_per_l);//terumo.getStringUnit();
                String displayStr = valueStr + " " + unitStr;

               /* SpannableString ss =  new SpannableString(displayStr);
                ss.setSpan(new RelativeSizeSpan(0.8f), valueStr.length(), displayStr.length() , 0); // set size*/

                ((TerumoView)holder).valueText.setText(valueStr);
                ((TerumoView)holder).unitText.setText(unitStr);
                ((TerumoView)holder).timeText.setText(getTime(terumo.getDate()));
                ((TerumoView)holder).mealTakenText.setText(terumo.isBeforeMeal()?"Before Meal":"After Meal");

                if(terumo.getRemark()!=null&&!terumo.getRemark().equalsIgnoreCase("")){
                    ((TerumoView)holder).remarkText.setVisibility(View.VISIBLE);
                    ((TerumoView)holder).remarkText.setText( HtmlUtil.decodeString(terumo.getRemark()));
                }else{
                    ((TerumoView)holder).remarkText.setVisibility(View.GONE);
                }

                if(!isHealthy){
                    ((TerumoView)holder).expressionImg.setVisibility(View.VISIBLE);
                    ((TerumoView)holder).valueText.setTextColor(Color.RED);
                    ((TerumoView)holder).unitText.setTextColor(Color.RED);
                }else{
                    ((TerumoView)holder).expressionImg.setVisibility(View.GONE);
                    ((TerumoView)holder).valueText.setTextColor(mContext.getResources().getColor(R.color.black_lighter));
                    ((TerumoView)holder).unitText.setTextColor(mContext.getResources().getColor(R.color.black_lighter));
                }

                break;

            case TYPE_SYMPTOM:
                //TypedArray maleIcons = mContext.getResources().obtainTypedArray(R.array.sign_and_symptoms_male_icons);
                //TypedArray femaleIcons = mContext.getResources().obtainTypedArray(R.array.sign_and_symptoms_female_icons);
                String[] symptoms = mContext.getResources().getStringArray(R.array.symptoms_names);

                Symptom symptom = (Symptom) ro;
                String data = symptom.getSymptoms();
                String[] items = data.split(",");
                ArrayList<Integer> symptomId = new ArrayList<>();
                //mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
                //mUser = mRealm.where(User.class).findFirst();

                if(items.length == 17) {
                    for (int i = 0; i < items.length; i++) {
                        if (items[i].equals("t")) {
                            symptomId.add(i);
                        }
                    }
                }
                else{
                    for (int i = 0; i < items.length; i++){
                        int num = Integer.valueOf(items[i]) - 1;
                        symptomId.add(num);
                        if(num>17){
                            return;
                        }
                    }
                }
                ((SymptomView)holder).typeText.setText("");

                for(int i = 0; i < symptomId.size(); i++){
                    Timber.d("Symptom Id = " + i);
                    ((SymptomView)holder).typeText.append(symptoms[symptomId.get(i)]);
                    if( i != symptomId.size()-1){
                        ((SymptomView)holder).typeText.append("\n");
                    }
                }

                if(!symptom.getRemark().isEmpty()){
                    ((SymptomView)holder).remarkText.setVisibility(View.VISIBLE);
                    ((SymptomView)holder).remarkText.setText(HtmlUtil.decodeString(symptom.getRemark()));
                }else{
                    ((SymptomView)holder).remarkText.setVisibility(View.GONE);
                }
                /*for(int i = 0; i < ((SymptomView)holder).imageList.length; i++){
                    ((SymptomView)holder).imageList[i].setVisibility(View.GONE);
                    ((SymptomView)holder).nameList[i].setVisibility(View.GONE);
                }*/

                /*for(int i = 0; i < symptomId.size(); i++){

                    ((SymptomView)holder).imageList[i].setVisibility(View.VISIBLE);
                    ((SymptomView)holder).nameList[i].setVisibility(View.VISIBLE);
                    ((SymptomView)holder).nameList[i].setText(symptoms[symptomId.get(i)]);

                    if ((mUser != null) && (mUser.isFemale())) {
                        ((SymptomView)holder).imageList[i].setImageDrawable(femaleIcons.getDrawable(symptomId.get(i)));
                    }else{
                        ((SymptomView)holder).imageList[i].setImageDrawable(maleIcons.getDrawable(symptomId.get(i)));
                    }
                }
                maleIcons.recycle();
                femaleIcons.recycle();*/

                ((SymptomView)holder).timeText.setText(getTime(symptom.getDate()));
               /* if(symptom.getRemark().equals("")){
                    //((SymptomView)holder).greyLineView.setVisibility(View.GONE);
                    ((SymptomView)holder).remarkText.setVisibility(View.GONE);
                }
                else {
                    String remarks = HtmlUtil.decodeString(symptom.getRemark());
                    //((SymptomView)holder).greyLineView.setVisibility(View.VISIBLE);
                    ((SymptomView)holder).remarkText.setVisibility(View.VISIBLE);
                    ((SymptomView) holder).remarkText.setText(remarks);
                }*/

                //mRealm.close();
                break;

            case TYPE_PHOTO:
                final Photo photo = (Photo) ro;

               final String remarks = HtmlUtil.decodeString(photo.getRemark());
                if(remarks!=null&&!remarks.isEmpty()){
                    ((PhotoView)holder).remarkText.setVisibility(View.VISIBLE);
                    ((PhotoView)holder).remarkText.setText(remarks);
                }else{
                    ((PhotoView)holder).remarkText.setVisibility(View.GONE);
                }
                ((PhotoView)holder).timeText.setText(getTime(photo.getDate()));
               /* ((PhotoView)holder).photoImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(mContext, EnlargeImageActivity.class);
                        intent.putExtra("Image",photo.getImage());
                        intent.putExtra("Remarks",remarks);
                        mContext.startActivity(intent);
                    }
                });*/
                ((PhotoView)holder).photoImage.setTag(photo.getImage());
                if(photo.isUrl()){
                    Picasso.with(mContext).load(photo.getImage())
                            //.placeholder(R.drawable.avatarboy_1)
                            .into(((PhotoView) holder).photoImage);
                }
                else {
                    Picasso.with(mContext).load("file:///" + photo.getImage()).into(((PhotoView) holder).photoImage);
                }

                break;

            case TYPE_MEDICATION:
                final Medication medication = (Medication) ro;
                String dosageStr = (medication.getDosage()+" "+medication.getStringUnit()).replace(".0 "," ");
                if(medication.getDosage()>1 && medication.getStringUnit().equalsIgnoreCase("unit"))
                    dosageStr = dosageStr + "s";

                ((MedicationView)holder).timeText.setText(getTime(medication.getDate()));
                ((MedicationView)holder).typeText.setText(medication.getStringType());
                ((MedicationView)holder).dosageText.setText(dosageStr);
                break;

            case TYPE_BLOOD_PRESSURE:
                final BloodPressure bp = (BloodPressure) ro;

                boolean isNormal = true;
                float systolic = bp.getSystolic();
                float diastolic = bp.getDistolic();
                if(systolic > 120 && diastolic > 80) isNormal = false;

                String bpValueStr = (systolic+"/"+diastolic+" ").replace(".0/","/").replace(".0 "," ");
                String bpUnitStr = bp.getStringUnit();
                String bpDisplayStr = bpValueStr + bpUnitStr;

                 //String arterialStr = (bp.getArterialPressure()+" "+bp.getStringUnit()).replace(".0 "," ");
                String pulseRateStr = (bp.getPulseRate()+" bpm").replace(".0 "," ");

                SpannableString ss2 =  new SpannableString(bpDisplayStr);
                ss2.setSpan(new RelativeSizeSpan(0.8f), bpValueStr.length(), bpDisplayStr.length() , 0); // set size
                if(!isNormal) {
                    ss2.setSpan(new ForegroundColorSpan(Color.RED), 0, bpDisplayStr.length(), 0);// set color
                }
                ((BPView)holder).bpText.setText(ss2);
                ((BPView)holder).pulseText.setText(pulseRateStr);
                //((BPView)holder).arterialText.setVisibility(View.GONE);
                //((BPView)holder).arterialText.setText(arterialStr);
                ((BPView)holder).timeText.setText(getTime(bp.getDate()));
                if(bp.getRemark().equalsIgnoreCase("")) {
                    ((BPView) holder).remarkText.setVisibility(View.GONE);
                }
                else{
                    ((BPView)holder).remarkText.setVisibility(View.VISIBLE);
                    ((BPView)holder).remarkText.setText(bp.getRemark());
                }
                //((BPView)holder).remarkText.setText(getTime(bp.getRemark()));

                if(isNormal)
                    ((BPView)holder).cautionIcon.setVisibility(View.GONE);
                else
                    ((BPView)holder).cautionIcon.setVisibility(View.VISIBLE);

                break;

            case TYPE_WEIGHT:
                final Weight weight = (Weight) ro;
                double value = weight.getWeight();
                //trick to round up to 2 decimal places
                value = value*100;
                value = Math.round(value);
                value = value/100;

                String weightStr = (value+" "+weight.getStringUnit());
                weightStr = weightStr.replace(".0 "," ");

                ((WeightView)holder).weightText.setText(weightStr);
                ((WeightView)holder).timeText.setText(getTime(weight.getDate()));
                break;

            case TYPE_TEMPERATURE:
                final Temperature temp = (Temperature) ro;
                double tempVal = temp.getValue();
                //trick to round up to 2 decimal places
                tempVal = tempVal*100;
                tempVal = Math.round(tempVal);
                tempVal = tempVal/100;

                String tempStr = (tempVal+" "+temp.getStringUnit());
                tempStr = tempStr.replace(".0 "," ");

                ((TemperatureView)holder).weightText.setText(tempStr);
                ((TemperatureView)holder).timeText.setText(getTime(temp.getDate()));
                break;

            case TYPE_NOTE:
                final Note note = (Note) ro;
                String notes = HtmlUtil.decodeString(note.getNote());

                ((NoteView)holder).noteText.setText(notes);
                ((NoteView)holder).timeText.setText(getTime(note.getDate()));
                break;
        }

    }

    @Override
    public int getItemCount() {
        int count = 0;
        if (mList != null) {
            count = mList.size();
        }

        //Timber.d("getItemCount: count=" + count);

        return count;
    }

    @Override
    public int getItemViewType(int position) {

        RealmObject ro = mList.get(position);

        if (ro instanceof Terumo) {
            return TYPE_TERUMO;
        } else if (ro instanceof Symptom) {
            return TYPE_SYMPTOM;
        } else if (ro instanceof Photo) {
            return TYPE_PHOTO;
        } else if (ro instanceof Medication){
            return TYPE_MEDICATION;
        } else if (ro instanceof BloodPressure) {
            return TYPE_BLOOD_PRESSURE;
        } else if (ro instanceof Weight) {
            return TYPE_WEIGHT;
        } else if (ro instanceof Temperature) {
            return TYPE_TEMPERATURE;
        } else if (ro instanceof Note) {
            return TYPE_NOTE;
        }

        return TYPE_UNKNOWN;
    }

    public void addAll(ArrayList<RealmObject> lists) {
        mList = lists;
        notifyDataSetChanged();
    }

    private String getTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        String time = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        return time;
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        ActivityAdapter.clickListener = clickListener;
    }

    public interface ClickListener {
        void onItemClick(int position, int type, View v);
        //void onItemLongClick(int position, int type, View v);
    }
}
