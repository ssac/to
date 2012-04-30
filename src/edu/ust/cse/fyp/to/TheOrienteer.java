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

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class TheOrienteer extends MapActivity {

	int uid = 0;
	boolean addMode = false;
	List<Map<String, Object>> checkpoints = new ArrayList<Map<String, Object>>();
	MapView mapView;
	CheckpointItemizedOverlay overlay;
	MyLocationOverlay mlo;
	SimpleAdapter adapter;
	ListView list;
	Menu options;
	
	enum MenuMode {
		STOPPED, STARTED, ADDMODE, ADDED, SELECTED, DESELECTED
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        //mapView.getController().setCenter(new GeoPoint(22337505,114262968));
        mapView.getController().setZoom(18);
        
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
	
	class CheckpointItemizedOverlay extends ItemizedOverlay<OverlayItem> {

		public CheckpointItemizedOverlay(Drawable defaultMarker) {
			super(boundCenter(defaultMarker));
		}

		@Override
		protected OverlayItem createItem(int i) {
			return (OverlayItem) checkpoints.get(i).get("overlayItem");
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
	    		List<Overlay> overlays = mapView.getOverlays();

	    		Map<String, Object> cp = new HashMap<String, Object>();
	    		cp.put("title", "Checkpoint " + ++uid); 
	    		cp.put("desc", "");
	    		cp.put("overlayItem", new OverlayItem(mapView.getProjection().fromPixels((int)event.getX(), (int)event.getY()), (String)cp.get("title"), (String)cp.get("desc")));
	    		
	    		checkpoints.add(cp);
	    		overlay.update();
	    		list.setAdapter(adapter);
	    		
	    		if(!overlays.contains(overlay)) {
	    			overlays.add(overlay);
	    		}
	    		
	    		setMenuMode(MenuMode.ADDED);
	    		
	    		return false;
        	}
        });
        
        mapView.getOverlays().add(new Overlay() {

			@Override
			public boolean onTouchEvent(MotionEvent e, MapView mapView) {
				if(addMode) {
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
		adapter = new SimpleAdapter(this, checkpoints, R.layout.list_item, new String[] { "title" }, new int[] { R.id.item_title });
		list = (ListView) findViewById(R.id.point_list);
		
		list.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectCheckpoint(position);
			}
		});
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
	    		Location fix = mlo.getLastFix();
	    		if(fix != null) mapView.getController().animateTo(new GeoPoint((int)(fix.getLatitude() * 1E6), (int)(fix.getLongitude() * 1E6)));
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
				setMenuMode(MenuMode.STARTED);
	            return true;
	        case R.id.checkin:
	            return true;
	        case R.id.review:
	            return true;
	        case R.id.cfg_import:
	            return true;
	        case R.id.cfg_export:
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	int selectedIndex;
	void selectCheckpoint(int index) {
		selectedIndex = index;
		list.setItemChecked(index, true);
		
		Map<String, Object> item = checkpoints.get(index);

		((TextView) findViewById(R.id.point_info_title)).setText((String)item.get("title"));
		((TextView) findViewById(R.id.point_info_desc)).setText((String)item.get("desc"));
		
		setMenuMode(MenuMode.SELECTED);
	}
	
	void setMenuMode(MenuMode mode) {
		findViewById(R.id.point_info).setVisibility(EnumSet.of(MenuMode.DESELECTED, MenuMode.ADDMODE).contains(mode) ? View.INVISIBLE : View.VISIBLE);
		
		boolean isUserMode = EnumSet.of(MenuMode.STOPPED, MenuMode.STARTED).contains(mode);
		
    	options.setGroupVisible(R.id.admin_group, !isUserMode);
    	options.setGroupVisible(R.id.user_group, isUserMode);
    	
    	if(isUserMode) {
    		options.findItem(R.id.start).setVisible(mode == MenuMode.STOPPED);
    		options.findItem(R.id.checkin).setVisible(mode == MenuMode.STARTED);
    		options.findItem(R.id.review).setVisible(mode == MenuMode.STARTED);
    	}
    	else {
    		addMode = mode == MenuMode.ADDMODE;
    		options.findItem(R.id.add).setVisible(mode == MenuMode.DESELECTED);
    		options.findItem(R.id.OK).setVisible(mode == MenuMode.SELECTED || mode == MenuMode.ADDED);
    		options.findItem(R.id.cancel).setVisible(mode == MenuMode.ADDMODE && mode == MenuMode.SELECTED);
    		options.findItem(R.id.up).setVisible(mode == MenuMode.SELECTED || mode == MenuMode.ADDED);
    		options.findItem(R.id.down).setVisible(mode == MenuMode.SELECTED || mode == MenuMode.ADDED);
    		options.findItem(R.id.remove).setVisible(mode == MenuMode.SELECTED || mode == MenuMode.ADDED);
    	}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		builder.setTitle("Review");
		builder.setMessage("00:00:00   -   Start\n\n00:03:30   -   Control Point 1\n\n00:08:24   -   Control Point 2\n\n00:11:49   -   Control Point 3\n\n00:15:33   -   End");
		
		return builder.create();
	}
}
