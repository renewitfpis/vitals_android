package sg.lifecare.medicare.ui.alarm;

import java.util.Comparator;

import sg.lifecare.medicare.database.model.Reminder;

/**
 * Created by wanping on 17/6/16.
 */
public class ReminderTimeComparator implements Comparator<Reminder>
{
    public int compare(Reminder left, Reminder right) {

        Integer hour1 = left.getHour();
        Integer hour2 = right.getHour();

        Integer min1 = left.getMin();
        Integer min2 = right.getMin();

        if(hour1.compareTo(hour2) == 0)
        {
            return min1.compareTo(min2);
        }
        else
            return hour1.compareTo(hour2);
    }
}