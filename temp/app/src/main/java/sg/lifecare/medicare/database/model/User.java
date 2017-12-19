package sg.lifecare.medicare.database.model;

import android.text.TextUtils;

import java.util.Calendar;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by sweelai on 13/6/16.
 */
public class User extends RealmObject {
    @PrimaryKey
    private long id;
    private String gender;
    private String name;
    private String firstName;
    private String lastName;
    private String height;
    private String weight;
    private String year;
    private String email;
    private String entityId;
    private String imgUrl;

    public User() {}

    public long getId() {
        return id;
    }

    public String getGender() {
        return gender;
    }

    public boolean isMale() {
        return "M".equalsIgnoreCase(gender);
    }

    public boolean isFemale() {
        return "F".equalsIgnoreCase(gender);
    }

    public String getName() {
        return name;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getHeight() {
        return height;
    }

    public String getWeight() {
        return weight;
    }

    public String getYear() {
        return year;
    }

    public String getEmail() {
        return email;
    }

    public String getAge() {
        if (!TextUtils.isEmpty(year)) {
            int value = Integer.valueOf(year);
            Calendar cal = Calendar.getInstance();

            return String.valueOf(cal.get(Calendar.YEAR) - value);
        }

        return "";
    }

    public String getEntityId(){
        return entityId;
    }

    public String getImgUrl(){
        return imgUrl;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setMale() {
        gender = "M";
    }

    public void setFemale() {
        gender = "F";
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public void setAge(String age) {
        int value = Integer.valueOf(age);

        Calendar cal = Calendar.getInstance();
        int thisYear = cal.get(Calendar.YEAR);

        int year  = thisYear - value;

        if (year > 1900) {
            setYear(String.valueOf(year));
        }

    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public void setEntityId(String entityId) { this.entityId = entityId; }
}
