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

import java.util.List;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.jogamp.audio.OpenALAudio;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.Screen;
import com.jogamp.newt.util.MonitorModeUtil;

/** Implements the {@link Graphics} interface with Jogl.
 * 
 * @author mzechner */
public class JoglGraphics extends JoglGraphicsBase {
	ApplicationListener listener = null;
	boolean created = false;
	boolean exclusiveMode = false;
	final JoglDisplayMode desktopMode;
	final JoglApplicationConfiguration config;
	String extensions;

	public JoglGraphics (ApplicationListener listener, JoglApplicationConfiguration config) {
		initialize(config);
		if (listener == null) throw new GdxRuntimeException("RenderListener must not be null");
		this.listener = listener;
		this.config = config;
		canvas.setFullscreen(config.fullscreen);
		canvas.setUndecorated(config.fullscreen);
		canvas.getScreen().addReference();
		MonitorMode mode = canvas.getMainMonitor().getCurrentMode();
		//FIXME use JoglApplicationConfiguration.getDesktopDisplayMode ()
		desktopMode = (JoglDisplayMode) new JoglDisplayMode(mode.getRotatedWidth(), mode.getRotatedHeight(), (int) mode.getRefreshRate(), mode.getSurfaceSize().getBitsPerPixel(), mode);
	}

	public void create () {
		super.create();
	}

	public void pause () {
		super.pause();
		if (!canvas.getContext().isCurrent()) {
		    canvas.getContext().makeCurrent();
		}
		listener.pause();
	}

	public void resume () {
		if (!canvas.getContext().isCurrent()) {
		    canvas.getContext().makeCurrent();
		}
		listener.resume();
		super.resume();
	}

	@Override
	public void init (GLAutoDrawable drawable) {
		initializeGLInstances(drawable);
		setVSync(config.vSyncEnabled);

		if (!created) {
			listener.create();
			synchronized (this) {
				paused = false;
			}
			created = true;
		}
	}

	@Override
	public void reshape (GLAutoDrawable drawable, int x, int y, int width, int height) {
		listener.resize(width, height);
	}

	@Override
	public void display (GLAutoDrawable arg0) {
		synchronized (this) {
			if (!paused) {
				updateTimes();
				synchronized (((JoglApplication)Gdx.app).runnables) {
					JoglApplication app = ((JoglApplication)Gdx.app);
					app.executedRunnables.clear();
					app.executedRunnables.addAll(app.runnables);
					app.runnables.clear();

					for (int i = 0; i < app.executedRunnables.size(); i++) {
						try {
							app.executedRunnables.get(i).run();
						} catch (Throwable t) {
							t.printStackTrace();
						}
					}
				}
				((JoglInput)(Gdx.input)).processEvents();
				listener.render();
				((OpenALAudio)Gdx.audio).update();
			}
		}
	}

	public void destroy () {
		if (!canvas.getContext().isCurrent()) {
		    canvas.getContext().makeCurrent();
		}
		listener.dispose();
		canvas.setFullscreen(false);
	}

	private float getScreenResolution() {
		Screen screen = canvas.getScreen();
		screen.addReference();
		MonitorMode mmode = canvas.getMainMonitor().getCurrentMode();
		final DimensionImmutable sdim = canvas.getMainMonitor().getSizeMM();
		final DimensionImmutable spix = mmode.getSurfaceSize().getResolution();
        float screenResolution = (float)spix.getWidth() / (float)sdim.getWidth();
        canvas.getScreen().removeReference();
        return(screenResolution);
	}
	
	@Override
	public float getPpiX () {
		return getScreenResolution();
	}

	@Override
	public float getPpiY () {
		return getScreenResolution();
	}

	@Override
	public float getPpcX () {
		return (getScreenResolution() / 2.54f);
	}

	@Override
	public float getPpcY () {
		return (getScreenResolution() / 2.54f);
	}

	@Override
	public float getDensity () {
		return (getScreenResolution() / 160f);
	}

	@Override
	public boolean supportsDisplayModeChange () {
		return true;
	}

	protected static class JoglDisplayMode extends DisplayMode {
		final MonitorMode mode;

