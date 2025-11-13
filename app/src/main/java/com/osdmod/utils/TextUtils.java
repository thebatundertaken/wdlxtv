package com.osdmod.utils;

import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.ScrollView;
import android.widget.TextView;

public class TextUtils {

    public static ScrollView linkifyText(String message, Context context) {
        return linkifyTextHelper(message, context, true);
    }

    public static ScrollView linkifyTextNoLink(String message, Context context) {
        return linkifyTextHelper(message, context, false);
    }

    private static ScrollView linkifyTextHelper(String message, Context context, boolean html) {
        ScrollView svMessage = new ScrollView(context);
        TextView tvMessage = new TextView(context);
        tvMessage.setText(html ? Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY) : message);
        tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
        tvMessage.setTextColor(-1);
        svMessage.setPadding(14, 2, 10, 12);
        svMessage.addView(tvMessage);
        return svMessage;
    }

}
