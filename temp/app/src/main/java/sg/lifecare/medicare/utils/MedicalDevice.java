package sg.lifecare.medicare.utils;

import android.content.Context;
import android.text.TextUtils;

import java.io.Serializable;
import java.util.ArrayList;

import sg.lifecare.medicare.R;

/**
 * Medical devices class
 */
public class MedicalDevice implements Serializable, Cloneable {
    //TODO: add thermometer product code

    public static final String MANUFACTURER_CODE_TAIDOC = "544149";
    public static final String MANUFACTURER_CODE_TERUMO = "544552";
    public static final String MANUFACTURER_CODE_AAND = "414e44";
    public static final String MANUFACTURER_CODE_FORACARE = "544149";
    public static final String MANUFACTURER_CODE_BERRY = "424d44";
    public static final String MANUFACTURER_CODE_NONIN = "4e4f4e";
    public static final String MANUFACTURER_CODE_ZENCRO = "906967";
    public static final String MANUFACTURER_CODE_ACCU_CHEK = "464852";
    public static final String MANUFACTURER_CODE_JUMPER = "Temp";
    public static final String MANUFACTURER_CODE_URION = "Urion";
    public static final String MANUFACTURER_CODE_YOLANDA = "Yolanda";
    public static final String MANUFACTURER_CODE_VIVACHEK = "VivaChek";

    public static final String PRODUCT_TYPE_CODE_TAIDOC_TD1261 = "43";
    public static final String PRODUCT_TYPE_CODE_TERUMO_MEDISAFE_FIT = "4d534652";
    public static final String PRODUCT_TYPE_CODE_AAND_UC352 = "5543";
    public static final String PRODUCT_TYPE_CODE_AAND_UA651 = "5541";
    public static final String PRODUCT_TYPE_CODE_AAND_UT201 = "5599"; //TODO
    public static final String PRODUCT_TYPE_CODE_FORACARE_D40 = "42";
    public static final String PRODUCT_TYPE_CODE_BERRY_BM1000B = "424d";
    public static final String PRODUCT_TYPE_CODE_NONIN_3230 = "42";
    public static final String PRODUCT_TYPE_CODE_ZENCRO_X6 = "806877";
    public static final String PRODUCT_TYPE_CODE_ACCU_CHEK_AC = "636f6e6e656374";
    public static final String PRODUCT_TYPE_CODE_JUMPER_FR302 = "Temp";
    public static final String PRODUCT_TYPE_CODE_JUMPER_JPD500E = "Temp";
    public static final String PRODUCT_TYPE_CODE_URION_BP = "UrionBP";
    public static final String PRODUCT_TYPE_CODE_YOLANDA_BW = "YolandaBW";
    public static final String PRODUCT_TYPE_CODE_VIVACHEK_BG = "VivaChekBG";

    public static final String PRODUCT_CODE_TAIDOC_TD1261 = "31323631";
    public static final String PRODUCT_CODE_TERUMO_MEDISAFE_FIT = "3230314241";
    public static final String PRODUCT_CODE_AANDD_UC352 = "333532";
    public static final String PRODUCT_CODE_AANDD_UA651 = "363531";
    public static final String PRODUCT_CODE_AANDD_UT201 = "363530"; //TODO
    public static final String PRODUCT_CODE_FORACARE_D40 = "33323631";
    public static final String PRODUCT_CODE_BERRY_BM1000B = "3130303043";
    public static final String PRODUCT_CODE_NONIN_3230 = "33323330";
    public static final String PRODUCT_CODE_ZENCRO_X6 = "49494851";
    public static final String PRODUCT_CODE_ACCU_CHEK_AC = "6176697661";
    public static final String PRODUCT_CODE_JUMPER_FR302 = "Temp";
    public static final String PRODUCT_CODE_JUMPER_JPD500E = "Temp1";
    public static final String PRODUCT_CODE_URION_BP = "UrionBPU80E";
    public static final String PRODUCT_CODE_YOLANDA_BW = "YolandaBWLite";
    public static final String PRODUCT_CODE_VIVACHEK_BG = "VivaChekBGInoSmart";

