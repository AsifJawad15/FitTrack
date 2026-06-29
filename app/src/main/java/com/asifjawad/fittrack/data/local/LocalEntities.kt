package com.asifjawad.fittrack.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val age: Int,
    val sex: String,
    val heightCm: Double,
    val startWeightKg: Double,
    val targetWeightKg: Double,
    val goalDateEpochDay: Long,
    val activityLevel: String,
    val updatedAtEpochMillis: Long
)

@Entity(
    tableName = "weight_logs",
    indices = [Index(value = ["dateEpochDay"])]
)
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val weightKg: Double,
    val note: String? = null,
    val createdAtEpochMillis: Long
)

@Entity(
    tableName = "waist_logs",
    indices = [Index(value = ["dateEpochDay"])]
)
data class WaistLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val waistCm: Double,
    val note: String? = null,
    val createdAtEpochMillis: Long
)

@Entity(
    tableName = "foods",
    indices = [Index(value = ["name"], unique = true)]
)
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val isSeed: Boolean,
    val updatedAtEpochMillis: Long
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val servings: Double,
    val notes: String? = null,
    val updatedAtEpochMillis: Long
)

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodItemId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["recipeId"]), Index(value = ["foodItemId"])]
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val foodItemId: Long?,
    val customName: String?,
    val grams: Double,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

data class RecipeWithIngredientsEntity(
    @Embedded val recipe: RecipeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipeId"
    )
    val ingredients: List<RecipeIngredientEntity>
)

@Entity(
    tableName = "meal_logs",
    foreignKeys = [
        ForeignKey(
            entity = FoodItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodItemId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["dateEpochDay"]), Index(value = ["foodItemId"]), Index(value = ["recipeId"])]
)
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val mealType: String,
    val foodItemId: Long? = null,
    val recipeId: Long? = null,
    val displayName: String,
    val grams: Double,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val createdAtEpochMillis: Long
)

@Entity(tableName = "daily_summaries")
data class HealthDailySummaryEntity(
    @PrimaryKey val dateEpochDay: Long,
    val sourceLabel: String,
    val steps: Long?,
    val sleepMinutes: Long?,
    val exerciseMinutes: Long?,
    val activeCalories: Double?,
    val totalCaloriesBurned: Double?,
    val weightKg: Double?,
    val heightCm: Double?,
    val isPending: Boolean,
    val lastSyncEpochMillis: Long
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val key: String,
    val lastSyncEpochMillis: Long,
    val status: String,
    val errorMessage: String?
)
