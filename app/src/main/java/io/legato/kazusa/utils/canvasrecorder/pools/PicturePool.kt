package io.legato.kazusa.utils.canvasrecorder.pools

import android.graphics.Picture
import io.legato.kazusa.utils.objectpool.BaseObjectPool

class PicturePool : BaseObjectPool<Picture>(64) {

    override fun create(): Picture = Picture()

}
