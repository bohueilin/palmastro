package com.palmastro.content

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.ContentComposer

class ContentComposerImpl(
    private val templates: ContentTemplates = ContentTemplates.default()
) : ContentComposer {

    override fun compose(input: ContentInput): Map<String, SemanticPayload> {
        val domains = listOf("career", "wealth", "family", "health")
        return domains.associateWith { domain ->
            val score = input.scoringResult.domainScores[domain] ?: 50
            val grade = input.scoringResult.grade
            val domainExplain = input.scoringResult.explainability.filter { it.mappingZh.contains(domain) }
            val allSignals = input.scoringResult.explainability.map { it.signalId }.distinct()
            val observations = domainExplain.take(3).map { Observation(it.signalId, formatSignalName(it.signalId), formatSignalEvidence(it.signalId, it.contribution)) }
            val delta = input.deltaResult?.domainDeltas?.get(domain)
            val confidence = input.scoringResult.confidence
            val calcLevel = input.calcLevel

            SemanticPayload(
                domain = domain, monthKey = input.monthKey, calcLevel = calcLevel, confidence = confidence,
                observations = observations,
                interpretationZh = generateRichInterpretation("en", domain, score, grade, confidence, calcLevel, domainExplain, delta),
                blindspotZh = generateRichBlindspot("en", domain, score),
                actionTodayZh = generateContextualAction("en", domain, score, "today"),
                actionWeekZh = generateContextualAction("en", domain, score, "week"),
                promptZh = generateDeepPrompt("en", domain, score, grade),
                safetyNotesZh = generateSafetyNotes("en", domain),
                explainability = domainExplain,
                scoreCard = ScoreCard(score, grade, delta, input.deltaResult?.comparabilityScore, input.scoringResult.subdimScores.filter { it.key.startsWith("$domain.") })
            )
        }
    }

    fun composeInLanguage(input: ContentInput, lang: String): Map<String, SemanticPayload> {
        val domains = listOf("career", "wealth", "family", "health")
        return domains.associateWith { domain ->
            val score = input.scoringResult.domainScores[domain] ?: 50
            val grade = input.scoringResult.grade
            val domainExplain = input.scoringResult.explainability.filter { it.mappingZh.contains(domain) }
            val observations = domainExplain.take(3).map { Observation(it.signalId, formatSignalName(it.signalId), formatSignalEvidence(it.signalId, it.contribution)) }
            val delta = input.deltaResult?.domainDeltas?.get(domain)
            val confidence = input.scoringResult.confidence
            val calcLevel = input.calcLevel

            SemanticPayload(
                domain = domain, monthKey = input.monthKey, calcLevel = calcLevel, confidence = confidence,
                observations = observations,
                interpretationZh = generateRichInterpretation(lang, domain, score, grade, confidence, calcLevel, domainExplain, delta),
                blindspotZh = generateRichBlindspot(lang, domain, score),
                actionTodayZh = generateContextualAction(lang, domain, score, "today"),
                actionWeekZh = generateContextualAction(lang, domain, score, "week"),
                promptZh = generateDeepPrompt(lang, domain, score, grade),
                safetyNotesZh = generateSafetyNotes(lang, domain),
                explainability = domainExplain,
                scoreCard = ScoreCard(score, grade, delta, input.deltaResult?.comparabilityScore, input.scoringResult.subdimScores.filter { it.key.startsWith("$domain.") })
            )
        }
    }

    private fun generateRichInterpretation(lang: String, domain: String, score: Int, grade: String, confidence: String, calcLevel: CalcLevel, signals: List<ExplainEntry>, delta: DeltaValue?): String {
        val sb = StringBuilder()
        val dn = domainName(lang, domain)

        sb.appendLine(when (lang) {
            "zh-TW" -> when { score >= 80 -> "${dn}方面本月展現出非凡的力量。你的掌紋與星象信號高度對齊，預示著良好的發展態勢。"; score >= 65 -> "你的${dn}能量正處於積極階段。分析顯示出穩固的基礎，有著穩定成長的明確指標。"; score >= 50 -> "你的${dn}正處於轉型期。基礎面穩定，但有些領域需要特別關注以獲得突破。"; score >= 35 -> "你的${dn}正在累積階段。這是耐心經營、打好基礎的時期。"; else -> "你的${dn}指標正在提醒你需要注意。這不是警告，而是重新調整的邀請。" }
            "zh-CN" -> when { score >= 80 -> "${dn}方面本月展现出非凡的力量。你的掌纹与星象信号高度对齐，预示着良好的发展态势。"; score >= 65 -> "你的${dn}能量正处于积极阶段。分析显示出稳固的基础，有着稳定成长的明确指标。"; score >= 50 -> "你的${dn}正处于转型期。基础面稳定，但有些领域需要特别关注以获得突破。"; score >= 35 -> "你的${dn}正在积累阶段。这是耐心经营、打好基础的时期。"; else -> "你的${dn}指标正在提醒你需要注意。这不是警告，而是重新调整的邀请。" }
            "hi" -> when { score >= 80 -> "${dn} इस महीने असाधारण ऊर्जा दिखा रहा है। आपकी हस्तरेखा और ज्योतिषीय संकेत मजबूत संरेखण में हैं।"; score >= 65 -> "आपकी ${dn} ऊर्जा सकारात्मक चरण में है। विश्लेषण एक मजबूत नींव और स्थिर विकास के संकेत दिखाता है।"; score >= 50 -> "आपका ${dn} संक्रमण काल में है। नींव स्थिर है, लेकिन कुछ क्षेत्रों पर ध्यान देने की जरूरत है।"; score >= 35 -> "आपका ${dn} निर्माण के चरण में है। यह धैर्य और मजबूत नींव बनाने का समय है।"; else -> "आपके ${dn} संकेतक ध्यान देने की मांग कर रहे हैं। यह चिंता नहीं, बल्कि पुनर्मूल्यांकन का निमंत्रण है।" }
            "hi" -> when { score >= 80 -> "${dn} इस महीने असाधारण ऊर्जा दिखा रहा है। आपकी हस्तरेखा और ज्योतिषीय संकेत मजबूत संरेखण में हैं।"; score >= 65 -> "आपकी ${dn} ऊर्जा सकारात्मक चरण में है। विश्लेषण एक मजबूत नींव और स्थिर विकास के संकेत दिखाता है।"; score >= 50 -> "आपका ${dn} संक्रमण काल में है। नींव स्थिर है, लेकिन कुछ क्षेत्रों पर ध्यान देने की जरूरत है।"; score >= 35 -> "आपका ${dn} निर्माण के चरण में है। यह धैर्य और मजबूत नींव बनाने का समय है।"; else -> "आपके ${dn} संकेतक ध्यान देने की मांग कर रहे हैं। यह चिंता नहीं, बल्कि पुनर्मूल्यांकन का निमंत्रण है।" }
            "ja" -> when { score >= 80 -> "${dn}は今月、非常に強いエネルギーを示しています。手相と星座のシグナルが高度に一致しています。"; score >= 65 -> "${dn}のエネルギーはポジティブな段階にあります。安定した基盤と着実な成長の兆しが見えます。"; score >= 50 -> "${dn}は過渡期にあります。基礎は安定していますが、注力すべき分野があります。"; score >= 35 -> "${dn}は蓄積の段階です。忍耐強く基礎を築く時期です。"; else -> "${dn}の指標が注意を促しています。これは警告ではなく、再調整への招待です。" }
            else -> when { score >= 80 -> "$dn is showing exceptional strength this month. Your readings indicate a powerful alignment of both palm features and astrological signals working in your favor."; score >= 65 -> "Your ${dn.lowercase()} energy is in a positive phase this month. The analysis reveals a solid foundation with clear indicators of steady growth."; score >= 50 -> "Your ${dn.lowercase()} is in a transitional period. While the fundamentals are stable, there are areas where focused attention could unlock meaningful progress."; score >= 35 -> "Your ${dn.lowercase()} readings suggest a building phase. This is a time for patience and intentional groundwork rather than bold moves."; else -> "Your ${dn.lowercase()} indicators are calling for attention this month. This isn't cause for alarm — it's an invitation to pause and recalibrate." }
        })

        if (signals.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine(if (lang.startsWith("zh")) "關鍵因素：" else if (lang == "ja") "主な要因：" else "Key factors in your reading:")
            signals.take(3).forEach { entry ->
                sb.appendLine("• ${formatSignalInsight(lang, entry.signalId, domain)}")
            }
        }

        if (delta != null && delta.value != 0) {
            sb.appendLine()
            when (lang) {
                "zh-TW" -> sb.append("與上月相比，你的${dn}分數${if (delta.value > 0) "提升了${delta.value}分" else "下降了${-delta.value}分"}。${if (delta.value > 0) "繼續保持。" else "小幅調整即可扭轉趨勢。"}")
                "zh-CN" -> sb.append("与上月相比，你的${dn}分数${if (delta.value > 0) "提升了${delta.value}分" else "下降了${-delta.value}分"}。${if (delta.value > 0) "继续保持。" else "小幅调整即可扭转趋势。"}")
                "ja" -> sb.append("先月と比較して、${dn}のスコアは${if (delta.value > 0) "${delta.value}ポイント上昇" else "${-delta.value}ポイント低下"}しました。")
                else -> sb.append("Compared to last month, your score ${if (delta.value > 0) "improved by ${delta.value} points. Keep it up." else "shifted down by ${-delta.value} points. Small adjustments can help."}")
            }
        }

        return sb.toString().trim()
    }

    private fun generateRichBlindspot(lang: String, domain: String, score: Int): String = when (lang) {
        "zh-TW" -> when (domain) {
            "career" -> if (score >= 65) "事業能量強勁時，容易過度承諾。成功可能掩蓋倦怠的早期跡象。" else "在事業低潮期，避免做出衝動的決定。給自己至少48小時冷靜期。"
            "wealth" -> if (score >= 65) "財務自信可能導致生活方式膨脹。「能負擔」和「應該買」之間的差距會在順境中擴大。" else "當財務信號較低時，避免恐慌性儲蓄或壓力性消費。"
            "family" -> if (score >= 65) "家庭和諧時容易假設每個人都很好。最安靜的家人可能最需要關注。" else "家庭關係困難時期，記住距離並不總是拒絕——有時人們需要空間。"
            "health" -> if (score >= 65) "良好的健康指標可能產生虛假的安全感。這其實是建立預防習慣的最佳時機。" else "較低的健康指標不是預測——而是優先照顧自己的邀請。從基本做起：睡眠、補水、運動。"
            else -> ""
        }
        "zh-CN" -> when (domain) {
            "career" -> if (score >= 65) "事业能量强劲时，容易过度承诺。成功可能掩盖倦怠的早期迹象。" else "在事业低潮期，避免做出冲动的决定。给自己至少48小时冷静期。"
            "wealth" -> if (score >= 65) "财务自信可能导致生活方式膨胀。「能负担」和「应该买」之间的差距会在顺境中扩大。" else "当财务信号较低时，避免恐慌性储蓄或压力性消费。"
            "family" -> if (score >= 65) "家庭和谐时容易假设每个人都很好。最安静的家人可能最需要关注。" else "家庭关系困难时期，记住距离并不总是拒绝——有时人们需要空间。"
            "health" -> if (score >= 65) "良好的健康指标可能产生虚假的安全感。这其实是建立预防习惯的最佳时机。" else "较低的健康指标不是预测——而是优先照顾自己的邀请。"
            else -> ""
        }
        "hi" -> when (domain) {
            "career" -> if (score >= 65) "जब करियर ऊर्जा मजबूत होती है, तो अधिक प्रतिबद्धताएं लेने की प्रवृत्ति होती है। सफलता बर्नआउट के शुरुआती संकेतों को छिपा सकती है।" else "करियर की चुनौतीपूर्ण अवधि में, प्रतिक्रियात्मक निर्णय लेने से बचें।"
            "wealth" -> if (score >= 65) "वित्तीय आत्मविश्वास जीवनशैली में मुद्रास्फीति का कारण बन सकता है।" else "जब वित्तीय संकेत कम हों, तो घबराहट में बचत या तनाव-खर्च दोनों से बचें।"
            "family" -> if (score >= 65) "पारिवारिक सामंजस्य अच्छा होने पर आप मान लेते हैं कि सब ठीक है। सबसे शांत सदस्य को शायद सबसे ज्यादा ध्यान की जरूरत है।" else "कठिन समय में, दूरी हमेशा अस्वीकृति नहीं होती।"
            "health" -> if (score >= 65) "अच्छे स्वास्थ्य संकेतक अजेयता का भ्रम पैदा कर सकते हैं। यह निवारक आदतें स्थापित करने का सबसे अच्छा समय है।" else "कम स्वास्थ्य संकेतक भविष्यवाणी नहीं हैं — वे आत्म-देखभाल को प्राथमिकता देने का निमंत्रण हैं।"
            else -> ""
        }
            "hi" -> when (domain) {
            "career" -> if (score >= 65) "जब करियर ऊर्जा मजबूत होती है, तो अधिक प्रतिबद्धताएं लेने की प्रवृत्ति होती है। सफलता बर्नआउट के शुरुआती संकेतों को छिपा सकती है।" else "करियर की चुनौतीपूर्ण अवधि में, प्रतिक्रियात्मक निर्णय लेने से बचें।"
            "wealth" -> if (score >= 65) "वित्तीय आत्मविश्वास जीवनशैली में मुद्रास्फीति का कारण बन सकता है।" else "जब वित्तीय संकेत कम हों, तो घबराहट में बचत या तनाव-खर्च दोनों से बचें।"
            "family" -> if (score >= 65) "पारिवारिक सामंजस्य अच्छा होने पर आप मान लेते हैं कि सब ठीक है। सबसे शांत सदस्य को शायद सबसे ज्यादा ध्यान की जरूरत है।" else "कठिन समय में, दूरी हमेशा अस्वीकृति नहीं होती।"
            "health" -> if (score >= 65) "अच्छे स्वास्थ्य संकेतक अजेयता का भ्रम पैदा कर सकते हैं। यह निवारक आदतें स्थापित करने का सबसे अच्छा समय है।" else "कम स्वास्थ्य संकेतक भविष्यवाणी नहीं हैं — वे आत्म-देखभाल को प्राथमिकता देने का निमंत्रण हैं।"
            else -> ""
        }
            "ja" -> when (domain) {
            "career" -> if (score >= 65) "キャリアエネルギーが強い時、過度なコミットメントに注意。成功が燃え尽きの兆候を隠すことがあります。" else "キャリアの低迷期には、衝動的な決断を避けましょう。"
            "wealth" -> if (score >= 65) "財務的な自信がライフスタイルの肥大化につながることがあります。" else "財務シグナルが低い時は、パニック貯蓄もストレス消費も避けましょう。"
            "family" -> if (score >= 65) "家族の調和が良い時、全員が大丈夫だと思いがちです。最も静かな家族が最も注意を必要としているかもしれません。" else "困難な時期、距離は常に拒絶ではありません。"
            "health" -> if (score >= 65) "良好な健康指標は偽りの安心感を生むことがあります。予防習慣を確立する最良の時期です。" else "低い健康指標は予測ではなく、自分を優先する招待です。"
            else -> ""
        }
        else -> when (domain) {
            "career" -> if (score >= 65) "When career energy is strong, there's a tendency to overcommit. Success can mask the early signs of burnout." else "During challenging career periods, avoid making reactive decisions. Give yourself at least 48 hours before responding to stressful situations."
            "wealth" -> if (score >= 65) "Financial confidence can lead to lifestyle inflation. The gap between 'can afford' and 'should buy' widens when things feel abundant." else "When wealth signals are lower, avoid both panic-saving and stress-spending."
            "family" -> if (score >= 65) "Strong family harmony can make you assume everyone is fine. The quietest family member may need the most attention." else "During challenging family periods, remember that distance isn't always rejection."
            "health" -> if (score >= 65) "Good health readings can create a false sense of invincibility. This is the best time to establish preventive habits." else "Lower health indicators aren't predictions — they're invitations to prioritize yourself."
            else -> ""
        }
    }

    private fun generateContextualAction(lang: String, domain: String, score: Int, timeframe: String): String {
        if (lang == "en" || lang == "") return generateContextualActionEn(domain, score, timeframe)
        val en = generateContextualActionEn(domain, score, timeframe)
        return when (lang) {
            "zh-TW" -> when (domain) {
                "career" -> if (timeframe == "today") { if (score >= 65) "找到一位你欣賞的人，針對他最近做的具體事情給予讚美。" else "花10分鐘列出你事業中順利的事——而非需要修正的。" } else { if (score >= 65) "接受一個超出舒適區的任務或專案。" else "本週安排兩個30分鐘的「思考時間」——不開會、不做任務。" }
                "wealth" -> if (timeframe == "today") { if (score >= 65) "檢視你的訂閱服務，取消一個過去一個月沒使用的。" else "找出一個真正讓你開心的免費活動。" } else { if (score >= 65) "設定一筆自動轉帳到儲蓄帳戶。" else "本週和一個人坦誠地談談金錢。" }
                "family" -> if (timeframe == "today") { if (score >= 65) "今晚吃飯時放下手機，全心與家人在一起。" else "寫下一件你感謝某位家人但從未說過的事。" } else { if (score >= 65) "和家人安排一個具體活動——不是「找時間聚聚」而是「週六一起做某件事」。" else "本週花20分鐘在一段需要關注的家庭關係上。" }
                "health" -> if (timeframe == "today") { if (score >= 65) "做一件你真正喜歡的運動——散步、跳舞、伸展。" else "今晚早睡30分鐘。睡眠是一切的基礎。" } else { if (score >= 65) "本週試做一道新的健康食譜。" else "如果你最近沒做過健康檢查，安排一次。" }
                else -> en
            }
            "zh-CN" -> when (domain) {
                "career" -> if (timeframe == "today") { if (score >= 65) "找到一位你欣赏的人，给予具体的赞美。" else "花10分钟列出你事业中顺利的事。" } else { if (score >= 65) "接受一个超出舒适区的任务。" else "安排思考时间——不开会、不做任务。" }
                else -> en
            }
            "hi" -> when (domain) {
            "career" -> if (score >= 65) "जब करियर ऊर्जा मजबूत होती है, तो अधिक प्रतिबद्धताएं लेने की प्रवृत्ति होती है। सफलता बर्नआउट के शुरुआती संकेतों को छिपा सकती है।" else "करियर की चुनौतीपूर्ण अवधि में, प्रतिक्रियात्मक निर्णय लेने से बचें।"
            "wealth" -> if (score >= 65) "वित्तीय आत्मविश्वास जीवनशैली में मुद्रास्फीति का कारण बन सकता है।" else "जब वित्तीय संकेत कम हों, तो घबराहट में बचत या तनाव-खर्च दोनों से बचें।"
            "family" -> if (score >= 65) "पारिवारिक सामंजस्य अच्छा होने पर आप मान लेते हैं कि सब ठीक है। सबसे शांत सदस्य को शायद सबसे ज्यादा ध्यान की जरूरत है।" else "कठिन समय में, दूरी हमेशा अस्वीकृति नहीं होती।"
            "health" -> if (score >= 65) "अच्छे स्वास्थ्य संकेतक अजेयता का भ्रम पैदा कर सकते हैं। यह निवारक आदतें स्थापित करने का सबसे अच्छा समय है।" else "कम स्वास्थ्य संकेतक भविष्यवाणी नहीं हैं — वे आत्म-देखभाल को प्राथमिकता देने का निमंत्रण हैं।"
            else -> ""
        }
            "hi" -> when (domain) {
            "career" -> if (score >= 65) "जब करियर ऊर्जा मजबूत होती है, तो अधिक प्रतिबद्धताएं लेने की प्रवृत्ति होती है। सफलता बर्नआउट के शुरुआती संकेतों को छिपा सकती है।" else "करियर की चुनौतीपूर्ण अवधि में, प्रतिक्रियात्मक निर्णय लेने से बचें।"
            "wealth" -> if (score >= 65) "वित्तीय आत्मविश्वास जीवनशैली में मुद्रास्फीति का कारण बन सकता है।" else "जब वित्तीय संकेत कम हों, तो घबराहट में बचत या तनाव-खर्च दोनों से बचें।"
            "family" -> if (score >= 65) "पारिवारिक सामंजस्य अच्छा होने पर आप मान लेते हैं कि सब ठीक है। सबसे शांत सदस्य को शायद सबसे ज्यादा ध्यान की जरूरत है।" else "कठिन समय में, दूरी हमेशा अस्वीकृति नहीं होती।"
            "health" -> if (score >= 65) "अच्छे स्वास्थ्य संकेतक अजेयता का भ्रम पैदा कर सकते हैं। यह निवारक आदतें स्थापित करने का सबसे अच्छा समय है।" else "कम स्वास्थ्य संकेतक भविष्यवाणी नहीं हैं — वे आत्म-देखभाल को प्राथमिकता देने का निमंत्रण हैं।"
            else -> ""
        }
            "ja" -> when (domain) {
                "career" -> if (timeframe == "today") { if (score >= 65) "尊敬する人を見つけ、具体的な褒め言葉を送りましょう。" else "キャリアでうまくいっていることを10分間リストアップしましょう。" } else { if (score >= 65) "コンフォートゾーンの外のプロジェクトに挑戦しましょう。" else "今週、30分の「思考時間」を2回確保しましょう。" }
                else -> en
            }
            else -> en
        }
    }

    private fun generateContextualActionEn(domain: String, score: Int, timeframe: String): String = when (domain) {
        "career" -> if (timeframe == "today") { if (score >= 65) "Identify one person whose work you admire and send them a specific compliment." else "Spend 10 minutes listing what's going well in your career — not what needs fixing." } else { if (score >= 65) "Take on one stretch assignment outside your comfort zone." else "Block two 30-minute 'thinking time' slots this week — no meetings, no tasks." }
        "wealth" -> if (timeframe == "today") { if (score >= 65) "Review your subscriptions and cancel one you haven't used this month." else "Identify one free activity that genuinely makes you happy." } else { if (score >= 65) "Set up an automatic transfer to savings." else "Have one honest conversation about money this week." }
        "family" -> if (timeframe == "today") { if (score >= 65) "Put your phone down during dinner and be fully present." else "Write down one thing you appreciate about a family member but haven't told them." } else { if (score >= 65) "Plan a specific activity with family — not 'let's hang out' but 'let's do X on Saturday.'" else "Spend 20 minutes this week on a family relationship that needs attention." }
        "health" -> if (timeframe == "today") { if (score >= 65) "Do something physical you enjoy — walking, dancing, stretching." else "Go to bed 30 minutes earlier tonight." } else { if (score >= 65) "Try one new healthy recipe this week." else "Schedule a wellness check if you haven't had one recently." }
        else -> ""
    }

    private fun generateDeepPrompt(lang: String, domain: String, score: Int, grade: String): String = when (lang) {
        "zh-TW" -> when (domain) { "career" -> if (score >= 65) "如果金錢不是問題，你會選擇做同樣的工作嗎？你會改變什麼？" else "如果你現在的職業挑戰正在試圖教你什麼，那個教訓會是什麼？"; "wealth" -> if (score >= 65) "除了財務安全，對你來說「足夠」是什麼樣子？" else "如果你明天可以改變一個理財習慣，哪一個會產生最大的漣漪效應？"; "family" -> if (score >= 65) "有什麼對話你一直在迴避？用同理心去開啟它需要什麼？" else "你對家庭關係抱持的某個期望，值得重新審視嗎？"; "health" -> if (score >= 65) "你和身體之間的溝通品質如何？你在傾聽嗎？" else "如果你的身體現在可以寫一封信給你，它會說什麼？"; else -> "" }
        "zh-CN" -> when (domain) { "career" -> if (score >= 65) "如果钱不是问题，你会选择做同样的工作吗？" else "如果你现在的职业挑战试图教你什么，那个教训是什么？"; "wealth" -> if (score >= 65) "除了财务安全，「足够」对你来说是什么样子？" else "如果明天可以改变一个理财习惯，哪个影响最大？"; "family" -> if (score >= 65) "有什么对话你一直在回避？" else "你对家庭关系的某个期望值得重新审视吗？"; "health" -> if (score >= 65) "你和身体之间的沟通质量如何？" else "如果你的身体可以写信给你，它会说什么？"; else -> "" }
        "hi" -> when (domain) {
            "career" -> if (score >= 65) "जब करियर ऊर्जा मजबूत होती है, तो अधिक प्रतिबद्धताएं लेने की प्रवृत्ति होती है। सफलता बर्नआउट के शुरुआती संकेतों को छिपा सकती है।" else "करियर की चुनौतीपूर्ण अवधि में, प्रतिक्रियात्मक निर्णय लेने से बचें।"
            "wealth" -> if (score >= 65) "वित्तीय आत्मविश्वास जीवनशैली में मुद्रास्फीति का कारण बन सकता है।" else "जब वित्तीय संकेत कम हों, तो घबराहट में बचत या तनाव-खर्च दोनों से बचें।"
            "family" -> if (score >= 65) "पारिवारिक सामंजस्य अच्छा होने पर आप मान लेते हैं कि सब ठीक है। सबसे शांत सदस्य को शायद सबसे ज्यादा ध्यान की जरूरत है।" else "कठिन समय में, दूरी हमेशा अस्वीकृति नहीं होती।"
            "health" -> if (score >= 65) "अच्छे स्वास्थ्य संकेतक अजेयता का भ्रम पैदा कर सकते हैं। यह निवारक आदतें स्थापित करने का सबसे अच्छा समय है।" else "कम स्वास्थ्य संकेतक भविष्यवाणी नहीं हैं — वे आत्म-देखभाल को प्राथमिकता देने का निमंत्रण हैं।"
            else -> ""
        }
            "hi" -> when (domain) {
            "career" -> if (score >= 65) "जब करियर ऊर्जा मजबूत होती है, तो अधिक प्रतिबद्धताएं लेने की प्रवृत्ति होती है। सफलता बर्नआउट के शुरुआती संकेतों को छिपा सकती है।" else "करियर की चुनौतीपूर्ण अवधि में, प्रतिक्रियात्मक निर्णय लेने से बचें।"
            "wealth" -> if (score >= 65) "वित्तीय आत्मविश्वास जीवनशैली में मुद्रास्फीति का कारण बन सकता है।" else "जब वित्तीय संकेत कम हों, तो घबराहट में बचत या तनाव-खर्च दोनों से बचें।"
            "family" -> if (score >= 65) "पारिवारिक सामंजस्य अच्छा होने पर आप मान लेते हैं कि सब ठीक है। सबसे शांत सदस्य को शायद सबसे ज्यादा ध्यान की जरूरत है।" else "कठिन समय में, दूरी हमेशा अस्वीकृति नहीं होती।"
            "health" -> if (score >= 65) "अच्छे स्वास्थ्य संकेतक अजेयता का भ्रम पैदा कर सकते हैं। यह निवारक आदतें स्थापित करने का सबसे अच्छा समय है।" else "कम स्वास्थ्य संकेतक भविष्यवाणी नहीं हैं — वे आत्म-देखभाल को प्राथमिकता देने का निमंत्रण हैं।"
            else -> ""
        }
            "ja" -> when (domain) { "career" -> if (score >= 65) "お金が問題でなかったら、同じ仕事を選びますか？" else "今のキャリアの課題があなたに何かを教えようとしているなら、それは何でしょう？"; "wealth" -> if (score >= 65) "経済的安全を超えて、「十分」とはどのような姿ですか？" else "明日一つの金銭習慣を変えられるなら、どれが最大の波及効果をもたらしますか？"; "family" -> if (score >= 65) "避けている会話はありますか？" else "家族関係に対する期待で、見直す価値のあるものはありますか？"; "health" -> if (score >= 65) "あなたと体のコミュニケーションの質はどうですか？" else "体が手紙を書けるとしたら、何と言いますか？"; else -> "" }
        else -> when (domain) { "career" -> if (score >= 65) "Would you choose this work if money wasn't a factor? What would you redesign about your role?" else "If your career challenges right now are trying to teach you something, what would that lesson be?"; "wealth" -> if (score >= 65) "Beyond financial security, what does 'enough' look like for you?" else "If you could change one financial habit tomorrow, which one would have the biggest ripple effect?"; "family" -> if (score >= 65) "What's one conversation you've been avoiding? What would it take to have it with compassion?" else "What's one expectation you hold for a family relationship that might be worth examining?"; "health" -> if (score >= 65) "Your body is your longest relationship. How's the communication quality right now?" else "If your body could write you a letter, what would it say?"; else -> "" }
    }

    private fun generateSafetyNotes(lang: String, domain: String): List<String> = when (domain) {
        "wealth" -> listOf(when (lang) { "zh-TW" -> "本內容僅供個人成長參考，不構成任何投資、理財或稅務建議。"; "zh-CN" -> "本内容仅供个人成长参考，不构成任何投资、理财或税务建议。"; "ja" -> "この内容は自己成長のためのものであり、投資・財務アドバイスではありません。"; else -> "For personal reflection only — not investment, financial, or tax advice." })
        "health" -> listOf(when (lang) { "zh-TW" -> "本內容僅供自我覺察參考，不構成任何醫療診斷或治療建議。"; "zh-CN" -> "本内容仅供自我觉察参考，不构成任何医疗诊断或治疗建议。"; "ja" -> "この内容は自己観察のためのものであり、医療診断やアドバイスではありません。"; else -> "For self-awareness only — not medical diagnosis or health advice." })
        else -> emptyList()
    }

    private fun domainName(lang: String, domain: String): String = when (lang) {
        "zh-TW" -> when (domain) { "career" -> "事業"; "wealth" -> "財富"; "family" -> "家庭"; "health" -> "健康"; else -> domain }
        "zh-CN" -> when (domain) { "career" -> "事业"; "wealth" -> "财富"; "family" -> "家庭"; "health" -> "健康"; else -> domain }
        "ja" -> when (domain) { "career" -> "キャリア"; "wealth" -> "財運"; "family" -> "家庭"; "health" -> "健康"; else -> domain }
        "hi" -> when (domain) { "career" -> "करियर"; "wealth" -> "धन"; "family" -> "परिवार"; "health" -> "स्वास्थ्य"; else -> domain }
        else -> when (domain) { "career" -> "Career"; "wealth" -> "Wealth"; "family" -> "Family"; "health" -> "Health"; else -> domain }
    }
    private fun formatSignalName(signalId: String): String = signalId.replace("PALM_", "").replace("ASTRO_", "").replace("_", " ").lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    private fun formatSignalEvidence(signalId: String, contribution: Double): String { val dir = if (contribution > 0) "Positive" else "Attention"; val str = when { kotlin.math.abs(contribution) > 3 -> "strong"; kotlin.math.abs(contribution) > 1.5 -> "moderate"; else -> "subtle" }; return "$dir ($str)" }

    private fun formatSignalInsight(lang: String, signalId: String, domain: String): String = when {
        signalId.contains("HEADLINE") && signalId.contains("CLEAR") -> when (lang) { "zh-TW" -> "清晰的頭線反映出強大的分析與決策能力"; "zh-CN" -> "清晰的头线反映出强大的分析与决策能力"; "ja" -> "はっきりとした頭脳線は、強い分析力と意思決定能力を反映"; else -> "Your clear head line reflects strong analytical and decision-making capabilities" }
        signalId.contains("HEARTLINE") -> when (lang) { "zh-TW" -> "明顯的感情線表示豐富的情感深度與人際連結"; "zh-CN" -> "明显的感情线表示丰富的情感深度与人际连结"; "ja" -> "はっきりとした感情線は、感情の深さと対人関係の強さを示す"; else -> "A prominent heart line indicates emotional depth and strong connections" }
        signalId.contains("LIFELINE") -> when (lang) { "zh-TW" -> "清晰的生命線暗示著活力與韌性"; "zh-CN" -> "清晰的生命线暗示着活力与韧性"; "ja" -> "はっきりとした生命線は、活力と回復力を示唆"; else -> "Your clear life line suggests vitality and resilience" }
        signalId.contains("FATELINE") -> when (lang) { "zh-TW" -> "分明的命運線指向強烈的方向感與目標感"; "zh-CN" -> "分明的命运线指向强烈的方向感与目标感"; "ja" -> "はっきりとした運命線は、強い方向性と目的意識を示す"; else -> "A well-defined fate line points to a strong sense of direction" }
        signalId.contains("SUN") -> when (lang) { "zh-TW" -> "你的太陽星座能量影響著整體表達方式"; "ja" -> "太陽星座のエネルギーが全体的な表現に影響"; else -> "Your sun sign energy shapes how you express yourself" }
        signalId.contains("SATURN") -> when (lang) { "zh-TW" -> "土星的影響帶來紀律與結構"; "ja" -> "土星の影響は規律と構造をもたらす"; else -> "Saturn brings discipline and structure" }
        signalId.contains("JUPITER") -> when (lang) { "zh-TW" -> "木星的存在暗示著擴展與機遇"; "ja" -> "木星の存在は拡大と機会を示唆"; else -> "Jupiter suggests expansion and opportunity" }
        else -> when (lang) { "zh-TW" -> "此信號影響你的整體讀數"; "ja" -> "このシグナルが全体的なリーディングに影響"; else -> "This signal contributes to your overall reading" }
    }
}