    public static final String PRODUCT_ID_TAIDOC_TD1261 = "564eb8b392b59eeb00cf8a49";
    public static final String PRODUCT_ID_TERUMO_MEDISAFE_FIT = "5695b84046749cea001c59fa";
    public static final String PRODUCT_ID_AANDD_UC352 = "565ffa42613668e70093c5c9";
    public static final String PRODUCT_ID_AANDD_UA651 = "565ffa1d613668e70093c5c6";
    public static final String PRODUCT_ID_AANDD_UT201 = "57da1d8eb004a94d9defa532";
    public static final String PRODUCT_ID_FORACARE_D40 = "564eb7ba92b59eeb00cf8a42";
    public static final String PRODUCT_ID_BERRY_BM1000B = "5798e3df9cdc0af600027df9";
    public static final String PRODUCT_ID_NONIN_3230 = "57da26ffb004a94d9defa5ff";
    public static final String PRODUCT_ID_ZENCRO_X6 = "56deaa763ccbb6eb004fc612";
    public static final String PRODUCT_ID_ACCU_CHEK_AC = "5829216b4484db226c6c2680";
    public static final String PRODUCT_ID_JUMPER_FR302 = "Temp";
    public static final String PRODUCT_ID_JUMPER_JPD500E = "Temp1";
    public static final String PRODUCT_ID_URION_BP_U80E = "UrionBPU80E";
    public static final String PRODUCT_ID_YOLANDA_BW_LITE = "YolandaBWLite";
    public static final String PRODUCT_ID_VIVACHEK_BG_INOSMART = "VivaChekBGInoSmart";

    public static final String WCONFIG_CODE_BLE = "BLE";
    public static final String WCONFIG_CODE_NFC = "NFC";

    public enum Brand {
        UNKNOWN,
        TERUMO,
        FORACARE,
        AANDD,
        TAIDOC,
        BERRY,
        NONIN,
        ZENCRO,
        ACCU_CHEK,
        JUMPER,
        URION,
        YOLANDA,
        VIVACHEK,
    }

    public enum Model {
        UNKNOWN,
        TERUMO_MEDISAFE_FIT,
        AANDD_UC_352,
        AANDD_UA_651,
        AANDD_UT_201,
        FORACARE_D40,
        TAIDOC_TD1261,
        NONIN_3230,
        BERRY_BM1000B,
        ZENCRO_X6,
        ACCU_CHEK_AVIVA_CONNECT,
        JUMPER_FR302,
        JUMPER_JPD500E,
        URION_BP_U80E,
        YOLANDA_LITE,
        VIVACHEK_INO_SMART,
    }

    //public static final int BRAND_UNKNOWN = 1000;
    public static final int ROW_BRAND_TERUMO = 0;
    public static final int ROW_BRAND_AANDD= 1;
    public static final int ROW_BRAND_BERRY = 2;
    public static final int ROW_BRAND_NONIN = 3;
    public static final int ROW_BRAND_FORACARE = 4;
    public static final int ROW_BRAND_TAIDOC = 5;
    public static final int ROW_BRAND_ZENCRO = 6;
    public static final int ROW_BRAND_ACCU_CHEK = 7;
    public static final int ROW_BRAND_JUMPER = 8;
    public static final int ROW_BRAND_URION = 9;
    public static final int ROW_BRAND_YOLANDA = 10;
    public static final int ROW_BRAND_VIVACHEK = 11;

    // Terumo
    public static final int ROW_MODEL_MEDISAFE_FIT = 0;

    // A&D medical
    public static final int ROW_MODEL_UC_352 = 0;
    public static final int ROW_MODEL_UA_651 = 1;
    public static final int ROW_MODEL_UT_201= 2;

    // Foraare
    public static final int ROW_MODEL_D40 = 0;

    // TaiDoc
    public static final int ROW_MODEL_TD1261 = 0;

    // Berry
    public static final int ROW_MODEL_BM1000B = 0;

    // Nonin
    public static final int ROW_MODEL_N3230 = 0;

