package com.notewise.clone

import android.app.Application
import com.notewise.clone.core.database.AppDatabase

class NoteApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}

