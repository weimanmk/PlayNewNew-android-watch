package niuniu.a.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import niuniu.a.presentation.data.NiuNiuStorage
import niuniu.a.presentation.feedback.DeviceFeedback
import niuniu.a.presentation.model.PlayRecord
import niuniu.a.presentation.model.dailyAverages
import niuniu.a.presentation.model.doubleStats
import niuniu.a.presentation.model.intStats
import niuniu.a.presentation.model.recordsForLastDays
import niuniu.a.presentation.model.recordsForToday
import niuniu.a.presentation.sensor.HeartRateMonitor
import niuniu.a.presentation.sensor.ShakeDetector
import niuniu.a.presentation.theme.NiuniuTheme
import kotlin.math.max
import kotlin.math.sqrt

private enum class Route { HOME, PLAY, RECORD, CHALLENGE, SETTINGS, SENSITIVITY }
private enum class RecordTab { OVERVIEW, DAILY, WEEKLY, MONTHLY }
private enum class ChallengeTab { MENU, FREQ_CONFIG, FREQ_RUN, EDGE, TIME }

@Composable
fun NiuNiuWatchApp() {
    val context = LocalContext.current
    var route by rememberSaveable { mutableStateOf(Route.HOME) }
    var recordsVersion by rememberSaveable { mutableIntStateOf(0) }

    var soundEnabled by rememberSaveable { mutableStateOf(NiuNiuStorage.isSoundEnabled(context)) }
    var accelerationThreshold by rememberSaveable { mutableFloatStateOf(NiuNiuStorage.accelerationThreshold(context)) }
    var timeThreshold by rememberSaveable { mutableFloatStateOf(NiuNiuStorage.timeThreshold(context)) }

    var hasBodySensors by remember { mutableStateOf(context.hasBodySensorsPermission()) }
    val bodySensorsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasBodySensors = it
    }

    NiuniuTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            when (route) {
                Route.HOME -> HomeScreen(
                    onPlay = { route = Route.PLAY },
                    onRecord = { route = Route.RECORD },
                    onChallenge = { route = Route.CHALLENGE },
                    onSettings = { route = Route.SETTINGS },
                )
                Route.PLAY -> PlayScreen(
                    soundEnabled = soundEnabled,
                    accelerationThreshold = accelerationThreshold,
                    timeThreshold = timeThreshold,
                    hasBodySensorsPermission = hasBodySensors,
                    requestBodySensorsPermission = { bodySensorsLauncher.launch(Manifest.permission.BODY_SENSORS) },
                    onSaveRecord = {
                        NiuNiuStorage.appendRecord(context, it)
                        recordsVersion += 1
                    },
                    onBack = { route = Route.HOME },
                )
                Route.RECORD -> RecordScreen(recordsVersion = recordsVersion, onBack = { route = Route.HOME })
                Route.CHALLENGE -> ChallengeScreen(
                    soundEnabled = soundEnabled,
                    accelerationThreshold = accelerationThreshold,
                    timeThreshold = timeThreshold,
                    onBack = { route = Route.HOME },
                )
                Route.SETTINGS -> SettingsScreen(
                    soundEnabled = soundEnabled,
                    appVersion = context.appVersionName(),
                    onToggleSound = {
                        soundEnabled = !soundEnabled
                        NiuNiuStorage.setSoundEnabled(context, soundEnabled)
                    },
                    onSensitivity = { route = Route.SENSITIVITY },
                    onClear = {
                        NiuNiuStorage.clearRecords(context)
                        recordsVersion += 1
                    },
                    onBack = { route = Route.HOME },
                )
                Route.SENSITIVITY -> SensitivityScreen(
                    accelerationThreshold = accelerationThreshold,
                    timeThreshold = timeThreshold,
                    onAccelerationChange = {
                        accelerationThreshold = it
                        NiuNiuStorage.setAccelerationThreshold(context, it)
                    },
                    onTimeChange = {
                        timeThreshold = it
                        NiuNiuStorage.setTimeThreshold(context, it)
                    },
                    onRestore = {
                        NiuNiuStorage.resetSensitivity(context)
                        accelerationThreshold = NiuNiuStorage.accelerationThreshold(context)
                        timeThreshold = NiuNiuStorage.timeThreshold(context)
                    },
                    onBack = { route = Route.SETTINGS },
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(onPlay: () -> Unit, onRecord: () -> Unit, onChallenge: () -> Unit, onSettings: () -> Unit) {
    ScrollColumn {
        Text("牛牛挤奶🐮", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        FullWidthButton("🥵 开始挤奶", onPlay)
        FullWidthButton("😍 挤奶记录", onRecord)
        FullWidthButton("😶‍🌫️ 奶桶挑战", onChallenge)
        FullWidthButton("🤫 挤奶设置", onSettings)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "项目源自abc2754667876/PlayNewNew\n由weimanmk/PlayNewNew-android-watch二创",
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
        )
    }
}
@Composable
private fun PlayScreen(
    soundEnabled: Boolean,
    accelerationThreshold: Float,
    timeThreshold: Float,
    hasBodySensorsPermission: Boolean,
    requestBodySensorsPermission: () -> Unit,
    onSaveRecord: (PlayRecord) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val soundEnabledState = rememberUpdatedState(soundEnabled)
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var showSummary by rememberSaveable { mutableStateOf(false) }
    var elapsedSeconds by rememberSaveable { mutableIntStateOf(0) }
    var shakeCount by rememberSaveable { mutableIntStateOf(0) }
    var currentHeartRate by rememberSaveable { mutableIntStateOf(0) }
    var startMillis by rememberSaveable { mutableLongStateOf(0L) }
    var summaryFrequency by rememberSaveable { mutableStateOf(0.0) }
    var summaryHeartRate by rememberSaveable { mutableStateOf(0.0) }
    var startTonePlayed by rememberSaveable { mutableStateOf(false) }

    val heartRates = remember { mutableListOf<Int>() }
    val runningState = rememberUpdatedState(isRunning)

    val shakeDetector = remember {
        ShakeDetector(context, onShake = {
            shakeCount += 1
            if (shakeCount >= 3 && !startTonePlayed) {
                startTonePlayed = true
                DeviceFeedback.playStartTone(soundEnabledState.value)
            }
        })
    }
    val heartRateMonitor = remember {
        HeartRateMonitor(context) {
            currentHeartRate = it
            if (runningState.value) heartRates += it
        }
    }
    val hasHeartRateSensor = remember { heartRateMonitor.isSupported() }

    shakeDetector.threshold = accelerationThreshold
    shakeDetector.minIntervalSeconds = timeThreshold

    DisposableEffect(Unit) { onDispose { shakeDetector.stop(); heartRateMonitor.stop() } }

    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        while (isRunning) {
            elapsedSeconds = ((SystemClock.elapsedRealtime() - startMillis) / 1000L).toInt()
            delay(200L)
        }
    }
    LaunchedEffect(isRunning, hasBodySensorsPermission, hasHeartRateSensor) {
        if (!isRunning || !hasBodySensorsPermission || !hasHeartRateSensor) {
            heartRateMonitor.stop()
            return@LaunchedEffect
        }
        heartRateMonitor.start()
    }

    ScrollColumn {
        Header("开始挤奶", onBack)
        val heartRateText = when {
            !hasHeartRateSensor -> "❤️ 设备不支持心率"
            !hasBodySensorsPermission -> "❤️ 未授予心率权限"
            isRunning -> "❤️ $currentHeartRate"
            else -> "❤️ 0"
        }
        Text(heartRateText, fontSize = 14.sp)
        Text(formatDuration(elapsedSeconds), fontSize = 32.sp, fontWeight = FontWeight.Bold)
        FullWidthButton(if (isRunning) "🤒 结束挤奶" else "🥵 开始挤奶") {
            if (isRunning) {
                isRunning = false
                shakeDetector.stop()
                heartRateMonitor.stop()

                val safeElapsed = max(elapsedSeconds, 1)
                val avgHeart = if (heartRates.isEmpty()) 0.0 else heartRates.average()
                val frequency = shakeCount.toDouble() / safeElapsed.toDouble()
                summaryHeartRate = avgHeart
                summaryFrequency = frequency
                showSummary = true
                DeviceFeedback.playEndTone(soundEnabled)

                onSaveRecord(
                    PlayRecord(
                        timestampMillis = System.currentTimeMillis(),
                        spendSeconds = elapsedSeconds,
                        count = shakeCount,
                        frequency = frequency,
                        heartRate = avgHeart,
                    ),
                )
            } else {
                if (!hasBodySensorsPermission) requestBodySensorsPermission()
                isRunning = true
                showSummary = false
                elapsedSeconds = 0
                shakeCount = 0
                currentHeartRate = 0
                heartRates.clear()
                startTonePlayed = false
                startMillis = SystemClock.elapsedRealtime()
                shakeDetector.start()
            }
        }
        Text(if (isRunning) "🍼已挤奶${shakeCount}下" else "👋点击按钮后开始为牛牛挤奶", fontSize = 12.sp)

        if (showSummary) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.surface.copy(alpha = 0.25f))
                    .padding(10.dp),
            ) {
                Text("🥰奶桶已满", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                StatLine("⏱用时", formatDuration(elapsedSeconds))
                StatLine("👋次数", "$shakeCount")
                StatLine("👌频率", "${formatOneDecimal(summaryFrequency)}次/秒")
                StatLine("💗心率", formatOneDecimal(summaryHeartRate))
            }
        }
    }
}

