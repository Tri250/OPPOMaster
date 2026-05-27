package com.omaster.app.service

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.omaster.app.model.CameraParams
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class RealtimePreviewRenderer(
    private val context: Context
) : GLSurfaceView.Renderer {

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        attribute vec2 vTexCoord;
        varying vec2 texCoord;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
            texCoord = vTexCoord;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision highp float;
        uniform sampler2D uTexture;
        uniform float uContrast;
        uniform float uSaturation;
        uniform float uTemperature;
        uniform float uVignette;
        varying vec2 texCoord;
        
        void main() {
            vec4 color = texture2D(uTexture, texCoord);
            
            color.rgb = (color.rgb - 0.5) * uContrast + 0.5;
            
            float lum = dot(color.rgb, vec3(0.299, 0.587, 0.114));
            color.rgb = mix(vec3(lum), color.rgb, uSaturation);
            
            color.r += uTemperature * 0.15;
            color.b -= uTemperature * 0.15;
            
            float dist = length(texCoord - 0.5);
            color.rgb *= 1.0 - dist * uVignette;
            
            gl_FragColor = color;
        }
    """.trimIndent()

    private var program: Int = 0
    private var textureId: Int = 0
    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    private var contrast: Float = 1.0f
    private var saturation: Float = 1.0f
    private var temperature: Float = 0.0f
    private var vignette: Float = 0.0f

    private val triangleVertices = floatArrayOf(
        -1.0f, 1.0f, 0.0f,
        -1.0f, -1.0f, 0.0f,
        1.0f, 1.0f, 0.0f,
        1.0f, -1.0f, 0.0f
    )

    private val textureVertices = floatArrayOf(
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        1.0f, 1.0f
    )

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var textureBuffer: FloatBuffer

    init {
        initBuffers()
    }

    private fun initBuffers() {
        vertexBuffer = ByteBuffer.allocateDirect(triangleVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(triangleVertices)
                position(0)
            }

        textureBuffer = ByteBuffer.allocateDirect(textureVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(textureVertices)
                position(0)
            }
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        textureId = textureIds[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(program)

        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)

        val texCoordHandle = GLES20.glGetAttribLocation(program, "vTexCoord")
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureBuffer)

        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, vPMatrix, 0)

        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uContrast"), contrast)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uSaturation"), saturation)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uTemperature"), temperature)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uVignette"), vignette)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }

    fun updateParams(params: CameraParams) {
        contrast = params.contrast
        saturation = params.saturation
        temperature = if (params.wb.startsWith("5")) 0f else if (params.wb.toInt() > 5500) 0.5f else -0.5f
        vignette = params.vignette
    }

    fun enableSplitCompare(enable: Boolean) {
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}
