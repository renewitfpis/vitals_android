package sg.lifecare.medicare.object;

import java.io.Serializable;

/**
 * Created by ct on 13/1/16.
 */
public class MeasurementDevice implements Serializable
{
    public String brand;
    public String brandName;
    public String deviceName;
    public String deviceType;
    public String deviceDesc;

    public String manufacturerCode;
    public String manufacturerId;
    public String systemProductCode;
    public String systemProductTypeCode;
    public String productId;

    public String assignedDeviceName;
    public String pairedDeviceId;

    public MeasurementDevice()
    {
        this.brand = "";
        this.brandName = "";
        deviceName = "";
        deviceType = "";
        deviceDesc = "";

        manufacturerCode = "";
        manufacturerId = "";
        systemProductCode = "";
        systemProductTypeCode = "";
        productId = "";

        assignedDeviceName = "";
        pairedDeviceId = "";
    }
}
