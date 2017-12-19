package sg.lifecare.medicare.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.provider.MediaStore.Images.Media;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Locale;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Medication;
import sg.lifecare.medicare.database.model.Photo;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.database.model.Weight;
import sg.lifecare.medicare.ui.ChartActivity;
import timber.log.Timber;

/**
 * Display the value on chart
 */
public class ValueMarkerView extends MarkerView {

    private String TAG = "ValueMarkerView";

    private Context context;

    private final int TERUMO = 0, PHOTO = 1, MEDIC = 2, BP = 3, WEIGHT = 4;

    private final String[] markerTimeFormat = new String[]{
            "hh:mm a", "dd MMM, hh:mm a","dd MMM, hh:mm a"
    };

    private RelativeLayout[] viewLayouts;

    //terumo
    private TextView mTextValue;
    private TextView mTextTime;
    private TextView mTextUnit;

    //photo
    private ImageView imageviewPhoto;
    private TextView textPhotoTime;

    //medication
    private TextView textMedicTime;
    private TextView textMedicType;
    private TextView textMedicDosage;
    private TextView textMedicUnit;

    //bp
    private TextView textBpTime;
    private TextView textBpValue;
    private TextView textBpUnit;

    //weight
    private TextView textWeightTime;
    private TextView textWeightValue;
    private TextView textWeightUnit;

    public ValueMarkerView(Context context) {
        super(context, R.layout.marker_view);
        this.context = context;

        viewLayouts = new RelativeLayout[]{
                (RelativeLayout) findViewById(R.id.terumo_content),
                (RelativeLayout) findViewById(R.id.photo_content),
                (RelativeLayout) findViewById(R.id.medication_content),
                (RelativeLayout) findViewById(R.id.bp_content),
                (RelativeLayout) findViewById(R.id.weight_content)
        };

        mTextValue = (TextView) findViewById(R.id.text_value);
        mTextTime = (TextView) findViewById(R.id.text_time);
        mTextUnit = (TextView) findViewById(R.id.text_unit);

        textMedicTime = (TextView) findViewById(R.id.medication_time);
        textMedicDosage = (TextView) findViewById(R.id.medication_dosage);
        textMedicUnit = (TextView) findViewById(R.id.medication_unit);
        textMedicType = (TextView) findViewById(R.id.medication_type);

        textPhotoTime = (TextView) findViewById(R.id.photo_time);
        imageviewPhoto = (ImageView) findViewById(R.id.food_image);

        textBpTime = (TextView) findViewById(R.id.bp_time);
        textBpValue = (TextView) findViewById(R.id.bp_value);
        textBpUnit = (TextView) findViewById(R.id.bp_unit);

        textWeightTime = (TextView) findViewById(R.id.weight_time);
        textWeightValue = (TextView) findViewById(R.id.weight_value);
        textWeightUnit = (TextView) findViewById(R.id.weight_unit);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
        if(context instanceof ChartActivity) {
            int mode = ((ChartActivity) context).currentMode;
            sdf = new SimpleDateFormat(markerTimeFormat[mode], Locale.ENGLISH);
        }

        if(e.getData() instanceof Terumo) {
            showView(TERUMO);

            Terumo terumo = (Terumo) e.getData();
            String time = sdf.format(terumo.getDate());

            mTextValue.setText(String.valueOf(e.getVal()));
            mTextTime.setText(time);
            mTextUnit.setText(terumo.getStringUnit());
        }
        else if(e.getData() instanceof Photo){
            showView(PHOTO);

            final Photo photo = (Photo) e.getData();
            final String imageURI = photo.getImage();
            //Picasso.with(context).load("file:///" + imageURI).into(imageviewPhoto);
            if(photo.isUrl()){
                Bitmap bitmap = getBitmapFromURL(imageURI);
                imageviewPhoto.setImageBitmap(bitmap);
                Timber.d("IS URL");
            }
            else {
                Timber.d("NOT URL - LOCAL PATH");
                try {
                    Bitmap bitmap = Media.getBitmap(context.getContentResolver(),
                            Uri.parse("file:///" + imageURI));
                    imageviewPhoto.setImageBitmap(bitmap);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            textPhotoTime.setText(sdf.format(photo.getDate()));
        }
        else if(e.getData() instanceof Medication){
            showView(MEDIC);

            Medication medication = (Medication) e.getData();
            String time = sdf.format(medication.getDate());

            textMedicTime.setText(time);
            textMedicDosage.setText(String.valueOf(medication.getDosage()).replace(".0",""));
            textMedicUnit.setText(medication.getStringUnit());
            textMedicType.setText(medication.getStringType());
        }
        else if(e.getData() instanceof BloodPressure){
            showView(BP);

            BloodPressure bp = (BloodPressure) e.getData();
            String time = sdf.format(bp.getDate());
            String value = (bp.getSystolic()+"/"+bp.getDistolic()).replace(".0","");

            textBpTime.setText(time);
            textBpValue.setText(value);
            textBpUnit.setText(bp.getStringUnit());
        }
        else if(e.getData() instanceof Weight){
            showView(WEIGHT);

            Weight weight = (Weight) e.getData();
            String time = sdf.format(weight.getDate());

            textWeightTime.setText(time);
            textWeightValue.setText(String.valueOf(weight.getWeight()));
            textWeightUnit.setText(weight.getStringUnit());
        }

    }

    private void showView(int type){
        for (RelativeLayout viewLayout : viewLayouts)
            viewLayout.setVisibility(GONE);

        viewLayouts[type].setVisibility(VISIBLE);
    }

    /*@Override
    public int getXOffset(float xpos) {

        int offset = getWidth() / 2;
        if (offset > (int)xpos) {
            offset = (int)xpos;
        }
        return -offset;
    }

    @Override
    public int getYOffset(float ypos) {
        return -(getHeight()/2);
    }*/


    @Override
    public int getXOffset(float xpos) {
        // this will center the marker-view horizontally
        //return -(getWidth() / 2);
        // this will center the marker-view horizontally

        //Log.d("main","xpos = " + xpos);
        if(xpos < 130)
            return 0;

        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        if(metrics.widthPixels - xpos < 170)
            return -getWidth();
        return -(getWidth() / 2);
    }

    @Override
    public int getYOffset(float ypos) {
        //Log.d("main", "ypos = " +ypos +" , -getHeight() = " + -getHeight() );
        // this will cause the marker-view to be above the selected value
        // return -getHeight();

        /*
        if(ypos > getHeight()) {
            if(ypos > 840)
                return -getHeight();
            else
                return 0;

        }
        else {
            return -50;
        }*/

        return -getHeight();
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            // Log exception
            return null;
        }
    }

    @Override
    public void draw(Canvas canvas, float posx, float posy) {

        // take offsets into consideration
        posx += getXOffset(posx);
        posy += getYOffset(posy);

        // translate to the correct position and draw
        canvas.translate(posx, posy);
        draw(canvas);
        canvas.translate(-posx, -posy);
    }
}
