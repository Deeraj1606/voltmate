package com.company.voltmate.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.company.voltmate.data.model.TransactionType
import com.company.voltmate.data.model.Wallet
import com.company.voltmate.data.model.WalletTransaction
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

interface WalletRepository {
    fun observeWallet(uid: String): Flow<Wallet>
    fun observeTransactions(uid: String): Flow<List<WalletTransaction>>
    suspend fun getWallet(uid: String): Wallet
    suspend fun topUp(uid: String, amountPaise: Long, methodTitle: String): Result<WalletTransaction>
    suspend fun payForChargingSession(
        driverId: String,
        ownerId: String,
        sessionId: String,
        chargerName: String,
        amountPaise: Long
    ): Result<WalletTransaction>
    suspend fun withdraw(
        uid: String,
        amountPaise: Long,
        destination: String
    ): Result<WalletTransaction>
    fun getSavedWithdrawalDestination(uid: String): String
    fun saveWithdrawalDestination(uid: String, dest: String)
}

class FirestoreWalletRepository(
    private val firestore: FirebaseFirestore,
    context: Context
) : WalletRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("voltmate_wallet_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    // In-memory active flows keyed by uid
    private val walletFlows = mutableMapOf<String, MutableStateFlow<Wallet>>()
    private val transactionFlows = mutableMapOf<String, MutableStateFlow<List<WalletTransaction>>>()

    private fun getOrCreateWalletFlow(uid: String): MutableStateFlow<Wallet> {
        return walletFlows.getOrPut(uid) {
            val balance = getLocalBalance(uid)
            MutableStateFlow(Wallet(uid = uid, balance = balance))
        }
    }

    private fun getOrCreateTxFlow(uid: String): MutableStateFlow<List<WalletTransaction>> {
        return transactionFlows.getOrPut(uid) {
            val txs = getLocalTransactions(uid)
            MutableStateFlow(txs)
        }
    }

    override fun observeWallet(uid: String): Flow<Wallet> {
        val flow = getOrCreateWalletFlow(uid)
        // Background sync with Firestore
        scope.launch { syncWalletFromFirestore(uid) }
        return flow.asStateFlow()
    }

    override fun observeTransactions(uid: String): Flow<List<WalletTransaction>> {
        val flow = getOrCreateTxFlow(uid)
        scope.launch { syncTransactionsFromFirestore(uid) }
        return flow.asStateFlow()
    }

    override suspend fun getWallet(uid: String): Wallet {
        return getOrCreateWalletFlow(uid).value
    }

    private fun getLocalBalance(uid: String): Long {
        if (!prefs.getBoolean("wallet_init_$uid", false)) {
            // Seed new accounts with Rs 500 (50000 paise) welcome credit
            val defaultBalance = 50000L
            prefs.edit()
                .putLong("balance_$uid", defaultBalance)
                .putBoolean("wallet_init_$uid", true)
                .apply()

            val welcomeTx = WalletTransaction(
                id = "welcome_${UUID.randomUUID().toString().take(8)}",
                uid = uid,
                type = TransactionType.TOPUP.name,
                amount = defaultBalance,
                title = "Welcome credit",
                counterparty = "VoltMate",
                status = "COMPLETED",
                createdAt = System.currentTimeMillis()
            )
            saveLocalTransactions(uid, listOf(welcomeTx))
            return defaultBalance
        }
        return prefs.getLong("balance_$uid", 50000L)
    }

    private fun setLocalBalance(uid: String, balance: Long) {
        prefs.edit().putLong("balance_$uid", balance).apply()
        getOrCreateWalletFlow(uid).value = Wallet(uid = uid, balance = balance)
    }

    private fun getLocalTransactions(uid: String): List<WalletTransaction> {
        val raw = prefs.getString("txs_$uid", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<WalletTransaction>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    WalletTransaction(
                        id = obj.getString("id"),
                        uid = obj.optString("uid", uid),
                        type = obj.getString("type"),
                        amount = obj.getLong("amount"),
                        title = obj.getString("title"),
                        counterparty = obj.optString("counterparty", ""),
                        sessionId = obj.optString("sessionId", ""),
                        status = obj.optString("status", "COMPLETED"),
                        createdAt = obj.getLong("createdAt")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveLocalTransactions(uid: String, txs: List<WalletTransaction>) {
        val trimmed = txs.take(50)
        val array = JSONArray()
        trimmed.forEach { tx ->
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("uid", tx.uid)
                put("type", tx.type)
                put("amount", tx.amount)
                put("title", tx.title)
                put("counterparty", tx.counterparty)
                put("sessionId", tx.sessionId)
                put("status", tx.status)
                put("createdAt", tx.createdAt)
            }
            array.put(obj)
        }
        prefs.edit().putString("txs_$uid", array.toString()).apply()
        getOrCreateTxFlow(uid).value = trimmed
    }

    override suspend fun topUp(uid: String, amountPaise: Long, methodTitle: String): Result<WalletTransaction> {
        val current = getLocalBalance(uid)
        val newBalance = current + amountPaise
        setLocalBalance(uid, newBalance)

        val tx = WalletTransaction(
            id = "topup_${UUID.randomUUID().toString().take(8)}",
            uid = uid,
            type = TransactionType.TOPUP.name,
            amount = amountPaise,
            title = "Wallet Top-up ($methodTitle)",
            counterparty = "VoltMate Pay",
            status = "COMPLETED",
            createdAt = System.currentTimeMillis()
        )

        val currentTxs = getLocalTransactions(uid).toMutableList()
        currentTxs.add(0, tx)
        saveLocalTransactions(uid, currentTxs)

        // Background sync to Firestore
        scope.launch {
            try {
                withTimeoutOrNull(4000) {
                    val walletRef = firestore.collection("wallets").document(uid)
                    val txRef = walletRef.collection("transactions").document(tx.id)
                    firestore.runBatch { batch ->
                        batch.set(walletRef, mapOf("balance" to newBalance), SetOptions.merge())
                        batch.set(txRef, txToMap(tx))
                    }.await()
                }
            } catch (e: Exception) {
                // background offline sync
            }
        }

        return Result.success(tx)
    }

    override suspend fun payForChargingSession(
        driverId: String,
        ownerId: String,
        sessionId: String,
        chargerName: String,
        amountPaise: Long
    ): Result<WalletTransaction> {
        if (driverId.isNotBlank() && ownerId.isNotBlank() && driverId == ownerId) {
            return Result.failure(Exception("You cannot charge at your own registered charger bay"))
        }

        val driverBalance = getLocalBalance(driverId)
        if (driverBalance < amountPaise) {
            val shortPaise = amountPaise - driverBalance
            return Result.failure(Exception("Insufficient wallet balance. Short by ₹%.2f".format(shortPaise / 100.0)))
        }

        // Deduct from driver wallet
        val newDriverBalance = driverBalance - amountPaise
        setLocalBalance(driverId, newDriverBalance)

        val driverTx = WalletTransaction(
            id = "pay_${UUID.randomUUID().toString().take(8)}",
            uid = driverId,
            type = TransactionType.CHARGE_PAYMENT.name,
            amount = amountPaise,
            title = "Charge at $chargerName",
            counterparty = ownerId,
            sessionId = sessionId,
            status = "COMPLETED",
            createdAt = System.currentTimeMillis()
        )
        val driverTxs = getLocalTransactions(driverId).toMutableList()
        driverTxs.add(0, driverTx)
        saveLocalTransactions(driverId, driverTxs)

        // Calculate owner net earning:
        // Net = price x kWh minus 15% platform commission, minus 2% gateway fee, minus 18% GST on commission
        val gross = amountPaise
        val commission = Math.round(gross * 0.15)
        val gatewayFee = Math.round(gross * 0.02)
        val gstOnCommission = Math.round(commission * 0.18)
        val netPayout = (gross - commission - gatewayFee - gstOnCommission).coerceAtLeast(0L)

        // If owner is local or present, update owner local wallet immediately
        if (ownerId.isNotBlank()) {
            val ownerBalance = getLocalBalance(ownerId)
            val newOwnerBalance = ownerBalance + netPayout
            setLocalBalance(ownerId, newOwnerBalance)

            val ownerTx = WalletTransaction(
                id = "earn_${UUID.randomUUID().toString().take(8)}",
                uid = ownerId,
                type = TransactionType.EARNING.name,
                amount = netPayout,
                title = "Host Earning: $chargerName",
                counterparty = driverId,
                sessionId = sessionId,
                status = "COMPLETED",
                createdAt = System.currentTimeMillis()
            )
            val ownerTxs = getLocalTransactions(ownerId).toMutableList()
            ownerTxs.add(0, ownerTx)
            saveLocalTransactions(ownerId, ownerTxs)
        }

        // Firestore atomic batch
        scope.launch {
            try {
                withTimeoutOrNull(4000) {
                    val batch = firestore.batch()
                    val driverWalletRef = firestore.collection("wallets").document(driverId)
                    val driverTxRef = driverWalletRef.collection("transactions").document(driverTx.id)
                    batch.set(driverWalletRef, mapOf("balance" to newDriverBalance), SetOptions.merge())
                    batch.set(driverTxRef, txToMap(driverTx))

                    if (ownerId.isNotBlank()) {
                        val ownerWalletRef = firestore.collection("wallets").document(ownerId)
                        val ownerTxId = "earn_${driverTx.id.takeLast(8)}"
                        val ownerTxRef = ownerWalletRef.collection("transactions").document(ownerTxId)
                        val ownerTxMap = mapOf(
                            "id" to ownerTxId,
                            "uid" to ownerId,
                            "type" to TransactionType.EARNING.name,
                            "amount" to netPayout,
                            "title" to "Host Earning: $chargerName",
                            "counterparty" to driverId,
                            "sessionId" to sessionId,
                            "status" to "COMPLETED",
                            "createdAt" to System.currentTimeMillis()
                        )
                        batch.set(ownerWalletRef, mapOf("balance" to FieldValue.increment(netPayout)), SetOptions.merge())
                        batch.set(ownerTxRef, ownerTxMap)
                    }

                    if (sessionId.isNotBlank()) {
                        val sessionRef = firestore.collection("sessions").document(sessionId)
                        batch.update(sessionRef, "paid", true)
                    }

                    batch.commit().await()
                }
            } catch (e: Exception) {
                // background sync handles offline
            }
        }

        return Result.success(driverTx)
    }

    override suspend fun withdraw(
        uid: String,
        amountPaise: Long,
        destination: String
    ): Result<WalletTransaction> {
        val currentBalance = getLocalBalance(uid)
        if (amountPaise < 10000L) {
            return Result.failure(Exception("Minimum withdrawal amount is ₹100"))
        }
        if (amountPaise > currentBalance) {
            return Result.failure(Exception("Withdrawal amount exceeds available wallet balance"))
        }

        val newBalance = currentBalance - amountPaise
        setLocalBalance(uid, newBalance)
        saveWithdrawalDestination(uid, destination)

        val tx = WalletTransaction(
            id = "wth_${UUID.randomUUID().toString().take(8)}",
            uid = uid,
            type = TransactionType.WITHDRAWAL.name,
            amount = amountPaise,
            title = "Withdrawal to $destination",
            counterparty = destination,
            status = "PROCESSING",
            createdAt = System.currentTimeMillis()
        )

        val currentTxs = getLocalTransactions(uid).toMutableList()
        currentTxs.add(0, tx)
        saveLocalTransactions(uid, currentTxs)

        // Simulate flip to "COMPLETED" after ~5 seconds
        scope.launch {
            delay(5000)
            completeWithdrawal(uid, tx.id)
        }

        return Result.success(tx)
    }

    private fun completeWithdrawal(uid: String, txId: String) {
        val list = getLocalTransactions(uid).toMutableList()
        val index = list.indexOfFirst { it.id == txId }
        if (index != -1) {
            val updated = list[index].copy(status = "COMPLETED")
            list[index] = updated
            saveLocalTransactions(uid, list)
        }
    }

    override fun getSavedWithdrawalDestination(uid: String): String {
        return prefs.getString("withdraw_dest_$uid", "tata.ev.owner@okhdfcbank") ?: "tata.ev.owner@okhdfcbank"
    }

    override fun saveWithdrawalDestination(uid: String, dest: String) {
        prefs.edit().putString("withdraw_dest_$uid", dest).apply()
    }

    private suspend fun syncWalletFromFirestore(uid: String) {
        try {
            withTimeoutOrNull(4000) {
                val doc = firestore.collection("wallets").document(uid).get().await()
                if (doc.exists()) {
                    val serverBalance = doc.getLong("balance")
                    if (serverBalance != null) {
                        setLocalBalance(uid, serverBalance)
                    }
                }
            }
        } catch (e: Exception) {
            // fallback to local
        }
    }

    private suspend fun syncTransactionsFromFirestore(uid: String) {
        try {
            withTimeoutOrNull(4000) {
                val snap = firestore.collection("wallets").document(uid)
                    .collection("transactions")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(50)
                    .get().await()
                if (!snap.isEmpty) {
                    val serverTxs = snap.documents.mapNotNull { d ->
                        val id = d.getString("id") ?: d.id
                        val type = d.getString("type") ?: TransactionType.TOPUP.name
                        val amount = d.getLong("amount") ?: 0L
                        val title = d.getString("title") ?: ""
                        val counterparty = d.getString("counterparty") ?: ""
                        val sessionId = d.getString("sessionId") ?: ""
                        val status = d.getString("status") ?: "COMPLETED"
                        val createdAt = d.getLong("createdAt") ?: System.currentTimeMillis()
                        WalletTransaction(id, uid, type, amount, title, counterparty, sessionId, status, createdAt)
                    }
                    saveLocalTransactions(uid, serverTxs)
                }
            }
        } catch (e: Exception) {
            // fallback to local
        }
    }

    private fun txToMap(tx: WalletTransaction): Map<String, Any> {
        return mapOf(
            "id" to tx.id,
            "uid" to tx.uid,
            "type" to tx.type,
            "amount" to tx.amount,
            "title" to tx.title,
            "counterparty" to tx.counterparty,
            "sessionId" to tx.sessionId,
            "status" to tx.status,
            "createdAt" to tx.createdAt
        )
    }
}
