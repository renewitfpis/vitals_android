package sg.lifecare.medicare.database.model;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import sg.lifecare.medicare.utils.JSONUtil;
import timber.log.Timber;

/**
 * Caregiver
 */
public class LifecareCaregiver implements Cloneable {

    private static final String TAG = "LifecareCaregiver";

    public static final int TYPE2_PRIMARY = 1;


    public static ArrayList<LifecareCaregiver> parse(JSONArray data) {
        ArrayList<LifecareCaregiver> caregivers = new ArrayList<>();

        if (data == null || data.length() == 0) {
            return caregivers;
        }

        for (int i = 0; i < data.length(); i++) {
            try {
                JSONObject object = data.getJSONObject(i);

                String type = JSONUtil.getString(object, "type");
                /*if (!TextUtils.isEmpty(type)) {
                    Timber.d("parse: has type " + type +", ignore");
                    continue;
                }*/

                String id = JSONUtil.getString(object, "_id");

                if (TextUtils.isEmpty(id)) {
                    Timber.d("parse: no id!!!");
                    continue;
                }

                LifecareCaregiver caregiver = new LifecareCaregiver(id);

                if (object.has("entity")) {
                    JSONObject entityObject = object.getJSONObject("entity");

                    String name = JSONUtil.getString(entityObject, "name");

                    if (TextUtils.isEmpty(name)) {
                        Timber.d("parse: name is null!!!");
                        continue;
                    }

                    caregiver.setName(name);
                }


                caregiver.setType2(JSONUtil.getInt(object, "type2"));
                caregiver.setType3(JSONUtil.getInt(object, "type3"));

                caregivers.add(caregiver);

            } catch (JSONException ex) {
                Timber.e(ex.getMessage(), ex);
            }
        }

        return caregivers;
    }

    private String id;
    private String name;
    private String type;
    private int type2;
    private int type3;

    private LifecareCaregiver(String id) {
        this.id = id;
    }

    public int getType2() {
        return type2;
    }

    public int getType3() {
        return type3;
    }

    public String getName() {
        return name;
    }

    public boolean isPrimary() {
        return TYPE2_PRIMARY == type2;
    }

    public void setType2(int type2) {
        Timber.d("setType2: "+ type2);
        this.type2 = type2;
    }

    public void setType3(int type3) {
        Timber.d("setType33: "+ type3);
        this.type3 = type3;

    }

    public void setName(String name) {
        this.name = name;
    }

}
