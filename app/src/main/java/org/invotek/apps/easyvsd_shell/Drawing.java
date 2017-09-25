package org.invotek.apps.easyvsd_shell;

import java.util.ArrayList;
import java.util.Locale;

import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

public class Drawing {
	private static final int DEFAULT_COLOR = R.color.blue;
	private static final float DEFAULT_WIDTH = 15;
	
	private String drawingString;
	protected SerializablePath path_being_drawn;
	protected int currentColor;
	protected float stroke_width;
	private ArrayList<SerializablePath> allPaths;
	private ArrayList<Integer> allColors;
	private ArrayList<Float> allWidths;
	private int width = -1 ;
	private int height = -1;
	
	public Drawing(Resources res, int width, int height){
		this(res, new ArrayList<SerializablePath>(), new ArrayList<Integer>(), new ArrayList<Float>(), width, height);
	}
	
	public Drawing(Resources res, float strokeWidth, int width, int height){
		this(res, new ArrayList<SerializablePath>(), new ArrayList<Integer>(), new ArrayList<Float>(), width, height);
		stroke_width = strokeWidth;
	}
	
	public Drawing(Resources res, int color, int width, int height){
		this(res, new ArrayList<SerializablePath>(), new ArrayList<Integer>(), new ArrayList<Float>(), width, height);
		currentColor = color;
	}
	
	public Drawing(Resources res, int color, float strokeWidth, int width, int height){
		this(res, new ArrayList<SerializablePath>(), new ArrayList<Integer>(), new ArrayList<Float>(), width, height);
		currentColor = color;
		stroke_width = strokeWidth;
	}
	
	public Drawing(Resources res, ArrayList<SerializablePath> paths, ArrayList<Integer> colors, ArrayList<Float> widths, int width, int height){
		allPaths = paths;
		allColors = colors;
		allWidths = widths;
		
		if(allColors.isEmpty())
			currentColor = res.getColor(DEFAULT_COLOR);
		else
			currentColor = allColors.get(allColors.size() - 1);
		
		if(allWidths.isEmpty())
			stroke_width = DEFAULT_WIDTH;
		else
			stroke_width = allWidths.get(allWidths.size() - 1);
	}
	
	public Drawing(String drawingString){
		this.drawingString = drawingString;
	}
	
	public String saveDrawing(int width, int height, boolean reordering){
		this.width = ((width == this.width) || (this.width == -1)) ? width : this.width;
		this.height = ((height == this.height) || (this.height == -1)) ? height : this.height;
		Log.i("PlayTalk", "Drawing.saveDrawing: entered, dimens=[" + this.width + "," + this.height + "]");
		drawingString = DrawingToString(this, this.width, this.height, reordering);
		return drawingString;
	}
	
	private Drawing loadDrawing(int width, int height){
		if(TextUtils.isEmpty(drawingString)){ return this;}
		Log.i("PlayTalk", "Drawing.loadDrawing: entered, dimens=[" + width + "," + height + "]");
		this.width = width;
		this.height = height;
		String allText = drawingString;
		String currentPoint;
		int startIndex, endIndex, commaPoint;
		float[] point;
		double x,y;
//		int pathCount=0;
//		int pointCount=0;
		startIndex = allText.indexOf("[");
		endIndex = allText.indexOf("]");
		startIndex = allText.indexOf("[");
		endIndex = allText.indexOf("]");
		
		while(!TextUtils.isEmpty(allText) && (endIndex > -1)){
			currentPoint = allText.substring(startIndex + 1, endIndex - startIndex);
			if(currentPoint.contains("path"))
			{
				commitPath();
				commaPoint = currentPoint.indexOf(",");
				currentPoint = currentPoint.substring(commaPoint + 1);
				commaPoint = currentPoint.indexOf(",");
				try{
					setColor(Integer.parseInt(currentPoint.substring(0, commaPoint)));
				}catch(NumberFormatException e1){
					Log.i("PlayTalk", "Drawing.loadDrawing1: Error: " + e1.getMessage() );
					Log.i("PlayTalk", "Drawing.loadDrawing1: currentPoint=[" + currentPoint + "]");
					e1.printStackTrace();
					setColor(DEFAULT_COLOR);
				}
				try{
					setStrokeWidth(Float.parseFloat(currentPoint.substring(commaPoint + 1)));
				}catch(NumberFormatException e2){
					Log.i("PlayTalk", "Drawing.loadDrawing2: Error: " + e2.getMessage() );
					Log.i("PlayTalk", "Drawing.loadDrawing2: currentPoint=[" + currentPoint + "]");
					e2.printStackTrace();
					setStrokeWidth(DEFAULT_WIDTH);
				}
//				pathCount++;
			}
			else
			{
				
				try{
					commaPoint = currentPoint.indexOf(",");
					point = new float[2];
					
					x = Double.parseDouble(currentPoint.substring(0, commaPoint));
					y = Double.parseDouble(currentPoint.substring(commaPoint + 1));
					
					point[0] = (float)(x * width * -1);
					point[1] = (float)(y * height * -1);
					getPath().addPathPoint(point);
				}catch(NumberFormatException e3){
					Log.i("PlayTalk", "Drawing.loadDrawing3: Error: " + e3.getMessage() );
					Log.i("PlayTalk", "Drawing.loadDrawing3: currentPoint=[" + currentPoint + "]");
					e3.printStackTrace();
				}
//				pointCount++;
			}
			allText = allText.substring(endIndex + 1);
			startIndex = allText.indexOf("[");
			endIndex = allText.indexOf("]");
		}
		//Log.i("PlayTalk", "Drawing.loadDrawing: exit, " + pathCount + " paths and " + pointCount + " points"); 
		commitPath();
		return this;
	}
	
