package sg.lifecare.medicare.utils;

/**
 * Smartband library
 */
public class DevDecode_X6
{
    public String[] decode_MAC_SN(byte[] data)
    {
        if (data.length < 5) {
            return null;
        }
        if (data[0] != -91) {
            return null;
        }
        int dataLength = data.length - 4;
        if (data[1] != dataLength) {
            return null;
        }

        byte[] crc = CRC_16(data, 2, dataLength);
        if (crc.length != 2) {
            return null;
        }

        if ((crc[0] == data[(data.length - 1)]) && (crc[1] == data[(data.length - 2)])) {
            byte[] devAddress = cutBytes(data, 3, 6);
            devAddress = switchBytes(devAddress);

            byte[] devSN = cutBytes(data, 9, 4);
            devSN = switchBytes(devSN);

            return new String[] { byteToString(devAddress), byteToString(devSN) };
        }

        return null;
    }

    public int[] decode_PersonalInfo(byte[] data, int type)
    {
        if (data.length < 5) {
            return null;
        }
        if (data[0] != -91) {
            return null;
        }
        int dataLength = data.length - 4;
        if (data[1] != dataLength) {
            return null;
        }

        byte[] crc = CRC_16(data, 2, dataLength);
        if (crc.length != 2) {
            return null;
        }

        if ((crc[0] == data[(data.length - 1)]) && (crc[1] == data[(data.length - 2)]))
        {
            if (type == 1) {
                int height = cutBytes(data, 4, 1)[0] & 0xFF;
                int weight = cutBytes(data, 5, 1)[0] & 0xFF;
                int sex = cutBytes(data, 6, 1)[0] & 0xFF;
                int age = cutBytes(data, 7, 1)[0] & 0xFF;

                return new int[] { height, weight, sex, age };
            } else if (type == 2) {
                data = switchBytes(data);
                byte[] stand_bytes = cutBytes(data, 2, 2);
                int stand = bytesToInt2_2Bytes(stand_bytes);

                return new int[] { stand };
            } else if (type == 3) {
                data = switchBytes(data);
                byte[] target_bytes = cutBytes(data, 2, 4);
                int target = byteToInt2_4Bytes(target_bytes);

                return new int[] { target };
            } else if (type == 4) {
                int start_Hour = cutBytes(data, 4, 1)[0];
                int start_Minute = cutBytes(data, 5, 1)[0];
                int end_Hour = cutBytes(data, 6, 1)[0];
                int end_Minute = cutBytes(data, 7, 1)[0];

                return new int[] { start_Hour, start_Minute, end_Hour, end_Minute };
            } else if (type == 5) {
                int disconnectNotify = cutBytes(data, 4, 1)[0];
                int timeType = cutBytes(data, 5, 1)[0];
                int UIType = cutBytes(data, 6, 1)[0];

                return new int[] { disconnectNotify, timeType, UIType };
            } else if (type == 6) {
                int enable = cutBytes(data, 4, 1)[0];
                int start_Hour = cutBytes(data, 5, 1)[0];
                int start_Minute = cutBytes(data, 6, 1)[0];
                int end_Hour = cutBytes(data, 7, 1)[0];
                int end_Minute = cutBytes(data, 8, 1)[0];

                return new int[] { enable, start_Hour, start_Minute, end_Hour, end_Minute };
            } else if (type == 7) {
                int language = cutBytes(data, 4, 1)[0];
                return new int[]{language};
            } else if (type == 8) {
                int orientation = cutBytes(data, 4, 1)[0];
                return new int[]{orientation};
            } else if (type == 9) {
                int handup = cutBytes(data, 4, 1)[0];
                return new int[]{handup};
            }

            return null;
        }

        return null;
    }

    public int[] decode_Date_Time(byte[] data)
    {
        if (data.length < 5) {
            return null;
        }
        if (data[0] != -91) {
            return null;
        }
        int dataLength = data.length - 4;
        if (data[1] != dataLength) {
            return null;
        }

        byte[] crc = CRC_16(data, 2, dataLength);
        if (crc.length != 2) {
            return null;
        }

        if ((crc[0] == data[(data.length - 1)]) && (crc[1] == data[(data.length - 2)]))
        {
            byte year01 = cutBytes(data, 3, 1)[0];
            byte year02 = cutBytes(data, 4, 1)[0];

            int year = bytesToInt2_2Bytes(new byte[] { year01, year02 });

            int month = cutBytes(data, 5, 1)[0];
            int day = cutBytes(data, 6, 1)[0];
            int hour = cutBytes(data, 7, 1)[0];
            int minute = cutBytes(data, 8, 1)[0];
            int second = cutBytes(data, 9, 1)[0];
            int week = cutBytes(data, 10, 1)[0];

            return new int[] { year, month, day, hour, minute, second, week };
        }

        return null;
    }

