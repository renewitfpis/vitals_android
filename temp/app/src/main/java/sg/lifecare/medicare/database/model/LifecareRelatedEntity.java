package sg.lifecare.medicare.database.model;

import timber.log.Timber;

/**
 * Lifecare related entity
 */
public class LifecareRelatedEntity {

    private static final int TYPE2_PRIMARY = 1;

    private String mId;
    private String mType;
    private int mType2;
    private int mType3;
    private String mKinship;

    public LifecareRelatedEntity(String id, String type) {
        mId = id;
        mType = type;
        mKinship = type;
    }

    public void setType2(int type) {
        //LogUtil.d("LifecareRelatedEntity", "type2=" + type);
        mType2 = type;
    }

    public void setType3(int type) {
        //LogUtil.d("LifecareRelatedEntity", "type3=" + type);
        mType3 = type;
    }

    public String getId() {
        return mId;
    }

    public String getType() {
        return mType;
    }

    public int getType2() {
        return mType2;
    }

    public boolean isPrimary() {
        Timber.d("mType2=" + mType2);
        return mType2 == TYPE2_PRIMARY;
    }

    public String getKinship() {
        return mKinship;
    }

    public void setKinship(String type) {
        mKinship = type;
    }

    public String getKinshipRelation() {
        switch (mKinship) {
            case "F":
                return "Father";
            case "M":
                return "Mother";
            case "ML":
                return "Mother in Law";
            case "FL":
                return "Father in Law";
            case "FF":
                return "Grandfather(Paternal)";
            case "FM":
                return "Grandmother(Paternal)";
            case "MF":
                return "Grandfather";
            case "MM":
                return "Grandmother";
            case "GFF":
                return "Great Grandfather(Paternal)";
            case "GFM":
                return "Great Grandmother(Paternal)";
            case "MGF":
                return "Great Grandfather(Maternal)";
            case "MGM":
                return "Great Grandmother(Maternal)";

            case "UC":
                return "Uncle";

            case "AT":
                return "Aunty";
            default:
                return "";
        }
    }

}
