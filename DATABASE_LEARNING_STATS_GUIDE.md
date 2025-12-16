# 💾 دليل: حفظ وإدارة بيانات التعلم
## Guide: Saving and Managing Learning Data

---

## 📋 البنية المقترحة للقاعدة

### 1️⃣ كيان الإحصائيات
```kotlin
// LearningStatsEntity.kt
@Entity(tableName = "learning_stats")
data class LearningStatsEntity(
    @PrimaryKey
    val label: String,  // الحرف أو الكلمة
    
    val totalAttempts: Int = 0,
    val successfulDetections: Int = 0,
    val averageConfidence: Float = 0f,
    val bestConfidence: Float = 0f,
    val worstConfidence: Float = 1f,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
```

### 2️⃣ كيان السجل الزمني
```kotlin
// LearningSessionEntity.kt
@Entity(
    tableName = "learning_sessions",
    indices = [Index("user_id"), Index("session_date")]
)
data class LearningSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0,
    
    @ColumnInfo(name = "user_id")
    val userId: String = "default_user",
    
    val label: String,  // الحرف المتعلم
    val attemptsInSession: Int,
    val successInSession: Int,
    val averageConfidenceInSession: Float,
    
    @ColumnInfo(name = "session_date")
    val sessionDate: Long = System.currentTimeMillis(),
    
    val sessionDurationMs: Long = 0
)
```

### 3️⃣ كيان الإنجازات
```kotlin
// AchievementEntity.kt
@Entity(
    tableName = "achievements",
    indices = [Index("user_id")]
)
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "user_id")
    val userId: String = "default_user",
    
    val achievementType: String,  // "PERFECT_LETTER", "10_LETTERS", "LEVEL_UP", etc.
    val achievementLabel: String,  // الحرف المتقن أو وصف الإنجاز
    
    @ColumnInfo(name = "earned_at")
    val earnedAt: Long = System.currentTimeMillis(),
    
    val reward: Int = 10  // نقاط
)
```

### 4️⃣ كيان نقاط المستخدم
```kotlin
// UserProfileEntity.kt
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val userId: String = "default_user",
    
    val displayName: String = "المستخدم",
    val totalPoints: Int = 0,
    val totalLettersLearned: Int = 0,
    val currentLevel: String = "BEGINNER",
    val highestAccuracy: Float = 0f,
    
    @ColumnInfo(name = "joined_at")
    val joinedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "last_login")
    val lastLogin: Long = System.currentTimeMillis()
)
```

---

## 🔌 DAOs والعمليات

### DAO للإحصائيات
```kotlin
// LearningStatsDao.kt
@Dao
interface LearningStatsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stat: LearningStatsEntity)
    
    @Query("SELECT * FROM learning_stats WHERE label = :label")
    suspend fun getStats(label: String): LearningStatsEntity?
    
    @Query("SELECT * FROM learning_stats ORDER BY updated_at DESC")
    fun getAllStats(): Flow<List<LearningStatsEntity>>
    
    @Query("SELECT * FROM learning_stats WHERE totalAttempts >= 10 AND (successfulDetections * 100 / totalAttempts) >= 85 ORDER BY label")
    fun getMasteredLetters(): Flow<List<LearningStatsEntity>>
    
    @Query("SELECT * FROM learning_stats WHERE totalAttempts < 10 OR (successfulDetections * 100 / totalAttempts) < 70 ORDER BY label")
    fun getWeakLetters(): Flow<List<LearningStatsEntity>>
    
    @Query("DELETE FROM learning_stats WHERE label = :label")
    suspend fun deleteStats(label: String)
    
    @Query("DELETE FROM learning_stats")
    suspend fun deleteAllStats()
    
    @Query("SELECT COUNT(*) FROM learning_stats WHERE (successfulDetections * 100 / totalAttempts) >= 85")
    suspend fun getMasteredCount(): Int
}
```

### DAO للجلسات
```kotlin
// LearningSessionDao.kt
@Dao
interface LearningSessionDao {
    
    @Insert
    suspend fun insertSession(session: LearningSessionEntity)
    
    @Query("SELECT * FROM learning_sessions WHERE user_id = :userId ORDER BY session_date DESC LIMIT 10")
    fun getRecentSessions(userId: String = "default_user"): Flow<List<LearningSessionEntity>>
    
    @Query("""
        SELECT label, 
               SUM(attemptsInSession) as totalAttempts,
               SUM(successInSession) as successInSession,
               AVG(averageConfidenceInSession) as avgConfidence
        FROM learning_sessions
        WHERE user_id = :userId AND session_date >= :startDate
        GROUP BY label
        ORDER BY session_date DESC
    """)
    fun getSessionStats(
        userId: String = "default_user",
        startDate: Long = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000  // Last 7 days
    ): Flow<List<SessionStatistic>>
}

data class SessionStatistic(
    val label: String,
    val totalAttempts: Int,
    val successInSession: Int,
    val avgConfidence: Float
)
```

