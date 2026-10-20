package com.nhandev.notificationfix.di;

import com.nhandev.notificationfix.domain.DeviceProfileManager;
import com.nhandev.notificationfix.domain.FixEngine;
import com.nhandev.notificationfix.domain.RootHelper;
import com.nhandev.notificationfix.domain.ShizukuHelper;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideFixEngineFactory implements Factory<FixEngine> {
  private final Provider<ShizukuHelper> shizukuProvider;

  private final Provider<RootHelper> rootProvider;

  private final Provider<DeviceProfileManager> deviceProfileManagerProvider;

  public AppModule_ProvideFixEngineFactory(Provider<ShizukuHelper> shizukuProvider,
      Provider<RootHelper> rootProvider,
      Provider<DeviceProfileManager> deviceProfileManagerProvider) {
    this.shizukuProvider = shizukuProvider;
    this.rootProvider = rootProvider;
    this.deviceProfileManagerProvider = deviceProfileManagerProvider;
  }

  @Override
  public FixEngine get() {
    return provideFixEngine(shizukuProvider.get(), rootProvider.get(), deviceProfileManagerProvider.get());
  }

  public static AppModule_ProvideFixEngineFactory create(Provider<ShizukuHelper> shizukuProvider,
      Provider<RootHelper> rootProvider,
      Provider<DeviceProfileManager> deviceProfileManagerProvider) {
    return new AppModule_ProvideFixEngineFactory(shizukuProvider, rootProvider, deviceProfileManagerProvider);
  }

  public static FixEngine provideFixEngine(ShizukuHelper shizuku, RootHelper root,
      DeviceProfileManager deviceProfileManager) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideFixEngine(shizuku, root, deviceProfileManager));
  }
}
