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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.ViewCompat;

public class DayView extends ViewGroup {

    /**
     * Because of daylight saving time, some days are shorter or longer than 24 hours. Most calendar
     * apps assume there are 24 hours in each day, and then to handle events that span a daylight
     * saving time switch those events are adjusted. For example, when daylight saving time begins,
     * an event from 1:00 AM to 3:00 AM would only last an hour since the switch happens at 2:00 AM.
     * This means for events that span the beginning of daylight saving time, they will be drawn
     * with an extra hour. For events that span the end of daylight saving time, they'll be drawn at
     * the minimum height for an event if the event's duration is roughly an hour or less.
     */
    public static final int HOUR_COUNT = 24;

    /**
     * The total number of usable minutes in this day.
     */
    public static final int MINUTE_COUNT = 1440;

    /**
     * The hour labels count here is 25 so we can include the start of the midnight hour of the next
     * day. {@link #setHourLabelViews(List)} expects exactly this many labels.
     */
    public static final int HOUR_LABELS_COUNT = HOUR_COUNT + 1;

    private static final int HOUR_DIVIDERS_COUNT = HOUR_COUNT + 1;
    private static final int HALF_HOUR_DIVIDERS_COUNT = HOUR_COUNT;

    @NonNull
    @VisibleForTesting
    final List<DirectionalRect> hourLabelRects;
    @NonNull
    @VisibleForTesting
    final List<DirectionalRect> hourDividerRects;
    @NonNull
    @VisibleForTesting
    final List<DirectionalRect> halfHourDividerRects;
    @NonNull
    @VisibleForTesting
    final List<View> hourLabelViews;
    @NonNull
    @VisibleForTesting
    final List<View> eventViews;
    @NonNull
    @VisibleForTesting
    final List<EventTimeRange> eventTimeRanges;
    @NonNull
    @VisibleForTesting
    final List<DirectionalRect> eventRects;

    @Nullable
    @VisibleForTesting
    EventColumnSpansHelper eventColumnSpansHelper;

    @NonNull
    private final Paint hourDividerPaint;
    @NonNull
    private final Paint halfHourDividerPaint;
    private final int dividerHeight;

    private final int usableHalfHourHeight;
    private final int hourLabelWidth;
    private final int hourLabelMarginEnd;
    private final int eventMargin;

    private boolean isRtl;
    private int parentWidth;
    private float minuteHeight;

    public DayView(@NonNull Context context) {
        this(context, null);
    }