@Composable
private fun RecordScreen(recordsVersion: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val records = remember(recordsVersion) { NiuNiuStorage.loadRecords(context) }
    val weeklyRecords = records.recordsForLastDays(7)
    var tab by rememberSaveable { mutableStateOf(RecordTab.OVERVIEW) }
    var easterTap by rememberSaveable { mutableIntStateOf(0) }

    ScrollColumn {
        Header("挤奶记录", onBack)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallButton("总览", { tab = RecordTab.OVERVIEW }, tab == RecordTab.OVERVIEW)
            SmallButton("日报", { tab = RecordTab.DAILY }, tab == RecordTab.DAILY)
            SmallButton("周报", { tab = RecordTab.WEEKLY }, tab == RecordTab.WEEKLY)
            SmallButton("月报", { tab = RecordTab.MONTHLY }, tab == RecordTab.MONTHLY)
        }

        when (tab) {
            RecordTab.OVERVIEW -> {
                val overview = when {
                    weeklyRecords.isEmpty() -> "🥵急需挤奶"
                    weeklyRecords.size <= 3 -> "😋奶质优良"
                    weeklyRecords.size <= 7 -> "😶奶质堪忧"
                    else -> "😨精尽牛亡"
                }
                Text(overview, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "您本周挤奶${weeklyRecords.size}次，注意适当控制挤奶次数，可有效防止奶源枯竭",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { easterTap += 1 },
                )
                if (easterTap >= 10) {
                    Text("🎉称号彩蛋", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            weeklyRecords.isEmpty() -> "挤奶菜鸟"
                            weeklyRecords.size <= 3 -> "挤奶新手"
                            weeklyRecords.size <= 7 -> "挤奶大师"
                            else -> "精尽人亡"
                        },
                        fontSize = 22.sp,
                    )
                    FullWidthButton("关闭彩蛋") { easterTap = 0 }
                }
            }
            RecordTab.DAILY -> StatsReport("☀挤奶日报", records.recordsForToday(), "您今日未挤奶") { "您今天挤奶${it}次" }
            RecordTab.WEEKLY -> StatsReport("📅挤奶周报", weeklyRecords, "您本周未挤奶") { "您本周挤奶${it}次" }
            RecordTab.MONTHLY -> StatsReport("🌙挤奶月报", records.recordsForLastDays(30), "您本月未挤奶") { "您本月挤奶${it}次" }
        }
    }
}

