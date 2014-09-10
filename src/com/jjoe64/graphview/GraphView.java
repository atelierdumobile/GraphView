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

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.GraphViewStyle.GridStyle;
import com.jjoe64.graphview.compatible.ScaleGestureDetector;
import com.jjoe64.graphview.utils.DisplayUtils;
import com.jjoe64.graphview.utils.DisplayUtils.DisplayMode;

/**
 * GraphView is a Android View for creating zoomable and scrollable graphs. This is the abstract base class for all
 * graphs. Extend this class and implement
 * {@link #drawSeries(Canvas, GraphViewDataInterface[], float, float, float, double, double, double, double, float)} to
 * display a custom graph. Use {@link LineGraphView} for creating a line chart.
 * 
 * @author jjoe64 - jonas gehring - http://www.jjoe64.com
 * 
 *         Copyright (C) 2011 Jonas Gehring Licensed under the GNU Lesser General Public License (LGPL)
 *         http://www.gnu.org/licenses/lgpl.html
 */

abstract public class GraphView extends LinearLayout {

	public static final String GRAPH_NO_DATA_TAG = "GRAPH_NO_DATA_TAG";

	static final private class GraphViewConfig {
		static final float BORDER = 20;
	}

	public static interface GraphDataListener {
		public void onTouchValueRequest(String tag, double x, double y);

		public void onTouchValueClear();
	}

	private static final long HIDE_DELAY = 500;

	private SimpleDateFormat dateFormatter;

	public void setDisplayMode(DisplayMode currentDisplayMode) {
		this.displayMode = currentDisplayMode;
	}

