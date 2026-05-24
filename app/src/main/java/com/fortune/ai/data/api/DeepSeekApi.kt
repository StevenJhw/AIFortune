package com.fortune.ai.data.api

import com.fortune.ai.BuildConfig
import com.fortune.ai.data.model.FortuneMethod
import com.fortune.ai.data.model.UserProfile
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class DeepSeekApi {

    private val client = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val gson = Gson()
    private val baseUrl = "https://api.deepseek.com/v1/chat/completions"

    private fun getApiKey(): String = BuildConfig.DEEPSEEK_API_KEY

    suspend fun divine(method: FortuneMethod, profile: UserProfile, question: String? = null, extra: String? = null): String {
        return withContext(Dispatchers.IO) {
            val systemPrompt = buildSystemPrompt(method)
            val userPrompt = buildUserPrompt(method, profile, question, extra)
            callApi(systemPrompt, userPrompt)
        }
    }

    suspend fun generateSummary(allResults: Map<FortuneMethod, String>, profile: UserProfile): String {
        return withContext(Dispatchers.IO) {
            val systemPrompt = """你是一位学贯中西的命理宗师，精通八字、紫微斗数、西方占星、数秘术、人类图等多种体系。
你的任务是综合多种命理体系的分析结果，找出交叉印证的共同点，给出一份权威的综合总评。
语言风格：沉稳大气，像一位阅人无数的老师傅，一针见血但不危言耸听。"""

            val userPrompt = buildSummaryPrompt(allResults, profile)
            callApi(systemPrompt, userPrompt)
        }
    }

    suspend fun chat(method: FortuneMethod, context: String, userQuestion: String): String {
        return withContext(Dispatchers.IO) {
            val systemPrompt = buildSystemPrompt(method) + "\n\n以下是之前的解读结果，用户现在要追问：\n$context"
            callApiWithMessages(listOf(
                Message("system", systemPrompt),
                Message("user", userQuestion)
            ))
        }
    }

    private fun callApiWithMessages(messages: List<Message>): String {
        val requestBody = ChatRequest(
            model = "deepseek-chat",
            messages = messages,
            temperature = 0.8,
            maxTokens = 4096
        )

        val json = gson.toJson(requestBody)
        return executeWithRetry(json)
    }

    private fun callApi(systemMessage: String, userMessage: String): String {
        val requestBody = ChatRequest(
            model = "deepseek-chat",
            messages = listOf(
                Message("system", systemMessage),
                Message("user", userMessage)
            ),
            temperature = 0.8,
            maxTokens = 4096
        )

        val json = gson.toJson(requestBody)
        return executeWithRetry(json)
    }

    private fun executeWithRetry(json: String, maxRetries: Int = 3): String {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                val request = Request.Builder()
                    .url(baseUrl)
                    .addHeader("Authorization", "Bearer ${getApiKey()}")
                    .addHeader("Content-Type", "application/json")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: throw Exception("Empty response")

                    if (response.code == 429 || response.code >= 500) {
                        throw RetryableException("API error ${response.code}: $body")
                    }

                    if (!response.isSuccessful) {
                        throw Exception("API error ${response.code}: $body")
                    }

                    val chatResponse = gson.fromJson(body, ChatResponse::class.java)
                    return chatResponse.choices.firstOrNull()?.message?.content ?: "无法获取结果"
                }
            } catch (e: RetryableException) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    Thread.sleep((attempt + 1) * 2000L)
                }
            } catch (e: java.io.IOException) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    Thread.sleep((attempt + 1) * 2000L)
                }
            } catch (e: Exception) {
                throw e
            }
        }
        throw lastException ?: Exception("请求失败")
    }

    private class RetryableException(message: String) : Exception(message)

    private fun buildSystemPrompt(method: FortuneMethod): String {
        return when (method) {
            FortuneMethod.BAZI -> """你是一位精通四柱八字的命理大师，有40年批命经验。
你熟悉天干地支、五行生克、十神关系、神煞、大运流年等所有八字知识。
请用专业但通俗的语言解读命盘，既要有术语也要有白话解释。
风格：稳重权威，像老中医把脉一样精准。"""

            FortuneMethod.ZIWEI -> """你是一位紫微斗数名家，精通斗数十四主星、辅星、煞星的庙旺利陷。
你能根据出生信息排出完整命盘，分析十二宫位，解读星曜组合的含义。
风格：儒雅博学，条理清晰。"""

            FortuneMethod.NAME_STUDY -> """你是一位姓名学专家，精通五格剖象法、三才配置、字义分析。
你能根据姓名笔画数计算天格、人格、地格、外格、总格，分析其吉凶含义。
同时结合汉字的字义、字形、音韵来综合分析姓名对人生的影响。
风格：细致入微，善于从文字中发现命运密码。"""

            FortuneMethod.ZODIAC -> """你是一位生肖运势专家，精通十二生肖的五行属性、六合六冲、三合三刑等关系。
请结合今年的太岁、流年飞星，给出详细的年度运势分析。
风格：亲切生动，具体实用，给出可操作的建议。"""

            FortuneMethod.QIMEN -> """你是一位奇门遁甲高手，精通三奇六仪、八门九星、八神等要素。
请根据当前时间自动起局（时家奇门），结合用户信息分析近期运势走向。
风格：神秘高深但解释清晰，像一位军师运筹帷幄。"""

            FortuneMethod.ASTROLOGY -> """你是一位西方占星师，精通行星、星座、宫位、相位的含义和解读。
请根据出生信息推算太阳星座、月亮星座、上升星座（需要出生时间和地点），
并解读这三大星座对性格和命运的影响。
风格：优雅浪漫，富有诗意，像一位看透星空的智者。"""

            FortuneMethod.NATAL_CHART -> """你是一位专业占星师，能够解读完整的出生星盘（Natal Chart）。
请分析太阳、月亮、水星、金星、火星、木星、土星等行星的落座和宫位，
以及主要相位（合相、对冲、三分、四分、六分）的含义。
风格：系统全面，像一份专业的星盘报告。"""

            FortuneMethod.NUMEROLOGY -> """你是一位数秘术（Numerology）专家。
请将用户的中文姓名转为拼音，然后根据毕达哥拉斯数字对应表（A=1,B=2...I=9,J=1...）
计算生命路径数（Life Path Number，由出生日期各位数相加）、
命运数（Destiny Number，由姓名字母数值相加）、
灵魂渴望数（Soul Urge Number，由元音字母数值相加）。
详细解读每个核心数字的含义。
风格：神秘而精确，像破解数字密码。"""

            FortuneMethod.VEDIC -> """你是一位吠陀占星（Jyotish）专家，精通印度占星体系。
请根据出生信息分析其Rashi（月亮星座）、Lagna（上升星座）、
各行星的Nakshatra（月宿）分布，以及主要的Dasha时期。
注意吠陀占星使用恒星黄道（Sidereal），与西方热带黄道有约24度差异。
风格：智慧深邃，带有东方哲学气息。"""

            FortuneMethod.HUMAN_DESIGN -> """你是一位人类图（Human Design）分析师。
请根据出生信息判断用户的类型（显示者/生产者/显示生产者/投射者/反映者）、
策略（Strategy）、内在权威（Authority）、人生角色（Profile）、
以及定义和未定义的能量中心。
给出生活和决策方面的具体建议。
风格：现代、实用，像一位人生教练。"""

            FortuneMethod.MAYAN -> """你是一位玛雅历法专家，精通卓尔金历（Tzolkin）。
请根据出生日期计算用户的星系印记（Galactic Signature），包括：
Kin编号、太阳图腾（Solar Seal）、银河音调（Galactic Tone）、
引导力量、挑战力量、隐藏力量、支持力量。
风格：神秘古老，充满宇宙能量感。"""

            FortuneMethod.CELTIC_TREE -> """你是一位凯尔特树历（Celtic Tree Calendar/Ogham）专家。
请根据出生日期对应凯尔特树历中的守护树，分析其象征意义、
与之对应的性格特征、适合的人生方向、以及与其他树历的相合关系。
凯尔特树历将一年分为13个月，每月对应一种树木。
风格：自然诗意，充满凯尔特民族的灵性智慧。"""

            FortuneMethod.BLOOD_TYPE -> """你是一位血型性格分析专家，精通日本血型学说。
请根据用户的血型详细分析其性格特征、行为模式、
适合的工作类型、恋爱模式、人际关系风格、健康倾向，
以及与其他血型的相容性。
风格：活泼有趣，像一位善于观察的心理学家。"""

            FortuneMethod.TAROT -> """你是一位资深塔罗牌解读师，精通韦特塔罗78张牌的正位逆位含义。
你善于结合牌面之间的关系、牌阵位置来给出深层解读。
不仅解释每张牌的含义，更要综合分析牌面之间的故事和指引。
风格：神秘温暖，像一位灵性导师，给出具体可行的建议。"""

            FortuneMethod.LIUYAO -> """你是一位六爻占卜大师，精通周易六十四卦、六亲、世应、动变等断卦技术。
请根据用户摇出的卦象，装卦、定世应、排六亲、看动爻变爻，
结合月建日辰进行详细断卦。
风格：严谨古朴，像一位坐在古庙里的老先生。"""

            FortuneMethod.MEIHUA -> """你是一位梅花易数高手，精通先天起卦、后天起卦和断卦法则。
请根据用户提供的数字起卦，确定上卦下卦和动爻，
分析体用关系、卦象寓意，给出占断结论。
风格：灵活巧妙，善于从象中见意。"""

            FortuneMethod.CEZI -> """你是一位测字大师，精通拆字、合字、象形、会意等测字技法。
请从用户写的字中分析其字形结构、笔画含义、拆合变化，
结合所问事项给出占断。
测字讲究灵活应变，一字多解，但需言之有理。
风格：机智灵活，妙语连珠，像古代的街头测字先生。"""

            FortuneMethod.RUNES -> """你是一位北欧卢恩符文（Runes/Futhark）解读师。
请解读用户抽到的符文，分析其古日耳曼语含义、神话背景、
对应的北欧神祇和力量，以及对提问的指引。
每个符文都有正位和逆位（倒置）的不同含义。
风格：苍劲古朴，带有北欧神话的力量感。"""

            FortuneMethod.DICE -> """你是一位骰子占卜师，精通以骰子数字组合来占断吉凶。
请根据三个骰子的点数组合（总和3-18），分析其数字含义、
对应的能量象征，结合用户问题给出解读和建议。
风格：简洁有力，直击要害。"""

            FortuneMethod.PENDULUM -> """你是一位灵摆占卜师。灵摆占卜适合回答是/否类的简单问题。
请根据问题的性质，给出明确的是/否回答，并解释原因和建议。
如果问题不适合用是/否回答，请提示用户重新组织问题。
风格：简洁清晰，温和但坚定。"""
        }
    }

    private fun buildUserPrompt(method: FortuneMethod, profile: UserProfile, question: String?, extra: String?): String {
        val today = LocalDate.now()
        val currentYear = today.year
        val currentMonth = today.monthValue
        val baseInfo = """
以下是求测者的基本信息：
- 姓名：${profile.name}
- 性别：${profile.gender}
- 出生日期（公历）：${profile.birthDate}
- 出生时间：${profile.birthTime}
- 出生城市：${profile.birthCity}
- 血型：${profile.bloodType}
- 当前日期：${today}（请基于此日期分析运势）
""".trimIndent()

        val taskPrompt = when (method) {
            FortuneMethod.BAZI -> """
$baseInfo

请为此人批八字，要求：
1. 先排出四柱（年柱、月柱、日柱、时柱）
2. 分析日主强弱和喜用神
3. 解读十神格局
4. 分析五行分布和缺失
5. 推断大运流年走势（重点分析${currentYear}年及未来3年）
6. 详细分析${currentYear}年${currentMonth}月起未来半年的月运
7. 给出事业、财运、婚姻、健康方面的总体判断
8. 给出趋吉避凶的建议"""

            FortuneMethod.ZIWEI -> """
$baseInfo

请为此人排紫微斗数命盘，要求：
1. 确定命宫所在宫位和主星
2. 逐一分析：命宫、兄弟宫、夫妻宫、子女宫、财帛宫、疾厄宫、迁移宫、交友宫、事业宫、田宅宫、福德宫、父母宫
3. 重点解读命宫、事业宫、财帛宫、夫妻宫的星曜组合
4. 分析当前大限和${currentYear}年流年运势
5. 分析${currentYear}年${currentMonth}月流月运势
6. 给出人生建议"""

            FortuneMethod.NAME_STUDY -> """
$baseInfo

请对姓名「${profile.name}」进行全面分析：
1. 计算各字笔画数（使用康熙字典笔画）
2. 算出天格、人格、地格、外格、总格的数理
3. 分析每个格的吉凶含义
4. 判断三才（天人地）配置的吉凶
5. 从字义、字形、音韵角度分析姓名气场
6. 如有不利之处，给出改名建议或化解方法"""

            FortuneMethod.ZODIAC -> """
$baseInfo

请分析此人${currentYear}年（当前为${currentMonth}月）的生肖运势：
1. 确定其生肖和五行属性
2. 分析与今年太岁的关系（犯太岁、刑太岁、冲太岁等）
3. 分项解读：事业运、财运、感情运、健康运、学业运
4. 重点分析当前月份（${currentMonth}月）及未来3个月的运势
5. 每月运势概览（标注特别好和特别需注意的月份）
6. 给出开运建议（颜色、方位、数字等）
7. 提示近期需要注意的事项"""

            FortuneMethod.QIMEN -> """
$baseInfo

请以当前时间为此人起奇门遁甲局：
1. 确定阴阳遁、几局
2. 排布天盘、地盘、九星、八门、八神
3. 分析用神所在宫位的组合含义
4. 判断当前时期的整体运势方向
5. 给出近期行动建议（利于做什么、不利于做什么）"""

            FortuneMethod.ASTROLOGY -> """
$baseInfo

请进行占星分析：
1. 根据出生日期确定太阳星座
2. 根据出生时间和地点推算月亮星座和上升星座
3. 详细解读太阳星座的核心特质
4. 解读月亮星座的情感模式和内在需求
5. 解读上升星座的外在表现和人生方向
6. 分析三者之间的配合或冲突
7. 分析${currentYear}年${currentMonth}月当前行星过境对此人的具体影响
8. 给出未来3个月的星象提醒"""

            FortuneMethod.NATAL_CHART -> """
$baseInfo

请解读完整出生星盘：
1. 列出太阳、月亮、水星、金星、火星、木星、土星、天王星、海王星、冥王星的星座和宫位
2. 分析主要相位（合相0度、对冲180度、三分120度、四分90度、六分60度）
3. 找出星盘中的重要格局（大三角、T三角、大十字等）
4. 分析元素分布（火土风水）和模式分布（基本、固定、变动）
5. 综合解读性格、天赋、挑战和人生主题"""

            FortuneMethod.NUMEROLOGY -> """
$baseInfo

请进行数秘术分析：
1. 将姓名「${profile.name}」转为拼音
2. 根据拼音字母计算命运数（所有字母数值之和化简到1-9或11/22/33）
3. 根据出生日期计算生命路径数（所有数字之和化简）
4. 计算灵魂渴望数（元音字母数值之和）
5. 计算人格数（辅音字母数值之和）
6. 计算生日数（出生日化简）
7. 详细解读每个核心数字的含义和人生指引
8. 分析各数字之间的和谐度"""

            FortuneMethod.VEDIC -> """
$baseInfo

请用吠陀占星体系分析：
1. 确定Lagna（上升星座，使用恒星黄道）
2. 分析月亮所在Rashi和Nakshatra
3. 列出九大行星（Navagraha）的位置
4. 判断各行星的强弱（Shadbala）
5. 分析当前的Dasha主运期和子运期
6. 给出Yoga（瑜伽组合）分析
7. 建议适合的宝石、颜色和吉日"""

            FortuneMethod.HUMAN_DESIGN -> """
$baseInfo

请分析此人的人类图：
1. 确定类型（Type）：显示者/生产者/显示生产者/投射者/反映者
2. 确定策略（Strategy）和内在权威（Authority）
3. 确定人生角色（Profile）如 1/3, 4/6 等
4. 分析定义的能量中心和开放的能量中心
5. 解读各通道和闸门的含义
6. 给出具体的生活决策建议
7. 说明此类型在工作、关系中的最佳运作方式"""

            FortuneMethod.MAYAN -> """
$baseInfo

请计算玛雅星系印记：
1. 根据出生日期换算为玛雅卓尔金历的Kin编号（1-260）
2. 确定太阳图腾（20种中的哪一种）
3. 确定银河音调（1-13中的哪一个）
4. 解读太阳图腾的核心能量和特质
5. 解读银河音调的创造力表达
6. 分析引导、支持、挑战、隐藏四个方向的力量
7. 给出活出星系印记的生活建议"""

            FortuneMethod.CELTIC_TREE -> """
$baseInfo

请对应凯尔特树历分析：
1. 根据出生日期确定对应的守护树（13种树之一）
2. 解读此树在凯尔特神话中的象征意义
3. 分析对应的欧甘字母（Ogham）含义
4. 描述此树人的性格特征和天赋
5. 分析适合的职业方向和人生追求
6. 说明与其他树历的相合和相克关系
7. 给出与守护树连接的实践建议"""

            FortuneMethod.BLOOD_TYPE -> """
$baseInfo

请详细分析${profile.bloodType}型血的特征：
1. 核心性格特征（优势和短板）
2. 工作风格和适合的职业类型
3. 恋爱模式和理想伴侣血型
4. 人际交往的特点和注意事项
5. 健康倾向和养生建议
6. 压力应对方式
7. 与A/B/AB/O各型的相容性分析"""

            FortuneMethod.TAROT -> """
$baseInfo

用户的问题是：$question
用户抽到的牌是：$extra

请进行塔罗解读：
1. 逐张解释每张牌的含义（正位/逆位）
2. 分析牌面之间的关系和故事线
3. 结合用户的问题给出针对性解读
4. 给出明确的建议和行动指引
5. 提示需要注意的事项"""

            FortuneMethod.LIUYAO -> """
$baseInfo

用户的问题是：$question
摇卦结果（从初爻到上爻）：
$extra

请进行六爻断卦：
1. 根据卦象确定本卦和变卦
2. 装纳甲、定世应
3. 排六亲（以日主五行为基准）
4. 分析动爻和变爻
5. 结合月建日辰判断各爻旺衰
6. 找出用神并判断其状态
7. 给出最终占断结论"""

            FortuneMethod.MEIHUA -> """
$baseInfo

用户的问题是：$question
用户提供的数字：$extra

请用梅花易数起卦解读：
1. 以数字起卦（上卦、下卦、动爻）
2. 确定本卦和互卦、变卦
3. 判断体卦和用卦
4. 分析体用之间的五行生克关系
5. 结合卦象给出占断
6. 给出时间应期的判断"""

            FortuneMethod.CEZI -> """
$baseInfo

用户的问题是：$question
用户写的字是：$extra

请进行测字占断：
1. 分析此字的字形结构（上下/左右/内外/独体）
2. 进行拆字分析（可拆成哪些字或部件）
3. 进行合字联想（与其他字组合的含义）
4. 分析笔画数的吉凶
5. 从字义角度解读
6. 综合以上分析，结合问题给出占断"""

            FortuneMethod.RUNES -> """
$baseInfo

用户的问题是：$question
抽到的符文：$extra

请解读此卢恩符文：
1. 符文的名称、字母和原始含义
2. 对应的北欧神话故事和神祇
3. 正位含义和指引
4. 如果是逆位，解读逆位含义
5. 结合用户问题给出具体建议
6. 给出冥想或连接此符文能量的方法"""

            FortuneMethod.DICE -> """
$baseInfo

用户的问题是：$question
骰子结果：$extra

请根据骰子点数占卜：
1. 分析三个骰子各自的数字含义
2. 计算总和并解读其象征
3. 分析数字组合的特殊含义（顺子、对子、豹子等）
4. 结合用户问题给出解读
5. 给出明确的建议"""

            FortuneMethod.PENDULUM -> """
$baseInfo

用户的问题是：$question

请进行灵摆占卜：
1. 判断此问题是否适合用是/否来回答
2. 给出明确的"是"或"否"的回答
3. 解释为何是这个答案
4. 如果答案有条件或时间限制，请说明
5. 给出补充建议"""
        }

        return "$taskPrompt\n\n【输出格式要求】\n请严格按以下格式输出：\n\n第一行：一句话总结（不超过20字，用于列表展示）\n第二行：---\n第三部分：大白话总结（用最通俗易懂的语言，像跟朋友聊天一样，说人话，不要术语，3-5句话概括核心意思，让完全不懂命理的人也能秒懂）\n第四行：---\n第五部分：详细专业解读（可以用术语，完整分析）"
    }

    private fun buildSummaryPrompt(allResults: Map<FortuneMethod, String>, profile: UserProfile): String {
        val resultsText = allResults.entries.joinToString("\n\n") { (method, result) ->
            "=== ${method.displayName} ===\n$result"
        }
        return """
以下是对「${profile.name}」（${profile.gender}，${profile.birthDate} ${profile.birthTime}生，${profile.birthCity}）
用多种命理体系算出的完整结果：

$resultsText

---

请综合以上所有结果，撰写一份「命运总评」：

1. **核心特质**：多个体系共同指向的性格核心是什么？
2. **天赋才能**：此人最突出的能力和适合的方向
3. **事业方向**：哪些行业/岗位最适合？给出具体建议
4. **财运格局**：正财偏财？何时财运最旺？
5. **感情婚姻**：感情模式、理想伴侣类型、注意事项
6. **健康提醒**：需要注意的身体部位或健康习惯
7. **近期运势**：最近1-2年的重点机遇和挑战
8. **核心建议**：3条最重要的人生建议

注意：找出不同体系之间相互印证的共同点，这些交叉点往往最准确。
如果不同体系有矛盾之处，也请指出并给出你的判断。
""".trimIndent()
    }
}

data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double,
    @com.google.gson.annotations.SerializedName("max_tokens")
    val maxTokens: Int = 4096
)

data class Message(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)
