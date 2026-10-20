package com.nhandev.notificationfix.data;

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
public final class FirestoreRepository_Factory implements Factory<FirestoreRepository> {
  private final Provider<Context> contextProvider;

  public FirestoreRepository_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public FirestoreRepository get() {
    return newInstance(contextProvider.get());
  }

  public static FirestoreRepository_Factory create(Provider<Context> contextProvider) {
    return new FirestoreRepository_Factory(contextProvider);
  }

  public static FirestoreRepository newInstance(Context context) {
    return new FirestoreRepository(context);
  }
}
