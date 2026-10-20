package com.nhandev.notificationfix;

import com.nhandev.notificationfix.domain.ShizukuHelper;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<ShizukuHelper> shizukuHelperProvider;

  public MainActivity_MembersInjector(Provider<ShizukuHelper> shizukuHelperProvider) {
    this.shizukuHelperProvider = shizukuHelperProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<ShizukuHelper> shizukuHelperProvider) {
    return new MainActivity_MembersInjector(shizukuHelperProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectShizukuHelper(instance, shizukuHelperProvider.get());
  }

  @InjectedFieldSignature("com.nhandev.notificationfix.MainActivity.shizukuHelper")
  public static void injectShizukuHelper(MainActivity instance, ShizukuHelper shizukuHelper) {
    instance.shizukuHelper = shizukuHelper;
  }
}
