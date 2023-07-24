package com.example.android.unscramble.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides

@Module
class AppModule(private val application: Application) {
    @Provides
    fun providesContext(): Context = application

    @Provides
    fun providesApplication(): Application = application
}