package io.legato.kazusa.lib.permission

interface OnRequestPermissionsResultCallback {

    fun onRequestPermissionsResult(permissions: Array<String>, grantResults: IntArray)

    fun onSettingActivityResult()

    fun onError(e: Exception)
}
