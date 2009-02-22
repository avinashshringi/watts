package au.id.weston.scott.Watts;

import java.util.Date;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;
import android.widget.TextView;
import android.widget.Toast;

class WattsGrapher extends View implements OnGestureListener {
	private static final String TAG = "WattsGrapher";
	
	private Context mContext;
	
	private int width;;
	private int height;
	
	private static final int axisLabelArea = 50;
	
	private GestureDetector mg;
	
	private Paint mPaintChart;
	private Paint mPaintAxis;
	private Paint mPaintAxisGrid;
	private Paint mPaintAxisText;
	private Paint mPaintText;
	private Paint mPaintRecent;
	private Paint mPaintTextExtra;
	private Paint mPaintBackground;
	
	private static Canvas mCanvas;
	private static Bitmap mBitmap;
	private static BatteryData batteryData;
	
	private int screenWidth;
	
	// default to 1 days worth of data to a 5 minute resolution;
	private long timeWindow = 24*60*60*1000;
	private int resolution = 300;
	
	private float vx;
	
	private boolean pausePainter = true;
	
	private Handler mHandler;
	
	// Handler messages
	
	private static final int REDRAW = 900;
	private static final int DIE = 901;
	
	private TextView axisText;
	
	protected final Thread painter = new Thread(new Runnable() {
		
		public void run() {
			Log.d(TAG, "painter.run(): " + painter.getId());
			Looper.prepare();
			
			mHandler = new Handler() {
				public void handleMessage(Message msg) {
					// process incoming messages here
					Log.d(TAG, "handleMessage: " + msg);
					switch (msg.what) {
					case REDRAW:
						Log.d(TAG, "Redraw():");
						if (!pausePainter) {
							Toast.makeText(mContext, "updating", Toast.LENGTH_SHORT).show();
							Log.d(TAG, painter.getId() + ": repaint requested");
							long now = System.currentTimeMillis();
							batteryData.buildDataMap(now - timeWindow, 0, resolution);
							
							synchronized (mBitmap) {
								clearCanvas();
								drawBackground();
								drawGrid();
								drawAxis();
								drawGraph();
							}

							postInvalidate();
						} else {
							Log.d(TAG, "painter: requested repaint, but paused.");
						}
						mHandler.removeMessages(REDRAW);
						break;
					case DIE:
						Looper.myLooper().quit();
						return;
					}
				}
			};

			Looper.loop();
			Log.d(TAG, "after loop");
		}
	});

	
	public WattsGrapher(Context context, TextView tv) {
		super(context);
		
		mContext = context;
		axisText = tv;
		
		setFocusable(true);
		setFocusableInTouchMode(true);
		setLongClickable(true);
		setId(1);
		
		mg = new GestureDetector(this);
		mg.setIsLongpressEnabled(true);
		
		// fill the paintbox
		
		mPaintChart = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintChart.setStyle(Paint.Style.STROKE);
		mPaintChart.setStrokeWidth(4);
		mPaintChart.setColor(Color.GREEN);
		
		mPaintRecent = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintRecent.setStyle(Paint.Style.STROKE);
		mPaintRecent.setStrokeWidth(1);
		mPaintRecent.setColor(0x80ffffff);
		
		mPaintAxis = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintAxis.setStyle(Paint.Style.STROKE);
		mPaintAxis.setStrokeWidth(2);
		mPaintAxis.setColor(Color.WHITE);
	
		mPaintAxisGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintAxisGrid.setStyle(Paint.Style.STROKE);
		mPaintAxisGrid.setStrokeWidth(1);
		mPaintAxisGrid.setColor(Color.argb(50, 255, 255, 255));
		
		mPaintText = new Paint(Paint.LINEAR_TEXT_FLAG);
		mPaintText.setStyle(Paint.Style.FILL);
		mPaintText.setAntiAlias(true);
		mPaintText.setTextSize(22);
		mPaintText.setColor(Color.WHITE);
		
		mPaintTextExtra = new Paint(Paint.LINEAR_TEXT_FLAG);
		mPaintTextExtra.setStyle(Paint.Style.FILL);
		mPaintTextExtra.setAntiAlias(true);
		mPaintTextExtra.setTextSize(12);
		mPaintTextExtra.setColor(Color.WHITE);
		
		mPaintAxisText = new Paint(Paint.LINEAR_TEXT_FLAG);
		mPaintAxisText.setStyle(Paint.Style.FILL);
		mPaintAxisText.setAntiAlias(true);
		mPaintAxisText.setTextSize(11);
		mPaintAxisText.setColor(0x80ffffff);
		
		batteryData = new BatteryData(context);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		synchronized (mBitmap) {
			if (mBitmap != null) {
				Paint paint = new Paint();
				canvas.drawBitmap(mBitmap, 0, 0, paint);
			} else {
				canvas.drawColor(Color.RED);
			}
		}
		vx = vx * 0.99f;
		scrollBy((int)vx, 0);
		super.onDraw(canvas);
	}

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		int x;
		
		if (l < 0)
			x = 0;
		else if (l > (width - screenWidth))
			x = width - screenWidth;
		else
			x = l;
		
		scrollTo(x, 0);
		super.onScrollChanged(l, t, oldl, oldt);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.d(TAG, "onSizeChanged()");
		
		screenWidth = w;
		
		Log.d(TAG, "x="+w+" y="+h+" w="+screenWidth);
		
		// we always only want to fill the vertical
		setHeight(h);
		allocate();
		
		drawBackground();
		drawGrid();
		drawAxis();
		
		scrollTo(width - w, 0);
		
