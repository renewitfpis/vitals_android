package sg.lifecare.medicare;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.kitnew.ble.QNApiManager;
import com.kitnew.ble.QNResultCallback;
import com.kitnew.ble.utils.QNLog;

import io.fabric.sdk.android.Fabric;
import io.realm.Realm;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.LifecareSharedPreference;
import timber.log.Timber;

/**
 * MediCare application
 */
public class MediCareApplication extends Application {

    private static Context mContext;
    public static int GENERAL = 0;
    public static int MEDISAFE = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        Realm.init(this);

        mContext = this;

        // default configuration for realm
        //RealmConfiguration realmConfig = new RealmConfiguration.Builder(this).build();
        //Realm.setDefaultConfiguration(realmConfig);

        PatientData.getInstance();

        LifecareSharedPreference.init(this);
        // initialize timber
        Timber.plant(new Timber.DebugTree());


        QNLog.DEBUG = true;

        QNApiManager.getApi(getApplicationContext()).initSDK("123456789", new QNResultCallback() {
            @Override
            public void onCompete(int errorCode) {
                Timber.i( "QNApiManager : " + errorCode);
            }
        });
    }

    @Override
    public void onTerminate(){
        super.onTerminate();
        Timber.d("TERMINATE");


    }

    public static Context getContext() {
        return mContext;
    }

}
