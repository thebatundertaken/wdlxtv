package com.osdmod.customviews;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public class ActionItem {
    private int actionId;
    private Drawable icon;
    private boolean selected;
    private boolean sticky;
    private Bitmap thumb;
    private String title;

    public ActionItem(int actionId2, String title2, Drawable icon2) {
        this.actionId = -1;
        this.title = title2;
        this.icon = icon2;
        this.actionId = actionId2;
    }

    public ActionItem() {
        this(-1, (String) null, (Drawable) null);
    }

    public ActionItem(int actionId2, String title2) {
        this(actionId2, title2, (Drawable) null);
    }

    public ActionItem(Drawable icon2) {
        this(-1, (String) null, icon2);
    }

    public ActionItem(int actionId2, Drawable icon2) {
        this(actionId2, (String) null, icon2);
    }

    public void setTitle(String title2) {
        this.title = title2;
    }

    public String getTitle() {
        return this.title;
    }

    public void setIcon(Drawable icon2) {
        this.icon = icon2;
    }

    public Drawable getIcon() {
        return this.icon;
    }

    public void setActionId(int actionId2) {
        this.actionId = actionId2;
    }

    public int getActionId() {
        return this.actionId;
    }

    public void setSticky(boolean sticky2) {
        this.sticky = sticky2;
    }

    public boolean isSticky() {
        return this.sticky;
    }

    public void setSelected(boolean selected2) {
        this.selected = selected2;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setThumb(Bitmap thumb2) {
        this.thumb = thumb2;
    }

    public Bitmap getThumb() {
        return this.thumb;
    }
}
