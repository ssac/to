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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class TheOrienteer extends MapActivity {
	
	MapView mapView;
	
	boolean addMode = false;
	
	List<Map<String, Object>> checkpoints = new ArrayList<Map<String, Object>>();
	
	SimpleAdapter adapter;

	public class CheckpointItemizedOverlay extends ItemizedOverlay<OverlayItem> {

		public CheckpointItemizedOverlay(Drawable defaultMarker) {
			super(boundCenterBottom(defaultMarker));
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
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        mapView.getController().setCenter(new GeoPoint(22337505,114262968));
        mapView.getController().setZoom(18);
        
        initOverlay();
        initButtons();
        initListView();
    }

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	CheckpointItemizedOverlay overlay;
	int uid = 0;
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
	    		
	    		addMode = false;
	    		
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
	}
	
	void initButtons() {
        final View buttons = findViewById(R.id.point_list_buttons);
        final View addButton = findViewById(R.id.point_list_add);
        final View deleteButton = findViewById(R.id.point_list_delete);
        final View saveButton = findViewById(R.id.point_list_save);
        
        addButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				addMode = true;
				buttons.setVisibility(View.INVISIBLE);
			}
        });
        
        deleteButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				checkpoints.remove(selectedIndex);
				overlay.update();
				list.setAdapter(adapter);
				
				findViewById(R.id.point_info).setVisibility(View.INVISIBLE);
				buttons.setVisibility(View.VISIBLE);
				addButton.setVisibility(View.VISIBLE);
				deleteButton.setVisibility(View.GONE);
				saveButton.setVisibility(View.GONE);
			}
        });
        
        saveButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				Map<String, Object> item = checkpoints.get(selectedIndex);
				
				item.put("title", ((TextView) findViewById(R.id.point_info_title_content)).getText().toString());
				item.put("desc", ((TextView) findViewById(R.id.point_info_desc_content)).getText().toString());
				
				list.setAdapter(adapter);
			}
        });
	}
	
	ListView list;
	
	void initListView() {
		adapter = new SimpleAdapter(this, checkpoints, R.layout.list_item, new String[] { "title" }, new int[] { R.id.item_title });
		
		list = (ListView) findViewById(R.id.point_list_listview);
		
		list.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				selectCheckpoint(arg2);
			}
			
		});
	}
	
	int selectedIndex;
	void selectCheckpoint(int index) {
		selectedIndex = index;
		findViewById(R.id.point_info).setVisibility(View.VISIBLE);
		findViewById(R.id.point_list_buttons).setVisibility(View.VISIBLE);
		findViewById(R.id.point_list_add).setVisibility(View.VISIBLE);
		findViewById(R.id.point_list_delete).setVisibility(View.VISIBLE);
		findViewById(R.id.point_list_save).setVisibility(View.VISIBLE);
		
		Map<String, Object> item = checkpoints.get(index);
		GeoPoint point = ((OverlayItem)item.get("overlayItem")).getPoint();

		((TextView) findViewById(R.id.point_info_title_content)).setText((String)item.get("title"));
		((TextView) findViewById(R.id.point_info_coordinate_content)).setText(point.getLatitudeE6() + ", " + point.getLongitudeE6());
		((TextView) findViewById(R.id.point_info_desc_content)).setText((String)item.get("desc"));
	}
}
