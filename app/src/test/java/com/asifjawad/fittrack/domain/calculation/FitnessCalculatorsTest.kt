package com.asifjawad.fittrack.domain.calculation

import com.asifjawad.fittrack.domain.model.ActivityLevel
import com.asifjawad.fittrack.domain.model.MealLog
import com.asifjawad.fittrack.domain.model.MealType
import com.asifjawad.fittrack.domain.model.Sex
import com.asifjawad.fittrack.domain.model.UserProfile
import com.asifjawad.fittrack.domain.model.WeightLog
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FitnessCalculatorsTest {
    @Test
    fun bmrUsesMifflinStJeorFormula() {
        val bmr = BmrCalculator.mifflinStJeor(
            weightKg = 90.0,
            heightCm = 180.0,
            age = 28,
            sex = Sex.Male
        )

        assertEquals(1890.0, bmr, 0.01)
    }

    @Test
    fun plannedWeightUsesUserGoalNotFixedTarget() {
        val startDate = LocalDate.of(2026, 1, 1)
        val currentDate = LocalDate.of(2026, 1, 31)
        val goalDate = LocalDate.of(2026, 4, 1)

        val planned = GoalProgressCalculator.plannedWeightToday(
            startWeightKg = 95.0,
            targetWeightKg = 87.0,
            startDate = startDate,
            goalDate = goalDate,
            currentDate = currentDate
        )

        assertEquals(92.333, planned, 0.01)
    }

    @Test
    fun trajectoryScoreWorksForWeightLossAndWeightGainGoals() {
        val lossScore = GoalProgressCalculator.trajectoryScore(
            startWeightKg = 90.0,
            plannedWeightKg = 88.0,
            trendWeightKg = 87.0
        )
        val gainScore = GoalProgressCalculator.trajectoryScore(
            startWeightKg = 70.0,
            plannedWeightKg = 72.0,
            trendWeightKg = 71.0
        )

        assertEquals(150.0, lossScore!!, 0.01)
        assertEquals(50.0, gainScore!!, 0.01)
    }

    @Test
    fun trendWeightAveragesLatestDistinctDailyWeights() {
        val today = LocalDate.of(2026, 6, 29)
        val logs = (0..8).map { index ->
            WeightLog(
                id = index.toLong(),
                date = today.minusDays(index.toLong()),
                weightKg = 90.0 - index,
                note = null
            )
        }

        val trend = TrendWeightCalculator.sevenDayTrend(logs, today)

        assertEquals(87.0, trend!!, 0.01)
    }

    @Test
    fun dailyPlanSplitsAggressiveLossBetweenFoodAndExercise() {
        val profile = sampleProfile(
            startWeightKg = 90.0,
            targetWeightKg = 84.0,
            goalDate = LocalDate.of(2026, 3, 26)
        )
        val bmr = BmrCalculator.mifflinStJeor(90.0, 180.0, 28, Sex.Male)
        val tdee = TdeeCalculator.estimate(bmr, ActivityLevel.Light)
        val required = GoalProgressCalculator.requiredDailyEnergyAdjustment(
            startWeightKg = profile.startWeightKg,
            targetWeightKg = profile.targetWeightKg,
            startDate = LocalDate.of(2026, 1, 1),
            goalDate = profile.goalDate
        )

        val plan = DailyPlanCalculator.create(
            tdee = tdee,
            requiredDailyEnergyAdjustment = required,
            profile = profile,
            weightKg = 90.0
        )

        assertEquals(550.0, required, 0.01)
        assertEquals(500.0, plan.foodDeficit, 0.01)
        assertEquals(50.0, plan.plannedExerciseCalories, 0.01)
        assertEquals(tdee - 500.0, plan.foodCalorieTarget, 0.01)
        assertTrue(plan.briskWalkMinutes > 0.0)
    }

    @Test
    fun progressEngineUsesMealsAndUserGoalForDynamicTargets() {
        val currentDate = LocalDate.of(2026, 1, 15)
        val profile = sampleProfile(
            startWeightKg = 90.0,
            targetWeightKg = 87.0,
            goalDate = LocalDate.of(2026, 4, 1)
        )
        val weights = listOf(
            WeightLog(1, currentDate, 89.0, null),
            WeightLog(2, currentDate.minusDays(1), 89.2, null),
            WeightLog(3, currentDate.minusDays(2), 89.4, null)
        )
        val meals = listOf(
            MealLog(
                id = 1,
                date = currentDate,
                mealType = MealType.Lunch,
                displayName = "Rice and curry",
                grams = 350.0,
                calories = 650.0,
                protein = 20.0,
                carbs = 80.0,
                fat = 22.0
            )
        )

        val metrics = ProgressEngine.calculate(
            profile = profile,
            weightLogs = weights,
            mealsToday = meals,
            currentDate = currentDate
        )

        assertEquals(650.0, metrics.caloriesIn, 0.01)
        assertTrue(metrics.dailyPlan.foodCalorieTarget > 0.0)
        assertNotNull(metrics.trajectoryScore)
        assertTrue(metrics.formulaLines.any { it.label == "Exercise target" })
    }

    private fun sampleProfile(
        startWeightKg: Double,
        targetWeightKg: Double,
        goalDate: LocalDate
    ): UserProfile {
        return UserProfile(
            age = 28,
            sex = Sex.Male,
            heightCm = 180.0,
            startWeightKg = startWeightKg,
            targetWeightKg = targetWeightKg,
            goalDate = goalDate,
            activityLevel = ActivityLevel.Light
        )
    }
}