    public int[] decode_AlarmClock(byte[] data)
    {
        if (data.length < 5) {
            return null;
        }
        if (data[0] != -91) {
            return null;
        }
        int dataLength = data.length - 4;
        if (data[1] != dataLength) {
            return null;
        }

        byte[] crc = CRC_16(data, 2, dataLength);
        if (crc.length != 2) {
            return null;
        }

        if ((crc[0] == data[(data.length - 1)]) && (crc[1] == data[(data.length - 2)]))
        {
            int ID = cutBytes(data, 3, 1)[0];
            int type = cutBytes(data, 4, 1)[0];
            int enable = cutBytes(data, 5, 1)[0];
            int hour = cutBytes(data, 6, 1)[0];
            int minute = cutBytes(data, 7, 1)[0];
            int remindTime = cutBytes(data, 8, 1)[0];

            weekTransTo(enable);

            return new int[] { ID, type, enable, hour, minute, remindTime };
        }

        return null;
    }

    public int decode_CurrentValue_Auto(byte[] data)
    {
        if (data == null) {
            return 0;
        }

        data = switchBytes(data);

        int steps = byteToInt2_4Bytes(data);
        return steps;
    }

    public int[] decode_CurrentValue(byte[] data)
    {
        if (data.length < 5) {
            return null;
        }
        if (data[0] != -91) {
            return null;
        }
        int dataLength = data.length - 4;
        if (data[1] != dataLength) {
            return null;
        }

        byte[] crc = CRC_16(data, 2, dataLength);
        if (crc.length != 2) {
            return null;
        }

        if ((crc[0] == data[(data.length - 1)]) && (crc[1] == data[(data.length - 2)]))
        {
            int steps_int = 0; int calories_int = 0; int distances_int = 0;

            byte[] steps = cutBytes(data, 4, 4);
            byte[] calories = cutBytes(data, 8, 4);
            byte[] distances = cutBytes(data, 12, 4);

            steps = switchBytes(steps);
            calories = switchBytes(calories);
            distances = switchBytes(distances);

            steps_int = byteToInt2_4Bytes(steps);
            calories_int = byteToInt2_4Bytes(calories);
            distances_int = byteToInt2_4Bytes(distances);

            return new int[] { steps_int, distances_int, calories_int };
        }
        return null;
    }

    public int[][] decode_HistoryRecodeDate(byte[] data, int length)
    {
        if (data.length < 5) {
            return null;
        }
        if (data[0] != -91) {
            return null;
        }
        int dataLength = length - 4;

        if (data[1] != dataLength) {
            return null;
        }

        byte[] crc = CRC_16(data, 2, dataLength);
        if (crc.length != 2) {
            return null;
        }
        if ((crc[0] == data[(length - 1)]) && (crc[1] == data[(length - 2)]))
        {
            int[][] blockData = new int[7][4];

            byte[][] blockData_Byte = new byte[7][5];

            for (int i = 0; i * 5 + 8 + 1 < length; i++) {
                int n = i * 5 + 3;
                blockData_Byte[i] = cutBytes(data, n, 5);
            }

            for (int i = 0; i < blockData_Byte.length; i++) {
                byte temp = blockData_Byte[i][1];
                blockData_Byte[i][1] = blockData_Byte[i][2];
                blockData_Byte[i][1] = temp;
                blockData[i][0] = blockData_Byte[i][0];
                blockData[i][1] = bytesToInt2_2Bytes(new byte[] { blockData_Byte[i][2], blockData_Byte[i][1] });
                blockData[i][2] = blockData_Byte[i][3];
                blockData[i][3] = blockData_Byte[i][4];
            }

            return blockData;
        }
        return null;
    }

    public int[][] decode_HistoryRecodeDatail(byte[] data)
    {
        if (data.length < 67) {
            return null;
        }

        if (data[0] != -91) {
            return null;
        }
        int dataLength = data.length - 4;

        if (data[1] != dataLength) {
            return null;
        }

        byte[] crc = CRC_16(data, 2, dataLength);

        if (crc.length != 2) {
            return null;
        }

        if ((crc[0] == data[(data.length - 1)]) && (crc[1] == data[(data.length - 2)]))
        {
            int[][] steps = new int[31][2];

            steps[0][0] = cutBytes(data, 4, 1)[0];

            for (int i = 1; i < steps.length; i++) {
                byte[] stepsData = cutBytes(data, (i + 1) * 2 + 1, 2);

                steps[i] = separateData(stepsData);

                if (steps[i][1] == 4095) {
                    steps[i][0] = -1;
                    steps[i][1] = 0;
                } else if (steps[i][1] == 3840) {
                    steps[i][0] = -1;
                    steps[i][1] = 0;
                }
            }

            return steps;
        }
        android.util.Log.d("decode", "test 5");
        return null;
    }

