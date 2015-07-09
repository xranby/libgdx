package com.badlogic.gdx.backends.jogamp;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import com.badlogic.gdx.backends.jogamp.JoglAwtGraphics.JoglAwtDisplayMode;
import com.badlogic.gdx.Graphics.DisplayMode;

public class JoglAwtApplicationConfiguration extends JoglApplicationConfiguration {

	public DisplayMode getDesktopDisplayMode () {
		GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice device = genv.getDefaultScreenDevice();
		java.awt.DisplayMode mode = device.getDisplayMode();
		return new JoglAwtDisplayMode(mode.getWidth(), mode.getHeight(), mode.getRefreshRate(), mode.getBitDepth(), mode);
	}

	public DisplayMode[] getDisplayModes () {
		GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice device = genv.getDefaultScreenDevice();
		java.awt.DisplayMode desktopMode = device.getDisplayMode();
		java.awt.DisplayMode[] displayModes = device.getDisplayModes();
		ArrayList<DisplayMode> modes = new ArrayList<DisplayMode>();
		for (java.awt.DisplayMode mode : displayModes) {
			boolean duplicate = false;
			for (int i = 0; i < modes.size(); i++) {
				if (modes.get(i).width == mode.getWidth() && modes.get(i).height == mode.getHeight()
					&& modes.get(i).bitsPerPixel == mode.getBitDepth()) {
					duplicate = true;
					break;
				}
			}
			if (duplicate) continue;
			if (mode.getBitDepth() != desktopMode.getBitDepth()) continue;
			modes.add(new JoglAwtDisplayMode(mode.getWidth(), mode.getHeight(), mode.getRefreshRate(), mode.getBitDepth(), mode));
		}

		return modes.toArray(new DisplayMode[modes.size()]);
	}
}
