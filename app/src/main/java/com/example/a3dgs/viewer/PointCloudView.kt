package com.example.a3dgs.viewer

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.example.a3dgs.ply.PointCloud

class PointCloudView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {
    private val renderer = PointCloudRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun setPointCloud(pc: PointCloud) {
        renderer.setPointCloud(pc)
    }
}


