package com.badlogic.gdx.backends.jogamp;

import java.util.List;

import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.backends.jogamp.JoglGraphics.JoglNewtDisplayMode;
import com.jogamp.newt.Display;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;

public class JoglNewtApplicationConfiguration extends JoglApplicationConfiguration {

	@Override
	public DisplayMode[] getDisplayModes() {
        Display display = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(display,0);
        screen.addReference();
        List<MonitorMode> screenModes = screen.getMonitorModes();
		DisplayMode[] displayModes = new DisplayMode[screenModes.size()];
		for (int modeIndex = 0 ; modeIndex < displayModes.length ; modeIndex++) {
			MonitorMode mode = screenModes.get(modeIndex);
			displayModes[modeIndex] = new JoglNewtDisplayMode(mode.getRotatedWidth(), mode.getRotatedHeight(), (int) mode.getRefreshRate(), mode.getSurfaceSize().getBitsPerPixel(), mode);
		}
		screen.removeReference();
		return displayModes;
	}
	
	@Override
	public JoglNewtDisplayMode getDesktopDisplayMode () {
		Display display = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(display,0);
        screen.addReference();
        MonitorMode mode = screen.getPrimaryMonitor().getCurrentMode();
        JoglNewtDisplayMode desktopMode = new JoglNewtDisplayMode(mode.getRotatedWidth(), mode.getRotatedHeight(), (int) mode.getRefreshRate(), mode.getSurfaceSize().getBitsPerPixel(), mode);
        screen.removeReference();
        return desktopMode;
	}
}
