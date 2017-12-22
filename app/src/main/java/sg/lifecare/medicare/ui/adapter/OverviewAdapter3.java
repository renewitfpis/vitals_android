package sg.lifecare.medicare.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.SpO2;
import sg.lifecare.medicare.database.model.Step;
import sg.lifecare.medicare.database.model.Temperature;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.database.model.User;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.object.OverviewItem;
import sg.lifecare.medicare.ui.DashboardActivity;
import timber.log.Timber;

/**
 * Timeline adapter
 */
public class OverviewAdapter3 extends RecyclerView.Adapter<RecyclerView.ViewHolder>  /*RecyclerView.Adapter<RecyclerView.ViewHolder>*/{

    public static final int TYPE_UNKNOWN = -1;
    public static final int TYPE_TERUMO = 0;
    public static final int TYPE_BLOOD_PRESSURE = 1;
    public static final int TYPE_WEIGHT = 2;
    public static final int TYPE_TEMPERATURE = 3;
    public static final int TYPE_SPO2 = 4;
    public static final int TYPE_STEP = 5;
    public static final int TYPE_SEPARATOR = 6;
    public static final int TYPE_NOTE = 7;
    public static final int TYPE_MEDICATION = 8;
    public static final int TYPE_SYMPTOM = 9;
    public static final int TYPE_PHOTO = 10;
    public static final int TYPE_ADD_MORE = 11;

    public static final int LAST_ITEM = TYPE_PHOTO;
    public static final int LAST_ITEM_CAREGIVER = TYPE_STEP;

    private static ClickListener clickListener;
    private static Context mContext;
    private static Activity mActivity;
    private Realm mRealm;
    private User mUser;
    private static int viewType;

    static class VitalsItemView extends RecyclerView.ViewHolder implements OnClickListener  {
        TextView valueText;
        TextView unitText;
        TextView nameText;
        ImageView iconImg;
        ImageView menuImg;
        ImageView bigIconImg;
        Button pairMeasureBtn;

