package sg.lifecare.medicare.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import sg.lifecare.medicare.R;


/**
 * Pairing result view
 */
public class PairingResultView extends RelativeLayout {

    private static final String TAG = "PairingResultView";

    private ImageView mProductImage;
    private ImageView mResultImage;

    public PairingResultView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
        Log.d(TAG, "2");
    }

    public PairingResultView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);

        Log.d(TAG, "3");
    }

    private void initView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.view_medical_device_pairing_result, this);

        mProductImage = (ImageView) view.findViewById(R.id.product_image);
        mResultImage = (ImageView) view.findViewById(R.id.result_image);

    }

    public void setProductImage(int resId) {
        mProductImage.setImageResource(resId);
    }

    public void setResultImage(int resId) {
        mResultImage.setImageResource(resId);
    }
}
