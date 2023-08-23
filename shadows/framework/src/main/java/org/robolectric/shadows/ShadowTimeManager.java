package org.robolectric.shadows;

import static android.app.time.DetectorStatusTypes.DETECTION_ALGORITHM_STATUS_RUNNING;
import static android.app.time.DetectorStatusTypes.DETECTOR_STATUS_RUNNING;

import android.annotation.SystemApi;
import android.app.time.Capabilities;
import android.app.time.Capabilities.CapabilityState;
import android.app.time.ExternalTimeSuggestion;
import android.app.time.LocationTimeZoneAlgorithmStatus;
import android.app.time.TelephonyTimeZoneAlgorithmStatus;
import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.app.time.TimeZoneDetectorStatus;
import android.os.Build.VERSION_CODES;
import android.os.UserHandle;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;
import org.robolectric.versioning.AndroidVersions.U;

/** Shadow for internal Android {@code TimeManager} class introduced in S. */
@Implements(value = TimeManager.class, minSdk = VERSION_CODES.S, isInAndroidSdk = false)
public class ShadowTimeManager {

  public static final String CONFIGURE_GEO_DETECTION_CAPABILITY =
      "configure_geo_detection_capability";

  private TimeZoneCapabilities timeZoneCapabilities = getTimeZoneCapabilities();

  private TimeZoneDetectorStatus detectorStatus =
          new TimeZoneDetectorStatus(
                  DETECTOR_STATUS_RUNNING,
                  new TelephonyTimeZoneAlgorithmStatus(DETECTION_ALGORITHM_STATUS_RUNNING),
                  new LocationTimeZoneAlgorithmStatus(DETECTION_ALGORITHM_STATUS_RUNNING,
                          LocationTimeZoneAlgorithmStatus.PROVIDER_STATUS_NOT_READY, null,
                          LocationTimeZoneAlgorithmStatus.PROVIDER_STATUS_NOT_READY, null));

  private TimeZoneConfiguration timeZoneConfiguration;

  /**
   * Capabilites are predefined and not controlled by user, so they can't be changed via TimeManager
   * API.
   */
  public void setCapabilityState(String capability, @CapabilityState int value) {
    TimeZoneCapabilities.Builder builder = new TimeZoneCapabilities.Builder(timeZoneCapabilities);

    switch (capability) {
      case CONFIGURE_GEO_DETECTION_CAPABILITY:
        builder.setConfigureGeoDetectionEnabledCapability(value);
        break;
      default:
        throw new IllegalArgumentException("Unrecognized capability=" + capability);
    }

    this.timeZoneCapabilities = builder.build();
  }

  @Implementation
  @SystemApi
  protected TimeZoneCapabilitiesAndConfig getTimeZoneCapabilitiesAndConfig()
      throws ClassNotFoundException {
    Objects.requireNonNull(timeZoneConfiguration, "timeZoneConfiguration was not set");

    if (RuntimeEnvironment.getApiLevel() >= U.SDK_INT) {
      Object telephonyAlgoStatus =
          ReflectionHelpers.callConstructor(
              Class.forName("android.app.time.TelephonyTimeZoneAlgorithmStatus"),
              ClassParameter.from(int.class, 3));
      Object locationAlgoStatus =
          ReflectionHelpers.callConstructor(
              Class.forName("android.app.time.LocationTimeZoneAlgorithmStatus"),
              ClassParameter.from(int.class, 3),
              ClassParameter.from(int.class, 3),
              ClassParameter.from(
                  Class.forName("android.service.timezone.TimeZoneProviderStatus"), null),
              ClassParameter.from(int.class, 3),
              ClassParameter.from(
                  Class.forName("android.service.timezone.TimeZoneProviderStatus"), null));

      Object timeZoneDetectorStatus =
          ReflectionHelpers.callConstructor(
              Class.forName("android.app.time.TimeZoneDetectorStatus"),
              ClassParameter.from(int.class, 0),
              ClassParameter.from(
                  Class.forName("android.app.time.TelephonyTimeZoneAlgorithmStatus"),
                  telephonyAlgoStatus),
              ClassParameter.from(
                  Class.forName("android.app.time.LocationTimeZoneAlgorithmStatus"),
                  locationAlgoStatus));
      return ReflectionHelpers.callConstructor(
          TimeZoneCapabilitiesAndConfig.class,
          ClassParameter.from(
              Class.forName("android.app.time.TimeZoneDetectorStatus"), timeZoneDetectorStatus),
          ClassParameter.from(
              Class.forName("android.app.time.TimeZoneCapabilities"), timeZoneCapabilities),
          ClassParameter.from(
              Class.forName("android.app.time.TimeZoneConfiguration"), timeZoneConfiguration));
    } else {
      return ReflectionHelpers.callConstructor(
          TimeZoneCapabilitiesAndConfig.class,
          ClassParameter.from(
              Class.forName("android.app.time.TimeZoneCapabilities"), timeZoneCapabilities),
          ClassParameter.from(
              Class.forName("android.app.time.TimeZoneConfiguration"), timeZoneConfiguration));
    }
  }

  @Implementation
  @SystemApi
  protected boolean updateTimeZoneConfiguration(TimeZoneConfiguration configuration) {
    this.timeZoneConfiguration = configuration;
    return true;
  }

  @Implementation
  protected void addTimeZoneDetectorListener(
      Executor executor, TimeManager.TimeZoneDetectorListener listener) {}

  @Implementation
  protected void removeTimeZoneDetectorListener(TimeManager.TimeZoneDetectorListener listener) {}

  @Implementation
  protected void suggestExternalTime(ExternalTimeSuggestion timeSuggestion) {}

  private TimeZoneCapabilities getTimeZoneCapabilities() {
    TimeZoneCapabilities.Builder timeZoneCapabilitiesBuilder =
        new TimeZoneCapabilities.Builder(UserHandle.CURRENT)
            .setConfigureAutoDetectionEnabledCapability(Capabilities.CAPABILITY_POSSESSED)
            .setConfigureGeoDetectionEnabledCapability(Capabilities.CAPABILITY_POSSESSED);

    if (RuntimeEnvironment.getApiLevel() >= U.SDK_INT) {
      ReflectionHelpers.callInstanceMethod(
          timeZoneCapabilitiesBuilder,
          "setUseLocationEnabled",
          ClassParameter.from(boolean.class, true));
      ReflectionHelpers.callInstanceMethod(
          timeZoneCapabilitiesBuilder,
          "setSetManualTimeZoneCapability",
          ClassParameter.from(int.class, Capabilities.CAPABILITY_POSSESSED));
      return timeZoneCapabilitiesBuilder.build();
    } else {
      ReflectionHelpers.callInstanceMethod(
          timeZoneCapabilitiesBuilder,
          "setSuggestManualTimeZoneCapability",
          ClassParameter.from(int.class, Capabilities.CAPABILITY_POSSESSED));
      return timeZoneCapabilitiesBuilder.build();
    }
  }
}