    public DayView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DayView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, true);
    }

    @VisibleForTesting
    DayView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, boolean enableDrawing) {
        super(context, attrs, defStyleAttr);

        hourDividerRects = new ArrayList<>(HOUR_DIVIDERS_COUNT);
        for (int i = 0; i < HOUR_DIVIDERS_COUNT; i++) {
            hourDividerRects.add(new DirectionalRect());
        }

        halfHourDividerRects = new ArrayList<>(HALF_HOUR_DIVIDERS_COUNT);
        for (int i = 0; i < HALF_HOUR_DIVIDERS_COUNT; i++) {
            halfHourDividerRects.add(new DirectionalRect());
        }

        hourLabelRects = new ArrayList<>(HOUR_LABELS_COUNT);
        for (int i = 0; i < HOUR_LABELS_COUNT; i++) {
            hourLabelRects.add(new DirectionalRect());
        }

        hourLabelViews = new ArrayList<>();
        eventViews = new ArrayList<>();
        eventTimeRanges = new ArrayList<>();
        eventRects = new ArrayList<>();

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.DayView);
        dividerHeight = array.getDimensionPixelSize(R.styleable.DayView_dividerHeight, 0);
        usableHalfHourHeight =
                dividerHeight + array.getDimensionPixelSize(R.styleable.DayView_halfHourHeight, 0);

        hourDividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        halfHourDividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // This view draws its hour and half hour dividers directly
        if (enableDrawing) {
            setWillNotDraw(false);
            hourDividerPaint.setColor(array.getColor(R.styleable.DayView_hourDividerColor, 0));
            halfHourDividerPaint.setColor(array.getColor(R.styleable.DayView_halfHourDividerColor, 0));
        }

        hourLabelWidth = array.getDimensionPixelSize(R.styleable.DayView_hourLabelWidth, 0);
        hourLabelMarginEnd = array.getDimensionPixelSize(R.styleable.DayView_hourLabelMarginEnd, 0);
        eventMargin = array.getDimensionPixelSize(R.styleable.DayView_eventMargin, 0);
        array.recycle();
    }

    /**
     * @param hourLabelViews the list of views to show as labels for each hour, this list must not
     *                       be null and its length must be {@link #HOUR_LABELS_COUNT}
     */
    public void setHourLabelViews(@NonNull List<View> hourLabelViews) {
        for (View view : this.hourLabelViews) {
            removeView(view);
        }

        this.hourLabelViews.clear();
        this.hourLabelViews.addAll(hourLabelViews);

        for (View view : this.hourLabelViews) {
            addView(view);
        }
    }

    /**
     * @param eventViews      the list of event views to display
     * @param eventTimeRanges the list of event params that describe each event view's start/end
     *                        times, this list must be equal in length to the list of event views,
     *                        or both should be null
     */
    public void setEventViews(@Nullable List<View> eventViews,
                              @Nullable List<EventTimeRange> eventTimeRanges) {
        for (View view : this.eventViews) {
            removeView(view);
        }

        this.eventViews.clear();
        this.eventTimeRanges.clear();
        eventRects.clear();
        eventColumnSpansHelper = null;

        if (eventViews != null) {
            this.eventViews.addAll(eventViews);
        }

        if (eventTimeRanges != null) {
            this.eventTimeRanges.addAll(eventTimeRanges);
        }

        if (!this.eventViews.isEmpty() && !this.eventTimeRanges.isEmpty()) {
            eventColumnSpansHelper = new EventColumnSpansHelper(this.eventTimeRanges);

            for (int i = 0; i < this.eventViews.size(); i++) {
                View view = this.eventViews.get(i);
                addView(view);

                eventRects.add(new DirectionalRect());
            }
        }
    }

    /**
     * Removes all of the existing event views.
     *
     * @return the event views that have been removed, they are safe to recycle and reuse at this
     * point
     */
    @Nullable
    public List<View> removeEventViews() {
        List<View> eventViews = this.eventViews;
        setEventViews(null, null);

        return eventViews;
    }

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the top of the given hour.
     *
     * @param hour the hour of the day, should be between 0 (12:00 AM of the current day) and 24
     *             (12:00 AM of the next day)
     * @return the vertical offset of the top of the given hour in pixels
     */
    public int getHourTop(int hour) {
        if (hour < 0 || hour >= HOUR_LABELS_COUNT) {
            throw new IllegalStateException("Hour must be between 0 and " + HOUR_LABELS_COUNT);
        }

        return hourDividerRects.get(hour).getBottom();
    }

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the bottom of the given hour.
     *
     * @param hour the hour of the day, should be between 0 (12:00 AM of the current day) and 24
     *             (12:00 AM of the next day)
     * @return the vertical offset of the bottom of the given hour in pixels
     */
    public int getHourBottom(int hour) {
        if (hour < 0 || hour >= HOUR_LABELS_COUNT) {
            throw new IllegalStateException("Hour must be between 0 and " + HOUR_LABELS_COUNT);
        }

        if (hour == HOUR_LABELS_COUNT - 1) {
            return hourDividerRects.get(hour).getBottom();
        }

        return hourDividerRects.get(hour + 1).getTop();
    }

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the top of the first event.
     *
     * @return the vertical offset of the top of the first event in pixels, or zero if there are no
     * events
     */
    public int getFirstEventTop() {
        return !eventRects.isEmpty() ? eventRects.get(0).getTop() : 0;
    }

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the bottom of the first event.
     *
     * @return the vertical offset of the bottom of the first event in pixels, or zero if there are
     * no events
     */
    public int getFirstEventBottom() {
        return !eventRects.isEmpty() ? eventRects.get(0).getBottom() : 0;
    }

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the top of the last event.
     *
     * @return the vertical offset of the top of the last event in pixels, or zero if there are no
     * events
     */
    public int getLastEventTop() {
        return !eventRects.isEmpty() ? eventRects.get(eventRects.size() - 1).getTop() : 0;
    }

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the bottom of the last event.
     *
     * @return the vertical offset of the bottom of the last event in pixels, or zero if there are
     * no events
     */
    public int getLastEventBottom() {
        return !eventRects.isEmpty() ? eventRects.get(eventRects.size() - 1).getBottom() : 0;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (int i = 0; i < hourLabelViews.size(); i++) {
            View view = hourLabelViews.get(i);
            DirectionalRect rect = hourLabelRects.get(i);
            view.layout(rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom());
        }

        for (int i = 0; i < eventViews.size(); i++) {
            View view = eventViews.get(i);
            DirectionalRect rect = eventRects.get(i);
            view.layout(rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom());
        }
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Draw the hour and half-hour divider lines directly onto the canvas
        for (DirectionalRect rect : hourDividerRects) {
            canvas.drawRect(rect.getLeft(),
                    rect.getTop(),
                    rect.getRight(),
                    rect.getBottom(),
                    hourDividerPaint);
        }

        for (DirectionalRect rect : halfHourDividerRects) {
            canvas.drawRect(rect.getLeft(),
                    rect.getTop(),
                    rect.getRight(),
                    rect.getBottom(),
                    halfHourDividerPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        validateChildViews();

        isRtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;

        // Start with the default measured dimension
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        parentWidth = getMeasuredWidth();

        // Measure the hour labels using two passes, this first pass is only to figure out the
        // heights
        int hourLabelStart = isRtl ? getPaddingRight() : getPaddingLeft();
        int hourLabelEnd = hourLabelStart + hourLabelWidth;
        int firstDividerTop = 0;
        int lastDividerMarginBottom = 0;
        int hourLabelViewsSize = hourLabelViews.size();
        for (int i = 0; i < hourLabelViewsSize; i++) {
            View view = hourLabelViews.get(i);
            measureChild(view, widthMeasureSpec, heightMeasureSpec);

            if (i == 0) {
                firstDividerTop = view.getMeasuredHeight() / 2;
            } else if (i == hourLabelViewsSize - 1) {
                lastDividerMarginBottom = view.getMeasuredHeight() / 2;
            }
        }

        // Calculate the measured height
        int usableHeight = (hourDividerRects.size() + halfHourDividerRects.size() - 1) * usableHalfHourHeight;
        minuteHeight = (float) usableHeight / MINUTE_COUNT;
        firstDividerTop += getPaddingTop();
        int verticalPadding = firstDividerTop + lastDividerMarginBottom + getPaddingBottom() + dividerHeight;
        int measuredHeight = usableHeight + verticalPadding;

        // Calculate the horizontal positions of the dividers
        int dividerStart = hourLabelEnd + hourLabelMarginEnd;
        int dividerEnd = getMeasuredWidth() - (isRtl ? getPaddingLeft() : getPaddingRight());

        // Set the rects for hour labels, dividers, and events
        setHourLabelRects(hourLabelStart, hourLabelEnd, firstDividerTop);
        setDividerRects(firstDividerTop, dividerStart, dividerEnd);
        setEventRects(firstDividerTop, minuteHeight, dividerStart, dividerEnd);

        // Measure the hour labels and events for a final time
        measureHourLabels();
        measureEvents();

        setMeasuredDimension(widthMeasureSpec, measuredHeight);
    }

    protected void measureExactly(@NonNull View view, @NonNull DirectionalRect rect) {
        view.measure(MeasureSpec.makeMeasureSpec(rect.getRight() - rect.getLeft(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(rect.getBottom() - rect.getTop(), MeasureSpec.EXACTLY));
    }

    /**
     * Sets the dimensions of a rect while factoring in whether or not right-to-left mode is on.
     *
     * @param rect   the rect to update
     * @param start  the start of the rect in left-to-right mode
     * @param top    the top of the rect, it will not be translated
     * @param end    the end of the rect in left-to-right mode
     * @param bottom the bottom of the rect, it will not be translated
     */
    protected void setRect(@NonNull DirectionalRect rect, int start, int top, int end, int bottom) {
        rect.set(isRtl, parentWidth, start, top, end, bottom);
    }

    /**
     * @return the height in pixels taken up by each hour and half-hour divider
     */
    protected int getDividerHeight() {
        return dividerHeight;
    }

    /**
     * @return the height in pixels taken up by each minute
     */
    protected float getMinuteHeight() {
        return minuteHeight;
    }

    @VisibleForTesting
    void setHourLabelRects(int hourLabelStart, int hourLabelEnd, int firstDividerTop) {
        for (int i = 0; i < hourLabelViews.size(); i++) {
            View view = hourLabelViews.get(i);

            int height = view.getMeasuredHeight();

            int top = firstDividerTop + usableHalfHourHeight * i * 2 - height / 2;
            int bottom = top + height;

            setRect(hourLabelRects.get(i), hourLabelStart, top, hourLabelEnd, bottom);
        }
    }

    @VisibleForTesting
    void setDividerRects(int firstDividerTop, int dividerStart, int dividerEnd) {
        for (int i = 0; i < hourDividerRects.size(); i++) {
            int top = firstDividerTop + i * 2 * usableHalfHourHeight;
            int bottom = top + dividerHeight;

            setRect(hourDividerRects.get(i), dividerStart, top, dividerEnd, bottom);
        }

        for (int i = 0; i < halfHourDividerRects.size(); i++) {
            int top = firstDividerTop + (i * 2 + 1) * usableHalfHourHeight;
            int bottom = top + dividerHeight;

            setRect(halfHourDividerRects.get(i), dividerStart, top, dividerEnd, bottom);
        }
    }

    @VisibleForTesting
    void setEventRects(int firstDividerTop, float minuteHeight, int dividerStart, int dividerEnd) {
        if (eventColumnSpansHelper == null) {
            return;
        }

        int eventColumnWidth = eventColumnSpansHelper.columnCount > 0
                ? (dividerEnd - dividerStart) / eventColumnSpansHelper.columnCount
                : 0;

        for (int i = 0; i < eventViews.size(); i++) {
            EventTimeRange timeRange = eventTimeRanges.get(i);
            EventColumnSpan columnSpan = eventColumnSpansHelper.columnSpans.get(i);

            int duration = timeRange.endMinute - timeRange.startMinute;

            int start = columnSpan.startColumn * eventColumnWidth + dividerStart + eventMargin;
            int end = start + (columnSpan.endColumn - columnSpan.startColumn) * eventColumnWidth - eventMargin * 2;

            int topOffset = (int) (timeRange.startMinute * minuteHeight);

            int top = firstDividerTop + topOffset + dividerHeight + eventMargin;
            int bottom = top + (int) (duration * minuteHeight) - eventMargin * 2 - dividerHeight;

            setRect(eventRects.get(i), start, top, end, bottom);
        }
    }

    /**
     * Validates the state of the child views during {@link #onMeasure(int, int)}.
     *
     * @throws IllegalStateException thrown when one or more of the child views are not in a valid
     *                               state
     */
    @CallSuper
    protected void validateChildViews() throws IllegalStateException {
        if (hourLabelViews.size() == 0) {
            throw new IllegalStateException("No hour label views, setHourLabelViews() must be called before this view is rendered");
        } else if (hourLabelViews.size() != HOUR_LABELS_COUNT) {
            throw new IllegalStateException("Inconsistent number of hour label views, there should be " + HOUR_LABELS_COUNT + " but " + hourLabelViews.size() + " were found");
        } else if (eventViews.size() != eventTimeRanges.size()) {
            throw new IllegalStateException("Inconsistent number of event views or event time ranges, they should either be equal in length or both should be null");
        }
    }

    /**
     * Can be used by subclasses that need to layout child views relative to the hour dividers.
     *
     * @return a list of hour divider rects
     */
    @NonNull
    protected List<DirectionalRect> getHourDividerRects() {
        return hourDividerRects;
    }

    /**
     * Can be used by subclasses that need to layout child views relative to the half-hour dividers.
     *
     * @return a list of half-hour divider rects
     */
    @NonNull
    protected List<DirectionalRect> getHalfHourDividerRects() {
        return halfHourDividerRects;
    }

    @VisibleForTesting
    void setParentWidth(int parentWidth) {
        this.parentWidth = parentWidth;
    }

    private void measureHourLabels() {
        for (int i = 0; i < hourLabelViews.size(); i++) {
            measureExactly(hourLabelViews.get(i), hourLabelRects.get(i));
        }
    }

    private void measureEvents() {
        for (int i = 0; i < eventViews.size(); i++) {
            measureExactly(eventViews.get(i), eventRects.get(i));
        }
    }

    /**
     * Represents the start and end time of a calendar event. Both times are in minutes since the
     * start of the day.
     */
    public static class EventTimeRange {

        private final int startMinute;
        private final int endMinute;

        public EventTimeRange(int startMinute, int endMinute) {
            this.startMinute = startMinute;
            this.endMinute = endMinute;
        }

        /**
         * @param range the time range to compare
         * @return true if the time range to compare overlaps in any way with this time range
         */
        @VisibleForTesting
        boolean conflicts(@NonNull EventTimeRange range) {
            return startMinute >= range.startMinute && startMinute < range.endMinute
                    || endMinute > range.startMinute && endMinute <= range.endMinute
                    || range.startMinute >= startMinute && range.startMinute < endMinute
                    || range.endMinute > startMinute && range.endMinute <= endMinute;
        }
    }

    /**
     * Represents the start and end columns a calendar event should span between.
     */
    @VisibleForTesting
    static class EventColumnSpan {

        int startColumn = -1;
        int endColumn = -1;
    }

    /**
     * Helps calculate the start and end columns for a collection of calendar events.
     */
    @VisibleForTesting
    static class EventColumnSpansHelper {

        @NonNull
        final List<EventColumnSpan> columnSpans;
        int columnCount;

        @NonNull
        private final List<EventTimeRange> timeRanges;

        @VisibleForTesting
        EventColumnSpansHelper(@NonNull List<EventTimeRange> timeRanges) {
            this.timeRanges = timeRanges;
            this.columnSpans = new ArrayList<>(timeRanges.size());

            // Find the start and end columns for each event
            for (int i = 0; i < this.timeRanges.size(); i++) {
                findStartColumn(i);
            }

            for (int i = 0; i < this.timeRanges.size(); i++) {
                findEndColumn(i);
            }
        }

        private void findStartColumn(int position) {
            for (int i = 0; i < timeRanges.size(); i++) {
                if (isColumnEmpty(i, position)) {
                    EventColumnSpan columnSpan = new EventColumnSpan();
                    columnSpan.startColumn = i;
                    columnSpan.endColumn = i + 1;
                    columnSpans.add(columnSpan);

                    columnCount = Math.max(columnCount, i + 1);

                    break;
                }
            }
        }

        private void findEndColumn(int position) {
            EventColumnSpan columnSpan = columnSpans.get(position);
            for (int i = columnSpan.endColumn; i < columnCount; i++) {
                if (!isColumnEmpty(i, position)) {
                    break;
                }

                columnSpan.endColumn++;
            }
        }

        private boolean isColumnEmpty(int column, int position) {
            EventTimeRange timeRange = timeRanges.get(position);
            for (int i = 0; i < columnSpans.size(); i++) {
                if (position == i) {
                    continue;
                }

                EventTimeRange compareTimeRange = timeRanges.get(i);
                EventColumnSpan compareColumnSpan = columnSpans.get(i);
                if (compareColumnSpan.startColumn == column && compareTimeRange.conflicts(timeRange)) {
                    return false;
                }
            }

            return true;
        }
    }
}
