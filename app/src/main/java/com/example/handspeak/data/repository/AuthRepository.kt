package com.example.handspeak.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Repository لإدارة المصادقة باستخدام Firebase Authentication
 */
class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * الحصول على المستخدم الحالي
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * تسجيل الدخول باستخدام البريد الإلكتروني وكلمة المرور
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): kotlin.Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                kotlin.Result.success(user)
            } else {
                kotlin.Result.failure(Exception("Failed to sign in: User is null"))
            }
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }

    /**
     * إنشاء حساب جديد باستخدام البريد الإلكتروني وكلمة المرور
     */
    suspend fun createUserWithEmailAndPassword(email: String, password: String): kotlin.Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                kotlin.Result.success(user)
            } else {
                kotlin.Result.failure(Exception("Failed to create account: User is null"))
            }
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }

    /**
     * تحديث اسم المستخدم
     */
    suspend fun updateDisplayName(name: String): kotlin.Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profileUpdates).await()
                kotlin.Result.success(Unit)
            } else {
                kotlin.Result.failure(Exception("No user is currently signed in"))
            }
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }

    /**
     * تسجيل الخروج
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * التحقق من حالة تسجيل الدخول
     */
    fun isUserSignedIn(): Boolean = auth.currentUser != null

    /**
     * إعادة تعيين كلمة المرور
     */
    suspend fun resetPassword(email: String): kotlin.Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
}

