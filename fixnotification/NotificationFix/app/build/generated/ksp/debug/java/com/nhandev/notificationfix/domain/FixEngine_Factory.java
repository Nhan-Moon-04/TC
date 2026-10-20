package com.nhandev.notificationfix.domain;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class FixEngine_Factory implements Factory<FixEngine> {
  private final Provider<ShizukuHelper> shizukuProvider;

  private final Provider<RootHelper> rootProvider;

  private final Provider<DeviceProfileManager> deviceProfileManagerProvider;

  public FixEngine_Factory(Provider<ShizukuHelper> shizukuProvider,
      Provider<RootHelper> rootProvider,
      Provider<DeviceProfileManager> deviceProfileManagerProvider) {
    this.shizukuProvider = shizukuProvider;
    this.rootProvider = rootProvider;
    this.deviceProfileManagerProvider = deviceProfileManagerProvider;
  }

  @Override
  public FixEngine get() {
    return newInstance(shizukuProvider.get(), rootProvider.get(), deviceProfileManagerProvider.get());
  }

  public static FixEngine_Factory create(Provider<ShizukuHelper> shizukuProvider,
      Provider<RootHelper> rootProvider,
      Provider<DeviceProfileManager> deviceProfileManagerProvider) {
    return new FixEngine_Factory(shizukuProvider, rootProvider, deviceProfileManagerProvider);
  }

  public static FixEngine newInstance(ShizukuHelper shizuku, RootHelper root,
      DeviceProfileManager deviceProfileManager) {
    return new FixEngine(shizuku, root, deviceProfileManager);
  }
}
