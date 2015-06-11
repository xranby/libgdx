/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.backends.jogamp;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;

public abstract class JoglGraphicsBase implements Graphics, GLEventListener {
	static int major, minor;

	GLWindow canvas;
	Animator animator;
	long frameStart = System.nanoTime();
	long lastFrameTime = System.nanoTime();
	float deltaTime = 0;
	int fps;
	int frames;
	boolean paused = true;
	JoglApplicationConfiguration config;

	GL20 gl20;
	GL30 gl30;

	void initialize (JoglApplicationConfiguration config) {
		this.config = config;

		GLCapabilities caps;
		if(config.useGL30){
			caps = new GLCapabilities(GLProfile.getMaxProgrammable(true));
		} else {
			// GL20 uses glDrawElements and glVertexAttribPointer
			// passing buffers, these functions are removed in
			// OpenGL core only contexts.
			// libgdx shaders are currently only GLES2 and GL2
			// compatible.
			// try allocate an GLES2 or GL2 context first
			// before picking a OpenGL core only GL2ES2 context.
			caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
			if (caps == null)
				caps = new GLCapabilities(GLProfile.get(GLProfile.GLES2));
			if (caps == null) {
				// glDrawElements and glVertexAttribPointer
				// not supported by opengl context
				// on this hardware.
				// Get a GL2ES2 context instead
				// (non backward compatible GL3, GL4 &
				// GLES3 etc.. )
				caps = new GLCapabilities(GLProfile.getGL2ES2());
			}
		}
		
		caps.setRedBits(config.r);
		caps.setGreenBits(config.g);
		caps.setBlueBits(config.b);
		caps.setAlphaBits(config.a);
		caps.setDepthBits(config.depth);
		caps.setStencilBits(config.stencil);
		caps.setNumSamples(config.samples);
		caps.setSampleBuffers(config.samples > 0);
		caps.setDoubleBuffered(true);

		canvas = GLWindow.create(caps);
		//canvas.setBackground(Color.BLACK);
		canvas.addGLEventListener(this);

	}

	GLWindow getCanvas () {
		return canvas;
	}

	void create () {
		frameStart = System.nanoTime();
		lastFrameTime = frameStart;
		deltaTime = 0;
		animator = new Animator(canvas);
		animator.start();
	}

	void pause () {
		synchronized (this) {
			paused = true;
		}
		animator.stop();
	}

	void resume () {
		paused = false;
		frameStart = System.nanoTime();
		lastFrameTime = frameStart;
		deltaTime = 0;
		animator.resume();
		animator.setRunAsFastAsPossible(true);
		animator.start();
	}

	void initializeGLInstances (GLAutoDrawable drawable) {
		major = drawable.getGL().getContext().getGLVersionNumber().getMajor();
		minor = drawable.getGL().getContext().getGLVersionNumber().getMinor();

		if (config.useGL30 && major >= 3) {
			gl30 = new JoglGL30();
			gl20 = gl30;
		} else {
			gl20 = new JoglGL20();
		}

		Gdx.gl = gl20;
		Gdx.gl20 = gl20;
		Gdx.gl30 = gl30;
		
		if (major <= 1)
			throw new GdxRuntimeException("OpenGL 2.0 or higher with the FBO extension is required. OpenGL version: " + major + "." + minor);
		if (major == 2 && !drawable.getGL().isGLES2Compatible()) {
			if (!supportsExtension("GL_EXT_framebuffer_object") && !supportsExtension("GL_ARB_framebuffer_object")) {
				final String vendor = drawable.getGL().glGetString(GL.GL_VENDOR);
				final String renderer = drawable.getGL().glGetString(GL.GL_RENDERER);
				final String version = drawable.getGL().glGetString(GL.GL_VERSION);
				final String glInfo = vendor + "\n" + renderer + "\n" + version;
				throw new GdxRuntimeException("OpenGL 2.0 or higher with the FBO extension is required. OpenGL version: " + major + "." + minor
					+ ", FBO extension: false" + (glInfo.isEmpty() ? "" : ("\n" + glInfo)));
			}
		}
	}

	void updateTimes () {
		deltaTime = (System.nanoTime() - lastFrameTime) / 1000000000.0f;
		lastFrameTime = System.nanoTime();

		if (System.nanoTime() - frameStart > 1000000000) {
			fps = frames;
			frames = 0;
			frameStart = System.nanoTime();
		}
		frames++;
	}

	@Override
	public float getDeltaTime () {
		return deltaTime;
	}

	@Override
	public float getRawDeltaTime () {
		return deltaTime;
	}

	@Override
	public int getFramesPerSecond () {
		return fps;
	}

	@Override
	public int getHeight () {
		return canvas.getHeight();
	}

	@Override
	public int getWidth () {
		return canvas.getWidth();
	}

	@Override
	public GL20 getGL20 () {
		return gl20;
	}
	
	@Override
	public GL30 getGL30 () {
		return gl30;
	}

	@Override
	public GraphicsType getType () {
		return GraphicsType.JoglGL;
	}

	@Override
	public void requestRendering () {
		//TODO: fix recursive loop
		//canvas.display();
	}
}
