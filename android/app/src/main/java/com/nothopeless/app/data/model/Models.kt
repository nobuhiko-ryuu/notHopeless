package com.nothopeless.app.data.model

import com.google.firebase.Timestamp

data class Post(
    val postId: String = "",
    val authorId: String = "",
    val scene: String = "",
    val kindnessType: String = "",
    val userState: String? = null,
    val effect: String = "",
    val body: String = "",
    val reactionCounts: Map<String, Long> = mapOf(
        "notHopeless" to 0L, "moved" to 0L, "doToo" to 0L
    ),
    val isStock: Boolean = false,
    val createdAt: Timestamp? = null,
    val status: String = PostStatus.VISIBLE,
)

object PostStatus {
    const val VISIBLE = "visible"
    const val HIDDEN = "hidden"
}

enum class SceneType(val key: String, val label: String) {
    COMMUTE("commute", "通勤・通学"),
    SHOP("shop", "お店"),
    WORKPLACE("workplace", "職場"),
    PUBLIC("public", "街・公共"),
}

enum class KindnessType(val key: String, val label: String) {
    CARE("care", "気遣い"),
    HELP("help", "手助け"),
    INTEGRITY("integrity", "誠実"),
    COURAGE("courage", "勇気"),
    PRO("pro", "プロの仕事"),
}

enum class UserStateType(val key: String, val label: String) {
    TIRED("tired", "疲れてた"),
    RUSHED("rushed", "焦ってた"),
    DOWN("down", "落ちてた"),
    NORMAL("normal", "普通"),
}

enum class EffectType(val key: String, val label: String) {
    RELIEVED("relieved", "少し安心した"),
    LIGHTER("lighter", "気持ちが軽くなった"),
    INSPIRED("inspired", "自分も優しくしようと思った"),
    SURVIVED("survived", "今日を乗り切れた"),
    NOT_HOPELESS("notHopeless", "捨てたもんじゃないと思った"),
    TRUST("trust", "人を信じてみようと思った"),
}

enum class ReactionType(val key: String, val label: String) {
    NOT_HOPELESS("notHopeless", "捨てたもんじゃない"),
    MOVED("moved", "沁みた"),
    DO_TOO("doToo", "自分もする"),
}

enum class ReportReason(val key: String, val label: String) {
    PERSONAL_INFO("personal_info", "個人情報・特定できる情報が含まれている"),
    HARASSMENT("harassment", "誹謗中傷・悪口"),
    DISCRIMINATION("discrimination", "差別的な内容"),
    SEXUAL("sexual", "性的な内容"),
    OTHER("other", "その他"),
}
