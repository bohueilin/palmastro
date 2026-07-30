package com.palmastro.app.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.app.R
import com.palmastro.app.viewmodel.OnboardingState
import com.palmastro.app.viewmodel.OnboardingSteps
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

/** Localized zodiac: string resource id + symbol. */
private fun zodiacFor(month: Int, day: Int): Pair<Int, String> = when {
    (month == 3 && day >= 21) || (month == 4 && day <= 19) -> R.string.ob_zodiac_aries to "♈"
    (month == 4 && day >= 20) || (month == 5 && day <= 20) -> R.string.ob_zodiac_taurus to "♉"
    (month == 5 && day >= 21) || (month == 6 && day <= 20) -> R.string.ob_zodiac_gemini to "♊"
    (month == 6 && day >= 21) || (month == 7 && day <= 22) -> R.string.ob_zodiac_cancer to "♋"
    (month == 7 && day >= 23) || (month == 8 && day <= 22) -> R.string.ob_zodiac_leo to "♌"
    (month == 8 && day >= 23) || (month == 9 && day <= 22) -> R.string.ob_zodiac_virgo to "♍"
    (month == 9 && day >= 23) || (month == 10 && day <= 22) -> R.string.ob_zodiac_libra to "♎"
    (month == 10 && day >= 23) || (month == 11 && day <= 21) -> R.string.ob_zodiac_scorpio to "♏"
    (month == 11 && day >= 22) || (month == 12 && day <= 21) -> R.string.ob_zodiac_sagittarius to "♐"
    (month == 12 && day >= 22) || (month == 1 && day <= 19) -> R.string.ob_zodiac_capricorn to "♑"
    (month == 1 && day >= 20) || (month == 2 && day <= 18) -> R.string.ob_zodiac_aquarius to "♒"
    (month == 2 && day >= 19) || (month == 3 && day <= 20) -> R.string.ob_zodiac_pisces to "♓"
    else -> R.string.ob_zodiac_capricorn to "♑"
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.isComplete) { if (state.isComplete) onComplete() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.step > 0) {
                    IconButton(onClick = { viewModel.prevStep() }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ob_back))
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.app_name),
                        fontSize = 20.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    val progressDesc = stringResource(R.string.ob_progress, state.step + 1, OnboardingSteps.TOTAL)
                    LinearProgressIndicator(
                        progress = { (state.step + 1).toFloat() / OnboardingSteps.TOTAL },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                            .semantics { contentDescription = progressDesc },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                Spacer(Modifier.size(48.dp))
            }
            Spacer(Modifier.height(12.dp))

            AnimatedContent(
                targetState = state.step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "step",
            ) { step ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (step) {
                        OnboardingSteps.WELCOME -> WelcomeStep { viewModel.nextStep() }
                        OnboardingSteps.PRIVACY -> PrivacyStep { viewModel.nextStep() }
                        OnboardingSteps.NAME -> NameStep(state.name, state.gender, viewModel::setName, viewModel::setGender) { viewModel.nextStep() }
                        OnboardingSteps.BIRTHDAY -> BirthdayStep(state.birthday) { viewModel.setBirthday(it); viewModel.nextStep() }
                        OnboardingSteps.HAND -> HandStatusStep(state.dominantHand, state.relationshipStatus, viewModel::setHand, viewModel::setRelationshipStatus, canProceed = viewModel.canProceedFrom(OnboardingSteps.HAND)) { viewModel.nextStep() }
                        OnboardingSteps.BIRTH_DETAILS -> BirthDetailsStep(
                            onSkip = { viewModel.skipBirthDetails(); viewModel.nextStep() },
                            onContinue = { h, m, loc -> viewModel.setBirthTime(h, m); if (loc != null) viewModel.setBirthPlace(loc.name, loc.lat, loc.lon); viewModel.nextStep() },
                        )
                        OnboardingSteps.TONE -> ToneStep(state.tone, viewModel::setTone) { viewModel.nextStep() }
                        OnboardingSteps.LANGUAGE -> LanguageStep(state.language, onSelect = { code ->
                            viewModel.setLanguage(code)
                            AppLanguage.apply(code)
                        }) { viewModel.nextStep() }
                        OnboardingSteps.SUMMARY -> SummaryStep(state) { viewModel.nextStep() }
                        OnboardingSteps.CAMERA -> CameraEducationStep(enabled = viewModel.canComplete()) { viewModel.completeOnboarding() }
                    }
                }
            }
        }
    }
}

