package com.palmastro.app.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.palmastro.app.R
import com.palmastro.app.viewmodel.OnboardingViewModel
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

private data class Location(val name: String, val lat: Double, val lon: Double)
private val locations = listOf(
    Location("New York, USA", 40.713, -74.006), Location("Los Angeles, USA", 34.052, -118.244),
    Location("Chicago, USA", 41.878, -87.630), Location("San Francisco, USA", 37.775, -122.419),
    Location("Seattle, USA", 47.607, -122.332), Location("Houston, USA", 29.760, -95.370),
    Location("Miami, USA", 25.762, -80.192), Location("Boston, USA", 42.361, -71.058),
    Location("Toronto, Canada", 43.653, -79.383), Location("Vancouver, Canada", 49.283, -123.121),
    Location("Taipei, Taiwan", 25.033, 121.565), Location("Tokyo, Japan", 35.682, 139.692),
    Location("Seoul, South Korea", 37.567, 126.978), Location("Beijing, China", 39.904, 116.407),
    Location("Shanghai, China", 31.230, 121.474), Location("Hong Kong", 22.320, 114.169),
    Location("Singapore", 1.352, 103.820), Location("Bangkok, Thailand", 13.756, 100.502),
    Location("Mumbai, India", 19.076, 72.878), Location("Delhi, India", 28.614, 77.209),
    Location("Bangalore, India", 12.972, 77.594), Location("Chennai, India", 13.083, 80.270),
    Location("Kolkata, India", 22.573, 88.364), Location("Hyderabad, India", 17.385, 78.487),
    Location("London, UK", 51.507, -0.128), Location("Paris, France", 48.857, 2.352),
    Location("Sydney, Australia", -33.869, 151.209),
)

private fun getZodiac(month: Int, day: Int): Pair<String, String> = when {
    (month == 3 && day >= 21) || (month == 4 && day <= 19) -> "Aries" to "♈"
    (month == 4 && day >= 20) || (month == 5 && day <= 20) -> "Taurus" to "♉"
    (month == 5 && day >= 21) || (month == 6 && day <= 20) -> "Gemini" to "♊"
    (month == 6 && day >= 21) || (month == 7 && day <= 22) -> "Cancer" to "♋"
    (month == 7 && day >= 23) || (month == 8 && day <= 22) -> "Leo" to "♌"
    (month == 8 && day >= 23) || (month == 9 && day <= 22) -> "Virgo" to "♍"
    (month == 9 && day >= 23) || (month == 10 && day <= 22) -> "Libra" to "♎"
    (month == 10 && day >= 23) || (month == 11 && day <= 21) -> "Scorpio" to "♏"
    (month == 11 && day >= 22) || (month == 12 && day <= 21) -> "Sagittarius" to "♐"
    else -> "Capricorn" to "♑"
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.isComplete) { if (state.isComplete) onComplete() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Spacer(Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PalmAstro", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { (state.step + 1).toFloat() / 6f }, modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outlineVariant)
            }
            Spacer(Modifier.height(12.dp))

            AnimatedContent(targetState = state.step, transitionSpec = { slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut() }, label = "step") { step ->
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    when (step) {
                        0 -> NameGenderStep(state.name, state.gender, { viewModel.setName(it) }, { viewModel.setGender(it) }, { viewModel.nextStep() })
                        1 -> BirthdayStep { viewModel.setBirthday(it); viewModel.nextStep() }
                        2 -> HandStatusStep(state.dominantHand, state.relationshipStatus, { viewModel.setHand(it) }, { viewModel.setRelationshipStatus(it) }, { viewModel.nextStep() })
                        3 -> BirthDetailsStep(onSkip = { viewModel.skipBirthDetails(); viewModel.nextStep() }, onContinue = { h, m, loc -> viewModel.setBirthTime(h, m); if (loc != null) viewModel.setBirthPlace(loc.name, loc.lat, loc.lon); viewModel.nextStep() })
                        4 -> ToneStep(state.tone, { viewModel.setTone(it) }, { viewModel.nextStep() })
                        5 -> ReviewStep(state, { viewModel.completeOnboarding() })
                    }
                }
            }
        }
    }
}

@Composable
private fun Illustration(resId: Int) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().height(180.dp)) {
        Image(painter = painterResource(resId), contentDescription = null, modifier = Modifier.fillMaxSize().padding(12.dp), contentScale = ContentScale.Fit)
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun Title(title: String, subtitle: String) {
    Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun MainButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp), enabled = enabled) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun Label(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(10.dp))
}