@Composable
private fun StatsReport(title: String, records: List<PlayRecord>, emptyTip: String, summaryText: (Int) -> String) {
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    if (records.isEmpty()) {
        Text(emptyTip, fontSize = 12.sp)
        return
    }

    Text(summaryText(records.size), fontSize = 12.sp)
    val spend = records.intStats { it.spendSeconds }
    val count = records.intStats { it.count }
    val frequency = records.doubleStats { it.frequency }
    val heart = records.doubleStats { it.heartRate }

    StatLine("⏱最短用时", formatDuration(spend.min))
    StatLine("⏱最长用时", formatDuration(spend.max))
    StatLine("⏱平均用时", formatDuration(spend.average))
    StatLine("👋最低次数", "${count.min}")
    StatLine("👋最高次数", "${count.max}")
    StatLine("👋平均次数", formatOneDecimal(count.average.toDouble()))
    StatLine("👌最低频率", "${formatOneDecimal(frequency.min)}次/秒")
    StatLine("👌最高频率", "${formatOneDecimal(frequency.max)}次/秒")
    StatLine("👌平均频率", "${formatOneDecimal(frequency.average)}次/秒")
    StatLine("💗最低心率", formatOneDecimal(heart.min))
    StatLine("💗最高心率", formatOneDecimal(heart.max))
    StatLine("💗平均心率", formatOneDecimal(heart.average))

    if (title != "☀挤奶日报") {
        records.dailyAverages().forEach {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.surface.copy(alpha = 0.25f))
                    .padding(10.dp),
            ) {
                Text("${it.day} · 挤奶${it.times}次", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                StatLine("⏱平均用时", formatDuration(it.spendAverage.toInt()))
                StatLine("👋平均次数", formatOneDecimal(it.countAverage))
                StatLine("👌平均频率", "${formatOneDecimal(it.frequencyAverage)}次/秒")
                StatLine("💗平均心率", formatOneDecimal(it.heartRateAverage))
            }
        }
    }
}
@Composable
private fun ChallengeScreen(
    soundEnabled: Boolean,
    accelerationThreshold: Float,
    timeThreshold: Float,
    onBack: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(ChallengeTab.MENU) }
    var frequency by rememberSaveable { mutableIntStateOf(1) }

    when (tab) {
        ChallengeTab.MENU -> ScrollColumn {
            Header("奶桶挑战", onBack)
            FullWidthButton("👌频率挑战") { tab = ChallengeTab.FREQ_CONFIG }
            FullWidthButton("🤞边缘挑战") { tab = ChallengeTab.EDGE }
            FullWidthButton("⏰时长挑战") { tab = ChallengeTab.TIME }
        }
        ChallengeTab.FREQ_CONFIG -> FrequencyChallengeConfig(
            frequency = frequency,
            onFrequencyChange = { frequency = it },
            onStart = { tab = ChallengeTab.FREQ_RUN },
            onBack = { tab = ChallengeTab.MENU },
        )
        ChallengeTab.FREQ_RUN -> FrequencyChallengeRun(
            targetFrequency = frequency,
            soundEnabled = soundEnabled,
            accelerationThreshold = accelerationThreshold,
            timeThreshold = timeThreshold,
            onBack = { tab = ChallengeTab.MENU },
        )
        ChallengeTab.EDGE -> EdgeChallengeRun(
            soundEnabled = soundEnabled,
            accelerationThreshold = accelerationThreshold,
            timeThreshold = timeThreshold,
            onBack = { tab = ChallengeTab.MENU },
        )
        ChallengeTab.TIME -> TimeChallengeRun(
            soundEnabled = soundEnabled,
            accelerationThreshold = accelerationThreshold,
            timeThreshold = timeThreshold,
            onBack = { tab = ChallengeTab.MENU },
        )
    }
}

