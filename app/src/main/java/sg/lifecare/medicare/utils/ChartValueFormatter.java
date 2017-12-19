package sg.lifecare.medicare.utils;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;

import java.text.DecimalFormat;

/**
 * Created by wanping on 26/10/16.
 */
public class ChartValueFormatter implements YAxisValueFormatter {

    private DecimalFormat mFormat;

    public ChartValueFormatter() {
        mFormat = new DecimalFormat("#.#"); // use one decimal
    }

    @Override
    public String getFormattedValue(float value, YAxis yAxis) {
        return mFormat.format(value); // e.g. append a dollar-sign
    }
}