package com.asifjawad.fittrack.repository

import com.asifjawad.fittrack.data.local.FoodDao
import com.asifjawad.fittrack.data.local.FoodItemEntity
import com.asifjawad.fittrack.data.local.MealLogDao
import com.asifjawad.fittrack.data.local.MealLogEntity
import com.asifjawad.fittrack.data.local.RecipeDao
import com.asifjawad.fittrack.data.local.RecipeEntity
import com.asifjawad.fittrack.data.local.RecipeIngredientEntity
import com.asifjawad.fittrack.data.local.RecipeWithIngredientsEntity
import com.asifjawad.fittrack.data.local.SeedData
import com.asifjawad.fittrack.domain.model.FoodItem
import com.asifjawad.fittrack.domain.model.MealLog
import com.asifjawad.fittrack.domain.model.MealType
import com.asifjawad.fittrack.domain.model.RecipeIngredient
import com.asifjawad.fittrack.domain.model.RecipeIngredientInput
import com.asifjawad.fittrack.domain.model.RecipeSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

class NutritionRepository(
    private val foodDao: FoodDao,
    private val recipeDao: RecipeDao,
    private val mealDao: MealLogDao
) {
    fun observeFoods(): Flow<List<FoodItem>> {
        return foodDao.observeAll().map { foods -> foods.map { it.toDomain() } }
    }

    fun observeRecipes(): Flow<List<RecipeSummary>> {
        return recipeDao.observeRecipesWithIngredients().map { recipes ->
            recipes.map { it.toDomain() }
        }
    }

    fun observeMealsForDate(date: LocalDate): Flow<List<MealLog>> {
        return mealDao.observeByDate(date.toEpochDay()).map { logs ->
            logs.map { entity ->
                MealLog(
                    id = entity.id,
                    date = LocalDate.ofEpochDay(entity.dateEpochDay),
                    mealType = MealType.valueOf(entity.mealType),
                    displayName = entity.displayName,
                    grams = entity.grams,
                    calories = entity.calories,
                    protein = entity.protein,
                    carbs = entity.carbs,
                    fat = entity.fat,
                    amountLabel = if (entity.recipeId != null) {
                        "${entity.grams.trimTrailingZero()} serving"
                    } else {
                        "${entity.grams.trimTrailingZero()} g"
                    }
                )
            }
        }
    }

    suspend fun seedFoodsIfNeeded() {
        val now = Instant.now().toEpochMilli()
        foodDao.insertAll(SeedData.bangladeshiFoods(now))
    }

    suspend fun addCustomFood(
        name: String,
        caloriesPer100g: Double,
        proteinPer100g: Double = 0.0,
        carbsPer100g: Double = 0.0,
        fatPer100g: Double = 0.0
    ) {
        foodDao.insert(
            FoodItemEntity(
                name = name.trim(),
                caloriesPer100g = caloriesPer100g,
                proteinPer100g = proteinPer100g,
                carbsPer100g = carbsPer100g,
                fatPer100g = fatPer100g,
                isSeed = false,
                updatedAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
    }

    suspend fun createRecipe(
        name: String,
        servings: Double,
        ingredients: List<RecipeIngredientInput>,
        notes: String? = null
    ): Boolean {
        if (ingredients.isEmpty()) return false

        val now = Instant.now().toEpochMilli()
        val recipeId = recipeDao.insertRecipe(
            RecipeEntity(
                name = name.trim(),
                servings = servings,
                notes = notes,
                updatedAtEpochMillis = now
            )
        )
        val ingredientEntities = ingredients.mapNotNull { input ->
            val food = foodDao.getById(input.foodId) ?: return@mapNotNull null
            val multiplier = input.grams / 100.0
            RecipeIngredientEntity(
                recipeId = recipeId,
                foodItemId = food.id,
                customName = food.name,
                grams = input.grams,
                calories = food.caloriesPer100g * multiplier,
                protein = food.proteinPer100g * multiplier,
                carbs = food.carbsPer100g * multiplier,
                fat = food.fatPer100g * multiplier
            )
        }

        if (ingredientEntities.isEmpty()) return false
        recipeDao.insertIngredients(ingredientEntities)
        return true
    }

    suspend fun logMealFromFood(
        date: LocalDate,
        mealType: MealType,
        foodId: Long,
        grams: Double
    ): Boolean {
        val food = foodDao.getById(foodId) ?: return false
        val multiplier = grams / 100.0
        mealDao.insert(
            MealLogEntity(
                dateEpochDay = date.toEpochDay(),
                mealType = mealType.name,
                foodItemId = food.id,
                recipeId = null,
                displayName = food.name,
                grams = grams,
                calories = food.caloriesPer100g * multiplier,
                protein = food.proteinPer100g * multiplier,
                carbs = food.carbsPer100g * multiplier,
                fat = food.fatPer100g * multiplier,
                createdAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
        return true
    }

    suspend fun logMealFromRecipe(
        date: LocalDate,
        mealType: MealType,
        recipeId: Long,
        servings: Double
    ): Boolean {
        val recipe = recipeDao.getRecipeWithIngredients(recipeId)?.toDomain() ?: return false
        val multiplier = servings / recipe.servings
        mealDao.insert(
            MealLogEntity(
                dateEpochDay = date.toEpochDay(),
                mealType = mealType.name,
                foodItemId = null,
                recipeId = recipe.id,
                displayName = recipe.name,
                grams = servings,
                calories = recipe.totalCalories * multiplier,
                protein = recipe.totalProtein * multiplier,
                carbs = recipe.totalCarbs * multiplier,
                fat = recipe.totalFat * multiplier,
                createdAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
        return true
    }

    private fun FoodItemEntity.toDomain(): FoodItem {
        return FoodItem(
            id = id,
            name = name,
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
            isSeed = isSeed
        )
    }

    private fun RecipeWithIngredientsEntity.toDomain(): RecipeSummary {
        val ingredientModels = ingredients.map { ingredient ->
            RecipeIngredient(
                id = ingredient.id,
                foodItemId = ingredient.foodItemId,
                name = ingredient.customName ?: "Custom ingredient",
                grams = ingredient.grams,
                calories = ingredient.calories,
                protein = ingredient.protein,
                carbs = ingredient.carbs,
                fat = ingredient.fat
            )
        }
        val safeServings = recipe.servings.takeIf { it > 0.0 } ?: 1.0
        return RecipeSummary(
            id = recipe.id,
            name = recipe.name,
            servings = safeServings,
            totalCalories = ingredientModels.sumOf { it.calories },
            totalProtein = ingredientModels.sumOf { it.protein },
            totalCarbs = ingredientModels.sumOf { it.carbs },
            totalFat = ingredientModels.sumOf { it.fat },
            ingredients = ingredientModels
        )
    }

    private fun Double.trimTrailingZero(): String {
        return if (this % 1.0 == 0.0) {
            toInt().toString()
        } else {
            "%.2f".format(this).trimEnd('0').trimEnd('.')
        }
    }
}