// ── Step 0: Name + Gender ──
@Composable
private fun NameGenderStep(name: String, gender: String?, onName: (String) -> Unit, onGender: (String) -> Unit, onNext: () -> Unit) {
    Illustration(R.drawable.img_onboarding_welcome)
    Title("Welcome!", "Let's personalize your experience")
    OutlinedTextField(value = name, onValueChange = onName, label = { Text("Your name") }, placeholder = { Text("Enter your name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
    Spacer(Modifier.height(20.dp))
    Label("I am")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("female" to "Female", "male" to "Male", "other" to "Other").forEach { (key, label) ->
            FilterChip(selected = gender == key, onClick = { onGender(key) }, label = { Text(label) }, modifier = Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(32.dp))
    MainButton("Next", enabled = name.isNotBlank()) { onNext() }
}

// ── Step 1: Birthday (dropdown selectors — reliable) ──
@Composable
private fun BirthdayStep(onConfirm: (LocalDate) -> Unit) {
    var selMonth by remember { mutableStateOf(6) }
    var selDay by remember { mutableStateOf(15) }
    var selYear by remember { mutableStateOf(1990) }
    var monthExpanded by remember { mutableStateOf(false) }
    var dayExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val currentYear = LocalDate.now().year
    val (zodiacName, zodiacEmoji) = getZodiac(selMonth, selDay)

    Illustration(R.drawable.img_onboarding_birthday)
    Title("Birthday", "Used for astrological calculations")

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.weight(1.3f)) {
            OutlinedButton(onClick = { monthExpanded = true }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) { Text(Month.of(selMonth).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)) }
            DropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }) {
                (1..12).forEach { m -> DropdownMenuItem(text = { Text(Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH)) }, onClick = { selMonth = m; monthExpanded = false }) }
            }
        }
        Box(modifier = Modifier.weight(0.8f)) {
            OutlinedButton(onClick = { dayExpanded = true }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) { Text("$selDay") }
            DropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                (1..31).forEach { d -> DropdownMenuItem(text = { Text("$d") }, onClick = { selDay = d; dayExpanded = false }) }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = { yearExpanded = true }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) { Text("$selYear") }
            DropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                (currentYear downTo 1930).forEach { y -> DropdownMenuItem(text = { Text("$y") }, onClick = { selYear = y; yearExpanded = false }) }
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), shape = RoundedCornerShape(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(zodiacEmoji, fontSize = 28.sp); Spacer(Modifier.width(10.dp))
            Column { Text(zodiacName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text("Your zodiac sign", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    if (error != null) { Spacer(Modifier.height(8.dp)); Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }
    Spacer(Modifier.height(24.dp))
    MainButton("Next") {
        try { val d = LocalDate.of(selYear, selMonth, selDay); if (d.isAfter(LocalDate.now())) error = "Birthday can't be in the future" else { error = null; onConfirm(d) } } catch (_: Exception) { error = "Invalid date combination" }
    }
}

// ── Step 2: Hand + Status ──
@Composable
private fun HandStatusStep(hand: String, status: String?, onHand: (String) -> Unit, onStatus: (String) -> Unit, onNext: () -> Unit) {
    Illustration(R.drawable.img_onboarding_hands)
    Title("About You", "A few more details")
    Label("Dominant hand")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("left" to "🤚 Left", "right" to "✋ Right").forEach { (key, label) ->
            FilterChip(selected = hand == key, onClick = { onHand(key) }, label = { Text(label, fontSize = 15.sp) }, modifier = Modifier.weight(1f).height(48.dp))
        }
    }
    Spacer(Modifier.height(20.dp))
    Label("Relationship status")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("single" to "Single", "relationship" to "In a relationship", "married" to "Married").forEach { (key, label) ->
            FilterChip(selected = status == key, onClick = { onStatus(key) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
        }
    }
    Spacer(Modifier.height(32.dp))
    MainButton("Next") { onNext() }
}

// ── Step 3: Birth Details (dropdown selectors) ──
@Composable
private fun BirthDetailsStep(onSkip: () -> Unit, onContinue: (Int, Int, Location?) -> Unit) {
    var selHour by remember { mutableStateOf(12) }
    var selMinute by remember { mutableStateOf(0) }
    var hourExpanded by remember { mutableStateOf(false) }
    var minuteExpanded by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<Location?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var locExpanded by remember { mutableStateOf(false) }
    val filtered = remember(searchQuery) { if (searchQuery.isBlank()) locations else locations.filter { it.name.contains(searchQuery, ignoreCase = true) } }

    Illustration(R.drawable.img_onboarding_birth_details)
    Title("Birth Details", "Optional — unlocks L2 precision")

    Label("Time of birth")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = { hourExpanded = true }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) { Text("${selHour.toString().padStart(2, '0')}h") }
            DropdownMenu(expanded = hourExpanded, onDismissRequest = { hourExpanded = false }) {
                (0..23).forEach { h -> DropdownMenuItem(text = { Text("${h.toString().padStart(2, '0')}:00") }, onClick = { selHour = h; hourExpanded = false }) }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = { minuteExpanded = true }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) { Text("${selMinute.toString().padStart(2, '0')}m") }
            DropdownMenu(expanded = minuteExpanded, onDismissRequest = { minuteExpanded = false }) {
                (0..59).forEach { m -> DropdownMenuItem(text = { Text(":${m.toString().padStart(2, '0')}") }, onClick = { selMinute = m; minuteExpanded = false }) }
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    Label("Place of birth")
    Box {
        OutlinedTextField(value = selectedLocation?.name ?: searchQuery, onValueChange = { searchQuery = it; selectedLocation = null; locExpanded = true }, label = { Text("Search city...") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
        DropdownMenu(expanded = locExpanded && filtered.isNotEmpty(), onDismissRequest = { locExpanded = false }) {
            DropdownMenuItem(text = { Text("Other / Unknown") }, onClick = { selectedLocation = null; searchQuery = ""; locExpanded = false })
            filtered.take(8).forEach { loc -> DropdownMenuItem(text = { Text(loc.name) }, onClick = { selectedLocation = loc; searchQuery = ""; locExpanded = false }) }
        }
    }

    Spacer(Modifier.height(28.dp))
    MainButton("Confirm") { onContinue(selHour, selMinute, selectedLocation) }
    TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Skip this step") }
    Spacer(Modifier.height(16.dp))
}

// ── Step 4: Tone ──
@Composable
private fun ToneStep(selected: String, onSelect: (String) -> Unit, onNext: () -> Unit) {
    Illustration(R.drawable.img_onboarding_tone)
    Title("Your Style", "How should we talk to you?")
    data class T(val key: String, val emoji: String, val label: String, val desc: String)
    val tones = listOf(T("scientific", "🔬", "Scientific", "Objective, data-driven"), T("healing", "🌿", "Healing", "Warm, encouraging"), T("roast_safe", "🔥", "Straight Talk", "Direct but safe"))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tones.forEach { tone ->
            Card(onClick = { onSelect(tone.key) }, colors = CardDefaults.cardColors(containerColor = if (selected == tone.key) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tone.emoji, fontSize = 28.sp); Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) { Text(tone.label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Text(tone.desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (selected == tone.key) Text("✓", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    Spacer(Modifier.height(28.dp))
    MainButton("Next") { onNext() }
}

// ── Step 5: Review ──
@Composable
private fun ReviewStep(state: com.palmastro.app.viewmodel.OnboardingState, onComplete: () -> Unit) {
    val (zodiacName, zodiacEmoji) = getZodiac(state.birthdayMonth, state.birthdayDay)
    Illustration(R.drawable.img_onboarding_ready)
    Title("All Set!", "Review your details")
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (state.name.isNotBlank()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Name", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(state.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            state.gender?.let { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Gender", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(it.replaceFirstChar { c -> c.uppercase() }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) } }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Hand", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(state.dominantHand.replaceFirstChar { it.uppercase() }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Birthday", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${Month.of(state.birthdayMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${state.birthdayDay}, ${state.birthdayYear}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Zodiac", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("$zodiacEmoji $zodiacName", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            state.relationshipStatus?.let { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Status", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(it.replaceFirstChar { c -> c.uppercase() }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) } }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Style", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(state.tone.replaceFirstChar { it.uppercase() }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Analysis", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(if (state.hasBirthTime) "L2 (detailed)" else "L1 (standard)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
    Spacer(Modifier.height(28.dp))
    MainButton("Get Started") { onComplete() }
}
