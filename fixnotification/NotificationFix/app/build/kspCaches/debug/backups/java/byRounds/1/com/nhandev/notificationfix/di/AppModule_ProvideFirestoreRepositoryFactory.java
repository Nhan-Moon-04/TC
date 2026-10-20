package com.nhandev.notificationfix.di;

import android.content.Context;
import com.nhandev.notificationfix.data.FirestoreRepository;
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
public final class AppModule_ProvideFirestoreRepositoryFactory implements Factory<FirestoreRepository> {
  private final Provider<Context> ctxProvider;

  public AppModule_ProvideFirestoreRepositoryFactory(Provider<Context> ctxProvider) {
    this.ctxProvider = ctxProvider;
  }

  @Override
  public FirestoreRepository get() {
    return provideFirestoreRepository(ctxProvider.get());
  }

  public static AppModule_ProvideFirestoreRepositoryFactory create(Provider<Context> ctxProvider) {
    return new AppModule_ProvideFirestoreRepositoryFactory(ctxProvider);
  }

  public static FirestoreRepository provideFirestoreRepository(Context ctx) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideFirestoreRepository(ctx));
  }
}
