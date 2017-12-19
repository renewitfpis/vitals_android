package sg.lifecare.medicare.ui.pairing;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.ui.view.CustomToolbar.OnToolbarClickListener;
import sg.lifecare.medicare.ui.view.ViewPagerFixed;
import sg.lifecare.medicare.utils.MedicalDevice;
import sg.lifecare.medicare.utils.MedicalDevice.Model;
import timber.log.Timber;


/**
 * Created by wanping on 13/10/16.
 */

public class DevicePairingMenuActivity extends FragmentActivity {

    private static int REQ_PAIRING_STATUS = 130;

    /**
     * The actual pages that contain device information
     */
    private int NUM_REAL_PAGES;

    /**
     * The number of fake page added at first position & final position
     * for the purpose of circular viewpager scrolling effect
     */
    private int NUM_FAKE_PAGES = 2;

    /**
     * The total number of pages (REAL_PAGES + FAKE_PAGES)
     */
    private int NUM_PAGES;
    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPagerFixed imagePager;

    private ViewPagerFixed infoPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter imagePagerAdapter;
    private PagerAdapter infoPagerAdapter;

    private ArrayList<MedicalDevice> medicalDeviceList = new ArrayList<>();

    private boolean isSpo2, isGlucose, isCustom = false;

    private int startingPosition;

    private List<ImageView> dots;

