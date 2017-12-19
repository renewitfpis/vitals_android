package sg.lifecare.medicare.ui;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.ui.adapter.MeasureAdapter;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.ui.view.CustomToolbar.OnToolbarClickListener;
import timber.log.Timber;

/**
 * Created by wanping on 19/9/16.
 */
public class MeasureActivity extends ListActivity {
    String[] measureTitles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure);
        //setListAdapter(adapter);

        measureTitles = getResources().getStringArray(R.array.measure_titles);
        TypedArray iconsTypedArray = getResources().obtainTypedArray(R.array.measure_icons);

        Drawable[] drawables = new Drawable[iconsTypedArray.length()];
        for(int i = 0; i < iconsTypedArray.length(); i++) {
            drawables[i] = iconsTypedArray.getDrawable(i);
        }
        iconsTypedArray.recycle();
        Timber.d("measuretitles size = " + measureTitles.length + ", drawables size = "+drawables.length);
        MeasureAdapter adapter = new MeasureAdapter(this,R.layout.measure_item, measureTitles, drawables);
        setListAdapter(adapter);
        adapter.notifyDataSetChanged();

        CustomToolbar mToolbar = (CustomToolbar)findViewById(R.id.toolbar);
        mToolbar.setListener(new OnToolbarClickListener() {
            @Override
            public void leftButtonClick() {
                onBackPressed();
            }

            @Override
            public void rightButtonClick() {

            }

            @Override
            public void secondRightButtonClick() {

            }
        });
        mToolbar.setTitle("Measure");
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.hideRightButton();
        mToolbar.hideSecondRightButton();

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        switch(measureTitles[position]){
            case "Blood Glucose":
                Intent intent = new Intent(MeasureActivity.this, GeneralGlucoseReadingActivity.class);
                startActivity(intent);
                finish();
                break;

            case "Blood Pressure":
                Intent intent2 = new Intent(MeasureActivity.this, BloodPressureReadingActivity.class);
                startActivity(intent2);
                finish();
                break;

            case "Weight":
                Intent intent3 = new Intent(MeasureActivity.this, WeightReadingActivity.class);
                startActivity(intent3);
                finish();
                break;

            case "Temperature":
                Intent intent5 = new Intent(MeasureActivity.this, TemperatureReadingActivity.class);
                startActivity(intent5);
                finish();
                break;

            case "SpO2":
                Intent intent4 = new Intent(MeasureActivity.this, SpO2ReadingActivity.class);
                startActivity(intent4);
                finish();
                break;
        }
        /*if(measureTitles[position].equalsIgnoreCase("Blood Glucose")){

        }*/
    }
}
