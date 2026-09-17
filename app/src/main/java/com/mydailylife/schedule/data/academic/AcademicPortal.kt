package com.mydailylife.schedule.data.academic

/** Per-school WebView entry + onboarding copy for教务导入. */
data class AcademicPortalConfig(
    val entryUrl: String,
    val schoolLabel: String,
    val guidanceSteps: List<String>,
)

object AcademicPortals {
    fun forSchoolId(schoolId: String): AcademicPortalConfig? = when (schoolId) {
        AcademicSchoolIds.CQU -> AcademicPortalConfig(
            entryUrl = CquPortal.ENTRY_URL,
            schoolLabel = CquPortal.SCHOOL_LABEL,
            guidanceSteps = CquPortal.guidanceSteps,
        )
        AcademicSchoolIds.BNU -> AcademicPortalConfig(
            entryUrl = BnuPortal.ENTRY_URL,
            schoolLabel = BnuPortal.SCHOOL_LABEL,
            guidanceSteps = BnuPortal.guidanceSteps,
        )
        else -> null
    }
}
