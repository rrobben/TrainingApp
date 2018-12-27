package com.example.android.harjoitus7_8;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Calendar c = Calendar.getInstance();
        EditText dateEdit = getActivity().findViewById(R.id.et_date);

        try {
            if (dateEdit.getText().toString() != "") {
                SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
                Date date = df.parse(dateEdit.getText().toString());
                c.setTime(date);
            }

        } catch (Exception ex) {
            // Do nothing
        }

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, year, month, day);

    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        EditText dateEdit = getActivity().findViewById(R.id.et_date);
        String date = String.valueOf(dayOfMonth) + "." + String.valueOf(month + 1) + "." + String.valueOf(year);
        dateEdit.setText(date);
    }
}
