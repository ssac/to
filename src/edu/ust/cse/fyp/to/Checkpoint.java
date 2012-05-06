package edu.ust.cse.fyp.to;

import com.google.android.maps.GeoPoint;

public class Checkpoint implements Cloneable {
	private double latitude;
	private double longitude;
	private String title = "Unnamed Checkpoint";
	private String desc = "";
	private boolean reached = false;
	private long reachTime;
	
	public Checkpoint(double latitude, double longitude) {
		setLatitude(latitude);
		setLongitude(longitude);
	}
	
	public Checkpoint(GeoPoint point) {
		setPoint(point);
	}
	
	public Checkpoint(Checkpoint cp) {
		setPoint(cp.getPoint());
		setTitle(cp.getTitle());
		setDesc(cp.getDesc());
	}
	
	public double getLatitude() {
		return latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public GeoPoint getPoint() {
		return new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6));
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public String getDesc() {
		return this.desc;
	}
	
	public long getReachedTime() {
		return reachTime;
	}
	
	public boolean isReached() {
		return reached;
	}
	
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	
	public void setPoint(GeoPoint point) {
		this.latitude = point.getLatitudeE6() / 1E6;
		this.longitude = point.getLongitudeE6() / 1E6;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	public void setReached(boolean reached) {
		this.reached = reached;
		
		if(reached) {
			this.reachTime = System.currentTimeMillis();
		}
	}
}
