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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.awt.GLCanvas;

/**
 * 
 * @author Julien Gouesse
 *
 */
public class JoglAwtGraphics extends JoglGraphicsBase {

	private final JoglAwtDisplayMode desktopMode;
	
	private boolean isFullscreen = false;

	public JoglAwtGraphics (ApplicationListener listener, JoglAwtApplicationConfiguration config) {
		super();
		this.isFullscreen = config.fullscreen;
		initialize(listener, config);
		desktopMode = config.getDesktopDisplayMode();
	}
	
	protected GLCanvas createCanvas(final GLCapabilities caps) {
		return new GLCanvas(caps);
	}
	
	GLCanvas getCanvas () {
		return (GLCanvas) super.getCanvas();
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
		GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice device = genv.getDefaultScreenDevice();
		device.setFullScreenWindow(null);
	}

	@Override
	public boolean supportsDisplayModeChange () {
		GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice device = genv.getDefaultScreenDevice();
		return device.isFullScreenSupported() && (Gdx.app instanceof JoglAwtApplication);
	}

	protected static class JoglAwtDisplayMode extends DisplayMode {
		final java.awt.DisplayMode mode;

		protected JoglAwtDisplayMode (int width, int height, int refreshRate, int bitsPerPixel, java.awt.DisplayMode mode) {
			super(width, height, refreshRate, bitsPerPixel);
			this.mode = mode;
		}
	}

	@Override
	public void setTitle (String title) {
		final Frame frame = findFrame(getCanvas());
		if (frame != null) {
		    frame.setTitle(title);
		}
	}

	@Override
	public DisplayMode getDesktopDisplayMode () {
		return desktopMode;
	}

	@Override
	public boolean isFullscreen () {
		return isFullscreen;
	}
	
	@Override
	public boolean setDisplayMode (int width, int height, boolean fullscreen) {
		if (!supportsDisplayModeChange()) return false;

		if (!fullscreen) {
			isFullscreen = false;
			return setWindowedMode(width, height);
		} else {
			DisplayMode mode = findBestMatch(width, height);
			if (mode == null) return false;
			isFullscreen = true;
			return setDisplayMode(mode);
		}
	}

	protected JoglAwtDisplayMode findBestMatch (int width, int height) {
		DisplayMode[] modes = getDisplayModes();
		//int maxBitDepth = 0;
		DisplayMode best = null;
		for (DisplayMode mode : modes) {
			if (mode.width == width && mode.height == height && mode.bitsPerPixel == desktopMode.bitsPerPixel) {
				//maxBitDepth = mode.bitsPerPixel;
				best = mode;
			}
		}
		return (JoglAwtDisplayMode)best;
	}

	@Override
	public boolean setDisplayMode (DisplayMode displayMode) {
		if (!supportsDisplayModeChange()) return false;

		GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice device = genv.getDefaultScreenDevice();
		final Frame frame = findFrame(getCanvas());
		if (frame == null) return false;

		// create new canvas, sharing the rendering context with the old canvas
		// and pause the animator
		super.pause();
		GLCanvas newCanvas = new GLCanvas(canvas.getChosenGLCapabilities(), null, device);
		newCanvas.setSharedContext(canvas.getContext());
		newCanvas.addGLEventListener(this);

		Frame newframe = new Frame(frame.getTitle());
		newframe.setUndecorated(true);
		newframe.setResizable(false);
		newframe.add(newCanvas, BorderLayout.CENTER);
		newframe.setLocationRelativeTo(null);
		newframe.pack();
		newframe.setVisible(true);

		device.setFullScreenWindow(newframe);
		device.setDisplayMode(((JoglAwtDisplayMode)displayMode).mode);

		initializeGLInstances(canvas);
		this.canvas = newCanvas;
		((JoglAwtInput)Gdx.input).setListeners(getCanvas());
		getCanvas().requestFocus();
		newframe.addWindowListener(((JoglAwtApplication)Gdx.app).windowListener);
		((JoglAwtApplication)Gdx.app).frame = newframe;
		resume();

		Gdx.app.postRunnable(new Runnable() {
			public void run () {
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run () {
						frame.dispose();
					}
				});
			}
		});

		isFullscreen = true;
		return true;
	}

	private boolean setWindowedMode (int width, int height) {

		GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice device = genv.getDefaultScreenDevice();
		if (device.isDisplayChangeSupported()) {
			device.setDisplayMode(desktopMode.mode);
			device.setFullScreenWindow(null);

			final Frame frame = findFrame(getCanvas());
			if (frame == null) return false;

			// create new canvas, sharing the rendering context with the old canvas
			// and pause the animator
			super.pause();
			GLCanvas newCanvas = new GLCanvas(canvas.getChosenGLCapabilities(), null, device);
			newCanvas.setSharedContext(canvas.getContext());
			newCanvas.setBackground(Color.BLACK);
			newCanvas.setPreferredSize(new Dimension(width, height));
			newCanvas.addGLEventListener(this);

			Frame newframe = new Frame(frame.getTitle());
			newframe.setUndecorated(false);
			newframe.setResizable(true);
			newframe.setSize(width + newframe.getInsets().left + newframe.getInsets().right,
				newframe.getInsets().top + newframe.getInsets().bottom + height);
			newframe.add(newCanvas, BorderLayout.CENTER);
			newframe.setLocationRelativeTo(null);
			newframe.pack();
			newframe.setVisible(true);

			initializeGLInstances(canvas);
			this.canvas = newCanvas;
			((JoglAwtInput)Gdx.input).setListeners(getCanvas());
			getCanvas().requestFocus();
			newframe.addWindowListener(((JoglAwtApplication)Gdx.app).windowListener);
			((JoglAwtApplication)Gdx.app).frame = newframe;
			resume();

			Gdx.app.postRunnable(new Runnable() {
				public void run () {
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run () {
							frame.dispose();
						}
					});
				}
			});
		} else {
			final Frame frame = findFrame(getCanvas());
			if (frame == null) return false;
			frame.setSize(width + frame.getInsets().left + frame.getInsets().right, frame.getInsets().top + frame.getInsets().bottom
				+ height);
		}

		return true;
	}

	protected static Frame findFrame (Component component) {
		Container parent = component.getParent();
		while (parent != null) {
			if (parent instanceof Frame) {
				return (Frame)parent;
			}
			parent = parent.getParent();
		}

		return null;
	}

	@Override
	public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot) {
		return null;
	}

	@Override
	public void setCursor(Cursor cursor) {
	}
}
