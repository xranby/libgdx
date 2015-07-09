package com.badlogic.gdx.backends.jogamp;

import com.badlogic.gdx.utils.Clipboard;

/**
 * Clipboard based on NEWT. As this toolkit doesn't support this kind of feature yet, this class is a dummy implementation.
 * 
 * @author Julien Gouesse
 *
 */
public class JoglNewtClipboard implements Clipboard {
	
	public JoglNewtClipboard() {
		super();
	}

	@Override
	public String getContents() {
		return null;
	}

	@Override
	public void setContents(String content) {
	}

}
