package io.legato.kazusa.help.update

import androidx.annotation.Keep
import io.legato.kazusa.constant.AppConst
import io.legato.kazusa.exception.NoStackTraceException
import io.legato.kazusa.help.config.AppConfig
import io.legato.kazusa.help.coroutine.Coroutine
import io.legato.kazusa.help.http.newCallResponse
import io.legato.kazusa.help.http.okHttpClient
import io.legato.kazusa.help.http.text
import io.legato.kazusa.utils.GSON
import io.legato.kazusa.utils.fromJsonObject
import kotlinx.coroutines.CoroutineScope

@Keep
@Suppress("unused")
object AppUpdateGitHub : AppUpdate.AppUpdateInterface {

    private val checkVariant: AppVariant
        get() = when (AppConfig.updateToVariant) {
            "official_version" -> AppVariant.OFFICIAL
            "beta_release_version" -> AppVariant.BETA_RELEASE
            "beta_releaseA_version" -> AppVariant.BETA_RELEASEA
            else -> AppConst.appInfo.appVariant
        }

    private suspend fun getLatestRelease(): List<AppReleaseInfo> {
        val lastReleaseUrl = if (checkVariant.isBeta()) {
            "https://api.github.com/repos/HapeLee/legado/releases/tags/beta"
        } else {
            "https://api.github.com/repos/HapeLee/legado/releases/latest"
        }
        val res = okHttpClient.newCallResponse {
            url(lastReleaseUrl)
        }
        if (!res.isSuccessful) {
            throw NoStackTraceException("获取新版本出错(${res.code})")
        }
        val body = res.body?.text()
        if (body.isNullOrBlank()) {
            throw NoStackTraceException("获取新版本出错")
        }
        return GSON.fromJsonObject<GithubRelease>(body)
            .getOrElse {
                throw NoStackTraceException("获取新版本出错 " + it.localizedMessage)
            }
            .gitReleaseToAppReleaseInfo()
            .sortedByDescending { it.createdAt }
    }

    override fun check(
        scope: CoroutineScope,
    ): Coroutine<AppUpdate.UpdateInfo> {
        return Coroutine.async(scope) {
            getLatestRelease()
                .filter { it.appVariant == checkVariant }
                .firstOrNull { it.versionName > AppConst.appInfo.versionName }
                ?.let {
                    return@async AppUpdate.UpdateInfo(
                        it.versionName,
                        it.note,
                        it.downloadUrl,
                        it.name
                    )
                }
                ?: throw NoStackTraceException("已是最新版本")
        }.timeout(10000)
    }
}
