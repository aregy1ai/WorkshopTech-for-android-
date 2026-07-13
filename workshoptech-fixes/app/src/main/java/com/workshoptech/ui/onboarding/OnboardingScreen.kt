package com.workshoptech.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.workshoptech.ui.theme.*
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val tint: androidx.compose.ui.graphics.Color
)

private val pages = listOf(
    OnboardingPage(Icons.Default.CameraAlt,   "تصوير ذكي",          "صوّر السيارة وسيحلل النظام الأضرار تلقائياً بالذكاء الاصطناعي",           Blue600),
    OnboardingPage(Icons.Default.Psychology,   "تشخيص دقيق",         "7 طبقات من الذكاء الاصطناعي تكتشف الضرر وتقدر التكلفة في ثوانٍ",           Orange600),
    OnboardingPage(Icons.Default.CheckCircle,  "6 نقاط تفتيش",       "نظام جودة متكامل من T1 إلى T6 يضمن أعلى معايير السمكرة والدهان",          Green700),
    OnboardingPage(Icons.Default.Palette,      "مطابقة ألوان",        "قاعدة بيانات 500+ لون وحساسية Delta-E لمطابقة مثالية في كل مرة",            Blue500),
    OnboardingPage(Icons.Default.Language,     "22 دولة عربية",       "يدعم لوحات جميع الدول العربية ويتعرف عليها تلقائياً",                       Orange700)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onOnboardingFinished: () -> Unit) {
    val pager  = rememberPagerState { pages.size }
    val scope  = rememberCoroutineScope()
    val isLast = pager.currentPage == pages.lastIndex

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onOnboardingFinished) { Text("تخطى") }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val p = pages[page]
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier.size(140.dp).clip(CircleShape).background(p.tint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(p.icon, null, Modifier.size(72.dp), tint = p.tint)
                }
                Spacer(Modifier.height(32.dp))
                Text(p.title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(p.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = MaterialTheme.typography.bodyMedium.lineHeight)
            }
        }

        // Dots
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center) {
            pages.forEachIndexed { i, _ ->
                val active = i == pager.currentPage
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(if (active) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (active) Blue600 else Gray400)
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            if (pager.currentPage > 0) {
                OutlinedButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) { Text("السابق") }
            } else { Spacer(Modifier.size(1.dp)) }

            Button(
                onClick = {
                    if (isLast) onOnboardingFinished()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Blue600)
            ) {
                Text(if (isLast) "ابدأ الآن" else "التالي")
            }
        }
    }
}
