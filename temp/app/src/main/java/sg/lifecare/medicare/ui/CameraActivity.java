package sg.lifecare.medicare.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.ui.fragment.CameraFragment;
import timber.log.Timber;

/**
 * Camera activity
 */
public class CameraActivity extends AppCompatActivity {

    public static final String TAG = CameraActivity.class.getSimpleName();
    Fragment fragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Timber.tag(TAG);
        //setTheme(R.style.squarecamera__CameraFullScreenTheme);
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_camera);

       fragment = CameraFragment.newInstance();
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container,fragment, CameraFragment.TAG)
                .commit();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CameraFragment.MY_PERMISSIONS_REQUEST_CAM: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    ((CameraFragment)fragment).restartPreview();
                    Log.d(TAG,"Camera Permission granted!");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    Log.d(TAG,"Camera Permission denied!");
                    finish();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void returnPhotoUri(Uri uri) {
        if(uri==null){
            Timber.d("Directory full or not found!");
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setTitle("Memory is nearly full or directory is not found");
            dialog.setCancelable(true);
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, "ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    finish();
                }
            });
            dialog.show();
        }

        Intent data = new Intent();
        data.setData(uri);

        if (getParent() == null) {
            setResult(RESULT_OK, data);
        } else {
            getParent().setResult(RESULT_OK, data);
        }

        finish();
    }

    public void onCancel(View view) {
        getSupportFragmentManager().popBackStack();
    }
}