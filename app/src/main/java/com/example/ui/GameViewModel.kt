package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioSettingsState
import com.example.audio.GameSfx
import com.example.audio.NoOpSoundController
import com.example.audio.SoundController
import com.example.data.AdventureRepository
import com.example.data.InventoryItem
import com.example.data.JournalEntry
import com.example.data.PlayerProfile
import com.example.game.AchievementBadge
import com.example.game.AchievementCatalog
import com.example.game.AchievementSummary
import com.example.game.AdventureWorld
import com.example.game.CollectedItemEvent
import com.example.game.GameProgressionEngine
import com.example.game.InventoryCategory
import com.example.game.InventoryState
import com.example.game.ItemReward
import com.example.game.RegionInfo
import com.example.game.RiddleChallenge
import com.example.game.ShopItem
import com.example.game.StoryActionResult
import com.example.game.StoryChoice
import com.example.game.StoryNode
import com.example.game.TransitionOutcome
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AdventureTab {
  PETUALANGAN,
  PETA,
  TAS,
  JURNAL,
}

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

data class FeedbackBanner(
  val title: String,
  val message: String,
  val isCelebration: Boolean = false,
  val isWarning: Boolean = false,
)

data class RiddleUiState(
  val nodeId: String = "",
  val selectedOptionIndex: Int? = null,
  val solved: Boolean = false,
  val hintVisible: Boolean = false,
  val statusMessage: String? = null,
)

data class EncounterUiState(
  val nodeId: String = "",
  val enemyHp: Int = 0,
  val enemyMaxHp: Int = 0,
  val won: Boolean = false,
  val battleLog: String = "",
)

data class AdventureTransientState(
  val activeTab: AdventureTab = AdventureTab.PETUALANGAN,
  val isMutating: Boolean = false,
  val pipTipExpanded: Boolean = true,
  val banner: FeedbackBanner? = null,
  val newlyUnlockedBadge: AchievementBadge? = null,
  val riddle: RiddleUiState = RiddleUiState(),
  val encounter: EncounterUiState = EncounterUiState(),
  val lastSavedAtMillis: Long? = null,
)

private data class InventoryUiMeta(
  val selectedCategory: InventoryCategory = InventoryCategory.ALL,
  val lastCollectedItem: CollectedItemEvent? = null,
)

sealed interface GameUiState {
  data object Loading : GameUiState

  data object Initializing : GameUiState

  data class Error(val message: String) : GameUiState

  data class Ready(
    val profile: PlayerProfile,
    val currentRegion: RegionInfo,
    val currentNode: StoryNode,
    val inventory: List<InventoryItem>,
    val inventoryState: InventoryState,
    val journal: List<JournalEntry>,
    val achievementSummary: AchievementSummary = AchievementSummary(),
    val transient: AdventureTransientState,
  ) : GameUiState
}