### DAO للإنجازات
```kotlin
// AchievementDao.kt
@Dao
interface AchievementDao {
    
    @Insert
    suspend fun insertAchievement(achievement: AchievementEntity)
    
    @Query("SELECT * FROM achievements WHERE user_id = :userId ORDER BY earned_at DESC")
    fun getUserAchievements(userId: String = "default_user"): Flow<List<AchievementEntity>>
    
    @Query("SELECT SUM(reward) FROM achievements WHERE user_id = :userId")
    suspend fun getTotalRewards(userId: String = "default_user"): Int?
    
    @Query("SELECT COUNT(*) FROM achievements WHERE user_id = :userId AND achievementType = :type")
    suspend fun getAchievementCount(
        userId: String = "default_user",
        type: String
    ): Int
}
```

---

## 📦 Repository

```kotlin
// LearningRepository.kt
class LearningRepository(
    private val statsDao: LearningStatsDao,
    private val sessionDao: LearningSessionDao,
    private val achievementDao: AchievementDao,
    private val userProfileDao: UserProfileDao
) {
    
    // ============ الإحصائيات ============
    
    suspend fun saveOrUpdateStats(stats: LearningStats) {
        statsDao.insertOrUpdate(
            LearningStatsEntity(
                label = stats.label,
                totalAttempts = stats.totalAttempts,
                successfulDetections = stats.successfulDetections,
                averageConfidence = stats.averageConfidence,
                bestConfidence = stats.bestConfidence,
                worstConfidence = stats.worstConfidence,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
    
    fun getStats(label: String): Flow<LearningStats?> = flow {
        statsDao.getStats(label)?.let {
            emit(it.toLearningStats())
        }
    }
    
    fun getAllStats(): Flow<List<LearningStats>> = statsDao.getAllStats()
        .map { list -> list.map { it.toLearningStats() } }
    
    fun getMasteredLetters(): Flow<List<String>> = statsDao.getMasteredLetters()
        .map { list -> list.map { it.label } }
    
    fun getWeakLetters(): Flow<List<String>> = statsDao.getWeakLetters()
        .map { list -> list.map { it.label } }
    
    // ============ الجلسات ============
    
    suspend fun saveSession(
        label: String,
        attempts: Int,
        successes: Int,
        avgConfidence: Float,
        durationMs: Long
    ) {
        sessionDao.insertSession(
            LearningSessionEntity(
                label = label,
                attemptsInSession = attempts,
                successInSession = successes,
                averageConfidenceInSession = avgConfidence,
                sessionDurationMs = durationMs
            )
        )
    }
    
    fun getRecentSessions(): Flow<List<LearningSessionEntity>> = 
        sessionDao.getRecentSessions()
    
    // ============ الإنجازات ============
    
    suspend fun unlockAchievement(
        type: String,
        label: String,
        reward: Int = 10
    ) {
        achievementDao.insertAchievement(
            AchievementEntity(
                achievementType = type,
                achievementLabel = label,
                reward = reward
            )
        )
        
        // تحديث نقاط المستخدم
        updateUserPoints(reward)
    }
    
    fun getUserAchievements(): Flow<List<AchievementEntity>> =
        achievementDao.getUserAchievements()
    
    // ============ ملف المستخدم ============
    
    suspend fun updateUserProfile(profile: UserProfileEntity) {
        userProfileDao.insertOrUpdate(profile)
    }
    
    fun getUserProfile(): Flow<UserProfileEntity> = userProfileDao.getProfile()
    
    private suspend fun updateUserPoints(points: Int) {
        val currentProfile = userProfileDao.getProfileDirect()
        if (currentProfile != null) {
            userProfileDao.insertOrUpdate(
                currentProfile.copy(
                    totalPoints = currentProfile.totalPoints + points,
                    lastLogin = System.currentTimeMillis()
                )
            )
        }
    }
}

// Extension functions
private fun LearningStatsEntity.toLearningStats() = LearningStats(
    label = label,
    totalAttempts = totalAttempts,
    successfulDetections = successfulDetections,
    averageConfidence = averageConfidence,
    bestConfidence = bestConfidence,
    worstConfidence = worstConfidence
)
```

---

## 🎯 استخدام Repository

```kotlin
// في ViewModel
class LearningViewModel(
    private val repository: LearningRepository
) : ViewModel() {
    
    val masteredLetters = repository.getMasteredLetters().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
    
    val weakLetters = repository.getWeakLetters().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
    
    fun saveLearningSession(label: String, stats: LearningStats, durationMs: Long) {
        viewModelScope.launch {
            // حفظ الإحصائيات
            repository.saveOrUpdateStats(stats)
            
            // حفظ الجلسة
            repository.saveSession(
                label = label,
                attempts = stats.totalAttempts,
                successes = stats.successfulDetections,
                avgConfidence = stats.averageConfidence,
                durationMs = durationMs
            )
            
            // تحقق من الإنجازات
            if (stats.accuracyPercentage >= 100f) {
                repository.unlockAchievement(
                    type = "PERFECT_LETTER",
                    label = label,
                    reward = 50
                )
            }
            
            // تحقق من عدد الحروف المتقنة
            val masteredCount = repository.getMasteredLetters().first().size
            if (masteredCount == 10) {
                repository.unlockAchievement(
                    type = "TEN_LETTERS",
                    label = "10_letters",
                    reward = 100
                )
            }
        }
    }
    
    fun getUserProfile(): Flow<UserProfileEntity> = repository.getUserProfile()
}
```

