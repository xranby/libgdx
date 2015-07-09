/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
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
import com.jogamp.opengl.GLCapabilities;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.util.MonitorModeUtil;

/** Implements the {@link Graphics} interface with Jogl.
 * 
 * @author mzechner */
public class JoglGraphics extends JoglGraphicsBase {
	/**
	 * TODO move most of the code into a separate NEWT JoglGraphicsBase implementation and into a NEWT JoglApplicationConfiguration implementation,
	 * implement getDesktopDisplayMode() and move getDisplayModes() into the latter
	 */
	final JoglNewtDisplayMode desktopMode;

	public JoglGraphics (ApplicationListener listener, JoglNewtApplicationConfiguration config) {
		initialize(listener, config);
		getCanvas().setFullscreen(config.fullscreen);
		getCanvas().setUndecorated(config.fullscreen);
		getCanvas().getScreen().addReference();
		desktopMode = config.getDesktopDisplayMode();
	}
	
	protected GLWindow createCanvas(final GLCapabilities caps) {
		return GLWindow.create(caps);
	}
	
	GLWindow getCanvas () {
		return (GLWindow) super.getCanvas();
	}
	
	@Override
	public int getHeight () {
		return getCanvas().getHeight();
	}

	@Override
	public int getWidth () {
		return getCanvas().getWidth();
	}
	
	@Override
	public void create() {
		super.create();
	}
	
	@Override
	public void pause() {
		super.pause();
	}
	
	@Override
	public void resume() {
		super.resume();
	}
	
	@Override
	public void destroy () {
		super.destroy();
		getCanvas().setFullscreen(false);
	}

	@Override
	public boolean supportsDisplayModeChange () {
		return true;
	}

	protected static class JoglNewtDisplayMode extends DisplayMode {
		final MonitorMode mode;

		protected JoglNewtDisplayMode (int width, int height, int refreshRate, int bitsPerPixel, MonitorMode mode) {
			super(width, height, refreshRate, bitsPerPixel);
			this.mode = mode;
		}
	}
	
	@Override
	public DisplayMode getDesktopDisplayMode () {
		return desktopMode;
	}

	@Override
	public void setTitle (String title) {
		getCanvas().setTitle(title);
	}

	@Override
	public boolean setDisplayMode (int width, int height, boolean fullscreen) {
		if (width == getCanvas().getWidth() && height == getCanvas().getHeight() && getCanvas().isFullscreen() == fullscreen) {
			return true;
		}
		MonitorMode targetDisplayMode = null;
		final MonitorDevice monitor = getCanvas().getMainMonitor();
        List<MonitorMode> monitorModes = monitor.getSupportedModes();
		Dimension dimension = new Dimension(width,height);
		monitorModes = MonitorModeUtil.filterByResolution(monitorModes, dimension);
		monitorModes = MonitorModeUtil.filterByRate(monitorModes, getCanvas().getMainMonitor().getCurrentMode().getRefreshRate());
		monitorModes = MonitorModeUtil.getHighestAvailableRate(monitorModes);
		if (monitorModes == null || monitorModes.isEmpty()) {
			return false;
		}
		targetDisplayMode = monitorModes.get(0);
		getCanvas().setUndecorated(fullscreen);
		getCanvas().setFullscreen(fullscreen);
		monitor.setCurrentMode(targetDisplayMode);
		if (Gdx.gl != null) Gdx.gl.glViewport(0, 0, targetDisplayMode.getRotatedWidth(), targetDisplayMode.getRotatedHeight());
		config.width = targetDisplayMode.getRotatedWidth();
		config.height = targetDisplayMode.getRotatedHeight();
		return true;
	}
	
	@Override
	public boolean setDisplayMode (DisplayMode displayMode) {
		MonitorMode screenMode = ((JoglNewtDisplayMode)displayMode).mode;
		
		getCanvas().setUndecorated(true);
		getCanvas().setFullscreen(true);
		getCanvas().getMainMonitor().setCurrentMode(screenMode);
		if (Gdx.gl != null) Gdx.gl.glViewport(0, 0, displayMode.width, displayMode.height);
		config.width = displayMode.width;
		config.height = displayMode.height;
		
		return true;
	}

	@Override
	public boolean isFullscreen () {
		return getCanvas().isFullscreen();
	}
}