    // Zencro
    public static final int ROW_MODEL_X6 = 0;

    // Accu-chek
    public static final int ROW_MODEL_AC = 0;

    // Jumper
    public static final int ROW_MODEL_JUMPER_FR302 = 0;
    public static final int ROW_MODEL_JUMPER_JPD500E = 1;

    // Urion
    public static final int ROW_MODEL_URION_BP_U80E = 0;

    // Yolanda
    public static final int ROW_MODEL_YOLANDA_LITE = 0;

    // VivaChek
    public static final int ROW_MODEL_VIVACHEK_INO_SMART = 0;

    private Model mModel;
    private String mManufacturerCode;
    private String mProductCode;
    private String mProductTypeCode;
    private String mProductId;
    private String mWConfigCode;
    private String mAssignedName;
    private String mDeviceId;
    private String mMediaUrl;

    public MedicalDevice(Model model, String manufacturerCode, String productTypeCode,
                         String productCode, String productId, String wConfigCode, String assignedName) {
        mModel = model;
        mManufacturerCode = manufacturerCode;
        mProductCode = productCode;
        mProductTypeCode = productTypeCode;
        mProductId = productId;
        mWConfigCode = wConfigCode;
        mAssignedName = assignedName;
        mDeviceId = "";
        mMediaUrl = "";
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        MedicalDevice cloned = (MedicalDevice)super.clone();
        // the above is applicable in case of primitive member types,
        // however, in case of non primitive types
        // cloned.setNonPrimitiveType(cloned.getNonPrimitiveType().clone());
        return cloned;
    }

    public String getProductId() {
        return mProductId;
    }

    public String getMode() {
        return mWConfigCode;
    }

    public String getAssignedName() {
        return mAssignedName;
    }

    public int getImage() {
        return getModelImage(mModel);
    }

    public String getDeviceId() {
        return mDeviceId;
    }

    public String getManufacturerCode() {
        return mManufacturerCode;
    }

    public String getProductTypeCode() {
        return mProductTypeCode;
    }

    public String getProductCode() {
        return mProductCode;
    }

    public Brand getBrand() {
        return getModelBrand(mModel);
    }

    public String getMediaImageURL(){
        return mMediaUrl;
    }

    public void setDeviceId(String deviceId) {
        mDeviceId = deviceId;
    }

    public void setAssignedName(String name) {
        mAssignedName = name;
    }

    public void setMediaImageURL(String mediaUrl) {
        mMediaUrl = mediaUrl;
    }

    /**
     * Get the brand by position in string array
     *
     * @return brand
     */
    public static Brand getBrandByPosition(int position) {

        switch (position) {
            case ROW_BRAND_TERUMO:
                return Brand.TERUMO;

            case ROW_BRAND_FORACARE:
                return Brand.FORACARE;

            case ROW_BRAND_AANDD:
                return Brand.AANDD;

            case ROW_BRAND_TAIDOC:
                return Brand.TAIDOC;

            case ROW_BRAND_BERRY:
                return Brand.BERRY;

            case ROW_BRAND_NONIN:
                return Brand.NONIN;

            case ROW_BRAND_ZENCRO:
                return Brand.NONIN;

            case ROW_BRAND_ACCU_CHEK:
                return Brand.ACCU_CHEK;

            case ROW_BRAND_JUMPER:
                return Brand.JUMPER;

            case ROW_BRAND_URION:
                return Brand.URION;

            case ROW_BRAND_YOLANDA:
                return Brand.YOLANDA;

            case ROW_BRAND_VIVACHEK:
                return Brand.VIVACHEK;
        }

        return Brand.UNKNOWN;
    }

