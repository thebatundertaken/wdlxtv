package com.osdmod.customviews;

import android.content.Context;
import android.os.Handler;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.osdmod.remote.R;

public class NumberPicker extends LinearLayout
        implements View.OnClickListener, View.OnFocusChangeListener, View.OnLongClickListener {
    private static final boolean DEFAULT_WRAP = true;
    private final Handler mHandler;
    private final Runnable mRunnable;
    private final EditText mText;
    private final long mSpeed = 300;
    private final NumberPickerButton mDecrementButton;
    private final NumberPickerButton mIncrementButton;
    private boolean mDecrement;
    private boolean mIncrement;
    private int mCurrent;
    private int mEnd;
    private int mPrevious;
    private int mStart;
    private NumberPickerChangeListener mListener;

    public NumberPicker(Context context) {
        this(context, null);
    }

    public NumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRunnable = new Runnable() {
            public void run() {
                if (mIncrement) {
                    changeCurrent(mCurrent + 1);
                    mHandler.postDelayed(this, mSpeed);
                } else if (mDecrement) {
                    changeCurrent(mCurrent - 1);
                    mHandler.postDelayed(this, mSpeed);
                }
            }
        };
        setOrientation(LinearLayout.VERTICAL);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
                R.layout.dialog_numberpicker, this, DEFAULT_WRAP);
        mHandler = new Handler();
        InputFilter inputFilter = new NumberPickerInputFilter();
        mIncrementButton = findViewById(R.id.increment);
        mIncrementButton.setOnClickListener(this);
        mIncrementButton.setOnLongClickListener(this);
        mIncrementButton.setNumberPicker(this);
        mDecrementButton = findViewById(R.id.decrement);
        mDecrementButton.setOnClickListener(this);
        mDecrementButton.setOnLongClickListener(this);
        mDecrementButton.setNumberPicker(this);
        mText = findViewById(R.id.timepicker_input);
        mText.setOnFocusChangeListener(this);
        mText.setFilters(new InputFilter[]{inputFilter});
        mText.setRawInputType(2);
        if (!isEnabled()) {
            setEnabled(false);
        }

        mStart = 0;
        mEnd = 200;
        mCurrent = 0;
        mText.setText(String.valueOf(mCurrent));
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mIncrementButton.setEnabled(enabled);
        mDecrementButton.setEnabled(enabled);
        mText.setEnabled(enabled);
    }

    public void setOnChangeListener(NumberPickerChangeListener listener) {
        this.mListener = listener;
    }

    public void setRange(int start, int end) {
        mStart = start;
        mCurrent = start;
        mEnd = end;
        updateView();
    }

    public void onClick(View v) {
        validateInput(mText);
        if (!mText.hasFocus()) {
            mText.requestFocus();
        }
        if (R.id.increment == v.getId()) {
            changeCurrent(mCurrent + 1);
        } else if (R.id.decrement == v.getId()) {
            changeCurrent(mCurrent - 1);
        }
    }

    private void changeCurrent(int current) {
        if (current > mEnd) {
            current = mEnd;
        } else if (current < mStart) {
            current = mStart;
        }
        mPrevious = mCurrent;
        mCurrent = current;
        notifyChange();
        updateView();
    }

    private void notifyChange() {
        if (mListener != null) {
            mListener.onChanged(this, mPrevious, mCurrent);
        }
    }

    private void updateView() {
        mText.setText(String.valueOf(mCurrent));
        mText.setSelection(mText.getText().length());
    }

    private void validateCurrentView(CharSequence str) {
        int val = Integer.parseInt(str.toString());
        if (val >= mStart && val <= mEnd && mCurrent != val) {
            mPrevious = mCurrent;
            mCurrent = val;
            notifyChange();
        }
        updateView();
    }

    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            validateInput(v);
        }
    }

    private void validateInput(View v) {
        String str = String.valueOf(((TextView) v).getText());
        if (str.isEmpty()) {
            updateView();
        } else {
            validateCurrentView(str);
        }
    }

    public boolean onLongClick(View v) {
        mText.clearFocus();
        mText.requestFocus();
        if (R.id.increment == v.getId()) {
            mIncrement = DEFAULT_WRAP;
            mHandler.post(mRunnable);
        } else if (R.id.decrement == v.getId()) {
            mDecrement = DEFAULT_WRAP;
            mHandler.post(mRunnable);
        }
        return DEFAULT_WRAP;
    }

    public void cancelIncrement() {
        mIncrement = false;
    }

    public void cancelDecrement() {
        mDecrement = false;
    }

    public int getCurrent() {
        return mCurrent;
    }

    public void setCurrent(int current) {
        mCurrent = current;
        updateView();
    }
}