		protected JoglDisplayMode (int width, int height, int refreshRate, int bitsPerPixel, MonitorMode mode) {
			super(width, height, refreshRate, bitsPerPixel);
			this.mode = mode;
		}
	}

	@Override
	public DisplayMode[] getDisplayModes () {
		//FIXME use JoglApplicationConfiguration.getDisplayModes()
		List<MonitorMode> screenModes = canvas.getScreen().getMonitorModes();
		DisplayMode[] displayModes = new DisplayMode[screenModes.size()];
		for (int modeIndex = 0 ; modeIndex < displayModes.length ; modeIndex++) {
			MonitorMode mode = screenModes.get(modeIndex);
			displayModes[modeIndex] = new JoglDisplayMode(mode.getRotatedWidth(), mode.getRotatedHeight(), (int) mode.getRefreshRate(), mode.getSurfaceSize().getBitsPerPixel(), mode);
		}
		return displayModes;
	}

	@Override
	public void setTitle (String title) {
		canvas.setTitle(title);
	}

	@Override
	public DisplayMode getDesktopDisplayMode () {
		return desktopMode;
	}

	@Override
	public boolean setDisplayMode (int width, int height, boolean fullscreen) {
		if (width == canvas.getWidth() && height == canvas.getHeight() && canvas.isFullscreen() == fullscreen) {
			return true;
		}
		MonitorMode targetDisplayMode = null;
		final MonitorDevice monitor = canvas.getMainMonitor();
        List<MonitorMode> monitorModes = monitor.getSupportedModes();
		Dimension dimension = new Dimension(width,height);
		monitorModes = MonitorModeUtil.filterByResolution(monitorModes, dimension);
		monitorModes = MonitorModeUtil.filterByRate(monitorModes, canvas.getMainMonitor().getCurrentMode().getRefreshRate());
		monitorModes = MonitorModeUtil.getHighestAvailableRate(monitorModes);
		if (monitorModes == null || monitorModes.isEmpty()) {
			return false;
		}
		targetDisplayMode = monitorModes.get(0);
		canvas.setUndecorated(fullscreen);
		canvas.setFullscreen(fullscreen);
		monitor.setCurrentMode(targetDisplayMode);
		if (Gdx.gl != null) Gdx.gl.glViewport(0, 0, targetDisplayMode.getRotatedWidth(), targetDisplayMode.getRotatedHeight());
		config.width = targetDisplayMode.getRotatedWidth();
		config.height = targetDisplayMode.getRotatedHeight();
		return true;
	}
	
	@Override
	public boolean setDisplayMode (DisplayMode displayMode) {
		MonitorMode screenMode = ((JoglDisplayMode)displayMode).mode;
		
		canvas.setUndecorated(true);
		canvas.setFullscreen(true);
		canvas.getMainMonitor().setCurrentMode(screenMode);
		if (Gdx.gl != null) Gdx.gl.glViewport(0, 0, displayMode.width, displayMode.height);
		config.width = displayMode.width;
		config.height = displayMode.height;
		
		return true;
	}

	@Override
	public void setVSync (boolean vsync) {
		if (vsync)
			canvas.getGL().setSwapInterval(1);
		else
			canvas.getGL().setSwapInterval(0);
	}

	@Override
	public BufferFormat getBufferFormat () {
		GLCapabilitiesImmutable caps = canvas.getChosenGLCapabilities();
		return new BufferFormat(caps.getRedBits(), caps.getGreenBits(), caps.getBlueBits(), caps.getAlphaBits(),
			caps.getDepthBits(), caps.getStencilBits(), caps.getNumSamples(), false);
	}

	@Override
	public boolean supportsExtension (String extension) {
		if (extensions == null) extensions = Gdx.gl.glGetString(GL20.GL_EXTENSIONS);
		return extensions.contains(extension);
	}

	@Override
	public boolean isFullscreen () {
		return canvas.isFullscreen();
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		setContinuousRendering(true);
        pause();
        destroy();
	}

	@Override
	public void setContinuousRendering(boolean isContinuous) {
	}

	@Override
	public boolean isContinuousRendering() {
		return true;
	}

	@Override
	public boolean isGL30Available() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long getFrameId() {
		// TODO Auto-generated method stub
		return 0;
	}
}