@Composable
private fun FrequencyChallengeConfig(frequency: Int, onFrequencyChange: (Int) -> Unit, onStart: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var preview by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(preview, frequency) {
        if (!preview) return@LaunchedEffect
        val interval = (1000L / frequency.coerceAtLeast(1)).coerceAtLeast(60L)
        while (preview) {
            DeviceFeedback.vibratePulse(context)
            delay(interval)
        }
    }

    ScrollColumn {
        Header("频率挑战", onBack = { preview = false; onBack() })
        Text("目标频率：$frequency", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        NumberAdjustRow(
            valueText = frequency.toString(),
            minText = "1",
            maxText = "30",
            onMinus = { onFrequencyChange((frequency - 1).coerceAtLeast(1)) },
            onPlus = { onFrequencyChange((frequency + 1).coerceAtMost(30)) },
        )
        FullWidthButton(if (preview) "停止感受" else "感受频率") { preview = !preview }
        FullWidthButton("进入挑战") { preview = false; onStart() }
    }
}

@Composable
private fun FrequencyChallengeRun(
    targetFrequency: Int,
    soundEnabled: Boolean,
    accelerationThreshold: Float,
    timeThreshold: Float,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val soundEnabledState = rememberUpdatedState(soundEnabled)
    var countdown by rememberSaveable { mutableIntStateOf(6) }
    var running by rememberSaveable { mutableStateOf(false) }
    var elapsed by rememberSaveable { mutableIntStateOf(0) }
    var shakeCount by rememberSaveable { mutableIntStateOf(0) }
    var startMillis by rememberSaveable { mutableLongStateOf(0L) }
    var lastShakeMillis by rememberSaveable { mutableLongStateOf(0L) }

    val detector = remember {
        ShakeDetector(context, onShake = {
            shakeCount += 1
            lastShakeMillis = SystemClock.elapsedRealtime()
        })
    }
    detector.threshold = accelerationThreshold
    detector.minIntervalSeconds = timeThreshold

    DisposableEffect(Unit) { onDispose { detector.stop() } }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000L)
            countdown -= 1
        }
        running = true
        startMillis = SystemClock.elapsedRealtime()
        lastShakeMillis = startMillis
        detector.start()
    }

    LaunchedEffect(running, targetFrequency) {
        if (!running) return@LaunchedEffect
        val interval = (1000L / targetFrequency.coerceAtLeast(1)).coerceAtLeast(60L)
        while (running) {
            val now = SystemClock.elapsedRealtime()
            elapsed = ((now - startMillis) / 1000L).toInt()
            if (now - lastShakeMillis > 4000L) {
                running = false
                detector.stop()
                DeviceFeedback.playEndTone(soundEnabledState.value)
                break
            }
            DeviceFeedback.vibratePulse(context)
            delay(interval)
        }
    }

    ScrollColumn {
        Header("频率挑战", onBack = { running = false; detector.stop(); onBack() })
        if (countdown > 0) {
            Text("准备开始", fontSize = 18.sp)
            Text("$countdown", fontSize = 64.sp, fontWeight = FontWeight.Bold)
        } else if (running) {
            Text("👌频率挑战开始", fontSize = 18.sp)
            Text(formatDuration(elapsed), fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("🍼已挤奶${shakeCount}下", fontSize = 13.sp)
        } else {
            Text("频率挑战结束", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            StatLine("👋目标频率", "${targetFrequency}下/秒")
            StatLine("⏱实际用时", formatDuration((elapsed - 3).coerceAtLeast(0)))
            StatLine("👋摇晃次数", "$shakeCount")
            FullWidthButton("返回挑战列表", onBack)
        }
    }
}

@Composable
private fun EdgeChallengeRun(soundEnabled: Boolean, accelerationThreshold: Float, timeThreshold: Float, onBack: () -> Unit) {
    val context = LocalContext.current
    val soundEnabledState = rememberUpdatedState(soundEnabled)
    var countdown by rememberSaveable { mutableIntStateOf(6) }
    var running by rememberSaveable { mutableStateOf(false) }
    var rounds by rememberSaveable { mutableIntStateOf(1) }
    var shakeCount by rememberSaveable { mutableIntStateOf(0) }
    var isMilking by rememberSaveable { mutableStateOf(true) }
    var phaseCountdown by rememberSaveable { mutableIntStateOf(10) }
    var noMilkingSec by rememberSaveable { mutableIntStateOf(0) }
    var lastShakeCount by rememberSaveable { mutableIntStateOf(0) }

    val runningState = rememberUpdatedState(running)
    val milkingState = rememberUpdatedState(isMilking)

    val detector = remember {
        ShakeDetector(context, onShake = {
            if (runningState.value && milkingState.value) shakeCount += 1
        })
    }
    detector.threshold = accelerationThreshold
    detector.minIntervalSeconds = timeThreshold

    DisposableEffect(Unit) { onDispose { detector.stop() } }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
        running = true
        detector.start()
    }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        while (running) {
            delay(1000)
            if (isMilking) {
                if (shakeCount == lastShakeCount) noMilkingSec += 1 else {
                    noMilkingSec = 0
                    lastShakeCount = shakeCount
                }
                if (noMilkingSec >= 4) {
                    running = false
                    detector.stop()
                    DeviceFeedback.playEndTone(soundEnabledState.value)
                    break
                }
            }
            phaseCountdown -= 1
            if (phaseCountdown <= 0) {
                if (isMilking) {
                    phaseCountdown = 5
                } else {
                    phaseCountdown = 10
                    rounds += 1
                    noMilkingSec = 0
                    lastShakeCount = shakeCount
                }
                isMilking = !isMilking
            }
        }
    }

    ScrollColumn {
        Header("边缘挑战", onBack = { running = false; detector.stop(); onBack() })
        if (countdown > 0) {
            Text("准备开始", fontSize = 18.sp)
            Text("$countdown", fontSize = 64.sp, fontWeight = FontWeight.Bold)
        } else if (running) {
            Text("🤞第${rounds}轮", fontSize = 14.sp)
            Text(if (isMilking) "现在是：挤奶时间" else "现在是：休息时间", fontSize = 18.sp)
            Text("$phaseCountdown", fontSize = 54.sp, fontWeight = FontWeight.Bold)
            Text("👋已挤奶${shakeCount}下", fontSize = 13.sp)
        } else {
            Text("🤞边缘挑战完成", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("本次坚持了${(rounds - 1).coerceAtLeast(0)}轮", fontSize = 14.sp)
            FullWidthButton("返回挑战列表", onBack)
        }
    }
}

