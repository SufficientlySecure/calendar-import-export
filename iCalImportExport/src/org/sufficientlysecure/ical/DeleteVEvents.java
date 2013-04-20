package org.sufficientlysecure.ical;

import java.util.List;

import org.sufficientlysecure.ical.tools.dialogs.DialogTools;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.component.VEvent;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class DeleteVEvents extends ProcessVEvent {

    public DeleteVEvents(Activity activity, Calendar calendar, int calendarId) {
        super(activity, calendar, calendarId);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void run(ProgressDialog dialog) {
        if (!DialogTools.decisionDialog(getActivity(), R.string.dialog_information_title,
                R.string.dialog_delete_entries, R.string.dialog_yes, R.string.dialog_no,
                R.drawable.calendar)) {
            return;
        }
        setProgressMessage(R.string.progress_deleting_calendarentries);
        ComponentList vevents = getCalendar().getComponents(VEvent.VEVENT);

        dialog.setMax(vevents.size());

        int i = 0;
        ContentResolver contentResolver = getActivity().getContentResolver();
        for (Object event : vevents) {
            ContentValues values = VEventWrapper.resolve((VEvent) event, getCalendarId());
            List<Integer> ids = getIds(values);

            for (Integer id : ids) {
                i += contentResolver.delete(
                        Uri.withAppendedPath(VEventWrapper.getContentURI(), Integer.toString(id)),
                        null, null);
                // Delete reminder
                contentResolver.delete(Reminder.getContentURI(),
                        CalendarContract.Reminders.EVENT_ID + " = ?",
                        new String[] { Integer.toString(id) });
            }
            incrementProgress(1);
        }

        DialogTools.showInformationDialog(getActivity(),
                getActivity().getString(R.string.dialog_information_title), getActivity()
                        .getString(R.string.dialog_entries_deleted, i), R.drawable.calendar);
    }
}
