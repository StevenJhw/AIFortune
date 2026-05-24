package com.fortune.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.ai.ui.theme.MysticDarkPurple
import com.fortune.ai.ui.theme.MysticGold
import com.fortune.ai.ui.theme.MysticLightGold
import com.fortune.ai.ui.theme.MysticPurple
import com.fortune.ai.ui.theme.SectionDivider
import com.fortune.ai.ui.theme.TextPrimary
import com.fortune.ai.ui.theme.TextSecondary

@Composable
fun FortuneTextContent(
    rawText: String,
    modifier: Modifier = Modifier
) {
    val sections = remember(rawText) { parseFortuneText(rawText) }
    val groups = remember(sections) { groupSectionsByHeader(sections) }

    if (groups.size > 1) {
        Column(
            modifier = modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groups.forEachIndexed { index, group ->
                CollapsibleFortuneSection(
                    title = group.title,
                    icon = getFortuneIcon(index),
                    initiallyExpanded = index == 0
                ) {
                    RenderSections(group.sections)
                }
            }
        }
    } else {
        Column(
            modifier = modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sections.forEach { section ->
                RenderSection(section)
            }
        }
    }
}

@Composable
private fun RenderSections(sections: List<ParsedSection>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        sections.forEach { section -> RenderSection(section) }
    }
}

@Composable
private fun RenderSection(section: ParsedSection) {
    when (section.type) {
        SectionType.HEADER -> FortuneHeader(section.content, section.level)
        SectionType.PARAGRAPH -> FortuneParagraph(section.content)
        SectionType.NUMBERED_LIST -> FortuneNumberedList(section.content)
        SectionType.BULLET_LIST -> FortuneBulletList(section.content)
        SectionType.SEPARATOR -> FortuneSeparator()
        SectionType.BOLD_PARAGRAPH -> FortuneBoldParagraph(section.content)
        SectionType.QUOTE -> FortuneQuote(section.content)
    }
}

@Composable
private fun CollapsibleFortuneSection(
    title: String,
    icon: String = "✦",
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(300, easing = EaseOutCubic),
        label = "rotation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MysticDarkPurple),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MysticGold,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "›",
                    fontSize = 20.sp,
                    color = MysticGold.copy(alpha = 0.7f),
                    modifier = Modifier.rotate(rotationAngle)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(350, easing = EaseOutCubic)) + fadeIn(tween(350)),
                exit = shrinkVertically(tween(250, easing = EaseInCubic)) + fadeOut(tween(200))
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(color = SectionDivider, modifier = Modifier.padding(bottom = 12.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun FortuneHeader(text: String, level: Int) {
    Spacer(modifier = Modifier.height(if (level == 1) 16.dp else 8.dp))
    Text(
        text = text,
        style = when (level) {
            1 -> MaterialTheme.typography.headlineMedium
            2 -> MaterialTheme.typography.titleLarge
            else -> MaterialTheme.typography.titleMedium
        },
        color = MysticGold,
        fontWeight = FontWeight.Bold
    )
    if (level <= 2) {
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(if (level == 1) 60.dp else 40.dp)
                .height(2.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(MysticGold, MysticGold.copy(alpha = 0f))
                    )
                )
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun FortuneParagraph(text: String) {
    Text(
        text = parseInlineStyles(text),
        style = MaterialTheme.typography.bodyLarge,
        color = TextPrimary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun FortuneBoldParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MysticLightGold,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun FortuneNumberedList(content: String) {
    val items = content.split("\n")
    Column(modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MysticGold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${index + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MysticGold
                    )
                }
                Text(
                    text = parseInlineStyles(item),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FortuneBulletList(content: String) {
    val items = content.split("\n")
    Column(modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    "◆",
                    fontSize = 8.sp,
                    color = MysticGold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = parseInlineStyles(item),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FortuneSeparator() {
    Spacer(modifier = Modifier.height(12.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MysticGold.copy(alpha = 0.4f),
                        MysticGold.copy(alpha = 0.6f),
                        MysticGold.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                )
            )
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun FortuneQuote(text: String) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(MysticGold, MysticGold.copy(alpha = 0.3f))
                    ),
                    shape = RoundedCornerShape(2.dp)
                )
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun parseInlineStyles(text: String) = buildAnnotatedString {
    var remaining = text
    while (remaining.isNotEmpty()) {
        when {
            remaining.startsWith("**") -> {
                val end = remaining.indexOf("**", startIndex = 2)
                if (end > 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MysticLightGold)) {
                        append(remaining.substring(2, end))
                    }
                    remaining = remaining.substring(end + 2)
                } else {
                    append("**")
                    remaining = remaining.substring(2)
                }
            }
            remaining.startsWith("*") -> {
                val end = remaining.indexOf("*", startIndex = 1)
                if (end > 0) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = TextSecondary)) {
                        append(remaining.substring(1, end))
                    }
                    remaining = remaining.substring(end + 1)
                } else {
                    append("*")
                    remaining = remaining.substring(1)
                }
            }
            else -> {
                val nextSpecial = remaining.indexOfFirst { it == '*' }
                if (nextSpecial > 0) {
                    append(remaining.substring(0, nextSpecial))
                    remaining = remaining.substring(nextSpecial)
                } else {
                    append(remaining)
                    remaining = ""
                }
            }
        }
    }
}

private data class SectionGroup(
    val title: String,
    val sections: List<ParsedSection>
)

private fun groupSectionsByHeader(sections: List<ParsedSection>): List<SectionGroup> {
    val groups = mutableListOf<SectionGroup>()
    var currentTitle = "总论"
    var currentSections = mutableListOf<ParsedSection>()

    sections.forEach { section ->
        if (section.type == SectionType.HEADER && section.level <= 2) {
            if (currentSections.isNotEmpty()) {
                groups.add(SectionGroup(currentTitle, currentSections.toList()))
            }
            currentTitle = section.content
            currentSections = mutableListOf()
        } else {
            currentSections.add(section)
        }
    }
    if (currentSections.isNotEmpty()) {
        groups.add(SectionGroup(currentTitle, currentSections.toList()))
    }
    return groups
}

private fun getFortuneIcon(index: Int): String {
    val icons = listOf("☯", "✦", "☽", "★", "◈", "❋", "✧", "⚝", "❂", "✺")
    return icons[index % icons.size]
}