@Composable
private fun TimeChallengeRun(soundEnabled: Boolean, accelerationThreshold: Float, timeThreshold: Float, onBack: () -> Unit) {
    val context = LocalContext.current
    val soundEnabledState = rememberUpdatedState(soundEnabled)
    var countdown by rememberSaveable { mutableIntStateOf(6) }
    var running by rememberSaveable { mutableStateOf(false) }
    var elapsed by rememberSaveable { mutableIntStateOf(0) }
    var shakeCount by rememberSaveable { mutableIntStateOf(0) }
    var startMillis by rememberSaveable { mutableLongStateOf(0L) }
    val runningState = rememberUpdatedState(running)

    val detector = remember {
        ShakeDetector(context, onShake = {
            if (runningState.value) shakeCount += 1
        })
    }
    detector.threshold = accelerationThreshold
    detector.minIntervalSeconds = timeThreshold

    DisposableEffect(Unit) { onDispose { detector.stop() } }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
        running = true
        startMillis = SystemClock.elapsedRealtime()
        detector.start()
    }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        while (running) {
            elapsed = ((SystemClock.elapsedRealtime() - startMillis) / 1000L).toInt()
            delay(200)
        }
    }

    ScrollColumn {
        Header("时长挑战", onBack = { running = false; detector.stop(); onBack() })
        if (countdown > 0) {
            Text("准备开始", fontSize = 18.sp)
            Text("$countdown", fontSize = 64.sp, fontWeight = FontWeight.Bold)
        } else if (running) {
            Text("⏰时长挑战开始", fontSize = 18.sp)
            Text(formatDuration(elapsed), fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("🍼已挤奶${shakeCount}下", fontSize = 13.sp)
            FullWidthButton("结束挑战") {
                running = false
                detector.stop()
                DeviceFeedback.playEndTone(soundEnabledState.value)
            }
        } else {
            Text("时长挑战结束", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            StatLine("⏱挑战用时", formatDuration(elapsed))
            StatLine("👋摇晃次数", "$shakeCount")
            FullWidthButton("返回挑战列表", onBack)
        }
    }
}

@Composable
private fun SettingsScreen(
    soundEnabled: Boolean,
    appVersion: String,
    onToggleSound: () -> Unit,
    onSensitivity: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    var clearConfirm by rememberSaveable { mutableStateOf(false) }
    ScrollColumn {
        Header("挤奶设置", onBack)
        FullWidthButton(if (soundEnabled) "播放音效：开" else "播放音效：关", onToggleSound)
        FullWidthButton("灵敏度设置", onSensitivity)
        FullWidthButton("清除挤奶数据") { clearConfirm = true }
        if (clearConfirm) {
            Text("确认清空全部记录？", color = MaterialTheme.colors.error, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallButton("取消", { clearConfirm = false }, false)
                SmallButton("确认", { clearConfirm = false; onClear() }, true)
            }
        }
        Text("当前版本：$appVersion", fontSize = 12.sp)
    }
}

@Composable
private fun SensitivityScreen(
    accelerationThreshold: Float,
    timeThreshold: Float,
    onAccelerationChange: (Float) -> Unit,
    onTimeChange: (Float) -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var testing by rememberSaveable { mutableStateOf(false) }
    var calibrating by rememberSaveable { mutableStateOf(false) }
    var calibrationSecondsLeft by rememberSaveable { mutableIntStateOf(0) }
    var hasCalibrationSuggestion by rememberSaveable { mutableStateOf(false) }
    var suggestedAccelerationThreshold by rememberSaveable { mutableFloatStateOf(accelerationThreshold) }
    var suggestedTimeThreshold by rememberSaveable { mutableFloatStateOf(timeThreshold) }
    var shakeCount by rememberSaveable { mutableIntStateOf(0) }
    var x by rememberSaveable { mutableFloatStateOf(0f) }
    var y by rememberSaveable { mutableFloatStateOf(0f) }
    var z by rememberSaveable { mutableFloatStateOf(0f) }
    val testingState = rememberUpdatedState(testing)
    val calibratingState = rememberUpdatedState(calibrating)
    val calibrationMagnitudes = remember { mutableListOf<Float>() }
    val calibrationShakeTimestamps = remember { mutableListOf<Long>() }

    val detector = remember {
        ShakeDetector(
            context,
            onShake = {
                if (testingState.value) shakeCount += 1
                if (calibratingState.value) calibrationShakeTimestamps += SystemClock.elapsedRealtime()
            },
            onSensorUpdate = { xx, yy, zz ->
                x = xx
                y = yy
                z = zz
                if (calibratingState.value) {
                    val magnitude = sqrt((xx * xx + yy * yy + zz * zz).toDouble()).toFloat()
                    calibrationMagnitudes += magnitude
                }
            },
        )
    }
    if (calibrating) {
        detector.threshold = 0.35f
        detector.minIntervalSeconds = 0.12f
    } else {
        detector.threshold = accelerationThreshold
        detector.minIntervalSeconds = timeThreshold
    }
    DisposableEffect(Unit) { onDispose { detector.stop() } }
    LaunchedEffect(calibrating) {
        if (!calibrating) return@LaunchedEffect
        calibrationSecondsLeft = 15
        while (calibrating && calibrationSecondsLeft > 0) {
            delay(1000L)
            calibrationSecondsLeft -= 1
        }
        if (!calibrating) return@LaunchedEffect

        calibrating = false
        detector.stop()
        val (recommendedAcceleration, recommendedInterval) = suggestSensitivityBySession(
            magnitudes = calibrationMagnitudes,
            shakeTimestampsMillis = calibrationShakeTimestamps,
            currentAcceleration = accelerationThreshold,
            currentInterval = timeThreshold,
        )
        suggestedAccelerationThreshold = recommendedAcceleration
        suggestedTimeThreshold = recommendedInterval
        hasCalibrationSuggestion = true
    }

    ScrollColumn {
        Header("灵敏度设置", onBack = {
            testing = false
            calibrating = false
            detector.stop()
            onBack()
        })
        Text("加速度敏感值(${formatTwoDecimal(accelerationThreshold.toDouble())})")
        NumberAdjustRow(formatTwoDecimal(accelerationThreshold.toDouble()), "0.1", "5.0", {
            onAccelerationChange((accelerationThreshold - 0.1f).coerceAtLeast(0.1f))
        }, {
            onAccelerationChange((accelerationThreshold + 0.1f).coerceAtMost(5.0f))
        })

        Text("时间敏感值(${formatTwoDecimal(timeThreshold.toDouble())}s)")
        NumberAdjustRow(formatTwoDecimal(timeThreshold.toDouble()), "0.01", "1.00", {
            onTimeChange((timeThreshold - 0.01f).coerceAtLeast(0.01f))
        }, {
            onTimeChange((timeThreshold + 0.01f).coerceAtMost(1.0f))
        })

        FullWidthButton(
            if (calibrating) "自动校准中(${calibrationSecondsLeft}s)" else "自动校准(15秒)",
        ) {
            if (calibrating) return@FullWidthButton
            testing = false
            shakeCount = 0
            hasCalibrationSuggestion = false
            calibrationMagnitudes.clear()
            calibrationShakeTimestamps.clear()
            calibrating = true
            detector.start()
        }
        Text("校准时按你真实节奏连续挤奶 15 秒", fontSize = 11.sp)

        if (hasCalibrationSuggestion) {
            Text(
                "推荐：加速度${formatTwoDecimal(suggestedAccelerationThreshold.toDouble())}，时间${formatTwoDecimal(suggestedTimeThreshold.toDouble())}s",
                fontSize = 11.sp,
            )
            FullWidthButton("应用推荐值") {
                onAccelerationChange(suggestedAccelerationThreshold)
                onTimeChange(suggestedTimeThreshold)
                hasCalibrationSuggestion = false
            }
        }

        FullWidthButton(if (testing) "结束测试" else "开始测试") {
            if (calibrating) return@FullWidthButton
            if (testing) {
                testing = false
                shakeCount = 0
                detector.stop()
            } else {
                testing = true
                shakeCount = 0
                detector.start()
            }
        }

        Text("🍼已挤奶${shakeCount}下")
        Text("x: ${formatTwoDecimal(x.toDouble())}")
        Text("y: ${formatTwoDecimal(y.toDouble())}")
        Text("z: ${formatTwoDecimal(z.toDouble())}")

        FullWidthButton("恢复默认值") {
            testing = false
            calibrating = false
            shakeCount = 0
            hasCalibrationSuggestion = false
            detector.stop()
            onRestore()
        }
    }
}

private fun suggestSensitivityBySession(
    magnitudes: List<Float>,
    shakeTimestampsMillis: List<Long>,
    currentAcceleration: Float,
    currentInterval: Float,
): Pair<Float, Float> {
    if (magnitudes.size < 40) return currentAcceleration to currentInterval

    val sortedMagnitudes = magnitudes.sorted()
    val p50 = percentile(sortedMagnitudes, 0.50f)
    val p90 = percentile(sortedMagnitudes, 0.90f)
    val recommendedAcceleration = (p50 + (p90 - p50) * 0.45f).coerceIn(0.8f, 4.0f)

    if (shakeTimestampsMillis.size < 3) return recommendedAcceleration to currentInterval
    val intervals = shakeTimestampsMillis
        .zipWithNext { a, b -> ((b - a).toFloat() / 1000f).coerceAtLeast(0.01f) }
        .filter { it in 0.05f..2.0f }
    if (intervals.isEmpty()) return recommendedAcceleration to currentInterval

    val medianInterval = median(intervals)
    val recommendedInterval = (medianInterval * 0.55f).coerceIn(0.18f, 1.0f)
    return recommendedAcceleration to recommendedInterval
}

private fun percentile(sortedValues: List<Float>, ratio: Float): Float {
    if (sortedValues.isEmpty()) return 0f
    val index = (ratio.coerceIn(0f, 1f) * (sortedValues.size - 1)).toInt()
    return sortedValues[index]
}

private fun median(values: List<Float>): Float {
    if (values.isEmpty()) return 0f
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 0) {
        (sorted[mid - 1] + sorted[mid]) / 2f
    } else {
        sorted[mid]
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SmallButton("返回", onBack, false)
        Spacer(Modifier.width(6.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScrollColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun FullWidthButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun SmallButton(text: String, onClick: () -> Unit, selected: Boolean) {
    Button(onClick = onClick, modifier = Modifier.height(36.dp)) {
        Text(text, fontSize = 11.sp, color = MaterialTheme.colors.onPrimary.copy(alpha = if (selected) 1f else 0.65f))
    }
}

@Composable
private fun NumberAdjustRow(
    valueText: String,
    minText: String,
    maxText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        SmallButton("-", onMinus, false)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(valueText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("$minText ~ $maxText", fontSize = 10.sp)
        }
        SmallButton("+", onPlus, false)
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 13.sp)
    }
}

private fun formatDuration(seconds: Int): String {
    val s = max(seconds, 0)
    return "%02d:%02d".format(s / 60, s % 60)
}

private fun formatOneDecimal(value: Double): String = "%.1f".format(value)
private fun formatTwoDecimal(value: Double): String = "%.2f".format(value)

private fun Context.hasBodySensorsPermission(): Boolean {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED
}


private fun Context.appVersionName(): String {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName
        }
    }.getOrNull() ?: "未知"
}

@Preview(showSystemUi = true)
@Composable
private fun PreviewHome() {
    NiuniuTheme { HomeScreen({}, {}, {}, {}) }
}
