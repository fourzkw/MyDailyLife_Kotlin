package com.mydailylife.schedule.data.academic

/**
 * Schools that can be opened for 教务 WebView import.
 * Sorted / grouped by [initialLetter] (pinyin first letter A–Z).
 */
data class AcademicSchool(
    val id: String,
    val name: String,
    /** Pinyin without spaces, lowercase — used for search & sort. */
    val pinyin: String,
    /** A–Z section header (pinyin initial of the school name). */
    val initialLetter: Char,
    val subtitle: String = "",
) {
    fun matchesQuery(query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        return name.contains(q, ignoreCase = true) ||
            pinyin.contains(q) ||
            initialLetter.lowercaseChar() == q.firstOrNull() && q.length == 1
    }
}

object AcademicSchoolIds {
    const val CQU = "cqu"
    const val BNU = "bnu"
}

object AcademicSchools {
    val all: List<AcademicSchool> = listOf(
        AcademicSchool(
            id = AcademicSchoolIds.BNU,
            name = "北京师范大学",
            pinyin = "beijingshifandaxue",
            initialLetter = 'B',
            subtitle = "VPN 统一认证 onevpn.bnu.edu.cn",
        ),
        AcademicSchool(
            id = AcademicSchoolIds.CQU,
            name = "重庆大学",
            pinyin = "chongqingdaxue",
            initialLetter = 'C',
            subtitle = "智慧教务 my.cqu.edu.cn",
        ),
    ).sortedWith(
        compareBy<AcademicSchool> { it.initialLetter.uppercaseChar() }
            .thenBy { it.pinyin },
    )

    fun findById(id: String): AcademicSchool? = all.find { it.id == id }

    /** Filter + keep A–Z / pinyin order. */
    fun filtered(query: String): List<AcademicSchool> =
        all.filter { it.matchesQuery(query) }
}
