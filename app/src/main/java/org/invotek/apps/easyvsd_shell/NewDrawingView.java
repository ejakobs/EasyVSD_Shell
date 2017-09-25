package org.invotek.apps.easyvsd_shell;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class NewDrawingView extends View {
	
	private Drawing drawings;
	private boolean erasing_enabled = false;
	private boolean drawing_enabled = true;
	private Paint pathPaint;
	private Paint pointPaint;
	private Handler mHandler;
	private boolean closing = false;
	private boolean resizing = false;
	private float strokeWidthMultiplier;
	
	ArrayList<NewDrawingViewListener> listeners = new ArrayList<NewDrawingViewListener>();

	public NewDrawingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        strokeWidthMultiplier = Math.max(metrics.widthPixels, metrics.heightPixels) / 2560f;
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		//Log.i("PlayTalk", "NewDrawingView.onSizeChanged: w=" + w + ", h=" + h + ", oldw=" + oldw + ", oldh=" + oldh);
//		resizing = ((w != oldw) || (h != oldh));
//		invalidate();
	}
	
	public void addDrawingViewListener(NewDrawingViewListener listener){
		listeners.add(listener);
	}
	
	public void setDrawingInfo(Drawing drawings){
		//if((this.drawings == null) ^ (drawings == null)){
		
			//Log.i("PlayTalk", "NewDrawingView.setDrawingInfo: drawings=" + 
			//	(drawings==null ? "null" : drawings.toString()) );
			this.drawings = drawings;
			resizing = true;
			if(drawings != null){
				drawingsToShow = drawings.pathCount();
				this.drawings.toLog();
		        pathPaint = new Paint();
		        pointPaint = new Paint();
		        InitializePaint();
		        
			}else{
				
			}
			invalidate();

		//}
	}
	
	public boolean setEnabled(boolean enabled, boolean erasing){
		drawing_enabled = enabled || erasing;
		erasing_enabled = erasing;
		resizing = true;
		if(drawing_enabled){
			if(mHandler == null) {mHandler = new Handler();}
			mHandler.post(mUpdate);
		}
		stopUpdate = !drawing_enabled;
		super.setEnabled(drawing_enabled);
		return drawing_enabled;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Log.d("Drawing", "DrawingView: onTouch");
		if((drawing_enabled) && (drawings != null))
		{
			switch(event.getAction())
			{
				case MotionEvent.ACTION_DOWN:
					onTouchDown(event);
					return true;
				case MotionEvent.ACTION_MOVE:
					onTouchMove(event);
					return true;
				case MotionEvent.ACTION_UP:
					onTouchUp(event);
					return true;
				default:
					return super.onTouchEvent(event);
			}
		}
		return super.onTouchEvent(event);
	}
	
	private void onTouchDown(MotionEvent event){
		Log.d("Drawing", "DrawingView: onTouch.Down");
		float[] newPoint = new float[]{event.getX(), event.getY()};
		drawings.removePaths(drawingsToShow);
		drawingsToShow = drawings.pathCount();
		if(erasing_enabled)
			drawings.erasePoint(newPoint);
		else
			drawings.getPath().addPathPoint(newPoint);
		lastPoint = newPoint;
	}
	
	private void onTouchMove(MotionEvent event){
		float[] newPoint = new float[]{event.getX(), event.getY()};
		float[] nextPoint = newPoint;
		int drawingsCount = drawings.pathCount();
		//int index = 0;
		do{
			nextPoint = addPoint(newPoint);
			//++index;
			//Log.d("PlayTalk", "NewDrawingView.onTouchMove: (loop " + index + ") newPoint=[" + newPoint[0] + "," + newPoint[1] + 
			//	"] nextPoint=[" + nextPoint[0] + "," + nextPoint[1] + "]");
			if(erasing_enabled)
				drawings.erasePoint(nextPoint);
			else
				drawings.getPath().addPathPoint(nextPoint);
			lastPoint = nextPoint;
		}while(nextPoint != newPoint); // && getDistance(nextPoint, lastPoint) > drawings.getStrokeWidth(null));
		if(erasing_enabled)
			drawingsToShow += (drawings.pathCount() - drawingsCount);
	}
	
	private float[] lastPoint;
	private float[] addPoint(float[] pt){
		// We need points closer together than what is being created by onTouchMove
		// Add in points in the middle
		float distance = drawings.getStrokeWidth(null);
		if(getDistance(pt, lastPoint) > distance)
		{
			float dx = (lastPoint[0] - pt[0]);
			float dy = (lastPoint[1] - pt[1]);
			Log.d("PlayTalk", "addPoint: dx=[" + dx + "] dy=[" + dy + "]");
			if(dx == 0){
				float y1 = lastPoint[1] + distance;
				float y2 = lastPoint[1] - distance;
				if((y1 < pt[1]) && (y1 > lastPoint[1]))
					return new float[]{lastPoint[0], y1};
				else
					return new float[]{lastPoint[0], y2};
			}else if(dy == 0){
				float x1 = lastPoint[0] + distance;
				float x2 = lastPoint[0] - distance;
				if((x1 < pt[0]) && (x1 > lastPoint[0]))
					return new float[]{x1, lastPoint[1]};
				else
					return new float[]{x2, lastPoint[1]};
			}else{
				float m = dy/dx;
				float n = (float)Math.sqrt(1 + Math.pow(m, 2));
				float w = distance/n;
				float x1 = lastPoint[0] + w;
				float x2 = lastPoint[0] - w;
				float y1 = m*(x1 - lastPoint[0]) + lastPoint[1];
				float y2 = m*(x2 - lastPoint[0]) + lastPoint[1];
				if((x1 < pt[0]) && (x1 > lastPoint[0]))
					return new float[]{x1,y1};
				else
					return new float[]{x2, y2};
			}
		}else
		{
			return pt;
		}
	}
	
	public double getDistance(float[] p1, float[] p2)
	{
		return Math.sqrt(Math.pow(p1[0] - p2[0], 2) + Math.pow(p1[1]- p2[1], 2));
	}
	
	private void onTouchUp(MotionEvent event){
		float[] newPoint = new float[]{event.getX(), event.getY()};
		float[] nextPoint = newPoint;
		//int index = 0;
		do{
			//++index;
			//Log.d("PlayTalk", "NewDrawingView.onTouchUp: (loop " + index +") newPoint=[" + newPoint[0] + "," + newPoint[1] + 
			//	"] nextPoint=[" + nextPoint[0] + "," + nextPoint[1] + "]");
			nextPoint = addPoint(newPoint);
			if(erasing_enabled)
				drawings.erasePoint(nextPoint);
			else
				drawings.getPath().addPathPoint(nextPoint);
			lastPoint = nextPoint;
		}while(nextPoint != newPoint); // && getDistance(nextPoint, lastPoint) > drawings.getStrokeWidth(null));
		if(!erasing_enabled){drawings.commitPath();}
		for(NewDrawingViewListener listener : listeners){
			listener.onEditingFinished();
		}
	}
	
	public void clearDrawing(){
		if(drawings != null){
			drawings.clearDrawing();
			for(NewDrawingViewListener listener : listeners){
				listener.onEditingFinished();
			}
		}
	}
	
	public int drawingsToShow = 0;
	public void redo(){
		++drawingsToShow;
		Log.d("PlayTalk", "redo clicked: drawingsToShow= " + drawingsToShow);
	}
	public boolean canRedo(){return ((drawings != null) && (drawingsToShow < drawings.pathCount()));}
	
	
	public void undo(){
		--drawingsToShow;
		Log.d("PlayTalk", "undo clicked: drawingsToShow= " + drawingsToShow);
	}
	public boolean canUndo(){return drawingsToShow > 0;}
	
	public void dispose(){
		if(drawings != null){
			drawings.Dispose();
			drawings = null;
		}
		mHandler = null;
		listeners.clear();
	}
	
	public void close(){
		closing = true;
		dispose();
	}
	
    private void InitializePaint()
    {
    	pathPaint.setDither(true); 
	    pathPaint.setColor(Color.TRANSPARENT);
    	pathPaint.setStyle(Paint.Style.STROKE);
    	pathPaint.setStrokeJoin(Paint.Join.ROUND);
    	pathPaint.setStrokeCap(Paint.Cap.ROUND);
    	if(drawings == null)
    		pathPaint.setStrokeWidth(5 * strokeWidthMultiplier);
    	else
    		pathPaint.setStrokeWidth(drawings.getStrokeWidth(null) * strokeWidthMultiplier);
    	
    	pointPaint.setDither(true);
    	pointPaint.setColor(Color.TRANSPARENT);
    	pointPaint.setStyle(Paint.Style.FILL);
	}
	
	@Override
	public void onDraw(Canvas canvas){
		try{
			if(canvas == null){return;}
			//if(resizing){canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);}
			if((drawings != null))
			{
				for(int i = 0; (i < drawingsToShow) && (i < drawings.pathCount()); ++i){
				//for (SerializablePath path : drawings.getGraphicsPath()) {
					SerializablePath path = drawings.getGraphicsPath().get(i);
					if(path.isPoint()){
						pointPaint.setColor(drawings.getColor(path));
						canvas.drawCircle(path.getFirstPoint()[0], path.getFirstPoint()[1],
								drawings.getStrokeWidth(path) * strokeWidthMultiplier /2f, pointPaint);
					}else{
						pathPaint.setColor(drawings.getColor(path));
						pathPaint.setStrokeWidth(drawings.getStrokeWidth(path) * strokeWidthMultiplier);
						canvas.drawPath(path, pathPaint);
					}
				}
				if(drawings.getPath().isPoint()){
					pointPaint.setColor(drawings.getColor(null));
					canvas.drawCircle(drawings.getPath().getFirstPoint()[0], drawings.getPath().getFirstPoint()[1],
							drawings.getStrokeWidth(null) * strokeWidthMultiplier, pointPaint);
				}else{
					pathPaint.setColor(drawings.getColor(null));
					pathPaint.setStrokeWidth(drawings.getStrokeWidth(null) * strokeWidthMultiplier);
					canvas.drawPath(drawings.getPath(), pathPaint);
				}
			}
			if(resizing){resizing = false;}
		}catch(NullPointerException e){
			e.printStackTrace();
		}
	}
	
	private boolean stopUpdate = false;
	private Runnable mUpdate = new Runnable(){
		@Override
		public void run(){
			if(closing)
				dispose();
			else{
				invalidate();
				if(!stopUpdate){mHandler.postDelayed(this, 10);}
			}
		}
	};
	
	public interface NewDrawingViewListener
	{
		void onEditingFinished();
	}
}
