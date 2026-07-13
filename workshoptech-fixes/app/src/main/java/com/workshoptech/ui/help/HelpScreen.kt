package com.workshoptech.ui.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.workshoptech.ui.common.WorkshopTopBar
import com.workshoptech.ui.theme.*

private data class FaqItem(val question: String, val answer: String)

private val FAQS = listOf(
    FaqItem("كيف أضيف سيارة جديدة؟",
        "من الصفحة الرئيسية اضغط على زر + أو اذهب لقائمة الملفات ثم اضغط إضافة. أدخل رقم اللوحة وبيانات العميل والسيارة."),
    FaqItem("كيف يعمل نظام التفتيش T1-T6؟",
        "النظام يتتبع 6 نقاط تفتيش في دورة حياة الملف: T1 استلام، T2 بعد السمكرة، T3 تسليم للدهان، T4 قبل الرش، T5 بعد الدهان، T6 التسليم النهائي. كل نقطة لها قائمة تفتيش مختلفة."),
    FaqItem("كيف أُطابق لون الدهان؟",
        "اذهب لشاشة مطابقة الألوان، أدخل كود اللون، حدد عمر السيارة ومستوى التعرض للشمس، ثم اضغط 'احسب الخلطة'. سيقترح النظام نسبة التعديل وقيمة Delta E."),
    FaqItem("ما هو Delta E؟",
        "Delta E هو مقياس الفرق بين لونين. القيم: أقل من 2.0 = ممتاز، 2.0-3.5 = مقبول، أكثر من 3.5 = غير مقبول ويجب إعادة الخلط."),
    FaqItem("كيف يقرأ النظام اللوحة تلقائياً؟",
        "عند تصوير اللوحة، يستخدم النظام ML Kit لقراءة النص، ثم يُطابق النتيجة مع أنماط 22 دولة عربية لتحديد الدولة تلقائياً. دقة القراءة تتجاوز 85% في الإضاءة الجيدة."),
    FaqItem("ما هي متطلبات الصورة للحصول على أفضل تحليل؟",
        "• إضاءة كافية وغير مباشرة\n• لا انعكاسات شمس قوية\n• مسافة 50-150 سم من الضرر\n• صورة ثابتة (لا اهتزاز)\n• زاوية مستقيمة 90° للضرر"),
    FaqItem("هل يعمل التطبيق بدون إنترنت؟",
        "نعم، جميع وظائف التحليل الأساسية (قراءة اللوحة، تحليل الضرر، نقاط التفتيش، مطابقة الألوان) تعمل بالكامل بدون إنترنت. البيانات تُحفظ محلياً."),
    FaqItem("كيف أُصدّر تقرير التسليم؟",
        "من تفاصيل الملف، عند اكتمال نقطة التفتيش T6، يتوفر زر 'تقرير التسليم' الذي يُولّد تقريراً كاملاً بالصور وتوقيعات الفنيين والعميل."),
    FaqItem("كيف أُدير المخزون؟",
        "من قائمة 'المخزون' يمكنك إضافة الأصناف وتحديد الكميات والحد الأدنى. التطبيق سيُنبهك تلقائياً عند انخفاض مخزون أي صنف تحت الحد المحدد."),
    FaqItem("ما الدول المدعومة؟",
        "يدعم النظام 22 دولة عربية: ليبيا، مصر، السعودية، الإمارات، الكويت، قطر، البحرين، عُمان، الأردن، لبنان، سوريا، العراق، اليمن، فلسطين، السودان، تونس، الجزائر، المغرب، موريتانيا، الصومال، جيبوتي، جزر القمر.")
)

private data class HelpSection(val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val items: List<String>)
private val QUICK_TIPS = listOf(
    HelpSection(Icons.Default.CameraAlt, "تصوير احترافي", listOf("صوّر في إضاءة طبيعية", "تجنب الشمس المباشرة", "ثبّت اليد جيداً")),
    HelpSection(Icons.Default.CheckCircle, "نقاط التفتيش", listOf("أكمل كل بنود نقطة T قبل الانتقال", "التوثيق بالصور إلزامي", "الملاحظات مهمة")),
    HelpSection(Icons.Default.Palette, "مطابقة الألوان", listOf("اختبر دائماً على بطاقة أولاً", "قارن في ضوء الشمس والمصباح", "سجّل كود الخلطة")),
)

@Composable
fun HelpScreen(onNavigateBack: () -> Unit) {
    var expanded by remember { mutableStateOf<Int?>(null) }

    Scaffold(topBar = { WorkshopTopBar("المساعدة", onBack = onNavigateBack) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding() + 12.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Quick tips
            item {
                Text("نصائح سريعة", style = MaterialTheme.typography.titleMedium, color = Blue600)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QUICK_TIPS.forEach { tip ->
                        Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(tip.icon, null, tint = Blue600, modifier = Modifier.size(22.dp))
                                Text(tip.title, style = MaterialTheme.typography.labelMedium)
                                tip.items.forEach { hint ->
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text("•", Modifier.padding(end = 4.dp, top = 2.dp), style = MaterialTheme.typography.labelSmall, color = Blue600)
                                        Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // FAQ header
            item {
                Spacer(Modifier.height(4.dp))
                Text("الأسئلة الشائعة", style = MaterialTheme.typography.titleMedium, color = Blue600)
            }

            // FAQ items
            items(FAQS.indices.toList()) { idx ->
                val faq = FAQS[idx]
                val isOpen = expanded == idx
                Card(
                    shape     = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(if (isOpen) 3.dp else 1.dp),
                    modifier  = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().clickable { expanded = if (isOpen) null else idx }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(if (isOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Blue600, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(faq.question, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        }
                        AnimatedVisibility(visible = isOpen) {
                            Column {
                                HorizontalDivider(Modifier.padding(horizontal = 14.dp))
                                Text(faq.answer, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = MaterialTheme.typography.bodySmall.lineHeight)
                            }
                        }
                    }
                }
            }

            // Contact
            item {
                Spacer(Modifier.height(4.dp))
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Blue50)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SupportAgent, null, tint = Blue700, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("هل تحتاج مساعدة إضافية؟", style = MaterialTheme.typography.titleSmall, color = Blue700)
                            Text("تواصل مع فريق الدعم", style = MaterialTheme.typography.bodySmall, color = Blue600)
                        }
                    }
                }
            }
        }
    }
}