    public static Model getModelByPosition(Brand brand, int position) {

        switch (brand) {
            case TERUMO:
                return getTerumoModelByPosition(position);

            case AANDD:
                return getAAndDModelByPosition(position);

            case FORACARE:
                return getForaCareModelByPosition(position);

            case TAIDOC:
                return getTaiDocModelByPosition(position);

            case BERRY:
                return getBerryModelByPosition(position);

            case NONIN:
                return getNoninModelByPosition(position);

            case ZENCRO:
                return getZencroModelByPosition(position);

            case ACCU_CHEK:
                return getZencroModelByPosition(position);

            case JUMPER:
                return getJumperModelByPosition(position);

            case URION:
                return getUrionModelByPosition(position);

            case YOLANDA:
                return getYolandaModelByPosition(position);

            case VIVACHEK:
                return getVivaChekModelByPosition(position);
        }

        return Model.UNKNOWN;
    }

    private static Model getTerumoModelByPosition(int position) {
        switch (position) {
            case ROW_MODEL_MEDISAFE_FIT:
                return Model.TERUMO_MEDISAFE_FIT;
        }

        return Model.UNKNOWN;
    }

    private static Model getAAndDModelByPosition(int position) {
        switch (position) {
            case ROW_MODEL_UC_352:
                return Model.AANDD_UC_352;

            case ROW_MODEL_UA_651:
                return Model.AANDD_UA_651;

            case ROW_MODEL_UT_201:
                return Model.AANDD_UT_201;
        }

        return Model.UNKNOWN;
    }

    private static Model getForaCareModelByPosition(int position) {
        switch (position) {
            case ROW_MODEL_D40:
                return Model.FORACARE_D40;
        }

        return Model.UNKNOWN;
    }

    private static Model getTaiDocModelByPosition(int position) {
        switch (position) {
            case ROW_MODEL_TD1261:
                return Model.TAIDOC_TD1261;
        }

        return Model.UNKNOWN;
    }

    private static Model getBerryModelByPosition(int position) {
        switch (position) {
            case ROW_MODEL_BM1000B:
                return Model.BERRY_BM1000B;
        }

        return Model.UNKNOWN;
    }

    private static Model getNoninModelByPosition(int position) {
        switch (position) {
            case ROW_MODEL_N3230:
                return Model.NONIN_3230;
        }

        return Model.UNKNOWN;
    }

    private static Model getZencroModelByPosition(int position) {
        switch (position) {
            case ROW_MODEL_X6:
                return Model.ZENCRO_X6;
        }

        return Model.UNKNOWN;
    }

    private static Model getAccuChekModelByPosition(int position) {
        switch (position) {
            case ROW_MODEL_AC:
                return Model.ACCU_CHEK_AVIVA_CONNECT;
        }

        return Model.UNKNOWN;
    }

    private static Model getJumperModelByPosition(int position) {
        switch (position) {
            case ROW_MODEL_JUMPER_FR302:
                return Model.JUMPER_FR302;

            case ROW_MODEL_JUMPER_JPD500E:
                return Model.JUMPER_JPD500E;
        }

        return Model.UNKNOWN;
    }

    private static Model getUrionModelByPosition(int position) {
        switch (position) {
            case ROW_MODEL_URION_BP_U80E:
                return Model.URION_BP_U80E;

            default:
                return Model.UNKNOWN;
        }
    }

    private static Model getYolandaModelByPosition(int position) {
        switch (position) {
            case ROW_MODEL_YOLANDA_LITE:
                return Model.YOLANDA_LITE;

            default:
                return Model.UNKNOWN;
        }
    }

    private static Model getVivaChekModelByPosition(int position) {
        switch (position) {
            case ROW_MODEL_VIVACHEK_INO_SMART:
                return Model.VIVACHEK_INO_SMART;

            default:
                return Model.UNKNOWN;
        }
    }

    public static String getDeviceNameByModel(Model model) {
        switch (model) {
            case AANDD_UA_651:
                return "A&D_UA-651";

            case AANDD_UC_352:
                return "A&D_UC-352";

            case AANDD_UT_201:
                return "A&D_UT201";

            case BERRY_BM1000B:
                return "BerryMed";

            case NONIN_3230:
                return "Nonin3230";

            case ZENCRO_X6:
                return "X6";

            case ACCU_CHEK_AVIVA_CONNECT:
                return "Accu-Chek";

            case JUMPER_FR302:
                return "My Thermometer";

            case URION_BP_U80E:
                return "Bluetooth BP";

            case YOLANDA_LITE:
                return "QN-Scale";

            case VIVACHEK_INO_SMART:
                return "BLE-Vivachek";
        }

        return "unknown";
    }


