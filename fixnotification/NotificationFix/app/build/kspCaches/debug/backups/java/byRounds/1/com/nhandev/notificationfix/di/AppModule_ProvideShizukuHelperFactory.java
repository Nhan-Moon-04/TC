package com.nhandev.notificationfix.di;

import com.nhandev.notificationfix.domain.ShizukuHelper;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class AppModule_ProvideShizukuHelperFactory implements Factory<ShizukuHelper> {
  @Override
  public ShizukuHelper get() {
    return provideShizukuHelper();
  }

  public static AppModule_ProvideShizukuHelperFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ShizukuHelper provideShizukuHelper() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideShizukuHelper());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideShizukuHelperFactory INSTANCE = new AppModule_ProvideShizukuHelperFactory();
  }
}
