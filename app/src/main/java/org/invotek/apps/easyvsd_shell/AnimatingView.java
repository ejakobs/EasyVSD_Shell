package org.invotek.apps.easyvsd_shell;

import java.util.ArrayList;

import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

public class AnimatingView extends View {

	private Bitmap pageImage;
	private RectF cR;
	private RectF nRect;
	private float gL, gT, gR, gB;
	private float nL, nT, nR, nB;
	public boolean animating = false;
	private Handler mHandler;
	private int stepsCompleted;
	private float totalSteps;
	
	ArrayList<AnimatingViewListener> listeners;
	
	public AnimatingView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void addAnimatingtViewListener(AnimatingViewListener listener){
		if(listeners == null)
			listeners = new ArrayList<AnimatingViewListener>();
		listeners.add(listener);
	}
	
	public void startAnimation(int startLeft, int startTop, int startRight, int startBottom, int endLeft, int endTop,
			int endRight, int endBottom, float stepsToAnimate, Bitmap pageToMove, Context currentContext, int actionBarHeight)
	{
		pageImage = pageToMove;
		
		Rect animateRect = new Rect();
		Point animatePt = new Point();
		this.getGlobalVisibleRect(animateRect, animatePt);
		
		cR = new RectF(startLeft, startTop - actionBarHeight, startRight, startBottom - actionBarHeight);
		nRect = cR;
		
		gL = (endLeft - startLeft) / stepsToAnimate;
		gT = (endTop - startTop) / stepsToAnimate;
		gR = (endRight - startRight) / stepsToAnimate;
		gB = (endBottom - startBottom) / stepsToAnimate;
		
		stepsCompleted = 0;
		totalSteps = stepsToAnimate;
		animating = true;
		
//		if((gR < 0)||(gB < 0))
//		{
			for(AnimatingViewListener listener: listeners)
				listener.onAnimationStarted();
//		}
		
		mHandler = new Handler();
		mHandler.post(mUpdate);
	}
	
	public interface AnimatingViewListener
	{
		void onAnimationFinished();
		void onAnimationStarted();
	}
	
	public void clearView(){
		if(pageImage != null){
			pageImage.recycle();
			pageImage = null;
		}
		
		cR = null;
		nRect = null;
	}
	
	private Runnable mUpdate = new Runnable(){
		@Override
		public void run(){
			if(animating){
				if(stepsCompleted < totalSteps){
					nL = cR.left + gL;
					nT = cR.top + gT;
					nR = cR.right + gR;
					nB = cR.bottom + gB;
					nRect.set(nL, nT, nR, nB);

					++stepsCompleted;
					invalidate();
					mHandler.postDelayed(this, 10);
				}else
				{
					animating = false;
//					if((gR >= 0) || (gB >= 0))
//					{
						clearView();
						for(AnimatingViewListener listener : listeners)
							listener.onAnimationFinished();
//					}
						invalidate();
				}
			}
		}
	};

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if(animating){
			if(pageImage != null)
			{
				canvas.drawBitmap(pageImage, null, nRect, null);
				cR = nRect;
			}
		}
	}
}
