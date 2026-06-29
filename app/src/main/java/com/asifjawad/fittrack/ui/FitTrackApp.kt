package com.asifjawad.fittrack.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asifjawad.fittrack.domain.calculation.ProgressEngine
import com.asifjawad.fittrack.domain.model.ActivityLevel
import com.asifjawad.fittrack.domain.model.MealType
import com.asifjawad.fittrack.domain.model.Sex
import com.asifjawad.fittrack.domain.model.UserProfile
import com.asifjawad.fittrack.domain.model.UserProfileDraft
import com.asifjawad.fittrack.ui.theme.FittrackTheme
import java.time.LocalDate

@Composable
fun FitTrackApp(
    viewModel: FitTrackViewModel = viewModel()
) {
    val state = viewModel.uiState
    val profile = state.profile

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (profile != null) {
                FitTrackBottomBar(
                    selectedTab = state.selectedTab,
                    onTabSelected = viewModel::selectTab
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (profile == null) {
                ProfileSetupScreen(
                    draft = state.profileDraft,
                    error = state.profileError,
                    message = state.profileSavedMessage,
                    onDraftChange = viewModel::updateDraft,
                    onSave = viewModel::saveProfile
                )
            } else {
                when (state.selectedTab) {
                    FitTrackTab.Dashboard -> DashboardScreen(profile = profile, state = state)
                    FitTrackTab.Food -> FoodScreen(
                        state = state,
                        onFoodNameChange = viewModel::updateFoodName,
                        onFoodCaloriesChange = viewModel::updateFoodCalories,
                        onAddFood = viewModel::addCustomFood,
                        onMealGramsChange = viewModel::updateMealGrams,
                        onMealTypeChange = viewModel::setMealType,
                        onLogMeal = viewModel::logMeal
                    )
                    FitTrackTab.Progress -> ProgressScreen(
                        profile = profile,
                        state = state,
                        onWeightChange = viewModel::updateWeightInput,
                        onWaistChange = viewModel::updateWaistInput,
                        onSaveWeight = viewModel::addWeightLog,
                        onSaveWaist = viewModel::addWaistLog
                    )
                    FitTrackTab.Health -> HealthScreen()
                    FitTrackTab.Settings -> SettingsScreen(
                        draft = state.profileDraft,
                        error = state.profileError,
                        message = state.profileSavedMessage,
                        onDraftChange = viewModel::updateDraft,
                        onSave = viewModel::saveProfile
                    )
                }
            }
        }
    }
}

@Composable
private fun FitTrackBottomBar(
    selectedTab: FitTrackTab,
    onTabSelected: (FitTrackTab) -> Unit
) {
    NavigationBar {
        FitTrackTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label) }
            )
        }
    }
}

private fun FitTrackTab.icon(): ImageVector = when (this) {
    FitTrackTab.Dashboard -> Icons.Filled.Home
    FitTrackTab.Food -> Icons.AutoMirrored.Filled.List
    FitTrackTab.Progress -> Icons.Filled.Favorite
    FitTrackTab.Health -> Icons.Filled.Person
    FitTrackTab.Settings -> Icons.Filled.Settings
}

@Composable
private fun ProfileSetupScreen(
    draft: UserProfileDraft,
    error: String?,
    message: String?,
    onDraftChange: (UserProfileDraft.() -> UserProfileDraft) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = "FitTrack Personal",
            subtitle = "Set the numbers the app will use before it starts calculating progress."
        )

        ProfileFields(
            draft = draft,
            onDraftChange = onDraftChange
        )

        StatusMessage(error = error, message = message)

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Save profile")
        }
    }
}

