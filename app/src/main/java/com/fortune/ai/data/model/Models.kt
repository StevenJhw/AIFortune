package com.fortune.ai.data.model

data class UserProfile(
    val name: String = "",
    val gender: String = "",
    val birthDate: String = "",
    val birthTime: String = "",
    val birthCity: String = "",
    val bloodType: String = ""
)

enum class FortuneCategory(val label: String) {
    CHINESE("中式"),
    WESTERN("西式"),
    INTERACTIVE("问一卦")
}

enum class FortuneMethod(
    val displayName: String,
    val category: FortuneCategory,
    val needsQuestion: Boolean = false,
    val needsInteraction: Boolean = false
) {
    // Chinese - auto
    BAZI("八字算命", FortuneCategory.CHINESE),
    ZIWEI("紫微斗数", FortuneCategory.CHINESE),
    NAME_STUDY("姓名学", FortuneCategory.CHINESE),
    ZODIAC("生肖运势", FortuneCategory.CHINESE),
    QIMEN("奇门遁甲", FortuneCategory.CHINESE),

    // Western - auto
    ASTROLOGY("星座占星", FortuneCategory.WESTERN),
    NATAL_CHART("星盘解读", FortuneCategory.WESTERN),
    NUMEROLOGY("数秘术", FortuneCategory.WESTERN),
    VEDIC("吠陀占星", FortuneCategory.WESTERN),
    HUMAN_DESIGN("人类图", FortuneCategory.WESTERN),
    MAYAN("玛雅历", FortuneCategory.WESTERN),
    CELTIC_TREE("凯尔特树历", FortuneCategory.WESTERN),
    BLOOD_TYPE("血型性格", FortuneCategory.WESTERN),

    // Interactive
    TAROT("塔罗牌", FortuneCategory.INTERACTIVE, needsQuestion = true, needsInteraction = true),
    LIUYAO("六爻占卜", FortuneCategory.INTERACTIVE, needsQuestion = true, needsInteraction = true),
    MEIHUA("梅花易数", FortuneCategory.INTERACTIVE, needsQuestion = true, needsInteraction = true),
    CEZI("测字", FortuneCategory.INTERACTIVE, needsQuestion = true, needsInteraction = true),
    RUNES("卢恩符文", FortuneCategory.INTERACTIVE, needsQuestion = true, needsInteraction = true),
    DICE("骰子占卜", FortuneCategory.INTERACTIVE, needsQuestion = true, needsInteraction = true),
    PENDULUM("灵摆", FortuneCategory.INTERACTIVE, needsQuestion = true, needsInteraction = true);

    companion object {
        fun autoMethods() = entries.filter { !it.needsQuestion }
        fun interactiveMethods() = entries.filter { it.needsQuestion }
    }
}

data class FortuneResult(
    val method: FortuneMethod,
    val summary: String = "",
    val plainSummary: String = "",
    val detail: String = "",
    val isLoading: Boolean = false,
    val isComplete: Boolean = false
)
