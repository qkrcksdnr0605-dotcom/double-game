package com.example.chat   // ★ 프로젝트 경로에 맞게 유지!

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ------------------ 랭킹 데이터 ------------------
data class RankEntry(
    val label: String,
    val score: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BalloonGameScreen()
                }
            }
        }
    }
}

@Composable
fun BalloonGameScreen() {

    // ------------------ 점수 / 콤보 ------------------
    var score by rememberSaveable { mutableStateOf(0) }
    var combo by rememberSaveable { mutableStateOf(0) }
    var maxCombo by rememberSaveable { mutableStateOf(0) }

    // ------------------ 판정 메시지 ------------------
    var lastJudge by remember { mutableStateOf<String?>(null) }

    // ------------------ 더블클릭 콤보 ------------------
    var lastClickTime by remember { mutableStateOf(0L) }
    val doubleClickThreshold = 200L   // ★ 200ms 안에 두 번 누르면 콤보

    // ------------------ 박자 시각화 ------------------
    val bpm = 120
    val beatIntervalMs = (60000f / bpm).toLong()

    val infiniteTransition = rememberInfiniteTransition(label = "beat")
    val beatScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = beatIntervalMs.toInt()),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beatScale"
    )

    // ------------------ 제한시간 ------------------
    val totalTime = 30  // ★ 30초 제한
    var timeLeft by rememberSaveable { mutableStateOf(totalTime) }
    var isRunning by remember { mutableStateOf(true) }

    // 시간 감소 타이머
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            // 시간 종료 처리
            if (timeLeft <= 0) {
                isRunning = false
                lastJudge = "⏰ 시간 종료!"
            }
        }
    }

    // ------------------ 랭킹 데이터 ------------------
    var rankEntries by remember { mutableStateOf(listOf<RankEntry>()) }
    var playCount by remember { mutableStateOf(1) }

    // ------------------ 더블클릭 콤보 로직 ------------------
    fun onBalloonClick() {
        if (!isRunning || timeLeft <= 0) return

        val now = System.currentTimeMillis()
        val diff = now - lastClickTime

        if (diff in 1..doubleClickThreshold) {
            // === 더블클릭 성공 ===
            combo += 1
            if (combo > maxCombo) maxCombo = combo

            val gained = 10 + (combo * 5)
            score += gained
            lastJudge = "🔥 더블클릭 콤보! +$gained"
        } else {
            // === 단일 클릭 ===
            combo = 0
            score += 5
            lastJudge = "🙂 단일 클릭 +5"
        }

        lastClickTime = now
    }

    // =================== UI ===================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        // ---------------- 상단 UI ----------------
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("풍선 더블클릭 게임", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(4.dp))

            Text(
                text = "남은 시간: ${timeLeft}s",
                fontSize = 18.sp,
                color = if (timeLeft > 5) Color.Black else Color.Red,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            // 박자 애니메이션 원
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .scale(beatScale)
                        .background(Color.Red, CircleShape)
                )
            }
            Text(
                text = "⬆ 이 박자에 더블클릭!",
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("점수: $score", fontSize = 20.sp)
                Text("콤보: $combo", fontSize = 20.sp)
            }

            Text("최대 콤보: $maxCombo", fontSize = 15.sp, color = Color.Gray)

            lastJudge?.let {
                Text(
                    text = it,
                    fontSize = 16.sp,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // ---------------- 풍선 영역 ----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            BalloonArea(
                enabled = isRunning && timeLeft > 0,
                onClick = { onBalloonClick() }
            )
        }

        // ---------------- 하단 UI (랭킹 + 버튼) ----------------
        Column {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        if (score > 0) {
                            val newEntry = RankEntry("플레이 $playCount", score)
                            playCount++
                            rankEntries = (rankEntries + newEntry)
                                .sortedByDescending { it.score }
                                .take(5)
                        }
                    },
                    enabled = score > 0
                ) {
                    Text("점수 저장")
                }

                OutlinedButton(
                    onClick = {
                        // 전체 초기화
                        score = 0
                        combo = 0
                        maxCombo = 0
                        lastJudge = null
                        lastClickTime = 0L
                        timeLeft = totalTime
                        isRunning = true
                    }
                ) {
                    Text("다시 시작")
                }
            }

            Spacer(Modifier.height(10.dp))

            Text("랭킹 (TOP 5)", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(6.dp))

            if (rankEntries.isEmpty()) {
                Text("저장된 점수가 없습니다.", color = Color.Gray)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    rankEntries.forEachIndexed { idx, entry ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${idx + 1}위 - ${entry.label}")
                            Text("${entry.score}점")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BalloonArea(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    var clicked by remember { mutableStateOf(false) }

    val clickScale by animateFloatAsState(
        targetValue = if (clicked) 1.12f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "balloonClickScale"
    )

    LaunchedEffect(clicked) {
        if (clicked) {
            delay(120)
            clicked = false
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.bal), // ★ bal.jpeg
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .scale(clickScale)
                .clickable(enabled = enabled) {
                    clicked = true
                    onClick()
                }
        )
    }
}
