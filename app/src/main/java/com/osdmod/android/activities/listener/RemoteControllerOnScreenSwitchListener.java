package com.osdmod.android.activities.listener;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;

import com.osdmod.android.customviews.HorizontalPager;
import com.osdmod.android.customviews.OnScreenSwitchListener;
import com.osdmod.remote.R;

public class RemoteControllerOnScreenSwitchListener implements OnScreenSwitchListener {
    private final Activity activity;

    public RemoteControllerOnScreenSwitchListener(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onScreenSwitched(int screen) {
        ImageView img_pos = activity.findViewById(R.id.img_pos);
        int tot = ((HorizontalPager) activity.findViewById(
                R.id.horizontal_pager)).getChildCount();
        switch (screen) {
            case 0:
                    img_pos.setImageResource((tot != 3) ? R.drawable.one : R.drawable.tone);
                break;
            case 1:
                img_pos.setImageResource((tot != 3) ? R.drawable.two : R.drawable.ttwo);
                    break;
            case 2:
                img_pos.setImageResource(R.drawable.three);
                break;
        }

        if (tot == 1) {
            img_pos.setVisibility(View.GONE);
        }
    };
}
