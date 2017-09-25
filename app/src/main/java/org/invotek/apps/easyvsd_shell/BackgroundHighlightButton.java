package org.invotek.apps.easyvsd_shell;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.support.v7.widget.AppCompatButton;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;

public class BackgroundHighlightButton extends AppCompatButton {
	
	private State currentState = State.NORMAL;
	
	private int normalNavigationColor;
	private int normalDrawingColor;
	private int adminButtonColor;
	private int selectedPageColor;
	private int selectedActivityColor;
	private int selectedPausePointColor;
	private int movingColor;
	private int selectedDrawingColor;
	
	private Paint linePaint;
	private float[] corners;
	
	Paint textPaint;
	private float textX;
	private float textY;
	
	private boolean videoButton = false;
	private Paint bp;
	private Paint tp;
	private Rect box;
	
	private Bitmap backgroundImage;
	private int backgroundResource;
	private Rect backgroundSrcRect;
	private Rect backgroundDstRect;
	
	private Bitmap foregroundImage;
	private int foregroundResource;
	private Rect foregroundSrcRect;
	private Rect foregroundDstRect;
	
	private boolean cropToFill = false;
	private boolean adminButton = false;
	private boolean fillButton = false;
	private int navBorderWidth = 20;
	private int adminBorderWidth = 0;
	private int buttonSize = 125;


	private int mViewWidth;
	private int mViewHeight;
	
	public enum State {NORMAL, NORMAL_DRAWING, SELECTED_ACTIVITY, SELECTED_PAGE, SELECTED_PAUSEPOINT, MOVING, ADMIN, SELECTED_DRAWING}
	
	public BackgroundHighlightButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        navBorderWidth = (int)Math.round(navBorderWidth * Math.max(metrics.widthPixels, metrics.heightPixels) / 2560f);
		
		linePaint = new Paint();
		linePaint.setColor(Color.BLACK);
		linePaint.setStyle(Style.STROKE);
		linePaint.setStrokeWidth(navBorderWidth/2);
		
		textPaint = new Paint();
		textPaint.setColor(getCurrentTextColor());
		textPaint.setTextSize(getTextSize());
		textPaint.setTextAlign(Align.CENTER);
		setColors(context.getResources());
		
