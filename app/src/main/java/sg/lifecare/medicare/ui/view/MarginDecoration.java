package sg.lifecare.medicare.ui.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import sg.lifecare.medicare.R;

/**
 * Created by wanping on 9/12/16.
 */

public class MarginDecoration extends RecyclerView.ItemDecoration {
    private int marginTop;
    private int marginBottom;
    private int marginStart;
    private int marginEnd;

    public MarginDecoration(Context context) {
        marginTop = context.getResources().getDimensionPixelSize(R.dimen.item_margin_top);
        marginBottom = context.getResources().getDimensionPixelSize(R.dimen.item_margin_bottom);
        marginStart = context.getResources().getDimensionPixelSize(R.dimen.item_margin_start);
        marginEnd = context.getResources().getDimensionPixelSize(R.dimen.item_margin_end);
    }

    @Override
    public void getItemOffsets(
            Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.set(marginStart, marginTop, marginEnd, marginBottom);
    }
}