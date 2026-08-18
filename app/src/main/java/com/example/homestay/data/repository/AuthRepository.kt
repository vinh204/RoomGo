package com.example.homestay.data.repository

import com.example.homestay.data.dao.UserDao
import com.example.homestay.data.entity.User
import com.example.homestay.utils.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LocalLoginInfo(
    val failedAttempts: Int? = null, val lockedUntil: String? = null,
    val remainingAttempts: Int? = null, val locked: Boolean? = null,
    val permanent: Boolean? = null, val minutesRemaining: Long? = null,
    val message: String? = null
)

class LoginException(message: String, val authResponse: LocalLoginInfo? = null) : Exception(message)
data class AuthData(val user: User, val mongoUserId: String)

class AuthRepository(private val userDao: UserDao) {
    suspend fun login(email: String, password: String): Result<AuthData> = withContext(Dispatchers.IO) {
        val user = userDao.getUserByEmailForLogin(email.lowercase().trim())
        if (user != null && PasswordHasher.verify(password, user.password)) {
            Result.success(AuthData(user, user.id.toString()))
        } else Result.failure(LoginException("Email hoặc mật khẩu không đúng"))
    }

    suspend fun register(email: String, phone: String, password: String, fullName: String): Result<AuthData> = withContext(Dispatchers.IO) {
        try {
            val normalizedEmail = email.lowercase().trim()
            if (userDao.getUserByEmail(normalizedEmail) != null || userDao.getUserByPhone(phone) != null) {
                return@withContext Result.failure(Exception("Email hoặc số điện thoại đã tồn tại"))
            }
            val user = User(email = normalizedEmail, phone = phone, password = PasswordHasher.hash(password), fullName = fullName.trim())
            val id = userDao.insertUser(user)
            Result.success(AuthData(user.copy(id = id), id.toString()))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getUserByEmail(email: String) = withContext(Dispatchers.IO) { userDao.getUserByEmail(email.lowercase().trim()) }
    suspend fun getUserByPhone(phone: String) = withContext(Dispatchers.IO) { userDao.getUserByPhone(phone) }
    suspend fun getMongoUserIdByEmail(email: String): Result<String> = withContext(Dispatchers.IO) {
        userDao.getUserByEmail(email.lowercase().trim())?.let { Result.success(it.id.toString()) }
            ?: Result.failure(Exception("Không tìm thấy tài khoản"))
    }

    suspend fun updateUser(localUserId: Long, mongoUserId: String, fullName: String, newPassword: String? = null): Result<User> = withContext(Dispatchers.IO) {
        val current = userDao.getUserById(localUserId) ?: return@withContext Result.failure(Exception("Không tìm thấy tài khoản"))
        val updated = current.copy(fullName = fullName.trim(), password = newPassword?.takeIf(String::isNotBlank)?.let(PasswordHasher::hash) ?: current.password)
        userDao.updateUser(updated)
        Result.success(updated)
    }

    suspend fun syncUserFromBackend(localUserId: Long, mongoUserId: String): Result<User> = withContext(Dispatchers.IO) {
        userDao.getUserById(localUserId)?.let { Result.success(it) } ?: Result.failure(Exception("Không tìm thấy tài khoản"))
    }
}
