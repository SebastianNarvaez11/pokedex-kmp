package com.sebastiannarvaez.pokedex.di

import android.content.Context
import com.sebastiannarvaez.pokedex.data.local.getDatabaseBuilder
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { getDatabaseBuilder(androidContext()) }
}
