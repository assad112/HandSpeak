package com.example.handspeak.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.handspeak.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel لإدارة حالة المصادقة
 */
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isSignedIn = MutableStateFlow(authRepository.isUserSignedIn())
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    /**
     * تسجيل الدخول
     */
    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("يرجى إدخال البريد الإلكتروني وكلمة المرور")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authRepository.signInWithEmailAndPassword(email, password)
                .onSuccess { user ->
                    _uiState.value = AuthUiState.Success(user)
                    _isSignedIn.value = true
                }
                .onFailure { exception ->
                    _uiState.value = AuthUiState.Error(
                        getErrorMessage(exception.message ?: "حدث خطأ أثناء تسجيل الدخول")
                    )
                }
        }
    }

    /**
     * إنشاء حساب جديد
     */
    fun signUp(name: String, email: String, password: String) {
        if (name.isBlank()) {
            _uiState.value = AuthUiState.Error("يرجى إدخال الاسم")
            return
        }
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("يرجى إدخال البريد الإلكتروني وكلمة المرور")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("كلمة المرور يجب أن تكون 6 أحرف على الأقل")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authRepository.createUserWithEmailAndPassword(email, password)
                .onSuccess { user ->
                    // تحديث اسم المستخدم
                    authRepository.updateDisplayName(name)
                        .onSuccess {
                            _uiState.value = AuthUiState.Success(user)
                            _isSignedIn.value = true
                        }
                        .onFailure { _ ->
                            // حتى لو فشل تحديث الاسم، المستخدم تم إنشاؤه بنجاح
                            _uiState.value = AuthUiState.Success(user)
                            _isSignedIn.value = true
                        }
                }
                .onFailure { exception ->
                    _uiState.value = AuthUiState.Error(
                        getErrorMessage(exception.message ?: "حدث خطأ أثناء إنشاء الحساب")
                    )
                }
        }
    }

    /**
     * إعادة تعيين كلمة المرور
     */
    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("يرجى إدخال البريد الإلكتروني")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authRepository.resetPassword(email)
                .onSuccess {
                    _uiState.value = AuthUiState.PasswordResetSent
                }
                .onFailure { exception ->
                    _uiState.value = AuthUiState.Error(
                        getErrorMessage(exception.message ?: "حدث خطأ أثناء إرسال البريد")
                    )
                }
        }
    }

    /**
     * تسجيل الخروج
     */
    fun signOut() {
        authRepository.signOut()
        _isSignedIn.value = false
        _uiState.value = AuthUiState.Idle
    }

    /**
     * إعادة تعيين حالة UI
     */
    fun resetUiState() {
        _uiState.value = AuthUiState.Idle
    }

    /**
     * الحصول على المستخدم الحالي
     */
    fun getCurrentUser(): FirebaseUser? = authRepository.getCurrentUser()

    /**
     * تحويل رسائل الخطأ إلى عربية
     */
    private fun getErrorMessage(error: String): String {
        return when {
            error.contains("email address is badly formatted", ignoreCase = true) -> 
                "البريد الإلكتروني غير صحيح"
            error.contains("password is invalid", ignoreCase = true) || 
            error.contains("wrong password", ignoreCase = true) -> 
                "كلمة المرور غير صحيحة"
            error.contains("user not found", ignoreCase = true) -> 
                "المستخدم غير موجود"
            error.contains("email already in use", ignoreCase = true) -> 
                "البريد الإلكتروني مستخدم بالفعل"
            error.contains("weak password", ignoreCase = true) -> 
                "كلمة المرور ضعيفة. يجب أن تكون 6 أحرف على الأقل"
            error.contains("network", ignoreCase = true) -> 
                "خطأ في الاتصال. يرجى التحقق من الإنترنت"
            else -> error
        }
    }
}

/**
 * حالات UI للمصادقة
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: FirebaseUser) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    object PasswordResetSent : AuthUiState()
}

