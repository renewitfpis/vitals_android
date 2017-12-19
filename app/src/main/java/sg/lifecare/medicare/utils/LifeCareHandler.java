package sg.lifecare.medicare.utils;

import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sg.lifecare.medicare.MediCareApplication;
import sg.lifecare.medicare.VitalConfigs;
import timber.log.Timber;


/**
 * Created by ct on 15/5/15.
 */
public class LifeCareHandler
{
    private static final boolean DBG = true;
    private static final boolean SINGTEL = false;

    public static final MediaType MEDIA_TYPE_JSON =
            MediaType.parse("application/json; charset=utf-8");

    public static String TAG = "LifeCareHandler";
    public static String host = VitalConfigs.URL;
    //public static String protocol = "https://";

    public static HttpURLConnection con;
    public static OkHttpClient okclient;
    public boolean isLoggedIn = false;

    public static String privacyUrl = VitalConfigs.URL + "privacy";
    public static String termsUrl = VitalConfigs.URL +  "terms";
    public static String aboutUrl = VitalConfigs.URL + "about";

    public String tempProductId = "5695b84046749cea001c59fa";

    public static final String PREF_COOKIES = "c_pref";
    public static final String COOKIES = "c";

    static final String COOKIES_HEADER = "Set-Cookie";
    static CookieManager cookieManager;

