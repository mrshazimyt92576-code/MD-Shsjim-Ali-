package com.example.media

import android.content.Context
import android.opengl.GLES20
import androidx.annotation.OptIn
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import com.example.data.model.HdrGradingParams
import java.util.concurrent.atomic.AtomicReference

data class HdrShaderRenderState(
    val params: HdrGradingParams = HdrGradingParams.DolbyCinema,
    val splitPosition: Float = 0.5f,
    val splitEnabled: Boolean = true,
    val hdrEnhanced: Boolean = true
)

class HdrShaderController(initialState: HdrShaderRenderState = HdrShaderRenderState()) {
    private val stateRef = AtomicReference(initialState)

    fun update(
        params: HdrGradingParams,
        splitPosition: Float,
        splitEnabled: Boolean,
        hdrEnhanced: Boolean = true
    ) {
        stateRef.set(
            HdrShaderRenderState(
                params = params,
                splitPosition = splitPosition.coerceIn(0f, 1f),
                splitEnabled = splitEnabled,
                hdrEnhanced = hdrEnhanced
            )
        )
    }

    fun current(): HdrShaderRenderState = stateRef.get()
}

@OptIn(UnstableApi::class)
class HdrToneGlEffect(
    private val controller: HdrShaderController
) : GlEffect {

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return HdrToneShaderProgram(useHdr, controller)
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = false
}

@OptIn(UnstableApi::class)
private class HdrToneShaderProgram(
    useHdr: Boolean,
    private val controller: HdrShaderController
) : BaseGlShaderProgram(useHdr, /* texturePoolCapacity= */ 1) {

    private var glProgram: GlProgram? = null

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        // Ensure strictly even dimensions to protect hardware encoders from green-line / 4-way split stride bugs
        val safeW = (inputWidth.coerceAtLeast(2) and 1.inv())
        val safeH = (inputHeight.coerceAtLeast(2) and 1.inv())
        return Size(safeW, safeH)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        val program = glProgram ?: GlProgram(VERTEX_SHADER, FRAGMENT_SHADER).also {
            it.setBufferAttribute(
                "aFramePosition",
                GlUtil.getNormalizedCoordinateBounds(),
                GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE
            )
            val identity = GlUtil.create4x4IdentityMatrix()
            it.setFloatsUniform("uTransformationMatrix", identity)
            it.setFloatsUniform("uTexTransformationMatrix", identity)
            glProgram = it
        }

        val state = controller.current()
        val p = state.params
        val master = if (state.hdrEnhanced && p.intensityEnabled) {
            p.overallIntensity.coerceIn(0f, 1f)
        } else {
            0f
        }

        program.use()
        program.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
        program.setFloatUniform("uHighlightRecovery", if (p.highlightEnabled) p.highlightRecovery else 0f)
        program.setFloatUniform("uShadowLift", if (p.shadowEnabled) p.shadowLift else 0f)
        program.setFloatUniform("uDynamicContrast", if (p.contrastEnabled) p.dynamicContrast else 0f)
        program.setFloatUniform("uSaturationVibrance", if (p.saturationEnabled) p.saturationVibrance else 0f)
        program.setFloatUniform("uBrightnessExposure", if (p.brightnessEnabled) p.brightnessExposure else 0f)
        program.setFloatUniform("uOverallIntensity", master)
        program.setFloatUniform(
            "uSplitX",
            if (state.hdrEnhanced && state.splitEnabled) state.splitPosition else -1.0f
        )

        program.bindAttributesAndUniforms()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GlUtil.checkGlError()
    }

    override fun release() {
        super.release()
        try {
            glProgram?.delete()
        } catch (_: Exception) {
        }
        glProgram = null
    }

    companion object {
        private const val VERTEX_SHADER = """
            #version 100
            attribute vec4 aFramePosition;
            uniform mat4 uTransformationMatrix;
            uniform mat4 uTexTransformationMatrix;
            varying vec2 vTexSamplingCoord;
            void main() {
              gl_Position = uTransformationMatrix * aFramePosition;
              vec4 texCoord = vec4(aFramePosition.x * 0.5 + 0.5, aFramePosition.y * 0.5 + 0.5, 0.0, 1.0);
              vTexSamplingCoord = (uTexTransformationMatrix * texCoord).xy;
            }
        """

        private const val FRAGMENT_SHADER = """
            #version 100
            precision highp float;
            uniform sampler2D uTexSampler;
            uniform float uHighlightRecovery;
            uniform float uShadowLift;
            uniform float uDynamicContrast;
            uniform float uSaturationVibrance;
            uniform float uBrightnessExposure;
            uniform float uOverallIntensity;
            uniform float uSplitX;
            varying vec2 vTexSamplingCoord;

            float luminance(vec3 c) {
              return dot(c, vec3(0.2126, 0.7152, 0.0722));
            }

            void main() {
              vec4 src = texture2D(uTexSampler, vTexSamplingCoord);
              vec3 orig = clamp(src.rgb, 0.0, 1.0);

              // Left side of split preview remains untouched original footage
              if (uSplitX >= 0.0 && vTexSamplingCoord.x < uSplitX) {
                gl_FragColor = vec4(orig, src.a);
                return;
              }

              // Subtle luminous optical hairline on the split divider
              if (uSplitX >= 0.0 && abs(vTexSamplingCoord.x - uSplitX) < 0.0022) {
                gl_FragColor = vec4(0.36, 0.88, 1.0, 1.0);
                return;
              }

              vec3 graded = orig;

              // 1. Exposure / Brightness balance (smooth gain)
              float expGain = pow(2.0, uBrightnessExposure * 0.85);
              graded = clamp(graded * expGain, 0.0, 1.0);

              // 2. Shadow Lift (smooth toe curve preserving true blacks)
              float luma = luminance(graded);
              float shadowWeight = pow(1.0 - clamp(luma, 0.0, 1.0), 2.2);
              float shadowBoost = 1.0 + uShadowLift * 0.95 * shadowWeight;
              graded = clamp(graded * shadowBoost + vec3(uShadowLift * 0.025 * shadowWeight), 0.0, 1.0);

              // 3. Highlight Recovery (filmic shoulder roll-off eliminating clipping)
              luma = luminance(graded);
              float highMask = smoothstep(0.52, 1.0, luma);
              float compressedLuma = luma / (1.0 + uHighlightRecovery * 0.65 * highMask);
              float lumaScale = (luma > 0.0005) ? (compressedLuma / luma) : 1.0;
              graded = clamp(graded * mix(1.0, lumaScale, highMask), 0.0, 1.0);

              // 4. Dynamic Contrast (S-curve around perceptual mid-gray)
              vec3 sCurve = graded * graded * (3.0 - 2.0 * graded);
              graded = mix(graded, sCurve, uDynamicContrast * 0.68);

              // 5. Saturation & Skin-Safe Vibrance
              float postLuma = luminance(graded);
              float maxC = max(graded.r, max(graded.g, graded.b));
              float minC = min(graded.r, min(graded.g, graded.b));
              float sat = maxC - minC;
              float vibranceBoost = 1.0 + uSaturationVibrance * (0.55 + 0.45 * (1.0 - sat));
              graded = clamp(mix(vec3(postLuma), graded, vibranceBoost), 0.0, 1.0);

              // 6. Overall HDR Intensity blend (deterministic frame-to-frame)
              vec3 finalRgb = clamp(mix(orig, graded, uOverallIntensity), 0.0, 1.0);
              gl_FragColor = vec4(finalRgb, src.a);
            }
        """
    }
}
