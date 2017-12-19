package sg.lifecare.medicare.database.model;

/**
 * Lifecare Module
 */
public class LifecareModule {

    private String mId;
    private String mCode;
    private String mName;

    public LifecareModule(String id, String code, String name) {
        mId = id;
        mCode = code;
        mName = name;
    }

    public String getId() {
        return mId;
    }

    public String getCode() {
        return mCode;
    }

    public String getName() {
        return mName;
    }
}
