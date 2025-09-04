package com.example.a3dgs.viewer

import android.content.Context
import android.util.AttributeSet
import android.view.Choreographer
import android.view.SurfaceView
import com.example.a3dgs.ply.PointCloud
import com.google.android.filament.*
import com.google.android.filament.utils.Float3
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.Utils
import kotlin.math.min

class FilamentPointCloudView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), Choreographer.FrameCallback {
    private var engine: Engine? = null
    private var renderer: Renderer? = null
    private var swapChain: SwapChain? = null
    private var scene: Scene? = null
    private var camera: Camera? = null
    private var view: View? = null
    private var material: MaterialInstance? = null
    private var pointEntity: Int = 0
    private var choreographer: Choreographer = Choreographer.getInstance()

    private var manipulator: Manipulator = Manipulator.Builder()
        .targetPosition(0f, 0f, 0f)
        .orbitHomePosition(0f, 0f, 3f)
        .viewport(1, 1)
        .build()

    init {
        Utils.init()
        holder.addCallback(object : android.view.SurfaceHolder.Callback {
            override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                engine = Engine.create()
                renderer = engine!!.createRenderer()
                swapChain = engine!!.createSwapChain(holder.surface)
                scene = engine!!.createScene()
                camera = engine!!.createCamera()
                view = engine!!.createView().apply {
                    this.camera = camera
                    this.scene = scene
                }
                // Simple unlit material for vertex color
                val matPkg = Material.Builder()
                    .name("pc_unlit")
                    .shading(Shading.UNLIT)
                    .build(engine!!)
                material = matPkg.defaultInstance
                choreographer.postFrameCallback(this@FilamentPointCloudView)
            }
            override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {
                renderer?.let { _ ->
                    view?.viewport = Viewport(0, 0, width, height)
                    manipulator.setViewport(width, height)
                    camera?.setProjection(45.0, width.toDouble() / height.toDouble(), 0.1, 1000.0, Camera.Fov.VERTICAL)
                }
            }
            override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                choreographer.removeFrameCallback(this@FilamentPointCloudView)
                destroy()
            }
        })
    }

    fun setPointCloud(pc: PointCloud, maxPoints: Int = 200_000) {
        val eng = engine ?: return
        if (pointEntity != 0) {
            scene?.removeEntity(pointEntity)
            eng.destroyEntity(pointEntity)
            pointEntity = 0
        }
        val count = min(pc.points.size, maxPoints)
        if (count == 0) return
        val vb = VertexBuffer.Builder()
            .bufferCount(1)
            .vertexCount(count)
            .attribute(VertexBuffer.Attribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 6 * 4)
            .attribute(VertexBuffer.Attribute.COLOR, 0, VertexBuffer.AttributeType.FLOAT3, 3 * 4, 6 * 4)
            .build(eng)

        val ib = IndexBuffer.Builder()
            .indexCount(count)
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .build(eng)

        val floats = FloatArray(count * 6)
        for (i in 0 until count) {
            val p = pc.points[i]
            floats[i * 6 + 0] = p.x
            floats[i * 6 + 1] = p.y
            floats[i * 6 + 2] = p.z
            floats[i * 6 + 3] = p.r.toInt() / 255f
            floats[i * 6 + 4] = p.g.toInt() / 255f
            floats[i * 6 + 5] = p.b.toInt() / 255f
        }

        val vertexBuffer = java.nio.ByteBuffer.allocateDirect(floats.size * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(floats).position(0)
        vb.setBufferAt(eng, 0, VertexBuffer.BufferDescriptor(vertexBuffer))

        val indices = ShortArray(count) { it.toShort() }
        val indexBuffer = java.nio.ByteBuffer.allocateDirect(indices.size * 2).order(java.nio.ByteOrder.nativeOrder()).asShortBuffer()
        indexBuffer.put(indices).position(0)
        ib.setBuffer(eng, IndexBuffer.BufferDescriptor(indexBuffer))

        pointEntity = EntityManager.get().create()
        val renderable = RenderableManager.Builder(1)
            .boundingBox(Box(Float3(-1f), Float3(1f)))
            .geometry(0, RenderableManager.PrimitiveType.POINTS, vb, ib)
            .material(0, material!!)
            .build(eng, pointEntity)

        scene?.addEntity(pointEntity)
        camera?.lookAt(0.0, 0.0, 3.0, 0.0, 0.0, 0.0)
    }

    override fun doFrame(frameTimeNanos: Long) {
        choreographer.postFrameCallback(this)
        val eng = engine ?: return
        val rnd = renderer ?: return
        val sc = swapChain ?: return
        rnd.render(sc, view)
    }

    private fun destroy() {
        val eng = engine ?: return
        if (pointEntity != 0) {
            scene?.removeEntity(pointEntity)
            eng.destroyEntity(pointEntity)
            pointEntity = 0
        }
        view?.let { eng.destroyView(it) }
        camera?.let { eng.destroyCameraComponent(it.entity) }
        scene = null
        view = null
        renderer?.let { /* no-op */ }
        swapChain?.let { eng.destroySwapChain(it) }
        Engine.destroy(eng)
        engine = null
    }
}


