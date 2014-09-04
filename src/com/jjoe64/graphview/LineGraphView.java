/**
 * This file is part of GraphView.
 *
 * GraphView is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GraphView is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GraphView.  If not, see <http://www.gnu.org/licenses/lgpl.html>.
 *
 * Copyright Jonas Gehring
 */

package com.jjoe64.graphview;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;

import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;

/**
 * Line Graph View. This draws a line chart.
 */
public class LineGraphView extends GraphView {
	private final Paint paintBackground;
	private List<Integer> drawBackgroundIndex = new ArrayList<Integer>();
	private float dataPointsRadius = 10f;
	private List<Integer> drawDataIndex = new ArrayList<Integer>();
	private float graphwidth = -1;
	private int i;

	public LineGraphView(Context context, AttributeSet attrs) {
		super(context, attrs);

		paintBackground = new Paint();
		paintBackground.setColor(Color.rgb(20, 40, 60));
		paintBackground.setStrokeWidth(4);
		paintBackground.setAlpha(128);
	}

	public LineGraphView(Context context, String title) {
		super(context, title);

		paintBackground = new Paint();
		paintBackground.setColor(Color.rgb(20, 40, 60));
		paintBackground.setStrokeWidth(4);
		paintBackground.setAlpha(128);
	}

	@Override
	public double getRealYTimeValue(int index, double percentPosition, Point screenSize) {
		final GraphViewDataInterface[] values = _values(index);
		double lastX = 0;

		double minX = getMinX(false);
		double maxX = getMaxX(false);
		double diffX = maxX - minX;

		final double screenPosition = (double) percentPosition * graphwidth;

		for (int i = 0; i < values.length; i++) {
			// X
			double valX = values[i].getX() - minX;
			double ratX = valX / diffX;
			double x = (graphwidth * ratX);

			if (x > screenPosition) {
				double n1 = values[i].getY();
				double n = values[Math.max(0, i - 1)].getY();
				final double fraction = (double) (screenPosition - lastX) / (x - lastX);
				Log.e("DEBUG", "fraction=" + fraction);
				if (fraction > 0.5) {
					return n1;
				} else {
					return n;
				}
			}
			lastX = x;

		}

		return 0;
	}

	@Override
	public void drawSeries(Canvas canvas, int index, float graphwidth, float graphheight, float border, long minX,
			double minY, long diffX, double diffY, float horstart, GraphViewSeriesStyle style) {
		// draw background

		GraphViewSeries serie = graphSeries.get(index);

		GraphViewDataInterface[] values = _values(index);
		double lastEndY = 0;
		double lastEndX = 0;

		if (this.graphwidth == -1) {
			this.graphwidth = graphwidth;
		}

		// draw data
		paint.setStrokeWidth(style.thickness);
		paint.setColor(style.color);

		Path bgPath = null;
		if (drawBackgroundIndex.contains(index)) {
			bgPath = new Path();
		}

		lastEndY = 0;
		lastEndX = 0;
		float firstX = 0;
		for (int i = 0; i < values.length; i++) {

			// Y
			double valY = values[i].getY() - minY;
			double ratY = valY / diffY;
			double y = graphheight * ratY;
			// X
			double valX = values[i].getX() - minX;
			double ratX = valX / diffX;
			double x = graphwidth * ratX;
			if (i > 0) {
				float startX = (float) lastEndX + (horstart + 1);
				float startY = (float) (border - lastEndY) + graphheight;
				float endX = (float) x + (horstart + 1);
				float endY = (float) (border - y) + graphheight;

				// Log.e("DEBUG", "startX=" + startX);

				// Log.e("DEBUG", "endX" + endX);

				// draw data point
				if (drawDataIndex.contains(index)) {
					// fix: last value was not drawn. Draw here now the end values
					canvas.drawCircle(endX, endY, dataPointsRadius, paint);
				}

				canvas.drawLine(startX, startY, endX, endY, paint);
				if (bgPath != null) {
					if (i == 1) {
						firstX = startX;
						bgPath.moveTo(startX, startY);
					}
					bgPath.lineTo(endX, endY);
				}
			} else if (drawDataIndex.contains(index)) {
				// fix: last value not drawn as datapoint. Draw first point here, and then on every step the end values
				// (above)
				float first_X = (float) x + (horstart + 1);
				float first_Y = (float) (border - y) + graphheight;
				canvas.drawCircle(first_X, first_Y, dataPointsRadius, paint);

			}
			lastEndY = y;
			lastEndX = x;
		}

		if (bgPath != null) {
			// end / close path
			bgPath.lineTo((float) lastEndX, graphheight + border);
			bgPath.lineTo(firstX, graphheight + border);
			bgPath.close();
			canvas.drawPath(bgPath, paintBackground);
		}
	}

	public int getBackgroundColor() {
		return paintBackground.getColor();
	}

	public float getDataPointsRadius() {
		return dataPointsRadius;
	}

	public List<Integer> getDrawBackground() {
		return drawBackgroundIndex;
	}

	public List<Integer> getDrawDataPointsIndex() {
		return this.drawDataIndex;
	}

	/**
	 * sets the background color for the series. This is not the background color of the whole graph.
	 * 
	 * @see #setDrawBackground(boolean)
	 */
	@Override
	public void setBackgroundColor(int color) {
		paintBackground.setColor(color);
	}

	/**
	 * sets the radius of the circles at the data points.
	 * 
	 * @see #setDrawDataPoints(boolean)
	 * @param dataPointsRadius
	 */
	public void setDataPointsRadius(float dataPointsRadius) {
		this.dataPointsRadius = dataPointsRadius;
	}

	/**
	 * @param drawBackground
	 *            true for a light blue background under the graph line
	 * @see #setBackgroundColor(int)
	 */
	public void addDrawBackground(int index) {
		this.drawBackgroundIndex.add(index);
	}

	/**
	 * You can set the flag to let the GraphView draw circles at the data points
	 * 
	 * @see #setDataPointsRadius(float)
	 * @param drawDataPoints
	 */
	public void addDrawDataPoints(int index) {
		this.drawDataIndex.add(index);
	}

	public void addSeries(GraphViewSeries series, boolean drawPoints, boolean drawBackgroundColor) {
		super.addSeries(series);
		final int index = graphSeries.size() - 1;
		if (drawPoints) {
			addDrawDataPoints(index);
		}
		if (drawBackgroundColor) {
			addDrawBackground(index);
		}
	}

}
