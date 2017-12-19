package sg.lifecare.medicare.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import sg.lifecare.medicare.R;

/**
 * Created by sweelai on 13/6/16.
 */
public class CustomToolbar extends RelativeLayout implements View.OnClickListener{

    public interface OnToolbarClickListener {
        void leftButtonClick();
        void rightButtonClick();
        void secondRightButtonClick();
    }

    private TextView mTitle;
    private ImageView mLeftButton;
    private ImageView mRightButton;
    private ImageView mRightSecondButton;
    private RelativeLayout mDimmer;

    private OnToolbarClickListener mListener;

    public CustomToolbar(Context context) {
        this(context, null);
    }

    public CustomToolbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public CustomToolbar(Context context, AttributeSet attrs, int defStyle) {
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
                break;

            case R.id.button_second_right:
                mListener.secondRightButtonClick();
                break;

            case R.id.toolbar_dimmer:
                if(mRightSecondButton.getVisibility()==GONE){
                    mListener.secondRightButtonClick();
                }

        }
    }


    private void initView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.view_custom_toolbar, this);
        mLeftButton = (ImageView) view.findViewById(R.id.button_left);
        mTitle = (TextView) view.findViewById(R.id.actionbar_title);
        mRightButton = (ImageView) view.findViewById(R.id.button_right);
        mRightSecondButton = (ImageView) view.findViewById(R.id.button_second_right);
        mDimmer = (RelativeLayout) view.findViewById(R.id.toolbar_dimmer);

        mLeftButton.setOnClickListener(this);
        mRightButton.setOnClickListener(this);
        mRightSecondButton.setOnClickListener(this);
        mDimmer.setOnClickListener(this);

    }

    public void setDimmer(boolean dim){
        if(dim){
            mDimmer.setVisibility(VISIBLE);
        }else{
            mDimmer.setVisibility(INVISIBLE);
        }
    }

    public void setTitle(int res) {
        mTitle.setText(res);
    }

    public void setTitle(String res) {
        mTitle.setText(res);
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

    public void showRightButton(){
        mRightButton.setVisibility(View.VISIBLE);
    }

    public void hideSecondRightButton(){
        mRightSecondButton.setVisibility(View.GONE);
    }

    public void setListener(OnToolbarClickListener listener) {
        mListener = listener;
    }

    public View getRightView(){
        return mRightButton;
    }
}
