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
public final class ShizukuHelper_Factory implements Factory<ShizukuHelper> {
  @Override
  public ShizukuHelper get() {
    return newInstance();
  }

  public static ShizukuHelper_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ShizukuHelper newInstance() {
    return new ShizukuHelper();
  }

  private static final class InstanceHolder {
    private static final ShizukuHelper_Factory INSTANCE = new ShizukuHelper_Factory();
  }
}
