package com.asifjawad.fittrack.data.local

object SeedData {
    fun bangladeshiFoods(nowEpochMillis: Long): List<FoodItemEntity> {
        return listOf(
            FoodItemEntity(
                name = "White rice (boiled)",
                caloriesPer100g = 130.0,
                proteinPer100g = 2.7,
                carbsPer100g = 28.0,
                fatPer100g = 0.3,
                isSeed = true,
                updatedAtEpochMillis = nowEpochMillis
            ),
            FoodItemEntity(
                name = "Masoor dal (cooked)",
                caloriesPer100g = 116.0,
                proteinPer100g = 9.0,
                carbsPer100g = 20.0,
                fatPer100g = 0.4,
                isSeed = true,
                updatedAtEpochMillis = nowEpochMillis
            ),
            FoodItemEntity(
                name = "Chicken curry",
                caloriesPer100g = 180.0,
                proteinPer100g = 16.0,
                carbsPer100g = 5.0,
                fatPer100g = 11.0,
                isSeed = true,
                updatedAtEpochMillis = nowEpochMillis
            ),
            FoodItemEntity(
                name = "Rohu fish curry",
                caloriesPer100g = 170.0,
                proteinPer100g = 17.0,
                carbsPer100g = 4.0,
                fatPer100g = 9.0,
                isSeed = true,
                updatedAtEpochMillis = nowEpochMillis
            ),
            FoodItemEntity(
                name = "Mixed vegetable bhaji",
                caloriesPer100g = 95.0,
                proteinPer100g = 2.8,
                carbsPer100g = 10.0,
                fatPer100g = 4.8,
                isSeed = true,
                updatedAtEpochMillis = nowEpochMillis
            ),
            FoodItemEntity(
                name = "Mustard oil",
                caloriesPer100g = 884.0,
                proteinPer100g = 0.0,
                carbsPer100g = 0.0,
                fatPer100g = 100.0,
                isSeed = true,
                updatedAtEpochMillis = nowEpochMillis
            )
        )
    }
}
