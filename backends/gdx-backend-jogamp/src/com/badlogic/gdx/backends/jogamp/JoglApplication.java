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

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.jogamp.audio.OpenALAudio;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.ObjectMap;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;

/** An implementation of the {@link Application} interface based on Jogl for Windows, Linux and Mac. Instantiate this class with
 * appropriate parameters and then register {@link ApplicationListener} or {@link InputProcessor} instances.
 * 
 * @author mzechner */
public class JoglApplication implements Application {
	JoglGraphics graphics;
	JoglInput input;
	protected JoglNet net;
	JoglFiles files;
	OpenALAudio audio;
	protected final Array<Runnable> runnables = new Array<Runnable>();
	protected final Array<Runnable> executedRunnables = new Array<Runnable>();
	int logLevel = LOG_INFO;
	protected ApplicationListener listener;
	protected final Array<LifecycleListener> lifecycleListeners = new Array<LifecycleListener>();

	/** Creates a new {@link JoglApplication} with the given title and dimensions.
	 * 
	 * @param listener the ApplicationListener implementing the program logic
	 * @param title the title of the application
	 * @param width the width of the surface in pixels
	 * @param height the height of the surface in pixels*/
	public JoglApplication (final ApplicationListener listener, final String title, final int width, final int height) {
		final JoglNewtApplicationConfiguration config = new JoglNewtApplicationConfiguration();
		config.title = title;
		config.width = width;
		config.height = height;
        initialize(listener, config);
	}

	public JoglApplication (final ApplicationListener listener, final JoglNewtApplicationConfiguration config) {
		initialize(listener, config);
	}

	void initialize (ApplicationListener listener, JoglNewtApplicationConfiguration config) {
		JoglNativesLoader.load();
		this.listener = listener;
		graphics = new JoglGraphics(listener, config);
		input = new JoglInput(graphics.getCanvas());
		if (!JoglApplicationConfiguration.disableAudio && Gdx.audio == null) {
			try {
		        audio = new OpenALAudio(config.audioDeviceSimultaneousSources, config.audioDeviceBufferCount, config.audioDeviceBufferSize);
			} catch (Throwable t) {
				log("JoglApplication", "Couldn't initialize audio, disabling audio", t);
				JoglApplicationConfiguration.disableAudio = true;
			}
		} else {
			audio = null;
		}
		files = new JoglFiles();
        net = new JoglNet();
		Gdx.app = this;
		Gdx.graphics = getGraphics();
		Gdx.input = getInput();
		Gdx.audio = getAudio();
		Gdx.files = getFiles();
		Gdx.net = getNet();
		graphics.create();
		graphics.getCanvas().addWindowListener(windowListener);
		graphics.getCanvas().setTitle(config.title);
		graphics.getCanvas().setSize(config.width, config.height);
		graphics.getCanvas().setUndecorated(config.fullscreen);
		graphics.getCanvas().setFullscreen(config.fullscreen);
		graphics.getCanvas().setVisible(true);
	}

	WindowAdapter windowListener = new WindowAdapter() {
		public void windowDestroyed(WindowEvent e) {            
			end();
		}
	};
	
	/** {@inheritDoc} */
	@Override
	public Audio getAudio () {
		return audio;
	}

	/** {@inheritDoc} */
	@Override
	public Files getFiles () {
		return files;
	}

	/** {@inheritDoc} */
	@Override
	public Graphics getGraphics () {
		return graphics;
	}

	/** {@inheritDoc} */
	@Override
	public Input getInput () {
		return input;
	}

	/** {@inheritDoc} */
	@Override
	public ApplicationType getType () {
		return ApplicationType.Desktop;
	}

	@Override
	public int getVersion () {
		return 0;
	}

	@Override
	public long getJavaHeap () {
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}

	@Override
	public long getNativeHeap () {
		return getJavaHeap();
	}

	/** @return the GLCanvas of the application. */
	public GLWindow getGLCanvas () {
		return graphics.getCanvas();
	}

	ObjectMap<String, Preferences> preferences = new ObjectMap<String, Preferences>();

	@Override
	public Preferences getPreferences (String name) {
		if (preferences.containsKey(name)) {
			return preferences.get(name);
		} else {
			Preferences prefs = new JoglPreferences(name);
			preferences.put(name, prefs);
			return prefs;
		}
	}

	@Override
	public Clipboard getClipboard () {
		return new JoglNewtClipboard();
	}
	
	@Override
	public void postRunnable (Runnable runnable) {
		synchronized (runnables) {
			runnables.add(runnable);
			Gdx.graphics.requestRendering();
		}
	}
	
	public boolean executeRunnables () {
		synchronized (runnables) {
			for (int i = runnables.size - 1; i >= 0; i--)
				executedRunnables.add(runnables.get(i));
			runnables.clear();
		}
		if (executedRunnables.size == 0) return false;
		do
			executedRunnables.pop().run();
		while (executedRunnables.size > 0);
		return true;
	}

	@Override
	public void debug (String tag, String message) {
		if (logLevel >= LOG_DEBUG) {
			System.out.println(tag + ": " + message);
		}
	}

	@Override
	public void debug (String tag, String message, Throwable exception) {
		if (logLevel >= LOG_DEBUG) {
			System.out.println(tag + ": " + message);
			exception.printStackTrace(System.out);
		}
	}

	public void log (String tag, String message) {
		if (logLevel >= LOG_INFO) {
			System.out.println(tag + ": " + message);
		}
	}

	@Override
	public void error (String tag, String message) {
		if (logLevel >= LOG_ERROR) {
			System.err.println(tag + ": " + message);
		}
	}

	@Override
	public void error (String tag, String message, Throwable exception) {
		if (logLevel >= LOG_ERROR) {
			System.err.println(tag + ": " + message);
			exception.printStackTrace(System.err);
		}
	}

	@Override
	public void setLogLevel (int logLevel) {
		this.logLevel = logLevel;
	}
	
	/** Called when the game loop has exited. */
	protected void end () {
		synchronized (lifecycleListeners) {
			for (LifecycleListener listener : lifecycleListeners) {
				listener.pause();
				listener.dispose();
			}
		}
		if(audio!=null)
			audio.dispose();
		postRunnable(new Runnable() {
			@Override
			public void run () {
				//FIXME maybe it is a bit brutal (and useless)
				graphics.canvas.destroy();
			}
		});
	}

	@Override
	public void exit () {
		end();
	}

	@Override
	public Net getNet() {
		return net;
	}

    @Override
    public ApplicationListener getApplicationListener () {
        return listener;
    }

    @Override
    public void addLifecycleListener (LifecycleListener listener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.add(listener);
        }
    }

    @Override
    public void removeLifecycleListener (LifecycleListener listener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.removeValue(listener, true);
        }
    }

	@Override
	public void log(String tag, String message, Throwable exception) {
		if (logLevel >= LOG_INFO) {
			System.out.println(tag + ": " + message);
			exception.printStackTrace(System.out);
		}
		
	}

	@Override
	public int getLogLevel() {
		return logLevel;
	}
}
