package com.nhandev.notificationfix.domain;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AppScanner_Factory implements Factory<AppScanner> {
  private final Provider<Context> contextProvider;

  public AppScanner_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AppScanner get() {
    return newInstance(contextProvider.get());
  }

  public static AppScanner_Factory create(Provider<Context> contextProvider) {
    return new AppScanner_Factory(contextProvider);
  }

  public static AppScanner newInstance(Context context) {
    return new AppScanner(context);
  }
}
