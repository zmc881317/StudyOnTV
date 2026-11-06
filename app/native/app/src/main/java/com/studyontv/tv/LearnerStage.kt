package com.studyontv.tv

/**
 * 学段（身份）：小学 / 初中。
 *
 * 题目按 level 字段归类：
 * - 含「初中、初一/初二/初三、七/八/九年级、中考」→ 初中
 * - 含「高中、高一/高二/高三、高考」→ 高中（两个学段都不抽取）
 * - 其他（空、三年级、小学、通用、自定义文字…）→ 小学
 *   其中 level 为空或「通用」的题目两个学段共用。
 */
object LearnerStage {
    const val PRIMARY = "primary"
    const val JUNIOR = "junior"
    val ALL = listOf(PRIMARY, JUNIOR)

    private val juniorWords = listOf("初中", "初一", "初二", "初三", "七年级", "八年级", "九年级", "中考")
    private val seniorWords = listOf("高中", "高一", "高二", "高三", "高考")
    private val sharedLevels = listOf("", "通用", "全部")

    fun label(stage: String): String = when (stage) {
        PRIMARY -> "小学"
        JUNIOR -> "初中"
        else -> ""
    }

    fun normalize(value: String?): String = when (value?.trim()?.lowercase()) {
        PRIMARY, "小学", "小学生", "primary_school", "elementary" -> PRIMARY
        JUNIOR, "初中", "初中生", "middle", "junior_high" -> JUNIOR
        else -> ""
    }

    /** level → primary / junior / senior / shared */
    fun ofLevel(level: String?): String {
        val value = level?.trim() ?: ""
        if (value in sharedLevels) return "shared"
        if (juniorWords.any { value.contains(it) }) return JUNIOR
        if (seniorWords.any { value.contains(it) }) return "senior"
        return PRIMARY
    }

    /** SQL 片段：只保留当前学段可用的题（含共用题）。stage 为空时不过滤。 */
    fun sqlFilter(stage: String, column: String = "questions.level"): Pair<String, List<String>> {
        if (stage !in ALL) return "" to emptyList()
        val col = "IFNULL($column,'')"
        val shared = sharedLevels.joinToString(" OR ") { "$col=?" }
        val juniorLike = juniorWords.joinToString(" OR ") { "$col LIKE ?" }
        val seniorLike = seniorWords.joinToString(" OR ") { "$col LIKE ?" }
        val sharedArgs = sharedLevels
        val juniorArgs = juniorWords.map { "%$it%" }
        val seniorArgs = seniorWords.map { "%$it%" }
        return if (stage == JUNIOR) {
            "(($shared) OR ($juniorLike))" to (sharedArgs + juniorArgs)
        } else {
            "(($shared) OR (NOT ($juniorLike) AND NOT ($seniorLike)))" to (sharedArgs + juniorArgs + seniorArgs)
        }
    }
}
