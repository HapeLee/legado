package io.legato.kazusa.exception

import io.legato.kazusa.R
import splitties.init.appCtx

class NoBooksDirException: NoStackTraceException(appCtx.getString(R.string.no_books_dir))