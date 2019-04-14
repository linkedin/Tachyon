/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.tachyon.sample;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;

import com.linkedin.android.tachyon.DayView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.LongSparseArray;

/**
 * This sample activity demonstrates how to populate the day view with events.
 */
public class SampleActivity extends AppCompatActivity {

    /**
     * Some examples to demonstrate how the day view renders multiple events that are in close
     * proximity to each other.
     */
    @NonNull
    private static final Event[] INITIAL_EVENTS = {
            new Event("Walk the dog", "Park", 0, 0, 30, android.R.color.holo_red_dark),
            new Event("Meeting", "Office", 1, 30, 90, android.R.color.holo_purple),
            new Event("Phone call", "555-5555", 2, 0, 45, android.R.color.holo_orange_dark),
            new Event("Lunch", "Cafeteria", 2, 30, 30, android.R.color.holo_green_dark),
            new Event("Dinner", "Home", 18, 0, 30, android.R.color.holo_green_dark)};

    private Calendar day;
    private LongSparseArray<List<Event>> allEvents;
    private DateFormat dateFormat;
    private DateFormat timeFormat;
    private Calendar editEventDate;
    private Calendar editEventStartTime;
    private Calendar editEventEndTime;
    private Event editEventDraft;

    private ViewGroup content;
    private TextView dateTextView;
    private ScrollView scrollView;
    private DayView dayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a new calendar object set to the start of today
        day = Calendar.getInstance();
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);

        // Populate today's entry in the map with a list of example events
        allEvents = new LongSparseArray<>();
        allEvents.put(day.getTimeInMillis(), new ArrayList<>(Arrays.asList(INITIAL_EVENTS)));

        dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());

        setContentView(R.layout.sample_activity);

        content = findViewById(R.id.sample_content);
        dateTextView = findViewById(R.id.sample_date);
        scrollView = findViewById(R.id.sample_scroll);
        dayView = findViewById(R.id.sample_day);

        // Inflate a label view for each hour the day view will display
        Calendar hour = (Calendar) day.clone();
        List<View> hourLabelViews = new ArrayList<>();
        for (int i = 0; i < DayView.HOUR_LABELS_COUNT; i++) {
            hour.set(Calendar.HOUR_OF_DAY, i);

            TextView hourLabelView = (TextView) getLayoutInflater().inflate(R.layout.hour_label, dayView, false);
            hourLabelView.setText(timeFormat.format(hour.getTime()));
            hourLabelViews.add(hourLabelView);
        }
        dayView.setHourLabelViews(hourLabelViews);

        onDayChange();
    }

    public void onPreviousClick(View v) {
        day.add(Calendar.DAY_OF_YEAR, -1);
        onDayChange();
    }

    public void onNextClick(View v) {
        day.add(Calendar.DAY_OF_YEAR, 1);
        onDayChange();
    }

    public void onAddEventClick(View v) {
        editEventDate = (Calendar) day.clone();

        editEventStartTime = (Calendar) day.clone();

        editEventEndTime = (Calendar) day.clone();
        editEventEndTime.add(Calendar.MINUTE, 30);

        showEditEventDialog(false, null, null, android.R.color.holo_red_dark);
    }

    public void onScrollClick(View v) {
        showScrollTargetDialog();
    }

    private void onDayChange() {
        dateTextView.setText(dateFormat.format(day.getTime()));
        onEventsChange();
    }

    private void onEventsChange() {
        // The day view needs a list of event views and a corresponding list of event time ranges
        List<View> eventViews = null;
        List<DayView.EventTimeRange> eventTimeRanges = null;
        List<Event> events = allEvents.get(day.getTimeInMillis());

        if (events != null) {
            // Sort the events by start time so the layout happens in correct order
            Collections.sort(events, new Comparator<Event>() {
                @Override
                public int compare(Event o1, Event o2) {
                    return o1.hour < o2.hour ? -1 : (o1.hour == o2.hour ? (o1.minute < o2.minute ? -1 : (o1.minute == o2.minute ? 0 : 1)) : 1);
                }
            });

            eventViews = new ArrayList<>();
            eventTimeRanges = new ArrayList<>();

            // Reclaim all of the existing event views so we can reuse them if needed, this process
            // can be useful if your day view is hosted in a recycler view for example
            List<View> recycled = dayView.removeEventViews();
            int remaining = recycled != null ? recycled.size() : 0;

            for (final Event event : events) {
                // Try to recycle an existing event view if there are enough left, otherwise inflate
                // a new one
                View eventView = remaining > 0 ? recycled.get(--remaining) : getLayoutInflater().inflate(R.layout.event, dayView, false);

                ((TextView) eventView.findViewById(R.id.event_title)).setText(event.title);
                ((TextView) eventView.findViewById(R.id.event_location)).setText(event.location);
                eventView.setBackgroundColor(getResources().getColor(event.color));

                // When an event is clicked, start a new draft event and show the edit event dialog
                eventView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editEventDraft = event;

                        editEventDate = (Calendar) day.clone();

                        editEventStartTime = Calendar.getInstance();
                        editEventStartTime.set(Calendar.HOUR_OF_DAY, editEventDraft.hour);
                        editEventStartTime.set(Calendar.MINUTE, editEventDraft.minute);
                        editEventStartTime.set(Calendar.SECOND, 0);
                        editEventStartTime.set(Calendar.MILLISECOND, 0);

                        editEventEndTime = (Calendar) editEventStartTime.clone();
                        editEventEndTime.add(Calendar.MINUTE, editEventDraft.duration);

                        showEditEventDialog(true, editEventDraft.title, editEventDraft.location, editEventDraft.color);
                    }
                });

                eventViews.add(eventView);

                // The day view needs the event time ranges in the start minute/end minute format,
                // so calculate those here
                int startMinute = 60 * event.hour + event.minute;
                int endMinute = startMinute + event.duration;
                eventTimeRanges.add(new DayView.EventTimeRange(startMinute, endMinute));
            }
        }

        // Update the day view with the new events
        dayView.setEventViews(eventViews, eventTimeRanges);
    }

    private void showEditEventDialog(boolean eventExists, @Nullable String eventTitle, @Nullable String eventLocation, @ColorRes int eventColor) {
        View view = getLayoutInflater().inflate(R.layout.edit_event_dialog, content, false);
        final TextView titleTextView = view.findViewById(R.id.edit_event_title);
        final TextView locationTextView = view.findViewById(R.id.edit_event_location);
        final Button dateButton = view.findViewById(R.id.edit_event_date);
        final Button startTimeButton = view.findViewById(R.id.edit_event_start_time);
        final Button endTimeButton = view.findViewById(R.id.edit_event_end_time);
        final RadioButton redRadioButton = view.findViewById(R.id.edit_event_red);
        final RadioButton blueRadioButton = view.findViewById(R.id.edit_event_blue);
        final RadioButton orangeRadioButton = view.findViewById(R.id.edit_event_orange);
        final RadioButton greenRadioButton = view.findViewById(R.id.edit_event_green);
        final RadioButton purpleRadioButton = view.findViewById(R.id.edit_event_purple);

        titleTextView.setText(eventTitle);
        locationTextView.setText(eventLocation);

        dateButton.setText(dateFormat.format(editEventDate.getTime()));
        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        editEventDate.set(Calendar.YEAR, year);
                        editEventDate.set(Calendar.MONTH, month);
                        editEventDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        dateButton.setText(dateFormat.format(editEventDate.getTime()));
                    }
                };

                new DatePickerDialog(SampleActivity.this, listener, day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH)).show();

            }
        });

        startTimeButton.setText(timeFormat.format(editEventStartTime.getTime()));
        startTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        editEventStartTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        editEventStartTime.set(Calendar.MINUTE, minute);

                        startTimeButton.setText(timeFormat.format(editEventStartTime.getTime()));

                        if (!editEventEndTime.after(editEventStartTime)) {
                            editEventEndTime = (Calendar) editEventStartTime.clone();
                            editEventEndTime.add(Calendar.MINUTE, 30);

                            endTimeButton.setText(timeFormat.format(editEventEndTime.getTime()));
                        }
                    }
                };

                new TimePickerDialog(SampleActivity.this, listener, editEventStartTime.get(Calendar.HOUR_OF_DAY), editEventStartTime.get(Calendar.MINUTE), android.text.format.DateFormat.is24HourFormat(SampleActivity.this)).show();

            }
        });

        endTimeButton.setText(timeFormat.format(editEventEndTime.getTime()));
        endTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        editEventEndTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        editEventEndTime.set(Calendar.MINUTE, minute);

                        if (!editEventEndTime.after(editEventStartTime)) {
                            editEventEndTime = (Calendar) editEventStartTime.clone();
                            editEventEndTime.add(Calendar.MINUTE, 30);
                        }

                        endTimeButton.setText(timeFormat.format(editEventEndTime.getTime()));
                    }
                };

                new TimePickerDialog(SampleActivity.this, listener, editEventEndTime.get(Calendar.HOUR_OF_DAY), editEventEndTime.get(Calendar.MINUTE), android.text.format.DateFormat.is24HourFormat(SampleActivity.this)).show();

            }
        });

        if (eventColor == android.R.color.holo_blue_dark) {
            blueRadioButton.setChecked(true);
        } else if (eventColor == android.R.color.holo_orange_dark) {
            orangeRadioButton.setChecked(true);
        } else if (eventColor == android.R.color.holo_green_dark) {
            greenRadioButton.setChecked(true);
        } else if (eventColor == android.R.color.holo_purple) {
            purpleRadioButton.setChecked(true);
        } else {
            redRadioButton.setChecked(true);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // If the event already exists, we are editing it, otherwise we are adding a new event
        builder.setTitle(eventExists ? R.string.edit_event : R.string.add_event);

        // When the event changes are confirmed, read the new values from the dialog and then add
        // this event to the list
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                List<Event> events = allEvents.get(editEventDate.getTimeInMillis());
                if (events == null) {
                    events = new ArrayList<>();
                    allEvents.put(editEventDate.getTimeInMillis(), events);
                }

                String title = titleTextView.getText().toString();
                String location = locationTextView.getText().toString();
                int hour = editEventStartTime.get(Calendar.HOUR_OF_DAY);
                int minute = editEventStartTime.get(Calendar.MINUTE);
                int duration = (int) (editEventEndTime.getTimeInMillis() - editEventStartTime.getTimeInMillis()) / 60000;

                @ColorRes int color;
                if (blueRadioButton.isChecked()) {
                    color = android.R.color.holo_blue_dark;
                } else if (orangeRadioButton.isChecked()) {
                    color = android.R.color.holo_orange_dark;
                } else if (greenRadioButton.isChecked()) {
                    color = android.R.color.holo_green_dark;
                } else if (purpleRadioButton.isChecked()) {
                    color = android.R.color.holo_purple;
                } else {
                    color = android.R.color.holo_red_dark;
                }

                events.add(new Event(title, location, hour, minute, duration, color));

                onEditEventDismiss(true);
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onEditEventDismiss(false);
            }
        });

        // If the event already exists, provide a delete option
        if (eventExists) {
            builder.setNeutralButton(R.string.edit_event_delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onEditEventDismiss(true);
                }
            });
        }

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                onEditEventDismiss(false);
            }
        });
        builder.setView(view);
        builder.show();
    }

    private void showScrollTargetDialog() {
        View view = getLayoutInflater().inflate(R.layout.scroll_target_dialog, content, false);
        final Button timeButton = view.findViewById(R.id.scroll_target_time);
        final Button firstEventTopButton = view.findViewById(R.id.scroll_target_first_event_top);
        final Button firstEventBottomButton = view.findViewById(R.id.scroll_target_first_event_bottom);
        final Button lastEventTopButton = view.findViewById(R.id.scroll_target_last_event_top);
        final Button lastEventBottomButton = view.findViewById(R.id.scroll_target_last_event_bottom);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.scroll_to);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setView(view);

        final AlertDialog dialog = builder.show();

        timeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        int top = dayView.getHourTop(hourOfDay);
                        int bottom = dayView.getHourBottom(hourOfDay);
                        int y = top + (bottom - top) * minute / 60;
                        scrollView.smoothScrollTo(0, y);
                        dialog.dismiss();
                    }
                };

                new TimePickerDialog(SampleActivity.this, listener, 0, 0, android.text.format.DateFormat.is24HourFormat(SampleActivity.this)).show();

            }
        });

        firstEventTopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollView.smoothScrollTo(0, dayView.getFirstEventTop());
                dialog.dismiss();
            }
        });

        firstEventBottomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollView.smoothScrollTo(0, dayView.getFirstEventBottom());
                dialog.dismiss();
            }
        });

        lastEventTopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollView.smoothScrollTo(0, dayView.getLastEventTop());
                dialog.dismiss();
            }
        });

        lastEventBottomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollView.smoothScrollTo(0, dayView.getLastEventBottom());
                dialog.dismiss();

            }
        });
    }

    private void onEditEventDismiss(boolean modified) {
        if (modified && editEventDraft != null) {
            List<Event> events = allEvents.get(day.getTimeInMillis());
            if (events != null) {
                events.remove(editEventDraft);
            }
        }
        editEventDraft = null;

        onEventsChange();
    }

    /**
     * A data class used to represent an event on the calendar.
     */
    private static class Event {
        @Nullable
        private final String title;
        @Nullable
        private final String location;
        private final int hour;
        private final int minute;
        private final int duration;
        @ColorRes
        private final int color;

        private Event(@Nullable String title, @Nullable String location, int hour, int minute, int duration, @ColorRes int color) {
            this.title = title;
            this.location = location;
            this.hour = hour;
            this.minute = minute;
            this.duration = duration;
            this.color = color;
        }
    }
}
