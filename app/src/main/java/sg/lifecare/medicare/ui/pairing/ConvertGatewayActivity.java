package sg.lifecare.medicare.ui.pairing;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import org.json.JSONException;
import org.json.JSONObject;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.object.LocalMeasurementDevicesHandler;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.utils.LifeCareHandler;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Brand;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import timber.log.Timber;

/**
 * Created by ct on 13/1/16.
 */
public class ConvertGatewayActivity extends FragmentActivity
        implements SelectBrandFragment.OnBrandSelected,
        SelectDeviceTypeFragment.OnDeviceSelected,
        AddDeviceFragment.OnPairingDetected,
        PairingIndicatorFragment.OnButtonSelected
{
    private final String TAG = "ConvertGatewayActivity";

    // page
    private static final int PAGE_CONVERT = 0;
    private static final int PAGE_BRAND = 1;
    private static final int PAGE_TYPE = 2;
    private static final int PAGE_ADD = 3;
    private static final int PAGE_RESULT = 4;
    private static final int PAGE_GET = 5;

    public static int STATUS_PAIRED = 0;
    public static int STATUS_CANCELED = 1;

    private RelativeLayout loadingView;
    private ProgressBar loading;

    private RelativeLayout convertView;
    private Button convertButton;

    public static Boolean isDescPage = null;
    private boolean isGlucosePage = true;

    private CustomToolbar mToolbar;

    private Brand brand;
    private Model device;
    private String pairedDeviceId;
    private String assignedDeviceName;

    private int mPage;
    private FragmentManager mFragmentManager;

    private static Boolean isGateway = null;
    /*
    *   (iOS) Device ID to be entity + UUID
        (Android) Device ID to be entity + phone MAC + device MAC
    * */

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Timber.d("ON CREATE CONVERT GATEWAY");
        setContentView(R.layout.convert_gateway_desc_view);

        loadingView = (RelativeLayout) findViewById(R.id.loading_view);
        loading = (ProgressBar) findViewById(R.id.loading);
        loading.getIndeterminateDrawable().setColorFilter(
                ContextCompat.getColor(this,R.color.progress_bar),
                android.graphics.PorterDuff.Mode.SRC_IN
        );

        mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_activity_measurement_reading);
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.hideRightButton();
        mToolbar.hideSecondRightButton();
        mToolbar.setListener(mToolbarListener);

        convertView = (RelativeLayout) findViewById(R.id.desc_view);
        convertButton = (Button) findViewById(R.id.convert_button);
        convertButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                convertToGateway task = new convertToGateway();
                task.execute();
            }
        });

        mFragmentManager = getSupportFragmentManager();

        isGlucosePage = getIntent().getBooleanExtra("is_glucose",false);

        convertView.setVisibility(View.GONE);

        MedicalDevice medicalDevice = null;
        Bundle bundle = getIntent().getExtras();
        if(bundle!=null) {
            medicalDevice = (MedicalDevice)bundle.getSerializable("medical_device");
        }
        if(medicalDevice!=null){
            brand = medicalDevice.getBrand();
            device = medicalDevice.getModel();
            setPage(PAGE_TYPE);
            nextPage();
            Timber.d("RECEIVED BRAND = " + medicalDevice.getBrand().name() + " , " +
                medicalDevice.getModel());
        }else {
            startPage();
        }
        /*
        if(isDescPage==null) {
            Timber.w("check is gateway");
            startPage();
            //new CheckIsGateway().execute();
        }else if(isDescPage){
            Timber.w("convert to gateway");
            new convertToGateway().execute();
        }else{
            startPage();
        }*/
        Timber.w("deviceId=" + LifeCareHandler.getInstance().getMyDeviceID(ConvertGatewayActivity.this));
    }

    private void startPage(){
        isDescPage = false; //TODO remove this line
        if(!isDescPage){
            if(isGlucosePage){
                showGlucosePairingPage();
            } else {
                initPage();
            }
        }
        else{
            Timber.w("DEVICE IS NOT CONVERTED TO GATEWAY YET");
            new convertToGateway().execute();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {

        if (mPage == PAGE_ADD) {
            if (brand == Brand.TERUMO) {
                AddTerumoDeviceFragment fragment = getAddTerumoDeviceFragment();
                fragment.resolveIntent(intent);
            }
        }
    }

    private void showGlucosePairingPage(){
        this.brand = Brand.TERUMO;
        this.device = Model.TERUMO_MEDISAFE_FIT;
        setPage(PAGE_ADD);
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        Fragment frag = new AddTerumoDeviceFragment();
        ft.add(R.id.convert_content, frag, "AddDevice");
        ft.show(frag);
        ft.commitAllowingStateLoss();
    }

    private void initPage() {
        setPage(PAGE_BRAND);
        try {
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            Fragment frag = getSelectBrandFragment();
            ft.add(R.id.convert_content, frag, "SelectBrand");
            ft.show(frag);
            ft.commitAllowingStateLoss();
        }
        catch(IllegalStateException e){
            Timber.e(e.getMessage());
        }
    }

    private void setPage(int page) {
        mPage = page;

        switch (mPage) {
            case PAGE_CONVERT:
                mToolbar.setTitle("Convert to Gateway");
                break;

            case PAGE_BRAND:
                mToolbar.setTitle("Select Brand");
                break;

            case PAGE_TYPE:
                mToolbar.setTitle("Select Device");
                break;

            case PAGE_ADD:
                if(isGlucosePage){
                    mToolbar.setTitle("Add Glucometer");
                }
                else {
                    mToolbar.setTitle("Add Device");
                }
                break;

            case PAGE_RESULT:
                if (pairedDeviceId == null) {
                    mToolbar.setTitle("Device Adding Fail");
                } else {
                    mToolbar.setTitle("Device Adding Success");
                }
                break;

            case PAGE_GET:
                mToolbar.setTitle("Get Device Reading");
                break;
        }
    }

    private void nextPage() {
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        SelectDeviceTypeFragment selectDeviceTypeFrag;
        SelectBrandFragment selectBrandFragment;
        Fragment addDeviceFragment = null;
        Fragment pairingIndicatorFragment;
        Fragment getReadingFragment;
        Timber.d("Next page = " + mPage);

        switch (mPage) {
            case PAGE_BRAND:
                setPage(PAGE_TYPE);
                selectDeviceTypeFrag = getSelectDeviceTypeFragment();
                selectBrandFragment = getSelectBrandFragment();
                if (selectDeviceTypeFrag.isAdded()) {
                    selectDeviceTypeFrag.updateDeviceList();
                } else {
                    ft.add(R.id.convert_content, selectDeviceTypeFrag, "SelectDeviceType");
                }
                //ft.addToBackStack(null);
                ft.hide(selectBrandFragment);
                ft.show(selectDeviceTypeFrag);
                break;

            case PAGE_TYPE:
                setPage(PAGE_ADD);
                if (brand == Brand.TERUMO) {
                    addDeviceFragment = getAddTerumoDeviceFragment();
                } else if (brand == Brand.TAIDOC) {
                    //addDeviceFragment = getAddTaiDocDeviceFragment();
                } else if (brand == Brand.AANDD) {
                    Timber.d("Selected adddevicefrag");
                    addDeviceFragment = getAAndDDeviceFragment();
                } else if (brand == Brand.FORACARE) {
                    //addDeviceFragment = getAddForaCareDeviceFragment();
                } else if (brand == Brand.BERRY){
                    addDeviceFragment = getDeviceFragment();
                } else if (brand == Brand.NONIN){
                    addDeviceFragment = getDeviceFragment();
                } else if (brand == Brand.ZENCRO){
                    addDeviceFragment = getDeviceFragment();
                } else if (brand == Brand.ACCU_CHEK){
                    addDeviceFragment = getAccuChekDeviceFragment();
                } else if (brand == Brand.JUMPER) {
                    addDeviceFragment = getDeviceFragment();
                } else if (brand == Brand.URION) {
                    addDeviceFragment = getDeviceFragment();
                } else if (brand == Brand.YOLANDA) {
                    addDeviceFragment = getDeviceFragment();
                } else if (brand == Brand.VIVACHEK) {
                    addDeviceFragment = getDeviceFragment();
                }

                if (addDeviceFragment != null) {
                    selectDeviceTypeFrag = getSelectDeviceTypeFragment();

                    ft.add(R.id.convert_content, addDeviceFragment, "AddDevice");
                    ft.hide(selectDeviceTypeFrag);
                    ft.show(addDeviceFragment);
                    //ft.addToBackStack(null);
                }
                break;

            case PAGE_ADD:
                setPage(PAGE_RESULT);
                pairingIndicatorFragment = getPairingIndicatorFragment();
                addDeviceFragment = mFragmentManager.findFragmentByTag("AddDevice");
                ft.remove(addDeviceFragment);
                ft.add(R.id.convert_content, pairingIndicatorFragment, "PairingIndicator");
                ft.show(pairingIndicatorFragment);
                //ft.addToBackStack(null);
                break;


            case PAGE_RESULT:
               /* setPage(PAGE_GET);
                getReadingFragment = getGetReadingFragment();
                pairingIndicatorFragment = mFragmentManager.findFragmentByTag("PairingIndicator");
                ft.remove(pairingIndicatorFragment);
                ft.add(R.id.convert_content, getReadingFragment, "GetReading");
                ft.show(getReadingFragment);*/
                break;
        }

        if(!isFinishing()) {
            ft.commitAllowingStateLoss();
        }
    }

    @Override
    public void onBackPressed(){
        if(isGlucosePage||mPage == PAGE_ADD||mPage == PAGE_BRAND||mPage==PAGE_TYPE) {
            super.onBackPressed();
        }
        else {
            previousPage();
        }
    }

    private void previousPage() {
        if(isGlucosePage) {
            onBackPressed();
            return;
        }
        SelectDeviceTypeFragment selectDeviceTypeFragment;
        SelectBrandFragment selectBrandFragment;
        Fragment addDeviceFragment;
        Fragment pairingIndicatorFragment;
        FragmentTransaction ft = mFragmentManager.beginTransaction();

        switch (mPage) {
            case PAGE_BRAND:
                if(isDescPage)
                {
                    convertView.setVisibility(View.VISIBLE);
                    //TODO backImage.setOnClickListener(finishBack);

                    //mToolbar.setTitle("Convert to Gateway");
                    setPage(PAGE_CONVERT);
                }
                else
                {
                    onBackPressed();
                }
                break;

            case PAGE_TYPE:
                setPage(PAGE_BRAND);
                selectBrandFragment = getSelectBrandFragment();
                selectDeviceTypeFragment = getSelectDeviceTypeFragment();
                ft.show(selectBrandFragment);
                ft.hide(selectDeviceTypeFragment);
                break;

            case PAGE_ADD:
                setPage(PAGE_TYPE);
                selectDeviceTypeFragment = getSelectDeviceTypeFragment();
                addDeviceFragment = mFragmentManager.findFragmentByTag("AddDevice");
                ft.remove(addDeviceFragment);
                ft.show(selectDeviceTypeFragment);
                break;

            case PAGE_RESULT:
                setPage(PAGE_ADD);
                addDeviceFragment = mFragmentManager.findFragmentByTag("AddDevice");
                SelectDeviceTypeFragment selectDeviceTypeFrag;
                if (brand == Brand.TERUMO) {
                    addDeviceFragment = getAddTerumoDeviceFragment();
                } else if (brand == Brand.TAIDOC) {
                    //addDeviceFragment = getAddTaiDocDeviceFragment();
                } else if (brand == Brand.AANDD) {
                    Timber.d("Selected adddevicefrag");
                    addDeviceFragment = getAAndDDeviceFragment();
                } else if (brand == Brand.FORACARE) {
                    //addDeviceFragment = getAddForaCareDeviceFragment();
                } else if (brand == Brand.BERRY){
                    addDeviceFragment = getDeviceFragment();
                } else if (brand == Brand.NONIN){
                    addDeviceFragment = getDeviceFragment();
                } else if (brand == Brand.ZENCRO){
                    addDeviceFragment = getDeviceFragment();
                } else if (brand == Brand.ACCU_CHEK){
                    addDeviceFragment = getAccuChekDeviceFragment();
                } else if (brand == Brand.JUMPER) {
                    addDeviceFragment = getDeviceFragment();
                } else if (brand == Brand.URION) {
                    addDeviceFragment = getDeviceFragment();
                } else if (brand == Brand.YOLANDA) {
                    addDeviceFragment = getDeviceFragment();
                } else if (brand == Brand.VIVACHEK) {
                    addDeviceFragment = getDeviceFragment();
                }

                if (addDeviceFragment != null) {
                    selectDeviceTypeFrag = getSelectDeviceTypeFragment();
                    ft.add(R.id.convert_content, addDeviceFragment, "AddDevice");
                    ft.hide(selectDeviceTypeFrag);
                    ft.remove(getPairingIndicatorFragment());
                    ft.show(addDeviceFragment);
                    //ft.addToBackStack(null);
                }
                break;
        }

        ft.commit();
    }

    private SelectBrandFragment getSelectBrandFragment() {
        SelectBrandFragment frag = (SelectBrandFragment)mFragmentManager.findFragmentByTag("SelectBrand");
        if (frag == null) {
            frag = SelectBrandFragment.newInstance();
        }

        return frag;
    }

    private SelectDeviceTypeFragment getSelectDeviceTypeFragment() {
        SelectDeviceTypeFragment frag = (SelectDeviceTypeFragment)mFragmentManager.findFragmentByTag("SelectDeviceType");
        if (frag == null) {
            frag = SelectDeviceTypeFragment.newInstance();
        }

        return frag;
    }

    private GetReadingFragment getGetReadingFragment() {
        GetReadingFragment frag = (GetReadingFragment)mFragmentManager.findFragmentByTag("GetReading");
        if (frag == null) {
            frag = GetReadingFragment.newInstance();
        }

        return frag;
    }

    private AddTerumoDeviceFragment getAddTerumoDeviceFragment() {
        AddTerumoDeviceFragment frag = (AddTerumoDeviceFragment)mFragmentManager.findFragmentByTag("AddDevice");
        if (frag == null) {
            frag = AddTerumoDeviceFragment.newInstance();
        }

        return frag;
    }

   /* private AddTaiDocDeviceFragment getAddTaiDocDeviceFragment() {
        AddTaiDocDeviceFragment frag = (AddTaiDocDeviceFragment)mFragmentManager.findFragmentByTag("AddDevice");
        if (frag == null) {
            frag = AddTaiDocDeviceFragment.newInstance();
        }

        return frag;
    }

    private AddForaCareDeviceFragment getAddForaCareDeviceFragment() {
        AddForaCareDeviceFragment frag = (AddForaCareDeviceFragment)mFragmentManager.findFragmentByTag("AddDevice");
        if (frag == null) {
            frag = AddForaCareDeviceFragment.newInstance();
        }

        return frag;
    }*/

    private AddAAndDDeviceFragment getAAndDDeviceFragment() {
        AddAAndDDeviceFragment frag = (AddAAndDDeviceFragment)mFragmentManager.findFragmentByTag("AddDevice");
        if (frag == null) {
            frag = AddAAndDDeviceFragment.newInstance();
        }

        return frag;
    }

    private AddGeneralDeviceFragment getDeviceFragment() {
        AddGeneralDeviceFragment frag = (AddGeneralDeviceFragment)mFragmentManager.findFragmentByTag("AddDevice");
        if (frag == null) {
            frag = AddGeneralDeviceFragment.newInstance();
        }

        return frag;
    }

    private AddAccuChekDeviceFragment getAccuChekDeviceFragment() {
        AddAccuChekDeviceFragment frag = (AddAccuChekDeviceFragment)mFragmentManager.findFragmentByTag("AddDevice");
        if (frag == null) {
            frag = AddAccuChekDeviceFragment.newInstance();
        }

        return frag;
    }

    private PairingIndicatorFragment getPairingIndicatorFragment() {
        PairingIndicatorFragment frag = (PairingIndicatorFragment)mFragmentManager.findFragmentByTag("PairingIndicator");
        if (frag == null) {
            frag = PairingIndicatorFragment.newInstance();
        }

        return frag;
    }

    public Brand getBrand() {
        return brand;
    }

    public Model getDevice() {
        return device;
    }

    public String getPairedDeviceId() {
        return pairedDeviceId;
    }

    public String getDeviceName() {
        return MedicalDevice.getModelName(this, device);
    }

    View.OnClickListener finishBack = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            finish();
        }
    };

    View.OnClickListener stepBack = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            previousPage();
        }
    };

    private class convertToGateway extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {
            Timber.w("start converting to gateway");
            String deviceId = LifeCareHandler.getInstance().getMyDeviceID(ConvertGatewayActivity.this);
            SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
            String entityId = sh.getString("entity_id", "");
            Timber.w("device id = " + deviceId + ", entity id = " + entityId);

            String result = LifeCareHandler.getInstance().convertSmartphoneToGateway(deviceId, entityId);

            try {
                JSONObject json = new JSONObject(result);

                if(json.has("Data")){
                    String data = json.getString("Data");

                    if(data.equalsIgnoreCase("Converted")){
                        isDescPage = false;
                    }
                    else{
                        isDescPage = true;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Timber.w("result convert :" + result);

            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            isDescPage = false;
            startPage();
        }
    }

    private class CheckIsGateway extends AsyncTask<Void, Void, Void>
    {

        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {
            String deviceId = LifeCareHandler.getInstance().getMyDeviceID(ConvertGatewayActivity.this);
            isDescPage = !LifeCareHandler.getInstance().getGatewayStatus(deviceId);

            return null;
        }

        @Override
        public void onPostExecute(Void result)
        {
            if(isDescPage)
                new convertToGateway().execute();
            else
                startPage();
        }
    }

    private class commissionBLEDevice extends AsyncTask<Void, Void, Void>
    {
        String entityId;
        @Override
        protected void onPreExecute()
        {
            loadingView.setVisibility(View.VISIBLE);
            loading.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {

            Timber.e("posting - " + device);
            String gatewayId =  LifeCareHandler.getInstance().getMyDeviceID(ConvertGatewayActivity.this);
            SharedPreferences sh = getSharedPreferences("lifecare_pref", Context.MODE_PRIVATE);
            entityId = sh.getString("entity_id", "");
            String serial = entityId + "-" + gatewayId + "-" + pairedDeviceId.replace(":","");
            Timber.e("paired device id - " + pairedDeviceId);

            LocalMeasurementDevicesHandler.getInstance().retrieveCurrentLocalDevices(ConvertGatewayActivity.this,entityId);

            //MedicalDevice dummy = MedicalDevice.findModel(device);
            MedicalDevice medicalDevice = null;
            try {
                medicalDevice = (MedicalDevice)MedicalDevice.findModel(device).clone();
                //medicalDevice = new MedicalDevice(dummy.getModel(),dummy.getManufacturerCode(),dummy.getProductTypeCode(),
                //        dummy.getProductCode(),dummy.getProductId(),dummy.getMode(),dummy.getAssignedName());
                medicalDevice.setAssignedName(assignedDeviceName);
                medicalDevice.setDeviceId(pairedDeviceId);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }

            JSONObject data = new JSONObject();

            if (medicalDevice == null) {
                Timber.e("cannot find medical device " + device);
                return null;
            }

            try
            {
                data.put("ProductId", medicalDevice.getProductId());
                data.put("WConfigCode", medicalDevice.getMode());
                data.put("GatewayId", gatewayId);
                data.put("Zone", "56398aa0e4b00e308ce460ec");
                data.put("ZoneCode", "OT");
                data.put("Name", assignedDeviceName);
                data.put("EntityId", entityId);
                data.put("Serial", serial);
            }
            catch(JSONException e)
            {
                e.printStackTrace();
            }

            Timber.w("urlParameters: " + data.toString());
            Timber.e("paired device id 2222 - " + medicalDevice.getDeviceId());

            LocalMeasurementDevicesHandler.getInstance().storeMedicalDevice(medicalDevice);
            String res = LifeCareHandler.getInstance().pairSmartDevice(data);
            Timber.w("res:" + res);

            return null;
        }

        @Override
        public void onPostExecute(Void result)
        {

            LocalMeasurementDevicesHandler.getInstance().retrieveCurrentLocalDevices(ConvertGatewayActivity.this,entityId);
            loadingView.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);

            Intent intent = new Intent();
            intent.putExtra("status",STATUS_PAIRED);
            setResult(RESULT_OK,intent);
            switchToMainActivity();
        }
    }

    @Override
    public void onBrandSelected(Brand brand)
    {
        this.brand = brand;
        nextPage();
    }

    @Override
    public void onDeviceSelected(Model device)
    {
        this.device = device;
        nextPage();
    }

    @Override
    public void onPairSuccess(String devId)
    {
        pairedDeviceId = devId;
        Timber.d("PAIR DEVICE ID = " + devId);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                nextPage();
            }
        });
    }

    @Override
    public void onPairFailed()
    {
        pairedDeviceId = null;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                nextPage();
            }
        });
    }

    @Override
    public void onCancelSelected()
    {
        previousPage();
    }

    @Override
    public void onFinishSelected(String name)
    {

        Timber.d("On Detection Finished");
        assignedDeviceName = name;

        commissionBLEDevice task = new commissionBLEDevice();
        task.execute();
    }

    @Override
    public void onTryAgainSelected()
    {
        setPage(PAGE_RESULT);
        previousPage();

    }

    @Override
    public void onDeviceListingSelected()
    {
        switchToMainActivity();
    }

    private void switchToMainActivity()
    {
        Timber.d("Switched to Main Act");
       /* if(!isGlucosePage) {
            Intent intent = new Intent(ConvertGatewayActivity.this, MeasurementDeviceListActivity.class);
            intent.putExtra("refresh", true);
            startActivity(intent);
        }*/
        finish();
    }

    private CustomToolbar.OnToolbarClickListener mToolbarListener = new CustomToolbar.OnToolbarClickListener() {
        @Override
        public void leftButtonClick() {
            onBackPressed();
        }

        @Override public void rightButtonClick() {
        }

        @Override public void secondRightButtonClick() {
        }
    };
}