class GameViewModel(
  private val repository: AdventureRepository,
  private val currentUserId: String,
  private val defaultHeroName: String,
  private val soundController: SoundController = NoOpSoundController,
) : ViewModel() {

  val audioSettings: StateFlow<AudioSettingsState> = soundController.settings

  val initialStoryNodes: List<StoryNode> = createInitialStorySample()

  private val storyCatalog: Map<String, StoryNode> =
    AdventureWorld.allNodesMap() + initialStoryNodes.associateBy { it.id }

  fun resolveStoryNode(nodeId: String): StoryNode =
    storyCatalog[nodeId] ?: AdventureWorld.getNode(nodeId)

  private val profileFlow: Flow<UiState<PlayerProfile?>> =
    repository
      .observePlayerProfile(currentUserId)
      .map<PlayerProfile?, UiState<PlayerProfile?>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing player profile", rootError)
        emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat profil petualang."))
      }

  private val inventoryFlow: Flow<UiState<List<InventoryItem>>> =
    repository
      .observeInventory(currentUserId)
      .map<List<InventoryItem>, UiState<List<InventoryItem>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing inventory", rootError)
        emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat isi tas."))
      }

  private val journalFlow: Flow<UiState<List<JournalEntry>>> =
    repository
      .observeJournal(currentUserId)
      .map<List<JournalEntry>, UiState<List<JournalEntry>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing journal", rootError)
        emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat jurnal petualangan."))
      }

  private val _transientState = MutableStateFlow(AdventureTransientState())
  private val _inventoryMeta = MutableStateFlow(InventoryUiMeta())
  private var knownUnlockedAchievementIds: Set<String>? = null

  val inventoryState: StateFlow<InventoryState> =
    combine(inventoryFlow, _inventoryMeta) { inventoryRes, meta ->
        val items = (inventoryRes as? UiState.Success)?.data ?: emptyList()
        InventoryState(
          items = items.sortedBy { it.name },
          selectedCategory = meta.selectedCategory,
          lastCollectedItem = meta.lastCollectedItem,
          isLoading = inventoryRes is UiState.Loading,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = InventoryState(isLoading = true),
      )

  val uiState: StateFlow<GameUiState> =
    combine(profileFlow, inventoryState, journalFlow, _transientState) {
        profileRes,
        invState,
        journalRes,
        transient ->
        when (profileRes) {
          is UiState.Loading -> GameUiState.Loading
          is UiState.Error -> GameUiState.Error(profileRes.message)
          is UiState.Success -> {
            val profile = profileRes.data
            if (profile == null) {
              GameUiState.Initializing
            } else {
              val currentNode = resolveStoryNode(profile.currentNodeId)
              val currentRegion = AdventureWorld.getRegion(currentNode.regionId)
              val journal = (journalRes as? UiState.Success)?.data ?: emptyList()
              val achievementSummary = AchievementCatalog.evaluate(profile, invState.items)
              GameUiState.Ready(
                profile = profile,
                currentRegion = currentRegion,
                currentNode = currentNode,
                inventory = invState.items,
                inventoryState = invState,
                journal = journal,
                achievementSummary = achievementSummary,
                transient = transient,
              )
            }
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = GameUiState.Loading,
      )

  init {
    ensureStarterInitialized()
    observeAchievementUnlocks()
  }

  private fun observeAchievementUnlocks() {
    viewModelScope.launch {
      uiState.collect { state ->
        if (state !is GameUiState.Ready || state.inventoryState.isLoading) return@collect
        val currentIds = state.achievementSummary.unlockedAchievements.map { it.id }.toSet()
        val previousIds = knownUnlockedAchievementIds
        if (previousIds == null) {
          knownUnlockedAchievementIds = currentIds
          return@collect
        }
        val newlyUnlocked = AchievementCatalog.findNewlyUnlocked(previousIds, state.achievementSummary)
        if (newlyUnlocked.isNotEmpty()) {
          knownUnlockedAchievementIds = previousIds + newlyUnlocked.map { it.id }
          soundController.playSfx(GameSfx.LEVEL_UP)
          _transientState.update { it.copy(newlyUnlockedBadge = newlyUnlocked.last()) }
        }
      }
    }
  }

  fun ensureStarterInitialized() {
    viewModelScope.launch {
      repository
        .ensureStarterProfile(defaultHeroName)
        .onFailure { error ->
          Log.w(TAG, "Failed to ensure starter profile", error)
          _transientState.update {
            it.copy(
              banner =
                FeedbackBanner(
                  title = "Koneksi Awan Terkendala",
                  message = error.localizedMessage ?: "Gagal menyiapkan data awal petualang.",
                  isWarning = true,
                )
            )
          }
        }
    }
  }

  fun selectTab(tab: AdventureTab) {
    _transientState.update { it.copy(activeTab = tab) }
  }

  fun selectInventoryCategory(category: InventoryCategory) {
    _inventoryMeta.update { it.copy(selectedCategory = category) }
  }

  fun dismissCollectedItemNotice() {
    _inventoryMeta.update { it.copy(lastCollectedItem = null) }
  }

  fun dismissAchievementNotice() {
    _transientState.update { it.copy(newlyUnlockedBadge = null) }
  }

  fun onAudioResume() = soundController.resumeBgm()

  fun onAudioPause() = soundController.pauseBgm()

  fun toggleBgm() = soundController.toggleBgm()

  fun toggleSfx() = soundController.toggleSfx()

  fun setBgmVolume(volume: Float) = soundController.setBgmVolume(volume)

  fun setSfxVolume(volume: Float) = soundController.setSfxVolume(volume)

  fun playSfx(effect: GameSfx) = soundController.playSfx(effect)

  override fun onCleared() {
    soundController.stopAndRelease()
    super.onCleared()
  }

  /**
   * Menyimpan seluruh progres pemain ke Firestore secara terpadu:
   * data lokasi saat ini, status inventaris, dan poin statistik pemain.
   */
  fun savePlayerProgress() {
    val ready = uiState.value as? GameUiState.Ready ?: return
    savePlayerProgress(profile = ready.profile, inventoryState = ready.inventoryState)
  }

  fun savePlayerProgress(profile: PlayerProfile, inventoryState: InventoryState) {
    if (_transientState.value.isMutating) return
    val currentNode = resolveStoryNode(profile.currentNodeId)
    viewModelScope.launch {
      _transientState.update { it.copy(isMutating = true) }
      repository
        .savePlayerProgress(
          profile = profile,
          inventoryState = inventoryState,
          pruneRemovedItems = !inventoryState.isLoading,
        )
        .onSuccess {
          val now = System.currentTimeMillis()
          _transientState.update {
            it.copy(
              isMutating = false,
              lastSavedAtMillis = now,
              banner =
                FeedbackBanner(
                  title = "Progres Tersimpan ke Cloud!",
                  message =
                    "Lokasi (${currentNode.title}), ${inventoryState.totalItemQuantity} item tas, & statistik Lv.${profile.level} aman di Firestore.",
                  isCelebration = true,
                ),
            )
          }
        }
        .onFailure { error ->
          _transientState.update {
            it.copy(
              isMutating = false,
              banner =
                FeedbackBanner(
                  title = "Gagal Menyimpan Progres",
                  message = error.localizedMessage ?: "Periksa koneksi internetmu lalu coba lagi.",
                  isWarning = true,
                ),
            )
          }
        }
    }
  }

  fun collectStoryItem(reward: ItemReward, sourceLabel: String = "Perjalanan") {
    if (_transientState.value.isMutating) return
    viewModelScope.launch {
      _transientState.update { it.copy(isMutating = true) }
      repository
        .addOrIncrementItem(
          itemKey = reward.itemKey,
          name = reward.name,
          description = reward.description,
          category = reward.category,
          quantityDelta = reward.quantity,
          healAmount = reward.healAmount,
          energyAmount = reward.energyAmount,
          iconKey = reward.iconKey,
        )
        .onSuccess {
          val event =
            CollectedItemEvent(
              itemKey = reward.itemKey,
              itemName = reward.name,
              quantityAdded = reward.quantity,
              iconKey = reward.iconKey,
              sourceLabel = sourceLabel,
            )
          _inventoryMeta.update { it.copy(lastCollectedItem = event) }
          soundController.playSfx(GameSfx.ITEM_COLLECTED)
          _transientState.update {
            it.copy(
              isMutating = false,
              lastSavedAtMillis = System.currentTimeMillis(),
              banner =
                FeedbackBanner(
                  title = "Item Dikumpulkan!",
                  message = "+${reward.quantity} ${reward.name} tersimpan ke dalam Tas Petualang.",
                  isCelebration = true,
                ),
            )
          }
        }
        .onFailure { error ->
          _transientState.update {
            it.copy(
              isMutating = false,
              banner =
                FeedbackBanner(
                  title = "Gagal Menyimpan Item",
                  message = error.localizedMessage ?: "Coba beberapa saat lagi.",
                  isWarning = true,
                ),
            )
          }
        }
    }
  }

  fun togglePipTip() {
    _transientState.update { it.copy(pipTipExpanded = !it.pipTipExpanded) }
  }

  fun clearBanner() {
    _transientState.update { it.copy(banner = null) }
  }

  fun syncNodeChallenges(node: StoryNode) {
    _transientState.update { state ->
      val nextRiddle =
        if (state.riddle.nodeId == node.id) {
          state.riddle
        } else {
          RiddleUiState(nodeId = node.id)
        }
      val nextEncounter =
        if (state.encounter.nodeId == node.id) {
          state.encounter
        } else {
          val enc = node.encounter
          EncounterUiState(
            nodeId = node.id,
            enemyHp = enc?.maxHp ?: 0,
            enemyMaxHp = enc?.maxHp ?: 0,
            won = enc == null,
            battleLog = enc?.introLine.orEmpty(),
          )
        }
      state.copy(riddle = nextRiddle, encounter = nextEncounter)
    }
  }

  fun toggleRiddleHint() {
    _transientState.update { state ->
      state.copy(riddle = state.riddle.copy(hintVisible = !state.riddle.hintVisible))
    }
  }

  fun submitRiddleOption(node: StoryNode, optionIndex: Int) {
    val riddle = node.riddle ?: return
    val isCorrect = optionIndex == riddle.correctIndex
    soundController.playSfx(GameSfx.CHOICE_SELECT)
    _transientState.update { state ->
      state.copy(
        riddle =
          state.riddle.copy(
            nodeId = node.id,
            selectedOptionIndex = optionIndex,
            solved = isCorrect,
            statusMessage = if (isCorrect) riddle.successText else riddle.wrongHintText,
          )
      )
    }
  }

  fun attackFriendlyEncounter(profile: PlayerProfile, node: StoryNode) {
    val encounter = node.encounter ?: return
    if (_transientState.value.isMutating) return

    val currentEnc =
      if (_transientState.value.encounter.nodeId == node.id) _transientState.value.encounter
      else
        EncounterUiState(
          nodeId = node.id,
          enemyHp = encounter.maxHp,
          enemyMaxHp = encounter.maxHp,
          won = false,
          battleLog = encounter.introLine,
        )
    if (currentEnc.won) return

    val playerDamage = (5 + profile.level.toInt() * 2).coerceAtLeast(6)
    val remainingEnemyHp = (currentEnc.enemyHp - playerDamage).coerceAtLeast(0)

    if (remainingEnemyHp == 0) {
      _transientState.update {
        it.copy(
          encounter =
            currentEnc.copy(
              enemyHp = 0,
              won = true,
              battleLog = "Seranganmu ($playerDamage dmg) berhasil! ${encounter.victoryText}",
            )
        )
      }
      return
    }

    val counterDamage = encounter.enemyDamage.toLong()
    val rawNextHp = profile.hp - counterDamage
    val rescuedByPip = rawNextHp <= 0L
    val finalHp = if (rescuedByPip) profile.maxHp else rawNextHp

    viewModelScope.launch {
      _transientState.update { it.copy(isMutating = true) }
      repository
        .saveProgressUpdate(
          level = profile.level,
          xp = profile.xp,
          hp = finalHp,
          maxHp = profile.maxHp,
          energy = profile.energy,
          maxEnergy = profile.maxEnergy,
          coins = profile.coins,
          currentRegionId = profile.currentRegionId,
          currentNodeId = profile.currentNodeId,
          visitedNodeIds = profile.visitedNodeIds,
          completedRegionIds = profile.completedRegionIds,
          tutorialDone = profile.tutorialDone,
        )
        .onSuccess {
          val logText =
            if (rescuedByPip) {
              "Kamu memberi $playerDamage dmg (sisa HP ${encounter.enemyName}: $remainingEnemyHp). Pip memakai Sayap Pelindung agar HP-mu pulih penuh!"
            } else {
              "Kamu memberi $playerDamage dmg! ${encounter.enemyName} membalas lembut -$counterDamage HP."
            }
          _transientState.update {
            it.copy(
              isMutating = false,
              encounter = currentEnc.copy(enemyHp = remainingEnemyHp, battleLog = logText),
              banner =
                if (rescuedByPip) {
                  FeedbackBanner(
                    title = "Sayap Pelindung Pip!",
                    message = "Karena ini mode pemula, Pip langsung memulihkan seluruh HP-mu.",
                    isCelebration = true,
                  )
                } else it.banner,
            )
          }
        }
        .onFailure { error ->
          _transientState.update {
            it.copy(
              isMutating = false,
              banner =
                FeedbackBanner(
                  title = "Gagal Menyimpan Aksi",
                  message = error.localizedMessage ?: "Coba tekan lagi sebentar.",
                  isWarning = true,
                ),
            )
          }
        }
    }
  }

  fun negotiatePeaceEncounter(profile: PlayerProfile, node: StoryNode) {
    val encounter = node.encounter ?: return
    if (_transientState.value.isMutating) return
    val cost = encounter.peaceCostEnergy.toLong()
    if (profile.energy < cost) {
      _transientState.update {
        it.copy(
          banner =
            FeedbackBanner(
              title = "Energi Kurang",
              message =
                "Butuh $cost Energi untuk menyapa ramah. Tekan tombol Kemah untuk istirahat gratis!",
              isWarning = true,
            )
        )
      }
      return
    }

    viewModelScope.launch {
      _transientState.update { it.copy(isMutating = true) }
      repository
        .saveProgressUpdate(
          level = profile.level,
          xp = profile.xp,
          hp = profile.hp,
          maxHp = profile.maxHp,
          energy = (profile.energy - cost).coerceAtLeast(0L),
          maxEnergy = profile.maxEnergy,
          coins = profile.coins,
          currentRegionId = profile.currentRegionId,
          currentNodeId = profile.currentNodeId,
          visitedNodeIds = profile.visitedNodeIds,
          completedRegionIds = profile.completedRegionIds,
          tutorialDone = profile.tutorialDone,
        )
        .onSuccess {
          _transientState.update {
            it.copy(
              isMutating = false,
              encounter =
                it.encounter.copy(
                  nodeId = node.id,
                  enemyHp = 0,
                  enemyMaxHp = encounter.maxHp,
                  won = true,
                  battleLog = "Persahabatan terjalin! ${encounter.victoryText}",
                ),
            )
          }
        }
        .onFailure { error ->
          _transientState.update {
            it.copy(
              isMutating = false,
              banner =
                FeedbackBanner(
                  title = "Gagal Menyimpan",
                  message = error.localizedMessage ?: "Terjadi kesalahan jaringan.",
                  isWarning = true,
                ),
            )
          }
        }
    }
  }

  fun chooseStoryOption(profile: PlayerProfile, currentNode: StoryNode, choice: StoryChoice) {
    if (_transientState.value.isMutating) return
    when (
      val transition =
        GameProgressionEngine.applyStoryChoice(
          profile = profile,
          currentNode = currentNode,
          choice = choice,
          resolveNode = ::resolveStoryNode,
        )
    ) {
      is TransitionOutcome.InsufficientEnergy -> {
        _transientState.update {
          it.copy(
            banner =
              FeedbackBanner(
                title = "Energi Habis",
                message =
                  "Butuh ${transition.requiredEnergy} Energi. Ketuk tombol Kemah di atas atau makan camilan dari tab Tas.",
                isWarning = true,
              )
          )
        }
      }

      is TransitionOutcome.Success -> {
        soundController.playSfx(GameSfx.CHOICE_SELECT)
        val next = transition.updatedProfile
        val actionResult = transition.actionResult
        viewModelScope.launch {
          _transientState.update { it.copy(isMutating = true) }

          val saveResult =
            repository.saveProgressUpdate(
              level = next.level,
              xp = next.xp,
              hp = next.hp,
              maxHp = next.maxHp,
              energy = next.energy,
              maxEnergy = next.maxEnergy,
              coins = next.coins,
              currentRegionId = next.currentRegionId,
              currentNodeId = next.currentNodeId,
              visitedNodeIds = next.visitedNodeIds,
              completedRegionIds = next.completedRegionIds,
              tutorialDone = next.tutorialDone,
            )

          if (saveResult.isFailure) {
            _transientState.update {
              it.copy(
                isMutating = false,
                banner =
                  FeedbackBanner(
                    title = "Progres Belum Tersimpan",
                    message =
                      saveResult.exceptionOrNull()?.localizedMessage ?: "Coba lagi sebentar.",
                    isWarning = true,
                  ),
              )
            }
            return@launch
          }

          var itemCollectionWarning: String? = null
          actionResult.itemReward?.let { reward ->
            repository
              .addOrIncrementItem(
                itemKey = reward.itemKey,
                name = reward.name,
                description = reward.description,
                category = reward.category,
                quantityDelta = reward.quantity,
                healAmount = reward.healAmount,
                energyAmount = reward.energyAmount,
                iconKey = reward.iconKey,
              )
              .onSuccess {
                _inventoryMeta.update {
                  it.copy(
                    lastCollectedItem =
                      CollectedItemEvent(
                        itemKey = reward.itemKey,
                        itemName = reward.name,
                        quantityAdded = reward.quantity,
                        iconKey = reward.iconKey,
                        sourceLabel = currentNode.title,
                      )
                  )
                }
              }
              .onFailure { error ->
                Log.w(TAG, "Failed to collect story item ${reward.itemKey}", error)
                itemCollectionWarning = " (Item ${reward.name} gagal disimpan ke awan.)"
              }
          }

          if (!actionResult.journalTitle.isNullOrBlank() && !actionResult.journalSummary.isNullOrBlank()) {
            val entryId = "${choice.id}_${System.currentTimeMillis()}"
            repository.addJournalEntry(
              entryId = entryId,
              title = actionResult.journalTitle,
              summary = actionResult.journalSummary,
              regionId = currentNode.regionId,
              rewardXp = actionResult.xpReward.toLong(),
            )
          }

          val collectedReward = if (itemCollectionWarning == null) actionResult.itemReward else null
          if (collectedReward != null) {
            soundController.playSfx(GameSfx.ITEM_COLLECTED)
          } else if (transition.leveledUp || transition.regionCompleted) {
            soundController.playSfx(GameSfx.LEVEL_UP)
          }
          val bannerTitle =
            when {
              transition.leveledUp -> "Naik ke Level ${next.level}!"
              transition.regionCompleted -> "Wilayah Tuntas!"
              collectedReward != null ->
                "+${collectedReward.quantity} ${collectedReward.name} & +${actionResult.xpReward} XP"
              else -> "+${actionResult.xpReward} XP & +${actionResult.coinReward} Koin"
            }

          val baseMessage =
            if (transition.leveledUp) {
              "${actionResult.message} Max HP naik ke ${next.maxHp} & Energi terisi penuh!"
            } else {
              actionResult.message
            }
          val bannerBody = baseMessage + (itemCollectionWarning ?: "")

          _transientState.update {
            it.copy(
              isMutating = false,
              lastSavedAtMillis = System.currentTimeMillis(),
              banner =
                FeedbackBanner(
                  title = bannerTitle,
                  message = bannerBody,
                  isCelebration =
                    transition.leveledUp || transition.regionCompleted || collectedReward != null,
                ),
            )
          }
          syncNodeChallenges(transition.nextNode)
        }
      }
    }
  }

  fun restAtCamp(profile: PlayerProfile) {
    if (_transientState.value.isMutating) return
    viewModelScope.launch {
      _transientState.update { it.copy(isMutating = true) }
      repository
        .saveProgressUpdate(
          level = profile.level,
          xp = profile.xp,
          hp = profile.maxHp,
          maxHp = profile.maxHp,
          energy = profile.maxEnergy,
          maxEnergy = profile.maxEnergy,
          coins = profile.coins,
          currentRegionId = profile.currentRegionId,
          currentNodeId = profile.currentNodeId,
          visitedNodeIds = profile.visitedNodeIds,
          completedRegionIds = profile.completedRegionIds,
          tutorialDone = profile.tutorialDone,
        )
        .onSuccess {
          _transientState.update {
            it.copy(
              isMutating = false,
              lastSavedAtMillis = System.currentTimeMillis(),
              banner =
                FeedbackBanner(
                  title = "Istirahat di Kemah",
                  message = "HP (${profile.maxHp}) dan Energi (${profile.maxEnergy}) kembali penuh!",
                  isCelebration = true,
                ),
            )
          }
        }
        .onFailure { error ->
          _transientState.update {
            it.copy(
              isMutating = false,
              banner =
                FeedbackBanner(
                  title = "Gagal Beristirahat",
                  message = error.localizedMessage ?: "Periksa koneksi internetmu.",
                  isWarning = true,
                ),
            )
          }
        }
    }
  }

  fun travelToRegion(profile: PlayerProfile, regionId: String) {
    if (_transientState.value.isMutating) return
    if (!AdventureWorld.isRegionUnlocked(regionId, profile.completedRegionIds)) {
      _transientState.update {
        it.copy(
          banner =
            FeedbackBanner(
              title = "Wilayah Masih Terkunci",
              message = "Selesaikan wilayah sebelumnya terlebih dahulu untuk membuka jalur ini!",
              isWarning = true,
            )
        )
      }
      return
    }

    val region = AdventureWorld.getRegion(regionId)
    val startNode = resolveStoryNode(region.startNodeId)
    viewModelScope.launch {
      _transientState.update { it.copy(isMutating = true) }
      repository
        .saveProgressUpdate(
          level = profile.level,
          xp = profile.xp,
          hp = profile.hp,
          maxHp = profile.maxHp,
          energy = profile.energy,
          maxEnergy = profile.maxEnergy,
          coins = profile.coins,
          currentRegionId = region.id,
          currentNodeId = region.startNodeId,
          visitedNodeIds = (profile.visitedNodeIds + region.startNodeId).distinct(),
          completedRegionIds = profile.completedRegionIds,
          tutorialDone = true,
        )
        .onSuccess {
          _transientState.update {
            it.copy(
              isMutating = false,
              lastSavedAtMillis = System.currentTimeMillis(),
              activeTab = AdventureTab.PETUALANGAN,
              banner =
                FeedbackBanner(
                  title = "Tiba di ${region.name}",
                  message = "Selamat menjelajahi ${region.name}!",
                  isCelebration = true,
                ),
            )
          }
          syncNodeChallenges(startNode)
        }
        .onFailure { error ->
          _transientState.update {
            it.copy(
              isMutating = false,
              banner =
                FeedbackBanner(
                  title = "Gagal Berpindah Wilayah",
                  message = error.localizedMessage ?: "Coba beberapa saat lagi.",
                  isWarning = true,
                ),
            )
          }
        }
    }
  }

  fun useInventoryItem(profile: PlayerProfile, item: InventoryItem) {
    if (_transientState.value.isMutating) return
    if (item.healAmount <= 0L && item.energyAmount <= 0L) {
      _transientState.update {
        it.copy(
          banner =
            FeedbackBanner(
              title = item.name,
              message = "Item koleksi ini menjadi penanda pencapaian petualanganmu.",
            )
        )
      }
      return
    }

    viewModelScope.launch {
      _transientState.update { it.copy(isMutating = true) }
      val nextHp = (profile.hp + item.healAmount).coerceIn(0L, profile.maxHp)
      val nextEnergy = (profile.energy + item.energyAmount).coerceIn(0L, profile.maxEnergy)

      val consumeRes = repository.consumeItem(item)
      if (consumeRes.isFailure) {
        _transientState.update {
          it.copy(
            isMutating = false,
            banner =
              FeedbackBanner(
                title = "Gagal Memakai Item",
                message = consumeRes.exceptionOrNull()?.localizedMessage ?: "Coba lagi.",
                isWarning = true,
              ),
          )
        }
        return@launch
      }

      repository
        .saveProgressUpdate(
          level = profile.level,
          xp = profile.xp,
          hp = nextHp,
          maxHp = profile.maxHp,
          energy = nextEnergy,
          maxEnergy = profile.maxEnergy,
          coins = profile.coins,
          currentRegionId = profile.currentRegionId,
          currentNodeId = profile.currentNodeId,
          visitedNodeIds = profile.visitedNodeIds,
          completedRegionIds = profile.completedRegionIds,
          tutorialDone = profile.tutorialDone,
        )
        .onSuccess {
          _transientState.update {
            it.copy(
              isMutating = false,
              lastSavedAtMillis = System.currentTimeMillis(),
              banner =
                FeedbackBanner(
                  title = "${item.name} Digunakan",
                  message =
                    "HP menjadi $nextHp/${profile.maxHp} • Energi $nextEnergy/${profile.maxEnergy}.",
                  isCelebration = true,
                ),
            )
          }
        }
        .onFailure { error ->
          _transientState.update {
            it.copy(
              isMutating = false,
              banner =
                FeedbackBanner(
                  title = "Gagal Memperbarui Status",
                  message = error.localizedMessage ?: "Coba lagi.",
                  isWarning = true,
                ),
            )
          }
        }
    }
  }

  fun buyShopItem(profile: PlayerProfile, shopItem: ShopItem) {
    if (_transientState.value.isMutating) return
    if (profile.coins < shopItem.priceCoins) {
      _transientState.update {
        it.copy(
          banner =
            FeedbackBanner(
              title = "Koin Belum Cukup",
              message = "Kamu butuh ${shopItem.priceCoins} Koin untuk membeli ${shopItem.name}.",
              isWarning = true,
            )
        )
      }
      return
    }

    viewModelScope.launch {
      _transientState.update { it.copy(isMutating = true) }
      val nextCoins = profile.coins - shopItem.priceCoins
      val saveRes =
        repository.saveProgressUpdate(
          level = profile.level,
          xp = profile.xp,
          hp = profile.hp,
          maxHp = profile.maxHp,
          energy = profile.energy,
          maxEnergy = profile.maxEnergy,
          coins = nextCoins,
          currentRegionId = profile.currentRegionId,
          currentNodeId = profile.currentNodeId,
          visitedNodeIds = profile.visitedNodeIds,
          completedRegionIds = profile.completedRegionIds,
          tutorialDone = profile.tutorialDone,
        )

      if (saveRes.isFailure) {
        _transientState.update {
          it.copy(
            isMutating = false,
            banner =
              FeedbackBanner(
                title = "Pembelian Gagal",
                message = saveRes.exceptionOrNull()?.localizedMessage ?: "Coba lagi.",
                isWarning = true,
              ),
          )
        }
        return@launch
      }

      repository
        .addOrIncrementItem(
          itemKey = shopItem.itemKey,
          name = shopItem.name,
          description = shopItem.description,
          category = shopItem.category,
          quantityDelta = 1L,
          healAmount = shopItem.healAmount,
          energyAmount = shopItem.energyAmount,
          iconKey = shopItem.iconKey,
        )
        .onSuccess {
          soundController.playSfx(GameSfx.ITEM_COLLECTED)
          _transientState.update {
            it.copy(
              isMutating = false,
              lastSavedAtMillis = System.currentTimeMillis(),
              banner =
                FeedbackBanner(
                  title = "Berhasil Membeli!",
                  message = "${shopItem.name} masuk ke dalam Tas Petualangmu.",
                  isCelebration = true,
                ),
            )
          }
        }
        .onFailure { error ->
          _transientState.update {
            it.copy(
              isMutating = false,
              banner =
                FeedbackBanner(
                  title = "Item Belum Masuk Tas",
                  message = error.localizedMessage ?: "Coba lagi.",
                  isWarning = true,
                ),
            )
          }
        }
    }
  }

  fun updateHeroIdentity(heroName: String, avatarEmoji: String, tutorialDone: Boolean) {
    if (_transientState.value.isMutating) return
    viewModelScope.launch {
      _transientState.update { it.copy(isMutating = true) }
      repository
        .updateHeroIdentity(heroName, avatarEmoji, tutorialDone)
        .onSuccess {
          _transientState.update {
            it.copy(
              isMutating = false,
              banner =
                FeedbackBanner(
                  title = "Profil Diperbarui",
                  message = "Identitas petualang $avatarEmoji $heroName tersimpan di awan.",
                  isCelebration = true,
                ),
            )
          }
        }
        .onFailure { error ->
          _transientState.update {
            it.copy(
              isMutating = false,
              banner =
                FeedbackBanner(
                  title = "Gagal Mengubah Profil",
                  message = error.localizedMessage ?: "Pastikan nama minimal 2 huruf.",
                  isWarning = true,
                ),
            )
          }
        }
    }
  }

  fun resetAdventure() {
    if (_transientState.value.isMutating) return
    viewModelScope.launch {
      _transientState.update { it.copy(isMutating = true) }
      repository
        .resetAdventure()
        .onSuccess {
          val startNode = resolveStoryNode("forest_start")
          knownUnlockedAchievementIds = emptySet()
          _inventoryMeta.value = InventoryUiMeta()
          _transientState.update {
            it.copy(
              isMutating = false,
              activeTab = AdventureTab.PETUALANGAN,
              pipTipExpanded = true,
              newlyUnlockedBadge = null,
              banner =
                FeedbackBanner(
                  title = "Petualangan Dimulai Ulang",
                  message = "Semua siap untuk ekspedisi baru dari Gerbang Hutan Bisikan!",
                  isCelebration = true,
                ),
            )
          }
          syncNodeChallenges(startNode)
        }
        .onFailure { error ->
          _transientState.update {
            it.copy(
              isMutating = false,
              banner =
                FeedbackBanner(
                  title = "Gagal Mengulang Petualangan",
                  message = error.localizedMessage ?: "Coba beberapa saat lagi.",
                  isWarning = true,
                ),
            )
          }
        }
    }
  }

  companion object {
    private const val TAG = "GameViewModel"
    private const val STOP_TIMEOUT_MILLIS = 5000L

    /**
     * Contoh data awal cerita petualangan pemula menggunakan model [StoryNode] (naskah),
     * [StoryChoice] (daftar pilihan aksi), dan [StoryActionResult] (hasil aksi).
     */
    fun createInitialStorySample(): List<StoryNode> =
      listOf(
        StoryNode(
          id = "forest_start",
          regionId = "forest",
          title = "Gerbang Hutan Bisikan",
          speaker = "Pip si Burung Hantu",
          pipTip =
            "Tips Pemula: Pilih tombol di bawah untuk melangkah. Pilihan berlabel [Aman] tidak memakai Energi!",
          narrative =
            "Cahaya pagi menembus dedaunan hijau. Pip hinggap di papan kayu tua sambil mengepakkan sayap kecilnya.\n\n\"Selamat datang, petualang baru! Di depan ada dua jalur menuju Sungai Berkilau. Kamu ingin berjalan santai atau mencari bekal dulu?\"",
          choices =
            listOf(
              StoryChoice(
                id = "forest_start_path",
                label = "Ikuti Jalan Setapak Berbatu",
                badge = "Aman • +10 XP",
                actionResult =
                  StoryActionResult(
                    nextNodeId = "forest_stream",
                    message =
                      "Kamu menyusuri jalan setapak yang rapi dan menemukan 8 koin emas di tepi batu!",
                    energyCost = 0,
                    xpReward = 10,
                    coinReward = 8,
                  ),
              ),
              StoryChoice(
                id = "forest_start_berry",
                label = "Cari Beri di Semak Berkilau",
                badge = "Dapat Item • -1 Energi",
                actionResult =
                  StoryActionResult(
                    nextNodeId = "forest_stream",
                    message =
                      "Wah! Kamu memetik Beri Manis Rimba yang segar dan menyimpannya ke tas.",
                    energyCost = 1,
                    xpReward = 15,
                    coinReward = 5,
                    itemReward =
                      ItemReward(
                        itemKey = "berry_sweet",
                        name = "Beri Manis Rimba",
                        description = "Camilan segar pemula yang memulihkan 10 HP dan 4 Energi.",
                        category = "snack",
                        quantity = 1L,
                        healAmount = 10L,
                        energyAmount = 4L,
                        iconKey = "snack",
                      ),
                  ),
              ),
            ),
        ),
        StoryNode(
          id = "forest_stream",
          regionId = "forest",
          title = "Tepi Sungai Berkilau",
          speaker = "Penjaga Peri Air",
          pipTip =
            "Tips Pemula: Teka-teki di game ini selalu punya petunjuk gratis. Jangan takut mencoba menjawab!",
          narrative =
            "Air sungai jernih mengalir memantulkan warna langit. Di tengah jembatan kayu, seekor peri air mungil melambai ramah.\n\n\"Kalau ingin lewat dengan hadiah koin, jawab teka-teki sederhanaku! Atau bantu perbaiki papan jembatan untuk menemukan barang rahasia.\"",
          riddle =
            RiddleChallenge(
              question = "Aku selalu berlari, tidak pernah lelah, dan tidak punya kaki. Siapakah aku?",
              hint = "Lihat sesuatu yang sedang mengalir tepat di bawah jembatan kayu ini!",
              options = listOf("Sungai", "Angin", "Bayangan"),
              correctIndex = 0,
              successText = "Tepat sekali! Peri air tersenyum dan memberimu hadiah.",
              wrongHintText = "Belum tepat, coba perhatikan air yang terus mengalir di bawahmu!",
            ),
          choices =
            listOf(
              StoryChoice(
                id = "forest_stream_riddle_win",
                label = "Lanjutkan Setelah Teka-Teki",
                badge = "Hadiah • +20 XP",
                actionResult =
                  StoryActionResult(
                    nextNodeId = "forest_guardian",
                    message =
                      "Peri air memberimu 15 koin emas dan membuka jalan menuju padang rumput!",
                    xpReward = 20,
                    coinReward = 15,
                    journalTitle = "Menjawab Teka-Teki Peri Air",
                    journalSummary = "Berhasil memecahkan teka-teki Sungai Berkilau di Hutan Bisikan.",
                  ),
              ),
              StoryChoice(
                id = "forest_stream_bridge",
                label = "Perbaiki Papan Jembatan",
                badge = "Dapat Kunci • -2 Energi",
                actionResult =
                  StoryActionResult(
                    nextNodeId = "forest_guardian",
                    message = "Di balik papan kayu kamu menemukan Kunci Lumut berkilau!",
                    energyCost = 2,
                    xpReward = 20,
                    coinReward = 10,
                    itemReward =
                      ItemReward(
                        itemKey = "key_moss",
                        name = "Kunci Lumut",
                        description = "Kunci kuno berukir daun dari jembatan Sungai Berkilau.",
                        category = "key",
                        quantity = 1L,
                        iconKey = "key",
                      ),
                    journalTitle = "Memperbaiki Jembatan Sungai",
                    journalSummary = "Membantu peri air dan menemukan Kunci Lumut tersembunyi.",
                  ),
              ),
            ),
        ),
      )
  }
}
