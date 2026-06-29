package com.asifjawad.fittrack

import android.app.Application
import com.asifjawad.fittrack.data.local.FitTrackDatabase
import com.asifjawad.fittrack.repository.MeasurementRepository
import com.asifjawad.fittrack.repository.NutritionRepository
import com.asifjawad.fittrack.repository.ProfileRepository

class FitTrackApplication : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(FitTrackDatabase.getInstance(this))
    }
}

data class AppContainer(
    val database: FitTrackDatabase
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
            mealDao = database.mealLogDao()
        )
    }
}
