package com.nhandev.notificationfix.di;

import com.nhandev.notificationfix.domain.PrivilegeDetector;
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
public final class AppModule_ProvidePrivilegeDetectorFactory implements Factory<PrivilegeDetector> {
  private final Provider<ShizukuHelper> shizukuProvider;

  private final Provider<RootHelper> rootProvider;

  public AppModule_ProvidePrivilegeDetectorFactory(Provider<ShizukuHelper> shizukuProvider,
      Provider<RootHelper> rootProvider) {
    this.shizukuProvider = shizukuProvider;
    this.rootProvider = rootProvider;
  }

  @Override
  public PrivilegeDetector get() {
    return providePrivilegeDetector(shizukuProvider.get(), rootProvider.get());
  }

  public static AppModule_ProvidePrivilegeDetectorFactory create(
      Provider<ShizukuHelper> shizukuProvider, Provider<RootHelper> rootProvider) {
    return new AppModule_ProvidePrivilegeDetectorFactory(shizukuProvider, rootProvider);
  }

  public static PrivilegeDetector providePrivilegeDetector(ShizukuHelper shizuku, RootHelper root) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.providePrivilegeDetector(shizuku, root));
  }
}
