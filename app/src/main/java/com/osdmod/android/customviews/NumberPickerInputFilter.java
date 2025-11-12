package com.osdmod.android.customviews;

import android.text.InputFilter;
import android.text.Spanned;

public class NumberPickerInputFilter implements InputFilter {
    private final InputFilter mNumberInputFilter = new NumberRangeKeyListener();

    public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                               int dstart, int dend) {
        return mNumberInputFilter.filter(source, start, end, dest, dstart, dend);
    }
}