		Log.d(TAG, "wake up drawing thread");
		if (mHandler != null) mHandler.sendEmptyMessage(REDRAW);
		
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return mg.onTouchEvent(event);
	}
	
	public boolean onDown(MotionEvent e) {
		vx = 0;
		float x = batteryData.xPosToTime(getScrollX() + e.getX(), width);
		Date d = new Date((long)(x * 1000));
		axisText.setText(String.format("%1$tA %1$te %1$tb %1$tI:%1$tM %1$tp %1$tY", d));
		return true;
	}

	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		vx = -velocityX/100;
		return true;
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		
		float x = batteryData.xPosToTime(getScrollX() + e2.getX(), width);
		Date d = new Date((long)(x * 1000));
		axisText.setText(String.format("%1$tA %1$te %1$tb %1$tI:%1$tM %1$tp %1$tY", d));
		
		scrollBy((int)distanceX, 0);
		return true;
	}

	public void onShowPress(MotionEvent e) {
	}

	public boolean onSingleTapUp(MotionEvent arg0) {
		return false;
	}
	
	// ----------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------
	
	private void drawGraph() {
		Path p = batteryData.getPath(width, height - axisLabelArea);
		if (p != null) {
			mCanvas.drawPath(p, mPaintChart);
		} else {
			float[] pts = batteryData.getLines(width, height - axisLabelArea);
			if (pts != null) {
				mCanvas.drawLines(pts, mPaintChart);
			}
		}
	}
	
	private void drawGrid() {
		for (float level = height - axisLabelArea; level > 0; level -= (float)(height-axisLabelArea)/10f) {
			mCanvas.drawLine(0, level, width, level, mPaintAxisGrid);
		}
		
		for (float x = width; x > 0; x -= screenWidth) {
			int batLev = 0;
			for (float level = height - axisLabelArea; level > 0; level -= (float)(height-axisLabelArea)/10f) {
				mCanvas.drawText(String.format("%d%%", 10*batLev++), (int)x, (int)level-3, mPaintAxisText);
			}
		}
		
		mCanvas.drawLine(width, height-axisLabelArea+32, width, 0, mPaintAxis);
		mCanvas.drawLine(width, height-axisLabelArea+32, width - 12, height-axisLabelArea+44, mPaintAxis);
		mCanvas.drawLine(width - 12, height-axisLabelArea+44, width - 32, height-axisLabelArea+44, mPaintAxis);
		mCanvas.drawText("now", width-58, height - axisLabelArea + 47, mPaintTextExtra);
		
		for (float t = width; t > 0; t -= width/12) {
			if (t == width) continue;
			float x = batteryData.xPosToTime(t, width);
			Date d = new Date((long)(x * 1000));
			mCanvas.drawText(String.format("%1$tr", d), t-5 < 0 ? 0 : t-5, height - axisLabelArea + 12, mPaintAxisText);
			mCanvas.drawText(String.format("%1$tF", d), t-5 < 0 ? 0 : t-5, height - axisLabelArea + 22, mPaintAxisText);
			mCanvas.drawLine(t, height - axisLabelArea + 4, t, 0, mPaintAxisGrid);
		}
	}
	
	private void drawAxis() {
		mCanvas.drawLine(0, height-axisLabelArea, width, height-axisLabelArea, mPaintAxis);
	}
	
	private void drawBackground() {
		int[] gradColours = {0x80ffffff, 0x80000000, 0x00000000};
		float[] posiColours = {0.0F, 0.5F, 1.0F};
		LinearGradient mGradient = new LinearGradient(160, 0, 160, height, gradColours, posiColours, Shader.TileMode.CLAMP);
		mPaintBackground = new Paint();
		mPaintBackground.setShader(mGradient);
		
		int[] lGradColours = {0xff00ff00, 0xffffff00, 0xffff0000};
		LinearGradient lGradient = new LinearGradient(160, 0, 160, height - axisLabelArea, lGradColours, null, Shader.TileMode.CLAMP);
		mPaintChart.setShader(lGradient);
		
		Log.d(TAG, "mCanvas=" + mCanvas + " w=" + mCanvas.getWidth() + " h=" + mCanvas.getHeight());
		mCanvas.drawPaint(mPaintBackground);
	}

	int getResolution() {
		return resolution;
	}
	
	void setResolution(int res) {
		resolution = res;
		if (mHandler != null) mHandler.sendEmptyMessage(REDRAW);
	}
	
	void increaseRes() {
		resolution -= 10;
		if (resolution < 1) resolution = 1;
		if (mHandler != null) mHandler.sendEmptyMessage(REDRAW);
	}
	
	void decreaseRes() {
		resolution += 10;
		if (mHandler != null) mHandler.sendEmptyMessage(REDRAW);
	}
	
	void setTimeWindow(long timewindow) {
		timeWindow = timewindow;
		if (mHandler != null) mHandler.sendEmptyMessage(REDRAW);
	}
	
	long getTimeWindow() {
		return timeWindow;
	}

	void interruptDrawingThread() {
		if (mHandler != null) mHandler.sendEmptyMessage(REDRAW);
	}
	
	void pauseDrawingThread(boolean pause) {
		pausePainter = pause;
	}
	
	void startDrawingThread() {
		try {
			painter.start();
		} catch (IllegalThreadStateException e) {
			Log.e(TAG, "attempt to restart painter thread ignored.");
		}
	}
	
	void destroyDrawingThread() {
		if (mHandler != null) mHandler.sendEmptyMessage(DIE);
	}
	
	void setWidth(int w) {
		width = w;
	}
	
	void setHeight(int h) {
		height = h;
	}
	
	void allocate() {
		if (mBitmap != null) mBitmap.recycle();
		mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		if (mCanvas == null) {
			mCanvas = new Canvas();
		}
		mCanvas.setBitmap(mBitmap);
	}
	
	void clearCanvas() {
		if (mCanvas != null)
			mCanvas.drawColor(Color.BLACK);
	}
}