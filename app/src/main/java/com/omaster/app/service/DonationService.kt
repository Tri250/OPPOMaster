package com.omaster.app.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DonationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun openDonationPage() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AFDIAN_URL))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Timber.d("Opened donation page")
        } catch (e: Exception) {
            Timber.e(e, "Failed to open donation page")
        }
    }

    fun openWeChatDonation() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WECHAT_PAY_URL))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Timber.d("Opened WeChat donation")
        } catch (e: Exception) {
            Timber.e(e, "Failed to open WeChat donation")
        }
    }

    fun openAlipayDonation() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ALIPAY_URL))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Timber.d("Opened Alipay donation")
        } catch (e: Exception) {
            Timber.e(e, "Failed to open Alipay donation")
        }
    }

    fun getDonationLevels(): List<DonationLevel> {
        return listOf(
            DonationLevel(
                id = "bronze",
                name = "青铜支持者",
                amount = 5,
                benefits = listOf("感谢支持", "专属徽章"),
                icon = "🎖️"
            ),
            DonationLevel(
                id = "silver",
                name = "白银支持者",
                amount = 20,
                benefits = listOf("感谢支持", "专属徽章", "高级主题"),
                icon = "🥈"
            ),
            DonationLevel(
                id = "gold",
                name = "黄金支持者",
                amount = 50,
                benefits = listOf("感谢支持", "专属徽章", "高级主题", "云同步容量升级"),
                icon = "🥇"
            ),
            DonationLevel(
                id = "diamond",
                name = "钻石支持者",
                amount = 100,
                benefits = listOf("感谢支持", "专属徽章", "高级主题", "云同步容量升级", "优先支持"),
                icon = "💎"
            )
        )
    }

    fun getContributorLevel(contributionCount: Int): ContributorLevel {
        return when {
            contributionCount >= 50 -> ContributorLevel.LEGEND
            contributionCount >= 30 -> ContributorLevel.STAR
            contributionCount >= 15 -> ContributorLevel.ADVANCED
            contributionCount >= 5 -> ContributorLevel.ACTIVE
            else -> ContributorLevel.NEW
        }
    }

    data class DonationLevel(
        val id: String,
        val name: String,
        val amount: Int,
        val benefits: List<String>,
        val icon: String
    )

    enum class ContributorLevel(val name: String, val icon: String, val benefits: List<String>) {
        NEW("新手贡献者", "🌱", listOf("参与社区")),
        ACTIVE("活跃贡献者", "🌿", listOf("参与社区", "优先审核权")),
        ADVANCED("资深贡献者", "🌳", listOf("参与社区", "优先审核权", "专属标识")),
        STAR("明星贡献者", "⭐", listOf("参与社区", "优先审核权", "专属标识", "荣誉徽章")),
        LEGEND("传奇贡献者", "👑", listOf("参与社区", "优先审核权", "专属标识", "荣誉徽章", "核心决策参与"))
    }

    companion object {
        private const val AFDIAN_URL = "https://afdian.net/@OMaster"
        private const val WECHAT_PAY_URL = "weixin://pay"
        private const val ALIPAY_URL = "alipays://platformapi/startapp?appId=20000221"
    }
}