    public int[] getHistoryDistance(int[] historySteps, int userHeight)
    {
        if (historySteps == null) {
            return null;
        }

        int STRIDE_FACTOR = 415;

        int history_Counts = historySteps.length;
        int[] historyDistance = new int[history_Counts];
        try {
            for (int i = 0; i < history_Counts; i++)
            {
                historyDistance[i] = (historySteps[i] * userHeight / 241);
            }

            return historyDistance;
        } catch (Exception e) {
            System.out.println("异常");
        }return null;
    }

    public int[] getHistoryCalories(int[] historyDistan, int userWeight)
    {
        if (historyDistan == null) {
            return null;
        }
        int history_Counts = historyDistan.length;
        int[] historyCalories = new int[history_Counts];
        try {
            for (int i = 0; i < history_Counts; i++)
            {
                historyCalories[i] = (userWeight * historyDistan[i] / 965);
            }

            return historyCalories;
        } catch (Exception e) {
            System.out.println("异常");
        }return null;
    }

    public static int[] separateData(byte[] res)
    {
        int[] targets = new int[2];

        byte temp_Type = res[1];
        byte temp_Step = res[0];

        targets[0] = (temp_Type >>> 4 & 0xF);

        res[1] = ((byte)(res[1] & 0xF));

        targets[1] = bytesToInt2_2Bytes(new byte[] { res[1], temp_Step });

        return targets;
    }

    public static int[] weekTransTo(int week)
    {
        try
        {
            int[] intweekclock = new int[8];
            byte temp = (byte)week;

            for (int j = 0; j < 8; j++)
            {
                intweekclock[(7 - j)] = ((temp & (int)Math.pow(2.0D, 7 - j)) >>> 7 - j);
            }

            return intweekclock; } catch (Exception e) {
        }
        return null;
    }

    private byte[] CRC_16(byte[] data, int start, int length)
    {
        try
        {
            short crc_result = 0;
            int Poly = 4129;
            for (int i = start; i < start + length; i++) {
                for (int j = 128; j != 0; j >>= 1) {
                    if ((crc_result & 0x8000) != 0) {
                        crc_result = (short)(crc_result << 1);
                        crc_result = (short)(crc_result ^ Poly);
                    } else {
                        crc_result = (short)(crc_result << 1);
                    }
                    if ((data[i] & j) != 0) {
                        crc_result = (short)(crc_result ^ Poly);
                    }
                }
            }

            return short2bytes(crc_result);
        }
        catch (Exception localException) {
        }
        return short2bytes((short)-1);
    }

    private byte[] short2bytes(short s) {
        byte[] bytes = new byte[2];
        for (int i = 1; i >= 0; i--) {
            bytes[i] = ((byte)(s % 256));
            s = (short)(s >> 8);
        }
        return bytes;
    }

    public static byte[] switchBytes(byte[] data)
    {
        try
        {
            int length = data.length;
            byte[] data_temp = new byte[length];

            for (int i = 0; i < length; i++) {
                data_temp[i] = data[(length - i - 1)];
            }

            return data_temp;
        } catch (Exception e) {
        }
        return null;
    }

    public static byte[] cutBytes(byte[] data, int start, int length)
    {
        byte[] data_temp = new byte[length];
        for (int i = 0; i < length; i++) {
            data_temp[i] = data[(start + i)];
        }
        return data_temp;
    }

    public static int byteToInt2_4Bytes(byte[] b)
    {
        int mask = 255;
        int temp = 0;
        int n = 0;
        for (int i = 0; i < 4; i++) {
            n <<= 8;
            temp = b[i] & mask;
            n |= temp;
        }
        return n & 0xFFFFFFFF;
    }

    public static int bytesToInt2_2Bytes(byte[] src)
    {
        int value = (src[0] & 0xFF) << 8 | src[1] & 0xFF;
        return value;
    }

    public static String byteToString(byte[] data)
    {
        StringBuilder stringBuilder = new StringBuilder(data.length);
        byte[] arrayOfByte = data; int j = data.length; for (int i = 0; i < j; i++) { byte byteChar = arrayOfByte[i];
        stringBuilder.append(String.format("%02X ", new Object[] { Byte.valueOf(byteChar) }).toString());
    }
        return stringBuilder.toString();
    }
}