    static final TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain,
                String authType) throws CertificateException
            {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain,
                String authType) throws CertificateException
            {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers()
            {
                return new X509Certificate[]{};
            }
        }
    };


    private LifeCareHandler()
    {
        cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(new AddCookiesInterceptor());
        builder.addInterceptor(new ReceivedCookiesInterceptor());
        builder.connectTimeout(3, TimeUnit.SECONDS);

        if (SINGTEL)
        {
            try {
                // Install the all-trusting trust manager
                final SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new SecureRandom());
                // Create an ssl socket factory with our all-trusting manager
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                builder.sslSocketFactory(sslSocketFactory);
                builder.hostnameVerifier(new HostnameVerifier()
                {
                    @Override
                    public boolean verify(String hostname, SSLSession session)
                    {
                        return true;
                    }
                });
            } catch (KeyManagementException ex) {
                Log.e(TAG, ex.getMessage());
            } catch (NoSuchAlgorithmException ex) {
                Log.e(TAG, ex.getMessage());
            }
        }

        okclient = builder.build();
    }


    public void clearPrefCookies()
    {
        Log.d(TAG,"Cleared Pref Cookies");
        HashSet<String> cookies = new HashSet<>();
        cookies.add("empty");
        MediCareApplication.getContext().getSharedPreferences(PREF_COOKIES,
                Context.MODE_PRIVATE).edit()
            .putStringSet(COOKIES, cookies)
            .commit();
    }

    /**
     * This Interceptor add all received Cookies to the app DefaultPreferences.
     * Your implementation on how to save the Cookies on the Preferences MAY VARY.
     * <p>
     * Created by tsuharesu on 4/1/15.
     */
    public class ReceivedCookiesInterceptor implements Interceptor
    {
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException
        {
            try {
                Response originalResponse = chain.proceed(chain.request());

                //Log.d(TAG, "ReceivedCookiesInterceptor: " + originalResponse.toString());

                if (!originalResponse.headers("Set-Cookie").isEmpty()) {
                    HashSet<String> cookies = new HashSet<>();

                    for (String header : originalResponse.headers("Set-Cookie")) {
                        cookies.add(header);
                    }

                    MediCareApplication.getContext().getSharedPreferences(PREF_COOKIES,
                            Context.MODE_PRIVATE).edit()
                            .putStringSet(COOKIES, cookies)
                            .apply();
                }

                return originalResponse;
            }
            catch(Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }

   /* public static String getExpiry(){

        HashSet<String> cookies = (HashSet) MyApplication.getAppContext()
                .getSharedPreferences(
                        PREF_COOKIES, Context.MODE_PRIVATE).getStringSet(COOKIES, new HashSet<String>());
        if(cookies.size()>0) {
            for(String cookie : cookies){
                String[] ss = cookie.split("; ");


                if(ss.length > 0 ){
                    for(String s : ss){
                        Log.d(TAG, "s = " + s);
                        if(s.startsWith("Expires=")){
                            String expire = s.replace("Expires=","");
                            Log.d(TAG, "expiry = " + expire);
                            return expire;
                        }
                    }
                }
            }
        }

        return null;

    }*/
    /**
     * This interceptor put all the Cookies in Preferences in the Request.
     * Your implementation on how to get the Preferences MAY VARY.
     * <p>
     * Created by tsuharesu on 4/1/15.
     */
    public class AddCookiesInterceptor implements Interceptor
    {
        @Override
        public Response intercept(Chain chain) throws IOException
        {
            Request.Builder builder = chain.request().newBuilder();
            Context context = MediCareApplication.getContext();

            HashSet<String> preferences = (HashSet) MediCareApplication.getContext()
                    .getSharedPreferences(
                PREF_COOKIES, Context.MODE_PRIVATE).getStringSet(COOKIES, new HashSet<String>());

            Log.d(TAG,"PREF_SIZE = " + preferences.size());
            for (String cookie : preferences) {
                builder.addHeader("Cookie", cookie);
                Log.v("OkHttp", "Adding Header: " + cookie);
                // This is done so I know which headers are being added; this interceptor is used after the normal logging of OkHttp
            }

            //TODO
            Response response = null;
            try {
               response = chain.proceed(builder.build());

                if(response==null) {
                    Log.v("OkHttp", "Response is null, returned an empty response");
                    Response response1 = new Response.Builder().build();
                    return response1;
                }
            }
            catch(NullPointerException e){
                e.printStackTrace();
            }catch(RuntimeException ex){
                ex.printStackTrace();
            }

            Log.v("OkHttp", "Response is not null, returned original response");
            return response;

        }
    }

    //singleton
    private static LifeCareHandler instance = null;
    public static LifeCareHandler getInstance() {
        if (instance == null) {
            instance = new LifeCareHandler();
        }
        return instance;
    }

    public enum LifeCareWeightScaleType {
        kLIFECARE_WEIGHT_SCALE_TYPE_KG(0),
        kLIFECARE_WEIGHT_SCALE_TYPE_LB(1);
        private int value;

        private LifeCareWeightScaleType(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public static void cancelNotification(Context ctx, int notifyId)
    {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
        nMgr.cancel(notifyId);
    }

    public JSONArray getArrayFromData(String result)
    {
        //Log.w(TAG, result);

        JSONObject parentObject;
        JSONArray jArray = null;

        try
        {
            parentObject = new JSONObject(result);
            if (parentObject.has("Error")) {
                String err = parentObject.getString("Error");

                if (err.equalsIgnoreCase("false"))
                {
                    if (parentObject.has("Data"))
                    {
                        Log.w(TAG,"HAS DATA");
                        jArray = parentObject.getJSONArray("Data");
                    }
                }
            }
            else if (parentObject.has("error"))
            {
                String err = parentObject.getString("error");

                if (err.equalsIgnoreCase("false"))
                {
                    jArray = parentObject.getJSONArray("data");
                }
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        //Log.w(TAG,"Converted: " + jArray.toString());
        return jArray;
    }

    public JSONObject getObjectFromData(String result) {
        //Log.w(TAG, result);

        JSONObject parentObject;

        try {
            parentObject = new JSONObject(result);
            String err = parentObject.getString("Error");

            if (err.equalsIgnoreCase("false")) {
                JSONObject originalData = parentObject.getJSONObject("Data");
                if (originalData != null)
                    return originalData;
            }
            else
            {
                JSONObject errorObject = new JSONObject();
                try
                {
                    String errorDesc = parentObject.getString("ErrorDesc");
                    if(errorDesc.equalsIgnoreCase("null"))
                        return null;
                    else
                        errorObject.put("ErrorDesc", parentObject.getString("ErrorDesc"));
                    return errorObject;
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private String getResponseFromGet(String urlString)
    {
        Request request = new Request.Builder()
            .url(urlString)
            .build();

        try {
            Response response = okclient.newCall(request).execute();

            if (response.isSuccessful()) {
                String result = response.body().string();
                if (DBG) {
                    Log.d(TAG, "response:\n" + result);
                }

                response.body().close();
                return result;
            }

            response.body().close();
        } catch (IOException ioe) {
            Timber.e(ioe.getMessage());
        } catch (NullPointerException ne){
            Timber.e(ne.getMessage());
        } catch(Exception e){
            Timber.e(e.getMessage());
        }

        return "";
    }

    public String getResponseFromPost(String urlString, FormBody formBody)
    {
        Request request = new Request.Builder()
            .url(urlString)
            .post(formBody)
            .build();

        try {
            Response response = okclient.newCall(request).execute();

            String res = response.body().string();

            Log.d(TAG,"getResponseFromPost : " + res);

            response.body().close();

            Log.d(TAG,"succeed : " + res);

            return res;

        }catch (IOException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }

        return "";
    }

    private String getResponseFromPostJSON(String urlString, JSONObject urlParams)
    {
        Request request = new Request.Builder()
            .url(urlString)
            .post(RequestBody.create(MEDIA_TYPE_JSON, urlParams.toString())).build();

        try {
            Response response = okclient.newCall(request).execute();
            String res = response.body().string();

            if (DBG) {
                Log.d(TAG, "getResponseFromPostJSON response:\n" + res);
            }
            response.body().close();

            return res;

        }catch (IOException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }catch (NullPointerException e){
            Timber.e(e.getMessage());
        }

        return "";
    }

    public void trustEveryone()
    {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }});
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager(){
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {}
                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {}
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }}}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
                    context.getSocketFactory());
        } catch (Exception e) { // should never happen
            e.printStackTrace();
        }
    }

    public String singtelReqAuthToken(String cpRequestId, String otac, String appId,
                                      String additionalParams)
    {
        String result = "";
        trustEveryone();

        String urlString = "https://stagmsso.singtel.com/sso/ReqAuthToken";
        //String urlString = "https://msso.singtel.com/sso/ReqAuthToken";

        ContentValues values = new ContentValues();
        values.put("cpRequestId", cpRequestId);
        values.put("otac", otac);
        values.put("applicationId", appId);

        if(!additionalParams.isEmpty())
        {
           values.put("Access", additionalParams);
        }

        try
        {
            URL url = new URL(urlString);
            con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");

            con.setDoOutput(true);

            OutputStream os = con.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getQuery(values));
            writer.flush();
            writer.close();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null)
            {
                response.append(inputLine);
            }
            in.close();

            //print result
            result = response.toString();
            System.out.println(response.toString());
        }
        catch (IllegalArgumentException e)
        {
            Log.w(TAG, e.getMessage());
        }
        catch (IOException e)
        {
            Log.w(TAG, e.getMessage());
        }

        return result;
    }

    private String getQuery(ContentValues values) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, Object> entry : values.valueSet())
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
        }

        return result.toString();
    }

    public static String getExpiry(){

        HashSet<String> cookies = (HashSet) MediCareApplication.getContext()
                .getSharedPreferences(
                        PREF_COOKIES, Context.MODE_PRIVATE).getStringSet(COOKIES, new HashSet<String>());
        if(cookies.size()>0) {
            for(String cookie : cookies){
                String[] ss = cookie.split("; ");


                if(ss.length > 0 ){
                    for(String s : ss){
                        Log.d(TAG, "s = " + s);
                        if(s.startsWith("Expires=")){
                            String expire = s.replace("Expires=","");
                            Log.d(TAG, "expiry = " + expire);
                            return expire;
                        }
                    }
                }
            }
        }

        return null;

    }

    public String signup(String email, String first, String last, String pass){
        String urlString = VitalConfigs.URL + "mlifecare/authentication/smarthomeUserSignup";

        FormBody formBody = new FormBody.Builder()
                .add("AuthenticationString", email)
                .add("FirstName", first)
                .add("LastName", last)
                .add("Password", pass)
                .build();

        return getResponseFromPost(urlString,formBody);
    }

    public String signup(String email, String first, String last, String pass, String enterpriseId){
        String urlString = VitalConfigs.URL + "mlifecare/authentication/smarthomeUserSignup";

        FormBody formBody = new FormBody.Builder()
                .add("AuthenticationString", email)
                .add("FirstName", first)
                .add("LastName", last)
                .add("Password", pass)
                .add("Enterprise",enterpriseId)
                .build();

        return getResponseFromPost(urlString,formBody);
    }

    public JSONArray login(String cpRequestId, String accessToken, String otac,
                           String deviceId, String deviceType, String token)
    {
        JSONObject parentObject;
        JSONArray jArray = null;

        String urlString = VitalConfigs.URL + "mlifecare/singtel/appLogin";

        try
        {
            FormBody formBody = new FormBody.Builder()
                    .add("CpRequestId", cpRequestId)
                    .add("Otac", otac)
                    .add("AccessToken", accessToken)
                    .add("DeviceId", deviceId)
                    .add("DeviceType", deviceType)
                    .add("Token", token)
                    .build();

            Request request = new Request.Builder()
                    .url(urlString)
                    .post(formBody)
                    .build();

            Response response = okclient.newCall(request).execute();

            if (response.isSuccessful())
            {
                if (DBG)
                {
                    Log.d(TAG, "header:\n" + response.headers().toString());
                }

                List<String> headers = response.headers(COOKIES_HEADER);
                if ((headers != null) && (headers.size() > 0)) {
                    for (String cookie : headers) {
                        cookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                    }
                }

                String result = response.body().string();

                if (DBG)
                {
                    //Log.d(TAG, "response:\n" + result);
                }

                if (result == null)
                {
                    return null;
                }

                try
                {
                    parentObject = new JSONObject(result);
                    if (parentObject.has("Error"))
                    {
                        String err = parentObject.getString("Error");

                        if (err.equalsIgnoreCase("false"))
                        {
                            if (parentObject.has("Data"))
                            {
                                jArray = parentObject.getJSONArray("Data");
                                return jArray;
                            }
                        }
                        else
                        {
                            //didnt get any data;
                            String errorDesc = parentObject.getString("ErrorDesc");
                            JSONObject errObject = new JSONObject();
                            errObject.put("ErrorDesc", errorDesc);
                            jArray = new JSONArray();
                            jArray.put(errObject);

                            return jArray;
                        }
                    }
                }
                catch(JSONException e)
                {
                    e.printStackTrace();
                }
            }

            response.body().close();
        }
        catch(UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        catch (IOException ex)
        {
             Log.e(TAG, ex.getMessage(), ex);
        }

        return jArray;
    }

    public HashMap<String, String> login(String user, String password, String deviceId,
                                         String tokenId, String deviceType)
    {
        String urlString = VitalConfigs.URL + "mlifecare/authentication/appLogin";

        FormBody formBody = new FormBody.Builder()
            .add("AuthenticationString", user)
            .add("Password", password)
            .add("Token", tokenId)
            .add("DeviceId", deviceId)
            .add("DeviceType", deviceType)
            .build();

        Request request = new Request.Builder()
            .url(urlString)
            .post(formBody)
            .build();

        try {
            Response response = okclient.newCall(request).execute();
            if (response.isSuccessful()) {

                if (DBG) {
                    Log.d(TAG, "loginact header:\n" + response.headers().toString());
                }

                List<String> headers = response.headers(COOKIES_HEADER);
                if ((headers != null) && (headers.size() > 0)) {
                    for (String cookie : headers) {
                        cookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                    }
                }

                String result = response.body().string();

                if (DBG) {
                    Log.d(TAG, "loginact response:\n" + result);
                }

                if (result == null) {
                    Log.d(TAG, "RETURN loginact response null");
                    return null;
                }

                try {
                    JSONObject parentObject = new JSONObject(result);
                    String err = parentObject.getString("Error");

                    HashMap<String, String> map = new HashMap<>();

                    if (err.equalsIgnoreCase("false")) {

                        JSONObject json_data = parentObject.getJSONObject("Data");

                        String entity = json_data.getString("_id");
                        Log.v(TAG,"entityId = " + entity);
                        if (entity != null && !entity.isEmpty()) {
                            map.put("entity_id", entity);
                        }
                        else
                            return null;

                        String authorizationId = json_data.getString("authorization_level");
                        if (authorizationId != null && !authorizationId.isEmpty()) {
                            map.put("authorization_level", authorizationId);
                        }

                        String name = json_data.getString("name");
                        if (name != null && !name.isEmpty()) {
                            map.put("name", name);
                        }

                        JSONObject obj = json_data.getJSONObject("enterprise");

                        if(obj != null && obj.length() > 0)
                        {
                            if(obj.getString("_id")!=null) {
                                Log.d(TAG," ENterprise ID = " + obj.getString("_id"));
                                map.put("enterprise_id", obj.getString("_id"));
                            }
                            else{
                                Log.d(TAG,"Enterprise ID is null");
                            }
                        }



                    }
                    else
                        return null;

                    Log.d(TAG, "RETURN loginact response map");
                    return map;
                } catch (JSONException ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                }

            }
            response.body().close();
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }

        return null;
    }

    public boolean logout(String deviceId)
    {
        String urlString = VitalConfigs.URL + "mlifecare/authentication/appLogout";
        String urlParameters = "DeviceId=" + deviceId;

        FormBody formBody = new FormBody.Builder()
            .add("DeviceId", deviceId)
            .build();

        Request request = new Request.Builder()
            .url(urlString)
            .post(formBody)
            .build();

        Log.d(TAG, "logging out!");
        try {
            Response response = okclient.newCall(request).execute();
           // if (response.isSuccessful()) {
                Log.d(TAG, "response  !");
                String result = response.body().string();

                Log.d(TAG, "logout:\n" + result);
            isLoggedIn = false;
            clearPrefCookies();
                /*if (result != null) {
                    try {
                        JSONObject parentObject = new JSONObject(result);
                        String err = parentObject.getString("Error");
                        //String respons = parentObject.getString("Data");
                        if ("false".equalsIgnoreCase(err)) {
                            isLoggedIn = false;
                            clearPrefCookies();
                            return true;
                        }
                    } catch (JSONException ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                    }
                }*/
          /*  }else{

                Log.d(TAG, "response not successful!");
            }*/
            response.body().close();

            return true;
        } catch (NullPointerException ne) {
            Log.e(TAG, ne.getMessage());
        } catch (SocketTimeoutException | RuntimeException e){
            Log.e(TAG, e.getMessage());
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }

        return false;
    }

    public JSONObject sendTempPassword(String email)
    {
        String urlString = VitalConfigs.URL + "mlifecare/authentication/forgotPasswordRequest\n";
        //String urlParameters = "AuthenticationString=" + email;

        FormBody formBody = new FormBody.Builder()
            .add("AuthenticationString", email)
            .build();
        Request request = new Request.Builder()
            .url(urlString)
            .post(formBody)
            .build();

        try {
            Response response = okclient.newCall(request).execute();
            if (response.isSuccessful()) {
                String result = response.body().string();
                if (result != null) {
                    try {
                        JSONObject parentObject = new JSONObject(result);
                        return parentObject;
                    } catch (JSONException ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                    }
                }
            }
            response.body().close();
        }  catch (IOException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }

        return null;
    }

    public JSONArray getCurrentAssisted(String caregiverId)
    {
        String urlString = VitalConfigs.URL + "mlifecare/entityRelationship/getAssisted?CaregiverId="
                + caregiverId;

        String result = getResponseFromGet(urlString);
        if (DBG)
        {
            Log.d(TAG, "getCurrentAssisted:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getAdminEntities(String day)
    {
        //final HttpGet httppost = new HttpGet(protocol + host
        //        + "/mlifecare/entity/getAdminEntityAnalyticsDetails?Day=" + day);
        String urlString = VitalConfigs.URL + "mlifecare/entity/getAdminEntityAnalyticsDetails?Day=" + day;

        String result = getResponseFromGet(urlString);
        if (DBG) {
            //Log.d(TAG, "getAdminEntities:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getCaregiverEntities(String caregiverId, String day)
    {
        //final HttpGet httppost = new HttpGet(protocol + host
        //        + "/mlifecare/entity/getCaregiverEntityAnalyticsDetails?Day="
        //        + day + "&CaregiverEntityId=" + caregiverId);

        String urlString = VitalConfigs.URL + "mlifecare/entity/getCaregiverEntityAnalyticsDetails?Day="
                + day + "&CaregiverEntityId=" + caregiverId;

        Log.w(TAG, "urlString:" + urlString);

        String result = getResponseFromGet(urlString);
        if (DBG) {
            Log.d(TAG, "getCaregiverEntities:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getEnergyConsumptionDetails(String entityId, String day)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getEnergyConsumptionAnalytics?"
                + "EntityId=" + entityId
                + "&Day=" + day;

        String result = getResponseFromGet(urlString);
        if (DBG) {
            //Log.d(TAG, "getEnergyConsumptionDetails:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getEnergyConsumptionDetails(String entityId, String startDate, String endDate)
    {
        String urlString = VitalConfigs.URL + "mlifecare/historicalData/getEnergyConsumptionAnalytics?"
                + "EntityId=" + entityId
                + "&StartDate=" + startDate
                + "&EndDate=" + endDate;

        Log.d(TAG,"URL String = " + urlString);
        String result = getResponseFromGet(urlString);
        if (DBG) {
            //Log.d(TAG, "getEnergyConsumptionDetails:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getEnergyBreakdownDetails(String entityId, String day)
    {
        String urlString = VitalConfigs.URL
                + "mlifecare/event/getEnergyConsumptionBreakdownAnalytics?"
                + "EntityId=" + entityId
                + "&Day=" + day;

        String result = getResponseFromGet(urlString);
        if (DBG) {
            //Log.d(TAG, "getEnergyBreakdownDetails:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getWaterConsumptionDetails(String entityId, String day)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getWaterConsumptionAnalytics?"
                + "EntityId=" + entityId
                + "&Day=" + day;

        if(DBG) {
            Log.d(TAG, "API call (water) = " + urlString);
        }
        String result = getResponseFromGet(urlString);
        if (DBG) {
            //Log.d(TAG, "getWaterConsumptionDetails:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getWaterConsumptionDetails(String entityId, String startDate, String endDate)
    {
        String urlString = VitalConfigs.URL + "mlifecare/historicalData/getWaterConsumptionAnalytics?"
                + "EntityId=" + entityId
                + "&StartDate=" + startDate
                + "&EndDate=" + endDate;

        if(DBG) {
            Log.d(TAG, "API call (water) = " + urlString);
        }
        String result = getResponseFromGet(urlString);
        if (DBG) {
            //Log.d(TAG, "getWaterConsumptionDetails:\n" + result);
        }

        return getArrayFromData(result);
    }
    public JSONArray getLatestWaterConsumptionDetails(String entityId, String day)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getWaterConsumptionAnalytics?"
                + "EntityId=" + entityId
                + "&Day=" + day;

        String result = getResponseFromGet(urlString);
        if (DBG) {

            Log.d(TAG,"API call (week water) = " + urlString);
            Log.d(TAG, "getWaterConsumptionDetails:\n" + result);
        }

        return getArrayFromData(result);
    }
    public JSONArray getLatestWaterConsumptionDetails(String entityId, String startDate, String endDate)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getWaterConsumptionAnalytics?"
                + "EntityId=" + entityId
                + "&StartDate=" + startDate
                + "&EndDate=" + endDate;

        String result = getResponseFromGet(urlString);
        if (DBG) {

            Log.d(TAG,"API call (week water) = " + urlString);
            Log.d(TAG, "getWaterConsumptionDetails:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getBloodPressureReading(String entityId, int pageSize)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getBloodPressureReading?EntityId=" + entityId
                +"&PageSize=" + pageSize;

        String result = getResponseFromGet(urlString);
        if (DBG) {
            Log.d(TAG, "getBloodPressureReading:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getBloodPressureReadingByDate(String entityId, String date)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getBloodPressureReading?EntityId=" + entityId
                +"&Day=" + date;

        String result = getResponseFromGet(urlString);
        if (DBG) {
            Log.d(TAG, "getBloodPressureReading:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getBloodPressureReadingByDateRange(String entityId, String startDate, String endDate)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getBloodPressureReading?EntityId=" + entityId
                +"&StartDay=" + startDate
                +"&EndDay=" + endDate;

        String result = getResponseFromGet(urlString);
        if (DBG) {
            Log.d(TAG, "getBloodPressureReading of " + urlString + ":\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getActivityDistributionToday(String entityId, String startDate,
                                                  String endDate, boolean sortHourly)
    {

        String urlString = VitalConfigs.URL + "mlifecare/event/getAggregatedActivityCount?" +
                "EntityId=" + entityId +
                "&StartDateTime=" + startDate +
                "&EndDateTime=" + endDate;

        if(sortHourly == true)
        {
            urlString = urlString + "&SortHourly=true";
        }

        String result = getResponseFromGet(urlString);
        if (DBG) {
            //Log.d(TAG, "getActivityDistributionToday:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getElderlyProfileDetail(String entityId, String caregiverId)
    {
        String urlString = VitalConfigs.URL + "mlifecare/entity/getEntityDetail?EntityId="
                + entityId + "&CaregiverId=" + caregiverId;
        String result = getResponseFromGet(urlString);
        if (DBG) {
            //Log.d(TAG, "getElderlyProfileDetail:\n" + result);
        }

        return getArrayFromData(result);
    }

    public String editSeniorProfile(JSONObject urlParameters)
    {
        String result = "true";
        String urlString = VitalConfigs.URL + "mlifecare/entity/updateUserProfile";

        String res = getResponseFromPostJSON(urlString, urlParameters);
        Log.w(TAG, "res:" + res);

        JSONObject parentObject;

        try
        {
            parentObject = new JSONObject(res);
            String err = parentObject.getString("Error");

            if ("false".equalsIgnoreCase(err))
            {
                return result;
            }
            else
            {
                result = parentObject.getString("ErrorDesc");
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return result;
    }

    public JSONArray getAvailableRules(String entityId)
    {
        String urlString = VitalConfigs.URL + "mlifecare/rule/getRules?" +
                "EntityId=" + entityId;

        Log.w(TAG, "urlString :" + urlString);

        String result = getResponseFromGet(urlString);

        return getArrayFromData(result);
    }

    public String deleteAlertRules(FormBody formBody)
    {
        String result = "true";
        String urlString = VitalConfigs.URL + "mlifecare/rule/deleteRule";
        String res = getResponseFromPost(urlString, formBody);

        JSONObject parentObject;

        try
        {
            parentObject = new JSONObject(res);
            String err = parentObject.getString("Error");

            if ("false".equalsIgnoreCase(err))
            {
                return result;
            }
            else
            {
                result = parentObject.getString("ErrorDesc");
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public JSONArray getAvailableRuleTypes(String entityId, String ruleType)
    {
        String urlString = VitalConfigs.URL + "mlifecare/lookupValue/getUsersRuleTypes?" +
                "EntityId=" + entityId+
                "&RuleType=" + ruleType;

        String result = getResponseFromGet(urlString);

        return getArrayFromData(result);
    }

    public JSONArray getAvailableRuleZones(String entityId, boolean allZone)
    {
        String urlString = VitalConfigs.URL + "mlifecare/device/getActivityZoneCoverage?" +
                "EntityId=" + entityId;

        if(allZone == true)
        {
            urlString = urlString + "&AllZonesCoverage=true";
        }

        String result = getResponseFromGet(urlString);

        return getArrayFromData(result);
    }

    public String addNewAlertRules(FormBody formBody)
    {
        String result = "true";
        String urlString = VitalConfigs.URL + "mlifecare/rule/addRule";
        String res = getResponseFromPost(urlString, formBody);

        Log.w(TAG, "result :" + res);

        JSONObject parentObject;

        try
        {
            parentObject = new JSONObject(res);
            String err = parentObject.getString("Error");

            if ("false".equalsIgnoreCase(err))
            {
                return result;
            }
            else
            {
                result = parentObject.getString("ErrorDesc");
            }

        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public JSONArray getActivities(String entityId, int pageSize, int skipSize)
    {
        /*
        String urlString = protocol + host
                + "/mlifecare/message/getAlertMessages?EntityId=" + entityId
                + "&" + "PageSize=" + pageSize
                + "&" + "SkipSize=" + skipSize;*/

        String urlString = VitalConfigs.URL + "mlifecare/event/getActivityData?EntityId=" + entityId
                + "&PageSize=" + pageSize;

        if(skipSize > 0)
        {
            urlString = urlString + "&SkipSize=" + skipSize;
        }

        Log.w(TAG, "urlString :" + urlString);

        String result = getResponseFromGet(urlString);

        return getArrayFromData(result);
    }

    public boolean getGatewayStatus(String deviceId)
    {
        boolean status = false;
        String urlString = VitalConfigs.URL + "mlifecare/device/checkSmartphoneIsGateway?DeviceId=" + deviceId;
        String result = getResponseFromGet(urlString);

        Log.w(TAG, "is gateway :" + result);

        try
        {
            JSONObject data = new JSONObject(result);
            if(data.getString("Data").equalsIgnoreCase("false"))
                status = false;
            else if(data.getString("Data").equalsIgnoreCase("true"))
                status = true;
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        return status;
    }

    public String convertSmartphoneToGateway(String deviceId, String entityId)
    {
        String result = "";

        String urlString = VitalConfigs.URL + "mlifecare/device/convertDeviceToGateway";
        String ans = "DeviceId=" + deviceId + "&EntityId=" + entityId;
        Log.d(TAG,"LifeCareHandler: convertSPtoGateway = " + ans);
        Log.d(TAG,"convert smrt phone to gateway");
        FormBody urlParams = new FormBody.Builder()
            .add("DeviceId", deviceId)
            .add("EntityId", entityId)
            .build();

        result = getResponseFromPost(urlString, urlParams);

        Log.w(TAG, "res:" + result);

        JSONObject parentObject;

        try
        {
            parentObject = new JSONObject(result);
            String err = parentObject.getString("Error");

            if (err.equalsIgnoreCase("false"))
            {
                return result;
            }
            else
            {
                result = parentObject.getString("ErrorDesc");
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return result;
    }

    public JSONArray getCurrentCaregiverProfile(String entityId)
    {
        String urlString = VitalConfigs.URL + "mlifecare/entity/getEntityDetail?"
                + "EntityId=" + entityId;
        Timber.d(TAG,"getCurrentCaregiverProfile URL = " + urlString);
        String result = getResponseFromGet(urlString);

        return getArrayFromData(result);
    }

    public String editCaregiverProfile(FormBody formBody)
    {
        String result = "true";
        String urlString = VitalConfigs.URL + "mlifecare/entity/updateUserProfile";
        String res = getResponseFromPost(urlString, formBody);

        Log.w(TAG, "res:" + res);

        JSONObject parentObject;

        try
        {
            parentObject = new JSONObject(res);
            String err = parentObject.getString("Error");

            if (err.equalsIgnoreCase("false"))
            {
                return result;
            }
            else
            {
                result = parentObject.getString("ErrorDesc");
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return result;
    }

    public String completeProfileSetup(FormBody urlParameters)
    {
        String result = "true";
        String urlString = VitalConfigs.URL + "mlifecare/singtel/completeProfileSetup";
        String res = getResponseFromPost(urlString, urlParameters);

        Log.w(TAG, "res:" + res);

        JSONObject parentObject;

        try
        {
            parentObject = new JSONObject(res);
            String err = parentObject.getString("Error");

            if (err.equalsIgnoreCase("false"))
            {
                return result;
            }
            else
            {
                result = parentObject.getString("ErrorDesc");
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return result;
    }

    public JSONArray getEntityAnalyticDetails(String entityId, String date) {

        //final HttpGet httppost = new HttpGet(protocol + host
        //        + "/mlifecare/entity/getEntityAnalyticsDetails?EntityId="
        //        + entityId + "&Day=" + date);

        String urlString = VitalConfigs.URL + "mlifecare/entity/getEntityAnalyticsDetails?EntityId="
                + entityId + "&Day=" + date;

        String result = getResponseFromGet(urlString);
        if (DBG) {
            //Log.d(TAG, "getEntityAnalysisDetails:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getEntityStatus(String entityId)
    {

        //final HttpPost httppost = new HttpPost(protocol + host
        //        + "/mlifecare/entity/updateEntityStatus");

        String urlString = VitalConfigs.URL + "mlifecare/entity/updateEntityStatus";

        String result = getResponseFromGet(urlString);
        if (DBG) {
            //Log.d(TAG, "getEntityStatus:\n" + result);
        }

        return getArrayFromData(result);
    }

    public String getMedianWakeUp(String entityId, String day)
    {
        String medianWakeup = "";
        String urlString = VitalConfigs.URL + "mlifecare/informativeData/getMedianWakeupAnalytics"
                + "?EntityId=" + entityId + "&Day=" + day;

        String result = getResponseFromGet(urlString);
        if (DBG)
        {
            //Log.d(TAG, "getMedianWakeUp:\n" + result);
        }

        JSONArray res = getArrayFromData(result);
        if(res != null && res.length() > 0)
        {
            try
            {
                JSONObject data = res.getJSONObject(0);

                if(data.getString("median_wakeup_time") != null
                        && !data.getString("median_wakeup_time").equalsIgnoreCase("null")
                        && !data.getString("median_wakeup_time").isEmpty())
                {
                    medianWakeup = data.getString("median_wakeup_time");
                }
            }
            catch(JSONException e)
            {
                e.printStackTrace();
            }
        }

        return medianWakeup;
    }

    public String getMedianSleep(String entityId, String day)
    {
        String medianSleep = "";
        String urlString = VitalConfigs.URL + "mlifecare/informativeData/getMedianSleepAnalytics"
                + "?EntityId=" + entityId + "&Day=" + day;

        String result = getResponseFromGet(urlString);
        if (DBG)
        {
            //Log.d(TAG, "getMedianSleep:\n" + result);
        }

        JSONArray res = getArrayFromData(result);

        if(res != null && res.length() > 0)
        {
            try
            {
                JSONObject data = res.getJSONObject(0);

                if(data.getString("median_sleep_time") != null
                        && !data.getString("median_sleep_time").equalsIgnoreCase("null")
                        && !data.getString("median_sleep_time").isEmpty())
                {
                    medianSleep = data.getString("median_sleep_time");
                }
            }
            catch(JSONException e)
            {
                e.printStackTrace();
            }
        }

        return medianSleep;
    }

    public JSONArray getActivityStatAnalytics(String entityId, String day)
    {
        String urlString = VitalConfigs.URL + "mlifecare/informativeData/getActivityStatisticalAnalytics"
                + "?EntityId=" + entityId
                + "&Day=" + day;

        String result = getResponseFromGet(urlString);

        return getArrayFromData(result);
    }

    public JSONArray getBathroomStatAnalytics(String entityId, String day)
    {
        String urlString = VitalConfigs.URL + "mlifecare/informativeData/getBathroomStatisticalAnalytics"
                + "?EntityId=" + entityId
                + "&Day=" + day;

        String result = getResponseFromGet(urlString);

        return getArrayFromData(result);
    }

    public JSONArray getWakeUpAnalytics(String entityId, int pageSize, boolean isValid)
    {
        String urlString = VitalConfigs.URL + "mlifecare/historicalData/getWakeupAnalytics"
                + "?EntityId=" + entityId + "&PageSize=" + pageSize;

        if(isValid == true)
            urlString = urlString + "&IsValid=true";
        else
            urlString = urlString + "&IsValid=false";

        String result = getResponseFromGet(urlString);
        if (DBG) {
            Log.d(TAG, "getGlucoseReading:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getSleepAnalytics(String entityId, int pageSize, boolean isValid)
    {
        String urlString = VitalConfigs.URL + "mlifecare/historicalData/getSleepAnalytics"
                + "?EntityId=" + entityId + "&PageSize=" + pageSize;

        if(isValid == true)
            urlString = urlString + "&IsValid=true";
        else
            urlString = urlString + "&IsValid=false";

        String result = getResponseFromGet(urlString);
        if (DBG) {
            //Log.d(TAG, "getGlucoseReading:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getActivityStatAnalytics(String entityId, int pageSize, boolean isValid)
    {
        String urlString = VitalConfigs.URL + "mlifecare/informativeData/getActivityStatisticalAnalytics"
                +"?EntityId=" + entityId
                +"&PageSize=" + pageSize;

        if(isValid == true)
        {
            urlString = urlString +"&IsValid=true";
        }

        String result = getResponseFromGet(urlString);
        if (DBG)
        {
            //Log.d(TAG, "getActivityStatAnalytics:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getBathroomStatsAnalytical(String entityId, int pageSize, boolean isValid)
    {
        String urlString = VitalConfigs.URL + "mlifecare/informativeData/getBathroomStatisticalAnalytics"
                +"?EntityId=" + entityId
                +"&PageSize=" + pageSize;

        if(isValid == true)
        {
            urlString = urlString +"&IsValid=true";
        }

        String result = getResponseFromGet(urlString);
        if (DBG)
        {
            //Log.d(TAG, "getBathroomStatsAnalytical:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getGlucoseReading(String entityId, int pageSize)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getGlucoseReading?"
                + "EntityId=" + entityId
                + "&PageSize=" + pageSize;

        String result = getResponseFromGet(urlString);
        if (DBG) {
            Log.d(TAG, "getGlucoseReading:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getGlucoseReadingByDate(String entityId, String date)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getGlucoseReading?"
                + "EntityId=" + entityId
                + "&Day=" + date;

        String result = getResponseFromGet(urlString);
        if (DBG) {
            Log.d(TAG, "getGlucoseReading:\n" + result);
        }

        return getArrayFromData(result);
    }


    public JSONArray getGlucoseReadingByDateRange(String entityId, String startDate, String endDate)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getGlucoseReading?"
                + "EntityId=" + entityId
                + "&StartDay=" + startDate
                + "&EndDay=" + endDate;

        String result = getResponseFromGet(urlString);
        if (DBG) {
            Log.d(TAG, "getGlucoseReading:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getMedicationByDateRange(String entityId, String startDate, String endDate)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getMedications?"
                + "EntityId=" + entityId
                + "&StartDay=" + startDate
                + "&EndDay=" + endDate;

        String result = getResponseFromGet(urlString);
        if (DBG) {
            Log.d(TAG, "getMedicationByDateRange:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getFoodByDateRange(String entityId, String startDate, String endDate)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getFoodIntake?"
                + "EntityId=" + entityId
                + "&StartDay=" + startDate
                + "&EndDay=" + endDate;

        String result = getResponseFromGet(urlString);
        if (DBG) {
            Log.d(TAG, "getFoodByDateRange:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getSymptomsByDateRange(String entityId, String startDate, String endDate)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getSymptoms?"
                + "EntityId=" + entityId
                + "&StartDay=" + startDate
                + "&EndDay=" + endDate;

        String result = getResponseFromGet(urlString);
        if (DBG) {
            Log.d(TAG, "getSymptomsByDateRange of " + urlString +":\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getSPO2Reading(String entityId, int pageSize)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getSPO2Reading" +
                "?EntityId=" + entityId +
                "&PageSize=" + pageSize;

        String result = getResponseFromGet(urlString);
        if (DBG) {
            //Log.d(TAG, "getSPO2Reading:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getBodyTemperatureReading(String entityId, int pageSize)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getBodyTemperatureReading" +
                "?EntityId=" + entityId +
                "&PageSize=" + pageSize;

        String result = getResponseFromGet(urlString);
        if (DBG) {
            //Log.d(TAG, "getBodyTemperatureReading:\n" + result);
        }

        return getArrayFromData(result);
    }

    public JSONArray getBodyTemperatureReadingByDateRange(String entityId, String startDate, String endDate)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getBodyTemperatureReading"
                + "?EntityId=" + entityId
                + "&StartDay=" + startDate
                + "&EndDay=" + endDate;

        String result = getResponseFromGet(urlString);

        Timber.d(TAG, "getBodyTemperatureReading:\n" + result);


        return getArrayFromData(result);
    }

    public JSONArray getWeighingScaleReading(String entityId, /*LifeCareWeightScaleType type,*/ int pageSize)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getWeighingScaleReading"
                + "?EntityId=" + entityId
                + "&PageSize=" + pageSize;

        String result = getResponseFromGet(urlString);

        Log.d(TAG, "getWeighingScaleReading:\n" + result);

        return getArrayFromData(result);
    }

    public JSONArray getWeighingScaleReadingByDate(String entityId, String date)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getWeighingScaleReading"
                + "?EntityId=" + entityId
                + "&Day=" + date;

        String result = getResponseFromGet(urlString);

        Log.d(TAG, "getWeighingScaleReading:\n" + result);

        return getArrayFromData(result);
    }

    public JSONArray getWeighingScaleReadingByDateRange(String entityId, String startDate, String endDate)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getWeighingScaleReading"
                + "?EntityId=" + entityId
                + "&StartDay=" + startDate
                + "&EndDay=" + endDate;

        String result = getResponseFromGet(urlString);

        Log.d(TAG, "getWeighingScaleReading:\n" + result);

        return getArrayFromData(result);
    }

    public JSONArray getSpo2ReadingByDateRange(String entityId, String startDate, String endDate)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/getSPO2Reading"
                + "?EntityId=" + entityId
                + "&StartDay=" + startDate
                + "&EndDay=" + endDate;

        String result = getResponseFromGet(urlString);

        Log.d(TAG, "getSPO2Reading:\n" + result);

        return getArrayFromData(result);
    }

    public String editAlertRules(FormBody formBody)
    {
        String result = "true";
        String urlString = VitalConfigs.URL + "mlifecare/rule/editRule";
        String res = getResponseFromPost(urlString, formBody);

        Log.w(TAG, "res:" + res);

        JSONObject parentObject;

        try
        {
            parentObject = new JSONObject(res);
            String err = parentObject.getString("Error");

            if (err.equalsIgnoreCase("false"))
            {
                return result;
            }
            else
            {
                result = parentObject.getString("ErrorDesc");
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public JSONArray getAlertParties(String entityId, String ruleId)
    {
        String urlString = VitalConfigs.URL + "mlifecare/rule/getAlertParties?EntityId="
                + entityId + "&RuleId=" + ruleId;
        String result = getResponseFromGet(urlString);

        return getArrayFromData(result);
    }

    public String updateAlertParties(JSONObject urlParameters)
    {
        String result = "true";
        String urlString = VitalConfigs.URL + "mlifecare/rule/updateAlertParties";
        Log.w(TAG, urlParameters.toString());

        String res = getResponseFromPostJSON(urlString, urlParameters);

        JSONObject parentObject;

        try
        {
            parentObject = new JSONObject(res);
            String err = parentObject.getString("Error");

            if (err.equalsIgnoreCase("false"))
            {
                return result;
            }
            else
            {
                result = parentObject.getString("ErrorDesc");
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public JSONArray getKinship()
    {
        String urlString = VitalConfigs.URL + "mlifecare/lookupValue/getKinship";
        String result = getResponseFromGet(urlString);

        return getArrayFromData(result);
    }

    public JSONArray getSupportedBrands()
    {
        String urlString = VitalConfigs.URL + "mlifecare/product/getGatewaySupportedBrands?GatewayType=A";

        String res = getResponseFromGet(urlString);

        Log.w(TAG, "result brand:" + res);
        return getArrayFromData(res);
    }

    public JSONArray getBrandDevices(String gatewayType, String manufacturerId)
    {
        /*
        String urlString = protocol + host + "/mlifecare/product/getGatewaySupportedDevices?" +
                "GatewayType=" + gatewayType + "&ManufacturerId=" + manufacturerId;*/

        String urlString = VitalConfigs.URL + "mlifecare/product/getGatewaySupportedDevices?" +
                "GatewayType=A&ManufacturerId=9GV606QS-P1N8IW74-QCXDRL8U";

        String res = getResponseFromGet(urlString);

        Log.w(TAG, "result device:" + res);

        return getArrayFromData(res);
    }


    public String getBrandDevicesString()
    {
        /*
        String urlString = protocol + host + "/mlifecare/product/getGatewaySupportedDevices?" +
                "GatewayType=" + gatewayType + "&ManufacturerId=" + manufacturerId;*/

        String urlString = "https://www.lifecare.sg/mlifecare/product/getGatewaySupportedDevices?" +
                "GatewayType=A&ManufacturerId=9GV606QS-P1N8IW74-QCXDRL8U";

        String res = getResponseFromGet(urlString);

        Log.w(TAG, "result device:" + res);

        return res;
    }

    public String pairSmartDevice(JSONObject urlParameters)
    {
        String urlString = VitalConfigs.URL + "mlifecare/device/pairSmartDevice";
        String res = getResponseFromPostJSON(urlString, urlParameters);

        return res;
    }

    public String uploadObjectPicture(JSONObject urlParameters,String entityId)
    {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("EntityId",entityId);
            jsonObject.put("File",urlParameters);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"UploadObjectPicture = " + urlParameters + " , " + entityId);
        String urlString = VitalConfigs.URL + "mlifecare/media/uploadObjectPicture";
        String res = getResponseFromPostJSON(urlString,jsonObject);

        Log.d(TAG,"URL = " + urlString);

        return res;
    }

    public String uploadObjectPicture(String param,String entityId)
    {
        FormBody formBody = new FormBody.Builder()
                .add("EntityId",entityId)
                .add("File", param)
                .build();



        Log.d(TAG,"[STRING] UploadObjectPicture = " + param + " , " + entityId);
        String urlString = VitalConfigs.URL + "mlifecare/media/uploadObjectPicture";
        String res = getResponseFromPost(urlString, formBody);

        Log.d(TAG,"URL = " + urlString);
        return res;
    }

    public JSONArray getConnectedDeviceList(String entityId, String gatewayId)
    {
        String urlString = VitalConfigs.URL + "mlifecare/device/getConnectedSmartDevices"
                + "?EntityId=" + entityId + "&GatewayId=" + gatewayId;

        String res = getResponseFromGet(urlString);

        return getArrayFromData(res);
    }

    public JSONArray getGatewayList(String entityId)
    {
        String urlString = VitalConfigs.URL + "mlifecare/device/getConnectedGateways"
                + "?EntityId=" + entityId; //"EHR58JHZ-R5OJ6ZN6-5LV4IYT0";
        Log.v("DevicesFragment", "Entity ID = " + entityId);
        String res = getResponseFromGet(urlString);
        Log.v("DevicesFragment", "gateway res = " + res);
        return getArrayFromData(res);

    }

    public JSONArray getSmartDeviceList(String entityId)
    {
        String urlString = VitalConfigs.URL + "mlifecare/device/getConnectedSmartDevices"
                + "?EntityId=" + entityId; // "3DO11DZ6-G9CP72I9-CF30MBLA"
        Log.v("DevicesFragment", "Entity ID = " + entityId);
        String res = getResponseFromGet(urlString);
        Log.v("DevicesFragment", "smartdevice res = " + res);

        return getArrayFromData(res);

    }
    public boolean unpairSmartDevices(JSONObject urlParameters)
    {
        String result = "true";
        String urlString = VitalConfigs.URL + "mlifecare/device/unpairSmartDevice";

        String res = getResponseFromPostJSON(urlString, urlParameters);

        JSONObject parentObject;

        try
        {
            parentObject = new JSONObject(res);
            String err = parentObject.getString("Error");

            if (err.equalsIgnoreCase("false"))
            {
                return true;
            }
            else
            {
                result = parentObject.getString("ErrorDesc");
                return false;
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }


        return true;
    }

    public boolean updateGlucometerReading(JSONObject urlParams)
    {
        String urlString = VitalConfigs.URL + "mlifecare/event/addEvent";

        String result = getResponseFromPostJSON(urlString, urlParams);
        if (DBG) {
            //Log.d(TAG, "updateGlucometerReading:\n" + result);
        }

        if (result != null) {
            if (!TextUtils.isEmpty(result)) {
                return true;
            }
        }

        return false;
    }

    public String getMyDeviceID(Context context)
    {
        String androidID = android.provider.Settings.Secure.getString(context.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
        if (androidID != null)
        {
            if (androidID.length() < 12) {
                androidID = new String("000000000000" + androidID);
            }

            String mDeviceId = androidID.substring((androidID.length() - 12),
                    androidID.length());

            Log.i("device id ", mDeviceId);
            return mDeviceId;
        }
        return null;
    }

   /* public String getPhoneMACAddress(Context context)
    {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        String macAddress = wInfo.getMacAddress();

        return macAddress;
    }*/

    public String generateUUID()
    {
        UUID uuid = UUID.randomUUID();
        String randomUUID = uuid.toString();

        return randomUUID;
    }

    public String timeElapsedFromCurrent(Context context, Calendar cal) {

        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        DateFormat dateFormat = new SimpleDateFormat("dd MMM");
        Calendar current = Calendar.getInstance();

        boolean sameDay = (current.get(Calendar.DAY_OF_MONTH) == cal.get(Calendar.DAY_OF_MONTH));
        int hours = current.get(Calendar.HOUR_OF_DAY) - cal.get(Calendar.HOUR_OF_DAY);

        String defaultTime = dateFormat.format(cal.getTime()) + " at " + timeFormat.format(cal.getTime());

        if (sameDay && (hours <= 1)) {
            // same day
            int mins = 0;

            //Log.d(TAG, "timeElapsedFromCurrent: hours=" + hours + ", mins=" + mins + ", sameDay=" + sameDay);
            if (hours == 1) {
                mins = current.get(Calendar.MINUTE) + 60 - cal.get(Calendar.MINUTE);
            } else {
                mins = current.get(Calendar.MINUTE) - cal.get(Calendar.MINUTE);
            }

            if (mins == 0) {
                return "few seconds ago";
            } else if (mins == 1) {
                return "1 minute ago";
            } else if (mins > 1 && mins < 60) {
                return mins + " minutes ago";
            } else {
                return defaultTime;
            }
        } else {
            return defaultTime;
        }
    }

    public String timeElapsedFromCurrent(Calendar cal)
    {
        String returning = "";
        Calendar current = Calendar.getInstance();

        SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss");

        //Log.w(TAG, "current:" + df.format(current.getTime()) + "cal:" + df.format(cal.getTime()));

        long timediff = current.getTimeInMillis() - cal.getTimeInMillis();
        int days = (int) (timediff / (1000*60*60*24));
        int hours = (int) ((timediff - (1000*60*60*24*days)) / (1000*60*60));
        int min = (int) (timediff - (1000*60*60*24*days) - (1000*60*60*hours)) / (1000*60);

        //Log.w("time differences", days + " days" + hours + " hours" + min + " min");

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm a");
        String time = timeFormat.format(cal.getTime());
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd MMM yyyy");
        String day = dayFormat.format(cal.getTime());

        if(current.get(Calendar.YEAR) > cal.get(Calendar.YEAR))
        {
            returning = day + " at " + time;
            return returning;
        }
        else
        {
            int diff = current.get(Calendar.HOUR_OF_DAY) - cal.get(Calendar.HOUR_OF_DAY);
            if(min < 1 && diff == 0)
            {
                return "few seconds ago";
            }
            else if(days == 0 && hours == 0 && min >= 1)
            {
                if(min == 1)
                {
                    return "1 minute ago at " + time;
                }
                else
                {
                    returning = min + " minutes ago at " + time;
                }
            }
            else if(days == 0 && hours >= 1)
            {
                if(hours == 1)
                {
                    return "1 hour ago at " + time;
                }
                else
                {
                    returning = hours + " hours ago at " + time;
                }
            }
            else if(days >= 1)
            {

                Calendar yesterday = Calendar.getInstance();
                yesterday.add(Calendar.DAY_OF_YEAR, -1);

                if(yesterday.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
                        && yesterday.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
                        && yesterday.get(Calendar.DATE) == cal.get(Calendar.DATE))
                {
                    returning = "Yesterday at " + time;
                }
                else
                {

                    returning = day + " at " + time;
                }
            }
            else
            {
                returning = day +" at " + time;
            }
        }
        return returning;
    }

    public boolean InstallmyAPK(String apkName,String packageName, Context context)
    {
        boolean installed = appInstalledOrNot(packageName, context);
        if(installed)
        {
            //System.out.println("App is already installed on your phone");
            return true;
        }
        else
        {
            //System.out.println("App is not currently installed on your phone");

            AssetManager assetManager = context.getAssets();
            InputStream in;
            FileOutputStream out;
            File myAPKFile = new File(Environment.getExternalStorageDirectory()
                    .getPath() + "/" + apkName);

            try
            {
                if(!myAPKFile.exists())
                {
                    in = assetManager.open(apkName);
                    out = new FileOutputStream(myAPKFile);

                    byte[] buffer = new byte[1024];
                    int read;
                    while((read = in.read(buffer)) != -1)
                    {
                        out.write(buffer, 0, read);
                    }

                    in.close();
                    in = null;

                    out.flush();
                    out.close();
                    out = null;
                }

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(myAPKFile),
                        "application/vnd.android.package-archive");
                context.startActivity(intent);
                return true;

            } catch(Exception e) {return false;}
        }
    }

    private boolean appInstalledOrNot(String uri, Context context)
    {
        PackageManager pm = context.getPackageManager();
        boolean app_installed;
        try
        {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        }
        catch (PackageManager.NameNotFoundException e)

        {
            app_installed = false;
        }
        return app_installed;
    }

    public static boolean isNumeric(String str)
    {
        try
        {
            double d = Double.parseDouble(str);
        }
        catch(NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }
    public JSONObject getAllStatus(String deviceId)
    {
        String urlString = "https://eyecare.eyeorcas.com/eyeCare/getDeviceStatus.php?DeviceId="
                + deviceId;

        String result = getResponseFromGet(urlString);
        Log.d(TAG, "getAllStatus:\n" + result);

        return getObjectFromData(result);
    }
    public JSONObject getServerIP()
    {
        String urlString = "https://eyecare.eyeorcas.com/getServerIP.php";
        String result = getResponseFromGet(urlString);

        try
        {
            JSONObject data = new JSONObject(result);

            if(data.has("data"))
            {
                Object obj = data.get("data");
                if(obj instanceof JSONArray)
                {
                    JSONArray array = data.getJSONArray("data");
                    if(array != null && array.length() > 0)
                    {
                        JSONObject object = array.getJSONObject(0);
                        Log.w(TAG, "object : " + object.toString());
                        return object;
                    }
                }
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public String uploadPictureToLocalServer(RequestBody requestBody,String entityId)
    {
        String urlString = VitalConfigs.URL + "mobileUploadMedia";
        Request request = new Request.Builder()
                .url(urlString)
                .post(requestBody).build();

        try {
            Response response = okclient.newCall(request).execute();
            String res = response.body().string();

            if (DBG) {
                Log.d(TAG, "getResponseFromPostJSON response:\n" + res);
            }
            response.body().close();

            return res;

        }catch (IOException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }catch (NullPointerException e){
            Timber.e(e.getMessage());
        }

        return "";
    }
/*
    public LifeCareEntity getEntityFromData(JSONObject json_data)
    {
        LifeCareEntity entity = new LifeCareEntity();
        try
        {
            String entityId = json_data.getString("_id");
            if(entityId != null && !entityId.isEmpty())
                entity.entity_id = entityId;

            String fName = json_data.getString("first_name");
            if(fName != null && !fName.isEmpty())
                entity.firstName = fName;

            String lName = json_data.getString("last_name");
            if(lName != null && !lName.isEmpty())
                entity.lastName = lName;

            String name = json_data.getString("name");
            if(name != null && !name.isEmpty() && !name.equalsIgnoreCase("null"))
                entity.name = name;
            else
            {
                entity.name = fName + " " + lName;
            }

            String type = json_data.getString("type");
            if(type != null && !type.isEmpty())
                entity.gender = type;

            String authId = json_data.getString("authentication_string_lower");
            if(authId != null && !authId.isEmpty())
                entity.email = authId;

            entity.deviceInfo = new DeviceInfo();

            Object obj = json_data.get("devices");
            if(obj instanceof JSONArray)
            {
                JSONArray array = json_data.getJSONArray("devices");
                if(array.length() > 0 && array != null)
                {
                    JSONObject dev = array.getJSONObject(0);
                    String deviceId = dev.getString("_id");
                    entity.deviceInfo.deviceID = deviceId;
                }
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return entity;
    }*/
}