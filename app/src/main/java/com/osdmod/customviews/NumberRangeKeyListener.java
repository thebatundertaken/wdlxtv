package com.osdmod.customviews;

import android.text.InputType;
import android.text.Spanned;
import android.text.method.NumberKeyListener;

import androidx.annotation.NonNull;

public class NumberRangeKeyListener extends NumberKeyListener {
    private static final char[] DIGIT_CHARACTERS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    public int getInputType() {
        return InputType.TYPE_CLASS_NUMBER;
    }

    @NonNull
    protected char[] getAcceptedChars() {
        return DIGIT_CHARACTERS;
    }

    public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                               int dstart, int dend) {
        CharSequence filtered = super.filter(source, start, end, dest, dstart, dend);
        if (filtered == null) {
            filtered = source.subSequence(start, end);
        }

        String result = String.valueOf(
                dest.subSequence(0, dstart)) + filtered + dest.subSequence(dend, dest.length());

        if (result.isEmpty()) {
            return result;
        }

        return filtered;
    }
}