	public DisplayMode getDisplayMode() {
		return displayMode;
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public static void invalidateView(View view) {
		if (Build.VERSION.SDK_INT >= 16) {
			view.postInvalidateOnAnimation();
		} else {
			view.postInvalidate();
		}
	}

	private class GraphViewContentView extends View {
		private float lastTouchEventX;
		private float graphwidth;
		private boolean scrollingStarted;
		private int lastScreenPosition = 0;
		private Path valueIndicatorPath;
		private Paint valueIndicatorPaint;
		private int valueIndiatorSize;

		/**
		 * @param context
		 */
		public GraphViewContentView(Context context) {
			super(context);
			setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			valueIndicatorPath = new Path();
			valueIndicatorPaint = new Paint();
			valueIndicatorPaint.setColor(android.graphics.Color.RED);
			valueIndicatorPaint.setStyle(Paint.Style.FILL_AND_STROKE);
			valueIndicatorPaint.setAntiAlias(true);
			valueIndiatorSize = context.getResources().getDimensionPixelSize(R.dimen.graphview_valueIndicator_size);
		}

		/**
		 * @param canvas
		 */
		@Override
		protected void onDraw(Canvas canvas) {

			paint.setAntiAlias(true);

			// normal
			paint.setStrokeWidth(0);

			float border = GraphViewConfig.BORDER;
			float horstart = 0;
			float height = getHeight();
			float width = getWidth() - 1;
			double maxY = getMaxY();
			double minY = getMinY();
			long maxX = getMaxX(false);
			long minX = getMinX(false);
			long diffX = maxX - minX;

			// measure bottom text
			if (labelTextHeight == null || horLabelTextWidth == null) {
				paint.setTextSize(getGraphViewStyle().getTextSize());
				long testX = (long) (((getMaxX(true) - getMinX(true)) * 0.783) + getMinX(true));
				String testLabel = formatLabel(testX, 0, true);
				paint.getTextBounds(testLabel, 0, testLabel.length(), textBounds);
				labelTextHeight = (textBounds.height());
				horLabelTextWidth = (textBounds.width());
			}
			border += labelTextHeight;

			float graphheight = height - (2 * border);
			graphwidth = width;

			if (horlabels.isEmpty()) {
				generateHorlabels();
			}
			if (verlabels.isEmpty()) {
				generateVerlabels(graphheight);
			}

			// vertical lines
			if (graphViewStyle.getGridStyle() != GridStyle.HORIZONTAL) {
				paint.setTextAlign(Align.LEFT);
				int vers = verlabels.size() - 1;
				for (int i = 0; i < verlabels.size(); i++) {
					paint.setColor(graphViewStyle.getGridColor());
					float y = ((graphheight / vers) * i) + border;
					canvas.drawLine(horstart, y, width, y, paint);
				}
			}

			drawHorizontalLabels(canvas, border, horstart, height, horlabels, graphwidth);

			paint.setColor(graphViewStyle.getHorizontalLabelsColor());
			paint.setTextAlign(Align.CENTER);
			canvas.drawText(title, (graphwidth / 2) + horstart, border - 4, paint);

			if (maxY == minY) {
				// if min/max is the same, fake it so that we can render a line
				if (maxY == 0) {
					// if both are zero, change the values to prevent division by zero
					maxY = 1.0d;
					minY = 0.0d;
				} else {
					maxY = maxY * 1.05d;
					minY = minY * 0.95d;
				}
			}

			double diffY = maxY - minY;
			paint.setStrokeCap(Paint.Cap.ROUND);
			for (int i = 0; i < graphSeries.size(); i++) {
				drawSeries(canvas, i, graphwidth, graphheight, border, minX, minY, diffX, diffY, horstart,
						graphSeries.get(i).style);
			}

			final double screenPosition = (double) xCursor * graphwidth;
			generateArrowPath((int) screenPosition);
			canvas.drawPath(valueIndicatorPath, valueIndicatorPaint);
			canvas.drawLine((float) screenPosition - 1, valueIndiatorSize, (float) screenPosition + 1, graphheight
					+ border, valueIndicatorPaint);

			if (showLegend)
				drawLegend(canvas, height, width);
		}

		public void generateArrowPath(int screenPosition) {
			if (lastScreenPosition != screenPosition) {
				lastScreenPosition = screenPosition;
				valueIndicatorPath.reset();

				valueIndicatorPath.moveTo(screenPosition - valueIndiatorSize, 0);
				valueIndicatorPath.lineTo(screenPosition + valueIndiatorSize, 0);
				valueIndicatorPath.lineTo(screenPosition, valueIndiatorSize);
				valueIndicatorPath.close();

			}
		}

		Toast m_currentToast;
		private GraphDataListener mListener;

		private void showToast(String text) {
			if (m_currentToast == null) {
				m_currentToast = Toast.makeText(getContext(), text, Toast.LENGTH_LONG);
			}

			m_currentToast.setText(text);
			m_currentToast.setDuration(Toast.LENGTH_LONG);
			m_currentToast.show();

		}

		private void onMoveGesture(float eventX) {
			final float f = eventX - lastTouchEventX;
			// view port update
			if (viewportSize != 0) {
				viewportStart -= f * viewportSize / graphwidth;

				// minimal and maximal view limit
				long minX = getMinX(true);
				long maxX = getMaxX(true);
				if (viewportStart < minX) {
					viewportStart = minX;
				} else if (viewportStart + viewportSize > maxX) {
					viewportStart = maxX - viewportSize;
				}
				// labels have to be regenerated

				horlabels.clear();
				initRedrawHorizontalLabels();

				if (verlabels.isEmpty() == false) {
					invalidateView(viewVerLabels);
				}

				if (manualYAxis == false) {
					verlabels.clear();
					initRedrawVerticalLabels();
				}
			}
			invalidateView(this);
		}

		/**
		 * @param event
		 */
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (!isScrollable() || isDisableTouch()) {
				return super.onTouchEvent(event);
			}

			boolean handled = false;
			// first scale
			if (scalable && scaleDetector != null) {
				scaleDetector.onTouchEvent(event);
				handled = scaleDetector.isInProgress();
			}
			if (!handled) {
				// Log.d("GraphView", "on touch event scale not handled+"+lastTouchEventX);
				// if not scaled, scroll
				if ((event.getAction() & MotionEvent.ACTION_DOWN) == MotionEvent.ACTION_DOWN
						&& (event.getAction() & MotionEvent.ACTION_MOVE) == 0) {
					scrollingStarted = true;
					handled = true;

				}
				if ((event.getAction() & MotionEvent.ACTION_UP) == MotionEvent.ACTION_UP) {
					scrollingStarted = false;
					lastTouchEventX = 0;
					handled = true;
				}

				if ((event.getAction() & MotionEvent.ACTION_CANCEL) == MotionEvent.ACTION_CANCEL) {
					scrollingStarted = false;
					lastTouchEventX = 0;
					handled = true;
				}
				if ((event.getAction() & MotionEvent.ACTION_MOVE) == MotionEvent.ACTION_MOVE) {
					if (scrollingStarted) {
						if (lastTouchEventX != 0) {
							onMoveGesture(event.getX());
						}
						lastTouchEventX = event.getX();
						handled = true;
					}
				}
				if (handled)
					invalidateView(this);
			} else {
				// currently scaling
				scrollingStarted = false;
				lastTouchEventX = 0;
			}
			return handled;
		}

		public void setGraphListener(GraphDataListener listener) {
			this.mListener = listener;
		}
	}

	/**
	 * one data set for a graph series
	 */
	static public class GraphViewData implements GraphViewDataInterface {
		public final long valueX;
		public final double valueY;

		public GraphViewData(long valueX, double valueY) {
			super();
			this.valueX = valueX;
			this.valueY = valueY;
		}

		@Override
		public long getX() {
			return valueX;
		}

		@Override
		public double getY() {
			return valueY;
		}
	}

	public enum LegendAlign {
		TOP, MIDDLE, BOTTOM
	}

	private class VerLabelsView extends View {
		/**
		 * @param context
		 */
		public VerLabelsView(Context context) {
			super(context);
			setLayoutParams(new LayoutParams(getGraphViewStyle().getVerticalLabelsWidth() == 0 ? 100
					: getGraphViewStyle().getVerticalLabelsWidth(), LayoutParams.MATCH_PARENT));
		}

