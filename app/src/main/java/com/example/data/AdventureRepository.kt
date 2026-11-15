package com.example.data

import android.content.Context
import com.example.R
import com.example.game.InventoryState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AdventureRepository(private val db: FirebaseFirestore) {

  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
  }

  fun observePlayerProfile(userId: String): Flow<PlayerProfile?> {
    val docRef = db.collection("users").document(userId)
    return docRef
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(
            PlayerProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        } else {
          null
        }
      }
      .catch { error ->
        logFlowError(error, OperationType.GET, docRef.path, userId)
        throw error
      }
  }

  fun observeInventory(userId: String): Flow<List<InventoryItem>> {
    val colRef = db.collection("users").document(userId).collection("inventory")
    return colRef
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          InventoryItem::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      }
      .catch { error ->
        logFlowError(error, OperationType.LIST, colRef.path, userId)
        throw error
      }
  }

  fun observeJournal(userId: String): Flow<List<JournalEntry>> {
    val colRef = db.collection("users").document(userId).collection("journal")
    return colRef
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(JournalEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        logFlowError(error, OperationType.LIST, colRef.path, userId)
        throw error
      }
  }

  suspend fun ensureStarterProfile(defaultHeroName: String): Result<PlayerProfile> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        val loaded =
          existing.toObject(
            PlayerProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        if (loaded != null) return@runCatching loaded
      }

      val cleanName =
        defaultHeroName.trim().take(24).ifBlank { "Petualang Baru" }.let {
          if (it.length < 2) "Petualang" else it
        }

      val payload =
        mapOf(
          "userId" to uid,
          "heroName" to cleanName,
          "avatarEmoji" to "🧭",
          "level" to 1L,
          "xp" to 0L,
          "hp" to 30L,
          "maxHp" to 30L,
          "energy" to 15L,
          "maxEnergy" to 15L,
          "coins" to 25L,
          "currentRegionId" to "forest",
          "currentNodeId" to "forest_start",
          "visitedNodeIds" to listOf("forest_start"),
          "completedRegionIds" to emptyList<String>(),
          "tutorialDone" to false,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()

      // Seed starter healing berry in bag
      val starterItemRef =
        db.collection("users").document(uid).collection("inventory").document("berry_sweet")
      starterItemRef
        .set(
          mapOf(
            "itemKey" to "berry_sweet",
            "name" to "Beri Manis Rimba",
            "description" to "Camilan segar pemula yang memulihkan 10 HP dan 4 Energi.",
            "category" to "snack",
            "quantity" to 2L,
            "healAmount" to 10L,
            "energyAmount" to 4L,
            "iconKey" to "snack",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      val starterJournalRef =
        db.collection("users").document(uid).collection("journal").document("journey_begin")
      starterJournalRef
        .set(
          mapOf(
            "title" to "Memulai Ekspedisi Pemula",
            "summary" to "Membuka kompas emas di gerbang Hutan Bisikan bersama Pip.",
            "regionId" to "forest",
            "rewardXp" to 10L,
            "createdAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      docRef
        .get()
        .await()
        .toObject(
          PlayerProfile::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
        ?: PlayerProfile(id = uid, userId = uid, heroName = cleanName)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun updateHeroIdentity(
    heroName: String,
    avatarEmoji: String,
    tutorialDone: Boolean,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val cleanName = heroName.trim().take(24)
    require(cleanName.length >= 2) { "Nama petualang minimal 2 huruf." }
    try {
      docRef
        .update(
          mapOf(
            "heroName" to cleanName,
            "avatarEmoji" to avatarEmoji,
            "tutorialDone" to tutorialDone,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun saveProgressUpdate(
    level: Long,
    xp: Long,
    hp: Long,
    maxHp: Long,
    energy: Long,
    maxEnergy: Long,
    coins: Long,
    currentRegionId: String,
    currentNodeId: String,
    visitedNodeIds: List<String>,
    completedRegionIds: List<String>,
    tutorialDone: Boolean,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      docRef
        .update(
          mapOf(
            "level" to level.coerceIn(1L, 99L),
            "xp" to xp.coerceIn(0L, 100000L),
            "hp" to hp.coerceIn(0L, maxHp),
            "maxHp" to maxHp.coerceIn(10L, 500L),
            "energy" to energy.coerceIn(0L, maxEnergy),
            "maxEnergy" to maxEnergy.coerceIn(5L, 200L),
            "coins" to coins.coerceIn(0L, 1000000L),
            "currentRegionId" to currentRegionId,
            "currentNodeId" to currentNodeId,
            "visitedNodeIds" to visitedNodeIds.distinct().take(50),
            "completedRegionIds" to completedRegionIds.distinct().take(10),
            "tutorialDone" to tutorialDone,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  /**
   * Menyimpan seluruh progres pemain ke Firestore dalam satu WriteBatch atomik:
   * - Data lokasi saat ini (currentRegionId, currentNodeId, visitedNodeIds, completedRegionIds)
   * - Status inventaris (subkoleksi users/{uid}/inventory)
   * - Poin statistik pemain (level, xp, hp, maxHp, energy, maxEnergy, coins)
   */
  suspend fun savePlayerProgress(
    profile: PlayerProfile,
    inventoryState: InventoryState,
    pruneRemovedItems: Boolean = !inventoryState.isLoading,
  ): Result<Unit> =
    savePlayerProgress(
      snapshot = profile.toProgressSnapshot(inventoryState.items),
      pruneRemovedItems = pruneRemovedItems,
    )

  suspend fun savePlayerProgress(
    snapshot: PlayerProgressSnapshot,
    pruneRemovedItems: Boolean = true,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val invColRef = userRef.collection("inventory")

    val safeMaxHp = snapshot.maxHp.coerceIn(10L, 500L)
    val safeHp = snapshot.hp.coerceIn(0L, safeMaxHp)
    val safeMaxEnergy = snapshot.maxEnergy.coerceIn(5L, 200L)
    val safeEnergy = snapshot.energy.coerceIn(0L, safeMaxEnergy)
    val safeLevel = snapshot.level.coerceIn(1L, 99L)
    val safeXp = snapshot.xp.coerceIn(0L, 100000L)
    val safeCoins = snapshot.coins.coerceIn(0L, 1000000L)
    val safeRegionId =
      if (snapshot.currentRegionId in VALID_REGIONS) snapshot.currentRegionId else "forest"
    val safeNodeId = snapshot.currentNodeId.trim().take(64).ifBlank { "forest_start" }
    val safeVisitedNodes =
      (snapshot.visitedNodeIds + safeNodeId)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .take(50)
    val safeCompletedRegions =
      snapshot.completedRegionIds
        .map { it.trim() }
        .filter { it in VALID_REGIONS }
        .distinct()
        .take(10)

    try {
      val userSnapshot = userRef.get().await()
      val existingInvSnapshot = invColRef.get().await()
      val existingInvIds = existingInvSnapshot.documents.map { it.id }.toSet()

      val batch = db.batch()

      if (userSnapshot.exists()) {
        batch.update(
          userRef,
          mapOf(
            "level" to safeLevel,
            "xp" to safeXp,
            "hp" to safeHp,
            "maxHp" to safeMaxHp,
            "energy" to safeEnergy,
            "maxEnergy" to safeMaxEnergy,
            "coins" to safeCoins,
            "currentRegionId" to safeRegionId,
            "currentNodeId" to safeNodeId,
            "visitedNodeIds" to safeVisitedNodes,
            "completedRegionIds" to safeCompletedRegions,
            "tutorialDone" to snapshot.tutorialDone,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      } else {
        val cleanName =
          snapshot.heroName.trim().take(24).ifBlank { "Petualang Baru" }.let {
            if (it.length < 2) "Petualang" else it
          }
        val cleanAvatar = snapshot.avatarEmoji.trim().take(12).ifBlank { "🧭" }
        batch.set(
          userRef,
          mapOf(
            "userId" to uid,
            "heroName" to cleanName,
            "avatarEmoji" to cleanAvatar,
            "level" to safeLevel,
            "xp" to safeXp,
            "hp" to safeHp,
            "maxHp" to safeMaxHp,
            "energy" to safeEnergy,
            "maxEnergy" to safeMaxEnergy,
            "coins" to safeCoins,
            "currentRegionId" to safeRegionId,
            "currentNodeId" to safeNodeId,
            "visitedNodeIds" to safeVisitedNodes,
            "completedRegionIds" to safeCompletedRegions,
            "tutorialDone" to snapshot.tutorialDone,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      val activeItemsByKey =
        snapshot.inventoryItems
          .filter { it.quantity > 0L }
          .mapNotNull { item ->
            val key = item.itemKey.ifBlank { item.id }.trim().take(50)
            if (key.isBlank()) null else key to item
          }
          .toMap()

      activeItemsByKey.forEach { (itemKey, item) ->
        val itemRef = invColRef.document(itemKey)
        val clampedQty = item.quantity.coerceIn(1L, 99L)
        if (itemKey in existingInvIds) {
          batch.update(
            itemRef,
            mapOf(
              "quantity" to clampedQty,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        } else {
          val safeCategory =
            if (item.category in VALID_ITEM_CATEGORIES) item.category else "potion"
          val safeName = item.name.trim().take(60).ifBlank { itemKey }
          val safeDesc = item.description.trim().take(220).ifBlank { "Item petualangan." }
          val safeIcon = item.iconKey.trim().take(40).ifBlank { safeCategory }
          batch.set(
            itemRef,
            mapOf(
              "itemKey" to itemKey,
              "name" to safeName,
              "description" to safeDesc,
              "category" to safeCategory,
              "quantity" to clampedQty,
              "healAmount" to item.healAmount.coerceIn(0L, 200L),
              "energyAmount" to item.energyAmount.coerceIn(0L, 200L),
              "iconKey" to safeIcon,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
      }

      if (pruneRemovedItems) {
        existingInvSnapshot.documents.forEach { doc ->
          if (doc.id !in activeItemsByKey.keys) {
            batch.delete(doc.reference)
          }
        }
      }

      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, userRef.path)
      throw e
    }
  }

  suspend fun addOrIncrementItem(
    itemKey: String,
    name: String,
    description: String,
    category: String,
    quantityDelta: Long,
    healAmount: Long,
    energyAmount: Long,
    iconKey: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val itemRef = db.collection("users").document(uid).collection("inventory").document(itemKey)
    try {
      val snapshot = itemRef.get().await()
      if (snapshot.exists()) {
        val currentQty = (snapshot.getLong("quantity") ?: 1L)
        val newQty = (currentQty + quantityDelta).coerceIn(1L, 99L)
        itemRef
          .update(
            mapOf(
              "quantity" to newQty,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        itemRef
          .set(
            mapOf(
              "itemKey" to itemKey,
              "name" to name.take(60),
              "description" to description.take(220),
              "category" to category,
              "quantity" to quantityDelta.coerceIn(1L, 99L),
              "healAmount" to healAmount.coerceIn(0L, 200L),
              "energyAmount" to energyAmount.coerceIn(0L, 200L),
              "iconKey" to iconKey,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, itemRef.path)
      throw e
    }
  }

  suspend fun consumeItem(item: InventoryItem): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docId = item.id.ifBlank { item.itemKey }
    val itemRef = db.collection("users").document(uid).collection("inventory").document(docId)
    try {
      if (item.quantity <= 1L) {
        itemRef.delete().await()
      } else {
        itemRef
          .update(
            mapOf(
              "quantity" to (item.quantity - 1L).coerceAtLeast(1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, itemRef.path)
      throw e
    }
  }

  suspend fun addJournalEntry(
    entryId: String,
    title: String,
    summary: String,
    regionId: String,
    rewardXp: Long,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val entryRef = db.collection("users").document(uid).collection("journal").document(entryId)
    try {
      entryRef
        .set(
          mapOf(
            "title" to title.take(100),
            "summary" to summary.take(300),
            "regionId" to regionId,
            "rewardXp" to rewardXp.coerceIn(0L, 1000L),
            "createdAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, entryRef.path)
      throw e
    }
  }

  suspend fun resetAdventure(): Result<Unit> = runCatching {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    try {
      val invDocs = userRef.collection("inventory").get().await()
      for (doc in invDocs.documents) {
        doc.reference.delete().await()
      }
      val journalDocs = userRef.collection("journal").get().await()
      for (doc in journalDocs.documents) {
        doc.reference.delete().await()
      }
      userRef
        .update(
          mapOf(
            "level" to 1L,
            "xp" to 0L,
            "hp" to 30L,
            "maxHp" to 30L,
            "energy" to 15L,
            "maxEnergy" to 15L,
            "coins" to 25L,
            "currentRegionId" to "forest",
            "currentNodeId" to "forest_start",
            "visitedNodeIds" to listOf("forest_start"),
            "completedRegionIds" to emptyList<String>(),
            "tutorialDone" to false,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      userRef
        .collection("inventory")
        .document("berry_sweet")
        .set(
          mapOf(
            "itemKey" to "berry_sweet",
            "name" to "Beri Manis Rimba",
            "description" to "Camilan segar pemula yang memulihkan 10 HP dan 4 Energi.",
            "category" to "snack",
            "quantity" to 2L,
            "healAmount" to 10L,
            "energyAmount" to 4L,
            "iconKey" to "snack",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, userRef.path)
      throw e
    }
  }

  private fun logFlowError(
    error: Throwable,
    operationType: OperationType,
    path: String,
    userId: String,
  ) {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) return
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
      handleFirestoreError(exceptionToLog, operationType, path)
    }
  }

  private companion object {
    val VALID_REGIONS = setOf("forest", "ruins", "summit")
    val VALID_ITEM_CATEGORIES = setOf("potion", "key", "relic", "snack")
  }
}
