package com.nhandev.notificationfix.di

import android.content.Context
import com.nhandev.notificationfix.data.FirestoreRepository
import com.nhandev.notificationfix.domain.AppScanner
import com.nhandev.notificationfix.domain.FixEngine
import com.nhandev.notificationfix.domain.PrivilegeDetector
import com.nhandev.notificationfix.domain.RootHelper
import com.nhandev.notificationfix.domain.ShizukuHelper
import com.nhandev.notificationfix.domain.StatusChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideShizukuHelper() = ShizukuHelper()

    @Provides @Singleton
    fun provideRootHelper() = RootHelper()

    @Provides @Singleton
    fun providePrivilegeDetector(
        shizuku: ShizukuHelper,
        root: RootHelper,
    ) = PrivilegeDetector(shizuku, root)

    @Provides @Singleton
    fun provideDeviceProfileManager(
        shizuku: ShizukuHelper,
        root: RootHelper,
    ) = com.nhandev.notificationfix.domain.DeviceProfileManager(shizuku, root)

    @Provides @Singleton
    fun provideFixEngine(
        @ApplicationContext ctx: Context,
        shizuku: ShizukuHelper,
        root: RootHelper,
        deviceProfileManager: com.nhandev.notificationfix.domain.DeviceProfileManager,
    ) = FixEngine(ctx, shizuku, root, deviceProfileManager)

    @Provides @Singleton
    fun provideStatusChecker(
        @ApplicationContext ctx: Context,
        shizuku: ShizukuHelper,
        root: RootHelper,
    ) = StatusChecker(ctx, shizuku, root)

    @Provides @Singleton
    fun provideAppScanner(
        @ApplicationContext ctx: Context,
    ) = AppScanner(ctx)

    @Provides @Singleton
    fun provideFirestoreRepository(
        @ApplicationContext ctx: Context,
    ) = FirestoreRepository(ctx)
}
