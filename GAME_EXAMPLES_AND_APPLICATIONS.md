# 🎮 أمثلة عملية: ألعاب وتطبيقات تعليمية
## Practical Examples: Games and Learning Applications

---

## 🎯 مثال 1: لعبة "تطابق الحرف"
### Match the Letter Game

```kotlin
/**
 * لعبة بسيطة: اعرض حرفاً واطلب من المستخدم تكراره
 * Game Logic: Show letter → User performs gesture → Verify
 */
class MatchLetterGame(
    private val learningHelper: LearningHelper,
    private val classifier: SignLanguageClassifier
) {
    private var currentLetter: String = ""
    private var score: Int = 0
    private var attempts: Int = 0
    private val LETTERS_PER_ROUND = 5
    
    fun startNewRound() {
        currentLetter = getRandomLetter()
        attempts = 0
        Log.d("MatchLetterGame", "🎯 حرف جديد: $currentLetter")
    }
    
    fun checkGesture(landmarks: FloatArray): GameResult {
        attempts++
        
        val result = classifier.classify(landmarks) ?: return GameResult.ERROR
        val (predictedLabel, confidence) = result
        
        return when {
            predictedLabel == currentLetter && confidence > 0.75f -> {
                score++
                GameResult.SUCCESS(score, "🌟 صحيح! الحرف: $currentLetter")
            }
            confidence > 0.5f -> {
                GameResult.CLOSE(confidence, "قريب! الحرف هو: $currentLetter")
            }
            else -> {
                GameResult.WRONG("❌ اعادة المحاولة - الحرف: $currentLetter")
            }
        }
    }
    
    fun isRoundComplete(): Boolean = score >= LETTERS_PER_ROUND
    
    fun getGameStats(): String {
        return "🎮 اللعبة: $score/$LETTERS_PER_ROUND • محاولات: $attempts"
    }
    
    private fun getRandomLetter(): String {
        val letters = listOf("ا", "ب", "ت", "ث", "ج", "ح", "خ", "د")
        return letters.random()
    }
}

sealed class GameResult {
    data class SUCCESS(val score: Int, val message: String) : GameResult()
    data class CLOSE(val confidence: Float, val message: String) : GameResult()
    data class WRONG(val message: String) : GameResult()
    object ERROR : GameResult()
}
```

---

## 🏃 مثال 2: لعبة "سباق الحروف"
### Speed Race Game

```kotlin
/**
 * لعبة السرعة: كم حرفاً تستطيع التعرف عليها في وقت محدد؟
 */
class SpeedRaceGame(
    private val classifier: SignLanguageClassifier,
    private val learningHelper: LearningHelper
) {
    private var targetCount: Int = 10
    private var detectedCount: Int = 0
    private var startTime: Long = 0
    private val GAME_DURATION_MS = 60000L  // دقيقة واحدة
    private var lastDetectedLabel: String? = null
    
    fun startGame() {
        startTime = System.currentTimeMillis()
        detectedCount = 0
        lastDetectedLabel = null
        Log.d("SpeedRaceGame", "🏃 بدأت اللعبة! لديك دقيقة واحدة")
    }
    
    fun processFrame(landmarks: FloatArray): RaceResult {
        if (isGameEnded()) {
            return RaceResult.GameEnded(detectedCount, getElapsedTime())
        }
        
        val result = classifier.classify(landmarks) ?: return RaceResult.Processing
        val (predictedLabel, confidence) = result
        
        // تجنب العد المتكرر للحرف نفسه
        return if (confidence > 0.8f && predictedLabel != lastDetectedLabel) {
            lastDetectedLabel = predictedLabel
            detectedCount++
            
            val message = when {
                detectedCount >= targetCount -> "🏆 مبروك! وصلت للهدف!"
                else -> "✅ كشف: $predictedLabel ($detectedCount/$targetCount)"
            }
            
            RaceResult.Success(predictedLabel, detectedCount, message)
        } else {
            RaceResult.Processing
        }
    }
    
    private fun isGameEnded(): Boolean {
        return System.currentTimeMillis() - startTime > GAME_DURATION_MS
    }
    
    private fun getElapsedTime(): Int {
        return ((System.currentTimeMillis() - startTime) / 1000).toInt()
    }
    
    fun getGameStats(): String {
        return """
            🏃 سباق الحروف
            ───────────────
            📊 الحروف المكتشفة: $detectedCount
            ⏱️ الوقت: ${getElapsedTime()}s
            🎯 الهدف: $targetCount
        """.trimIndent()
    }
}

sealed class RaceResult {
    data class Success(val letter: String, val count: Int, val message: String) : RaceResult()
    data class GameEnded(val finalCount: Int, val timeInSeconds: Int) : RaceResult()
    object Processing : RaceResult()
}
```

