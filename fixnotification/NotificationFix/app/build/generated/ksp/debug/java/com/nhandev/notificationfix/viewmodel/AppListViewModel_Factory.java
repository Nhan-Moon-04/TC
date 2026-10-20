package com.nhandev.notificationfix.viewmodel;

import android.content.Context;
import com.nhandev.notificationfix.data.FirestoreRepository;
import com.nhandev.notificationfix.domain.AppScanner;
import com.nhandev.notificationfix.domain.FixEngine;
import com.nhandev.notificationfix.domain.PrivilegeDetector;
import com.nhandev.notificationfix.domain.ShizukuHelper;
import com.nhandev.notificationfix.domain.StatusChecker;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class AppListViewModel_Factory implements Factory<AppListViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<AppScanner> scannerProvider;

  private final Provider<StatusChecker> checkerProvider;

  private final Provider<FixEngine> fixEngineProvider;

  private final Provider<PrivilegeDetector> privilegeDetectorProvider;

  private final Provider<ShizukuHelper> shizukuProvider;

  private final Provider<FirestoreRepository> firestoreProvider;

  public AppListViewModel_Factory(Provider<Context> contextProvider,
      Provider<AppScanner> scannerProvider, Provider<StatusChecker> checkerProvider,
      Provider<FixEngine> fixEngineProvider, Provider<PrivilegeDetector> privilegeDetectorProvider,
      Provider<ShizukuHelper> shizukuProvider, Provider<FirestoreRepository> firestoreProvider) {
    this.contextProvider = contextProvider;
    this.scannerProvider = scannerProvider;
    this.checkerProvider = checkerProvider;
    this.fixEngineProvider = fixEngineProvider;
    this.privilegeDetectorProvider = privilegeDetectorProvider;
    this.shizukuProvider = shizukuProvider;
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public AppListViewModel get() {
    return newInstance(contextProvider.get(), scannerProvider.get(), checkerProvider.get(), fixEngineProvider.get(), privilegeDetectorProvider.get(), shizukuProvider.get(), firestoreProvider.get());
  }

  public static AppListViewModel_Factory create(Provider<Context> contextProvider,
      Provider<AppScanner> scannerProvider, Provider<StatusChecker> checkerProvider,
      Provider<FixEngine> fixEngineProvider, Provider<PrivilegeDetector> privilegeDetectorProvider,
      Provider<ShizukuHelper> shizukuProvider, Provider<FirestoreRepository> firestoreProvider) {
    return new AppListViewModel_Factory(contextProvider, scannerProvider, checkerProvider, fixEngineProvider, privilegeDetectorProvider, shizukuProvider, firestoreProvider);
  }

  public static AppListViewModel newInstance(Context context, AppScanner scanner,
      StatusChecker checker, FixEngine fixEngine, PrivilegeDetector privilegeDetector,
      ShizukuHelper shizuku, FirestoreRepository firestore) {
    return new AppListViewModel(context, scanner, checker, fixEngine, privilegeDetector, shizuku, firestore);
  }
}