@Composable
private fun Illustration(resId: Int) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().height(160.dp)) {
        Image(painter = painterResource(resId), contentDescription = null, modifier = Modifier.fillMaxSize().padding(12.dp), contentScale = ContentScale.Fit)
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun Title(title: String, subtitle: String) {
    Text(
        title,
        fontSize = 24.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(6.dp))
    Text(subtitle, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun MainButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(14.dp), enabled = enabled) {
        Text(text, fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().semantics { heading() },
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun BulletPoint(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Filled.CheckCircle, contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
    }
}

/**
 * Radio-style option row with full TalkBack semantics:
 * Role.RadioButton, selected state, stateDescription, min 48dp target.
 */
@Composable
private fun OptionRow(
    selected: Boolean,
    label: String,
    description: String? = null,
    onSelect: () -> Unit,
) {
    val selectedDesc = stringResource(R.string.common_selected)
    val unselectedDesc = stringResource(R.string.common_unselected)
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .semantics { stateDescription = if (selected) selectedDesc else unselectedDesc },
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold)
                if (description != null) {
                    Text(description, fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Step: Welcome / value proposition ──
@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Illustration(R.drawable.img_onboarding_welcome)
    Title(stringResource(R.string.ob_welcome_title), stringResource(R.string.ob_welcome_subtitle))
    Text(stringResource(R.string.ob_welcome_body), fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(32.dp))
    MainButton(stringResource(R.string.ob_next)) { onNext() }
}

// ── Step: Privacy promise (PRD 12.1) ──
@Composable
private fun PrivacyStep(onNext: () -> Unit) {
    Icon(
        Icons.Filled.Lock,
        contentDescription = stringResource(R.string.ob_privacy_icon),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(56.dp),
    )
    Spacer(Modifier.height(16.dp))
    Title(stringResource(R.string.ob_privacy_title), stringResource(R.string.ob_privacy_subtitle))
    Column(modifier = Modifier.fillMaxWidth()) {
        BulletPoint(stringResource(R.string.ob_privacy_point_on_device))
        BulletPoint(stringResource(R.string.ob_privacy_point_no_biometric))
        BulletPoint(stringResource(R.string.ob_privacy_point_reflection))
        BulletPoint(stringResource(R.string.ob_privacy_point_delete))
    }
    Spacer(Modifier.height(28.dp))
    MainButton(stringResource(R.string.ob_next)) { onNext() }
}

// ── Step: Name (clearly optional) + gender (optional) ──
@Composable
private fun NameStep(name: String, gender: String?, onName: (String) -> Unit, onGender: (String) -> Unit, onNext: () -> Unit) {
    Illustration(R.drawable.img_onboarding_welcome)
    Title(stringResource(R.string.ob_name_title), stringResource(R.string.ob_name_subtitle))
    OutlinedTextField(
        value = name, onValueChange = onName,
        label = { Text(stringResource(R.string.ob_name_label)) },
        placeholder = { Text(stringResource(R.string.ob_name_placeholder)) },
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
    )
    Spacer(Modifier.height(20.dp))
    Label(stringResource(R.string.ob_gender_label))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
        listOf(
            "female" to stringResource(R.string.ob_gender_female),
            "male" to stringResource(R.string.ob_gender_male),
            "other" to stringResource(R.string.ob_gender_other),
        ).forEach { (key, label) ->
            OptionRow(selected = gender == key, label = label) { onGender(key) }
        }
    }
    Spacer(Modifier.height(28.dp))
    // Name is optional: never gate on it.
    MainButton(stringResource(R.string.ob_next)) { onNext() }
}

// ── Step: Birthday (required, localized pickers) ──
@Composable
private fun BirthdayStep(existing: LocalDate?, onConfirm: (LocalDate) -> Unit) {
    var selMonth by remember { mutableStateOf(existing?.monthValue ?: 6) }
    var selDay by remember { mutableStateOf(existing?.dayOfMonth ?: 15) }
    var selYear by remember { mutableStateOf(existing?.year ?: 1990) }
    var monthExpanded by remember { mutableStateOf(false) }
    var dayExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    var errorRes by remember { mutableStateOf<Int?>(null) }
    val currentYear = LocalDate.now().year
    val locale = Locale.getDefault()
    val (zodiacRes, zodiacEmoji) = zodiacFor(selMonth, selDay)

    Illustration(R.drawable.img_onboarding_birthday)
    Title(stringResource(R.string.ob_birthday_title), stringResource(R.string.ob_birthday_subtitle))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.weight(1.3f)) {
            OutlinedButton(
                onClick = { monthExpanded = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(12.dp),
            ) { Text("${stringResource(R.string.ob_birthday_month)}: ${Month.of(selMonth).getDisplayName(TextStyle.SHORT, locale)}") }
            DropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }) {
                (1..12).forEach { m ->
                    DropdownMenuItem(
                        text = { Text(Month.of(m).getDisplayName(TextStyle.FULL, locale)) },
                        onClick = { selMonth = m; monthExpanded = false },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
        Box(modifier = Modifier.weight(0.9f)) {
            OutlinedButton(onClick = { dayExpanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("${stringResource(R.string.ob_birthday_day)}: $selDay")
            }
            DropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                (1..31).forEach { d -> DropdownMenuItem(text = { Text("$d") }, onClick = { selDay = d; dayExpanded = false }, modifier = Modifier.heightIn(min = 48.dp)) }
            }
        }
        Box(modifier = Modifier.weight(1.1f)) {
            OutlinedButton(onClick = { yearExpanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("${stringResource(R.string.ob_birthday_year)}: $selYear")
            }
            DropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                (currentYear downTo 1930).forEach { y -> DropdownMenuItem(text = { Text("$y") }, onClick = { selYear = y; yearExpanded = false }, modifier = Modifier.heightIn(min = 48.dp)) }
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), shape = RoundedCornerShape(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(zodiacEmoji, fontSize = 28.sp, lineHeight = 40.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    stringResource(zodiacRes),
                    fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.ob_zodiac_caption),
                    fontSize = 12.sp, lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    errorRes?.let {
        Spacer(Modifier.height(8.dp))
        Text(stringResource(it), color = MaterialTheme.colorScheme.error, fontSize = 14.sp, lineHeight = 20.sp)
    }
    Spacer(Modifier.height(24.dp))
    MainButton(stringResource(R.string.ob_next)) {
        try {
            val d = LocalDate.of(selYear, selMonth, selDay)
            if (d.isAfter(LocalDate.now())) errorRes = R.string.ob_birthday_future
            else { errorRes = null; onConfirm(d) }
        } catch (_: Exception) {
            errorRes = R.string.ob_birthday_invalid
        }
    }
}

// ── Step: Dominant hand (explicit, required) + relationship (optional) ──
@Composable
private fun HandStatusStep(
    hand: String?,
    status: String?,
    onHand: (String) -> Unit,
    onStatus: (String) -> Unit,
    canProceed: Boolean,
    onNext: () -> Unit,
) {
    Illustration(R.drawable.img_onboarding_hands)
    Title(stringResource(R.string.ob_hand_title), stringResource(R.string.ob_hand_subtitle))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
        OptionRow(selected = hand == "left", label = stringResource(R.string.ob_hand_left)) { onHand("left") }
        OptionRow(selected = hand == "right", label = stringResource(R.string.ob_hand_right)) { onHand("right") }
    }
    Spacer(Modifier.height(20.dp))
    Label(stringResource(R.string.ob_status_label))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
        listOf(
            "single" to stringResource(R.string.ob_status_single),
            "relationship" to stringResource(R.string.ob_status_relationship),
            "married" to stringResource(R.string.ob_status_married),
        ).forEach { (key, label) ->
            OptionRow(selected = status == key, label = label) { onStatus(key) }
        }
    }
    Spacer(Modifier.height(28.dp))
    MainButton(stringResource(R.string.ob_next), enabled = canProceed) { onNext() }
}

// ── Step: Birth details (optional) ──
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
    Title(stringResource(R.string.ob_birth_details_title), stringResource(R.string.ob_birth_details_subtitle))

    Label(stringResource(R.string.ob_birth_time_label))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = { hourExpanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("${stringResource(R.string.ob_birth_hour)}: ${selHour.toString().padStart(2, '0')}")
            }
            DropdownMenu(expanded = hourExpanded, onDismissRequest = { hourExpanded = false }) {
                (0..23).forEach { h -> DropdownMenuItem(text = { Text("${h.toString().padStart(2, '0')}:00") }, onClick = { selHour = h; hourExpanded = false }, modifier = Modifier.heightIn(min = 48.dp)) }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = { minuteExpanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("${stringResource(R.string.ob_birth_minute)}: ${selMinute.toString().padStart(2, '0')}")
            }
            DropdownMenu(expanded = minuteExpanded, onDismissRequest = { minuteExpanded = false }) {
                (0..59).forEach { m -> DropdownMenuItem(text = { Text(":${m.toString().padStart(2, '0')}") }, onClick = { selMinute = m; minuteExpanded = false }, modifier = Modifier.heightIn(min = 48.dp)) }
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    Label(stringResource(R.string.ob_birth_place_label))
    Box {
        OutlinedTextField(
            value = selectedLocation?.name ?: searchQuery,
            onValueChange = { searchQuery = it; selectedLocation = null; locExpanded = true },
            label = { Text(stringResource(R.string.ob_birth_place_search)) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
        )
        DropdownMenu(expanded = locExpanded && filtered.isNotEmpty(), onDismissRequest = { locExpanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.ob_birth_place_unknown)) }, onClick = { selectedLocation = null; searchQuery = ""; locExpanded = false }, modifier = Modifier.heightIn(min = 48.dp))
            filtered.take(8).forEach { loc -> DropdownMenuItem(text = { Text(loc.name) }, onClick = { selectedLocation = loc; searchQuery = ""; locExpanded = false }, modifier = Modifier.heightIn(min = 48.dp)) }
        }
    }

    Spacer(Modifier.height(28.dp))
    MainButton(stringResource(R.string.ob_birth_details_confirm)) { onContinue(selHour, selMinute, selectedLocation) }
    TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(stringResource(R.string.ob_birth_details_skip))
    }
    Spacer(Modifier.height(16.dp))
}

// ── Step: Tone (PRD 45 display names) ──
@Composable
private fun ToneStep(selected: String, onSelect: (String) -> Unit, onNext: () -> Unit) {
    Illustration(R.drawable.img_onboarding_tone)
    Title(stringResource(R.string.ob_tone_title), stringResource(R.string.ob_tone_subtitle))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.selectableGroup()) {
        OptionRow(selected = selected == "scientific", label = stringResource(R.string.ob_tone_analytical), description = stringResource(R.string.ob_tone_analytical_desc)) { onSelect("scientific") }
        OptionRow(selected = selected == "healing", label = stringResource(R.string.ob_tone_gentle), description = stringResource(R.string.ob_tone_gentle_desc)) { onSelect("healing") }
        OptionRow(selected = selected == "roast_safe", label = stringResource(R.string.ob_tone_direct), description = stringResource(R.string.ob_tone_direct_desc)) { onSelect("roast_safe") }
    }
    Spacer(Modifier.height(28.dp))
    MainButton(stringResource(R.string.ob_next)) { onNext() }
}

// ── Step: Language selection ──
@Composable
private fun LanguageStep(selected: String, onSelect: (String) -> Unit, onNext: () -> Unit) {
    Title(stringResource(R.string.ob_language_title), stringResource(R.string.ob_language_subtitle))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.selectableGroup()) {
        OptionRow(selected = selected == AppLanguage.SYSTEM, label = stringResource(R.string.ob_language_system)) { onSelect(AppLanguage.SYSTEM) }
        OptionRow(selected = selected == AppLanguage.ENGLISH, label = stringResource(R.string.ob_language_english)) { onSelect(AppLanguage.ENGLISH) }
        OptionRow(selected = selected == AppLanguage.TRADITIONAL_CHINESE, label = stringResource(R.string.ob_language_traditional_chinese)) { onSelect(AppLanguage.TRADITIONAL_CHINESE) }
    }
    Spacer(Modifier.height(28.dp))
    MainButton(stringResource(R.string.ob_next)) { onNext() }
}

// ── Step: Summary ──
@Composable
private fun SummaryStep(state: OnboardingState, onNext: () -> Unit) {
    val (zodiacRes, zodiacEmoji) = zodiacFor(state.birthdayMonth, state.birthdayDay)
    val locale = Locale.getDefault()
    Illustration(R.drawable.img_onboarding_ready)
    Title(stringResource(R.string.ob_summary_title), stringResource(R.string.ob_summary_subtitle))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SummaryRow(stringResource(R.string.ob_summary_name), state.name.ifBlank { stringResource(R.string.ob_summary_not_set) })
            SummaryRow(
                stringResource(R.string.ob_summary_gender),
                when (state.gender) {
                    "female" -> stringResource(R.string.ob_gender_female)
                    "male" -> stringResource(R.string.ob_gender_male)
                    "other" -> stringResource(R.string.ob_gender_other)
                    else -> stringResource(R.string.ob_summary_not_set)
                },
            )
            SummaryRow(
                stringResource(R.string.ob_summary_hand),
                when (state.dominantHand) {
                    "left" -> stringResource(R.string.ob_hand_left)
                    "right" -> stringResource(R.string.ob_hand_right)
                    else -> stringResource(R.string.ob_summary_not_set)
                },
            )
            SummaryRow(
                stringResource(R.string.ob_summary_birthday),
                "${Month.of(state.birthdayMonth).getDisplayName(TextStyle.FULL, locale)} ${state.birthdayDay}, ${state.birthdayYear}",
            )
            SummaryRow(stringResource(R.string.ob_summary_zodiac), "$zodiacEmoji ${stringResource(zodiacRes)}")
            SummaryRow(
                stringResource(R.string.ob_summary_status),
                when (state.relationshipStatus) {
                    "single" -> stringResource(R.string.ob_status_single)
                    "relationship" -> stringResource(R.string.ob_status_relationship)
                    "married" -> stringResource(R.string.ob_status_married)
                    else -> stringResource(R.string.ob_summary_not_set)
                },
            )
            SummaryRow(
                stringResource(R.string.ob_summary_tone),
                when (state.tone) {
                    "healing" -> stringResource(R.string.ob_tone_gentle)
                    "roast_safe" -> stringResource(R.string.ob_tone_direct)
                    else -> stringResource(R.string.ob_tone_analytical)
                },
            )
            SummaryRow(
                stringResource(R.string.ob_summary_language),
                when (state.language) {
                    AppLanguage.ENGLISH -> stringResource(R.string.ob_language_english)
                    AppLanguage.TRADITIONAL_CHINESE -> stringResource(R.string.ob_language_traditional_chinese)
                    else -> stringResource(R.string.ob_language_system)
                },
            )
            SummaryRow(
                stringResource(R.string.ob_summary_analysis),
                if (state.hasBirthTime && state.hasBirthPlace) stringResource(R.string.ob_summary_l2) else stringResource(R.string.ob_summary_l1),
            )
        }
    }
    Spacer(Modifier.height(28.dp))
    MainButton(stringResource(R.string.ob_next)) { onNext() }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 15.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
    }
}

// ── Step: Camera permission education (permission itself is requested on the scan screen) ──
@Composable
private fun CameraEducationStep(enabled: Boolean, onStart: () -> Unit) {
    Illustration(R.drawable.img_onboarding_hands)
    Title(stringResource(R.string.ob_camera_title), stringResource(R.string.ob_camera_subtitle))
    Text(stringResource(R.string.ob_camera_body), fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    Column(modifier = Modifier.fillMaxWidth()) {
        BulletPoint(stringResource(R.string.ob_camera_point_local))
        BulletPoint(stringResource(R.string.ob_camera_point_denied))
    }
    Spacer(Modifier.height(28.dp))
    MainButton(stringResource(R.string.ob_camera_start), enabled = enabled) { onStart() }
}
