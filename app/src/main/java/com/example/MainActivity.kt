package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.AudioSettingsState
import com.example.audio.GameSfx
import com.example.audio.GameSoundManager
import com.example.auth.authStateFlow
import com.example.auth.signOutFromLoka
import com.example.data.AdventureRepository
import com.example.ui.AdventureTab
import com.example.ui.AdventureTabScreen
import com.example.ui.AuthScreen
import com.example.ui.BagTabScreen
import com.example.ui.GameUiState
import com.example.ui.GameViewModel
import com.example.ui.JournalProfileTabScreen
import com.example.ui.MapTabScreen
import com.example.ui.theme.LokaPetualangTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      LokaPetualangTheme {
        AppNavigation()
      }
    }
  }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    val defaultHeroName = user.displayName?.takeIf { it.isNotBlank() } ?: "Petualang Baru"
    key(user.uid) {
      MainAdventureScaffold(
        currentUserId = user.uid,
        defaultHeroName = defaultHeroName,
      )
    }
  }
}

private data class NavTabMeta(
  val tab: AdventureTab,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val navTabs =
  listOf(
    NavTabMeta(
      tab = AdventureTab.PETUALANGAN,
      labelRes = R.string.nav_adventure,
      selectedIcon = Icons.Filled.Explore,
      unselectedIcon = Icons.Outlined.Explore,
      testTag = "nav_tab_adventure",
    ),
    NavTabMeta(
      tab = AdventureTab.PETA,
      labelRes = R.string.nav_map,
      selectedIcon = Icons.Filled.Map,
      unselectedIcon = Icons.Outlined.Map,
      testTag = "nav_tab_map",
    ),
    NavTabMeta(
      tab = AdventureTab.TAS,
      labelRes = R.string.nav_bag,
      selectedIcon = Icons.Filled.Backpack,
      unselectedIcon = Icons.Outlined.Backpack,
      testTag = "nav_tab_bag",
    ),
    NavTabMeta(
      tab = AdventureTab.JURNAL,
      labelRes = R.string.nav_journal,
      selectedIcon = Icons.Filled.MenuBook,
      unselectedIcon = Icons.Outlined.MenuBook,
      testTag = "nav_tab_journal",
    ),
  )

@Composable
fun MainAdventureScaffold(
  currentUserId: String,
  defaultHeroName: String,
) {
  val viewModel: GameViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            GameViewModel(
              repository = AdventureRepository(db),
              currentUserId = currentUserId,
              defaultHeroName = defaultHeroName,
              soundController = GameSoundManager(app.applicationContext),
            )
          }
        },
    )

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val inventoryState by viewModel.inventoryState.collectAsStateWithLifecycle()
  val audioSettings by viewModel.audioSettings.collectAsStateWithLifecycle()
  val activeTab = (uiState as? GameUiState.Ready)?.transient?.activeTab ?: AdventureTab.PETUALANGAN

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner, viewModel) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> viewModel.onAudioResume()
        Lifecycle.Event.ON_PAUSE -> viewModel.onAudioPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
      viewModel.onAudioResume()
    }
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      viewModel.onAudioPause()
    }
  }

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  BoxWithConstraints(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
  ) {
    val useRail = maxWidth >= 680.dp

    if (useRail) {
      Row(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
      ) {
        NavigationRail(
          containerColor = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxHeight(),
        ) {
          Spacer(modifier = Modifier.height(12.dp))
          navTabs.forEach { item ->
            val selected = activeTab == item.tab
            val label = stringResource(item.labelRes)
            NavigationRailItem(
              selected = selected,
              onClick = { viewModel.selectTab(item.tab) },
              icon = {
                NavTabIcon(
                  tab = item.tab,
                  icon = if (selected) item.selectedIcon else item.unselectedIcon,
                  label = label,
                  bagCount = inventoryState.totalItemQuantity,
                )
              },
              label = { Text(label) },
              modifier = Modifier.testTag(item.testTag),
            )
          }
        }

        Box(
          contentAlignment = Alignment.TopCenter,
          modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
          GameStateContainer(
            uiState = uiState,
            audioSettings = audioSettings,
            viewModel = viewModel,
            onRetryInit = { viewModel.ensureStarterInitialized() },
            onSignOut = { signOutFromLoka(credentialManager, coroutineScope) },
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
          )
        }
      }
    } else {
      Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            windowInsets = WindowInsets.navigationBars,
          ) {
            navTabs.forEach { item ->
              val selected = activeTab == item.tab
              val label = stringResource(item.labelRes)
              NavigationBarItem(
                selected = selected,
                onClick = { viewModel.selectTab(item.tab) },
                icon = {
                  NavTabIcon(
                    tab = item.tab,
                    icon = if (selected) item.selectedIcon else item.unselectedIcon,
                    label = label,
                    bagCount = inventoryState.totalItemQuantity,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
        },
      ) { innerPadding ->
        Box(
          contentAlignment = Alignment.TopCenter,
          modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
          GameStateContainer(
            uiState = uiState,
            audioSettings = audioSettings,
            viewModel = viewModel,
            onRetryInit = { viewModel.ensureStarterInitialized() },
            onSignOut = { signOutFromLoka(credentialManager, coroutineScope) },
            modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
          )
        }
      }
    }
  }
}

@Composable
private fun NavTabIcon(
  tab: AdventureTab,
  icon: ImageVector,
  label: String,
  bagCount: Long,
) {
  if (tab == AdventureTab.TAS && bagCount > 0L) {
    BadgedBox(
      badge = {
        Badge {
          Text(text = if (bagCount > 99L) "99+" else "$bagCount")
        }
      }
    ) {
      Icon(imageVector = icon, contentDescription = label)
    }
  } else {
    Icon(imageVector = icon, contentDescription = label)
  }
}

@Composable
private fun GameStateContainer(
  uiState: GameUiState,
  audioSettings: AudioSettingsState,
  viewModel: GameViewModel,
  onRetryInit: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier) {
    when (uiState) {
      is GameUiState.Loading -> {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.fillMaxSize().padding(24.dp),
        ) {
          CircularProgressIndicator(modifier = Modifier.size(40.dp))
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "Menyiapkan dunia petualangan…",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }
      }

      is GameUiState.Initializing -> {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.fillMaxSize().padding(24.dp),
        ) {
          CircularProgressIndicator(modifier = Modifier.size(40.dp))
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "Membuat kompas petualang pertamamu…",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }
      }

      is GameUiState.Error -> {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.fillMaxSize().padding(24.dp),
        ) {
          Text(
            text = "Koneksi Awan Terkendala",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = uiState.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(16.dp))
          Button(onClick = onRetryInit, shape = RoundedCornerShape(14.dp)) {
            Text("Coba Muat Ulang")
          }
        }
      }

      is GameUiState.Ready -> {
        val profile = uiState.profile
        val transient = uiState.transient

        when (transient.activeTab) {
          AdventureTab.PETUALANGAN ->
            AdventureTabScreen(
              profile = profile,
              currentNode = uiState.currentNode,
              inventory = uiState.inventory,
              uiState = transient,
              onSyncNode = { node -> viewModel.syncNodeChallenges(node) },
              onTogglePipTip = { viewModel.togglePipTip() },
              onDismissBanner = { viewModel.clearBanner() },
              onRestAtCamp = { viewModel.restAtCamp(profile) },
              onSubmitRiddleOption = { node, idx -> viewModel.submitRiddleOption(node, idx) },
              onToggleRiddleHint = { viewModel.toggleRiddleHint() },
              onAttackEncounter = { node -> viewModel.attackFriendlyEncounter(profile, node) },
              onNegotiateEncounter = { node -> viewModel.negotiatePeaceEncounter(profile, node) },
              onUseHealingItem = { item -> viewModel.useInventoryItem(profile, item) },
              onChooseOption = { node, choice ->
                viewModel.chooseStoryOption(profile, node, choice)
              },
              onOpenWorldMap = { viewModel.selectTab(AdventureTab.PETA) },
              onSaveProgress = { viewModel.savePlayerProgress(profile, uiState.inventoryState) },
              achievementSummary = uiState.achievementSummary,
              onDismissAchievementNotice = { viewModel.dismissAchievementNotice() },
              audioSettings = audioSettings,
              onToggleBgm = { viewModel.toggleBgm() },
              onToggleSfx = { viewModel.toggleSfx() },
            )

          AdventureTab.PETA ->
            MapTabScreen(
              profile = profile,
              isMutating = transient.isMutating,
              banner = transient.banner,
              onDismissBanner = { viewModel.clearBanner() },
              onTravelToRegion = { regionId -> viewModel.travelToRegion(profile, regionId) },
              onBackToAdventure = { viewModel.selectTab(AdventureTab.PETUALANGAN) },
            )

          AdventureTab.TAS ->
            BagTabScreen(
              profile = profile,
              inventoryState = uiState.inventoryState,
              isMutating = transient.isMutating,
              banner = transient.banner,
              onDismissBanner = { viewModel.clearBanner() },
              onSelectCategory = { category -> viewModel.selectInventoryCategory(category) },
              onDismissCollectedNotice = { viewModel.dismissCollectedItemNotice() },
              onUseItem = { item -> viewModel.useInventoryItem(profile, item) },
              onBuyShopItem = { shopItem -> viewModel.buyShopItem(profile, shopItem) },
              onBackToAdventure = { viewModel.selectTab(AdventureTab.PETUALANGAN) },
            )

          AdventureTab.JURNAL ->
            JournalProfileTabScreen(
              profile = profile,
              inventoryState = uiState.inventoryState,
              journal = uiState.journal,
              isMutating = transient.isMutating,
              lastSavedAtMillis = transient.lastSavedAtMillis,
              banner = transient.banner,
              onDismissBanner = { viewModel.clearBanner() },
              onSaveProgress = { viewModel.savePlayerProgress(profile, uiState.inventoryState) },
              onUpdateHeroIdentity = { name, emoji, tutDone ->
                viewModel.updateHeroIdentity(name, emoji, tutDone)
              },
              onResetAdventure = { viewModel.resetAdventure() },
              onSignOut = onSignOut,
              onBackToAdventure = { viewModel.selectTab(AdventureTab.PETUALANGAN) },
              achievementSummary = uiState.achievementSummary,
              audioSettings = audioSettings,
              onToggleBgm = { viewModel.toggleBgm() },
              onToggleSfx = { viewModel.toggleSfx() },
              onBgmVolumeChange = { viewModel.setBgmVolume(it) },
              onSfxVolumeChange = { viewModel.setSfxVolume(it) },
              onTestChoiceSfx = { viewModel.playSfx(GameSfx.CHOICE_SELECT) },
              onTestItemSfx = { viewModel.playSfx(GameSfx.ITEM_COLLECTED) },
            )
        }
      }
    }
  }
}
