
package com.mojang.metagun;

import com.badlogic.gdx.backends.jogl.JoglApplication;

public class MetagunDesktopJogl {
	public static void main (String[] argv) {
		new JoglApplication(new Metagun(), "Metagun", 320, 240, false);
	}
}
