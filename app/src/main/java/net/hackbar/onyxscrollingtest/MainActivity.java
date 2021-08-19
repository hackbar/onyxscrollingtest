package net.hackbar.onyxscrollingtest;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.onyx.android.sdk.api.device.EpdDeviceManager;
import com.onyx.android.sdk.api.device.epd.EpdController;
import com.onyx.android.sdk.api.device.epd.UpdateMode;

import net.hackbar.onyxscrollingtest.databinding.ActivityMainBinding;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

  ExecutorService executorService;
  Executor mainExecutor;
  private GestureDetector scrollGestureDetector;
  private ImageView imageView;
  private Button button;
  private Bitmap goodBitmap;
  private Bitmap badBitmap;
  private boolean isGoodBitmap;
  private float xStart;
  private float xEnd;
  private float yStart;
  private float yEnd;

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    executorService = Executors.newCachedThreadPool();
    mainExecutor = getMainExecutor();

    ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    imageView = binding.imageView;
    imageView.setScaleType(ImageView.ScaleType.MATRIX);
    button = binding.button;
    button.setOnClickListener(this);
    EpdController.setViewDefaultUpdateMode(imageView, UpdateMode.GU);

    scrollGestureDetector = new GestureDetector(this, new ScrollGestureListener());

    executorService.execute(() -> {
      goodBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.good);
      badBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bad);
      mainExecutor.execute(() -> {
        imageView.setImageBitmap(goodBitmap);
        button.setText("Change to bad bitmap");
        isGoodBitmap = true;

        // Flash the screen once to make it look nice
        EpdController.applyGCOnce();
        // Leave it in animation mode for nice scrolling
        EpdDeviceManager.enterAnimationUpdate(false);
        initMatrix();

        // Ignore events until the image is set up
        imageView.setOnTouchListener((View v, MotionEvent ev) -> {
          scrollGestureDetector.onTouchEvent(ev);
          return true;
        });
      });
    });
  }

  private void initMatrix() {
    xStart = 0;
    yStart = 0;
    xEnd = 800;
    yEnd = 800;
    updateMatrix();
  }

  private void updateMatrix() {
    RectF srcRect = new RectF(xStart, yStart, xEnd, yEnd);
    RectF dstRect = new RectF(0, 0, imageView.getWidth(), imageView.getHeight());
    Matrix matrix = new Matrix();
    matrix.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.START);
    imageView.setImageMatrix(matrix);
  }

  @Override
  public void onClick(View v) {
    if (v.getId() == button.getId()) {
      if (isGoodBitmap) {
        imageView.setImageBitmap(badBitmap);
        isGoodBitmap = false;
        button.setText("Change to good bitmap");
      } else {
        imageView.setImageBitmap(goodBitmap);
        isGoodBitmap = true;
        button.setText("Change to bad bitmap");
      }
    }
  }

  private class ScrollGestureListener extends GestureDetector.SimpleOnGestureListener {

    @Override
    public boolean onDown(MotionEvent e) {
      // Return true so the system will send us the of the gestures for this event
      return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
      xStart += distanceX;
      xEnd += distanceX;
      yStart += distanceY;
      yEnd += distanceY;
      //EpdDeviceManager.enterAnimationUpdate(false);
      updateMatrix();
      return true;  // consume the event
    }
  }
}