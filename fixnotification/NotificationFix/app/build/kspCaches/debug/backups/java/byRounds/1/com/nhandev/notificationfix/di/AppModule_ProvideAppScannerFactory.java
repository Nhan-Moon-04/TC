package com.nhandev.notificationfix.di;

import android.content.Context;
import com.nhandev.notificationfix.domain.AppScanner;
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
public final class AppModule_ProvideAppScannerFactory implements Factory<AppScanner> {
  private final Provider<Context> ctxProvider;

  public AppModule_ProvideAppScannerFactory(Provider<Context> ctxProvider) {
    this.ctxProvider = ctxProvider;
  }

  @Override
  public AppScanner get() {
    return provideAppScanner(ctxProvider.get());
  }

  public static AppModule_ProvideAppScannerFactory create(Provider<Context> ctxProvider) {
    return new AppModule_ProvideAppScannerFactory(ctxProvider);
  }

  public static AppScanner provideAppScanner(Context ctx) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAppScanner(ctx));
  }
}