---

## 📊 أمثلة الاستعلامات المتقدمة

### احصل على إحصائيات الأسبوع الماضي
```kotlin
@Query("""
    SELECT label, 
           SUM(attemptsInSession) as totalAttempts,
           SUM(successInSession) as totalSuccess,
           AVG(averageConfidenceInSession) as avgConfidence
    FROM learning_sessions
    WHERE session_date >= :weekAgo
    GROUP BY label
    ORDER BY totalSuccess DESC
    LIMIT 10
""")
suspend fun getWeeklyTopLetters(
    weekAgo: Long = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
): List<WeeklyStatistic>

data class WeeklyStatistic(
    val label: String,
    val totalAttempts: Int,
    val totalSuccess: Int,
    val avgConfidence: Float
)
```

### احصل على تقدم المستخدم
```kotlin
@Query("""
    SELECT 
        COUNT(DISTINCT label) as uniqueLetters,
        SUM(totalAttempts) as totalAttempts,
        SUM(successfulDetections) as totalSuccess,
        AVG(averageConfidence) as overallAvgConfidence
    FROM learning_stats
""")
suspend fun getUserProgress(): UserProgressStatistic

data class UserProgressStatistic(
    val uniqueLetters: Int,
    val totalAttempts: Int,
    val totalSuccess: Int,
    val overallAvgConfidence: Float
)
```

---

## 🔄 نظام التزامن (Sync)

```kotlin
// للتطبيقات المتقدمة - حفظ البيانات في السحابة
interface CloudSyncService {
    suspend fun uploadStats(stats: List<LearningStatsEntity>): Boolean
    suspend fun downloadStats(): List<LearningStatsEntity>
    suspend fun syncAchievements(): Boolean
}

class LocalRepositoryWithSync(
    private val localRepo: LearningRepository,
    private val cloudService: CloudSyncService
) {
    suspend fun syncWithCloud() {
        try {
            // تحميل البيانات للسحابة
            cloudService.uploadStats(localRepo.getAllStats().first())
            
            // تحميل بيانات جديدة من السحابة
            val cloudStats = cloudService.downloadStats()
            cloudStats.forEach { localRepo.saveOrUpdateStats(it.toLearningStats()) }
            
            Log.d("Sync", "✅ تم المزامنة بنجاح")
        } catch (e: Exception) {
            Log.e("Sync", "❌ فشلت المزامنة: ${e.message}")
        }
    }
}
```

---

## 📈 رؤى البيانات

```kotlin
object DataInsights {
    
    fun analyzeWeakPoints(
        stats: Map<String, LearningStats>
    ): List<InsightMessage> {
        return stats
            .filter { it.value.accuracyPercentage < 70f }
            .sortedBy { it.value.accuracyPercentage }
            .map { (label, stat) ->
                InsightMessage(
                    type = "WEAK_POINT",
                    message = "الحرف '$label' يحتاج تحسين (${stat.successRate})",
                    priority = when {
                        stat.accuracyPercentage < 30f -> "HIGH"
                        stat.accuracyPercentage < 50f -> "MEDIUM"
                        else -> "LOW"
                    }
                )
            }
    }
    
    fun findOptimalLearningOrder(
        stats: Map<String, LearningStats>
    ): List<String> {
        return stats
            .sortedWith(compareBy<Map.Entry<String, LearningStats>> { 
                it.value.totalAttempts  // الحروف القليلة أولاً
            }.thenBy { 
                it.value.accuracyPercentage  // ثم الأقل دقة
            })
            .map { it.key }
    }
}

data class InsightMessage(
    val type: String,
    val message: String,
    val priority: String
)
```

---

## ✅ الملخص

### 4 كيانات رئيسية
1. **LearningStatsEntity** - إحصائيات الحروف
2. **LearningSessionEntity** - سجل الجلسات
3. **AchievementEntity** - الإنجازات والنقاط
4. **UserProfileEntity** - ملف المستخدم

### العمليات الرئيسية
- ✅ حفظ/تحديث الإحصائيات
- ✅ تسجيل الجلسات
- ✅ فتح الإنجازات
- ✅ تتبع النقاط والمستويات
- ✅ تحليل البيانات

### الفوائد
📊 نظام تتبع شامل
📈 تحليل البيانات المتقدم
🏆 نظام إنجازات محفز
💾 سهولة المزامنة مع السحابة

**كل هذا يدعم النموذج المدرب بكامل القوة! 🚀**
