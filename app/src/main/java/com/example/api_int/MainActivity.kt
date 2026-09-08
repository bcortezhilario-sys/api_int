package com.example.api_int

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var countryInput: TextInputEditText
    private lateinit var searchButton: MaterialButton
    private lateinit var discoverButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var openMapButton: MaterialButton
    private lateinit var exportExcelButton: MaterialButton
    private lateinit var exportPowerPointButton: MaterialButton
    private lateinit var clearPassportButton: MaterialButton
    private lateinit var newQuizButton: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var resultCard: MaterialCardView
    private lateinit var flagImage: ImageView
    private lateinit var flagEmojiText: TextView
    private lateinit var countryNameText: TextView
    private lateinit var countryMetaText: TextView
    private lateinit var countryStatsText: TextView
    private lateinit var countryDetailText: TextView
    private lateinit var quizQuestionText: TextView
    private lateinit var quizOptionsContainer: LinearLayout
    private lateinit var quizFeedbackText: TextView
    private lateinit var passportSummaryText: TextView
    private lateinit var savedCountriesContainer: LinearLayout

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }
    private val random = Random(System.currentTimeMillis())
    private var currentCountry: CountryResult? = null
    private var activeQuiz: QuizQuestion? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindViews()
        setupActions()
        renderSavedCountries()
        statusText.text = "Busca un pais o descubre uno al azar."
    }

    private fun bindViews() {
        countryInput = findViewById(R.id.countryInput)
        searchButton = findViewById(R.id.searchButton)
        discoverButton = findViewById(R.id.discoverButton)
        saveButton = findViewById(R.id.saveButton)
        openMapButton = findViewById(R.id.openMapButton)
        exportExcelButton = findViewById(R.id.exportExcelButton)
        exportPowerPointButton = findViewById(R.id.exportPowerPointButton)
        clearPassportButton = findViewById(R.id.clearPassportButton)
        newQuizButton = findViewById(R.id.newQuizButton)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        resultCard = findViewById(R.id.resultCard)
        flagImage = findViewById(R.id.flagImage)
        flagEmojiText = findViewById(R.id.flagEmojiText)
        countryNameText = findViewById(R.id.countryNameText)
        countryMetaText = findViewById(R.id.countryMetaText)
        countryStatsText = findViewById(R.id.countryStatsText)
        countryDetailText = findViewById(R.id.countryDetailText)
        quizQuestionText = findViewById(R.id.quizQuestionText)
        quizOptionsContainer = findViewById(R.id.quizOptionsContainer)
        quizFeedbackText = findViewById(R.id.quizFeedbackText)
        passportSummaryText = findViewById(R.id.passportSummaryText)
        savedCountriesContainer = findViewById(R.id.savedCountriesContainer)
    }

    private fun setupActions() {
        searchButton.setOnClickListener { searchTypedCountry() }
        discoverButton.setOnClickListener { discoverRandomCountry() }
        saveButton.setOnClickListener { saveCurrentCountry() }
        openMapButton.setOnClickListener { openCurrentMap() }
        exportExcelButton.setOnClickListener { exportCurrentCountryAsExcel() }
        exportPowerPointButton.setOnClickListener { exportCurrentCountryAsPowerPoint() }
        clearPassportButton.setOnClickListener { clearPassport() }
        newQuizButton.setOnClickListener { startQuiz() }
        countryInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchTypedCountry()
                true
            } else {
                false
            }
        }
    }

    private fun searchTypedCountry() {
        val query = countryInput.text?.toString().orEmpty().trim()
        if (query.length < 2) {
            showError("Escribe al menos 2 caracteres para buscar un pais.")
            return
        }
        fetchCountry(query, "Buscando $query...")
    }

    private fun discoverRandomCountry() {
        val randomCountry = DISCOVERY_COUNTRIES[random.nextInt(DISCOVERY_COUNTRIES.size)]
        countryInput.setText(randomCountry)
        fetchCountry(randomCountry, "Descubriendo $randomCountry...")
    }

    private fun fetchCountry(query: String, loadingMessage: String) {
        val apiKey = BuildConfig.RESTCOUNTRIES_API_KEY.trim()
        if (apiKey.isBlank()) {
            showError("Falta RESTCOUNTRIES_API_KEY en local.properties. Agregala y sincroniza Gradle.")
            return
        }

        setLoading(true)
        statusText.text = loadingMessage

        Thread {
            val result = runCatching { requestCountry(apiKey, query) }
            runOnUiThread {
                setLoading(false)
                result
                    .onSuccess { showCountry(it) }
                    .onFailure { showError(it.message ?: "No se pudo consultar el pais.") }
            }
        }.start()
    }

    private fun requestCountry(apiKey: String, query: String): CountryResult {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val endpoint = "$COUNTRIES_API_BASE?q=$encodedQuery&pretty=1"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }

        return try {
            val responseCode = connection.responseCode
            val responseText = readResponse(connection, responseCode)
            if (responseCode !in 200..299) {
                throw IOException(formatApiError(responseCode, parseApiMessage(responseText)))
            }
            parseCountryResponse(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private fun showCountry(country: CountryResult) {
        currentCountry = country
        resultCard.visibility = View.VISIBLE
        countryNameText.text = country.displayName
        countryMetaText.text = country.metaText()
        countryStatsText.text = country.statsText()
        countryDetailText.text = country.detailText()
        flagEmojiText.text = country.flagEmoji.ifBlank { country.code.ifBlank { "?" } }
        flagEmojiText.visibility = View.VISIBLE
        saveButton.isEnabled = true
        openMapButton.isEnabled = country.mapUrl.isNotBlank()
        exportExcelButton.isEnabled = true
        exportPowerPointButton.isEnabled = true
        statusText.text = "Pais encontrado. Agrega su sello al pasaporte."
        loadFlagImage(country.flagPngUrl)
    }

    private fun showError(message: String) {
        currentCountry = null
        resultCard.visibility = View.GONE
        saveButton.isEnabled = false
        openMapButton.isEnabled = false
        exportExcelButton.isEnabled = false
        exportPowerPointButton.isEnabled = false
        flagImage.setImageDrawable(null)
        statusText.text = message
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        searchButton.isEnabled = !isLoading
        discoverButton.isEnabled = !isLoading
        clearPassportButton.isEnabled = !isLoading && loadSavedCountries().isNotEmpty()
        newQuizButton.isEnabled = !isLoading && loadSavedCountries().size >= MIN_COUNTRIES_FOR_QUIZ
        saveButton.isEnabled = !isLoading && currentCountry != null
        openMapButton.isEnabled = !isLoading && currentCountry?.mapUrl?.isNotBlank() == true
        exportExcelButton.isEnabled = !isLoading && currentCountry != null
        exportPowerPointButton.isEnabled = !isLoading && currentCountry != null
    }

    private fun saveCurrentCountry() {
        val country = currentCountry ?: return
        val savedCountries = loadSavedCountries().toMutableList()
        savedCountries.removeAll { it.sameCountryAs(country) }
        savedCountries.add(0, country)
        saveSavedCountries(savedCountries.take(MAX_SAVED_COUNTRIES))
        activeQuiz = null
        renderSavedCountries()
        Toast.makeText(this, "Sello agregado", Toast.LENGTH_SHORT).show()
    }

    private fun openCurrentMap() {
        val mapUrl = currentCountry?.mapUrl.orEmpty()
        if (mapUrl.isNotBlank()) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl)))
        }
    }

    private fun exportCurrentCountryAsExcel() {
        val country = currentCountry ?: return
        runCatching {
            val file = CountryExportWriter.createExcelFile(cacheDir, country)
            shareExport(file, EXCEL_MIME_TYPE, "Exportar a Excel")
        }.onSuccess {
            statusText.text = "Excel generado para ${country.displayName}."
        }.onFailure { error ->
            showExportError("Excel", error)
        }
    }

    private fun exportCurrentCountryAsPowerPoint() {
        val country = currentCountry ?: return
        runCatching {
            val file = CountryExportWriter.createPowerPointFile(cacheDir, country)
            shareExport(file, POWERPOINT_MIME_TYPE, "Exportar a PowerPoint")
        }.onSuccess {
            statusText.text = "PowerPoint generado para ${country.displayName}."
        }.onFailure { error ->
            showExportError("PowerPoint", error)
        }
    }

    private fun shareExport(file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(sendIntent, title))
    }

    private fun showExportError(fileType: String, error: Throwable) {
        val message = "No se pudo generar el archivo $fileType."
        statusText.text = message
        Toast.makeText(this, "$message ${error.message.orEmpty()}", Toast.LENGTH_LONG).show()
    }

    private fun clearPassport() {
        saveSavedCountries(emptyList())
        activeQuiz = null
        renderSavedCountries()
        Toast.makeText(this, "Pasaporte limpiado", Toast.LENGTH_SHORT).show()
    }

    private fun loadFlagImage(imageUrl: String?) {
        flagImage.setImageDrawable(null)
        if (imageUrl.isNullOrBlank()) {
            return
        }

        val expectedUrl = imageUrl
        Thread {
            val bitmap = runCatching {
                (URL(expectedUrl).openConnection() as HttpURLConnection).run {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    inputStream.use { BitmapFactory.decodeStream(it) }
                }
            }.getOrNull()

            runOnUiThread {
                if (currentCountry?.flagPngUrl == expectedUrl && bitmap != null) {
                    flagImage.setImageBitmap(bitmap)
                    flagEmojiText.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun renderSavedCountries() {
        val savedCountries = loadSavedCountries()
        clearPassportButton.isEnabled = savedCountries.isNotEmpty()
        newQuizButton.isEnabled = savedCountries.size >= MIN_COUNTRIES_FOR_QUIZ
        passportSummaryText.text = buildPassportSummary(savedCountries)
        savedCountriesContainer.removeAllViews()

        if (activeQuiz == null || savedCountries.size < MIN_COUNTRIES_FOR_QUIZ) {
            renderQuizPrompt(savedCountries)
        }

        if (savedCountries.isEmpty()) {
            savedCountriesContainer.addView(
                createText(
                    text = "Aun no hay sellos. Busca Canada, Peru, Japan o Brazil y toca Agregar sello.",
                    sizeSp = 14f,
                    color = "#667085"
                )
            )
            return
        }

        savedCountries.forEachIndexed { index, country ->
            savedCountriesContainer.addView(createStampCard(index + 1, country))
        }
    }

    private fun buildPassportSummary(countries: List<CountryResult>): String {
        if (countries.isEmpty()) {
            return "0 sellos guardados. Empieza buscando o descubriendo un pais."
        }
        val regions = countries.map { it.region }.filter { it.isNotBlank() }.distinct().size
        val languages = countries.flatMap { it.languages }.distinct().size
        val latest = countries.firstOrNull()?.displayName.orEmpty()
        return "${countries.size} sellos | $regions regiones | $languages idiomas | Ultimo: $latest"
    }

    private fun renderQuizPrompt(countries: List<CountryResult>) {
        quizOptionsContainer.removeAllViews()
        quizFeedbackText.text = ""
        quizFeedbackText.setTextColor(Color.parseColor("#667085"))
        quizQuestionText.text = if (countries.size < MIN_COUNTRIES_FOR_QUIZ) {
            "Guarda al menos 3 paises para activar preguntas de capital, moneda y bandera."
        } else {
            "Toca Nuevo quiz para practicar con los sellos de tu pasaporte."
        }
    }

    private fun startQuiz() {
        val savedCountries = loadSavedCountries()
        val question = buildQuizQuestions(savedCountries).randomOrNull(random)
        if (question == null) {
            activeQuiz = null
            renderQuizPrompt(savedCountries)
            quizFeedbackText.text = "Faltan datos suficientes. Guarda paises con capital, moneda o bandera."
            quizFeedbackText.setTextColor(Color.parseColor("#B42318"))
            return
        }

        activeQuiz = question
        quizQuestionText.text = question.prompt
        quizFeedbackText.text = ""
        quizFeedbackText.setTextColor(Color.parseColor("#667085"))
        quizOptionsContainer.removeAllViews()
        question.options.forEach { option ->
            quizOptionsContainer.addView(createQuizOptionButton(option))
        }
    }

    private fun createQuizOptionButton(option: String): MaterialButton {
        return MaterialButton(this).apply {
            text = option
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                46.dp()
            ).apply { bottomMargin = 8.dp() }
            cornerRadius = 8.dp()
            setOnClickListener { answerQuiz(option) }
        }
    }

    private fun answerQuiz(selectedAnswer: String) {
        val question = activeQuiz ?: return
        val correct = selectedAnswer == question.correctAnswer
        quizFeedbackText.text = if (correct) {
            "Correcto. ${question.explanation}"
        } else {
            "Casi. La respuesta era ${question.correctAnswer}. ${question.explanation}"
        }
        quizFeedbackText.setTextColor(Color.parseColor(if (correct) "#047857" else "#B42318"))

        for (index in 0 until quizOptionsContainer.childCount) {
            quizOptionsContainer.getChildAt(index).isEnabled = false
        }
    }

    private fun buildQuizQuestions(countries: List<CountryResult>): List<QuizQuestion> {
        if (countries.size < MIN_COUNTRIES_FOR_QUIZ) {
            return emptyList()
        }

        val questions = mutableListOf<QuizQuestion>()
        countries.forEach { country ->
            val otherCountries = countries.filterNot { it.sameCountryAs(country) }

            val wrongCapitals = otherCountries.map { it.capital }.filter { it.isKnownValue() }.distinct()
            if (country.capital.isKnownValue() && wrongCapitals.size >= 2) {
                questions.add(
                    QuizQuestion(
                        prompt = "Cual es la capital de ${country.displayName}?",
                        correctAnswer = country.capital,
                        options = makeOptions(country.capital, wrongCapitals),
                        explanation = "${country.displayName} tiene como capital ${country.capital}."
                    )
                )
            }

            val currency = country.primaryCurrency()
            val wrongCurrencies = otherCountries.mapNotNull { otherCountry ->
                otherCountry.primaryCurrency().takeIf { it.isKnownValue() }
            }
                .distinct()
            if (currency.isKnownValue() && wrongCurrencies.size >= 2) {
                questions.add(
                    QuizQuestion(
                        prompt = "Que moneda usa ${country.displayName}?",
                        correctAnswer = currency,
                        options = makeOptions(currency, wrongCurrencies),
                        explanation = "La moneda registrada es $currency."
                    )
                )
            }

            val wrongNames = otherCountries.map { it.displayName }.filter { it.isKnownValue() }.distinct()
            if (country.flagEmoji.isNotBlank() && wrongNames.size >= 2) {
                questions.add(
                    QuizQuestion(
                        prompt = "A que pais pertenece esta bandera? ${country.flagEmoji}",
                        correctAnswer = country.displayName,
                        options = makeOptions(country.displayName, wrongNames),
                        explanation = "La bandera corresponde a ${country.displayName}."
                    )
                )
            }

            val wrongRegions = otherCountries.map { it.region }.filter { it.isKnownValue() }.distinct()
            if (country.region.isKnownValue() && wrongRegions.size >= 2) {
                questions.add(
                    QuizQuestion(
                        prompt = "En que region esta ${country.displayName}?",
                        correctAnswer = country.region,
                        options = makeOptions(country.region, wrongRegions),
                        explanation = "${country.displayName} pertenece a ${country.region}."
                    )
                )
            }
        }
        return questions
    }

    private fun makeOptions(correctAnswer: String, wrongAnswers: List<String>): List<String> {
        return (wrongAnswers.filter { it != correctAnswer }.shuffled(random).take(2) + correctAnswer)
            .distinct()
            .shuffled(random)
    }

    private fun createStampCard(stampNumber: Int, country: CountryResult): MaterialCardView {
        val card = baseCard(bottomMargin = 12)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val title = createText(
            text = "Sello #$stampNumber  ${country.flagEmoji} ${country.displayName}".trim(),
            sizeSp = 16f,
            color = "#101828",
            typefaceStyle = Typeface.BOLD
        ).apply {
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }

        content.addView(title)
        content.addView(createText(country.metaText(), 13f, "#667085"))
        content.addView(createText("Capital: ${country.capital}", 14f, "#344054"))
        content.addView(createText("Moneda: ${country.primaryCurrency().ifBlank { "No disponible" }}", 14f, "#344054"))
        content.addView(createStampActions(country))
        card.addView(content)
        return card
    }

    private fun createStampActions(country: CountryResult): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 10.dp() }
        }

        val viewButton = createSmallButton("Ver").apply {
            setOnClickListener {
                showCountry(country)
                countryInput.setText(country.displayName)
            }
        }
        val mapButton = createSmallButton("Mapa").apply {
            isEnabled = country.mapUrl.isNotBlank()
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(country.mapUrl)))
            }
        }
        val removeButton = createSmallButton("Quitar").apply {
            setOnClickListener {
                val remaining = loadSavedCountries().filterNot { it.sameCountryAs(country) }
                saveSavedCountries(remaining)
                activeQuiz = null
                renderSavedCountries()
            }
        }

        row.addView(viewButton)
        row.addView(mapButton)
        row.addView(removeButton)
        return row
    }

    private fun baseCard(bottomMargin: Int): MaterialCardView {
        return MaterialCardView(this).apply {
            radius = 8.dp().toFloat()
            cardElevation = 1.dp().toFloat()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { this.bottomMargin = bottomMargin.dp() }
            setContentPadding(14.dp(), 14.dp(), 14.dp(), 14.dp())
        }
    }

    private fun createSmallButton(label: String): MaterialButton {
        return MaterialButton(this).apply {
            text = label
            isAllCaps = false
            minHeight = 0
            minimumHeight = 0
            setPadding(8.dp(), 0, 8.dp(), 0)
            layoutParams = LinearLayout.LayoutParams(0, 42.dp(), 1f).apply {
                marginEnd = 8.dp()
            }
            cornerRadius = 8.dp()
        }
    }

    private fun createText(
        text: String,
        sizeSp: Float,
        color: String,
        typefaceStyle: Int = Typeface.NORMAL
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(Color.parseColor(color))
            setTypeface(typeface, typefaceStyle)
            setLineSpacing(2f, 1f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 5.dp() }
        }
    }

    private fun parseCountryResponse(body: String): CountryResult {
        val countryJson = firstCountryObject(body)
            ?: throw IOException("No se encontraron paises para esa busqueda.")
        return parseCountry(countryJson)
    }

    private fun firstCountryObject(body: String): JSONObject? {
        val trimmed = body.trim()
        return if (trimmed.startsWith("[")) {
            JSONArray(trimmed).optJSONObject(0)
        } else {
            val root = JSONObject(trimmed)
            val dataObject = root.optJSONObject("data")
            root.optJSONArray("data")?.optJSONObject(0)
                ?: dataObject?.optJSONArray("objects")?.optJSONObject(0)
                ?: dataObject?.optJSONArray("countries")?.optJSONObject(0)
                ?: dataObject?.optJSONArray("results")?.optJSONObject(0)
                ?: dataObject?.takeIf { it.has("name") || it.has("names") || it.has("commonName") || it.has("country") }
                ?: root.optJSONArray("objects")?.optJSONObject(0)
                ?: root.optJSONArray("countries")?.optJSONObject(0)
                ?: root.optJSONArray("results")?.optJSONObject(0)
                ?: root.takeIf { it.has("name") || it.has("names") || it.has("commonName") || it.has("country") }
        }
    }

    private fun parseCountry(json: JSONObject): CountryResult {
        val nameObject = json.optJSONObject("name") ?: json.optJSONObject("names")
        val displayName = nameObject?.firstText("common", "official")
            ?: json.firstText("commonName", "name", "country", "officialName")
            ?: "Pais sin nombre"

        val officialName = nameObject?.firstText("official", "common")
            ?: json.firstText("officialName", "name")
            ?: displayName

        val codesObject = json.optJSONObject("codes")
        val code = json.firstText("cca2", "code", "alpha2Code", "countryCode")
            ?: codesObject?.firstText("alpha_2", "alpha2", "cca2")
            ?: json.firstText("cca3", "alpha3Code")
            ?: codesObject?.firstText("alpha_3", "alpha3", "cca3")
            ?: ""

        val capital = json.capitalText().ifBlank {
            json.firstText("capitalCity", "capital") ?: "Sin capital registrada"
        }
        val region = json.firstText("region", "continent") ?: ""
        val subregion = json.firstText("subregion", "subRegion") ?: ""
        val population = json.firstLong("population")
        val area = json.areaInSquareKilometers()
        val languages = json.stringListFromAny("languages")
        val currencies = json.currencyList()
        val timezones = json.stringListFromAny("timezones", "time_zones")
        val continents = json.stringListFromAny("continents")
        val flagObject = json.optJSONObject("flag")
        val flagEmoji = flagObject?.firstText("emoji")
            ?: json.firstText("flag", "emoji")
            ?: codeToFlagEmoji(code)
        val flagsObject = json.optJSONObject("flags") ?: flagObject
        val flagPng = flagsObject?.firstText("png", "url_png")
            ?: json.firstText("flagPng", "flag_png")
        val mapObject = json.optJSONObject("maps") ?: json.optJSONObject("links")
        val mapUrl = mapObject?.firstText("googleMaps", "openStreetMaps")
            ?: mapObject?.firstText("google_maps", "open_street_maps")
            ?: json.firstText("mapUrl", "mapsUrl")
            ?: ""

        return CountryResult(
            displayName = displayName,
            officialName = officialName,
            code = code,
            capital = capital,
            region = region,
            subregion = subregion,
            population = population,
            area = area,
            languages = languages,
            currencies = currencies,
            timezones = timezones,
            continents = continents,
            flagEmoji = flagEmoji,
            flagPngUrl = flagPng,
            mapUrl = mapUrl
        )
    }

    private fun readResponse(connection: HttpURLConnection, responseCode: Int): String {
        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }
        return stream.bufferedReader().use { it.readText() }
    }

    private fun parseApiMessage(body: String): String {
        return runCatching {
            val json = JSONObject(body)
            json.firstText("message", "error", "detail", "errorMessage") ?: body
        }.getOrDefault(body.ifBlank { "Respuesta sin detalle." })
    }

    private fun formatApiError(responseCode: Int, apiMessage: String): String {
        return when (responseCode) {
            401, 403 -> "La API rechazo la autorizacion. Revisa RESTCOUNTRIES_API_KEY."
            404 -> "No se encontro el pais solicitado."
            429 -> "Demasiadas consultas. Espera un momento y vuelve a intentar."
            else -> "HTTP $responseCode: $apiMessage"
        }
    }

    private fun loadSavedCountries(): List<CountryResult> {
        val raw = prefs.getString(SAVED_COUNTRIES_KEY, "[]").orEmpty()
        return runCatching {
            val jsonArray = JSONArray(raw)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    add(CountryResult.fromJson(jsonArray.getJSONObject(index)))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveSavedCountries(countries: List<CountryResult>) {
        val jsonArray = JSONArray()
        countries.forEach { jsonArray.put(it.toJson()) }
        prefs.edit().putString(SAVED_COUNTRIES_KEY, jsonArray.toString()).apply()
    }

    private fun CountryResult.sameCountryAs(other: CountryResult): Boolean {
        val thisKey = code.ifBlank { displayName }.uppercase(Locale.US)
        val otherKey = other.code.ifBlank { other.displayName }.uppercase(Locale.US)
        return thisKey == otherKey
    }

    private fun CountryResult.primaryCurrency(): String {
        return currencies.firstOrNull().orEmpty()
    }

    private fun String.isKnownValue(): Boolean {
        return isNotBlank() &&
            !contains("No disponible", ignoreCase = true) &&
            !contains("Sin capital", ignoreCase = true)
    }

    private fun JSONObject.firstText(vararg keys: String): String? {
        keys.forEach { key ->
            val value = opt(key) ?: return@forEach
            val text = when (value) {
                is String -> value
                is Number, is Boolean -> value.toString()
                else -> ""
            }.trim()
            if (text.isNotBlank() && text != "null") {
                return text
            }
        }
        return null
    }

    private fun JSONObject.joinedText(key: String): String {
        val value = opt(key) ?: return ""
        return when (value) {
            is JSONArray -> value.toStringList().joinToString(", ")
            is String -> value
            else -> ""
        }.trim()
    }

    private fun JSONObject.firstLong(vararg keys: String): Long {
        keys.forEach { key ->
            if (has(key) && !isNull(key)) {
                return optLong(key, 0L)
            }
        }
        return 0L
    }

    private fun JSONObject.firstDouble(vararg keys: String): Double {
        keys.forEach { key ->
            if (has(key) && !isNull(key)) {
                val value = opt(key)
                return when (value) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull() ?: 0.0
                    is JSONObject -> value.firstDouble("kilometers", "km2", "value")
                    else -> 0.0
                }
            }
        }
        return 0.0
    }

    private fun JSONObject.capitalText(): String {
        val capitals = optJSONArray("capitals")
        if (capitals != null) {
            return capitals.toStringList().joinToString(", ")
        }
        return joinedText("capital")
    }

    private fun JSONObject.areaInSquareKilometers(): Double {
        val areaValue = opt("area") ?: return 0.0
        return when (areaValue) {
            is Number -> areaValue.toDouble()
            is String -> areaValue.toDoubleOrNull() ?: 0.0
            is JSONObject -> areaValue.firstDouble("kilometers", "km2", "squareKilometers", "value")
            else -> 0.0
        }
    }

    private fun JSONObject.stringListFromAny(vararg keys: String): List<String> {
        keys.forEach { key ->
            val value = opt(key) ?: return@forEach
            val result = when (value) {
                is JSONArray -> value.toStringList()
                is JSONObject -> value.keys().asSequence().mapNotNull { childKey ->
                    value.optString(childKey).takeIf { it.isNotBlank() && it != "null" }
                }.toList()
                is String -> value.split(",").map { it.trim() }.filter { it.isNotBlank() }
                else -> emptyList()
            }
            if (result.isNotEmpty()) {
                return result
            }
        }
        return emptyList()
    }

    private fun JSONObject.currencyList(): List<String> {
        val value = opt("currencies") ?: return emptyList()
        return when (value) {
            is JSONArray -> {
                val currencies = mutableListOf<String>()
                for (index in 0 until value.length()) {
                    val item = value.opt(index)
                    val text = when (item) {
                        is JSONObject -> {
                            val code = item.firstText("code", "isoCode").orEmpty()
                            val name = item.firstText("name", "common", "value") ?: code
                            val symbol = item.firstText("symbol").orEmpty()
                            when {
                                code.isNotBlank() && symbol.isNotBlank() -> "$name ($code, $symbol)"
                                code.isNotBlank() -> "$name ($code)"
                                else -> name
                            }
                        }
                        is String -> item
                        else -> ""
                    }.trim()
                    if (text.isNotBlank() && text != "null") {
                        currencies.add(text)
                    }
                }
                currencies
            }
            is JSONObject -> value.keys().asSequence().map { code ->
                val currency = value.optJSONObject(code)
                val name = currency?.firstText("name") ?: code
                val symbol = currency?.firstText("symbol").orEmpty()
                if (symbol.isBlank()) "$name ($code)" else "$name ($code, $symbol)"
            }.toList()
            is String -> listOf(value)
            else -> emptyList()
        }
    }

    private fun JSONArray.toStringList(): List<String> {
        val values = mutableListOf<String>()
        for (index in 0 until length()) {
            val item = opt(index)
            val text = when (item) {
                is String -> item
                is Number, is Boolean -> item.toString()
                is JSONObject -> item.firstText("name", "common", "official", "value") ?: ""
                else -> ""
            }.trim()
            if (text.isNotBlank() && text != "null") {
                values.add(text)
            }
        }
        return values
    }

    private fun codeToFlagEmoji(code: String): String {
        val normalized = code.uppercase(Locale.US)
        if (normalized.length != 2 || normalized.any { it !in 'A'..'Z' }) {
            return ""
        }
        return normalized.map { char ->
            Character.toChars(0x1F1E6 + (char.code - 'A'.code)).concatToString()
        }.joinToString("")
    }

    private fun formatNumber(value: Long): String {
        return integerFormat.format(value)
    }

    private fun formatNumber(value: Double): String {
        return integerFormat.format(value)
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).roundToInt()
    }

    data class CountryResult(
        val displayName: String,
        val officialName: String,
        val code: String,
        val capital: String,
        val region: String,
        val subregion: String,
        val population: Long,
        val area: Double,
        val languages: List<String>,
        val currencies: List<String>,
        val timezones: List<String>,
        val continents: List<String>,
        val flagEmoji: String,
        val flagPngUrl: String?,
        val mapUrl: String
    ) {
        val density: Double
            get() = if (area > 0.0) population / area else 0.0

        fun metaText(): String {
            return listOf(region, subregion, code)
                .filter { it.isNotBlank() }
                .joinToString(" | ")
                .ifBlank { officialName }
        }

        fun statsText(): String {
            return listOf(
                "Capital: $capital",
                "Poblacion: ${integerFormat.format(population)}",
                "Area: ${integerFormat.format(area)} km2",
                "Densidad: ${decimalFormat.format(density)} hab/km2"
            ).joinToString("\n")
        }

        fun detailText(): String {
            val languageText = languages.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "No disponible"
            val currencyText = currencies.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "No disponible"
            val timezoneText = timezones.takeIf { it.isNotEmpty() }?.take(4)?.joinToString(", ") ?: "No disponible"
            val continentText = continents.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "No disponible"
            return listOf(
                "Idiomas: $languageText",
                "Monedas: $currencyText",
                "Zonas horarias: $timezoneText",
                "Continentes: $continentText"
            ).joinToString("\n")
        }

        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("displayName", displayName)
                put("officialName", officialName)
                put("code", code)
                put("capital", capital)
                put("region", region)
                put("subregion", subregion)
                put("population", population)
                put("area", area)
                put("languages", JSONArray(languages))
                put("currencies", JSONArray(currencies))
                put("timezones", JSONArray(timezones))
                put("continents", JSONArray(continents))
                put("flagEmoji", flagEmoji)
                put("flagPngUrl", flagPngUrl)
                put("mapUrl", mapUrl)
            }
        }

        companion object {
            fun fromJson(json: JSONObject): CountryResult {
                return CountryResult(
                    displayName = json.optString("displayName"),
                    officialName = json.optString("officialName"),
                    code = json.optString("code"),
                    capital = json.optString("capital"),
                    region = json.optString("region"),
                    subregion = json.optString("subregion"),
                    population = json.optLong("population"),
                    area = json.optDouble("area"),
                    languages = jsonArrayToStringList(json.optJSONArray("languages")),
                    currencies = jsonArrayToStringList(json.optJSONArray("currencies")),
                    timezones = jsonArrayToStringList(json.optJSONArray("timezones")),
                    continents = jsonArrayToStringList(json.optJSONArray("continents")),
                    flagEmoji = json.optString("flagEmoji"),
                    flagPngUrl = json.optString("flagPngUrl").ifBlank { null },
                    mapUrl = json.optString("mapUrl")
                )
            }

            private fun jsonArrayToStringList(array: JSONArray?): List<String> {
                if (array == null) {
                    return emptyList()
                }
                val values = mutableListOf<String>()
                for (index in 0 until array.length()) {
                    val text = array.optString(index).trim()
                    if (text.isNotBlank() && text != "null") {
                        values.add(text)
                    }
                }
                return values
            }
        }
    }

    data class QuizQuestion(
        val prompt: String,
        val correctAnswer: String,
        val options: List<String>,
        val explanation: String
    )

    companion object {
        private const val PREFS_NAME = "countries_atlas"
        private const val SAVED_COUNTRIES_KEY = "saved_countries"
        private const val COUNTRIES_API_BASE = "https://api.restcountries.com/countries/v5"
        private const val MAX_SAVED_COUNTRIES = 30
        private const val MIN_COUNTRIES_FOR_QUIZ = 3
        private const val EXCEL_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        private const val POWERPOINT_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"

        private val integerFormat = DecimalFormat("#,##0")
        private val decimalFormat = DecimalFormat("#,##0.0")

        private val DISCOVERY_COUNTRIES = listOf(
            "Canada",
            "Peru",
            "Japan",
            "Brazil",
            "Germany",
            "France",
            "Italy",
            "Spain",
            "Mexico",
            "Argentina",
            "Chile",
            "Colombia",
            "Australia",
            "New Zealand",
            "South Korea",
            "Egypt",
            "Morocco",
            "India",
            "Norway",
            "Sweden",
            "Finland",
            "Greece",
            "Portugal",
            "Thailand",
            "Indonesia"
        )
    }
}