	public Drawing reloadDrawing(int width, int height) {
		Log.i("PlayTalk", "Drawing.reloadDrawing: width=" + width + ", height=" + height );
		unloadDrawing();
		return loadDrawing( width, height);
	}
	
	public void unloadDrawing(){
		//Log.i("PlayTalk", "Drawing.unloadDrawing: enter" ); 
		if(path_being_drawn != null){
			path_being_drawn.clear();
			path_being_drawn = null;
		}
		if(allPaths != null){
			for(SerializablePath p : allPaths){
				p.clear();
			}
			allPaths.clear();
			allPaths = null;
		}
		if(allColors != null){
			allColors.clear();
			allColors = null;
		}
		if(allWidths != null){
			allWidths.clear();
			allWidths = null;
		}
	}

	public boolean requiresResize(int width, int height){
		if((this.width != width) && (this.height != height)){
			this.width = width;
			this.height = height;
			return true;
		}
		return false;
	}
	
	public void clearDrawing(){
		unloadDrawing();
		drawingString = "empty";
	}
	
	public SerializablePath getPath(){
		if(path_being_drawn == null)
			path_being_drawn = new SerializablePath();
		return path_being_drawn;
	}
	
	public void erasePoint(float[] point){
		//Log.i("PlayTalk", "Drawing.erasePoint entry" +
		//	", point=" + formatFloatArray(point) );
		if(allPaths == null) return;
		int numPaths = allPaths.size();
		int i = 0;
		while(i < numPaths){
			SerializablePath p = allPaths.get(i);
			int first = -1;
			int last = -1;
			int size = p.pathPoints.size();
			//Log.i("PlayTalk", "Drawing.erasePoint: " + this.toString() + 
			//		" checking path[" + Integer.toString(i) +
			//		"] with " + Integer.toString(size) + " points" );
			for(int j = 0; j < size; j++){
				if(getDistance(point, p.pathPoints.get(j)) > getStrokeWidth(null))
				{
					if(last < first)
						last = j;
				}
				else{ 
					if(first < 0)
						first = j;
				}
			}
			if (first > -1)
			{
				//Log.i("PlayTalk", "Drawing.erasePoint: " + this.toString() + 
				//	" splitting path[" + Integer.toString(i) + "]" +
				//	", first=" + Integer.toString(first) +
				//	", last=" + Integer.toString(last) );
				allPaths.remove(i);
				int color = allColors.remove(i);
				float width = allWidths.remove(i);
				--numPaths;
				int numAdded = 0;
				if(first > 0){
					allPaths.add(i, new SerializablePath(p.pathPoints.subList(0, first)));
					allColors.add(i, color);
					allWidths.add(i, width);
					++numAdded;
				}
				if(last > first){
					if(i + numAdded >= allPaths.size()){
						allPaths.add(new SerializablePath(p.pathPoints.subList(last,size)));
						allColors.add(color);
						allWidths.add(width);
					}else{
						allPaths.add(i + numAdded, new SerializablePath(p.pathPoints.subList(last,size)));
						allColors.add(i + numAdded, color);
						allWidths.add(i + numAdded, width);
					}
					++numAdded;
				}
				i += numAdded;
			}else{
				++i;
			}
		}
	}
	
	public double getDistance(float[] p1, float[] p2)
	{
		return Math.sqrt(Math.pow(p1[0] - p2[0], 2) + Math.pow(p1[1]- p2[1], 2));
	}
	
	public void commitPath(){
		if((path_being_drawn == null) || (path_being_drawn.isEmpty()))
			return;
		if(allPaths == null)
			allPaths = new ArrayList<SerializablePath>();
		allPaths.add(path_being_drawn);
		if(allColors == null)
			allColors = new ArrayList<Integer>();
		allColors.add(currentColor);
		if(allWidths == null)
			allWidths = new ArrayList<Float>();
		allWidths.add(stroke_width);
		path_being_drawn = null;
		//Log.i("PlayTalk", "Drawing.commitPath: allPaths=" + allPaths.size() + " paths" );
		return;
	}
	
