package sg.lifecare.medicare.database.model;

/**
 * Lifecare media
 *
 * "medias": [
 * {
 * "_id": "57319126408f799b62918a93",
 * "media_url": "https://res.cloudinary.com/astralink-technology/image/upload/c_thumb,g_face,h_128,w_128/v1462866212/EB3RAHY8-66VC96Y0-WB7AISOU/1462866210584-singtel-logo.png",
 * "type": "P",
 * "title": "EB3RAHY8-66VC96Y0-WB7AISOU/1462866210584-singtel-logo"
 * }
 * ]
 */
public class LifecareMedia {

    private String mId;
    private String mUrl;
    private String mType;

    public LifecareMedia(String id) {
        mId = id;
    }

    public String getId() {
        return mId;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getType() {
        return mType;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public void setType(String type) {
        mType = type;
    }
}
