package com.osdmod.remote;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.widget.ImageView;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class GetServicesDrawable {
    private final Map<String, Drawable> drawableMap = new HashMap<>();

    public Drawable fetchDrawable(String urlString) {
        if (this.drawableMap.containsKey(urlString)) {
            return this.drawableMap.get(urlString);
        }
        try {
            InputStream is = fetch(urlString);
            TypedValue typedValue = new TypedValue();
            typedValue.density = 0;
            Drawable drawable = Drawable.createFromResourceStream(null, typedValue, is, "src");
            this.drawableMap.put(urlString, drawable);
            return drawable;
        } catch (IOException e) {
            return null;
        }
    }

    public void fetchDrawableOnThread(final String urlString, final ImageView imageView) {
        if (this.drawableMap.containsKey(urlString)) {
            imageView.setImageDrawable(this.drawableMap.get(urlString));
        }
        final Handler handler = new Handler() {
            public void handleMessage(Message message) {
                if (message.obj != null) {
                    imageView.setImageDrawable((Drawable) message.obj);
                }
            }
        };
        new Thread(
                () -> handler.sendMessage(handler.obtainMessage(1, GetServicesDrawable.this.fetchDrawable(urlString)))).start();
    }

    private InputStream fetch(String urlString) throws IOException {
        return new DefaultHttpClient().execute(new HttpGet(urlString)).getEntity().getContent();
    }
}
