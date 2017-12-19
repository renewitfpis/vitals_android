package sg.lifecare.medicare.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import sg.lifecare.medicare.R;
import timber.log.Timber;

/**
 * Created by janice on 27/6/16.
 */
public class EnlargeImageActivity extends AppCompatActivity {
    private ImageView imageView,crossImage;
    private TextView remarks_text;
    String imageUrl;
    String remarks;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enlarge_image);

        crossImage = (ImageView) findViewById(R.id.cross_icon);
        imageView = (ImageView) findViewById(R.id.image_view);
        remarks_text = (TextView) findViewById(R.id.text_remark);

        crossImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Bundle extra = getIntent().getExtras();
        if(extra != null) {
            imageUrl = extra.getString("Image", "");
            remarks = extra.getString("Remarks");
            if (imageUrl.equals("")) {
                return;
            }
            Timber.d("IMAGE REMARKS = " + remarks);

            Timber.d("IMAGE URL = " + imageUrl);
            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                Picasso.with(this).load(imageUrl).into(imageView);
                Timber.d("LOADED URL");
            } else {
                Picasso.with(this).load("file:///" + imageUrl).into(imageView);
                Timber.d("LOADED LOCAL FILE PATH");
            }
            remarks_text.setText(remarks);
        }
    }


}