    public static String getBrandName(Brand brand) {
        switch (brand) {
            case TERUMO:
                return "Terumo";

            case FORACARE:
                return "Foracare";

            case AANDD:
                return "A&D";

            case BERRY:
                return "Berry";

            case NONIN:
                return "Nonin";

            case ZENCRO:
                return "Zencro";

            case ACCU_CHEK:
                return "Accu-Chek";

            case JUMPER:
                return "Jumper";

            case URION:
                return "Urion";

            case YOLANDA:
                return "Yolanda";

            case VIVACHEK:
                return "VivaChek";
        }

        // TODO: unknown brand
        return "Unknown";
    }

    public static int getBrandImage(Brand brand) {
        switch (brand) {
            case TERUMO:
                return R.drawable.terumo_logo;

            case FORACARE:
                return R.drawable.fora_logo;

            case AANDD:
                return R.drawable.and_logo;

            case BERRY:
                return R.drawable.berry_logo;

            case NONIN:
                return R.drawable.nonin_logo;

            case ZENCRO:
                return R.drawable.zencro_logo;

            case ACCU_CHEK:
                return R.drawable.accu_chek_logo;//TODO
        }

        // TODO: unknown brand icon
        return R.drawable.terumo_logo;
    }

    public static int getModelImage(Model model) {
        switch (model) {
            case TERUMO_MEDISAFE_FIT:
                return R.drawable.glucose_icon;

            case FORACARE_D40:
                return R.drawable.glucose_icon; //R.drawable.bloodglucose_icon;

            case TAIDOC_TD1261:
                return R.drawable.glucose_icon; // R.drawable.earthermo_icon;

            case AANDD_UA_651:
                return R.drawable.automatic_pair_bp;

            case AANDD_UC_352:
                return R.drawable.weighscale;

            case AANDD_UT_201:
                return R.drawable.thermometer;

            case BERRY_BM1000B:
                return R.drawable.ic_berry_oximeter;

            case NONIN_3230:
                return R.drawable.ic_nonin_oximeter;

            case ZENCRO_X6:
                return R.drawable.zencro_x6;

            case ACCU_CHEK_AVIVA_CONNECT:
                return R.drawable.accu_chek_ac;//TODO

            case JUMPER_FR302:
                return R.drawable.ic_jumper_fr302;

            case JUMPER_JPD500E:
                return R.drawable.ic_jumper_jpd500e;

            case URION_BP_U80E:
                return R.drawable.ic_urion_u80e;

            case YOLANDA_LITE:
                return R.drawable.ic_yolanda;

            case VIVACHEK_INO_SMART:
                return R.drawable.ic_vivachek_ino_smart;
        }

        // TODO: unknonw model icon
        return R.drawable.glucose_icon;
    }

    public static String[] getBrandList(Context context) {
        if(EnterpriseHandler.getCurrentEnterprise()==EnterpriseHandler.GENERAL) {
            return context.getResources().getStringArray(R.array.medical_device_brands);
        }
        else if(EnterpriseHandler.getCurrentEnterprise()==EnterpriseHandler.TERUMO){
            return context.getResources().getStringArray(R.array.medical_device_brands_for_terumo_enterprise);
        }

        return context.getResources().getStringArray(R.array.medical_device_brands);
    }