		bp = new Paint();
		bp.setColor(selectedPageColor);
		bp.setStyle(Style.FILL_AND_STROKE);
		bp.setStrokeJoin(Paint.Join.ROUND);
		bp.setStrokeCap(Paint.Cap.ROUND);
		bp.setStrokeWidth(1.0F);
		tp = new Paint();
		tp.setColor(Color.BLACK);
		tp.setTextAlign(Align.CENTER);
		tp.setTextSize(45.0F);	
		tp.setStyle(Style.FILL);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		cropToFill = prefs.getBoolean("crop_to_fill", false);
	}

	public BackgroundHighlightButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        navBorderWidth = (int)Math.round(navBorderWidth * Math.max(metrics.widthPixels, metrics.heightPixels) / 2560f);
		
		linePaint = new Paint();
		linePaint.setColor(Color.BLACK);
		linePaint.setStyle(Style.STROKE);
		linePaint.setStrokeWidth(navBorderWidth/2);
		
		textPaint = new Paint();
		textPaint.setColor(getCurrentTextColor());
		textPaint.setTextSize(getTextSize());
		textPaint.setTextAlign(Align.CENTER);
		setColors(context.getResources());
		
		bp = new Paint();
		bp.setColor(selectedPageColor);
		bp.setStyle(Style.FILL_AND_STROKE);
		bp.setStrokeJoin(Paint.Join.ROUND);
		bp.setStrokeCap(Paint.Cap.ROUND);
		bp.setStrokeWidth(1.0F);
		tp = new Paint();
		tp.setColor(Color.BLACK);
		tp.setTextAlign(Align.CENTER);
		tp.setTextSize(45.0F);	
		tp.setStyle(Style.FILL);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		cropToFill = prefs.getBoolean("crop_to_fill", false);
	}
	
	public BackgroundHighlightButton(Context context) {
		super(context);
		
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        navBorderWidth = (int)Math.round(navBorderWidth * Math.max(metrics.widthPixels, metrics.heightPixels) / 2560f);
        
		linePaint = new Paint();
		linePaint.setColor(Color.BLACK);
		linePaint.setStyle(Style.STROKE);
		linePaint.setStrokeWidth(navBorderWidth/2);
		
		textPaint = new Paint();
		textPaint.setColor(getCurrentTextColor());
		textPaint.setTextSize(getTextSize());
		textPaint.setTextAlign(Align.CENTER);
		setColors(context.getResources());
		
		bp = new Paint();
		bp.setColor(selectedPageColor);
		bp.setStyle(Style.FILL_AND_STROKE);
		bp.setStrokeJoin(Paint.Join.ROUND);
		bp.setStrokeCap(Paint.Cap.ROUND);
		bp.setStrokeWidth(1.0F);
		tp = new Paint();
		tp.setColor(Color.BLACK);
		tp.setTextAlign(Align.CENTER);
		tp.setTextSize(45.0F);	
		tp.setStyle(Style.FILL);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		cropToFill = prefs.getBoolean("crop_to_fill", false);
	}
	
	private void setColors(Resources res){
		adminButtonColor = res.getColor(R.color.dimgray);
		movingColor = res.getColor(R.color.red);
		normalDrawingColor = res.getColor(R.color.white);
		normalNavigationColor = res.getColor(R.color.black);
		selectedDrawingColor = res.getColor(R.color.yellow);
		selectedActivityColor = res.getColor(R.color.limegreen);
		selectedPageColor = res.getColor(R.color.magenta);
		selectedPausePointColor = res.getColor(R.color.skyblue);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		// save view size
		mViewWidth = w;
		mViewHeight = h;
		setForegroundImageRect();
		setBackgroundImageRect();
		if(foregroundDstRect != null){
			box = new Rect((int)(7 * Math.min(foregroundDstRect.width(), mViewWidth) / 10f) + Math.max(foregroundDstRect.left, 0), 
					(int)(6 * Math.min(foregroundDstRect.height(), mViewHeight) / 10f) + Math.max(foregroundDstRect.top, 0), 
					(int)(9 * Math.min(foregroundDstRect.width(), mViewWidth) / 10f) + Math.max(foregroundDstRect.left, 0), 
					(int)(9 * Math.min(foregroundDstRect.height(), mViewHeight)/ 10f) + Math.max(foregroundDstRect.top, 0));
		}
		Log.d("fillView", "BHB.onSizeChanged: admin=" + adminButton + " w=" + w + " h=" + h);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		try{
			super.onDraw(canvas);
            Log.d("BHB", "onDraw(): id=" + getId() + " state=" + getState() );
			if(backgroundImage == null){
				switch(currentState){
					case NORMAL:
						canvas.drawColor(normalNavigationColor);
						break;
					case NORMAL_DRAWING:
						canvas.drawColor(normalDrawingColor);
						break;
					case SELECTED_PAGE:
						canvas.drawColor(selectedPageColor);
						break;
					case SELECTED_ACTIVITY:
						canvas.drawColor(selectedActivityColor);
						break;
					case SELECTED_PAUSEPOINT:
						canvas.drawColor(selectedPausePointColor);
						break;
					case MOVING:
						canvas.drawColor(movingColor);
						break;
					case ADMIN:
						canvas.drawColor(adminButtonColor);
						break;
					case SELECTED_DRAWING:
						canvas.drawColor(selectedDrawingColor);
						break;
					default:
						canvas.drawColor(normalNavigationColor);
						break;
				}
			}else{
				canvas.drawBitmap(backgroundImage, backgroundSrcRect, backgroundDstRect, null );
			}
			if(foregroundImage != null) {
				if(foregroundImage.isRecycled()){reloadForegroundBitmap();}
				canvas.drawBitmap(foregroundImage, foregroundSrcRect, foregroundDstRect, null);
			}
			if(linePaint.getStrokeWidth() > 0){
				canvas.drawLines(corners, linePaint);
			}
			if(!TextUtils.isEmpty(getText())){
				canvas.drawText(getText().toString(), textX, textY, textPaint);
			}
			
			if(videoButton){
				float radius = Math.min(box.width() / 2, box.height() / 2);
				tp.setTextSize(radius * 1.3f);
				canvas.drawCircle(box.centerX(), box.centerY(), radius, bp);
				canvas.drawText(">", box.centerX(), box.centerY()+(float)(tp.getTextSize()/3.0) , tp);
			}
		}catch (Exception ex) {
			Log.e("Button", "Error drawing: " + ex.toString());
		}
	}

	public Bitmap getButtonBitmap(){
		if(foregroundImage != null)
			return foregroundImage;
		if(backgroundImage != null)
			return backgroundImage;
		return null;
	}

	
	public void setForegroundBitmap(Bitmap b, boolean isVideo){
		if(foregroundImage != b){
			if(foregroundImage != null)
				foregroundImage.recycle();
			foregroundImage = b;
			videoButton = isVideo;
			setForegroundImageRect();
		}
	}
	
	public void setBackgroundBitmap(Bitmap b){
		if(backgroundImage != b){
			if(backgroundImage != null)
				backgroundImage.recycle();
			backgroundImage = b;
			setBackgroundImageRect();
		}
	}

	public void reloadForegroundBitmap(){
		DisplayMetrics metrics = MainActivity.getAppContext().getResources().getDisplayMetrics();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.getAppContext());
		int buttonSize = Integer.parseInt(prefs.getString("nav_button_size", "250")) + 50;
		buttonSize = (int)Math.round(buttonSize * Math.max(metrics.widthPixels, metrics.heightPixels) / 2560f);
		//setState(adminButton ? State.ADMIN : State.NORMAL);
		setForegroundBitmap(decodeBitmapFromResource(MainActivity.getAppContext().getResources(),
				foregroundResource,
				adminButton ? buttonSize : buttonSize * 2,
				buttonSize), false);
	}
	
	public void setForegroundImageResource(Context c, int id, boolean adminButton){
		foregroundResource = id;
		this.adminButton = adminButton;
		linePaint.setStrokeWidth(adminButton ? adminBorderWidth : navBorderWidth);
		DisplayMetrics metrics = c.getResources().getDisplayMetrics();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        buttonSize = Integer.parseInt(prefs.getString("nav_button_size", "250")) + 50;
        buttonSize = (int)Math.round(buttonSize * Math.max(metrics.widthPixels, metrics.heightPixels) / 2560f);
        setState(adminButton ? State.ADMIN : State.NORMAL);
        setForegroundBitmap(decodeBitmapFromResource(c.getResources(), id, adminButton ? buttonSize : buttonSize * 2, buttonSize), false);
	}
	
	public void setBackgroundImageResource(Context c, int id, boolean adminButton, boolean fillButton){
		backgroundResource = id;
		this.adminButton = adminButton;
		this.fillButton = fillButton;
		linePaint.setStrokeWidth(adminButton ? adminBorderWidth : navBorderWidth);
		DisplayMetrics metrics = c.getResources().getDisplayMetrics();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        int buttonSize = Integer.parseInt(prefs.getString("nav_button_size", "250")) + 50;
        buttonSize = (int)Math.round(buttonSize * Math.max(metrics.widthPixels, metrics.heightPixels) / 2560f);
        setState(adminButton ? State.ADMIN : State.NORMAL);
        if(fillButton)
        	setBackgroundBitmap(decodeBitmapFromResource(c.getResources(), id, mViewWidth, mViewHeight));
        else
        	setBackgroundBitmap(decodeBitmapFromResource(c.getResources(), id, adminButton ? buttonSize : buttonSize * 2, buttonSize));
	}
	
	public void setState(State newState){
		currentState = newState;
		invalidate();
	}
	
	public State getState(){return currentState;}
	
	private void setForegroundImageRect(){
		if(foregroundImage != null){
			int borderWidth = adminButton ? adminBorderWidth : navBorderWidth;
			foregroundSrcRect = null;
			foregroundDstRect = null;
			System.gc();
			int x1 = 0;
			int y1 = 0;
			int x2 = 0;
			int y2 = 0;
			int newHeight = 0;
			int newWidth = 0;
			foregroundSrcRect = new Rect(0, 0, foregroundImage.getWidth(), foregroundImage.getHeight());
			if(foregroundImage.getWidth() > foregroundImage.getHeight()){
				// picture taken in landscape
				if(cropToFill && !adminButton){
					newHeight = mViewHeight - (borderWidth * 2);
					newWidth = (int)((float)foregroundImage.getWidth() * ((float)newHeight / (float)foregroundImage.getHeight()));
					y1 = borderWidth;
					y2 = y1 + newHeight;
					x1 = (mViewWidth - newWidth) / 2;
					x2 = x1 + newWidth;
				}else{
					newWidth = mViewWidth - (borderWidth * 2);
					newHeight = (int)((float)foregroundImage.getHeight() * ((float)newWidth / (float)foregroundImage.getWidth()));
					if(newHeight + (borderWidth * 2) < mViewHeight){
						x1 = borderWidth;
						x2 = x1 + newWidth;
						y1 = (Math.abs(mViewHeight - newHeight) / 2);
						y2 = y1 + newHeight;
					}else{
						newHeight = mViewHeight;
						newWidth = (int)((float)foregroundImage.getWidth() * ((float)newHeight / (float)foregroundImage.getHeight()));
						y1 = borderWidth;
						y2 = y1 + newHeight;
						x1 = (Math.abs(mViewWidth - newWidth) / 2);
						x2 = x1 + newWidth;
					}
				}
			}else{
				// picture taken in portrait
				if(cropToFill && !adminButton){
					newWidth = mViewWidth - (borderWidth * 2);
					newHeight = (int)((float)foregroundImage.getHeight() * ((float)newWidth / (float)foregroundImage.getWidth()));
					x1 = borderWidth;
					x2 = x1 + newWidth;
					y1 = (mViewHeight - newHeight) / 2;
					y2 = y1 + newHeight;
				}else{
					newHeight = mViewHeight - (borderWidth * 2);
					newWidth = (int)((float)foregroundImage.getWidth() * ((float)newHeight / (float)foregroundImage.getHeight()));
					if(newWidth + (borderWidth * 2) < mViewWidth){
						y1 = borderWidth;
						y2 = y1 + newHeight;
						x1 = (Math.abs(mViewWidth - newWidth) / 2);
						x2 = x1 + newWidth;
					}else{
						newWidth = mViewWidth;
						newHeight = (int)((float)foregroundImage.getHeight() * ((float)newWidth / (float)foregroundImage.getWidth()));
						x1 = borderWidth;
						x2 = x1 + newWidth;
						y1 = (Math.abs(mViewHeight - newHeight) / 2);
						y2 = y1 + newHeight;
					}
				}
			}
			foregroundDstRect = new Rect(x1, y1, x2, y2);
		}
		corners = new float[]{0, 0, 0, mViewHeight, 0, 0, mViewWidth, 0, mViewWidth, 0, mViewWidth, mViewHeight, 0, mViewHeight, mViewWidth, mViewHeight};
		
		if(!TextUtils.isEmpty(getText())){
			Rect bounds = new Rect();
			textPaint.getTextBounds(getText().toString(), 0, getText().length(), bounds);
			textX = (mViewWidth / 2);// + (bounds.width() / 2);
			textY = (mViewHeight / 2) + (bounds.height() / 2);
		}
	}
	
	private void setBackgroundImageRect(){
		if(backgroundImage != null){
			backgroundSrcRect = null;
			backgroundDstRect = null;
			System.gc();
			int x1 = 0;
			int y1 = 0;
			int x2 = 0;
			int y2 = 0;
			int newHeight = 0;
			int newWidth = 0;
			int borderWidth = adminButton ? adminBorderWidth : navBorderWidth;
			backgroundSrcRect = new Rect(0, 0, backgroundImage.getWidth(), backgroundImage.getHeight());
			if(backgroundImage.getWidth() > backgroundImage.getHeight()){
				// picture taken in landscape
				if(cropToFill && !adminButton){
					newHeight = mViewHeight - (borderWidth * 2);
					newWidth = (int)((float)backgroundImage.getWidth() * ((float)newHeight / (float)backgroundImage.getHeight()));
//					if(newWidth + (borderWidth * 2) < mViewWidth){
						y1 = borderWidth;
						y2 = y1 + newHeight;
						x1 = (mViewWidth - newWidth) / 2;
						x2 = x1 + newWidth;
//					}else{
//						newWidth = mViewWidth;
//						newHeight = (int)((float)backgroundImage.getHeight() * ((float)newWidth / (float)backgroundImage.getWidth()));
//						x1 = 0;
//						x2 = x1 + newWidth;
//						y1 = (Math.abs(mViewHeight - newHeight) / 2);
//						y2 = y1 + newHeight;
//					}
				}else if(fillButton){
					x1 = borderWidth;
					x2 = mViewWidth - borderWidth;
					y1 = borderWidth;
					y2 = mViewHeight - borderWidth;
				}else{
					newWidth = mViewWidth - (borderWidth * 2);
					newHeight = (int)((float)backgroundImage.getHeight() * ((float)newWidth / (float)backgroundImage.getWidth()));
					if(newHeight + (borderWidth * 2) < mViewHeight){
						x1 = borderWidth;
						x2 = x1 + newWidth;
						y1 = (Math.abs(mViewHeight - newHeight) / 2);
						y2 = y1 + newHeight;
					}else{
						newHeight = mViewHeight;
						newWidth = (int)((float)backgroundImage.getWidth() * ((float)newHeight / (float)backgroundImage.getHeight()));
						y1 = 0;
						y2 = y1 + newHeight;
						x1 = (Math.abs(mViewWidth - newWidth) / 2);
						x2 = x1 + newWidth;
					}
				}
			}else{
				// picture taken in portrait
				if(cropToFill && !adminButton){
					newWidth = mViewWidth - (borderWidth * 2);
					newHeight = (int)((float)backgroundImage.getHeight() * ((float)newWidth / (float)backgroundImage.getWidth()));
//					if(newHeight + (borderWidth * 2) < mViewHeight){
						x1 = borderWidth;
						x2 = x1 + newWidth;
						y1 = (mViewHeight - newHeight) / 2;
						y2 = y1 + newHeight;
//					}else{
//						newHeight = mViewHeight;
//						newWidth = (int)((float)backgroundImage.getWidth() * ((float)newHeight / (float)backgroundImage.getHeight()));
//						y1 = 0;
//						y2 = y1 + newHeight;
//						x1 = (Math.abs(mViewWidth - newWidth) / 2);
//						x2 = x1 + newWidth;
//					}
				}else if(fillButton){
					x1 = borderWidth;
					x2 = mViewWidth - borderWidth;
					y1 = borderWidth;
					y2 = mViewHeight - borderWidth;
				}else{
					newHeight = mViewHeight - (borderWidth * 2);
					newWidth = (int)((float)backgroundImage.getWidth() * ((float)newHeight / (float)backgroundImage.getHeight()));
					if(newWidth + (borderWidth * 2) < mViewWidth){
						y1 = borderWidth;
						y2 = y1 + newHeight;
						x1 = (Math.abs(mViewWidth - newWidth) / 2);
						x2 = x1 + newWidth;
					}else{
						newWidth = mViewWidth;
						newHeight = (int)((float)backgroundImage.getHeight() * ((float)newWidth / (float)backgroundImage.getWidth()));
						x1 = 0;
						x2 = x1 + newWidth;
						y1 = (Math.abs(mViewHeight - newHeight) / 2);
						y2 = y1 + newHeight;
					}
				}
			}
			backgroundDstRect = new Rect(x1, y1, x2, y2);
		}
		corners = new float[]{0, 0, 0, mViewHeight, 0, 0, mViewWidth, 0, mViewWidth, 0, mViewWidth, mViewHeight, 0, mViewHeight, mViewWidth, mViewHeight};
		
		if(!TextUtils.isEmpty(getText())){
			Rect bounds = new Rect();
			textPaint.getTextBounds(getText().toString(), 0, getText().length(), bounds);
			textX = (mViewWidth / 2);// + (bounds.width() / 2);
			textY = (mViewHeight / 2) + (bounds.height() / 2);
		}
	}
	
	public Bitmap decodeBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight){
		Bitmap ret = null;
		Bitmap tmp = null;
		Log.i("PlayTalk", "ActivityPage.decodeBitmapFromResource entry:"+
			" reqSize=[" + Integer.toString(reqWidth) + "," + Integer.toString(reqHeight) + "]" +
			", resID=" + Integer.toString(resId));
		try{
			System.gc();
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			tmp = BitmapFactory.decodeResource(res, resId, options);
			
			options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
			options.inPurgeable = true;
			options.inInputShareable = true;
			options.inJustDecodeBounds = false;
			ret = BitmapFactory.decodeResource(res, resId, options);
		}catch(Exception e){
			ret = null;
		}finally{
			if(tmp != null){
				tmp.recycle();
				tmp = null;
			}
		}
		Log.i("PlayTalk", "ActivityPage.decodeBitmapFromResource exit:" +
			" size=[" + Integer.toString(ret.getWidth()) + 
			"," + Integer.toString(ret.getHeight()) + "]");
		return ret;
	}
	
	private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight){
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;
		
		if(height > width){
			if((width > reqHeight) || (height > reqWidth))
			{
				final int heightRatio = Math.round((float)width/(float)reqHeight);
				final int widthRatio = Math.round((float)height/(float)reqWidth);
				inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
			}
		}else{
			if((height > reqHeight) || (width > reqWidth)){
				final int heightRatio = Math.round((float)height/(float)reqHeight);
				final int widthRatio = Math.round((float)width/(float)reqWidth);
				inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
			}
		}
		//Log.i("PlayTalk", "ActivityPage.calculateInSampleSize exit" +
		//	", requestSize=[" + Integer.toString(reqWidth) + "," + Integer.toString(reqHeight) + "]" +
		//	", options.outSize=[" + Integer.toString(options.outWidth) + "," + Integer.toString(options.outHeight) + "]" +
		//	", inSampleSize=" + Integer.toString(inSampleSize) );
		return inSampleSize;
	}

}
