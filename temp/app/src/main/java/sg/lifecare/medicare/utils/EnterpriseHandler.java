package sg.lifecare.medicare.utils;

import android.content.Context;

/**
 * Created by wanping on 4/10/16.
 */
public class EnterpriseHandler {
    public final static int GENERAL = 0;
    public final static int TERUMO = 1;
    public final static int HITACHI = 2;

    public static int currentEnterprise = 0;

    public final static String TERUMO_ID = "9GV606QS-P1N8IW74-QCXDRL8U";

    public static void initialize(Context context){
        currentEnterprise = context
                .getSharedPreferences("lifecare_pref",Context.MODE_PRIVATE)
                .getInt("current_enterprise",0);
    }

    public static void setCurrentEnterprise(int enterprise){
        currentEnterprise = enterprise;
    }

    public static int getCurrentEnterprise(){
        return currentEnterprise;
    }

}