    public static String[] getModelList(Context context, Brand brand) {
        switch (brand) {
            case TERUMO:
                return context.getResources().getStringArray(R.array.terumo_models);
            case FORACARE:
                return context.getResources().getStringArray(R.array.foracare_models);
            case AANDD:
                return context.getResources().getStringArray(R.array.aandd_models);
            case BERRY:
                return context.getResources().getStringArray(R.array.berry_models);
            case NONIN:
                return context.getResources().getStringArray(R.array.nonin_models);
            case ZENCRO:
                return context.getResources().getStringArray(R.array.zencro_models);
            case ACCU_CHEK:
                return context.getResources().getStringArray(R.array.accu_chek_models);

            case JUMPER:
                return context.getResources().getStringArray(R.array.jumper_models);

            case URION:
                return context.getResources().getStringArray(R.array.urion_models);

            case YOLANDA:
                return context.getResources().getStringArray(R.array.yolanda_models);

            case VIVACHEK:
                return context.getResources().getStringArray(R.array.vivachek_models);
        }

        return new String[0];
    }

    public static String[] getDescriptionList(Context context, Brand brand) {
        switch (brand) {
            case TERUMO:
                return context.getResources().getStringArray(R.array.terumo_descriptions);
            case FORACARE:
                return null;
                //return context.getResources().getStringArray(R.array.foracare_descriptions);
            case AANDD:
                return context.getResources().getStringArray(R.array.aandd_descriptions);
            case BERRY:
                return context.getResources().getStringArray(R.array.berry_descriptions);
            case NONIN:
                return context.getResources().getStringArray(R.array.nonin_descriptions);
            case ZENCRO:
                return context.getResources().getStringArray(R.array.zencro_descriptions);
            case ACCU_CHEK:
                return context.getResources().getStringArray(R.array.accu_check_descriptions);

            case JUMPER:
                return context.getResources().getStringArray(R.array.jumper_descriptions);

            case URION:
                return context.getResources().getStringArray(R.array.urion_descriptions);

            case YOLANDA:
                return context.getResources().getStringArray(R.array.yolanda_descriptions);

            case VIVACHEK:
                return context.getResources().getStringArray(R.array.vivachek_descriptions);
        }

        return new String[0];
    }

    private static Brand getModelBrand(Model model) {
        switch (model) {
            case TERUMO_MEDISAFE_FIT:
                return Brand.TERUMO;

            case TAIDOC_TD1261:
                return Brand.TAIDOC;

            case FORACARE_D40:
                return Brand.FORACARE;

            case AANDD_UA_651:
                return Brand.AANDD;

            case AANDD_UC_352:
                return Brand.AANDD;

            case AANDD_UT_201:
                return Brand.AANDD;

            case BERRY_BM1000B:
                return Brand.BERRY;

            case NONIN_3230:
                return Brand.NONIN;

            case ZENCRO_X6:
                return Brand.ZENCRO;

            case ACCU_CHEK_AVIVA_CONNECT:
                return Brand.ACCU_CHEK;

            case JUMPER_FR302:
                return Brand.JUMPER;

            case JUMPER_JPD500E:
                return Brand.JUMPER;

            case URION_BP_U80E:
                return Brand.URION;

            case YOLANDA_LITE:
                return Brand.YOLANDA;

            case VIVACHEK_INO_SMART:
                return Brand.VIVACHEK;
        }

        return Brand.UNKNOWN;
    }

    private static int getModelPosition(Brand brand, Model model) {
        int position = -1;
        switch (brand) {
            case TERUMO:
                if (model == Model.TERUMO_MEDISAFE_FIT) {
                    position = ROW_MODEL_MEDISAFE_FIT;
                }
                break;

            case TAIDOC:
                if (model == Model.TAIDOC_TD1261) {
                    position = ROW_MODEL_TD1261;
                }
                break;

            case AANDD:
                if (model == Model.AANDD_UA_651) {
                    position = ROW_MODEL_UA_651;
                }else if(model == Model.AANDD_UC_352){
                    position = ROW_MODEL_UC_352;
                }else{
                    position = ROW_MODEL_UT_201;
                }
                break;

            case BERRY:
                if (model == Model.BERRY_BM1000B) {
                    position = ROW_MODEL_BM1000B;
                }
                break;

            case NONIN:
                if (model == Model.NONIN_3230) {
                    position = ROW_MODEL_N3230;
                }
                break;

            case ZENCRO:
                if (model == Model.ZENCRO_X6) {
                    position = ROW_MODEL_X6;
                }
                break;

            case ACCU_CHEK:
                if (model == Model.ACCU_CHEK_AVIVA_CONNECT) {
                    position = ROW_MODEL_AC;
                }
                break;

            case JUMPER:
                if (model == Model.JUMPER_FR302) {
                    return ROW_MODEL_JUMPER_FR302;
                } else if (model == Model.JUMPER_JPD500E) {
                    return ROW_MODEL_JUMPER_JPD500E;
                }
                break;

            case URION:
                if (model == Model.URION_BP_U80E) {
                    return ROW_MODEL_URION_BP_U80E;
                }
                break;

            case YOLANDA:
                if (model == Model.YOLANDA_LITE) {
                    return ROW_MODEL_YOLANDA_LITE;
                }
                break;

            case VIVACHEK:
                if (model == Model.VIVACHEK_INO_SMART) {
                    return ROW_MODEL_VIVACHEK_INO_SMART;
                }
                break;
        }

        return position;
    }

