package io.legato.kazusa.utils

import io.legato.kazusa.help.globalExecutor
import java.util.logging.FileHandler
import java.util.logging.LogRecord

class AsyncFileHandler(pattern: String) : FileHandler(pattern) {

    override fun publish(record: LogRecord?) {
        if (!isLoggable(record)) {
            return
        }
        globalExecutor.execute {
            super.publish(record)
        }
    }

}
