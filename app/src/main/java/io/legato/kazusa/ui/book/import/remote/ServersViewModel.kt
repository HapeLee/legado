package io.legato.kazusa.ui.book.import.remote

import android.app.Application
import io.legato.kazusa.base.BaseViewModel
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.Server

class ServersViewModel(application: Application): BaseViewModel(application) {


    fun delete(server: Server) {
        execute {
            appDb.serverDao.delete(server)
        }
    }

}