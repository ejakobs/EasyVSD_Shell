package org.invotek.apps.easyvsd_shell;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Path;

class SerializablePath extends Path implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1560120917698193536L;
	public ArrayList<float[]> pathPoints;
	
	public SerializablePath(){
		super();
		pathPoints = new ArrayList<float[]>();
	}
	
	public SerializablePath(SerializablePath p){
		super(p);
		pathPoints = p.pathPoints;
	}
	
	public SerializablePath(List<float[]> points){
		this();
		for(float[] p : points)
			addPathPoint(p);
	}
	
	public float[] getFirstPoint()
	{
		return pathPoints.get(0);
	}
	
	public void addPathPoint(float[] point){
		if(pathPoints.isEmpty())
			super.moveTo(point[0], point[1]);
		super.lineTo(point[0], point[1]);
		pathPoints.add(point);
	}
	
	public boolean isPoint(){
		if((pathPoints == null) || pathPoints.isEmpty())
			return false;
		float[] fp = getFirstPoint();
		for(float[] pt : pathPoints){
			if((fp[0] != pt[0]) || (fp[1] != pt[1]))
				return false;
		}
		return true;
	}
	
	public void clear()
	{
		super.reset();
		pathPoints.clear();
	}
	
	public int pointCount( ) {
		return pathPoints.size();
	}
}