---

## 📚 مثال 3: نظام التعليم المتدرج
### Progressive Learning System

```kotlin
/**
 * نظام تعليم متدرج من السهل إلى الصعب
 */
class ProgressiveLearning(
    private val classifier: SignLanguageClassifier,
    private val learningHelper: LearningHelper
) {
    
    enum class LevelDifficulty {
        BEGINNER,      // الحروف السهلة (4-5 حروف)
        INTERMEDIATE,  // حروف متوسطة (10 حروف)
        ADVANCED,      // حروف صعبة (15+ حرف)
        EXPERT         // كل الحروف
    }
    
    data class LearningLevel(
        val difficulty: LevelDifficulty,
        val letters: List<String>,
        val requiredAccuracy: Float,
        val minAttempts: Int
    )
    
    private val levels = mapOf(
        LevelDifficulty.BEGINNER to LearningLevel(
            difficulty = LevelDifficulty.BEGINNER,
            letters = listOf("ا", "ب", "ت", "ث"),
            requiredAccuracy = 0.8f,
            minAttempts = 10
        ),
        LevelDifficulty.INTERMEDIATE to LearningLevel(
            difficulty = LevelDifficulty.INTERMEDIATE,
            letters = listOf("ا", "ب", "ت", "ث", "ج", "ح", "خ", "د", "ذ", "ر"),
            requiredAccuracy = 0.85f,
            minAttempts = 20
        ),
        LevelDifficulty.ADVANCED to LearningLevel(
            difficulty = LevelDifficulty.ADVANCED,
            letters = listOf("ا", "ب", "ت", "ث", "ج", "ح", "خ", "د", "ذ", "ر", "ز", "س", "ش", "ص", "ض"),
            requiredAccuracy = 0.90f,
            minAttempts = 30
        )
    )
    
    fun getCurrentLevel(): LevelDifficulty {
        val allStats = learningHelper.getAllStats()
        
        // تحقق إذا أكمل BEGINNER
        val beginnerLevel = levels[LevelDifficulty.BEGINNER]!!
        val beginnerStats = allStats.filterKeys { it in beginnerLevel.letters }
        val beginnerAccuracy = if (beginnerStats.isNotEmpty()) {
            beginnerStats.values.map { it.accuracyPercentage }.average() / 100f
        } else {
            0f
        }
        
        return when {
            beginnerAccuracy >= 0.8f && beginnerStats.size == beginnerLevel.letters.size 
                -> LevelDifficulty.INTERMEDIATE
            beginnerAccuracy >= 0.85f && beginnerStats.size >= 8 
                -> LevelDifficulty.ADVANCED
            else -> LevelDifficulty.BEGINNER
        }
    }
    
    fun getNextLesson(currentLevel: LevelDifficulty): String? {
        val level = levels[currentLevel] ?: return null
        val stats = learningHelper.getAllStats()
        
        // ابحث عن أسهل حرف لم يتم إتقانه
        return level.letters
            .filterNot { stats[it]?.accuracyPercentage ?: 0f > level.requiredAccuracy }
            .firstOrNull()
    }
    
    fun isLevelComplete(difficulty: LevelDifficulty): Boolean {
        val level = levels[difficulty] ?: return false
        val stats = learningHelper.getAllStats()
        
        return level.letters.all { letter ->
            val stat = stats[letter] ?: return@all false
            stat.accuracyPercentage >= level.requiredAccuracy * 100 && 
            stat.totalAttempts >= level.minAttempts
        }
    }
    
    fun getProgressReport(difficulty: LevelDifficulty): String {
        val level = levels[difficulty] ?: return "مستوى غير موجود"
        val stats = learningHelper.getAllStats()
        
        val completedLetters = level.letters.count { letter ->
            val stat = stats[letter]
            stat != null && stat.accuracyPercentage >= level.requiredAccuracy * 100
        }
        
        return """
            🎓 تقرير المستوى: ${difficulty.name}
            ────────────────────────────
            ✅ حروف مكتملة: $completedLetters/${level.letters.size}
            📈 التقدم: ${(completedLetters * 100 / level.letters.size)}%
            🎯 دقة مطلوبة: ${(level.requiredAccuracy * 100).toInt()}%
            
            ${if (isLevelComplete(difficulty)) "🏆 مستوى مكتمل!" else "💪 استمر في الممارسة"}
        """.trimIndent()
    }
}
```

