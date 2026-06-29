package com.asifjawad.fittrack.domain.calculation

import com.asifjawad.fittrack.domain.model.ActivityLevel
import com.asifjawad.fittrack.domain.model.MealLog
import com.asifjawad.fittrack.domain.model.Sex
import com.asifjawad.fittrack.domain.model.UserProfile
import com.asifjawad.fittrack.domain.model.WeightLog
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object BmrCalculator {
    fun mifflinStJeor(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        sex: Sex
    ): Double {
        val sexOffset = when (sex) {
            Sex.Male -> 5.0
            Sex.Female -> -161.0
        }
        return (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age) + sexOffset
    }
}

object TdeeCalculator {
    fun estimate(bmr: Double, activityLevel: ActivityLevel): Double {
        return bmr * activityLevel.multiplier
    }
}

object FoodCalorieCalculator {
    fun caloriesForGrams(caloriesPer100g: Double, grams: Double): Double {
        return caloriesPer100g * (grams / 100.0)
    }
}

object ExerciseCalculator {
    fun caloriesPerMinute(weightKg: Double, met: Double): Double {
        return met * 3.5 * weightKg / 200.0
    }

    fun minutesForCalories(calories: Double, weightKg: Double, met: Double): Double {
        if (calories <= 0.0) return 0.0
        return calories / caloriesPerMinute(weightKg, met)
    }
}

object TrendWeightCalculator {
    fun sevenDayTrend(
        logs: List<WeightLog>,
        currentDate: LocalDate
    ): Double? {
        val dailyLogs = logs
            .filter { !it.date.isAfter(currentDate) }
            .sortedByDescending { it.date }
            .distinctBy { it.date }
            .take(7)

        return dailyLogs
            .takeIf { it.isNotEmpty() }
            ?.map { it.weightKg }
            ?.average()
    }
}

object GoalProgressCalculator {
    const val KCAL_PER_KG = 7700.0

    fun plannedWeightToday(
        startWeightKg: Double,
        targetWeightKg: Double,
        startDate: LocalDate,
        goalDate: LocalDate,
        currentDate: LocalDate
    ): Double {
        val totalDays = max(1, ChronoUnit.DAYS.between(startDate, goalDate))
        val elapsedDays = ChronoUnit.DAYS.between(startDate, currentDate).coerceIn(0, totalDays)
        val progressRatio = elapsedDays.toDouble() / totalDays.toDouble()
        return startWeightKg + ((targetWeightKg - startWeightKg) * progressRatio)
    }

    fun trajectoryScore(
        startWeightKg: Double,
        plannedWeightKg: Double,
        trendWeightKg: Double
    ): Double? {
        val plannedChange = plannedWeightKg - startWeightKg
        if (abs(plannedChange) < 0.01) return null
        val actualChange = trendWeightKg - startWeightKg
        return 100.0 * actualChange / plannedChange
    }

    fun requiredDailyEnergyAdjustment(
        startWeightKg: Double,
        targetWeightKg: Double,
        startDate: LocalDate,
        goalDate: LocalDate
    ): Double {
        val totalDays = max(1, ChronoUnit.DAYS.between(startDate, goalDate))
        val signedWeightChangeKg = startWeightKg - targetWeightKg
        return (signedWeightChangeKg * KCAL_PER_KG) / totalDays.toDouble()
    }
}

object DailyPlanCalculator {
    private const val MAX_DEFAULT_FOOD_DEFICIT = 500.0
    private const val BRISK_WALK_MET = 4.3
    private const val MODERATE_STRENGTH_MET = 3.8

