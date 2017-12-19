package sg.lifecare.medicare.ui.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageButton;
import codetail.graphics.drawables.LollipopDrawablesCompat;
import sg.lifecare.medicare.R;

/**
 * Created by sweelai on 10/6/16.
 */
public class LifecareImageButton extends ImageButton {

    public LifecareImageButton(Context context) {
        this(context, null);
    }

    public LifecareImageButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LifecareImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setBackground(getDrawable2(context, R.drawable.ripple));
    }


    private Drawable getDrawable2(Context context, int resid){
        return LollipopDrawablesCompat.getDrawable(getResources(), resid, context.getTheme());
    }
}
