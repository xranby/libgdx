package de.swagner.paxbritannica;

import com.badlogic.gdx.backends.jogl.JoglApplication;

public class PaxBritannicaDesktop {
	public static void main(String[] args) {
	//	new JoglApplication(new PaxBritannica(),
	//			"Pax Britannica", 1024, 550,false);
		new JoglApplication(new PaxBritannica(),
		"Pax Britannica", 1920, 1080,false);
	}
}
