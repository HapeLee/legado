package io.legato.kazusa.lib.webdav

open class WebDavException(msg: String) : Exception(msg) {

    override fun fillInStackTrace(): Throwable {
        return this
    }

}

class ObjectNotFoundException(msg: String) : WebDavException(msg)