---

## 🎤 مثال 4: نظام التعرف على الكلمات
### Word Recognition System

```kotlin
/**
 * التعرف على كلمات كاملة (سلسلة من الحروف)
 */
class WordRecognitionSystem(
    private val classifier: SignLanguageClassifier,
    private val learningHelper: LearningHelper
) {
    
    private val recognizedSequence = mutableListOf<Pair<String, Float>>()
    private val CONFIDENCE_THRESHOLD = 0.75f
    private val SEQUENCE_TIMEOUT_MS = 3000L  // 3 ثواني
    private var lastFrameTime = System.currentTimeMillis()
    
    fun procesFrameForWord(landmarks: FloatArray): String {
        val currentTime = System.currentTimeMillis()
        
        // امسح التسلسل إذا انقضى الوقت
        if (currentTime - lastFrameTime > SEQUENCE_TIMEOUT_MS) {
            recognizedSequence.clear()
        }
        
        lastFrameTime = currentTime
        
        val result = classifier.classify(landmarks)
        if (result != null) {
            val (label, confidence) = result
            
            if (confidence > CONFIDENCE_THRESHOLD) {
                // أضف فقط إذا كان مختلفاً عن آخر حرف
                if (recognizedSequence.isEmpty() || recognizedSequence.last().first != label) {
                    recognizedSequence.add(Pair(label, confidence))
                }
            }
        }
        
        return getRecognizedWord()
    }
    
    private fun getRecognizedWord(): String {
        return recognizedSequence.joinToString(" ") { it.first }
    }
    
    fun clearSequence() {
        recognizedSequence.clear()
    }
    
    fun getSequenceConfidence(): Float {
        return if (recognizedSequence.isNotEmpty()) {
            recognizedSequence.map { it.second }.average().toFloat()
        } else {
            0f
        }
    }
}
```

---

## 📱 مثال 5: واجهة رسومية متكاملة
### Complete UI Integration

```kotlin
/**
 * مثال على كيفية دمج جميع الأنظمة معاً في الواجهة
 */
class LearningActivityViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _gameState = MutableStateFlow<GameState>(GameState.Idle)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    
    private val matchGameLauncher = MatchLetterGame(learningHelper, classifier)
    private val speedRaceLauncher = SpeedRaceGame(classifier, learningHelper)
    private val progressiveLearning = ProgressiveLearning(classifier, learningHelper)
    
    // 1️⃣ لعبة التطابق
    fun startMatchGame() {
        matchGameLauncher.startNewRound()
        _gameState.value = GameState.MatchGameActive(
            letter = matchGameLauncher.currentLetter,
            score = 0
        )
    }
    
    // 2️⃣ لعبة السرعة
    fun startSpeedRace() {
        speedRaceLauncher.startGame()
        _gameState.value = GameState.SpeedRaceActive(0)
    }
    
    // 3️⃣ التعليم المتدرج
    fun startProgressiveLearning() {
        val currentLevel = progressiveLearning.getCurrentLevel()
        val nextLesson = progressiveLearning.getNextLesson(currentLevel)
        
        _gameState.value = GameState.ProgressiveLearningActive(
            level = currentLevel,
            currentLesson = nextLesson ?: "لا توجد دروس متاحة"
        )
    }
    
    // 4️⃣ عرض التقرير الشامل
    fun showDetailedReport() {
        val report = StringBuilder()
        report.append(learningHelper.getProgressReport())
        report.append("\n\n")
        report.append(learningHelper.getSmartRecommendation())
        
        _gameState.value = GameState.ReportShowing(report.toString())
    }
}

sealed class GameState {
    object Idle : GameState()
    data class MatchGameActive(val letter: String, val score: Int) : GameState()
    data class SpeedRaceActive(val count: Int) : GameState()
    data class ProgressiveLearningActive(val level: String, val currentLesson: String) : GameState()
    data class ReportShowing(val report: String) : GameState()
}
```

---

## 🎨 مثال 6: رسائل تشجيعية ذكية
### Smart Motivation Messages

