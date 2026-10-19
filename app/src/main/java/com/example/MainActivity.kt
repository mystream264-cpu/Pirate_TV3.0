package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.model.CatalogItem
import com.example.ui.components.BottomBar
import com.example.ui.components.DetailDialog
import com.example.ui.components.SettingsContentDialog
import com.example.ui.components.SettingsPasswordDialog
import com.example.ui.components.TopBar
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IntroPlayer
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SectionScreen
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.PirateTVTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PirateTVTheme {
                PirateTVApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PirateTVApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val config by viewModel.config.collectAsState()
    val isLoginLoading by viewModel.isLoginLoading.collectAsState()
    val loginError by viewModel.loginError.collectAsState()
    val loginDiagnostic by viewModel.loginDiagnostic.collectAsState()
    val statusText by viewModel.statusText.collectAsState()

    val currentMode by viewModel.currentMode.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val isCategoriesOpen by viewModel.isCategoriesOpen.collectAsState()
    val isSectionLoading by viewModel.isSectionLoading.collectAsState()
    val catalogItems by viewModel.currentCatalogItems.collectAsState()
    val categories by viewModel.currentCategories.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()
    val heroItem by viewModel.heroItem.collectAsState()
    val continueList by viewModel.continueList.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchFilter by viewModel.searchFilter.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    val detailItem by viewModel.detailItem.collectAsState()
    val detailVodInfo by viewModel.detailVodInfo.collectAsState()
    val detailEpisodes by viewModel.detailEpisodes.collectAsState()
    val detailImdb by viewModel.detailImdb.collectAsState()
    val isImdbLoading by viewModel.isImdbLoading.collectAsState()
    val translatedPlot by viewModel.translatedPlot.collectAsState()
    val showSpanishPlot by viewModel.showSpanishPlot.collectAsState()

    val isPlayerOpen by viewModel.isPlayerOpen.collectAsState()
    val playerUrl by viewModel.playerUrl.collectAsState()
    val playerTitle by viewModel.playerTitle.collectAsState()
    val playerResumeSeconds by viewModel.playerResumeSeconds.collectAsState()
    val playerIsLive by viewModel.playerIsLive.collectAsState()

    val showIntro by viewModel.showIntro.collectAsState()
    val showPasswordDialog by viewModel.showPasswordDialog.collectAsState()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()

    // Manejo de botón Atrás nativo de Android
    BackHandler(
        enabled = isPlayerOpen || detailItem != null || showSettingsDialog ||
                showPasswordDialog || isCategoriesOpen || currentScreen == "search" ||
                currentScreen == "sec" || showIntro
    ) {
        when {
            showIntro -> viewModel.closeIntro()
            isPlayerOpen -> viewModel.closePlayer()
            detailItem != null -> viewModel.closeDetail()
            showSettingsDialog -> viewModel.closeSettingsDialog()
            showPasswordDialog -> viewModel.closePasswordDialog()
            isCategoriesOpen -> viewModel.closeCategoriesDrawer()
            currentScreen == "search" -> viewModel.clearSearch()
            currentScreen == "sec" -> viewModel.goHome()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        if (currentScreen == "login") {
            LoginScreen(
                initialUser = config.username,
                initialPass = config.password,
                isLoading = isLoginLoading,
                errorMessage = loginError,
                diagnosticText = loginDiagnostic,
                onLogin = { u, p -> viewModel.login(u, p) },
                onOpenSettings = { viewModel.openSettingsPasswordDialog() }
            )
        } else {
            val sectionTitle = when (currentScreen) {
                "sec" -> if (currentMode == "live") "TV en vivo" else if (currentMode == "vod") "Películas" else "Series"
                "search" -> "Búsqueda"
                else -> ""
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = BackgroundDark,
                topBar = {
                    TopBar(
                        currentScreen = currentScreen,
                        sectionTitle = sectionTitle,
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.onSearchQueryChange(it) },
                        onSearchClear = { viewModel.clearSearch() },
                        onGoHome = { viewModel.goHome() },
                        onToggleCategories = { viewModel.toggleCategoriesDrawer() },
                        onOpenSettings = { viewModel.openSettingsPasswordDialog() },
                        onLogout = { viewModel.logout() }
                    )
                },
                bottomBar = {
                    BottomBar(statusText = statusText)
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
                        "home" -> {
                            HomeScreen(
                                onOpenSection = { mode -> viewModel.openSection(mode) }
                            )
                        }
                        "sec" -> {
                            val catMap = viewModel.repository.categoryNames[currentMode] ?: emptyMap()
                            SectionScreen(
                                mode = currentMode,
                                categories = categories,
                                selectedCategoryId = selectedCategoryId,
                                isCategoriesOpen = isCategoriesOpen,
                                categoryCounts = categoryCounts,
                                totalCount = viewModel.repository.catalogCache[currentMode]?.size ?: 0,
                                heroItem = heroItem,
                                continueList = continueList,
                                catalogItems = catalogItems,
                                categoryNames = catMap,
                                progressMap = viewModel.progressManager.prog,
                                isLoading = isSectionLoading,
                                onSelectCategory = { viewModel.selectCategory(it) },
                                onCloseCategories = { viewModel.closeCategoriesDrawer() },
                                onItemClick = { viewModel.openDetail(it) },
                                onContinueClick = { cItem ->
                                    val item = CatalogItem(
                                        mode = cItem.entry.kind,
                                        sid = cItem.entry.sid,
                                        name = cItem.entry.name,
                                        img = cItem.entry.img,
                                        rating = 0f,
                                        added = 0L,
                                        ext = cItem.entry.ext
                                    )
                                    viewModel.openDetail(item)
                                },
                                onRemoveContinue = { viewModel.removeContinueItem(it) },
                                onOpenHero = { viewModel.openDetail(it) }
                            )
                        }
                        "search" -> {
                            SearchScreen(
                                searchQuery = searchQuery,
                                filterMode = searchFilter,
                                searchResults = searchResults,
                                categoryNames = viewModel.repository.categoryNames,
                                onFilterChange = { viewModel.setSearchFilter(it) },
                                onItemClick = { viewModel.openDetail(it) }
                            )
                        }
                    }
                }
            }
        }

        // Ficha Modal de Detalle
        val currentDetail = detailItem
        if (currentDetail != null) {
            val vodKey = "m${viewModel.repository.currentNs}:${currentDetail.sid}"
            val vodProg = viewModel.progressManager.getProgress(vodKey)

            DetailDialog(
                item = currentDetail,
                vodDetails = detailVodInfo,
                episodes = detailEpisodes,
                imdbInfo = detailImdb,
                isImdbLoading = isImdbLoading,
                translatedPlot = translatedPlot,
                showSpanishPlot = showSpanishPlot,
                vodProgress = vodProg,
                episodeProgressMap = viewModel.progressManager.prog,
                onPlayVod = { fromStart -> viewModel.playVod(currentDetail, fromStart) },
                onToggleVodWatched = { viewModel.toggleVodWatched(currentDetail) },
                onPlayEpisode = { ep, fromStart -> viewModel.playEpisode(currentDetail, ep, fromStart) },
                onToggleEpisodeWatched = { ep -> viewModel.toggleEpisodeWatched(currentDetail, ep) },
                onToggleTranslatePlot = { viewModel.toggleTranslatePlot() },
                onCustomImdbSearch = { q -> viewModel.customImdbSearch(q) },
                onClose = { viewModel.closeDetail() }
            )
        }

        // Diálogo de contraseña de Ajustes ("3623")
        if (showPasswordDialog) {
            SettingsPasswordDialog(
                onDismiss = { viewModel.closePasswordDialog() },
                onSuccess = { viewModel.onPasswordAccepted() }
            )
        }

        // Diálogo de Ajustes
        if (showSettingsDialog) {
            SettingsContentDialog(
                initialServer = config.serverUrl,
                initialLiveExt = config.liveExt,
                initialOmdbKey = config.omdbApiKey,
                onDismiss = { viewModel.closeSettingsDialog() },
                onSave = { srv, ext, omdb ->
                    viewModel.saveSettings(srv, ext, omdb)
                }
            )
        }

        // Reproductor Media3 a pantalla completa
        if (isPlayerOpen) {
            PlayerScreen(
                streamUrl = playerUrl,
                title = playerTitle,
                resumeSeconds = playerResumeSeconds,
                isLive = playerIsLive,
                onProgressUpdate = { cur, dur ->
                    viewModel.onPlayerProgressUpdate(cur, dur)
                },
                onClose = { viewModel.closePlayer() }
            )
        }

        // Video de bienvenida a pantalla completa (Intro)
        if (showIntro) {
            IntroPlayer(
                onFinish = { viewModel.closeIntro() }
            )
        }
    }
}