    fun create(
        tdee: Double,
        requiredDailyEnergyAdjustment: Double,
        profile: UserProfile,
        weightKg: Double
    ): DailyPlan {
        val minimumFoodCalories = when (profile.sex) {
            Sex.Male -> 1500.0
            Sex.Female -> 1200.0
        }
        val maxFoodDeficitFromFloor = max(0.0, tdee - minimumFoodCalories)

        val foodDeficit = if (requiredDailyEnergyAdjustment > 0.0) {
            min(requiredDailyEnergyAdjustment, min(MAX_DEFAULT_FOOD_DEFICIT, maxFoodDeficitFromFloor))
        } else {
            0.0
        }
        val plannedExerciseCalories = if (requiredDailyEnergyAdjustment > 0.0) {
            max(0.0, requiredDailyEnergyAdjustment - foodDeficit)
        } else {
            0.0
        }
        val foodCalorieTarget = if (requiredDailyEnergyAdjustment >= 0.0) {
            tdee - foodDeficit
        } else {
            tdee + abs(requiredDailyEnergyAdjustment)
        }
        val briskWalkMinutes = ExerciseCalculator.minutesForCalories(
            calories = plannedExerciseCalories,
            weightKg = weightKg,
            met = BRISK_WALK_MET
        )
        val strengthMinutes = ExerciseCalculator.minutesForCalories(
            calories = plannedExerciseCalories,
            weightKg = weightKg,
            met = MODERATE_STRENGTH_MET
        )

        val label = when {
            requiredDailyEnergyAdjustment > 0.0 -> "Weight-loss plan"
            requiredDailyEnergyAdjustment < 0.0 -> "Weight-gain plan"
            else -> "Maintenance plan"
        }

        val note = when {
            requiredDailyEnergyAdjustment > foodDeficit + 700.0 ->
                "This goal needs a large daily burn. Extending the goal date would make it easier to sustain."
            requiredDailyEnergyAdjustment > 0.0 && plannedExerciseCalories > 0.0 ->
                "Use food control plus planned movement; Health Connect activity will refine this in Phase 5."
            requiredDailyEnergyAdjustment > 0.0 ->
                "The current goal can be handled mostly through food intake if logging stays accurate."
            requiredDailyEnergyAdjustment < 0.0 ->
                "This target needs a calorie surplus; strength training helps that extra energy become useful mass."
            else ->
                "Keep food intake near TDEE and use exercise for health and consistency."
        }

        return DailyPlan(
            label = label,
            foodCalorieTarget = foodCalorieTarget,
            foodDeficit = foodDeficit,
            plannedExerciseCalories = plannedExerciseCalories,
            briskWalkMinutes = briskWalkMinutes,
            strengthMinutes = strengthMinutes,
            minimumFoodCalories = minimumFoodCalories,
            note = note
        )
    }
}

