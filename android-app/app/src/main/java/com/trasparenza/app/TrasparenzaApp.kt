package com.trasparenza.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class for Trasparenza
 * Uses Hilt for dependency injection
 */
@HiltAndroidApp
class TrasparenzaApp : Application()