		/**
		 * @param canvas
		 */
		@Override
		protected void onDraw(Canvas canvas) {
			// normal
			paint.setStrokeWidth(0);
			paint.setTypeface(getGraphViewStyle().getTypeface());

			// measure bottom text
			if (labelTextHeight == null || verLabelTextWidth == null) {
				float textSize = getGraphViewStyle().getTextSize();
				paint.setTextSize(textSize);
				double testY = ((getMaxY() - getMinY()) * 0.783) + getMinY();
				String testLabel = formatLabel(testY, 0, false);
				paint.getTextBounds(testLabel, 0, testLabel.length(), textBounds);
				labelTextHeight = (textBounds.height());
				verLabelTextWidth = (textBounds.width());
			}
			if (getGraphViewStyle().getVerticalLabelsWidth() == 0
					&& getLayoutParams().width != verLabelTextWidth + GraphViewConfig.BORDER) {
				mLayoutParams.width = (int) (verLabelTextWidth + GraphViewConfig.BORDER);
				mLayoutParams.height = LayoutParams.MATCH_PARENT;
				setLayoutParams(mLayoutParams);
			} else if (getGraphViewStyle().getVerticalLabelsWidth() != 0
					&& getGraphViewStyle().getVerticalLabelsWidth() != getLayoutParams().width) {
				mLayoutParams.width = getGraphViewStyle().getVerticalLabelsWidth();
				mLayoutParams.height = LayoutParams.MATCH_PARENT;
				setLayoutParams(mLayoutParams);
			}

			if (canShowVerticalLabels) {

				float border = GraphViewConfig.BORDER;
				border += labelTextHeight;
				float height = getHeight();
				float graphheight = height - (2 * border);

				if (verlabels.isEmpty()) {
					generateVerlabels(graphheight);
				}

				// vertical labels
				paint.setTextAlign(getGraphViewStyle().getVerticalLabelsAlign());
				int labelsWidth = getWidth();
				int labelsOffset = 0;
				if (getGraphViewStyle().getVerticalLabelsAlign() == Align.RIGHT) {
					labelsOffset = labelsWidth;
				} else if (getGraphViewStyle().getVerticalLabelsAlign() == Align.CENTER) {
					labelsOffset = labelsWidth / 2;
				}
				int vers = verlabels.size() - 1;
				for (int i = 0; i < verlabels.size(); i++) {
					float y = ((graphheight / vers) * i) + border;
					paint.setColor(graphViewStyle.getVerticalLabelsColor());
					canvas.drawText(verlabels.get(i), labelsOffset, y, paint);
				}

				// reset
				paint.setTextAlign(Align.LEFT);
			}
		}
	}

	protected final Paint paint;
	private Map<Long, String> horlabels = new LinkedHashMap<Long, String>();
	private Map<Integer, String> verlabels = new LinkedHashMap<Integer, String>();
	private String title;
	private boolean scrollable;
	private boolean disableTouch;
	private long viewportStart;
	private long viewportSize;
	private final View viewVerLabels;
	private ScaleGestureDetector scaleDetector;
	private boolean scalable;
	private final NumberFormat[] numberformatter = new NumberFormat[2];
	protected final List<GraphViewSeries> graphSeries;
	private boolean showLegend = false;
	private LegendAlign legendAlign = LegendAlign.MIDDLE;
	private boolean manualYAxis;
	private boolean manualMaxY;
	private boolean manualMinY;
	private double manualMaxYValue;
	private double manualMinYValue;
	protected GraphViewStyle graphViewStyle;
	private final GraphViewContentView graphViewContentView;
	private CustomLabelFormatter customLabelFormatter;
	private Integer labelTextHeight;
	private Integer horLabelTextWidth;
	private Integer verLabelTextWidth;
	private final Rect textBounds = new Rect();

	private boolean showHorizontalLabels = true;
	private boolean showVerticalLabels = true;
	private boolean canShowHorizontalLabels;
	private WindowManager windowManager;
	private Point screenSize;
	private DisplayMode displayMode;

	private LayoutParams mLayoutParams;

	private Calendar mCalendar;

	public GraphView(Context context, AttributeSet attrs) {
		this(context, attrs.getAttributeValue(null, "title"));

		int width = attrs.getAttributeIntValue("android", "layout_width", LayoutParams.MATCH_PARENT);
		int height = attrs.getAttributeIntValue("android", "layout_height", LayoutParams.MATCH_PARENT);
		setLayoutParams(new LayoutParams(width, height));
	}

	public abstract double getRealYTimeValue(int index, double percentPosition, Point screenSize);

	public GraphDataListener listener;

	public void setGraphDataListener(GraphDataListener listener) {
		this.listener = listener;
	}