    public static String IS_SPO2 = "is_spo2";
    public static String IS_GLUCOSE = "is_glucose";
    public static String IS_THERMOMETER = "is_thermometer";
    public static String IS_BLOOD_PRESSURE = "is_blood_pressure";
    public static String IS_SCALE = "is_scale";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_pairing_menu);

        isSpo2 = getIntent().getBooleanExtra(IS_SPO2,false);
        isGlucose = getIntent().getBooleanExtra(IS_GLUCOSE,false);

        boolean isBloodPressure = getIntent().getBooleanExtra(IS_BLOOD_PRESSURE, false);
        boolean isThermometer = getIntent().getBooleanExtra(IS_THERMOMETER, false);
        boolean isScale = getIntent().getBooleanExtra(IS_SCALE, false);

        if(isSpo2){
            medicalDeviceList.add(MedicalDevice.findModel(Model.JUMPER_JPD500E));
            //medicalDeviceList.add(MedicalDevice.findModel(Model.BERRY_BM1000B));
            medicalDeviceList.add(MedicalDevice.findModel(Model.NONIN_3230));
            isCustom = true;
            NUM_FAKE_PAGES = 0;
        } else if(isGlucose){
            medicalDeviceList.add(MedicalDevice.findModel(Model.VIVACHEK_INO_SMART));
            medicalDeviceList.add(MedicalDevice.findModel(Model.TERUMO_MEDISAFE_FIT));
            medicalDeviceList.add(MedicalDevice.findModel(Model.ACCU_CHEK_AVIVA_CONNECT));
            isCustom = true;
            NUM_FAKE_PAGES = 0;
        } else if (isThermometer) {
            medicalDeviceList.add(MedicalDevice.findModel(Model.JUMPER_FR302));
            medicalDeviceList.add(MedicalDevice.findModel(Model.AANDD_UT_201));
            isCustom = true;
            NUM_FAKE_PAGES = 0;
        } else if (isBloodPressure) {
            medicalDeviceList.add(MedicalDevice.findModel(Model.URION_BP_U80E));
            medicalDeviceList.add(MedicalDevice.findModel(Model.AANDD_UA_651));
            isCustom = true;
            NUM_FAKE_PAGES = 0;
        } else if (isScale) {
            medicalDeviceList.add(MedicalDevice.findModel(Model.YOLANDA_LITE));
            medicalDeviceList.add(MedicalDevice.findModel(Model.AANDD_UC_352));
            isCustom = true;
            NUM_FAKE_PAGES = 0;
        }

        else {
            medicalDeviceList = MedicalDevice.sMedicalDeviceList;
        }

        NUM_REAL_PAGES = medicalDeviceList.size();

        NUM_PAGES = NUM_REAL_PAGES + NUM_FAKE_PAGES;

        startingPosition = isCustom ? 0 : 1;

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        Timber.d("SCREEN SIZE = " + size.x + ", " + size.y);
        int padding = size.x / 5;

        addDots();

        imagePager = (ViewPagerFixed) findViewById(R.id.pager);
        imagePagerAdapter = new ImagePagerAdapter(medicalDeviceList);
        imagePager.setAdapter(imagePagerAdapter);
        imagePager.setClipToPadding(false);
        imagePager.setPadding(padding,0,padding,0);
        imagePager.setCurrentItem(startingPosition);
        imagePager.addOnPageChangeListener(onImagePageChangeListener);

        infoPager = (ViewPagerFixed) findViewById(R.id.pager_info);
        infoPagerAdapter = new InfoPagerAdapter(medicalDeviceList);
        infoPager.setAdapter(infoPagerAdapter);
        infoPager.setCurrentItem(startingPosition);
        infoPager.addOnPageChangeListener(onInfoPageChangeListener);

        imagePager.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                infoPager.onTouchEvent(event);
                return false;
            }
        });

        infoPager.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                imagePager.onTouchEvent(event);
                return false;
            }
        });

        Button addDeviceBtn = (Button) findViewById(R.id.button_add_device);
        addDeviceBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos;
                if(!isCustom){
                    if (imagePager.getCurrentItem() == 0) {
                        pos = NUM_REAL_PAGES - 1;
                    } else if (imagePager.getCurrentItem() == NUM_PAGES - 1) {
                        pos = 0;
                    } else {
                        pos = imagePager.getCurrentItem() - startingPosition;
                    }
                }else {
                    pos = imagePager.getCurrentItem() - startingPosition;
                }
                Timber.d("Add device: " + pos);
                Timber.d("Starting pos : " + startingPosition);
                Intent intent = new Intent(DevicePairingMenuActivity.this,ConvertGatewayActivity.class);
                intent.putExtra("medical_device", medicalDeviceList.get(pos));
                startActivityForResult(intent, REQ_PAIRING_STATUS);
            }
        });

        CustomToolbar mToolbar = (CustomToolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle("Devices");
        mToolbar.setLeftButtonImage(R.drawable.ic_toolbar_back);
        mToolbar.hideRightButton();
        mToolbar.hideSecondRightButton();
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
    }

    public void addDots() {
        dots = new ArrayList<>();
        LinearLayout dotsLayout = (LinearLayout) findViewById(R.id.dots);

        for(int i = 0; i < NUM_PAGES; i++) {
            ImageView dot = new ImageView(this);

            if(i!=startingPosition) {
                Timber.d("GREY COLOR");
                dot.setImageDrawable(getResources().getDrawable(R.drawable.circle_grey));
            }else{
                Timber.d("PRIMARY COLOR");
                dot.setImageDrawable(getResources().getDrawable(R.drawable.circle_primary_color));
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(16,16);
            params.setMargins(5,0,5,0);
            dotsLayout.addView(dot, params);

            if(!isCustom) {
                if (i == 0 || i == NUM_PAGES-1){
                    dot.setVisibility(View.INVISIBLE);
                }
            }
            dots.add(dot);
        }
    }

    public void selectDot(int idx) {
        Resources res = getResources();
        for(int i = 0; i < NUM_PAGES; i++) {
            int drawableId = (i==idx)?(R.drawable.circle_primary_color):(R.drawable.circle_grey);
            Drawable drawable = res.getDrawable(drawableId);
            dots.get(i).setImageDrawable(drawable);
        }
    }
    private OnPageChangeListener onImagePageChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

       @Override
        public void onPageSelected(int position) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE)
            {
                if(!isCustom) {
                    int pageCount = NUM_PAGES;

                    int currentItem = imagePager.getCurrentItem();

                    if (currentItem == 0) {
                        imagePager.setCurrentItem(pageCount - 2, false);
                        infoPager.setCurrentItem(pageCount - 2, false);
                    } else if (currentItem == pageCount - 1) {
                        imagePager.setCurrentItem(1, false);
                        infoPager.setCurrentItem(1, false);
                    } else {
                        if (infoPager.getCurrentItem() != imagePager.getCurrentItem()) {
                            infoPager.setCurrentItem(imagePager.getCurrentItem(), false);
                        }
                    }
                }else{
                    if (infoPager.getCurrentItem() != imagePager.getCurrentItem()) {
                        infoPager.setCurrentItem(imagePager.getCurrentItem(), false);
                    }
                }
                selectDot(imagePager.getCurrentItem());
            }
        }
    };

    private OnPageChangeListener onInfoPageChangeListener = new OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {

        }

        @Override public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE)
            {
                if(isCustom) {
                    int pageCount = NUM_PAGES;
                    int currentItem = infoPager.getCurrentItem();

                    Timber.d("onPageScrollStateChanged: current=%d", currentItem);

                    /*if (currentItem == 0) {
                        infoPager.setCurrentItem(pageCount - 2, false);
                        imagePager.setCurrentItem(pageCount - 2, false);
                    } else if (currentItem == pageCount - 1) {
                        infoPager.setCurrentItem(1, false);
                        imagePager.setCurrentItem(1, false);
                    } else {
                        if (infoPager.getCurrentItem() != imagePager.getCurrentItem()) {
                            imagePager.setCurrentItem(infoPager.getCurrentItem(), false);
                        }
                    }*/
                }else{
                    if (infoPager.getCurrentItem() != imagePager.getCurrentItem()) {
                        infoPager.setCurrentItem(imagePager.getCurrentItem(), false);
                    }
                }
                selectDot(infoPager.getCurrentItem());
            }
        }
    };
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQ_PAIRING_STATUS) {
            if(resultCode == Activity.RESULT_OK){
                int status=data.getIntExtra("status",-1);
                if(status==0){
                    finish();
                }
            }
        }
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ImagePagerAdapter extends PagerAdapter {
        private int pageSize = 0;
        private int actualPageSize = 0;
        private ArrayList<MedicalDevice> medicalDeviceList;

        public ImagePagerAdapter(ArrayList<MedicalDevice> medicalDeviceList){
            this.medicalDeviceList = medicalDeviceList;
            this.pageSize = medicalDeviceList.size() + NUM_FAKE_PAGES;
            this.actualPageSize =  medicalDeviceList.size();
        }

        @Override
        public int getCount() {
            return pageSize;
        }

        @Override
        public boolean isViewFromObject(final View view, final Object object) {
            return view.equals(object);
        }

        @Override
        public void destroyItem(final View container, final int position, final Object object) {
            ((ViewPager) container).removeView((View) object);
        }

        @Override
        public Object instantiateItem(final ViewGroup container, final int pagerPosition) {
            final View view = LayoutInflater.from(
                    getBaseContext()).inflate(R.layout.device_pairing_menu_view, null, false);
            container.addView(view);
            int position = pagerPosition;

            if(!isCustom) {
                if (pagerPosition == 0) {
                    position = actualPageSize - 1;
                } else if (pagerPosition == pageSize - 1) {
                    position = 0;
                } else {
                    position = pagerPosition - 1;
                }
            }

            ImageView ivDevice = (ImageView) view.findViewById(R.id.image_device);
            ivDevice.setImageResource(medicalDeviceList.get(position).getImage());

            return view;
        }
    }

    private class InfoPagerAdapter extends PagerAdapter {
        private int pageSize = 0;
        private int actualPageSize = 0;
        private ArrayList<MedicalDevice> medicalDeviceList;

        public InfoPagerAdapter(ArrayList<MedicalDevice> medicalDeviceList){
            this.medicalDeviceList = medicalDeviceList;
            this.actualPageSize = medicalDeviceList.size();
            this.pageSize = medicalDeviceList.size() + NUM_FAKE_PAGES;
        }
        @Override
        public int getCount() {
            return pageSize;
        }


        @Override
        public boolean isViewFromObject(final View view, final Object object) {
            return view.equals(object);
        }

        @Override
        public void destroyItem(final View container, final int position, final Object object) {
            ((ViewPager) container).removeView((View) object);
        }

        @Override
        public Object instantiateItem(final ViewGroup container, final int pagerPosition) {
            final View view = LayoutInflater.from(
                    getBaseContext()).inflate(R.layout.device_pairing_menu_text_view, null, false);
            container.addView(view);
            TextView tvName = (TextView) view.findViewById(R.id.text_name);
            TextView tvDesc = (TextView) view.findViewById(R.id.text_description);
            TextView tvBrand = (TextView) view.findViewById(R.id.text_brand);
            ImageView ivBrand = (ImageView) view.findViewById(R.id.image_brand);

            /*
             *  On the first page and last page (dummy pages),
             *  display the information of the last device & first device respectively
             *  to create circular viewpager scrolling illusion
             */
            int position = pagerPosition;

            if(!isCustom) {
                if (pagerPosition == 0) {
                    position = actualPageSize - 1; //last position of actual pages
                } else if (pagerPosition == pageSize - 1) {
                    position = 0; //first position of actual pages
                } else {
                    position = pagerPosition - 1;
                }
            }

            MedicalDevice medicalDevice = medicalDeviceList.get(position);
            tvName.setText(medicalDevice.getAssignedName());
            tvBrand.setText(MedicalDevice.getBrandName(medicalDevice.getBrand()));
            ivBrand.setImageResource(MedicalDevice.getBrandImage(medicalDevice.getBrand()));
            tvDesc.setText(MedicalDevice.getModelDescription(DevicePairingMenuActivity.this,medicalDevice.getModel()));

            return view;
        }
    }
}
