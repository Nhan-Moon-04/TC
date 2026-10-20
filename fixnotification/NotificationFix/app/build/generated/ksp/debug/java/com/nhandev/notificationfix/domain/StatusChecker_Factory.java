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
public final class StatusChecker_Factory implements Factory<StatusChecker> {
  private final Provider<Context> contextProvider;

  private final Provider<ShizukuHelper> shizukuProvider;

  private final Provider<RootHelper> rootProvider;

  public StatusChecker_Factory(Provider<Context> contextProvider,
      Provider<ShizukuHelper> shizukuProvider, Provider<RootHelper> rootProvider) {
    this.contextProvider = contextProvider;
    this.shizukuProvider = shizukuProvider;
    this.rootProvider = rootProvider;
  }

  @Override
  public StatusChecker get() {
    return newInstance(contextProvider.get(), shizukuProvider.get(), rootProvider.get());
  }

  public static StatusChecker_Factory create(Provider<Context> contextProvider,
      Provider<ShizukuHelper> shizukuProvider, Provider<RootHelper> rootProvider) {
    return new StatusChecker_Factory(contextProvider, shizukuProvider, rootProvider);
  }

  public static StatusChecker newInstance(Context context, ShizukuHelper shizuku, RootHelper root) {
    return new StatusChecker(context, shizuku, root);
  }
}