@Composable
private fun ProfileFields(
    draft: UserProfileDraft,
    onDraftChange: (UserProfileDraft.() -> UserProfileDraft) -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = draft.age,
                onValueChange = { value -> onDraftChange { copy(age = value) } },
                label = { Text("Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Sex", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sex.entries.forEach { sex ->
                    FilterChip(
                        selected = draft.sex == sex,
                        onClick = { onDraftChange { copy(sex = sex) } },
                        label = { Text(sex.label) }
                    )
                }
            }

            OutlinedTextField(
                value = draft.heightCm,
                onValueChange = { value -> onDraftChange { copy(heightCm = value) } },
                label = { Text("Height (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.startWeightKg,
                    onValueChange = { value -> onDraftChange { copy(startWeightKg = value) } },
                    label = { Text("Start kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = draft.targetWeightKg,
                    onValueChange = { value -> onDraftChange { copy(targetWeightKg = value) } },
                    label = { Text("Target kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = draft.goalDate,
                onValueChange = { value -> onDraftChange { copy(goalDate = value) } },
                label = { Text("Goal date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Activity level", style = MaterialTheme.typography.labelLarge)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityLevel.entries.forEach { level ->
                    FilterChip(
                        selected = draft.activityLevel == level,
                        onClick = { onDraftChange { copy(activityLevel = level) } },
                        label = {
                            Column {
                                Text(level.label)
                                Text(
                                    text = level.description,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen(profile: UserProfile, state: FitTrackUiState) {
    val metrics = ProgressEngine.calculate(
        profile = profile,
        weightLogs = state.weightLogs,
        mealsToday = state.mealsToday
    )
    val scoreLabel = metrics.trajectoryScore?.let { "${it.formatOne()}% trajectory" } ?: "Trajectory pending"
    val trendLabel = metrics.trendWeightKg?.let { "${it.formatOne()} kg" } ?: "Log weight"

    ScreenColumn {
        ScreenHeader(
            title = "Dashboard",
            subtitle = "A calculation-first control panel for the goal you set."
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = scoreLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${metrics.daysRemaining} days remaining. ${metrics.recommendation}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        MetricGrid(
            items = listOf(
                "Trend weight" to trendLabel,
                "Planned today" to "${metrics.plannedWeightKg.formatOne()} kg",
                "Calories today" to "${metrics.caloriesIn.formatOne()} kcal",
                "Food target" to "${metrics.dailyPlan.foodCalorieTarget.formatOne()} kcal",
                "Exercise target" to "${metrics.dailyPlan.plannedExerciseCalories.formatOne()} kcal",
                "BMR" to "${metrics.bmr.formatOne()} kcal",
                "TDEE" to "${metrics.tdee.formatOne()} kcal",
                "Energy gap" to "${metrics.estimatedEnergyGap.formatOne()} kcal",
                "Goal adjustment" to "${metrics.requiredDailyEnergyAdjustment.formatOne()} kcal"
            )
        )

        ActionCard(
            title = metrics.dailyPlan.label,
            body = metrics.dailyPlan.note + "\n\n" + metrics.formulaLines.take(5).joinToString(separator = "\n\n") { line ->
                "${line.label}: ${line.result}"
            }
        )
    }
}

@Composable
private fun FoodScreen(
    state: FitTrackUiState,
    onFoodNameChange: (String) -> Unit,
    onFoodCaloriesChange: (String) -> Unit,
    onAddFood: () -> Unit,
    onMealGramsChange: (String) -> Unit,
    onMealTypeChange: (MealType) -> Unit,
    onLogMeal: (Long) -> Unit
) {
    ScreenColumn {
        ScreenHeader(
            title = "Food",
            subtitle = "Add local foods and log meals quickly without internet or Health Connect."
        )

        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Add food", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = state.foodNameInput,
                    onValueChange = onFoodNameChange,
                    label = { Text("Food name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.foodCaloriesInput,
                    onValueChange = onFoodCaloriesChange,
                    label = { Text("Calories per 100g") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                StatusMessage(error = state.foodError, message = state.foodMessage)
                Button(onClick = onAddFood, modifier = Modifier.fillMaxWidth()) {
                    Text("Save food")
                }
            }
        }

        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Log meal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = state.mealGramsInput,
                    onValueChange = onMealGramsChange,
                    label = { Text("Grams") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealType.entries.forEach { mealType ->
                        FilterChip(
                            selected = state.selectedMealType == mealType,
                            onClick = { onMealTypeChange(mealType) },
                            label = { Text(mealType.label) }
                        )
                    }
                }
                StatusMessage(error = state.mealError, message = state.mealMessage)
            }
        }

        if (state.foods.isEmpty()) {
            EmptyStateCard(
                title = "No foods yet",
                body = "Add a food above to start meal logging.",
                icon = Icons.Filled.Add
            )
        } else {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Food list", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    state.foods.take(12).forEach { food ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(food.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${food.caloriesPer100g.formatOne()} kcal/100g",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { onLogMeal(food.id) }) {
                                Text("Log")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }

        if (state.mealsToday.isNotEmpty()) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    state.mealsToday.take(10).forEach { meal ->
                        Text(
                            text = "${meal.mealType.label}: ${meal.displayName} (${meal.grams.formatOne()} g, ${meal.calories.formatOne()} kcal)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressScreen(
    profile: UserProfile,
    state: FitTrackUiState,
    onWeightChange: (String) -> Unit,
    onWaistChange: (String) -> Unit,
    onSaveWeight: () -> Unit,
    onSaveWaist: () -> Unit
) {
    val metrics = ProgressEngine.calculate(
        profile = profile,
        weightLogs = state.weightLogs,
        mealsToday = state.mealsToday
    )

    ScreenColumn {
        ScreenHeader(
            title = "Progress",
            subtitle = "Track manual measurements and audit the formulas behind the dashboard."
        )

        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = state.weightInput,
                    onValueChange = onWeightChange,
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = onSaveWeight, modifier = Modifier.fillMaxWidth()) {
                    Text("Save weight")
                }
                OutlinedTextField(
                    value = state.waistInput,
                    onValueChange = onWaistChange,
                    label = { Text("Waist (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = onSaveWaist, modifier = Modifier.fillMaxWidth()) {
                    Text("Save waist")
                }
                StatusMessage(error = state.measurementError, message = state.measurementMessage)
            }
        }

        MetricGrid(
            items = listOf(
                "Goal date" to profile.goalDate.toString(),
                "Weight change" to "${(profile.targetWeightKg - profile.startWeightKg).formatOne()} kg",
                "Trajectory" to (metrics.trajectoryScore?.let { "${it.formatOne()}%" } ?: "Pending"),
                "Entries" to "${state.weightLogs.size} weight / ${state.waistLogs.size} waist"
            )
        )

        ActionCard(
            title = "Calculation Details",
            body = metrics.formulaLines.joinToString(separator = "\n\n") { line ->
                "${line.label}\n${line.formula}\n= ${line.result}"
            }
        )

        if (state.weightLogs.isNotEmpty()) {
            ActionCard(
                title = "Recent weight logs",
                body = state.weightLogs.take(5).joinToString(separator = "\n") {
                    "${it.date}: ${it.weightKg.formatOne()} kg"
                }
            )
        }

        if (state.waistLogs.isNotEmpty()) {
            ActionCard(
                title = "Recent waist logs",
                body = state.waistLogs.take(5).joinToString(separator = "\n") {
                    "${it.date}: ${it.waistCm.formatOne()} cm"
                }
            )
        }
    }
}

@Composable
private fun HealthScreen() {
    ScreenColumn {
        ScreenHeader(
            title = "Health",
            subtitle = "Samsung Health data will arrive through Health Connect in Phase 5."
        )
        ActionCard(
            title = "Planned data flow",
            body = "Galaxy Fit3 -> Samsung Health -> Health Connect -> FitTrack local summary."
        )
        EmptyStateCard(
            title = "No Health Connect sync yet",
            body = "This phase keeps the app offline and safe while the shell settles.",
            icon = Icons.Filled.Favorite
        )
    }
}

@Composable
private fun SettingsScreen(
    draft: UserProfileDraft,
    error: String?,
    message: String?,
    onDraftChange: (UserProfileDraft.() -> UserProfileDraft) -> Unit,
    onSave: () -> Unit
) {
    ScreenColumn {
        ScreenHeader(
            title = "Settings",
            subtitle = "Edit and persist profile inputs in the local Room database."
        )
        ProfileFields(draft = draft, onDraftChange = onDraftChange)
        StatusMessage(error = error, message = message)
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Update profile")
        }
    }
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
private fun ScreenHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetricGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { (label, value) ->
                    MetricCard(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(94.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ActionCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, body: String, icon: ImageVector) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AssistChip(
                onClick = {},
                label = { Text(title) },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatusMessage(error: String?, message: String?) {
    when {
        error != null -> Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        message != null -> Text(
            text = message,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun Double.formatOne(): String = "%,.1f".format(this)

@Preview(showBackground = true)
@Composable
private fun FitTrackAppPreview() {
    FittrackTheme {
        DashboardScreen(
            profile = UserProfile(
                age = 28,
                sex = Sex.Male,
                heightCm = 180.0,
                startWeightKg = 90.0,
                targetWeightKg = 80.0,
                goalDate = LocalDate.now().plusMonths(3),
                activityLevel = ActivityLevel.Light
            ),
            state = FitTrackUiState()
        )
    }
}
