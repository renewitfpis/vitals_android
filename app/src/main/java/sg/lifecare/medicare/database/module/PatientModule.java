package sg.lifecare.medicare.database.module;

import io.realm.annotations.RealmModule;
import sg.lifecare.medicare.database.model.BloodPressure;
import sg.lifecare.medicare.database.model.Medication;
import sg.lifecare.medicare.database.model.Note;
import sg.lifecare.medicare.database.model.PendingReminder;
import sg.lifecare.medicare.database.model.Photo;
import sg.lifecare.medicare.database.model.Reminder;
import sg.lifecare.medicare.database.model.SpO2;
import sg.lifecare.medicare.database.model.SpO2Set;
import sg.lifecare.medicare.database.model.Symptom;
import sg.lifecare.medicare.database.model.Temperature;
import sg.lifecare.medicare.database.model.Terumo;
import sg.lifecare.medicare.database.model.User;
import sg.lifecare.medicare.database.model.Weight;

/**
 *  Realm module for all the storage data
 */
@RealmModule(classes = {User.class, Photo.class, Symptom.class, Terumo.class,
        Reminder.class, PendingReminder.class, Medication.class, BloodPressure.class,
        Weight.class, Temperature.class, Note.class, SpO2.class, SpO2Set.class })
public class PatientModule {
}
