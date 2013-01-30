/*
   Copyright 2013 Harri Smatt

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package fi.harism.effects;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.os.SystemClock;

public class ViewCircles extends ViewBase {
	
	private final int CIRCLE_VERTICES = 1000;
	
	private boolean[] mShaderCompilerSupport = new boolean[1];
	private EffectsShader mShaderBackground = new EffectsShader();
	private EffectsShader mShaderCircle = new EffectsShader();
	private ByteBuffer mBufferQuad;
	private FloatBuffer mBufferCircle;

	public ViewCircles(Context context) {
		super(context);
		
		final byte[] QUAD = { -1, 1, -1, -1, 1, 1, 1, -1 };
		mBufferQuad = ByteBuffer.allocateDirect(8);
		mBufferQuad.put(QUAD).position(0);
		
		mBufferCircle = ByteBuffer.allocateDirect(4 * 4 * CIRCLE_VERTICES).order(ByteOrder.nativeOrder()).asFloatBuffer();
		for (int i = 0; i < CIRCLE_VERTICES; ++i) {
			float t = (float)(2.0 * Math.PI * i / (CIRCLE_VERTICES - 1));
			float d1 = (float)(Math.random() * 0.05 + 0.6);
			float d2 = (float)(Math.random() * 0.05 + 0.4);
			mBufferCircle.put(t).put(d1).put(t).put(d2);
		}
		mBufferCircle.position(0);
		
		setEGLContextClientVersion(2);
		setRenderer(this);
		setRenderMode(RENDERMODE_CONTINUOUSLY);
	}

	@Override
	public void onDrawFrame(GL10 unused) {
		
		if (mShaderCompilerSupport[0] == false) {
			GLES20.glClearColor(0f, 0f, 0f, 0f);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			return;
		}
		
		mShaderBackground.useProgram();
		GLES20.glUniform4f(mShaderBackground.getHandle("uColorInner"), 1.0f, 0.95f, 0.6f, 1.0f);
		GLES20.glUniform4f(mShaderBackground.getHandle("uColorCenter"), 0.7f, 0.5f, 0.2f, 1.0f);
		GLES20.glUniform4f(mShaderBackground.getHandle("uColorOuter"), 0.4f, 0.65f, 1.0f, 1.0f);
		
		GLES20.glVertexAttribPointer(mShaderBackground.getHandle("aPosition"), 2, GLES20.GL_BYTE, false, 0, mBufferQuad);
		GLES20.glEnableVertexAttribArray(mShaderBackground.getHandle("aPosition"));
		
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		
		mShaderCircle.useProgram();
		GLES20.glUniform4f(mShaderCircle.getHandle("uColor"), 1f, 1f, 1f, 1f);
		
		GLES20.glVertexAttribPointer(mShaderCircle.getHandle("aPosition"), 2, GLES20.GL_FLOAT, false, 0, mBufferCircle);
		GLES20.glEnableVertexAttribArray(mShaderCircle.getHandle("aPosition"));
		
		double t = 2.0 * Math.PI * ((SystemClock.uptimeMillis() % 10000) / 10000.0);
		float sin = (float)Math.sin(t);
		float cos = (float)Math.cos(t);
		for (int i = 0; i < 5; ++i) {
			switch (i) {
			case 0:
				GLES20.glUniform2f(mShaderCircle.getHandle("uPosition"), 0.5f * sin, 0.5f * cos);
				break;
			case 1:
				GLES20.glUniform2f(mShaderCircle.getHandle("uPosition"), sin, cos);
				break;
			case 2:
				GLES20.glUniform2f(mShaderCircle.getHandle("uPosition"), cos, sin * 0.7f);
				break;
			case 3:
				GLES20.glUniform2f(mShaderCircle.getHandle("uPosition"), cos, sin * 0.5f - cos);
				break;
			case 4:
				GLES20.glUniform2f(mShaderCircle.getHandle("uPosition"), sin + cos * 0.5f, cos);
				break;
			}
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 2 * CIRCLE_VERTICES);
		}
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		mShaderCircle.useProgram();
		GLES20.glUniform2f(mShaderCircle.getHandle("uAspectRatio"), 1.0f, (float)width / height);
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		// Check if shader compiler is supported.
		GLES20.glGetBooleanv(GLES20.GL_SHADER_COMPILER, mShaderCompilerSupport,
				0);

		// If not, show user an error message and return immediately.
		if (!mShaderCompilerSupport[0]) {
			String msg = getContext().getString(R.string.error_shader_compiler);
			showError(msg);
			return;
		}

		try {
			String vertexSource, fragmentSource;
			vertexSource = loadRawString(R.raw.background_vs);
			fragmentSource = loadRawString(R.raw.background_fs);
			mShaderBackground.setProgram(vertexSource, fragmentSource);
			vertexSource = loadRawString(R.raw.circle_vs);
			fragmentSource = loadRawString(R.raw.circle_fs);
			mShaderCircle.setProgram(vertexSource, fragmentSource);
		} catch (Exception ex) {
			showError(ex.getMessage());
		}
	}

}
