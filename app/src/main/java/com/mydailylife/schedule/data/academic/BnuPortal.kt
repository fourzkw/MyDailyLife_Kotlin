package com.mydailylife.schedule.data.academic

/** Beijing Normal University academic affairs (VPN → 数字京师 CAS → 教务). */
object BnuPortal {
    /**
     * School VPN CAS login (onevpn), then enter教务 / 我的课表.
     * Timetable document URL looks like:
     * `…/student/wsxk.xskcb*.jsp?params=…`
     */
    const val ENTRY_URL =
        "https://onevpn.bnu.edu.cn/https/77726476706e69737468656265737421f3f652d2253e7d1e7b0c9ce29b5b/cas/login?service=https%3A%2F%2Fonevpn.bnu.edu.cn%2Flogin%3Fcas_login%3Dtrue"
    const val SCHOOL_LABEL = "北京师范大学教务"

    val guidanceSteps: List<String> = listOf(
        "完成 VPN / 统一身份认证登录",
        "点「继续访问电脑端」",
        "打开「教务管理系统」→「网上选课」→「我的课表」",
        "选择「按课表方式显示」后点「检索」，等右侧/内嵌区出现课表网格",
        "顶部出现「已缓存课表页面」后再点「捕获课表」（勿在教务首页就点捕获）",
    )
}
