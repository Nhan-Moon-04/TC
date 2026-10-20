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
public final class DeviceProfileManager_Factory implements Factory<DeviceProfileManager> {
  private final Provider<ShizukuHelper> shizukuProvider;

  private final Provider<RootHelper> rootProvider;

  public DeviceProfileManager_Factory(Provider<ShizukuHelper> shizukuProvider,
      Provider<RootHelper> rootProvider) {
    this.shizukuProvider = shizukuProvider;
    this.rootProvider = rootProvider;
  }

  @Override
  public DeviceProfileManager get() {
    return newInstance(shizukuProvider.get(), rootProvider.get());
  }

  public static DeviceProfileManager_Factory create(Provider<ShizukuHelper> shizukuProvider,
      Provider<RootHelper> rootProvider) {
    return new DeviceProfileManager_Factory(shizukuProvider, rootProvider);
  }

  public static DeviceProfileManager newInstance(ShizukuHelper shizuku, RootHelper root) {
    return new DeviceProfileManager(shizuku, root);
  }
}