	/**
	 * @param context
	 * @param title
	 *            [optional]
	 */
	public GraphView(Context context, String title) {
		super(context);
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		if (title == null)
			this.title = "";
		else
			this.title = title;

		graphViewStyle = new GraphViewStyle();
		graphViewStyle.useTextColorFromTheme(context);

		paint = new Paint();
		graphSeries = new ArrayList<GraphViewSeries>();

		viewVerLabels = new VerLabelsView(context);
		addView(viewVerLabels);
		graphViewContentView = new GraphViewContentView(context);
		graphViewContentView.setGraphListener(listener);
		addView(graphViewContentView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1));
	}

	protected GraphViewDataInterface[] _values(int idxSeries) {
		GraphViewDataInterface[] values = graphSeries.get(idxSeries).values;
		synchronized (values) {
			if (viewportStart == 0 && viewportSize == 0) {
				// all data
				return values;
			} else {
				// viewport
				List<GraphViewDataInterface> listData = new ArrayList<GraphViewDataInterface>();
				for (int i = 0; i < values.length; i++) {
					if (values[i].getX() >= viewportStart) {
						if (values[i].getX() > viewportStart + viewportSize) {
							listData.add(values[i]); // one more for nice scrolling
							break;
						} else {
							listData.add(values[i]);
						}
					} else {
						if (listData.isEmpty()) {
							listData.add(values[i]);
						}
						listData.set(0, values[i]); // one before, for nice scrolling
					}
				}
				return listData.toArray(new GraphViewDataInterface[listData.size()]);
			}
		}
	}

	/**
	 * add a series of data to the graph
	 * 
	 * @param series
	 */
	public void addSeries(GraphViewSeries series) {
		series.addGraphView(this);
		graphSeries.add(series);
		redrawAll();
	}

	public int getGraphSeriesCount() {
		return graphSeries.size();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		if (screenSize == null) {

			canShowHorizontalLabels = true;
			canShowVerticalLabels = true;
			mLayoutParams = new LayoutParams(0, 0);

			// Date
			mCalendar = Calendar.getInstance();
			dateFormatter = new SimpleDateFormat("", Locale.getDefault());

			// Screen Size
			windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

		}
		final Display display = windowManager.getDefaultDisplay();
		screenSize = new Point();
		display.getSize(screenSize);

	}

	protected void drawHorizontalLabels(Canvas canvas, float border, float horstart, float height,
			Map<Long, String> horlabels2, float graphwidth) {
		// horizontal labels + lines
		float x = 0;
		final Long[] timeSet = horlabels2.keySet().toArray(new Long[horlabels2.size()]);
		long timeToSet = getMinX(false), lastValue, diff;
		double minX = getMinX(false);
		double maxX = getMaxX(false);
		double diffX = maxX - minX;

		for (int i = 0; i < horlabels2.size(); i++) {

			lastValue = Long.valueOf(timeToSet);
			timeToSet = timeSet[i];

			diff = timeToSet - lastValue;

			double valX = timeToSet - minX;
			double ratX = valX / diffX;

			x = (float) (graphwidth * ratX);

			// XSize = Math.max(XSize, (int) (diff / timePerPixel));
			// x = x + XSize;

			paint.setColor(graphViewStyle.getGridColor());
			paint.setTypeface(getGraphViewStyle().getTypeface());
			if (graphViewStyle.getGridStyle() != GridStyle.VERTICAL) {
				canvas.drawLine(x, height - border, x, border, paint);
			}
			if (showHorizontalLabels && canShowHorizontalLabels) {
				paint.setTextAlign(Align.CENTER);

				paint.setColor(graphViewStyle.getHorizontalLabelsColor());
				canvas.drawText(horlabels2.get(timeToSet), x, height - 4, paint);
			}
		}
	}

	protected void generateHorLabelsInternal(long minX, long maxX) {

		final long diff = maxX - minX;

		formatLabel(minX, diff, true);

		mCalendar.setTimeInMillis(minX);

		Log.e("DEBUG", "level=" + displayMode);

		final int level = displayMode.mLevel;

		if (level >= DisplayUtils.LEVEL_MINUTE) {
			mCalendar.set(Calendar.SECOND, 0);
		}
		if (level >= DisplayUtils.LEVEL_HOUR) {
			mCalendar.set(Calendar.MINUTE, 0);
		}
		if (level >= DisplayUtils.LEVEL_DAY) {
			mCalendar.set(Calendar.HOUR_OF_DAY, 0);
		}
		if (level >= DisplayUtils.LEVEL_MONTH) {
			mCalendar.set(Calendar.DAY_OF_MONTH, 0);
		}

		mCalendar.set(Calendar.MILLISECOND, 0);

		int potentialValue = mCalendar.getActualMinimum(displayMode.mCalendarField);
		final int maxValue = mCalendar.getActualMaximum(displayMode.mCalendarField);
		final int realValue = mCalendar.get(displayMode.mCalendarField);

		boolean potentialFound = false;
		for (int i = potentialValue; i <= maxValue; i = i + displayMode.mInterval) {
			if (i > realValue) {
				potentialFound = true;
				potentialValue = i;
				break;
			}
		}

		// Means that we are beyond maximum value of the current field
		if (!potentialFound) {

			mCalendar.add(displayMode.mCalendarField, displayMode.mInterval);
		}

		mCalendar.set(displayMode.mCalendarField, potentialValue);
		// Log.e("DEBUG", "after //// " + dateFormatter.format(mCalendar.getTimeInMillis()));

		long currentTime = -1;

		horlabels.clear();

		while ((currentTime = mCalendar.getTimeInMillis()) < maxX) {
			if (currentTime != -1) {
				horlabels.put(currentTime, formatLabel(currentTime, diff, true));
			}
			mCalendar.add(displayMode.mCalendarField, displayMode.mInterval);
		}

	}

	protected void drawLegend(Canvas canvas, float height, float width) {
		float textSize = paint.getTextSize();
		int spacing = getGraphViewStyle().getLegendSpacing();
		int border = getGraphViewStyle().getLegendBorder();
		int legendWidth = getGraphViewStyle().getLegendWidth();

		int shapeSize = (int) (textSize * 0.8d);
		Log.d("GraphView", "draw legend size: " + paint.getTextSize());

		// rect
		paint.setARGB(180, 100, 100, 100);
		float legendHeight = (shapeSize + spacing) * graphSeries.size() + 2 * border - spacing;
		float lLeft = width - legendWidth - border * 2;
		float lTop;
		switch (legendAlign) {
		case TOP:
			lTop = 0;
			break;
		case MIDDLE:
			lTop = height / 2 - legendHeight / 2;
			break;
		default:
			lTop = height - GraphViewConfig.BORDER - legendHeight - getGraphViewStyle().getLegendMarginBottom();
		}
		float lRight = lLeft + legendWidth;
		float lBottom = lTop + legendHeight;
		canvas.drawRoundRect(new RectF(lLeft, lTop, lRight, lBottom), 8, 8, paint);

		for (int i = 0; i < graphSeries.size(); i++) {
			paint.setColor(graphSeries.get(i).style.color);
			canvas.drawRect(new RectF(lLeft + border, lTop + border + (i * (shapeSize + spacing)), lLeft + border
					+ shapeSize, lTop + border + (i * (shapeSize + spacing)) + shapeSize), paint);
			if (graphSeries.get(i).description != null) {
				paint.setColor(Color.WHITE);
				paint.setTextAlign(Align.LEFT);
				canvas.drawText(graphSeries.get(i).description, lLeft + border + shapeSize + spacing, lTop + border
						+ shapeSize + (i * (shapeSize + spacing)), paint);
			}
		}
	}

	abstract protected void drawSeries(Canvas canvas, int index, float graphwidth, float graphheight, float border,
			long minX, double minY, long diffX, double diffY, float horstart, GraphViewSeriesStyle style);

	/**
	 * formats the label use #setCustomLabelFormatter or static labels if you want custom labels
	 * 
	 * @param value
	 *            x and y values
	 * @param isValueX
	 *            if false, value y wants to be formatted
	 * @param diff
	 * @deprecated use {@link #setCustomLabelFormatter(CustomLabelFormatter)}
	 * @return value to display
	 */
	@Deprecated
	protected String formatLabel(double value, long diff, boolean isValueX) {
		if (customLabelFormatter != null) {
			if (isValueX) {

				final DisplayMode newDisplayMode = customLabelFormatter.formatLabel(diff, isValueX);
				String label = null;
				if (newDisplayMode != displayMode) {
					displayMode = newDisplayMode;
					dateFormatter.applyPattern(displayMode.mFormatPattern);
				}

				if (newDisplayMode != null) {
					label = dateFormatter.format(value);
				}

				if (label != null) {
					return label;
				}
			}
		}

		int i = isValueX ? 1 : 0;
		if (numberformatter[i] == null) {
			numberformatter[i] = NumberFormat.getNumberInstance();

			if (mInterval < 0.1) {
				numberformatter[i].setMaximumFractionDigits(2);
			} else if (mInterval < 1) {
				numberformatter[i].setMaximumFractionDigits(1);
			} else {
				numberformatter[i].setMaximumFractionDigits(0);
			}
		}
		return numberformatter[i].format(value);
	}

	public long getDiff(long min, long max) {
		return max - min;
	}

	private void generateHorlabels() {

		final long min = getMinX(false);
		final long max = getMaxX(false);

		generateHorLabelsInternal(min, max);
	}

	synchronized private void generateVerlabels(float graphheight) {
		int numLabels = getGraphViewStyle().getNumVerticalLabels() - 1;
		if (numLabels < 0) {
			if (graphheight <= 0)
				graphheight = 1f;
			numLabels = (int) (graphheight / (labelTextHeight * 3));
			if (numLabels == 0) {
				Log.w("GraphView",
						"Height of Graph is smaller than the label text height, so no vertical labels were shown!");
			}
		}

		verlabels.clear();

		double min = getMinY();
		double max = getMaxY();
		if (max == min) {
			// if min/max is the same, fake it so that we can render a line
			if (max == 0) {
				// if both are zero, change the values to prevent division by zero
				max = 1.0d;
				min = 0.0d;
			} else {
				max = max * 1.05d;
				min = min * 0.95d;
			}
		}

		for (int i = 0; i <= numLabels; i++) {
			verlabels.put((numLabels - i), formatLabel(min + (mInterval * i), 0, false));
		}
	}

	public double getYValue(double xValue) {
		GraphViewDataInterface[] values = graphSeries.get(0).values;
		for (int i = 0; i < values.length; i++) {
			final GraphViewDataInterface m2 = values[i];
			final double m2_x = m2.getX();
			if (m2_x > xValue) {
				GraphViewDataInterface m1 = null;
				double top = 0;
				for (int j = 1; j < values.length - i; j++) {
					m1 = values[i + j];
					top = (m2.getY() - m1.getY());
					if (top != 0) {
						break;
					}
				}

				final double a = (double) ((m2.getY() - m1.getY()) / (m2.getX() - m1.getX()));
				final double b = m2.getY() - (a * m2.getX());
				return ((a * xValue) + b);
			}
		}

		return 0;
	}

	/**
	 * @return the custom label formatter, if there is one. otherwise null
	 */
	public CustomLabelFormatter getCustomLabelFormatter() {
		return customLabelFormatter;
	}

	/**
	 * @return the graphview style. it will never be null.
	 */
	public GraphViewStyle getGraphViewStyle() {
		return graphViewStyle;
	}

	/**
	 * get the position of the legend
	 * 
	 * @return
	 */
	public LegendAlign getLegendAlign() {
		return legendAlign;
	}

	/**
	 * @return legend width
	 * @deprecated use {@link GraphViewStyle#getLegendWidth()}
	 */
	@Deprecated
	public float getLegendWidth() {
		return getGraphViewStyle().getLegendWidth();
	}

	/**
	 * returns the maximal X value of the current viewport (if viewport is set) otherwise maximal X value of all data.
	 * 
	 * @param ignoreViewport
	 * 
	 *            warning: only override this, if you really know want you're doing!
	 */
	protected long getMaxX(boolean ignoreViewport) {
		// if viewport is set, use this
		if (!ignoreViewport && viewportSize != 0) {
			return viewportStart + viewportSize;
		} else {
			// otherwise use the max x value
			// values must be sorted by x, so the last value has the largest X value
			long highest = 0;
			if (graphSeries.size() > 0) {
				GraphViewDataInterface[] values = graphSeries.get(0).values;
				if (values.length == 0) {
					highest = 0;
				} else {
					highest = values[values.length - 1].getX();
				}
				for (int i = 1; i < graphSeries.size(); i++) {
					values = graphSeries.get(i).values;
					if (values.length > 0) {
						highest = Math.max(highest, values[values.length - 1].getX());
					}
				}
			}
			return highest;
		}
	}

	/**
	 * returns the maximal Y value of all data.
	 * 
	 * warning: only override this, if you really know want you're doing!
	 */
	protected double getMaxY() {
		double largest;
		if (manualYAxis || manualMaxY) {
			largest = manualMaxYValue;
		} else {
			largest = Integer.MIN_VALUE;
			for (int i = 0; i < graphSeries.size(); i++) {
				GraphViewDataInterface[] values = _values(i);
				for (int ii = 0; ii < values.length; ii++)
					if (values[ii].getY() > largest)
						largest = values[ii].getY();
			}
		}
		return largest;
	}

	/**
	 * returns the minimal X value of the current viewport (if viewport is set) otherwise minimal X value of all data.
	 * 
	 * @param ignoreViewport
	 * 
	 *            warning: only override this, if you really know want you're doing!
	 */
	protected long getMinX(boolean ignoreViewport) {
		// if viewport is set, use this
		if (!ignoreViewport && viewportSize != 0) {
			return viewportStart;
		} else {
			// otherwise use the min x value
			// values must be sorted by x, so the first value has the smallest X value
			long lowest = 0;
			if (graphSeries.size() > 0) {
				GraphViewDataInterface[] values = graphSeries.get(0).values;
				if (values.length == 0) {
					lowest = 0;
				} else {
					lowest = values[0].getX();
				}
				for (int i = 1; i < graphSeries.size(); i++) {
					values = graphSeries.get(i).values;
					if (values.length > 0) {
						lowest = Math.min(lowest, values[0].getX());
					}
				}
			}
			return lowest;
		}
	}

	/**
	 * returns the minimal Y value of all data.
	 * 
	 * warning: only override this, if you really know want you're doing!
	 */
	protected double getMinY() {
		double smallest;
		if (manualYAxis || manualMinY) {
			smallest = manualMinYValue;
		} else {
			smallest = Integer.MAX_VALUE;
			for (int i = 0; i < graphSeries.size(); i++) {
				GraphViewDataInterface[] values = _values(i);
				for (int ii = 0; ii < values.length; ii++)
					if (values[ii].getY() < smallest)
						smallest = values[ii].getY();
			}
		}
		return smallest;
	}

	/**
	 * returns the size of the Viewport
	 * 
	 */
	public double getViewportSize() {
		return viewportSize;
	}

	public boolean isDisableTouch() {
		return disableTouch;
	}

	public boolean isScrollable() {
		return scrollable;
	}

	public boolean isShowLegend() {
		return showLegend;
	}

	private Handler handler = new Handler();

	protected double xCursor = 0;

	public void setXCursor(double xCursor) {
		this.xCursor = xCursor;
	}

	private Runnable invalidateVerticalRunnable = new Runnable() {
		@Override
		public void run() {
			canShowVerticalLabels = true;
			invalidateView(viewVerLabels);
		}
	};

	private Runnable invalidateHorizontalRunnable = new Runnable() {

		@Override
		public void run() {
			if (listener != null) {
				for (int i = 0; i < graphSeries.size(); i++) {
					final GraphViewSeries serie = graphSeries.get(i);
					if (TextUtils.equals(serie.description, GRAPH_NO_DATA_TAG) == false) {
						final double y = getRealYTimeValue(i, xCursor, screenSize);
						listener.onTouchValueRequest(serie.description, xCursor, y);
					}

				}
			}
			canShowHorizontalLabels = true;
			invalidateView(graphViewContentView);
		}
	};

	private boolean canShowVerticalLabels;

	private double mInterval;

	public void initRedrawVerticalLabels() {
		canShowVerticalLabels = true;
		handler.removeCallbacks(invalidateVerticalRunnable);
		handler.post(invalidateVerticalRunnable);
	}

	public void initRedrawHorizontalLabels() {
		if (canShowHorizontalLabels) {
			if (listener != null) {
				listener.onTouchValueClear();
			}
			canShowHorizontalLabels = false;
		}
		handler.removeCallbacks(invalidateHorizontalRunnable);
		handler.postDelayed(invalidateHorizontalRunnable, HIDE_DELAY);
	}

	public void redrawHorizontalLabels() {
		initRedrawHorizontalLabels();
		invalidateView(graphViewContentView);
	}

	public void redrawVerticalLabels() {
		initRedrawVerticalLabels();
		invalidateView(viewVerLabels);
	}

	/**
	 * forces graphview to invalide all views and caches. Normally there is no need to call this manually.
	 */
	public void redrawAll() {

		verlabels.clear();
		horlabels.clear();
		numberformatter[0] = null;
		numberformatter[1] = null;
		labelTextHeight = null;
		horLabelTextWidth = null;
		verLabelTextWidth = null;

		invalidateView(this);
		redrawVerticalLabels();
		redrawHorizontalLabels();
	}

	/**
	 * removes all series
	 */
	public void removeAllSeries() {
		for (GraphViewSeries s : graphSeries) {
			s.removeGraphView(this);
		}
		while (!graphSeries.isEmpty()) {
			graphSeries.remove(0);
		}
		redrawAll();
	}

	/**
	 * removes a series
	 * 
	 * @param series
	 *            series to remove
	 */
	public void removeSeries(GraphViewSeries series) {
		series.removeGraphView(this);
		graphSeries.remove(series);
		redrawAll();
	}

	/**
	 * removes series
	 * 
	 * @param index
	 */
	public void removeSeries(int index) {
		if (index < 0 || index >= graphSeries.size()) {
			throw new IndexOutOfBoundsException("No series at index " + index);
		}

		removeSeries(graphSeries.get(index));
	}

	/**
	 * scrolls to the last x-value
	 * 
	 * @throws IllegalStateException
	 *             if scrollable == false
	 */
	public void scrollToEnd() {
		if (!scrollable)
			throw new IllegalStateException("This GraphView is not scrollable.");
		long max = getMaxX(true);
		viewportStart = max - viewportSize;

		// don't clear labels width/height cache
		// so that the display is not flickering

		verlabels.clear();
		horlabels.clear();

		invalidateView(this);
		redrawVerticalLabels();
		redrawHorizontalLabels();
	}

	/**
	 * set a custom label formatter
	 * 
	 * @param customLabelFormatter
	 */
	public void setCustomLabelFormatter(CustomLabelFormatter customLabelFormatter) {
		this.customLabelFormatter = customLabelFormatter;
	}

	/**
	 * The user can disable any touch gestures, this is useful if you are using a real time graph, but don't want the
	 * user to interact
	 * 
	 * @param disableTouch
	 */
	public void setDisableTouch(boolean disableTouch) {
		this.disableTouch = disableTouch;
	}

	/**
	 * set custom graphview style
	 * 
	 * @param style
	 */
	public void setGraphViewStyle(GraphViewStyle style) {
		graphViewStyle = style;
		labelTextHeight = null;
	}

	/**
	 * legend position
	 * 
	 * @param legendAlign
	 */
	public void setLegendAlign(LegendAlign legendAlign) {
		this.legendAlign = legendAlign;
	}

	/**
	 * legend width
	 * 
	 * @param legendWidth
	 * @deprecated use {@link GraphViewStyle#setLegendWidth(int)}
	 */
	@Deprecated
	public void setLegendWidth(float legendWidth) {
		getGraphViewStyle().setLegendWidth((int) legendWidth);
	}

	/**
	 * you have to set the bounds {@link #setManualYAxisBounds(double, double)}. That automatically enables
	 * manualYAxis-flag. if you want to disable the menual y axis, call this method with false.
	 * 
	 * @param manualYAxis
	 */
	public void setManualYAxis(boolean manualYAxis) {
		this.manualYAxis = manualYAxis;
	}

	/**
	 * if you want to disable the menual y axis maximum bound, call this method with false.
	 */
	public void setManualMaxY(boolean manualMaxY) {
		this.manualMaxY = manualMaxY;
	}

	/**
	 * if you want to disable the menual y axis minimum bound, call this method with false.
	 */
	public void setManualMinY(boolean manualMinY) {
		this.manualMinY = manualMinY;
	}

	/**
	 * set manual Y axis limit
	 * 
	 * @param max
	 * @param min
	 */
	public void setManualYAxisBounds(double min, double max, double interval) {
		manualMaxYValue = max;
		manualMinYValue = min;
		this.mInterval = interval;
		final int numVerticalLabels = (int) ((max - min + interval) / interval);
		getGraphViewStyle().setNumVerticalLabels(numVerticalLabels);
		manualYAxis = true;
	}

	/*
	 * set manual Y axis max limit
	 * 
	 * @param max
	 */
	public void setManualYMaxBound(double max) {
		manualMaxYValue = max;
		manualMaxY = true;
	}

	/*
	 * set manual Y axis min limit
	 * 
	 * @param min
	 */
	public void setManualYMinBound(double min) {
		manualMinYValue = min;
		manualMinY = true;
	}

	/**
	 * this forces scrollable = true
	 * 
	 * @param scalable
	 */
	synchronized public void setScalable(boolean scalable) {
		this.scalable = scalable;
		if (scalable == true && scaleDetector == null) {
			scrollable = true; // automatically forces this
			scaleDetector = new ScaleGestureDetector(getContext(),
					new ScaleGestureDetector.SimpleOnScaleGestureListener() {
						@Override
						public boolean onScale(ScaleGestureDetector detector) {
							long center = viewportStart + viewportSize / 2;

							viewportSize /= detector.getScaleFactor();
							viewportStart = center - viewportSize / 2;

							// viewportStart must not be < minX
							long minX = getMinX(true);
							if (viewportStart < minX) {
								viewportStart = minX;
							}

							// viewportStart + viewportSize must not be > maxX
							long maxX = getMaxX(true);
							if (viewportSize == 0) {
								viewportSize = maxX;
							}
							long overlap = viewportStart + viewportSize - maxX;
							if (overlap > 0) {
								// scroll left
								if (viewportStart - overlap > minX) {
									viewportStart -= overlap;
								} else {
									// maximal scale
									viewportStart = minX;
									viewportSize = maxX - viewportStart;
								}
							}
							redrawAll();
							return true;
						}
					});
		}
	}

	/**
	 * the user can scroll (horizontal) the graph. This is only useful if you use a viewport
	 * {@link #setViewPort(double, double)} which doesn't displays all data.
	 * 
	 * @param scrollable
	 */
	public void setScrollable(boolean scrollable) {
		this.scrollable = scrollable;
	}

	public void setShowLegend(boolean showLegend) {
		this.showLegend = showLegend;
	}

	/**
	 * sets the title of graphview
	 * 
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * set's the viewport for the graph.
	 * 
	 * @see #setManualYAxisBounds(double, double) to limit the y-viewport
	 * @param start
	 *            x-value
	 * @param size
	 */
	public void setViewPort(long start, long size) {
		if (size < 0) {
			throw new IllegalArgumentException("Viewport size must be greater than 0!");
		}
		viewportStart = start;
		viewportSize = size;
	}

	/**
	 * Sets whether horizontal labels are drawn or not.
	 * 
	 * @param showHorizontalLabels
	 */
	public void setShowHorizontalLabels(boolean showHorizontalLabels) {
		this.showHorizontalLabels = showHorizontalLabels;
		redrawAll();
	}

	/**
	 * Gets are horizontal labels drawn.
	 * 
	 * @return {@code True} if horizontal labels are drawn
	 */
	public boolean getShowHorizontalLabels() {
		return showHorizontalLabels;
	}

	/**
	 * Sets whether vertical labels are drawn or not.
	 * 
	 * @param showVerticalLabels
	 */
	public void setShowVerticalLabels(boolean showVerticalLabels) {
		this.showVerticalLabels = showVerticalLabels;
		if (this.showVerticalLabels) {
			addView(viewVerLabels, 0);
		} else {
			removeView(viewVerLabels);
		}
	}

	/**
	 * Gets are vertical labels are drawn.
	 * 
	 * @return {@code True} if vertical labels are drawn
	 */
	public boolean getShowVerticalLabels() {
		return showVerticalLabels;
	}

}
