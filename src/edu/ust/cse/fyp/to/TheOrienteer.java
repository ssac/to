/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.ust.cse.fyp.to;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class TheOrienteer extends MapActivity {

	int uid = 0;
	int selectedIndex = 0;
	int nextIndex = 0;
	List<Map<String, Object>> checkpoints = new ArrayList<Map<String, Object>>();
	MapView mapView;
	CheckpointItemizedOverlay overlay;
	MyLocationOverlay mlo;
	CheckpointListAdapter adapter;
	ListView list;
	Menu options;
	long startTime;
	
	enum MenuMode {
		STOPPED, STARTED, ADDMODE, SELECTED, DESELECTED
	}
	
	MenuMode currentMode = MenuMode.STOPPED;
	boolean isUserMode = true;
	
	final int DIALOG_REVIEW = 0;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        mapView.getController().setZoom(18);
        
		((TextView) findViewById(R.id.timer)).setText("00:00");
        
        initOverlay();
        initListView();
    }

	@Override
	protected void onPause() {
		super.onPause();
		
        mlo.disableMyLocation();
        mlo.disableCompass();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
        mlo.enableMyLocation();
        mlo.enableCompass();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.options, menu);
	    options = menu;
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    	case R.id.my_location:
	    		GeoPoint point = mlo.getMyLocation();
	    		if(point == null) {
	    			Toast.makeText(getApplicationContext(), "Unable to retrieve your location.", Toast.LENGTH_SHORT);
	    		}
	    		else {
	    			mapView.getController().animateTo(point);
	    		}
	    		return true;
	        case R.id.admin_mode:
	        	setMenuMode(MenuMode.DESELECTED);
	            return true;
	        case R.id.user_mode:
	        	setMenuMode(MenuMode.STOPPED);
	            return true;
	        case R.id.add:
	        	setMenuMode(MenuMode.ADDMODE);
	            return true;
	        case R.id.OK:
				Map<String, Object> cpInfo = checkpoints.get(selectedIndex);
				
				cpInfo.put("title", ((TextView) findViewById(R.id.point_info_title)).getText().toString());
				cpInfo.put("desc", ((TextView) findViewById(R.id.point_info_desc)).getText().toString());
				
				list.setAdapter(adapter);
				setMenuMode(MenuMode.DESELECTED);
	            return true;
	        case R.id.cancel:
				list.setAdapter(adapter);
	        	setMenuMode(MenuMode.DESELECTED);
	            return true;
	        case R.id.up:
	        	if(selectedIndex == 0) return true;
	        case R.id.down:
	        	if(item.getItemId() == R.id.down && selectedIndex == checkpoints.size() - 1) return true;
	        	
	        	Map<String, Object> cp = checkpoints.remove(selectedIndex);
	        	selectedIndex = item.getItemId() == R.id.up ? selectedIndex - 1 : selectedIndex + 1;
	        	checkpoints.add(selectedIndex, cp);
	        	overlay.update();
				list.setAdapter(adapter);
				selectCheckpoint(selectedIndex);
	            return true;
	        case R.id.remove:
				checkpoints.remove(selectedIndex);
				overlay.update();
				list.setAdapter(adapter);
				setMenuMode(MenuMode.DESELECTED);
	            return true;
	        case R.id.start:
	        	if(checkpoints.size() == 0) {
	        		Toast.makeText(getApplicationContext(), "Cannot start with 0 control points!", Toast.LENGTH_SHORT).show();
	        		return true;
	        	}
	        	
	        	nextIndex = 0;
	        	for(int i=0; i<checkpoints.size(); i++) {
	        		checkpoints.get(i).remove("reachTime");
	        	}
				setTimer(true);
				setMenuMode(MenuMode.STARTED);
	            return true;
	        case R.id.checkin:
	        	Location cur = mlo.getLastFix();
	        	
	        	if(cur == null) {
	    			Toast.makeText(getApplicationContext(), "Unable to retrieve your location.", Toast.LENGTH_SHORT).show();
	    			return true;
	        	}

	        	float[] dist = new float[1];
	        	GeoPoint next = ((OverlayItem) checkpoints.get(nextIndex).get("overlayItem")).getPoint();
	        	Location.distanceBetween(cur.getLatitude(), cur.getLongitude(), (double)next.getLatitudeE6()/1E6, (double)next.getLongitudeE6()/1E6, dist);
	        	
	        	String text;
	        	if(dist[0] < 10) {
	        		text = "Check-in success!";
	        		checkpoints.get(nextIndex).put("reachTime", System.currentTimeMillis());
	        		nextIndex++;
	        		list.setAdapter(adapter);
	        	}
	        	else {
	        		text = "Check-in failed! Please make sure you are close enough to the next control point.";
	        	}
	        	Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
	        	
	        	if(nextIndex == checkpoints.size()) {
		        	showDialog(DIALOG_REVIEW);
	        		setMenuMode(MenuMode.STOPPED);
	        	}
	        	
	            return true;
	        case R.id.review:
	        	showDialog(DIALOG_REVIEW);
	            return true;
	        case R.id.cfg_import:
	            return true;
	        case R.id.cfg_export:
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		
		switch(id) {
			case DIALOG_REVIEW:
				dialog = new Dialog(this);
				dialog.setContentView(R.layout.review);
				dialog.setTitle(nextIndex == checkpoints.size() ? "Game Complete!" : "Review");
				TableLayout table = (TableLayout) dialog.findViewById(R.id.review_table);
				
				LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
				long lastReachTime = 0;
				for(int i=0; i<checkpoints.size(); i++) {
					Map<String, Object> cp = checkpoints.get(i);
					
					if(cp.get("reachTime") == null) break;
					
					long reachTime = (Long)cp.get("reachTime");
					
					TableRow row = (TableRow) inflater.inflate(R.layout.review_row, table);
					for(int j=0; j<3; j++) {
						TextView tv = (TextView) row.getVirtualChildAt(0);
						switch(j) {
							case 0:
								tv.setText((String)cp.get("title"));
								break;
							case 1:
								tv.setText(DateUtils.formatElapsedTime(reachTime - startTime));
								break;
							case 2:
								tv.setText(DateUtils.formatElapsedTime(reachTime - lastReachTime));
								break;
						}
					}
					table.addView(row);
					
					lastReachTime = reachTime;
				}
				
				break;
		}
		
		return dialog;
	}
	
	class CheckpointItemizedOverlay extends ItemizedOverlay<OverlayItem> {

		public CheckpointItemizedOverlay(Drawable defaultMarker) {
			super(boundCenter(defaultMarker));
		}

		@Override
		protected OverlayItem createItem(int i) {
			OverlayItem item = (OverlayItem) checkpoints.get(i).get("overlayItem");
			item.setMarker(boundCenter(CheckpointDrawableFactory.create(getResources(), i + 1, false)));
			return item;
		}

		@Override
		public int size() {
			return checkpoints.size();
		}
		
		public void update() {
		    populate();
		    setLastFocusedIndex(-1);
		    mapView.invalidate();
		}

		@Override
		protected boolean onTap(int index) {
			super.onTap(index);
			selectCheckpoint(index);						
			return true;
		}
	}
	
	void initOverlay() {
		overlay = new CheckpointItemizedOverlay(this.getResources().getDrawable(R.drawable.androidmarker));
        
        final GestureDetector detector = new GestureDetector(this, new SimpleOnGestureListener() {
        	
        	@Override
        	public boolean onSingleTapUp(MotionEvent event) {
        		addCheckpoint(mapView.getProjection().fromPixels((int)event.getX(), (int)event.getY()));
	    		return true;
        	}
        });
        
        mapView.getOverlays().add(new Overlay() {

			@Override
			public boolean onTouchEvent(MotionEvent e, MapView mapView) {
				if(currentMode == MenuMode.ADDMODE) {
					detector.onTouchEvent(e);
				}
				
				return super.onTouchEvent(e, mapView);
			}
        });
        
        mlo = new MyLocationOverlay(this, mapView);
        mlo.runOnFirstFix(new Runnable() {
			public void run() {
				mapView.getController().animateTo(mlo.getMyLocation());
			}
        });
        mapView.getOverlays().add(mlo);
	}
	
	void initListView() {
		adapter = new CheckpointListAdapter(this, checkpoints);
		list = (ListView) findViewById(R.id.point_list);
		
		list.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectCheckpoint(position);
			}
		});
	}
	
	void addCheckpoint(GeoPoint point) {
		List<Overlay> overlays = mapView.getOverlays();

		Map<String, Object> cp = new HashMap<String, Object>();
		cp.put("title", "Checkpoint " + ++uid);
		cp.put("desc", "");
		cp.put("overlayItem", new OverlayItem(point, "", ""));
		
		checkpoints.add(cp);
		overlay.update();
		list.setAdapter(adapter);
		
		if(!overlays.contains(overlay)) {
			overlays.add(overlay);
		}
	}
	
	void selectCheckpoint(int index) {
		selectedIndex = index;
		list.setItemChecked(index, true);
		list.setSelection(index);
		
		Map<String, Object> item = checkpoints.get(index);

		((TextView) findViewById(R.id.point_info_title)).setText((String)item.get("title"));
		((TextView) findViewById(R.id.point_info_desc)).setText((String)item.get("desc"));

		findViewById(R.id.point_info).setVisibility(View.VISIBLE);
		
		if(!isUserMode) setMenuMode(MenuMode.SELECTED);
	}
	
	void setMenuMode(MenuMode mode) {
		currentMode = mode;
		boolean isLastUserMode = isUserMode;
		isUserMode = EnumSet.of(MenuMode.STOPPED, MenuMode.STARTED).contains(mode);
		
    	options.setGroupVisible(R.id.admin_group, !isUserMode);
    	options.setGroupVisible(R.id.user_group, isUserMode);
    	
    	findViewById(R.id.timer_container).setVisibility(isUserMode ? View.VISIBLE : View.GONE);
		
    	if(EnumSet.of(MenuMode.STOPPED, MenuMode.ADDMODE, MenuMode.DESELECTED).contains(mode)) {
    		findViewById(R.id.point_info).setVisibility(View.INVISIBLE);
    	}
    	
    	if(isLastUserMode != isUserMode) {
	    	ViewGroup info = (ViewGroup) findViewById(R.id.point_info);
	    	for(int i=0; i<info.getChildCount(); i++) {
	    		View view = info.getChildAt(i);
	    		if(view instanceof EditText) {
	    			if(isUserMode) {
	    				view.setFocusable(false);
	    			}
	    			else {
	    				view.setFocusableInTouchMode(true);
	    			}
	    		}
	    	}
    	}
    	
    	if(isUserMode) {
    		options.findItem(R.id.start).setVisible(mode == MenuMode.STOPPED);
    		options.findItem(R.id.checkin).setVisible(mode == MenuMode.STARTED);
    		options.findItem(R.id.review).setVisible(mode == MenuMode.STARTED);
    	}
    	else {
    		options.findItem(R.id.add).setVisible(mode == MenuMode.DESELECTED);
    		options.findItem(R.id.OK).setVisible(mode == MenuMode.SELECTED);
    		options.findItem(R.id.cancel).setVisible(mode == MenuMode.ADDMODE || mode == MenuMode.SELECTED);
    		options.findItem(R.id.up).setVisible(mode == MenuMode.SELECTED);
    		options.findItem(R.id.down).setVisible(mode == MenuMode.SELECTED);
    		options.findItem(R.id.remove).setVisible(mode == MenuMode.SELECTED);
    	}
	}
	
	Handler timerHandler = new Handler();
	Runnable timerRunner = new Runnable() {
		public void run() {
			((TextView) findViewById(R.id.timer)).setText(DateUtils.formatElapsedTime((System.currentTimeMillis() - startTime) / 1000));
			timerHandler.postDelayed(this, 1000);
		}
	};
	
	void setTimer(boolean enabled) {
		timerHandler.removeCallbacks(timerRunner);
		((TextView) findViewById(R.id.timer)).setText("00:00");
		
		if(enabled) {
			startTime = System.currentTimeMillis();
			timerHandler.postDelayed(timerRunner, 1000);
		}
	}
	
	public boolean isUserMode() {
		return isUserMode;
	}
	
	public int getNextIndex() {
		return nextIndex;
	}
	
	public int getSelectedIndex() {
		return selectedIndex;
	}
	
	public MenuMode getMenuMode() {
		return currentMode;
	}
}
