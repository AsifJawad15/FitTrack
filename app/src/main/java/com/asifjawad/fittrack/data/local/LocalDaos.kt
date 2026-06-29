package com.asifjawad.fittrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Upsert
    suspend fun upsert(profile: UserProfileEntity)
}

@Dao
interface WeightLogDao {
    @Query("SELECT * FROM weight_logs ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<WeightLogEntity>>

    @Insert
    suspend fun insert(log: WeightLogEntity)
}

@Dao
interface WaistLogDao {
    @Query("SELECT * FROM waist_logs ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<WaistLogEntity>>

    @Insert
    suspend fun insert(log: WaistLogEntity)
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM foods WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC")
    fun observeBySearch(query: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getById(id: Long): FoodItemEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: FoodItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<FoodItemEntity>): List<Long>
}

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY name COLLATE NOCASE ASC")
    fun observeRecipes(): Flow<List<RecipeEntity>>

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY updatedAtEpochMillis DESC")
    fun observeRecipesWithIngredients(): Flow<List<RecipeWithIngredientsEntity>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipeWithIngredients(id: Long): RecipeWithIngredientsEntity?

    @Insert
    suspend fun insertRecipe(entity: RecipeEntity): Long

    @Insert
    suspend fun insertIngredients(entities: List<RecipeIngredientEntity>)
}

@Dao
interface MealLogDao {
    @Query("SELECT * FROM meal_logs WHERE dateEpochDay = :dateEpochDay ORDER BY createdAtEpochMillis DESC")
    fun observeByDate(dateEpochDay: Long): Flow<List<MealLogEntity>>

    @Insert
    suspend fun insert(entity: MealLogEntity)
}

@Dao
interface DailySummaryDao {
    @Query("SELECT * FROM daily_summaries WHERE dateEpochDay = :dateEpochDay")
    fun observeByDate(dateEpochDay: Long): Flow<HealthDailySummaryEntity?>

    @Upsert
    suspend fun upsert(entity: HealthDailySummaryEntity)
}

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state WHERE key = :key")
    fun observeByKey(key: String): Flow<SyncStateEntity?>

    @Upsert
    suspend fun upsert(entity: SyncStateEntity)
}