object ProgressEngine {
    fun calculate(
        profile: UserProfile,
        weightLogs: List<WeightLog>,
        mealsToday: List<MealLog>,
        currentDate: LocalDate = LocalDate.now()
    ): ProgressMetrics {
        val startDate = weightLogs.minOfOrNull { it.date } ?: currentDate
        val daysRemaining = ChronoUnit.DAYS.between(currentDate, profile.goalDate).coerceAtLeast(0)
        val trendWeight = TrendWeightCalculator.sevenDayTrend(weightLogs, currentDate)
        val calculationWeight = trendWeight ?: profile.startWeightKg
        val plannedWeight = GoalProgressCalculator.plannedWeightToday(
            startWeightKg = profile.startWeightKg,
            targetWeightKg = profile.targetWeightKg,
            startDate = startDate,
            goalDate = profile.goalDate,
            currentDate = currentDate
        )
        val trajectoryScore = trendWeight?.let {
            GoalProgressCalculator.trajectoryScore(
                startWeightKg = profile.startWeightKg,
                plannedWeightKg = plannedWeight,
                trendWeightKg = it
            )
        }
        val bmr = BmrCalculator.mifflinStJeor(
            weightKg = calculationWeight,
            heightCm = profile.heightCm,
            age = profile.age,
            sex = profile.sex
        )
        val tdee = TdeeCalculator.estimate(bmr, profile.activityLevel)
        val requiredDailyEnergyAdjustment = GoalProgressCalculator.requiredDailyEnergyAdjustment(
            startWeightKg = profile.startWeightKg,
            targetWeightKg = profile.targetWeightKg,
            startDate = startDate,
            goalDate = profile.goalDate
        )
        val dailyPlan = DailyPlanCalculator.create(
            tdee = tdee,
            requiredDailyEnergyAdjustment = requiredDailyEnergyAdjustment,
            profile = profile,
            weightKg = calculationWeight
        )
        val caloriesIn = mealsToday.sumOf { it.calories }
        val estimatedEnergyGap = when {
            requiredDailyEnergyAdjustment > 0.0 -> tdee - caloriesIn
            requiredDailyEnergyAdjustment < 0.0 -> caloriesIn - tdee
            else -> caloriesIn - tdee
        }

        return ProgressMetrics(
            startDate = startDate,
            currentDate = currentDate,
            daysRemaining = daysRemaining,
            trendWeightKg = trendWeight,
            calculationWeightKg = calculationWeight,
            plannedWeightKg = plannedWeight,
            trajectoryScore = trajectoryScore,
            bmr = bmr,
            tdee = tdee,
            requiredDailyEnergyAdjustment = requiredDailyEnergyAdjustment,
            dailyPlan = dailyPlan,
            caloriesIn = caloriesIn,
            estimatedEnergyGap = estimatedEnergyGap,
            recommendation = recommendationFor(
                trajectoryScore = trajectoryScore,
                requiredDailyEnergyAdjustment = requiredDailyEnergyAdjustment,
                dailyPlan = dailyPlan
            ),
            formulaLines = formulaLines(
                profile = profile,
                calculationWeight = calculationWeight,
                plannedWeight = plannedWeight,
                trendWeight = trendWeight,
                trajectoryScore = trajectoryScore,
                bmr = bmr,
                tdee = tdee,
                requiredDailyEnergyAdjustment = requiredDailyEnergyAdjustment,
                dailyPlan = dailyPlan,
                caloriesIn = caloriesIn,
                estimatedEnergyGap = estimatedEnergyGap,
                startDate = startDate,
                currentDate = currentDate
            )
        )
    }

    private fun recommendationFor(
        trajectoryScore: Double?,
        requiredDailyEnergyAdjustment: Double,
        dailyPlan: DailyPlan
    ): String {
        if (requiredDailyEnergyAdjustment < 0.0) {
            return "Eat near ${dailyPlan.foodCalorieTarget.roundOne()} kcal/day and keep strength training consistent."
        }
        if (requiredDailyEnergyAdjustment == 0.0) {
            return "Maintain around ${dailyPlan.foodCalorieTarget.roundOne()} kcal/day and keep activity steady."
        }
        return when {
            trajectoryScore == null -> "Log weight regularly; today aim for ${dailyPlan.foodCalorieTarget.roundOne()} kcal and ${dailyPlan.plannedExerciseCalories.roundOne()} exercise kcal."
            trajectoryScore >= 100.0 -> "On pace. Keep today near ${dailyPlan.foodCalorieTarget.roundOne()} kcal and maintain planned movement."
            trajectoryScore >= 90.0 -> "Slightly behind. Tighten food logging and complete the planned activity burn."
            trajectoryScore >= 80.0 -> "Behind. Hit the food target and add about ${dailyPlan.briskWalkMinutes.roundOne()} minutes brisk walking."
            else -> "Far behind. Review the goal date, food portions, and daily movement target."
        }
    }

