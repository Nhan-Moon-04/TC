package com.nhandev.notificationfix.di;

import android.content.Context;
import com.nhandev.notificationfix.domain.RootHelper;
import com.nhandev.notificationfix.domain.ShizukuHelper;
import com.nhandev.notificationfix.domain.StatusChecker;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideStatusCheckerFactory implements Factory<StatusChecker> {
  private final Provider<Context> ctxProvider;

  private final Provider<ShizukuHelper> shizukuProvider;

  private final Provider<RootHelper> rootProvider;

  public AppModule_ProvideStatusCheckerFactory(Provider<Context> ctxProvider,
      Provider<ShizukuHelper> shizukuProvider, Provider<RootHelper> rootProvider) {
    this.ctxProvider = ctxProvider;
    this.shizukuProvider = shizukuProvider;
    this.rootProvider = rootProvider;
  }

  @Override
  public StatusChecker get() {
    return provideStatusChecker(ctxProvider.get(), shizukuProvider.get(), rootProvider.get());
  }

  public static AppModule_ProvideStatusCheckerFactory create(Provider<Context> ctxProvider,
      Provider<ShizukuHelper> shizukuProvider, Provider<RootHelper> rootProvider) {
    return new AppModule_ProvideStatusCheckerFactory(ctxProvider, shizukuProvider, rootProvider);
  }

  public static StatusChecker provideStatusChecker(Context ctx, ShizukuHelper shizuku,
      RootHelper root) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideStatusChecker(ctx, shizuku, root));
  }
}
