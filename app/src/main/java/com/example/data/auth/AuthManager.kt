package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class UserProfile(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val authProvider: String = "email" // "google" or "email"
)

class AuthManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("auth_user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHOTO = "user_photo"
        private const val KEY_AUTH_PROVIDER = "auth_provider"
        private const val KEY_REGISTERED_USERS = "registered_users_db"
    }

    fun getCurrentUser(): UserProfile? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null

        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val name = prefs.getString(KEY_USER_NAME, "Kullanıcı") ?: "Kullanıcı"
        val photo = prefs.getString(KEY_USER_PHOTO, null)
        val provider = prefs.getString(KEY_AUTH_PROVIDER, "email") ?: "email"

        return UserProfile(
            id = id,
            email = email,
            displayName = name,
            photoUrl = photo,
            authProvider = provider
        )
    }

    fun signInWithEmail(email: String, pass: String): Result<UserProfile> {
        val trimmedEmail = email.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return Result.failure(IllegalArgumentException("Lütfen geçerli bir e-posta adresi girin."))
        }
        if (pass.length < 6) {
            return Result.failure(IllegalArgumentException("Şifre en az 6 karakter olmalıdır."))
        }

        // Check registered users
        val usersJson = prefs.getString(KEY_REGISTERED_USERS, "{}") ?: "{}"
        val usersObj = JSONObject(usersJson)

        if (usersObj.has(trimmedEmail)) {
            val userRecord = usersObj.getJSONObject(trimmedEmail)
            if (!verifyPassword(userRecord, pass)) {
                return Result.failure(IllegalArgumentException("Hatalı şifre girdiniz. Lütfen tekrar deneyin."))
            }
            // Upgrade legacy plain-text records to salted hashes on first successful login
            if (userRecord.has("password")) {
                userRecord.remove("password")
                storePasswordHash(userRecord, pass)
                usersObj.put(trimmedEmail, userRecord)
                prefs.edit().putString(KEY_REGISTERED_USERS, usersObj.toString()).apply()
            }

            val user = UserProfile(
                id = userRecord.getString("id"),
                email = trimmedEmail,
                displayName = userRecord.getString("name"),
                authProvider = "email"
            )
            saveActiveSession(user)
            return Result.success(user)
        } else {
            // If user doesn't exist yet, auto-provision user profile for seamless onboarding
            val defaultName = trimmedEmail.substringBefore("@")
                .replace(".", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

            val newId = "usr_" + System.currentTimeMillis()
            val newRecord = JSONObject().apply {
                put("id", newId)
                put("name", defaultName)
                put("email", trimmedEmail)
                storePasswordHash(this, pass)
            }
            usersObj.put(trimmedEmail, newRecord)
            prefs.edit().putString(KEY_REGISTERED_USERS, usersObj.toString()).apply()

            val user = UserProfile(
                id = newId,
                email = trimmedEmail,
                displayName = defaultName,
                authProvider = "email"
            )
            saveActiveSession(user)
            return Result.success(user)
        }
    }

    fun signUpWithEmail(name: String, email: String, pass: String): Result<UserProfile> {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim().lowercase()

        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Lütfen adınızı ve soyadınızı girin."))
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return Result.failure(IllegalArgumentException("Lütfen geçerli bir e-posta adresi girin."))
        }
        if (pass.length < 6) {
            return Result.failure(IllegalArgumentException("Şifre en az 6 karakter olmalıdır."))
        }

        val usersJson = prefs.getString(KEY_REGISTERED_USERS, "{}") ?: "{}"
        val usersObj = JSONObject(usersJson)

        val newId = "usr_" + System.currentTimeMillis()
        val newRecord = JSONObject().apply {
            put("id", newId)
            put("name", trimmedName)
            put("email", trimmedEmail)
            storePasswordHash(this, pass)
        }
        usersObj.put(trimmedEmail, newRecord)
        prefs.edit().putString(KEY_REGISTERED_USERS, usersObj.toString()).apply()

        val user = UserProfile(
            id = newId,
            email = trimmedEmail,
            displayName = trimmedName,
            authProvider = "email"
        )
        saveActiveSession(user)
        return Result.success(user)
    }

    fun signInWithGoogle(
        email: String = "",
        displayName: String = "Google Kullanıcısı",
        photoUrl: String? = null
    ): Result<UserProfile> {
        val id = "google_" + System.currentTimeMillis()
        val user = UserProfile(
            id = id,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            authProvider = "google"
        )
        saveActiveSession(user)
        return Result.success(user)
    }

    fun signOut() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_PHOTO)
            .remove(KEY_AUTH_PROVIDER)
            .apply()
    }

    fun resetPassword(email: String): Result<String> {
        val trimmedEmail = email.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return Result.failure(IllegalArgumentException("Lütfen geçerli bir e-posta adresi girin."))
        }
        // Accounts are stored only on this device, so no reset e-mail can be sent.
        return Result.failure(
            IllegalStateException(
                "Hesaplar yalnızca bu cihazda saklanır, şifre sıfırlama e-postası gönderilemez. " +
                    "Aynı e-posta ile yeni bir hesap oluşturabilirsiniz."
            )
        )
    }

    private fun storePasswordHash(record: JSONObject, pass: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        record.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
        record.put("pw_hash", Base64.encodeToString(hashPassword(pass, salt), Base64.NO_WRAP))
    }

    private fun verifyPassword(record: JSONObject, pass: String): Boolean {
        if (record.has("pw_hash") && record.has("salt")) {
            val salt = Base64.decode(record.getString("salt"), Base64.NO_WRAP)
            val expected = Base64.decode(record.getString("pw_hash"), Base64.NO_WRAP)
            return MessageDigest.isEqual(expected, hashPassword(pass, salt))
        }
        // Legacy plain-text record (migrated after a successful login)
        if (!record.has("password")) return false
        val legacy = record.getString("password")
        return MessageDigest.isEqual(legacy.toByteArray(), pass.toByteArray())
    }

    private fun hashPassword(pass: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pass.toCharArray(), salt, 10_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
    }

    private fun saveActiveSession(user: UserProfile) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_NAME, user.displayName)
            .putString(KEY_USER_PHOTO, user.photoUrl)
            .putString(KEY_AUTH_PROVIDER, user.authProvider)
            .apply()
    }
}