    private fun formulaLines(
        profile: UserProfile,
        calculationWeight: Double,
        plannedWeight: Double,
        trendWeight: Double?,
        trajectoryScore: Double?,
        bmr: Double,
        tdee: Double,
        requiredDailyEnergyAdjustment: Double,
        dailyPlan: DailyPlan,
        caloriesIn: Double,
        estimatedEnergyGap: Double,
        startDate: LocalDate,
        currentDate: LocalDate
    ): List<CalculationLine> {
        val totalDays = max(1, ChronoUnit.DAYS.between(startDate, profile.goalDate))
        val elapsedDays = ChronoUnit.DAYS.between(startDate, currentDate).coerceIn(0, totalDays)
        val targetChangeKg = profile.targetWeightKg - profile.startWeightKg
        val plannedChangeKg = plannedWeight - profile.startWeightKg
        val actualChangeKg = trendWeight?.let { it - profile.startWeightKg }
        val energyDirection = when {
            requiredDailyEnergyAdjustment > 0.0 -> "deficit"
            requiredDailyEnergyAdjustment < 0.0 -> "surplus"
            else -> "maintenance"
        }

        return listOf(
            CalculationLine(
                label = "BMR",
                formula = "10 x weight + 6.25 x height - 5 x age + sex offset",
                result = "${bmr.roundOne()} kcal/day using ${calculationWeight.roundOne()} kg"
            ),
            CalculationLine(
                label = "TDEE",
                formula = "BMR x activity multiplier",
                result = "${tdee.roundOne()} kcal/day (${profile.activityLevel.label}, x${profile.activityLevel.multiplier})"
            ),
            CalculationLine(
                label = "Daily energy adjustment",
                formula = "(start weight - target weight) x 7700 / goal days",
                result = "${abs(requiredDailyEnergyAdjustment).roundOne()} kcal/day $energyDirection for ${targetChangeKg.roundOne()} kg over $totalDays days"
            ),
            CalculationLine(
                label = "Food target",
                formula = "TDEE adjusted by goal pressure, with food deficit capped before assigning exercise",
                result = "${dailyPlan.foodCalorieTarget.roundOne()} kcal/day"
            ),
            CalculationLine(
                label = "Exercise target",
                formula = "Remaining required deficit after food target",
                result = if (dailyPlan.plannedExerciseCalories > 0.0) {
                    "${dailyPlan.plannedExerciseCalories.roundOne()} kcal/day, about ${dailyPlan.briskWalkMinutes.roundOne()} min brisk walking"
                } else {
                    "No extra exercise calories required by the math today"
                }
            ),
            CalculationLine(
                label = "Planned weight today",
                formula = "start + ((target - start) x elapsed days / total days)",
                result = "${plannedWeight.roundOne()} kg after $elapsedDays of $totalDays days"
            ),
            CalculationLine(
                label = "Trend weight",
                formula = "Average of latest available daily weights, up to 7 days",
                result = trendWeight?.let { "${it.roundOne()} kg" } ?: "Not enough logged weight data"
            ),
            CalculationLine(
                label = "Trajectory score",
                formula = "100 x actual signed change / planned signed change",
                result = if (trajectoryScore == null || abs(plannedChangeKg) < 0.01 || actualChangeKg == null) {
                    "Starts after the goal line moves enough and weight is logged"
                } else {
                    "${trajectoryScore.roundOne()}% (${actualChangeKg.roundOne()} kg actual / ${plannedChangeKg.roundOne()} kg planned)"
                }
            ),
            CalculationLine(
                label = "Today from food log",
                formula = if (requiredDailyEnergyAdjustment >= 0.0) "TDEE - calories logged" else "calories logged - TDEE",
                result = "${estimatedEnergyGap.roundOne()} kcal vs ${caloriesIn.roundOne()} kcal logged"
            )
        )
    }

    private fun Double.roundOne(): String = "%,.1f".format(this)
}

data class ProgressMetrics(
    val startDate: LocalDate,
    val currentDate: LocalDate,
    val daysRemaining: Long,
    val trendWeightKg: Double?,
    val calculationWeightKg: Double,
    val plannedWeightKg: Double,
    val trajectoryScore: Double?,
    val bmr: Double,
    val tdee: Double,
    val requiredDailyEnergyAdjustment: Double,
    val dailyPlan: DailyPlan,
    val caloriesIn: Double,
    val estimatedEnergyGap: Double,
    val recommendation: String,
    val formulaLines: List<CalculationLine>
)

data class DailyPlan(
    val label: String,
    val foodCalorieTarget: Double,
    val foodDeficit: Double,
    val plannedExerciseCalories: Double,
    val briskWalkMinutes: Double,
    val strengthMinutes: Double,
    val minimumFoodCalories: Double,
    val note: String
)

data class CalculationLine(
    val label: String,
    val formula: String,
    val result: String
)
