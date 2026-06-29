package com.asifjawad.fittrack

import android.app.Application
import com.asifjawad.fittrack.data.health.HealthConnectManager
import com.asifjawad.fittrack.data.local.FitTrackDatabase
import com.asifjawad.fittrack.repository.HealthRepository
import com.asifjawad.fittrack.repository.MeasurementRepository
import com.asifjawad.fittrack.repository.NutritionRepository
import com.asifjawad.fittrack.repository.ProfileRepository

class FitTrackApplication : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(
            database = FitTrackDatabase.getInstance(this),
            healthConnectManager = HealthConnectManager(this)
        )
    }
}

data class AppContainer(
    val database: FitTrackDatabase,
    val healthConnectManager: HealthConnectManager
) {
    val profileRepository: ProfileRepository by lazy {
        ProfileRepository(database.userProfileDao())
    }

    val measurementRepository: MeasurementRepository by lazy {
        MeasurementRepository(
            weightDao = database.weightLogDao(),
            waistDao = database.waistLogDao()
        )
    }

    val nutritionRepository: NutritionRepository by lazy {
        NutritionRepository(
            foodDao = database.foodDao(),
            recipeDao = database.recipeDao(),
            mealDao = database.mealLogDao()
        )
    }

    val healthRepository: HealthRepository by lazy {
        HealthRepository(
            manager = healthConnectManager,
            dailySummaryDao = database.dailySummaryDao(),
            syncStateDao = database.syncStateDao()
        )
    }
}
