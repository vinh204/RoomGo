package com.example.homestay.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.ImageView;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ImageLoader {
  private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);

  private ImageLoader() {}

  public static void load(ImageView view, String source, int placeholder) {
    view.setImageResource(placeholder);
    if (source == null || source.trim().isEmpty()) return;
    Uri uri = Uri.parse(source.trim());
    String scheme = uri.getScheme();
    if ("content".equals(scheme) || "android.resource".equals(scheme) || "file".equals(scheme)) {
      try {
        view.setImageURI(uri);
      } catch (Exception ignored) {
        view.setImageResource(placeholder);
      }
      return;
    }
    if (!"https".equals(scheme) && !"http".equals(scheme)) return;
    WeakReference<ImageView> target = new WeakReference<>(view);
    EXECUTOR.execute(
        () -> {
          Bitmap bitmap = null;
          HttpURLConnection connection = null;
          try {
            connection = (HttpURLConnection) new URL(source).openConnection();
            connection.setConnectTimeout(8_000);
            connection.setReadTimeout(12_000);
            connection.setInstanceFollowRedirects(true);
            try (InputStream input = connection.getInputStream()) {
              bitmap = BitmapFactory.decodeStream(input);
            }
          } catch (Exception ignored) {
          } finally {
            if (connection != null) connection.disconnect();
          }
          ImageView image = target.get();
          Bitmap result = bitmap;
          if (image != null)
            image.post(
                () -> {
                  if (result != null) image.setImageBitmap(result);
                  else image.setImageResource(placeholder);
                });
        });
  }
}
