package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.R;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.FrameMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
public class ShadowWindowTest {
  @Test
  public void getFlag_shouldReturnWindowFlags() throws Exception {
    Activity activity = Robolectric.buildActivity(Activity.class).create().get();
    Window window = activity.getWindow();

    assertThat(shadowOf(window).getFlag(WindowManager.LayoutParams.FLAG_FULLSCREEN)).isFalse();
    window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    assertThat(shadowOf(window).getFlag(WindowManager.LayoutParams.FLAG_FULLSCREEN)).isTrue();
    window.setFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON, WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    assertThat(shadowOf(window).getFlag(WindowManager.LayoutParams.FLAG_FULLSCREEN)).isTrue();
  }

  @Test
  public void getTitle_shouldReturnWindowTitle() throws Exception {
    Activity activity = Robolectric.buildActivity(Activity.class).create().get();
    Window window = activity.getWindow();
    window.setTitle("My Window Title");
    assertThat(shadowOf(window).getTitle()).isEqualTo("My Window Title");
  }

  @Test
  public void getBackgroundDrawable_returnsSetDrawable() throws Exception {
    Activity activity = Robolectric.buildActivity(Activity.class).create().get();
    Window window = activity.getWindow();
    ShadowWindow shadowWindow = shadowOf(window);

    assertThat(shadowWindow.getBackgroundDrawable()).isNull();

    window.setBackgroundDrawableResource(R.drawable.btn_star);
    assertThat(shadowOf(shadowWindow.getBackgroundDrawable()).createdFromResId).isEqualTo(R.drawable.btn_star);
  }

  @Test
  public void getSoftInputMode_returnsSoftInputMode() throws Exception {
    TestActivity activity = Robolectric.buildActivity(TestActivity.class).create().get();
    Window window = activity.getWindow();
    ShadowWindow shadowWindow = shadowOf(window);

    window.setSoftInputMode(7);

    assertThat(shadowWindow.getSoftInputMode()).isEqualTo(7);
  }

  @Test
  public void getProgressBar_returnsTheProgressBar() {
    Activity activity = Robolectric.buildActivity(TestActivity.class).create().get();

    ProgressBar progress = shadowOf(activity.getWindow()).getProgressBar();

    assertThat(progress.getVisibility()).isEqualTo(View.INVISIBLE);
    activity.setProgressBarVisibility(true);
    assertThat(progress.getVisibility()).isEqualTo(View.VISIBLE);
    activity.setProgressBarVisibility(false);
    assertThat(progress.getVisibility()).isEqualTo(View.GONE);
  }

  @Test
  public void getIndeterminateProgressBar_returnsTheIndeterminateProgressBar() {
    ActivityController<TestActivity> testActivityActivityController = Robolectric.buildActivity(TestActivity.class);
    TestActivity activity = testActivityActivityController.get();
    activity.requestFeature = Window.FEATURE_INDETERMINATE_PROGRESS;
    testActivityActivityController.create();

    ProgressBar indeterminate = shadowOf(activity.getWindow()).getIndeterminateProgressBar();

    assertThat(indeterminate.getVisibility()).isEqualTo(View.INVISIBLE);
    activity.setProgressBarIndeterminateVisibility(true);
    assertThat(indeterminate.getVisibility()).isEqualTo(View.VISIBLE);
    activity.setProgressBarIndeterminateVisibility(false);
    assertThat(indeterminate.getVisibility()).isEqualTo(View.GONE);
  }

  @Test @Config(maxSdk = LOLLIPOP_MR1)
  public void forPreM_create_shouldCreateImplPhoneWindow() throws Exception {
    assertThat(
            ShadowWindow.create(ApplicationProvider.getApplicationContext()).getClass().getName())
        .isEqualTo("com.android.internal.policy.impl.PhoneWindow");
  }

  @Test @Config(minSdk = M)
  public void forM_create_shouldCreatePhoneWindow() throws Exception {
    assertThat(
            ShadowWindow.create(ApplicationProvider.getApplicationContext()).getClass().getName())
        .isEqualTo("com.android.internal.policy.PhoneWindow");
  }

  @Test
  @Config(minSdk = N)
  public void reportOnFrameMetricsAvailable_notifiesListeners() throws Exception {
    Window window = ShadowWindow.create(ApplicationProvider.getApplicationContext());
    Window.OnFrameMetricsAvailableListener listener =
        Mockito.mock(Window.OnFrameMetricsAvailableListener.class);
    FrameMetrics frameMetrics = new FrameMetricsBuilder().build();

    window.addOnFrameMetricsAvailableListener(listener, new Handler(Looper.getMainLooper()));
    shadowOf(window).reportOnFrameMetricsAvailable(frameMetrics);

    verify(listener)
        .onFrameMetricsAvailable(window, frameMetrics, /* dropCountSinceLastInvocation= */ 0);
  }

  @Test
  @Config(minSdk = N)
  public void reportOnFrameMetricsAvailable_nonZeroDropCount_notifiesListeners() throws Exception {
    Window window = ShadowWindow.create(ApplicationProvider.getApplicationContext());
    Window.OnFrameMetricsAvailableListener listener =
        Mockito.mock(Window.OnFrameMetricsAvailableListener.class);
    FrameMetrics frameMetrics = new FrameMetricsBuilder().build();

    window.addOnFrameMetricsAvailableListener(listener, new Handler(Looper.getMainLooper()));
    shadowOf(window)
        .reportOnFrameMetricsAvailable(frameMetrics, /* dropCountSinceLastInvocation= */ 3);

    verify(listener)
        .onFrameMetricsAvailable(window, frameMetrics, /* dropCountSinceLastInvocation= */ 3);
  }

  @Test
  @Config(minSdk = N)
  public void reportOnFrameMetricsAvailable_listenerRemoved_doesntnotifyListener()
      throws Exception {
    Window window = ShadowWindow.create(ApplicationProvider.getApplicationContext());
    Window.OnFrameMetricsAvailableListener listener =
        Mockito.mock(Window.OnFrameMetricsAvailableListener.class);
    FrameMetrics frameMetrics = new FrameMetricsBuilder().build();

    window.addOnFrameMetricsAvailableListener(listener, new Handler(Looper.getMainLooper()));
    window.removeOnFrameMetricsAvailableListener(listener);
    shadowOf(window).reportOnFrameMetricsAvailable(frameMetrics);

    verify(listener, never())
        .onFrameMetricsAvailable(
            any(Window.class),
            any(FrameMetrics.class),
            /* dropCountSinceLastInvocation= */ anyInt());
  }

  public static class TestActivity extends Activity {
    public int requestFeature = Window.FEATURE_PROGRESS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setTheme(R.style.Theme_Holo_Light);
      getWindow().requestFeature(requestFeature);
      setContentView(new LinearLayout(this));
      getActionBar().setIcon(R.drawable.ic_lock_power_off);
    }
  }
}
