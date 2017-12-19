package sg.lifecare.medicare.utils;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import timber.log.Timber;

/*
    Wrapper for Linear Layout Manager to solve RecyclerView internal bug
    as described in sakiM's answer in
    http://stackoverflow.com/questions/31759171/recyclerview-and-java-lang-indexoutofboundsexception-inconsistency-detected-in

 */
public class WrapContentLinearLayoutManager extends LinearLayoutManager {
    public WrapContentLinearLayoutManager(Context context) {
        super(context);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            super.onLayoutChildren(recycler, state);
        } catch (IndexOutOfBoundsException e) {
            Timber.e("Found a IndexOutOfBoundsException in RecyclerView");
        }
    }
}