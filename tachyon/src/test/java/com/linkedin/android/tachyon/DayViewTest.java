/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.tachyon;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class DayViewTest {
    private static final int DIVIDER_HEIGHT = 7;
    private static final int HALF_HOUR_HEIGHT = 28;
    private static final int HOUR_LABEL_MARGIN_END = 17;
    private static final int EVENT_MARGIN = 3;
    private static final float MINUTE_HEIGHT = HALF_HOUR_HEIGHT / 30f;
    private static final int PARENT_WIDTH = 200;

    @Mock
    Context context;
    @Mock
    AttributeSet attrs;
    @Mock
    TypedArray array;
    @Mock
    View hourLabelView;
    @Mock
    View eventView;

    private DayView dayView;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(context.obtainStyledAttributes(attrs, R.styleable.DayView)).thenReturn(array);
        when(array.getDimensionPixelSize(R.styleable.DayView_dividerHeight, 0)).thenReturn(
                DIVIDER_HEIGHT);
        when(array.getDimensionPixelSize(R.styleable.DayView_halfHourHeight, 0)).thenReturn(
                HALF_HOUR_HEIGHT);
        when(array.getDimensionPixelSize(R.styleable.DayView_hourLabelMarginEnd, 0)).thenReturn(
                HOUR_LABEL_MARGIN_END);
        when(array.getDimensionPixelSize(R.styleable.DayView_eventMargin, 0)).thenReturn(EVENT_MARGIN);
        when(array.getInt(R.styleable.DayView_startHour, DayView.MIN_START_HOUR)).thenReturn(DayView.MIN_START_HOUR);
        when(array.getInt(R.styleable.DayView_endHour, DayView.MAX_END_HOUR)).thenReturn(DayView.MAX_END_HOUR);

        when(hourLabelView.getMeasuredWidth()).thenReturn(50);
        when(hourLabelView.getMeasuredHeight()).thenReturn(20);

        dayView = new DayView(context, attrs, 0, false);

        List<View> hourLabelViews = new ArrayList<>();
        for (int i = dayView.getStartHour(); i <= dayView.getEndHour(); i++) {
            hourLabelViews.add(hourLabelView);
        }

        List<DayView.EventTimeRange> eventTimeRanges = new ArrayList<>();
        eventTimeRanges.add(new DayView.EventTimeRange(30, 180));
        eventTimeRanges.add(new DayView.EventTimeRange(90, 120));
        eventTimeRanges.add(new DayView.EventTimeRange(150, 300));
        eventTimeRanges.add(new DayView.EventTimeRange(150, 300));

        List<View> eventViews = new ArrayList<>();
        for (int i = 0; i < eventTimeRanges.size(); i++) {
            eventViews.add(eventView);
        }

        List<DirectionalRect> eventRects = new ArrayList<>();
        for (int i = 0; i < eventViews.size(); i++) {
            eventRects.add(new DirectionalRect());
        }

        dayView.hourLabelViews.addAll(hourLabelViews);
        dayView.filteredEventViews.addAll(eventViews);
        dayView.filteredEventTimeRanges.addAll(eventTimeRanges);
        dayView.eventColumnSpansHelper = new DayView.EventColumnSpansHelper(eventTimeRanges);
        dayView.eventRects.addAll(eventRects);
        dayView.setParentWidth(PARENT_WIDTH);
    }

    @Test
    public void setHourLabelRects() {
        dayView.setHourLabelRects(25, 75, 90);

        assertThat(dayView.hourLabelRects.get(0).getLeft(), is(25));
        assertThat(dayView.hourLabelRects.get(0).getTop(), is(80));
        assertThat(dayView.hourLabelRects.get(0).getRight(), is(75));
        assertThat(dayView.hourLabelRects.get(0).getBottom(), is(100));

        assertThat(dayView.hourLabelRects.get(6).getLeft(), is(25));
        assertThat(dayView.hourLabelRects.get(6).getTop(), is(500));
        assertThat(dayView.hourLabelRects.get(6).getRight(), is(75));
        assertThat(dayView.hourLabelRects.get(6).getBottom(), is(520));

        assertThat(dayView.hourLabelRects.get(13).getLeft(), is(25));
        assertThat(dayView.hourLabelRects.get(13).getTop(), is(990));
        assertThat(dayView.hourLabelRects.get(13).getRight(), is(75));
        assertThat(dayView.hourLabelRects.get(13).getBottom(), is(1010));

        assertThat(dayView.hourLabelRects.get(21).getLeft(), is(25));
        assertThat(dayView.hourLabelRects.get(21).getTop(), is(1550));
        assertThat(dayView.hourLabelRects.get(21).getRight(), is(75));
        assertThat(dayView.hourLabelRects.get(21).getBottom(), is(1570));
    }

    @Test
    public void setDividerRects() {
        dayView.setDividerRects(10, 5, 195);

        assertThat(dayView.hourDividerRects.get(0).getLeft(), is(5));
        assertThat(dayView.hourDividerRects.get(0).getTop(), is(10));
        assertThat(dayView.hourDividerRects.get(0).getRight(), is(195));
        assertThat(dayView.hourDividerRects.get(0).getBottom(), is(17));

        assertThat(dayView.halfHourDividerRects.get(7).getLeft(), is(5));
        assertThat(dayView.halfHourDividerRects.get(7).getTop(), is(535));
        assertThat(dayView.halfHourDividerRects.get(7).getRight(), is(195));
        assertThat(dayView.halfHourDividerRects.get(7).getBottom(), is(542));

        assertThat(dayView.hourDividerRects.get(19).getLeft(), is(5));
        assertThat(dayView.hourDividerRects.get(19).getTop(), is(1340));
        assertThat(dayView.hourDividerRects.get(19).getRight(), is(195));
        assertThat(dayView.hourDividerRects.get(19).getBottom(), is(1347));

        assertThat(dayView.halfHourDividerRects.get(22).getLeft(), is(5));
        assertThat(dayView.halfHourDividerRects.get(22).getTop(), is(1585));
        assertThat(dayView.halfHourDividerRects.get(22).getRight(), is(195));
        assertThat(dayView.halfHourDividerRects.get(22).getBottom(), is(1592));
    }

    @Test
    public void setEventRects() {
        dayView.setEventRects(10, MINUTE_HEIGHT, 5, 195);

        assertThat(dayView.eventRects, notNullValue());

        assertThat(dayView.eventRects.get(0).getLeft(), is(8));
        assertThat(dayView.eventRects.get(0).getTop(), is(48));
        assertThat(dayView.eventRects.get(0).getRight(), is(65));
        assertThat(dayView.eventRects.get(0).getBottom(), is(175));

        assertThat(dayView.eventRects.get(1).getLeft(), is(71));
        assertThat(dayView.eventRects.get(1).getTop(), is(104));
        assertThat(dayView.eventRects.get(1).getRight(), is(191));
        assertThat(dayView.eventRects.get(1).getBottom(), is(119));

        assertThat(dayView.eventRects.get(2).getLeft(), is(71));
        assertThat(dayView.eventRects.get(2).getTop(), is(160));
        assertThat(dayView.eventRects.get(2).getRight(), is(128));
        assertThat(dayView.eventRects.get(2).getBottom(), is(287));

        assertThat(dayView.eventRects.get(3).getLeft(), is(134));
        assertThat(dayView.eventRects.get(3).getTop(), is(160));
        assertThat(dayView.eventRects.get(3).getRight(), is(191));
        assertThat(dayView.eventRects.get(3).getBottom(), is(287));
    }

    @Test
    public void setRect() {
        DirectionalRect rect = new DirectionalRect();
        rect.set(false, 20, 1, 2, 3, 4);

        assertThat(rect.getLeft(), is(1));
        assertThat(rect.getTop(), is(2));
        assertThat(rect.getRight(), is(3));
        assertThat(rect.getBottom(), is(4));

        rect.set(true, 20, 1, 2, 3, 4);

        assertThat(rect.getLeft(), is(17));
        assertThat(rect.getTop(), is(2));
        assertThat(rect.getRight(), is(19));
        assertThat(rect.getBottom(), is(4));
    }

    @Test
    public void timeRanges() {
        DayView.EventTimeRange range = new DayView.EventTimeRange(20, 40);

        assertThat(range.conflicts(new DayView.EventTimeRange(5, 15)), is(false));
        assertThat(range.conflicts(new DayView.EventTimeRange(50, 90)), is(false));
        assertThat(range.conflicts(new DayView.EventTimeRange(5, 20)), is(false));
        assertThat(range.conflicts(new DayView.EventTimeRange(40, 90)), is(false));

        assertThat(range.conflicts(new DayView.EventTimeRange(20, 40)), is(true));
        assertThat(range.conflicts(new DayView.EventTimeRange(10, 60)), is(true));
        assertThat(range.conflicts(new DayView.EventTimeRange(25, 35)), is(true));
        assertThat(range.conflicts(new DayView.EventTimeRange(10, 35)), is(true));
        assertThat(range.conflicts(new DayView.EventTimeRange(25, 50)), is(true));
    }

    @Test
    public void singleEventColumnSpan() {
        List<DayView.EventTimeRange> timeRanges =
                Collections.singletonList(new DayView.EventTimeRange(55, 133));

        DayView.EventColumnSpansHelper columnSpansHelper =
                new DayView.EventColumnSpansHelper(timeRanges);

        assertThat(columnSpansHelper.columnSpans.get(0).startColumn, is(0));
        assertThat(columnSpansHelper.columnSpans.get(0).endColumn, is(1));

        assertThat(columnSpansHelper.columnCount, is(1));
    }

    @Test
    public void multipleEventColumnSpans() {
        List<DayView.EventTimeRange> timeRanges = new ArrayList<>();
        timeRanges.add(new DayView.EventTimeRange(30, 180));
        timeRanges.add(new DayView.EventTimeRange(90, 120));
        timeRanges.add(new DayView.EventTimeRange(150, 300));
        timeRanges.add(new DayView.EventTimeRange(150, 300));

        DayView.EventColumnSpansHelper columnSpansHelper =
                new DayView.EventColumnSpansHelper(timeRanges);

        assertThat(columnSpansHelper.columnSpans.get(0).startColumn, is(0));
        assertThat(columnSpansHelper.columnSpans.get(0).endColumn, is(1));

        assertThat(columnSpansHelper.columnSpans.get(1).startColumn, is(1));
        assertThat(columnSpansHelper.columnSpans.get(1).endColumn, is(3));

        assertThat(columnSpansHelper.columnSpans.get(2).startColumn, is(1));
        assertThat(columnSpansHelper.columnSpans.get(2).endColumn, is(2));

        assertThat(columnSpansHelper.columnSpans.get(3).startColumn, is(2));
        assertThat(columnSpansHelper.columnSpans.get(3).endColumn, is(3));

        assertThat(columnSpansHelper.columnCount, is(3));
    }
}
