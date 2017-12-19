package sg.lifecare.medicare.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class LinearLayoutThatDetectsSoftKeyboard extends LinearLayout {

    public LinearLayoutThatDetectsSoftKeyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LinearLayoutThatDetectsSoftKeyboard(Context context) {
        super(context);
    }

    private OnSoftKeyboardListener onSoftKeyboardListener;

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        if (onSoftKeyboardListener != null) {
            //final int newSpec = MeasureSpec.getSize(heightMeasureSpec);
            //final int oldSpec = getMeasuredHeight();
            /*if (oldSpec > newSpec){
                onSoftKeyboardListener.onShown();
            } else {
                onSoftKeyboardListener.onHidden();
            }*/
            int height = MeasureSpec.getSize(heightMeasureSpec);
            Activity activity = (Activity)getContext();
            Rect rect = new Rect();
            activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
            int statusBarHeight = rect.top;
            int screenHeight = activity.getWindowManager().getDefaultDisplay().getHeight();
            int diff = (screenHeight - statusBarHeight) - height;
            if(diff>128){
                onSoftKeyboardListener.onShown();
            }else{
                onSoftKeyboardListener.onHidden();
            }

            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public final void setOnSoftKeyboardListener(final OnSoftKeyboardListener listener) {
        this.onSoftKeyboardListener = listener;
    }

    public interface OnSoftKeyboardListener {
        public void onShown();
        public void onHidden();
    }

}