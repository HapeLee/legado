package io.legato.kazusa.help.storage

import cn.hutool.crypto.symmetric.AES
import io.legato.kazusa.help.config.LocalConfig
import io.legato.kazusa.utils.MD5Utils

class BackupAES : AES(
    MD5Utils.md5Encode(LocalConfig.password ?: "").encodeToByteArray(0, 16)
)