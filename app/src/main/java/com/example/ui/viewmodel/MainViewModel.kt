package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CatalogItem
import com.example.data.model.ContinueWatchingItem
import com.example.data.model.FlatEpisode
import com.example.data.model.ImdbInfo
import com.example.data.model.LastPlayedEntry
import com.example.data.model.PlaybackProgress
import com.example.data.model.VodInfoDetails
import com.example.data.model.XtreamCategory
import com.example.data.repository.XtreamRepository
import com.example.data.repository.cleanTitle
import com.example.data.storage.AppConfig
import com.example.data.storage.AppPreferences
import com.example.data.storage.PlaybackProgressManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.data.repository.ApiException
import com.example.data.util.normalizeServerUrl

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AppPreferences(application)
    val progressManager = PlaybackProgressManager(application)
    val repository = XtreamRepository(progressManager)

    // Pantalla actual: "login", "home", "sec", "search"
    private val _currentScreen = MutableStateFlow("login")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private var previousScreen = "home"

    // Configuración actual
    private val _config = MutableStateFlow(AppConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    // Estado del login
    private val _isLoginLoading = MutableStateFlow(false)
    val isLoginLoading: StateFlow<Boolean> = _isLoginLoading.asStateFlow()

    private val _loginError = MutableStateFlow("")
    val loginError: StateFlow<String> = _loginError.asStateFlow()

    private val _loginDiagnostic = MutableStateFlow<String?>("")
    val loginDiagnostic: StateFlow<String?> = _loginDiagnostic.asStateFlow()

    // Barra de estado inferior
    private val _statusText = MutableStateFlow("Listo.")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    // Sección actual ("live", "vod", "series")
    private val _currentMode = MutableStateFlow("vod")
    val currentMode: StateFlow<String> = _currentMode.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _isCategoriesOpen = MutableStateFlow(false)
    val isCategoriesOpen: StateFlow<Boolean> = _isCategoriesOpen.asStateFlow()

    private val _isSectionLoading = MutableStateFlow(false)
    val isSectionLoading: StateFlow<Boolean> = _isSectionLoading.asStateFlow()

    private val _currentCatalogItems = MutableStateFlow<List<CatalogItem>>(emptyList())
    val currentCatalogItems: StateFlow<List<CatalogItem>> = _currentCatalogItems.asStateFlow()

    private val _currentCategories = MutableStateFlow<List<XtreamCategory>>(emptyList())
    val currentCategories: StateFlow<List<XtreamCategory>> = _currentCategories.asStateFlow()

    private val _categoryCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val categoryCounts: StateFlow<Map<String, Int>> = _categoryCounts.asStateFlow()

    private val _heroItem = MutableStateFlow<CatalogItem?>(null)
    val heroItem: StateFlow<CatalogItem?> = _heroItem.asStateFlow()

    private val _continueList = MutableStateFlow<List<ContinueWatchingItem>>(emptyList())
    val continueList: StateFlow<List<ContinueWatchingItem>> = _continueList.asStateFlow()

    // Búsqueda global
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchFilter = MutableStateFlow("all")
    val searchFilter: StateFlow<String> = _searchFilter.asStateFlow()

    private val _searchResults = MutableStateFlow<Map<String, List<CatalogItem>>>(emptyMap())
    val searchResults: StateFlow<Map<String, List<CatalogItem>>> = _searchResults.asStateFlow()

    // Ficha de detalle
    private val _detailItem = MutableStateFlow<CatalogItem?>(null)
    val detailItem: StateFlow<CatalogItem?> = _detailItem.asStateFlow()

    private val _detailVodInfo = MutableStateFlow<VodInfoDetails?>(null)
    val detailVodInfo: StateFlow<VodInfoDetails?> = _detailVodInfo.asStateFlow()

    private val _detailEpisodes = MutableStateFlow<List<FlatEpisode>>(emptyList())
    val detailEpisodes: StateFlow<List<FlatEpisode>> = _detailEpisodes.asStateFlow()

    private val _detailImdb = MutableStateFlow<ImdbInfo?>(null)
    val detailImdb: StateFlow<ImdbInfo?> = _detailImdb.asStateFlow()

    private val _isImdbLoading = MutableStateFlow(false)
    val isImdbLoading: StateFlow<Boolean> = _isImdbLoading.asStateFlow()

    private val _translatedPlot = MutableStateFlow<String?>(null)
    val translatedPlot: StateFlow<String?> = _translatedPlot.asStateFlow()

    private val _showSpanishPlot = MutableStateFlow(false)
    val showSpanishPlot: StateFlow<Boolean> = _showSpanishPlot.asStateFlow()

    // Diálogos de Ajustes
    private val _showPasswordDialog = MutableStateFlow(false)
    val showPasswordDialog: StateFlow<Boolean> = _showPasswordDialog.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    // Reproductor Media3
    private val _isPlayerOpen = MutableStateFlow(false)
    val isPlayerOpen: StateFlow<Boolean> = _isPlayerOpen.asStateFlow()

    private val _playerUrl = MutableStateFlow("")
    val playerUrl: StateFlow<String> = _playerUrl.asStateFlow()

    private val _playerTitle = MutableStateFlow("")
    val playerTitle: StateFlow<String> = _playerTitle.asStateFlow()

    private val _playerResumeSeconds = MutableStateFlow(0L)
    val playerResumeSeconds: StateFlow<Long> = _playerResumeSeconds.asStateFlow()

    private val _playerIsLive = MutableStateFlow(false)
    val playerIsLive: StateFlow<Boolean> = _playerIsLive.asStateFlow()

    private var activePlayerKey: String? = null
    private var activePlayerLastKey: String? = null
    private var activePlayerLastEntry: LastPlayedEntry? = null

    // Video de bienvenida (Intro)
    private val _showIntro = MutableStateFlow(false)
    val showIntro: StateFlow<Boolean> = _showIntro.asStateFlow()

    init {
        viewModelScope.launch {
            val initial = preferences.configFlow.first()
            _config.value = initial
            repository.setupSession(initial.serverUrl, initial.username, initial.password)
        }
    }

    fun setStatus(text: String) {
        _statusText.value = text
    }

    fun login(username: String, pass: String) {
        if (_isLoginLoading.value) return
        val u = username.trim()
        val p = pass.trim()
        if (u.isEmpty() || p.isEmpty()) {
            _loginError.value = "Escribe tu usuario y tu contraseña."
            _loginDiagnostic.value = null
            return
        }

        viewModelScope.launch {
            _isLoginLoading.value = true
            _loginError.value = ""
            _loginDiagnostic.value = null
            setStatus("Conectando...")

            val currentCfg = _config.value
            val result = repository.login(currentCfg.serverUrl, u, p)

            _isLoginLoading.value = false

            result.onSuccess { userInfo ->
                preferences.saveCredentials(u, p)
                _config.value = _config.value.copy(username = u, password = p)

                var status = "Conectado"
                val exp = userInfo.expDate?.toLongOrNull()
                if (exp != null && exp > 0) {
                    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(exp * 1000))
                    status += "  |  Vence: $dateStr"
                }
                if (!userInfo.maxConnections.isNullOrBlank()) {
                    status += "  |  Conexiones: ${userInfo.activeCons ?: "1"}/${userInfo.maxConnections}"
                }
                setStatus(status)

                _currentScreen.value = "home"
                _showIntro.value = true
                prefetchCatalogs()
            }.onFailure { err ->
                if (err is ApiException) {
                    _loginError.value = err.userMessage
                    _loginDiagnostic.value = err.diagnostic?.formatDisplay()
                    setStatus(err.userMessage)
                } else {
                    val msg = err.message ?: "Error al conectar."
                    _loginError.value = msg
                    _loginDiagnostic.value = null
                    setStatus(msg)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            preferences.clearPassword()
            repository.clearSession()
            _config.value = _config.value.copy(password = "")
            _currentCatalogItems.value = emptyList()
            _currentCategories.value = emptyList()
            _heroItem.value = null
            _continueList.value = emptyList()
            _currentScreen.value = "login"
            setStatus("Listo.")
        }
    }

    fun closeIntro() {
        _showIntro.value = false
    }

    private fun prefetchCatalogs() {
        viewModelScope.launch {
            repository.loadCatalog("vod")
            repository.loadCatalog("series")
            repository.loadCatalog("live")
        }
    }

    fun goHome() {
        _searchQuery.value = ""
        _currentScreen.value = "home"
        _isCategoriesOpen.value = false
        setStatus("Listo.")
    }

    fun openSection(mode: String) {
        _currentMode.value = mode
        _selectedCategoryId.value = null
        _currentScreen.value = "sec"
        _isCategoriesOpen.value = false
        _isSectionLoading.value = true

        val modeLabel = if (mode == "live") "TV en vivo" else if (mode == "vod") "Películas" else "Series"
        setStatus("Cargando $modeLabel...")

        viewModelScope.launch {
            val categories = repository.loadCategories(mode)
            val items = repository.loadCatalog(mode)

            _currentCategories.value = categories.filter {
                (repository.categoryCounts[mode]?.get(it.idString) ?: 0) > 0
            }
            _categoryCounts.value = repository.categoryCounts[mode] ?: emptyMap()
            _currentCatalogItems.value = items
            _isSectionLoading.value = false

            // Seleccionar hero banner aleatorio
            if (mode != "live" && items.isNotEmpty()) {
                val candidate = items.take(30).randomOrNull()
                _heroItem.value = candidate
            } else {
                _heroItem.value = null
            }

            refreshContinueWatching()
            setStatus("${items.size} títulos")
        }
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
        val mode = _currentMode.value
        val allItems = repository.catalogCache[mode] ?: emptyList()

        _currentCatalogItems.value = if (categoryId == null) {
            allItems
        } else {
            allItems.filter { it.cid == categoryId }
        }

        setStatus("${_currentCatalogItems.value.size} títulos")
    }

    fun toggleCategoriesDrawer() {
        _isCategoriesOpen.value = !_isCategoriesOpen.value
    }

    fun closeCategoriesDrawer() {
        _isCategoriesOpen.value = false
    }

    fun refreshContinueWatching() {
        val ns = repository.currentNs
        val mode = _currentMode.value
        _continueList.value = progressManager.getContinueWatchingList(ns, mode)
    }

    fun removeContinueItem(item: ContinueWatchingItem) {
        viewModelScope.launch {
            progressManager.removeContinueItem(item.key)
            refreshContinueWatching()
        }
    }

    // Búsqueda
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.trim().isEmpty()) {
            if (_currentScreen.value == "search") {
                _currentScreen.value = previousScreen
            }
            return
        }

        if (_currentScreen.value != "search") {
            previousScreen = _currentScreen.value
            _currentScreen.value = "search"
        }

        executeSearch(query)
    }

    fun clearSearch() {
        _searchQuery.value = ""
        if (_currentScreen.value == "search") {
            _currentScreen.value = previousScreen
        }
    }

    fun setSearchFilter(filter: String) {
        _searchFilter.value = filter
    }

    private fun executeSearch(query: String) {
        viewModelScope.launch {
            val res = repository.search(query, "all")
            _searchResults.value = res
            val total = res.values.sumOf { it.size }
            setStatus("$total resultados")
        }
    }

    // Detalle
    fun openDetail(item: CatalogItem) {
        if (item.mode == "live") {
            playLive(item)
            return
        }

        _detailItem.value = item
        _detailVodInfo.value = null
        _detailEpisodes.value = emptyList()
        _detailImdb.value = null
        _translatedPlot.value = null
        _showSpanishPlot.value = false
        _isImdbLoading.value = true

        viewModelScope.launch {
            val omdbKey = _config.value.omdbApiKey
            // Lanzar búsqueda en IMDb/OMDb
            launch {
                val imdb = repository.lookupImdb(item.name, item.mode, null, omdbKey)
                _detailImdb.value = imdb
                _isImdbLoading.value = false
            }

            // Cargar datos específicos del servidor Xtream
            if (item.mode == "vod") {
                val (details, ext) = repository.getVodDetails(item.sid)
                _detailVodInfo.value = details
            } else {
                val (details, eps) = repository.getSeriesDetails(item.sid)
                _detailVodInfo.value = details
                _detailEpisodes.value = eps
            }
        }
    }

    fun closeDetail() {
        _detailItem.value = null
        _detailVodInfo.value = null
        _detailEpisodes.value = emptyList()
        _detailImdb.value = null
        refreshContinueWatching()
    }

    fun toggleTranslatePlot() {
        val currentImdb = _detailImdb.value ?: return
        val plot = currentImdb.plot ?: return
        if (_translatedPlot.value != null) {
            _showSpanishPlot.value = !_showSpanishPlot.value
            return
        }

        viewModelScope.launch {
            setStatus("Traduciendo...")
            val trans = repository.translateToSpanish(plot)
            if (trans != null) {
                _translatedPlot.value = trans
                _showSpanishPlot.value = true
                setStatus("Traducido.")
            } else {
                setStatus("No se pudo traducir.")
            }
        }
    }

    fun customImdbSearch(customQuery: String) {
        val item = _detailItem.value ?: return
        _isImdbLoading.value = true
        _detailImdb.value = null
        _translatedPlot.value = null
        _showSpanishPlot.value = false

        viewModelScope.launch {
            val imdb = repository.lookupImdb(item.name, item.mode, customQuery, _config.value.omdbApiKey)
            _detailImdb.value = imdb
            _isImdbLoading.value = false
        }
    }

    // Reproducción
    fun playLive(item: CatalogItem) {
        val url = repository.buildLiveStreamUrl(item.sid, _config.value.liveExt)
        _playerUrl.value = url
        _playerTitle.value = item.name
        _playerResumeSeconds.value = 0L
        _playerIsLive.value = true
        activePlayerKey = null
        activePlayerLastKey = null
        activePlayerLastEntry = null
        _isPlayerOpen.value = true
        setStatus("Reproduciendo: ${item.name}")
    }

    fun playVod(item: CatalogItem, fromStart: Boolean = false) {
        val ext = item.ext.ifEmpty { "mp4" }
        val url = repository.buildVodStreamUrl(item.sid, ext)
        val mk = PlaybackProgressManager.mkKey(repository.currentNs, item.sid)
        val resumeSec = if (fromStart) 0L else (progressManager.getProgress(mk)?.resumeSeconds ?: 0L)

        activePlayerKey = mk
        activePlayerLastKey = mk
        activePlayerLastEntry = LastPlayedEntry(
            kind = "vod",
            sid = item.sid,
            name = item.name,
            img = item.img,
            ext = ext
        )

        _playerUrl.value = url
        _playerTitle.value = item.name
        _playerResumeSeconds.value = resumeSec
        _playerIsLive.value = false
        _isPlayerOpen.value = true
        setStatus("Reproduciendo: ${item.name}")
    }

    fun playEpisode(item: CatalogItem, episode: FlatEpisode, fromStart: Boolean = false) {
        val resumeSec = if (fromStart) 0L else (progressManager.getProgress(episode.key)?.resumeSeconds ?: 0L)
        val title = "${cleanTitle(item.name).title} - T${episode.season} E${episode.num}${if (episode.title.isNotEmpty()) " - ${episode.title}" else ""}"

        activePlayerKey = episode.key
        activePlayerLastKey = PlaybackProgressManager.skKey(repository.currentNs, item.sid)
        activePlayerLastEntry = LastPlayedEntry(
            kind = "series",
            sid = item.sid,
            name = item.name,
            img = item.img,
            epId = episode.id,
            label = "T${episode.season} E${episode.num}"
        )

        _playerUrl.value = episode.streamUrl
        _playerTitle.value = title
        _playerResumeSeconds.value = resumeSec
        _playerIsLive.value = false
        _isPlayerOpen.value = true
        setStatus("Reproduciendo: $title")
    }

    fun onPlayerProgressUpdate(currentSeconds: Long, durationSeconds: Long) {
        val key = activePlayerKey ?: return
        viewModelScope.launch {
            progressManager.recordProgress(
                key = key,
                lastKey = activePlayerLastKey,
                lastEntry = activePlayerLastEntry,
                currentSeconds = currentSeconds,
                durationSeconds = durationSeconds
            )
        }
    }

    fun closePlayer() {
        _isPlayerOpen.value = false
        refreshContinueWatching()
        setStatus("Listo.")
    }

    fun toggleVodWatched(item: CatalogItem) {
        val mk = PlaybackProgressManager.mkKey(repository.currentNs, item.sid)
        val lastEntry = LastPlayedEntry(
            kind = "vod",
            sid = item.sid,
            name = item.name,
            img = item.img,
            ext = item.ext
        )
        viewModelScope.launch {
            progressManager.toggleWatched(mk, mk, lastEntry)
            refreshContinueWatching()
        }
    }

    fun toggleEpisodeWatched(item: CatalogItem, episode: FlatEpisode) {
        val sk = PlaybackProgressManager.skKey(repository.currentNs, item.sid)
        val lastEntry = LastPlayedEntry(
            kind = "series",
            sid = item.sid,
            name = item.name,
            img = item.img,
            epId = episode.id,
            label = "T${episode.season} E${episode.num}"
        )
        viewModelScope.launch {
            progressManager.toggleWatched(episode.key, sk, lastEntry)
            refreshContinueWatching()
        }
    }

    // Diálogos de Ajustes
    fun openSettingsPasswordDialog() {
        _showPasswordDialog.value = true
    }

    fun closePasswordDialog() {
        _showPasswordDialog.value = false
    }

    fun onPasswordAccepted() {
        _showPasswordDialog.value = false
        _showSettingsDialog.value = true
    }

    fun closeSettingsDialog() {
        _showSettingsDialog.value = false
    }

    fun saveSettings(serverUrl: String, liveExt: String, omdbKey: String) {
        viewModelScope.launch {
            val normalizedServer = normalizeServerUrl(serverUrl)
            preferences.saveSettings(normalizedServer, liveExt, omdbKey)
            _config.value = _config.value.copy(
                serverUrl = normalizedServer,
                liveExt = liveExt,
                omdbApiKey = omdbKey
            )
            repository.setupSession(normalizedServer, _config.value.username, _config.value.password)
            _showSettingsDialog.value = false
            setStatus("Ajustes guardados.")
        }
    }
}
