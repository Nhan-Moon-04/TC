package com.nhandev.notificationfix.domain;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class RootHelper_Factory implements Factory<RootHelper> {
  @Override
  public RootHelper get() {
    return newInstance();
  }

  public static RootHelper_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RootHelper newInstance() {
    return new RootHelper();
  }

  private static final class InstanceHolder {
    private static final RootHelper_Factory INSTANCE = new RootHelper_Factory();
  }
}