	public void setStrokeWidth(float newWidth){
		if(newWidth < 0)
			return;
		commitPath();
		stroke_width = newWidth;
	}
	
	public float getStrokeWidth(SerializablePath path){
		float ret;
		if(allPaths == null)
			ret = stroke_width;
		else{
			int index = allPaths.indexOf(path);
			if((index < 0) || (index >= allWidths.size()))
				ret = stroke_width;
			else
				ret = allWidths.get(index);
		}
		return validateWidth(ret);
	}
	
	float[] validWidths = {15, 25, 50};
	private float validateWidth(float width){
		float ret = DEFAULT_WIDTH;
		for(float w : validWidths){
			if(w == width)ret = width;
		}
		return ret;
	}
	
	public ArrayList<SerializablePath> getGraphicsPath(){
		if(allPaths == null)
			allPaths = new ArrayList<SerializablePath>();
		return allPaths;
	}
	
	public void removePaths(int removeTo){
		if((allPaths == null) || (allPaths.size() < 1) || (removeTo >= allPaths.size())){return;}
		Log.d("Drawing", "removePaths: removeTo=" + removeTo + " size=" + allPaths.size());
		removeTo = Math.max(0, removeTo);
		removeTo = Math.min(removeTo, allPaths.size() - 1);
		for(int i = allPaths.size() - 1; i >= removeTo; --i){
			Log.d("Drawing", "removing Path [" + i + "]");
			allPaths.remove(i);
			allColors.remove(i);
			allWidths.remove(i);
		}
	}
	
	public void setColor(int newColor){
		if(newColor == 0)
			return;
		commitPath();
		currentColor = newColor;
		//Log.i("PlayTalk","Drawing.setColor: newColor=" + currentColor);
	}

	public int getColor(SerializablePath path){
		int ret = DEFAULT_COLOR;
		if(allPaths == null)
			ret = currentColor;
		else{
			int index = allPaths.indexOf(path);
			if((index < 0) || (index >= allColors.size()))
				ret = currentColor;
			else
				ret = allColors.get(index);
		}
		return validateColor(ret);
	}
	
	public int validateColor(int color){
		int ret = validColors[1];
		if(contains(validColors, color))
			ret = color;
		return ret;
	}
	
	static int[] validColors = {R.color.black, R.color.blue, R.color.limegreen, R.color.pink, 
		R.color.purple, R.color.red, R.color.white, R.color.yellow};
	public static void loadColors(Resources res){
		if(validColors[0] == R.color.black){
			for(int i = 0; i < validColors.length; ++i){
				validColors[i] = res.getColor(validColors[i]);
				//Log.i("Drawing", "validColors[" + i + "] = " + validColors[i] + "]");
			}
		}
	}
	
	private boolean contains( final int[] array, final int color ) {
	    for ( int c : array )
	        if ( c == color)
	            return true;
	    return false;
	}
	
	public void Dispose()
	{
		unloadDrawing();
		drawingString = null;
	}
	
	
	public static String DrawingToString(Drawing d, int width, int height, boolean reordering)
	{
		if( d != null ) {
			//Log.i("PlayTalk", "Drawing.DrawingToString: width=" + width + ", height=" + height + ", drawing=NotNull");
		} else {
			//Log.i("PlayTalk", "Drawing.DrawingToString: width=" + width + ", height=" + height + ", drawing=Null");
		}
		if(reordering){
			if((d == null) || (d.drawingString == null) ||(d.drawingString.isEmpty()))
				return "empty";
			else
				return d.drawingString;
		}else{
			try{
				if(d.allPaths != null){
					StringBuilder ret = new StringBuilder();
					for(SerializablePath p: d.getGraphicsPath()){
						ArrayList<float[]> points = p.pathPoints;
						ret.append("[path,");
						ret.append(String.valueOf(d.getColor(p)) + ",");
						ret.append(String.valueOf(d.getStrokeWidth(p)) + "]");
						for(int i = 0; i < points.size(); ++i){
							ret.append("[" + String.format(Locale.US, "%.6f", (double)points.get(i)[0]/(double)width * -1));
							ret.append("," + String.format(Locale.US, "%.6f", (double)points.get(i)[1]/(double)height * -1) + "], ");
						}
					}
					String returning = ret.toString();
					if((returning == null) || (returning.isEmpty()))
						return "empty";
					else
						return returning;
				}else{
					if((d == null) || (d.drawingString == null) ||(d.drawingString.isEmpty()))
						return "empty";
					else
						return d.drawingString;
				}
			}catch(Exception ex){
				ex.printStackTrace();
				if((d == null) || (d.drawingString == null) ||(d.drawingString.isEmpty()))
					return "empty";
				else
					return d.drawingString;
			}
		}
	}
	