    public static String getModelName(Context context, Model model) {

        Brand brand = getModelBrand(model);
        String[] names = getModelList(context, brand);
        int position = getModelPosition(brand, model);

        if (position < 0) {
            return "";
        }

        return names[position];
    }

    public static String getModelDescription(Context context, Model model) {

        Brand brand = getModelBrand(model);
        String[] names = getDescriptionList(context, brand);
        int position = getModelPosition(brand, model);

        if (position < 0) {
            return "";
        }

        return names[position];
    }

    public static MedicalDevice findModel(Model model) {
        for (MedicalDevice device : sMedicalDeviceList) {
            if (device.mModel == model) {
                return device;
            }
        }

        return null;
    }

    public Model getModel() {
        return mModel;
    }

    public static MedicalDevice findByProductId(String productId) {
        if (!TextUtils.isEmpty(productId)) {
            for (MedicalDevice device : sMedicalDeviceList) {
                if (device.mProductId.equalsIgnoreCase(productId)) {
                    return device;
                }
            }
        }
        return null;
    }


    public static final ArrayList<MedicalDevice> sMedicalDeviceList;
    static {
        sMedicalDeviceList = new ArrayList<>();
        sMedicalDeviceList.add(new MedicalDevice(Model.TERUMO_MEDISAFE_FIT, MANUFACTURER_CODE_TERUMO,
                PRODUCT_TYPE_CODE_TERUMO_MEDISAFE_FIT, PRODUCT_CODE_TERUMO_MEDISAFE_FIT,
                PRODUCT_ID_TERUMO_MEDISAFE_FIT, WCONFIG_CODE_NFC, "Terumo Medisafe Fit"));
        sMedicalDeviceList.add(new MedicalDevice(Model.AANDD_UC_352, MANUFACTURER_CODE_AAND,
                PRODUCT_TYPE_CODE_AAND_UC352, PRODUCT_CODE_AANDD_UC352,
                PRODUCT_ID_AANDD_UC352, WCONFIG_CODE_BLE,"Weighscale"));
        sMedicalDeviceList.add(new MedicalDevice(Model.AANDD_UA_651, MANUFACTURER_CODE_AAND,
                PRODUCT_TYPE_CODE_AAND_UA651, PRODUCT_CODE_AANDD_UA651,
                PRODUCT_ID_AANDD_UA651, WCONFIG_CODE_BLE,"Blood Pressure Monitor"));
        sMedicalDeviceList.add(new MedicalDevice(Model.AANDD_UT_201, MANUFACTURER_CODE_AAND,
                PRODUCT_TYPE_CODE_AAND_UT201, PRODUCT_CODE_AANDD_UT201,
                PRODUCT_ID_AANDD_UT201, WCONFIG_CODE_BLE,"Thermometer"));
        sMedicalDeviceList.add(new MedicalDevice(Model.BERRY_BM1000B, MANUFACTURER_CODE_BERRY,
                PRODUCT_TYPE_CODE_BERRY_BM1000B, PRODUCT_CODE_BERRY_BM1000B,
                PRODUCT_ID_BERRY_BM1000B, WCONFIG_CODE_BLE,"Oximeter"));
        sMedicalDeviceList.add(new MedicalDevice(Model.NONIN_3230, MANUFACTURER_CODE_NONIN,
                PRODUCT_TYPE_CODE_NONIN_3230, PRODUCT_CODE_NONIN_3230,
                PRODUCT_ID_NONIN_3230, WCONFIG_CODE_BLE,"Oximeter"));
        sMedicalDeviceList.add(new MedicalDevice(Model.ZENCRO_X6, MANUFACTURER_CODE_ZENCRO,
                PRODUCT_TYPE_CODE_ZENCRO_X6, PRODUCT_CODE_ZENCRO_X6,
                PRODUCT_ID_ZENCRO_X6, WCONFIG_CODE_BLE,"Smart Bracelet"));
        sMedicalDeviceList.add(new MedicalDevice(Model.ACCU_CHEK_AVIVA_CONNECT, MANUFACTURER_CODE_ACCU_CHEK,
                PRODUCT_TYPE_CODE_ACCU_CHEK_AC, PRODUCT_CODE_ACCU_CHEK_AC,
                PRODUCT_ID_ACCU_CHEK_AC, WCONFIG_CODE_BLE,"Accu-chek Glucometer"));
        sMedicalDeviceList.add(new MedicalDevice(Model.JUMPER_FR302, MANUFACTURER_CODE_JUMPER,
                PRODUCT_TYPE_CODE_JUMPER_FR302, PRODUCT_CODE_JUMPER_FR302,
                PRODUCT_ID_JUMPER_FR302, WCONFIG_CODE_BLE, "My Thermometer"));
        sMedicalDeviceList.add(new MedicalDevice(Model.JUMPER_JPD500E, MANUFACTURER_CODE_JUMPER,
                PRODUCT_TYPE_CODE_JUMPER_JPD500E, PRODUCT_CODE_JUMPER_JPD500E,
                PRODUCT_ID_JUMPER_JPD500E, WCONFIG_CODE_BLE, "My Oximeter"));
        sMedicalDeviceList.add(new MedicalDevice(Model.URION_BP_U80E, MANUFACTURER_CODE_URION,
                PRODUCT_TYPE_CODE_URION_BP, PRODUCT_CODE_URION_BP,
                PRODUCT_ID_URION_BP_U80E, WCONFIG_CODE_BLE, "Bluetooth BP"));
        sMedicalDeviceList.add(new MedicalDevice(Model.YOLANDA_LITE, MANUFACTURER_CODE_YOLANDA,
                PRODUCT_TYPE_CODE_YOLANDA_BW, PRODUCT_CODE_YOLANDA_BW,
                PRODUCT_ID_YOLANDA_BW_LITE, WCONFIG_CODE_BLE, "QN-Scale"));
        sMedicalDeviceList.add(new MedicalDevice(Model.VIVACHEK_INO_SMART, MANUFACTURER_CODE_VIVACHEK,
                PRODUCT_TYPE_CODE_VIVACHEK_BG, PRODUCT_CODE_VIVACHEK_BG,
                PRODUCT_ID_VIVACHEK_BG_INOSMART, WCONFIG_CODE_BLE, "BLE-Vivachek"));
        /*sMedicalDeviceList.add(new MedicalDevice(Model.TAIDOC_TD1261, MANUFACTURER_CODE_TAIDOC,
                PRODUCT_TYPE_CODE_TAIDOC_TD1261, PRODUCT_CODE_TAIDOC_TD1261,
                PRODUCT_ID_TAIDOC_TD1261, WCONFIG_CODE_BLE,"TaiDoc TD1261"));*/
        /*sMedicalDeviceList.add(new MedicalDevice(Model.FORACARE_D40, MANUFACTURER_CODE_FORACARE,
                PRODUCT_TYPE_CODE_FORACARE_D40, PRODUCT_CODE_FORACARE_D40,
                PRODUCT_ID_FORACARE_D40, WCONFIG_CODE_BLE,"D40"));*/
    }
}
