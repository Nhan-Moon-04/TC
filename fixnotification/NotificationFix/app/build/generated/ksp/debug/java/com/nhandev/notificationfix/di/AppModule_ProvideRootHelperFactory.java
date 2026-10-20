package com.nhandev.notificationfix.di;

import com.nhandev.notificationfix.domain.RootHelper;
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
public final class AppModule_ProvideRootHelperFactory implements Factory<RootHelper> {
  @Override
  public RootHelper get() {
    return provideRootHelper();
  }

  public static AppModule_ProvideRootHelperFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RootHelper provideRootHelper() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideRootHelper());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideRootHelperFactory INSTANCE = new AppModule_ProvideRootHelperFactory();
  }
}