	public static Drawing StringToDrawing(String s, int width, int height, Resources resources){
		Drawing ret = new Drawing(resources, width, height);		
		String allText = s;
		String currentPoint;
		int startIndex, endIndex, commaPoint, color;
		float[] point;
		double x,y;
		float stroke_width;
		
		if( (s != null) && (s.length() > 0) ) {
			//Log.i("PlayTalk", "Drawing.StringToDrawing: width=" + width + ", height=" + height + ", string=" + s.length() + " chars");
		} else {
			//Log.i("PlayTalk", "Drawing.StringToDrawing: width=" + width + ", height=" + height + ", string=empty");
		}
		
		startIndex = allText.indexOf("[");
		endIndex = allText.indexOf("]");
		while(!TextUtils.isEmpty(allText) && (endIndex > -1)){
			currentPoint = allText.substring(startIndex + 1, endIndex - startIndex);
			if(currentPoint.contains("path"))
			{
				ret.commitPath();
				commaPoint = currentPoint.indexOf(",");
				currentPoint = currentPoint.substring(commaPoint + 1);
				commaPoint = currentPoint.indexOf(",");
				try{
					color = Integer.parseInt(currentPoint.substring(0, commaPoint));
				}catch(NumberFormatException e1)
				{
					e1.printStackTrace();
					color = resources.getColor(DEFAULT_COLOR);
				}
				try{
					stroke_width = Float.parseFloat(currentPoint.substring(commaPoint + 1));
				}catch(NumberFormatException e2){
					e2.printStackTrace();
					stroke_width = DEFAULT_WIDTH;
				}
				ret.setColor(color);
				ret.setStrokeWidth(stroke_width);
			}
			else
			{
				commaPoint = currentPoint.indexOf(",");
				point = new float[2];
				try
				{
					x = Double.parseDouble(currentPoint.substring(0, commaPoint));
					y = Double.parseDouble(currentPoint.substring(commaPoint + 1));
					
					point[0] = (float)(x * width *-1);
					point[1] = (float)(y * height * -1);
					ret.getPath().addPathPoint(point);
				}catch(NumberFormatException e3)
				{
					e3.printStackTrace();
				}
			}
			allText = allText.substring(endIndex + 1);
			startIndex = allText.indexOf("[");
			endIndex = allText.indexOf("]");
		}
		ret.commitPath();
		return ret;
	}
	
	public boolean isaKeeper( ) {
		// Check whether this drawing contains anything more than empty Lists of points, colors, and stroke widths
		// Return true if it does.
		if( ((allPaths==null) || (allPaths.isEmpty())) && 
			((allWidths==null) || (allWidths.isEmpty())) && 
			((allColors==null) || (allColors.isEmpty())) ) {
			return false;
		} else {
			return true;
		}
	}
	
	public int pathCount( ) {
		if( allPaths != null ) {
			return allPaths.size();
		} else {
			return 0;
		}
	}
	
	public int pointCount( int pathIndex ) {
		if( pathIndex >= 0 && allPaths != null && pathIndex < allPaths.size() ) {
			return allPaths.get(pathIndex).pointCount();
		} else {
			return 0;
		}
	}
	
	public void toLog( ) {
		Log.i("PlayTalk", "Drawing: drawing " + this.toString() + " contains " +
			Integer.toString(this.pathCount()) + " paths");	
		if( this.pathCount() > 0 ) {
			for( int i=0; i<allPaths.size(); i++ ) {
				Log.i("PlayTalk", ".. path[" + Integer.toString(i) + "]" +
					" Color=[" + Integer.toHexString(allColors.get(i)) + "]" +
					" Stroke=" + Float.toString(allWidths.get(i)) +
					" PointCount=" + Integer.toString(allPaths.get(i).pointCount()) );
				//SerializablePath sp = allPaths.get(i);
				//for( int j=0; j<Math.min(4, sp.pointCount()); j++ ){
				//	float[] temp = sp.pathPoints.get(j);
				//	Log.i("PlayTalk", ".... Point[" + Integer.toString(j) + "]=" + formatFloatArray(temp) );
				//}
			}
		}
	}
	
//	private String formatIntegerArray( int[] x ) {
//		String ret = "[";
//		for( int i=0; i<x.length; i++ ) {
//			ret += Integer.toString(x[i]);
//			if( i<(x.length-1)) {
//				ret += ",";
//			}
//		}
//		ret += "]";
//		return ret;
//	}
//	private String formatFloatArray( float[] x ) {
//		String ret = "[";
//		for( int i=0; i<x.length; i++ ) {
//			ret += Float.toString(x[i]);
//			if( i<(x.length-1)) {
//				ret += ",";
//			}
//		}
//		ret += "]";
//		return ret;
//	}

}
