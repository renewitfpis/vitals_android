package sg.lifecare.medicare.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import sg.lifecare.medicare.R;
import timber.log.Timber;

/**
 * Created by sweelai on 13/6/16.
 */
public class ChartToolbar extends RelativeLayout implements View.OnClickListener{

    public interface OnToolbarClickListener {
        void leftButtonClick();
        void rightButtonClick();
        void secondRightButtonClick();
        void spinnerClick();
    }

    private Spinner mSpinner, mOverflowMenu;
    private ImageView mLeftButton;
    private ImageView mRightButton;
    private ImageView mRightSecondButton;
    private ImageView mDropDown;
    private RelativeLayout mDimmer;
    private TextView mTitle;

    private OnToolbarClickListener mListener;

    public ChartToolbar(Context context) {
        this(context, null);
    }

    public ChartToolbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public ChartToolbar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        //mContext = context;
        initView(context);
    }

    @Override
    public void onClick(View v) {

        if (mListener == null) {
            return;
        }

        switch (v.getId()) {
            case R.id.button_left:
                mListener.leftButtonClick();
                break;

            case R.id.button_right:
                mListener.rightButtonClick();
                mOverflowMenu.performClick();
                break;

            case R.id.button_second_right:
                mListener.secondRightButtonClick();
                break;

            case R.id.toolbar_dimmer:
                if(mRightSecondButton.getVisibility()==GONE){
                    mListener.secondRightButtonClick();
                }
                break;

            case R.id.actionbar_title:
                mListener.spinnerClick();
                break;

            case R.id.drop_down_icon:
                Timber.d("Drop down clicked!");
                mSpinner.performClick();
                mListener.spinnerClick();
                break;

        }
    }


    private void initView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.view_chart_toolbar, this);
        mLeftButton = (ImageView) view.findViewById(R.id.button_left);
        mSpinner = (Spinner) view.findViewById(R.id.actionbar_spinner);
        mTitle = (TextView) view.findViewById(R.id.actionbar_title);
        mOverflowMenu = (Spinner) view.findViewById(R.id.spinner_right);
        mRightButton = (ImageView) view.findViewById(R.id.button_right);
        mRightSecondButton = (ImageView) view.findViewById(R.id.button_second_right);
        mDimmer = (RelativeLayout) view.findViewById(R.id.toolbar_dimmer);
        mDropDown = (ImageView) view.findViewById(R.id.drop_down_icon);

        mLeftButton.setOnClickListener(this);
        mRightButton.setOnClickListener(this);
        mRightSecondButton.setOnClickListener(this);
        mDimmer.setOnClickListener(this);
        mDropDown.setOnClickListener(this);

    }

    public void setDimmer(boolean dim){
        if(dim){
            mDimmer.setVisibility(VISIBLE);
        }else{
            mDimmer.setVisibility(INVISIBLE);
        }
    }

    public void setOverflowMenuAdapter(ArrayAdapter<String> adapter){
        mOverflowMenu.setAdapter(adapter);
    }

    public void setOverflowMenuListener(OnItemSelectedListener listener){
        mOverflowMenu.setOnItemSelectedListener(listener);
    }

    public void setOverflowMenuSelection(int pos){
        mOverflowMenu.setSelection(pos);
    }

    public void setSpinnerAdapter(ArrayAdapter<String> adapter){
        mSpinner.setAdapter(adapter);
    }

    public void setSpinnerListener(OnItemSelectedListener listener){
        mSpinner.setOnItemSelectedListener(listener);
    }

    public void setSpinnerSelection(int pos){
        mSpinner.setSelection(pos);
    }

    public void setTitle(int res) {
        mTitle.setVisibility(VISIBLE);
        mTitle.setText(res);
    }

    public void setTitle(String res) {
        mTitle.setVisibility(VISIBLE);
        mTitle.setText(res);
    }

    public void setSpinnerVisibility(int visibility){
        mSpinner.setVisibility(visibility);
        mDropDown.setVisibility(visibility);
    }

    public void setLeftButtonImage(int res) {
        mLeftButton.setVisibility(View.VISIBLE);
        mLeftButton.setImageResource(res);
    }

    public void setRightButtonImage(int res) {
        mRightButton.setImageResource(res);
        mRightButton.setVisibility(View.VISIBLE);
    }

    public void setSecondRightButtonImage(int res) {
        mRightSecondButton.setImageResource(res);
        mRightSecondButton.setVisibility(View.VISIBLE);
    }

    public void hideRightButton(){
        mRightButton.setVisibility(View.GONE);
    }

    public void hideSecondRightButton(){
        mRightSecondButton.setVisibility(View.GONE);
    }

    public void setListener(OnToolbarClickListener listener) {
        mListener = listener;
    }

}
