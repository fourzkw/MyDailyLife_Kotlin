package com.mydailylife.schedule.data.academic

/** Chongqing University smart academic affairs portal. */
object CquPortal {
    const val ENTRY_URL = "https://my.cqu.edu.cn/"
    const val SCHOOL_LABEL = "重庆大学智慧教务"

    val guidanceSteps: List<String> = listOf(
        "在下方页面完成统一身份认证登录",
        "进入「课表查询」或个人课表，等页面课程格子加载完成",
        "再点底部「捕获课表」（会读取 my-table-detail 接口数据）",
    )
}
