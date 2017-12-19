package sg.lifecare.medicare.object;

/**
 * Created by ct on 15/5/15.
 */

import com.sony.nfc.DeviceInfo;

import java.io.Serializable;
import java.util.Calendar;

public class LifeCareEntity implements Serializable
{
    public String address;
    public String authentication_id;
    public String authorization_id;
    public String contact;
    public String device_id;
    public DeviceInfo deviceInfo;
    //public LifecareAnalytic analytic;
    public String email;
    public String entity_id;
    public String firstName;
    public String gender;
    public String lastName;
    public String name;
    public String device_type;

    public Calendar lastUpdate;
    public String status;
    public String zone;
    public String seniorPhotoPath;
    //public Phone phone;

    public Calendar dob;

    public String kinshipRelationship;
    public String caregiverId;
    public String caregiverName;
    public String caregiverAuthenticationString;
    public boolean isCaregiverAssisting = false;

    public boolean approved;
    public boolean isSEMAS;
    public boolean isPrimaryElderly;

    public LifeCareEntity()
    {
        address = "";
        authentication_id = "";
        authorization_id = "";
        contact = "";
        device_id = "";
        email = "";
        entity_id = "";
        firstName = "";
        gender = "";
        lastName = "";
        name = "";
        device_type = "";

        status = "";
        zone = "";
        seniorPhotoPath = "";
        //phone = new Phone();

        kinshipRelationship = "";
        caregiverId = "";
        caregiverName = "";

        approved = false;
        isSEMAS = false;
        isPrimaryElderly = false;

        dob = Calendar.getInstance();
    }

    public String getEntityStatus()
    {
        if(this.status.equalsIgnoreCase("A"))
        {
            return "Active";
        }
        else if(this.status.equalsIgnoreCase("I"))
        {
            return "Idle";
        }
        else if(this.status.equalsIgnoreCase("U"))
        {
            return "Unknown";
        }
        else if(this.status.equalsIgnoreCase("W"))
        {
            return "Away";
        }
        else if(this.status.equalsIgnoreCase("L"))
        {
            return "Likely Away";
        }
        else
        {
            return "";
        }
    }
}