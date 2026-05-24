package com.fortune.ai.ui.components

data class ParsedSection(
    val type: SectionType,
    val content: String,
    val level: Int = 0
)

enum class SectionType {
    HEADER, PARAGRAPH, NUMBERED_LIST, BULLET_LIST, SEPARATOR, BOLD_PARAGRAPH, QUOTE
}

fun parseFortuneText(raw: String): List<ParsedSection> {
    val sections = mutableListOf<ParsedSection>()
    val lines = raw.split("\n")
    var i = 0

    while (i < lines.size) {
        val line = lines[i].trim()
        when {
            line.isBlank() -> { i++; continue }

            line.startsWith("###") -> {
                sections.add(ParsedSection(SectionType.HEADER, line.removePrefix("###").trim(), level = 3))
            }
            line.startsWith("##") -> {
                sections.add(ParsedSection(SectionType.HEADER, line.removePrefix("##").trim(), level = 2))
            }
            line.startsWith("#") && line.length > 1 && line[1] != '#' -> {
                sections.add(ParsedSection(SectionType.HEADER, line.removePrefix("#").trim(), level = 1))
            }

            line.startsWith("【") && line.contains("】") -> {
                val content = line.removePrefix("【").substringBefore("】")
                sections.add(ParsedSection(SectionType.HEADER, content, level = 2))
                val remainder = line.substringAfter("】").trim()
                if (remainder.isNotBlank()) {
                    sections.add(ParsedSection(SectionType.PARAGRAPH, remainder))
                }
            }
            line.matches(Regex("^[一二三四五六七八九十]+[、.].+")) -> {
                val content = line.replace(Regex("^[一二三四五六七八九十]+[、.]\\s*"), "")
                sections.add(ParsedSection(SectionType.HEADER, content, level = 2))
            }

            line.matches(Regex("^[-*=]{3,}$")) -> {
                sections.add(ParsedSection(SectionType.SEPARATOR, ""))
            }

            line.matches(Regex("^\\d+[.)].+")) -> {
                val listItems = mutableListOf(line.replace(Regex("^\\d+[.)]+\\s*"), ""))
                while (i + 1 < lines.size && lines[i + 1].trim().matches(Regex("^\\d+[.)].+"))) {
                    i++
                    listItems.add(lines[i].trim().replace(Regex("^\\d+[.)]+\\s*"), ""))
                }
                sections.add(ParsedSection(SectionType.NUMBERED_LIST, listItems.joinToString("\n")))
            }

            line.startsWith("**") && line.endsWith("**") -> {
                sections.add(ParsedSection(SectionType.BOLD_PARAGRAPH, line.removeSurrounding("**")))
            }

            line.matches(Regex("^[-*•]\\s+.+")) -> {
                val listItems = mutableListOf(line.replace(Regex("^[-*•]+\\s*"), ""))
                while (i + 1 < lines.size && lines[i + 1].trim().matches(Regex("^[-*•]\\s+.+"))) {
                    i++
                    listItems.add(lines[i].trim().replace(Regex("^[-*•]+\\s*"), ""))
                }
                sections.add(ParsedSection(SectionType.BULLET_LIST, listItems.joinToString("\n")))
            }

            line.startsWith(">") -> {
                val quoteLines = mutableListOf(line.removePrefix(">").trim())
                while (i + 1 < lines.size && lines[i + 1].trim().startsWith(">")) {
                    i++
                    quoteLines.add(lines[i].trim().removePrefix(">").trim())
                }
                sections.add(ParsedSection(SectionType.QUOTE, quoteLines.joinToString("\n")))
            }

            else -> {
                val paragraph = StringBuilder(line)
                while (i + 1 < lines.size) {
                    val next = lines[i + 1].trim()
                    if (next.isBlank() || next.startsWith("#") || next.startsWith("【") ||
                        next.matches(Regex("^[-*=]{3,}$")) || next.matches(Regex("^\\d+[.)].+")) ||
                        next.matches(Regex("^[-*•]\\s+.+")) || next.startsWith(">") ||
                        next.matches(Regex("^[一二三四五六七八九十]+[、.].+")) ||
                        (next.startsWith("**") && next.endsWith("**"))
                    ) break
                    i++
                    paragraph.append("\n").append(next)
                }
                sections.add(ParsedSection(SectionType.PARAGRAPH, paragraph.toString()))
            }
        }
        i++
    }
    return sections
}
