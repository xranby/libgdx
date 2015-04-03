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

package com.badlogic.gdx.tests.jogl;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.jogamp.JoglApplication;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;

public class LocalJoglTest extends ApplicationAdapter {
	ShapeRenderer renderer;
	OrthographicCamera camera;
	float[] coords = {-2.0f, 0.0f, -2.0f, 0.5f, 0.0f, 1.0f, 0.5f, 2.875f, 1.0f, 0.5f, 1.5f, 1.0f, 2.0f, 1.0f, 2.0f, 0.0f};
	private ShortArray triangles;
	private FloatArray polygon;

	@Override
	public void create () {
		renderer = new ShapeRenderer();
		camera = new OrthographicCamera(10, 10);
		camera.position.set(0, 0, 0);
		camera.update();

		
		polygon = new FloatArray(coords);
		triangles = new EarClippingTriangulator().computeTriangles(polygon);
	}

	@Override
	public void render () {
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		//Gdx.gl20.glColor4f(1, 1, 1, 1);

		renderer.setProjectionMatrix(camera.combined);
		renderer.setColor(1, 1, 1, 1);
		renderer.begin(ShapeType.Line);
		for (int j = 0; j < coords.length - 2; j += 2) {
			renderer.line(coords[j], coords[j + 1], coords[j + 2], coords[j + 3]);
		}
		renderer.line(coords[0], coords[1], coords[coords.length - 2], coords[coords.length - 1]);
		renderer.end();

		renderer.setColor(1, 0, 0, 1);
		renderer.translate(0, -4, 0);
		renderer.begin(ShapeType.Filled);//FIXME is it correct?
		for (int i = 0; i < triangles.size; i += 3) {
			float v1x = polygon.get(triangles.get(i) * 2);
			float v1y = polygon.get(triangles.get(i) * 2 + 1);
			float v2x = polygon.get(triangles.get(i + 1) * 2);
			float v2y = polygon.get(triangles.get(i + 1) * 2 + 1);
			float v3x = polygon.get(triangles.get(i + 2) * 2);
			float v3y = polygon.get(triangles.get(i + 2) * 2 + 1);
			
			renderer.triangle(v1x, v1y, v2x, v2y, v3x, v3y);
		}
		renderer.end();
		renderer.identity();
	}

	public static void main (String[] argv) {
		new JoglApplication(new LocalJoglTest(), "test", 480, 320);
	}
}
