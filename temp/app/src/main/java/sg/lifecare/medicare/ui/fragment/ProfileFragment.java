package sg.lifecare.medicare.ui.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import io.realm.Realm;
import sg.lifecare.medicare.R;
import sg.lifecare.medicare.database.PatientData;
import sg.lifecare.medicare.database.model.User;
import sg.lifecare.medicare.ui.DashboardActivity;
import sg.lifecare.medicare.ui.view.CustomToolbar;
import sg.lifecare.medicare.utils.CircleTransform;
import sg.lifecare.medicare.utils.LifeCareHandler;
import timber.log.Timber;

/**
 * Profile fragment
 */
public class ProfileFragment extends Fragment {

    public static final String TAG = "ProfileFragment";

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    private ImageButton maleBtn,femaleBtn;
    private RelativeLayout maleLayout, femaleLayout;
    private EditText editFirstName,editLastName,editAge,editHeight,editWeight;
    private ImageView profile_img;
    private User mUser;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Timber.tag("ProfileFragment");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profile_img = (ImageView) view.findViewById(R.id.profileImage);

        maleBtn      = (ImageButton)  view.findViewById(R.id.btnMale);
        femaleBtn    = (ImageButton)  view.findViewById(R.id.btnFemale);
        maleLayout   = (RelativeLayout) view.findViewById(R.id.layout_male);
        femaleLayout = (RelativeLayout) view.findViewById(R.id.layout_female);

        editFirstName = (EditText) view.findViewById(R.id.edit_first_name);
        editLastName  = (EditText) view.findViewById(R.id.edit_last_name);
        editAge    = (EditText) view.findViewById(R.id.edit_age);
        editHeight = (EditText) view.findViewById(R.id.edit_height);
        editWeight = (EditText) view.findViewById(R.id.edit_weight);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("ON RESUME");
        Realm realm = Realm.getDefaultInstance();
        String weight = PatientData.getInstance().updateUserLatestWeightProfile(realm,mUser.getEntityId());
        realm.close();

        if(editWeight!=null){
            editWeight.setText(weight);
            Timber.d("SET WEIGHT " + mUser.getWeight());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentActivity parent = getActivity();
        if (parent instanceof DashboardActivity) {
            ((DashboardActivity)parent).setToolbar(R.string.title_update_profile, R.drawable.ic_toolbar_back,
                R.drawable.ic_toolbar_tick);
            ((DashboardActivity)parent).setToolbarListener(mToolbarListener);

            mUser = ((DashboardActivity)parent).getUser();
        }

        if(mUser.getImgUrl()!=null && !mUser.getImgUrl().isEmpty()){
            Timber.d("not empty " + mUser.getImgUrl());
            Picasso.with(getActivity()).load(mUser.getImgUrl())
                    .transform(new CircleTransform())
                    .placeholder(mUser.isFemale()?R.drawable.ic_female_large:R.drawable.ic_male_large)
                    .into(profile_img);
        }else {
            Timber.d(" empty");
        }
        editFirstName.setText(mUser.getFirstName());
        editLastName.setText(mUser.getLastName());
        editAge.setText(mUser.getAge());
        editHeight.setText(mUser.getHeight());
        editWeight.setText(mUser.getWeight());

        genderSelection();

        maleLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUser.setMale();
                genderSelection();
            }
        });

        femaleLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUser.setFemale();
                genderSelection();
            }
        });
    }

    private void genderSelection(){
        if(mUser.isMale()){
            femaleBtn.setVisibility(View.GONE);
            maleBtn.setVisibility(View.VISIBLE);
            if(mUser.getImgUrl()==null || mUser.getImgUrl().isEmpty()) {
                profile_img.setImageResource(R.drawable.ic_male_large);
            }
        } else {
            maleBtn.setVisibility(View.GONE);
            femaleBtn.setVisibility(View.VISIBLE);
            if(mUser.getImgUrl()==null || mUser.getImgUrl().isEmpty()) {
                profile_img.setImageResource(R.drawable.ic_female_large);
            }
        }
    }

    private class UpdateUserProfileToServer extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {
            try {
                JSONObject data = new JSONObject();

                int yearOfBirth = Calendar.getInstance().get(Calendar.YEAR) - Integer.valueOf(mUser.getAge());

                data.put("FirstName", mUser.getFirstName());
                data.put("LastName", mUser.getLastName());
                data.put("EntityId", mUser.getEntityId());
                data.put("YearOfBirth", yearOfBirth);
                data.put("Gender", mUser.isFemale()?"F":"M");
                data.put("Height", mUser.getHeight());

                String jsonString = data.toString();

                Log.w(TAG, "updating user prof : " + jsonString);
                String res = LifeCareHandler.getInstance().editSeniorProfile(data);
                Timber.d("RESPONSE of updating user prof = " + res);
            } catch(JSONException ex) {
                Log.e(TAG, ex.getMessage(), ex);
            } catch(NumberFormatException e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void res){

        }

    }

    private CustomToolbar.OnToolbarClickListener mToolbarListener = new CustomToolbar.OnToolbarClickListener() {
        @Override
        public void leftButtonClick() {
            getActivity().onBackPressed();
        }

        @Override public void rightButtonClick() {

            String firstName = editFirstName.getText().toString().trim();
            String lastName = editLastName.getText().toString().trim();
            String age = editAge.getText().toString().trim();
            String height = editHeight.getText().toString().trim();
            String weight = editWeight.getText().toString().trim();

            if(firstName.isEmpty()) {
                editFirstName.setError("First name is required!");
            }
            else if(lastName.isEmpty()) {
                editLastName.setError("Last name is required!");
            }
            else if(age.isEmpty()) {
                editAge.setError("Age is required!");
            }
            else if(height.isEmpty()) {
                editHeight.setError("Height is required!");
            }
            /*else if(weight.isEmpty()) {
                editWeight.setError("Weight is required!");
            }*/
            else {
                mUser.setFirstName(firstName);
                mUser.setLastName(lastName);
                mUser.setName(firstName + " " + lastName);
                mUser.setHeight(height);
                //mUser.setWeight(weight);
                mUser.setAge(age);
                Timber.d("Updated: " + firstName + ", " + height + ", "+ weight +", " + age);
                Timber.d("Updated weight: " + mUser.getWeight());
                new UpdateUserProfileToServer().execute();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Realm realm = Realm.getDefaultInstance();
                        try {
                            realm.beginTransaction();
                            realm.copyToRealmOrUpdate(mUser);
                            realm.commitTransaction();
                        }finally{
                            realm.close();
                        }
                        ((DashboardActivity)getActivity()).setupDrawerProfile();

                        //Close virtual keyboard
                        View view = getActivity().getCurrentFocus();
                        if (view != null) {
                            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }
                        getActivity().onBackPressed();
                    }
                });
            }

        }

        @Override public void secondRightButtonClick() {

        }
    };

}
