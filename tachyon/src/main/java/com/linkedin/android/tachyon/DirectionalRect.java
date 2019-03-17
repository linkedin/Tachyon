/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.tachyon;

/**
 * Similar to {@link android.graphics.Rect} but provides the
 * {@link #set(boolean, int, int, int, int, int)} method to handle right-to-left mode.
 */
public class DirectionalRect {

    private int left;
    private int top;
    private int right;
    private int bottom;

    /**
     * Sets the rect's points but factors in whether or not the device is in right-to-left mode.
     *
     * @param isRtl       whether or not the device is in right-to-left mode
     * @param parentWidth the width of the parent view of the rect, this will be used to figure out
     *                    how to translate
     *                    the rect in right-to-left mode
     * @param start       the start of the rect in left-to-right mode
     * @param top         the top of the rect, it will not be translated
     * @param end         the end of the rect in left-to-right mode
     * @param bottom      the bottom of the rect, it will not be translated
     */
    public void set(boolean isRtl, int parentWidth, int start, int top, int end, int bottom) {
        this.left = isRtl ? parentWidth - end : start;
        this.top = top;
        this.right = isRtl ? parentWidth - start : end;
        this.bottom = bottom;
    }

    public int getLeft() {
        return left;
    }

    public int getTop() {
        return top;
    }

    public int getRight() {
        return right;
    }

    public int getBottom() {
        return bottom;
    }
}
