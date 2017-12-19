package sg.lifecare.medicare.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.GridView;

/**
 * Signs and symptoms
 */
public class SignsAndSymptomsGridView extends GridView {

    boolean expanded = false;

    public SignsAndSymptomsGridView(Context con) {
        super(con);
    }
    public SignsAndSymptomsGridView(Context con, AttributeSet attrs)
    {
        super(con, attrs);
    }

    public SignsAndSymptomsGridView(Context con, AttributeSet attrs,
                                    int defStyle)
    {
        super(con, attrs, defStyle);
    }

    public boolean isExpanded()
    {
        return expanded;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        if (isExpanded())
        {
            int expandSpec = MeasureSpec.makeMeasureSpec(MEASURED_SIZE_MASK,
                    MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, expandSpec);

            ViewGroup.LayoutParams params = getLayoutParams();
            params.height = getMeasuredHeight();
        }
        else
        {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public void setExpanded(boolean expanded)
    {
        this.expanded = expanded;
    }

}