```kotlin
/**
 * نظام رسائل تشجيعية بناءً على الأداء
 */
object MotivationSystem {
    
    fun getMotivationMessage(
        stats: LearningStats,
        confidence: Float,
        isCorrect: Boolean
    ): String {
        return when {
            !isCorrect && confidence > 0.85f -> {
                "🎯 نعم! النموذج اعتقد انك قلت: '${stats.label}'.. لكن هذا حرف آخر!\n" +
                "💡 الفرق بسيط جداً، ركز على التفاصيل الدقيقة"
            }
            isCorrect && stats.accuracyPercentage == 100f -> {
                "🏆 مبروك! أنت متقن تماماً للحرف '${stats.label}'!\n" +
                "⭐ انتقل للحرف التالي"
            }
            isCorrect && stats.accuracyPercentage >= 90f -> {
                "✨ رائع! أنت بطل في الحرف '${stats.label}'!\n" +
                "📚 يمكنك الآن المرور إلى التحدي التالي"
            }
            isCorrect && stats.accuracyPercentage >= 75f -> {
                "👍 جيد جداً! أنت على الطريق الصحيح\n" +
                "💪 مزيد من الممارسة ستجعلك أفضل"
            }
            isCorrect -> {
                "✅ صحيح! لكن دقتك ${stats.successRate}\n" +
                "🎯 استمر في الممارسة"
            }
            stats.totalAttempts > 5 && stats.accuracyPercentage < 50f -> {
                "💭 يبدو أن هذا الحرف صعب عليك..\n" +
                "📺 شاهد صورة الإشارة مرة أخرى\n" +
                "🎬 حاول تكرار الحركات بدقة"
            }
            else -> {
                "🎯 استمر في المحاولة!\n" +
                "🚀 كل محاولة تقربك من النجاح"
            }
        }
    }
    
    fun getLevelUpMessage(newLevel: String): String {
        return """
            ✨✨✨ مبروك! ✨✨✨
            
            📈 لقد انتقلت إلى المستوى: $newLevel
            
            🎉 أنت تحرز تقدماً رائعاً!
            💪 استمر في الممارسة
            🏆 النجاح يقتربك أكثر فأكثر
        """.trimIndent()
    }
    
    fun getMilestoneMessage(milestone: Int): String {
        return when (milestone) {
            10 -> "🥉 لقد أكملت 10 حروف! بداية قوية"
            25 -> "🥈 25 حرف! أنت في الطريق الصحيح"
            50 -> "🥇 50 حرف! أنت تقترب من الإتقان"
            else -> "🌟 مبروك على الوصول إلى $milestone حرف!"
        }
    }
}
```

---

## 🔗 دمج كل شيء معاً

```kotlin
// في الشاشة الرئيسية للتعليم:

@Composable
fun LearningScreen(viewModel: LearningActivityViewModel) {
    val gameState by viewModel.gameState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (gameState) {
            is GameState.MatchGameActive -> {
                MatchGameScreen(gameState as GameState.MatchGameActive)
            }
            is GameState.SpeedRaceActive -> {
                SpeedRaceScreen(gameState as GameState.SpeedRaceActive)
            }
            is GameState.ProgressiveLearningActive -> {
                ProgressiveLearningScreen(gameState as GameState.ProgressiveLearningActive)
            }
            is GameState.ReportShowing -> {
                ReportScreen(gameState as GameState.ReportShowing)
            }
            else -> {
                MenuScreen(viewModel)
            }
        }
    }
}
```

---

## 📊 ملخص الأمثلة

| الميزة | الاستخدام | الفائدة |
|--------|----------|--------|
| **MatchLetterGame** | للمبتدئين | تعلم حروف محددة |
| **SpeedRaceGame** | للمتقدمين | تطوير السرعة والدقة |
| **ProgressiveLearning** | الكل | نظام متدرج منطقي |
| **WordRecognitionSystem** | المتقدمون | كلمات وجمل |
| **MotivationSystem** | الكل | تشجيع مستمر |

---

## ✅ الفوائد الرئيسية

✅ استخدام النموذج المدرب بكامل قوته
✅ تطبيقات عملية وممتعة
✅ نظام تقييم تلقائي ودقيق
✅ تحفيز مستمر للمستخدم
✅ تتبع شامل للتقدم
✅ قابل للتوسع والتطوير

🎮 **اختر اللعبة التي تناسب مستوى المستخدم وابدأ الآن!**