        public VitalsItemView(View view) {
            super(view);
            valueText = (TextView) view.findViewById(R.id.text_value);
            unitText = (TextView) view.findViewById(R.id.text_unit);
            nameText = (TextView) view.findViewById(R.id.text_name);
            menuImg = (ImageView) view.findViewById(R.id.image_menu);
            iconImg = (ImageView) view.findViewById(R.id.image_icon);
            bigIconImg = (ImageView) view.findViewById(R.id.image_big_icon);
            pairMeasureBtn = (Button) view.findViewById(R.id.button_pair_measure);
            pairMeasureBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickListener.onPairButtonClick(getAdapterPosition(),getItemViewType(),v);
                }
            });

            valueText.setOnClickListener(onMainInfoClickListener);
            unitText.setOnClickListener(onMainInfoClickListener);
            nameText.setOnClickListener(onMainInfoClickListener);
            iconImg.setOnClickListener(onMainInfoClickListener);
            bigIconImg.setOnClickListener(onMainInfoClickListener);

            menuImg.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPopupMenu(view);
                }
            });
        }
        private void showPopupMenu(final View view) {
            if(getItemViewType()==TYPE_SPO2
                    || getItemViewType()==TYPE_STEP)
                return;

            // inflate menu
            PopupMenu popup = new PopupMenu(view.getContext(),view);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.overview_vitals_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    clickListener.onMenuItemClick(item.getItemId(),getItemViewType(),view);
                    return false;
                }
            });
            popup.show();
        }

        @Override
        public void onClick(View v) {
            Timber.d("ViewType = " + getItemViewType());
            clickListener.onItemClick(getAdapterPosition(),getItemViewType(),pairMeasureBtn);
        }

        OnClickListener onMainInfoClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onItemClick(getAdapterPosition(),getItemViewType(),valueText);
            }
        };
    }

    static class SymptomView extends RecyclerView.ViewHolder implements OnClickListener{
        TextView nameText;
        ImageView imgAdd;
        ImageView imgType;

        public SymptomView(View view) {
            super(view);
            view.setOnClickListener(this);
            nameText = (TextView) view.findViewById(R.id.text_name);
            imgAdd = (ImageView) view.findViewById(R.id.image_add);
            imgType = (ImageView) view.findViewById(R.id.image_icon);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(),TYPE_SYMPTOM,v);
        }
    }

    static class PhotoView extends RecyclerView.ViewHolder implements OnClickListener {
        TextView nameText;
        ImageView imgAdd;
        ImageView imgType;

        public PhotoView(View view) {
            super(view);
            view.setOnClickListener(this);
            nameText = (TextView) view.findViewById(R.id.text_name);
            imgAdd = (ImageView) view.findViewById(R.id.image_add);
            imgType = (ImageView) view.findViewById(R.id.image_icon);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(),TYPE_PHOTO,v);
        }
    }

    static class MedicationView extends RecyclerView.ViewHolder implements OnClickListener{
        TextView nameText;
        ImageView imgAdd;
        ImageView imgType;

        public MedicationView(View view) {
            super(view);
            view.setOnClickListener(this);
            nameText = (TextView) view.findViewById(R.id.text_name);
            imgAdd = (ImageView) view.findViewById(R.id.image_add);
            imgType = (ImageView) view.findViewById(R.id.image_icon);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(),TYPE_MEDICATION,v);
        }
    }

    static class AddMoreView extends RecyclerView.ViewHolder implements OnClickListener {

        public AddMoreView(View view) {
            super(view);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(),TYPE_ADD_MORE,v);
        }
    }

    static class NoteView extends RecyclerView.ViewHolder implements OnClickListener {
        TextView nameText;
        ImageView imgAdd;
        ImageView imgType;

        public NoteView(View view) {
            super(view);
            view.setOnClickListener(this);
            nameText = (TextView) view.findViewById(R.id.text_name);
            imgAdd = (ImageView) view.findViewById(R.id.image_add);
            imgType = (ImageView) view.findViewById(R.id.image_icon);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(),TYPE_NOTE,v);
        }
    }

    private List<OverviewItem> mList;

    public OverviewAdapter3(List<OverviewItem> list, Context context, Activity activity) {
        mList = list;
        mContext = context;
        mActivity = activity;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType2) {
        RecyclerView.ViewHolder vh = null;

        Timber.d("onCreateViewHolder: viewType = " + viewType2);
        this.viewType = viewType2;

        if (viewType == TYPE_TERUMO) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.overview_item_view_bg, parent, false);
            vh = new VitalsItemView(v);
        } else if (viewType == TYPE_SYMPTOM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.overview_item_add_misc, parent, false);
            vh = new SymptomView(v);
        } else if (viewType == TYPE_PHOTO) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.overview_item_add_misc, parent, false);
            vh = new PhotoView(v);
        } else if (viewType == TYPE_MEDICATION) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.overview_item_add_misc, parent, false);
            vh = new MedicationView(v);
        } else if (viewType == TYPE_BLOOD_PRESSURE) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.overview_item_view_bg, parent, false);
            vh = new VitalsItemView(v);
        } else if (viewType == TYPE_WEIGHT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.overview_item_view_bg, parent, false);
            vh = new VitalsItemView(v);
        } else if (viewType == TYPE_TEMPERATURE){
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.overview_item_view_bg, parent, false);
            vh = new VitalsItemView(v);
        } else if (viewType == TYPE_NOTE){
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.overview_item_add_misc, parent, false);
            vh = new NoteView(v);
        } else if (viewType == TYPE_SPO2){
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.overview_item_view_bg, parent, false);
            vh = new VitalsItemView(v);
        }  else if (viewType == TYPE_STEP){
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.overview_item_view_bg, parent, false);
            vh = new VitalsItemView(v);
        } else if (viewType == TYPE_ADD_MORE){
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.overview_item_add_more, parent, false);
            vh = new AddMoreView(v);
        } else if (viewType == TYPE_SEPARATOR){
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.view_empty, parent, false);
            vh = new AddMoreView(v);
        }

        return vh;
    }

   /* @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int section, int relativePosition, int absolutePosition) {
        // Setup non-header view.
        // 'section' is section index.
        // 'relativePosition' is index in this section.
        // 'absolutePosition' is index out of all non-header items.
        // See sample project for a visual of how these indices work.
    }*/
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        Timber.d("On Bind View Holder1");

        if(mList.isEmpty())
            return;

        Timber.d("On Bind View Holder2");

        OverviewItem ro = mList.get(position);
        if(holder instanceof VitalsItemView){
            Timber.d("SET TAG ! is RealmObject null? = " + (ro == null));
            ((VitalsItemView)holder).pairMeasureBtn.setTag(ro);
            ((VitalsItemView)holder).valueText.setTag(ro);
            ((VitalsItemView)holder).unitText.setTag(ro);
            ((VitalsItemView)holder).iconImg.setTag(ro);
            ((VitalsItemView)holder).bigIconImg.setTag(ro);
            ((VitalsItemView)holder).menuImg.setTag(ro);
        }

        mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
        int color;
        viewType = holder.getItemViewType();
        if( ro.getValue().equalsIgnoreCase("") && viewType==TYPE_NOTE
                ||  ro.getValue().equalsIgnoreCase("") && viewType==TYPE_MEDICATION){
            Timber.e("PROCEED 1 " + viewType);
        }/*else if(ro.getValue().equalsIgnoreCase(""))
        {
            Timber.e("RETURN " + viewType);
            return;
        } */else{
            Timber.e("PROCEED 2 " + viewType);
        }

        if(holder instanceof VitalsItemView) {
            Timber.e("RO VALUE = " + ro.getValue());

            if (ro.getValue().equalsIgnoreCase("")) {
                ((VitalsItemView) holder).unitText.setVisibility(View.INVISIBLE);
                ((VitalsItemView) holder).iconImg.setVisibility(View.INVISIBLE);
                ((VitalsItemView) holder).bigIconImg.setVisibility(View.VISIBLE);
                ((VitalsItemView) holder).valueText.setText("");
            } else {
                ((VitalsItemView) holder).unitText.setVisibility(View.VISIBLE);
                ((VitalsItemView) holder).iconImg.setVisibility(View.VISIBLE);
                ((VitalsItemView) holder).bigIconImg.setVisibility(View.GONE);
                ((VitalsItemView) holder).valueText.setText("");
            }
        }

        if(holder instanceof VitalsItemView) {

            if(mActivity instanceof DashboardActivity){
                boolean isCaregiver = ((DashboardActivity) mActivity).isCaregiver();
                if(isCaregiver){
                    ((VitalsItemView) holder).pairMeasureBtn.setVisibility(View.GONE);
                    ((VitalsItemView) holder).menuImg.setVisibility(View.GONE);
                }
            }
            if(viewType == TYPE_STEP){
                if (ro.isDevicePaired()) {
                    if(ro.getState() != Step.DISCONNECTED) {
                        ((VitalsItemView) holder).pairMeasureBtn.setText("Measuring");
                    }else{
                        ((VitalsItemView) holder).pairMeasureBtn.setText("Disconnected");
                    }
                }else{
                    ((VitalsItemView) holder).pairMeasureBtn.setText("Pair");
                }
            }else {
                Timber.d("RO = " + ro.getType() + ", isDevicePaired = " + ro.isDevicePaired());
                if (ro.getState() != Step.DISCONNECTED) {
                    //color = mContext.getResources().getColor(R.color.white);
                    //((VitalsItemView) holder).pairMeasureBtn.setTextColor(color);
                    if (ro.isDevicePaired()) {
                        ((VitalsItemView) holder).pairMeasureBtn.setText("Measure");
                    } else {
                        ((VitalsItemView) holder).pairMeasureBtn.setText("Pair");
                    }
                } else {
                    //color = mContext.getResources().getColor(R.color.light_grey);
                    // ((VitalsItemView) holder).pairMeasureBtn.setBackgroundColor(color);
                    ((VitalsItemView) holder).pairMeasureBtn.setText("Pair");//"Disconnected"
                    //((VitalsItemView) holder).pairMeasureBtn.setTextColor(color);
                }
            }
        }

        switch (viewType) {
            case TYPE_TERUMO:
                Timber.d("DATE = " + ro.getDate().toString());

                color = mContext.getResources().getColor(R.color.overview_glucose);
                ((VitalsItemView)holder).valueText.setTextColor(color);
                ((VitalsItemView)holder).pairMeasureBtn.setBackgroundColor(color);
                ((VitalsItemView)holder).nameText.setText(R.string.blood_glucose);
                ((VitalsItemView)holder).iconImg.setImageResource(R.drawable.blood_glucose);
                ((VitalsItemView)holder).bigIconImg.setImageResource(R.drawable.blood_glucose);

                Terumo terumo = PatientData.getInstance().getTerumoBySpecificDate(mRealm,ro.getDate());
                if(terumo == null)
                    return;
                if(holder instanceof VitalsItemView){
                    ((VitalsItemView)holder).pairMeasureBtn.setTag(terumo);
                    ((VitalsItemView)holder).valueText.setTag(terumo);
                    ((VitalsItemView)holder).menuImg.setTag(terumo);
                }
                boolean isHealthy = true;
                double glucoseLevel = terumo.getValue();
                //if(terumo.getUnit() == 1) glucoseLevel = glucoseLevel/18d;

                if(terumo.isBeforeMeal()==null){
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
                String valueStr = String.format(Locale.getDefault(),"%.1f", glucoseLevel);
                String unitStr = terumo.getStringUnit();

                ((VitalsItemView)holder).valueText.setText(valueStr);
                ((VitalsItemView)holder).unitText.setText(unitStr);

                break;

            case TYPE_SYMPTOM:
                if(holder instanceof SymptomView){
                    ((SymptomView) holder).nameText.setText("Symptom");
                    ((SymptomView) holder).imgType.setImageResource(R.drawable.action_symptoms);
                }
                break;

            case TYPE_PHOTO:
                if(holder instanceof PhotoView){
                ((PhotoView) holder).nameText.setText("Food");
                ((PhotoView) holder).imgType.setImageResource(R.drawable.action_food);
                }
                break;

            case TYPE_MEDICATION:
                if(holder instanceof MedicationView){
                    ((MedicationView) holder).nameText.setText("Medication");
                    ((MedicationView) holder).imgType.setImageResource(R.drawable.action_medication);
                }
                break;

            case TYPE_BLOOD_PRESSURE:
                color = mContext.getResources().getColor(R.color.overview_pressure);
                ((VitalsItemView)holder).valueText.setTextColor(color);
                ((VitalsItemView)holder).pairMeasureBtn.setBackgroundColor(color);
                ((VitalsItemView)holder).nameText.setText(R.string.blood_pressure);
                ((VitalsItemView)holder).iconImg.setImageResource(R.drawable.blood_pressure);
                ((VitalsItemView)holder).bigIconImg.setImageResource(R.drawable.blood_pressure);

                final BloodPressure bp = PatientData.getInstance().getBloodPressureBySpecificDate(mRealm,ro.getDate());
                if(bp == null)
                    return;
                if(holder instanceof VitalsItemView){
                    ((VitalsItemView)holder).pairMeasureBtn.setTag(bp);
                    ((VitalsItemView)holder).valueText.setTag(bp);
                    ((VitalsItemView)holder).menuImg.setTag(bp);
                }
                boolean isNormal = true;
                float systolic = bp.getSystolic();
                float diastolic = bp.getDistolic();
                if(systolic > 120 && diastolic > 80) isNormal = false;

                String bpValueStr = (systolic+"/"+diastolic+" ").replace(".0/","/").replace(".0 ","");
                String bpUnitStr = bp.getStringUnit();
                String bpDisplayStr = bpValueStr + bpUnitStr;

                 //String arterialStr = (bp.getArterialPressure()+" "+bp.getStringUnit()).replace(".0 "," ");
                String pulseRateStr = (bp.getPulseRate()+" bpm").replace(".0 "," ");

                SpannableString ss2 =  new SpannableString(bpDisplayStr);
                ss2.setSpan(new RelativeSizeSpan(0.8f), bpValueStr.length(), bpDisplayStr.length() , 0); // set size
                if(!isNormal) {
                    ss2.setSpan(new ForegroundColorSpan(Color.RED), 0, bpDisplayStr.length(), 0);// set color
                }

                ((VitalsItemView)holder).valueText.setText(bpValueStr);
                ((VitalsItemView)holder).unitText.setText(bpUnitStr);
                //((VitalsItemView)holder).timeText.setText(getTime(bp.getDate()));
                /*if(bp.getRemark().equalsIgnoreCase("")) {
                    ((BPView) holder).remarkText.setVisibility(View.GONE);
                }
                else{
                    ((BPView)holder).remarkText.setVisibility(View.VISIBLE);
                    ((BPView)holder).remarkText.setText(bp.getRemark());
                }*/
                //((BPView)holder).remarkText.setText(getTime(bp.getRemark()));

               /* if(isNormal) {
                    ((BPView) holder).cautionIcon.setVisibility(View.GONE);
                }
                else {
                    ((BPView) holder).cautionIcon.setVisibility(View.VISIBLE);
                }*/

                break;

            case TYPE_WEIGHT:
                color = mContext.getResources().getColor(R.color.overview_weight);
                ((VitalsItemView)holder).valueText.setTextColor(color);
                ((VitalsItemView)holder).pairMeasureBtn.setBackgroundColor(color);
                ((VitalsItemView)holder).nameText.setText(R.string.weight_info_weight);
                ((VitalsItemView)holder).iconImg.setImageResource(R.drawable.weight);
                ((VitalsItemView)holder).bigIconImg.setImageResource(R.drawable.weight);

                final Weight weight = PatientData.getInstance().getWeightBySpecificDate(mRealm,ro.getDate());
                if(weight==null) {
                    return;
                }
                if(holder instanceof VitalsItemView){
                    ((VitalsItemView)holder).pairMeasureBtn.setTag(weight);
                    ((VitalsItemView)holder).valueText.setTag(weight);
                    ((VitalsItemView)holder).menuImg.setTag(weight);
                }
                double value = weight.getWeight();
                //trick to round up to 2 decimal places
                value = value*100;
                value = Math.round(value);
                value = value/100;

                String weightStr = (value+" ");
                weightStr = weightStr.replace(".0 ","").replace(" ","");

                ((VitalsItemView)holder).valueText.setText(weightStr);
                ((VitalsItemView)holder).unitText.setText(weight.getStringUnit());
                break;

            case TYPE_TEMPERATURE:
                color = mContext.getResources().getColor(R.color.overview_temperature);
                ((VitalsItemView)holder).valueText.setTextColor(color);
                ((VitalsItemView)holder).pairMeasureBtn.setBackgroundColor(color);
                ((VitalsItemView)holder).nameText.setText(R.string.temperature_info_temperature);
                ((VitalsItemView)holder).iconImg.setImageResource(R.drawable.temp);
                ((VitalsItemView)holder).bigIconImg.setImageResource(R.drawable.temp);

                final Temperature temp =  PatientData.getInstance().getTempBySpecificDate(mRealm,ro.getDate());

                if(temp == null){
                    return;
                }
                if(holder instanceof VitalsItemView){
                    ((VitalsItemView)holder).pairMeasureBtn.setTag(temp);
                    ((VitalsItemView)holder).valueText.setTag(temp);
                    ((VitalsItemView)holder).menuImg.setTag(temp);
                }
                double tempVal = temp.getValue();
                //trick to round up to 2 decimal places
                tempVal = tempVal*100;
                tempVal = Math.round(tempVal);
                tempVal = tempVal/100;

                String tempStr = (tempVal+" ");
                tempStr = tempStr.replace(".0 "," ").replace(" ","");

                ((VitalsItemView)holder).valueText.setText(tempStr);
                ((VitalsItemView)holder).unitText.setText(temp.getStringUnit());
                break;

            case TYPE_STEP:
                color = mContext.getResources().getColor(R.color.overview_steps);
                ((VitalsItemView)holder).valueText.setTextColor(color);
                ((VitalsItemView)holder).pairMeasureBtn.setBackgroundColor(color);
                ((VitalsItemView)holder).nameText.setText(R.string.step);
                ((VitalsItemView)holder).iconImg.setImageResource(R.drawable.steps_connected);
                ((VitalsItemView)holder).bigIconImg.setImageResource(R.drawable.steps_connected);
                ((VitalsItemView)holder).valueText.setText(ro.getValue());
                ((VitalsItemView)holder).unitText.setText("steps");
                ((VitalsItemView)holder).pairMeasureBtn.setText(ro.getStringState());

                Step step = new Step();
                ((VitalsItemView)holder).pairMeasureBtn.setTag(ro);
                ((VitalsItemView)holder).valueText.setTag(step);
                ((VitalsItemView)holder).menuImg.setTag(step);
                boolean isAnim = false;
                Drawable type, type2;
                int state = ro.getState();

                if(ro.isDevicePaired()) {
                    if (state == Step.DISCONNECTED) {
                        type = ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.steps_disconnected, null);
                        type2 = ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.steps_disconnected, null);

                        Timber.d("ST DISCONNECTED");
                    } else if (state == Step.CONNECTING) {
                        type = ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.connecting_steps_anim, null);
                        type2 = ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.connecting_steps_anim, null);

                        Timber.d("ST CONNECTING");
                        isAnim = true;
                    } else {
                        Timber.d("ST CONNECTED");
                        type = ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.steps_connected, null);
                        type2 = ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.steps_connected, null);
                    }
                }else{
                    type = ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.steps_connected, null);
                    type2 = ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.steps_connected, null);
                }

                try {
                    if (((VitalsItemView) holder).iconImg.getVisibility() == View.VISIBLE) {
                        Timber.d("ICON IMG IS VISIBLE");
                        ((VitalsItemView) holder).iconImg.setImageDrawable(type);
                        if (isAnim) {
                            Timber.d("IS ANIM");
                            ((AnimationDrawable) ((VitalsItemView) holder).iconImg.getDrawable()).start();
                        }
                    } else if (((VitalsItemView) holder).bigIconImg.getVisibility() == View.VISIBLE) {
                        ((VitalsItemView) holder).bigIconImg.setImageDrawable(type2);
                        if (isAnim) {
                            ((AnimationDrawable) ((VitalsItemView) holder).bigIconImg.getDrawable()).start();
                        }
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
                Timber.d("IS ANIM = " + isAnim);
                /*if(isAnim){
                    ((AnimationDrawable)((VitalsItemView)holder).iconImg.getDrawable()).start();
                    ((AnimationDrawable)((VitalsItemView)holder).bigIconImg.getDrawable()).start();
                }*/

               /* final Temperature temp =  PatientData.getInstance().getTempBySpecificDate(mRealm,ro.getDate());
                if(temp == null){
                    return;
                }
                if(holder instanceof VitalsItemView){
                    ((VitalsItemView)holder).pairMeasureBtn.setTag(temp);
                    ((VitalsItemView)holder).valueText.setTag(temp);
                    ((VitalsItemView)holder).menuImg.setTag(temp);
                }
                double tempVal = temp.getValue();
                //trick to round up to 2 decimal places
                tempVal = tempVal*100;
                tempVal = Math.round(tempVal);
                tempVal = tempVal/100;

                String tempStr = (tempVal+" "+temp.getStringUnit());
                tempStr = tempStr.replace(".0 "," ");

                ((VitalsItemView)holder).valueText.setText(tempVal+"");
                ((VitalsItemView)holder).unitText.setText(temp.getStringUnit());*/
                break;

            case TYPE_NOTE:
             /*TODO:complete   final Note note = (Note) ro;
                String notes = HtmlUtil.decodeString(note.getNote());

                ((NoteView)holder).noteText.setText(notes);
                ((NoteView)holder).timeText.setText(getTime(note.getDate()));*/
                Timber.d("TYPE NOTES!");
                if(holder instanceof NoteView){
                    ((NoteView) holder).nameText.setText("Notes");
                    ((NoteView) holder).imgType.setImageResource(R.drawable.action_note);
                }
                break;

            case TYPE_SPO2:
                color = mContext.getResources().getColor(R.color.overview_spo2);
                ((VitalsItemView)holder).valueText.setTextColor(color);
                ((VitalsItemView)holder).pairMeasureBtn.setBackgroundColor(color);
                ((VitalsItemView)holder).nameText.setText(R.string.spo2);
                ((VitalsItemView)holder).iconImg.setImageResource(R.drawable.spo2);
                ((VitalsItemView)holder).bigIconImg.setImageResource(R.drawable.spo2);

                Timber.d("SPO2!!");
                final SpO2 spo2 =  PatientData.getInstance().getSpO2BySpecificDate(mRealm,ro.getDate());
                if(spo2 == null){
                    return;
                }
                if(holder instanceof VitalsItemView){
                    Timber.d("SET TAG!!");
                    ((VitalsItemView)holder).pairMeasureBtn.setTag(spo2);
                    ((VitalsItemView)holder).valueText.setTag(spo2);
                    ((VitalsItemView)holder).menuImg.setTag(spo2);
                }else{
                    Timber.d("NOT SET TAG!!");
                }
                //mRealm = Realm.getInstance(PatientData.getInstance().getRealmConfig());
                //mRealm.beginTransaction();
                //SpO2 spO2 = spo2.getHighestSpO2(mRealm);
                //mRealm.commitTransaction();
                //mRealm.close();
                ((VitalsItemView)holder).valueText.setText((spo2.getValue()+" ").replace(".0 "," ").replace(" ",""));
                ((VitalsItemView)holder).unitText.setText(spo2.getStringUnit());
                break;
        }


    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    private String getTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        String time = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        return time;
    }

    @Override
    public int getItemViewType(int position) {

        return mList.get(position).getType();

        //return TYPE_UNKNOWN;
    }
    public void setOnItemClickListener(ClickListener clickListener) {
        OverviewAdapter3.clickListener = clickListener;
    }

    public interface ClickListener {
        void onItemClick(int position, int type, View v);
        void onPairButtonClick(int position, int type, View v);
        void onMenuItemClick(int position, int type, View v);
        //void onItemLongClick(int position, int type, View v);
    }
}
