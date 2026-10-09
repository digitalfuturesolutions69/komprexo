package com.komprexo.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.komprexo.app.BuildConfig
import com.komprexo.app.R
import com.komprexo.app.billing.BillingServices

val supportedLanguages = listOf("" to R.string.language_system,"id" to R.string.language_id,"en" to R.string.language_en,
    "es" to R.string.language_es,"pt-BR" to R.string.language_pt,"hi" to R.string.language_hi)
fun documentLocale(tag: String): String = when(tag.substringBefore('-')) { "id","in"->"id"; "es"->"es"; "pt"->"pt-BR"; "hi"->"hi";else->"en" }

@Composable
fun SettingsScreen(onHome: ()->Unit, onPremium: ()->Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    var language by rememberSaveable { mutableStateOf(false) }
    var document by rememberSaveable { mutableStateOf<String?>(null) }
    var noEmail by rememberSaveable { mutableStateOf(false) }
    val billing by BillingServices.controller.ui.collectAsStateWithLifecycle()
    val documents = listOf("privacy" to R.string.privacy_policy,"terms" to R.string.terms_of_use,
        "premium" to R.string.premium_information,"licenses" to R.string.open_source_licenses,"about" to R.string.about_komprexo)
    BackHandler(document != null) { document = null }
    val title = if(document == null) stringResource(R.string.app_settings) else stringResource(documents.first { it.first == document }.second)
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing,topBar = { WorkflowTopBar(stringResource(R.string.tool_settings), onHome = onHome) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).verticalScroll(rememberScrollState())
            .padding(16.dp).testTag("appSettings"),verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title,style=MaterialTheme.typography.titleLarge,modifier=Modifier.semantics { heading() })
            if(document != null) {
                val locale = documentLocale(configuration.localesCompatTag())
                val text = remember(document,locale) { context.assets.open("legal/$locale/$document.txt").bufferedReader().use { it.readText() } }
                text.split("\n\n").forEach { paragraph ->
                    Text(paragraph.removePrefix("# "),style = if(paragraph.startsWith("# ")) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().then(if(paragraph.startsWith("# ")) Modifier.semantics { heading() } else Modifier))
                }
                if(document == "premium") TextButton({
                    try { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://support.google.com/googleplay/answer/2479637"))) }
                    catch (_: ActivityNotFoundException) { /* The offline policy remains available. */ }
                },modifier = Modifier.heightIn(min=48.dp)) { Text(stringResource(R.string.google_refund_help)) }
            } else {
                Text(stringResource(R.string.general_settings),style=MaterialTheme.typography.titleMedium)
                TextButton({ language=true },modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("languageSettings")) { Text(stringResource(R.string.language)) }
                Text(stringResource(R.string.premium_title),style=MaterialTheme.typography.titleMedium)
                Text(stringResource(if(billing.premium) R.string.billing_active else R.string.free_status),modifier=Modifier.testTag("settingsEntitlement"))
                TextButton(onPremium,modifier=Modifier.heightIn(min=48.dp).testTag("settingsPremium")) { Text(stringResource(R.string.premium_status)) }
                TextButton({ BillingServices.refresh(restore=true) },enabled=!billing.busy,modifier=Modifier.heightIn(min=48.dp).testTag("settingsRestore")) { Text(stringResource(R.string.restore_purchases)) }
                Text(stringResource(billingStatusLabel(billing.status)))
                Text(stringResource(R.string.legal_support),style=MaterialTheme.typography.titleMedium)
                documents.forEach { (key,label) -> TextButton({ document=key },modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("legal_$key")) { Text(stringResource(label)) } }
                TextButton({
                    try { context.startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:komprexo.support@gmail.com"))) }
                    catch (_: ActivityNotFoundException) { noEmail=true }
                },modifier=Modifier.heightIn(min=48.dp).testTag("contactSupport")) { Text(stringResource(R.string.contact_support)) }
                Text("komprexo.support@gmail.com")
                if(noEmail) Text(stringResource(R.string.email_unavailable))
                Text(stringResource(R.string.app_version,BuildConfig.VERSION_NAME,BuildConfig.VERSION_CODE))
            }
        }
    }
    if(language) ResponsiveSettingsDialog(stringResource(R.string.language),{language=false},{language=false}) {
        val selected=AppCompatDelegate.getApplicationLocales().toLanguageTags()
        supportedLanguages.forEach { (tag,label) ->
            OutlinedButton({
                language=false
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            },modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("locale_$tag")) {
                Text((if(selected==tag) "✓  " else "")+stringResource(label))
            }
        }
    }
}
@Suppress("DEPRECATION")
private fun android.content.res.Configuration.localesCompatTag(): String =
    if(android.os.Build.VERSION.SDK_INT>=24) locales[0].toLanguageTag() else locale.toLanguageTag()
