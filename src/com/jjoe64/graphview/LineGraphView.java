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
import android.util.AttributeSet;

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

	public double getFraction(double screenPosition, double lastX, double x) {
		return (double) (screenPosition - lastX) / (x - lastX);
	}

	@Override
	public double getRealYTimeValue(int index, double percentPosition) {
		final GraphViewDataInterface[] values = _values(index);

		double lastX = 0, lastY = 0;

		double minX = getMinX(false);
		double maxX = getMaxX(false);
		double diffX = maxX - minX;

		final double screenPosition = (double) percentPosition * graphwidth;

		for (int i = 0; i < values.length; i++) {
			// X
			double valX = values[i].getX() - minX;
			double ratX = valX / diffX;
			double x = (graphwidth * ratX);
			double n1 = values[i].getY();
			if (x > screenPosition) {
				double n = values[Math.max(0, i - 1)].getY();

				final double fraction = getFraction(screenPosition, lastX, x);
				if (fraction > 0.5) {
					return n1;
				} else {
					return n;
				}
			}
			lastX = x;
			lastY = n1;

		}

		return lastY;
	}

	public int getIndex(GraphViewDataInterface[] values, int index, double screenPosition, double minX, double diffX,
			double x, double x1) {

		final int index1 = Math.min(values.length - 1, index + 1);

		// Log.e("DEBUG", "x[" + index + "]= " + x);
		// Log.e("DEBUG", "x1[" + index1 + "]= " + x1);

		final double fraction = getFraction(screenPosition, x, x1);
		// Log.e("DEBUG", "fraction=" + fraction);
		if (fraction > 0.5) {
			return index1;
		} else {
			return index;
		}
	}

	public PaintHolder getCirclePainted(GraphViewDataInterface[] values, int i, double x, double screenPosition,
			boolean selectedFound, double minX, double diffX) {
		final PaintHolder paintHolder = new PaintHolder();
		// Log.e("DEBUG", "x=" + x);
		// Log.e("DEBUG", "screenPosition=" + screenPosition);
		// Log.e("DEBUG", "selectedFound=" + selectedFound);
		// Log.e("DEBUG", "i=" + i);

		final int index1 = Math.min(values.length - 1, i + 1);
		final double valX1 = values[index1].getX() - minX;
		final double ratX1 = valX1 / diffX;
		final double x1 = (graphwidth * ratX1);

		if ((x1 > screenPosition) && (selectedFound == false)) {
			final int newIndex = getIndex(values, i, screenPosition, minX, diffX, x, x1);
			// Log.e("DEBUG", "newIndex=" + newIndex);
			paintHolder.paint = getCirclePaint(newIndex == i);
			paintHolder.isSelected = (newIndex == i);
		} else {
			paintHolder.isSelected = false;
			paintHolder.paint = paint;
		}

		return paintHolder;
	}

	public Paint getCirclePaint(boolean isSelected) {
		if (isSelected) {
			return paint;
		} else {
			return paint;
		}
	}

	public static class PaintHolder {
		public Paint paint;
		public boolean isSelected;

		public PaintHolder() {
		}
	}

	@Override
	public void drawSeries(Canvas canvas, int index, float graphwidth, float graphheight, float border, long minX,
			double minY, long diffX, double diffY, float horstart, GraphViewSeriesStyle style) {
		// draw background

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

		lastEndY = 0;
		lastEndX = 0;
		float firstX = 0;

		final double screenPosition = (double) xCursor * graphwidth;

		PaintHolder paintHolder = null;
		boolean selectedFound = false;

		// final GraphViewSeries serie = graphSeries.get(index);
		// Log.e("DEBUG", "serie=" + serie.description);

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

				// draw data point
				if (drawDataIndex.contains(index)) {
					// fix: last value was not drawn. Draw here now the end values
					paintHolder = getCirclePainted(values, i, endX, screenPosition, selectedFound, minX, diffX);
					if (!selectedFound) {
						selectedFound = paintHolder.isSelected;
					}
					canvas.drawCircle(endX, endY, dataPointsRadius, paintHolder.paint);
				}

				canvas.drawLine(startX, startY, endX, endY, paint);

				if ((drawBackgroundIndex.contains(index)) && (bgPath == null)) {
					bgPath = new Path();
				}

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

				paintHolder = getCirclePainted(values, i, first_X, screenPosition, selectedFound, minX, diffX);
				if (!selectedFound) {
					selectedFound = paintHolder.isSelected;
				}
				canvas.drawCircle(first_X, first_Y, dataPointsRadius, paintHolder.paint);

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
