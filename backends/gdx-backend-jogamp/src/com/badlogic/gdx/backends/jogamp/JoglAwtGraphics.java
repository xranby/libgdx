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
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
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
		initialize(listener, config);
		//getCanvas().setFullscreen(config.fullscreen);
		//getCanvas().setUndecorated(config.fullscreen);
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
		return device.isFullScreenSupported() /*&& (Gdx.app instanceof JoglApplication)*/;
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
		final JFrame frame = findJFrame(getCanvas());
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
		final JFrame frame = findJFrame(getCanvas());
		if (frame == null) return false;

		// create new canvas, sharing the rendering context with the old canvas
		// and pause the animator
		super.pause();
		GLCanvas newCanvas = new GLCanvas(canvas.getChosenGLCapabilities(), null, device);
		newCanvas.setSharedContext(canvas.getContext());
		newCanvas.addGLEventListener(this);

		JFrame newframe = new JFrame(frame.getTitle());
		newframe.setUndecorated(true);
		newframe.setResizable(false);
		newframe.add(newCanvas, BorderLayout.CENTER);
		newframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		newframe.setLocationRelativeTo(null);
		newframe.pack();
		newframe.setVisible(true);

		device.setFullScreenWindow(newframe);
		device.setDisplayMode(((JoglAwtDisplayMode)displayMode).mode);

		initializeGLInstances(canvas);
		this.canvas = newCanvas;
		((JoglAWTInput)Gdx.input).setListeners(getCanvas());
		getCanvas().requestFocus();
		//FIXME
		//newframe.addWindowListener(((JoglApplication)Gdx.app).windowListener);
		//((JoglApplication)Gdx.app).frame = newframe;
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

			final JFrame frame = findJFrame(getCanvas());
			if (frame == null) return false;

			// create new canvas, sharing the rendering context with the old canvas
			// and pause the animator
			super.pause();
			GLCanvas newCanvas = new GLCanvas(canvas.getChosenGLCapabilities(), null, device);
			newCanvas.setSharedContext(canvas.getContext());
			newCanvas.setBackground(Color.BLACK);
			newCanvas.setPreferredSize(new Dimension(width, height));
			newCanvas.addGLEventListener(this);

			JFrame newframe = new JFrame(frame.getTitle());
			newframe.setUndecorated(false);
			newframe.setResizable(true);
			newframe.setSize(width + newframe.getInsets().left + newframe.getInsets().right,
				newframe.getInsets().top + newframe.getInsets().bottom + height);
			newframe.add(newCanvas, BorderLayout.CENTER);
			newframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			newframe.setLocationRelativeTo(null);
			newframe.pack();
			newframe.setVisible(true);

			initializeGLInstances(canvas);
			this.canvas = newCanvas;
			((JoglAWTInput)Gdx.input).setListeners(getCanvas());
			getCanvas().requestFocus();
			//FIXME
			//newframe.addWindowListener(((JoglApplication)Gdx.app).windowListener);
			//((JoglApplication)Gdx.app).frame = newframe;
			resume();

			Gdx.app.postRunnable(new Runnable() {
				public void run () {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run () {
							frame.dispose();
						}
					});
				}
			});
		} else {
			final JFrame frame = findJFrame(getCanvas());
			if (frame == null) return false;
			frame.setSize(width + frame.getInsets().left + frame.getInsets().right, frame.getInsets().top + frame.getInsets().bottom
				+ height);
		}

		return true;
	}

	protected static JFrame findJFrame (Component component) {
		Container parent = component.getParent();
		while (parent != null) {
			if (parent instanceof JFrame) {
				return (JFrame)parent;
			}
			parent = parent.getParent();
		}

		return null;
	}
}
