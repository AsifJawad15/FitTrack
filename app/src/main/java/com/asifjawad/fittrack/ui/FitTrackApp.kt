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
import com.asifjawad.fittrack.domain.model.ActivityLevel
import com.asifjawad.fittrack.domain.model.Sex
import com.asifjawad.fittrack.domain.model.UserProfile
import com.asifjawad.fittrack.domain.model.UserProfileDraft
import com.asifjawad.fittrack.ui.theme.FittrackTheme
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
                    FitTrackTab.Dashboard -> DashboardScreen(profile)
                    FitTrackTab.Food -> FoodScreen()
                    FitTrackTab.Progress -> ProgressScreen(profile)
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
private fun DashboardScreen(profile: UserProfile) {
    val daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), profile.goalDate).coerceAtLeast(0)
    val plannedLoss = profile.startWeightKg - profile.targetWeightKg

    ScreenColumn {
        ScreenHeader(
            title = "Dashboard",
            subtitle = "A control panel for the 80 kg target."
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
                    text = "${plannedLoss.formatOne()} kg to lose",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$daysRemaining days remaining. Phase 3 will replace this with trend math and trajectory score.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        MetricGrid(
            items = listOf(
                "Start" to "${profile.startWeightKg.formatOne()} kg",
                "Target" to "${profile.targetWeightKg.formatOne()} kg",
                "Height" to "${profile.heightCm.formatOne()} cm",
                "Activity" to profile.activityLevel.label
            )
        )

        ActionCard(
            title = "Next build step",
            body = "Phase 2 will make these profile and log values survive app restart with Room."
        )
    }
}

@Composable
private fun FoodScreen() {
    ScreenColumn {
        ScreenHeader(
            title = "Food",
            subtitle = "Manual food logging starts in Phase 2, then faster meal workflows arrive in Phase 4."
        )
        EmptyStateCard(
            title = "No meals logged yet",
            body = "The first local database pass will add foods, meals, recipes, and daily totals.",
            icon = Icons.Filled.Add
        )
    }
}

@Composable
private fun ProgressScreen(profile: UserProfile) {
    ScreenColumn {
        ScreenHeader(
            title = "Progress",
            subtitle = "Goal timeline and trend weight will land after the calculation engine."
        )
        MetricGrid(
            items = listOf(
                "Goal date" to profile.goalDate.toString(),
                "Loss goal" to "${(profile.startWeightKg - profile.targetWeightKg).formatOne()} kg",
                "Sex" to profile.sex.label,
                "BMR basis" to "Mifflin-St Jeor"
            )
        )
        ActionCard(
            title = "Accuracy rule",
            body = "The app will show formulas and assumptions instead of hiding health calculations."
        )
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
            subtitle = "Edit the current profile inputs. Persistence arrives in Phase 2."
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
            )
        )
    }
}
