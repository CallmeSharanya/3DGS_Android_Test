package com.example.a3dgs.viewer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.a3dgs.ply.Point
import com.example.a3dgs.ply.PointCloud
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PointCloudRenderer : GLSurfaceView.Renderer {
    private var pointsBuffer: FloatBuffer? = null
    private var numPoints: Int = 0

    private var program: Int = 0
    private val mvpMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)

    fun setPointCloud(pc: PointCloud) {
        numPoints = pc.points.size
        val floats = FloatArray(numPoints * 6)
        var i = 0
        for (p: Point in pc.points) {
            floats[i++] = p.x
            floats[i++] = p.y
            floats[i++] = p.z
            floats[i++] = p.r.toInt() / 255f
            floats[i++] = p.g.toInt() / 255f
            floats[i++] = p.b.toInt() / 255f
        }
        pointsBuffer = ByteBuffer.allocateDirect(floats.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(floats)
        pointsBuffer!!.position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        val vsh = """
            attribute vec3 aPos;
            attribute vec3 aColor;
            uniform mat4 uMVP;
            varying vec3 vColor;
            void main(){
              gl_Position = uMVP * vec4(aPos, 1.0);
              gl_PointSize = 2.0;
              vColor = aColor;
            }
        """
        val fsh = """
            precision mediump float;
            varying vec3 vColor;
            void main(){
              gl_FragColor = vec4(vColor, 1.0);
            }
        """
        program = buildProgram(vsh, fsh)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        Matrix.frustumM(projMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 100f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (pointsBuffer == null || numPoints == 0) return
        val mvp = FloatArray(16)
        Matrix.multiplyMM(mvp, 0, projMatrix, 0, viewMatrix, 0)
        GLES20.glUseProgram(program)
        val uMvp = GLES20.glGetUniformLocation(program, "uMVP")
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        val aPos = GLES20.glGetAttribLocation(program, "aPos")
        val aColor = GLES20.glGetAttribLocation(program, "aColor")
        pointsBuffer!!.position(0)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 6 * 4, pointsBuffer)
        GLES20.glEnableVertexAttribArray(aPos)
        pointsBuffer!!.position(3)
        GLES20.glVertexAttribPointer(aColor, 3, GLES20.GL_FLOAT, false, 6 * 4, pointsBuffer)
        GLES20.glEnableVertexAttribArray(aColor)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, numPoints)
    }

    private fun buildProgram(vsh: String, fsh: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vsh)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fsh)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        return program
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }
}


