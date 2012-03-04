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
import android.view.View.OnTouchListener;
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
		
		public void add() {
		    populate();
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
        //mapView.setBuiltInZoomControls(true);
        
        initOverlay();
        initButtons();
        initListView();
    }

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	void initOverlay() {
		final CheckpointItemizedOverlay overlay = new CheckpointItemizedOverlay(this.getResources().getDrawable(R.drawable.androidmarker));
        
        final GestureDetector detector = new GestureDetector(this, new SimpleOnGestureListener() {
        	
        	@Override
        	public boolean onSingleTapUp(MotionEvent event) {
	    		List<Overlay> overlays = mapView.getOverlays();

	    		Map<String, Object> cp = new HashMap<String, Object>();
	    		cp.put("title", "Checkpoint " + (overlay.size() + 1)); 
	    		cp.put("desc", "");
	    		cp.put("overlayItem", new OverlayItem(mapView.getProjection().fromPixels((int)event.getX(), (int)event.getY()), (String)cp.get("title"), (String)cp.get("desc")));
	    		
	    		checkpoints.add(cp);
	    		overlay.add();
	    		((ListView) TheOrienteer.this.findViewById(R.id.point_list_listview)).setAdapter(adapter);
	    		
	    		if(!overlays.contains(overlay)) {
	    			overlays.add(overlay);
	    		}
	    		
	    		addMode = false;
	    		
	    		return false;
        	}
        });
        
        mapView.setOnTouchListener(new OnTouchListener() {
        	
        	public boolean onTouch(View v, MotionEvent event) {
		    	return detector.onTouchEvent(event);
			}
        });
	}
	
	void initButtons() {
        final View addButton = findViewById(R.id.point_list_add);
        final View deleteButton = findViewById(R.id.point_list_delete);
        
        addButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				addMode = true;
				deleteButton.setVisibility(View.GONE);
			}
        });
	}
	
	void initListView() {
		adapter = new SimpleAdapter(this, checkpoints, R.layout.list_item, new String[] { "title", "desc" }, new int[] { R.id.item_title, R.id.item_text });
		
		ListView list = (ListView) TheOrienteer.this.findViewById(R.id.point_list_listview);
		
		list.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				selectCheckpoint(arg2);
			}
			
		});
	}
	
	void selectCheckpoint(int index) {
		findViewById(R.id.point_info).setVisibility(View.VISIBLE);
		findViewById(R.id.point_list_delete).setEnabled(true);
		((TextView) findViewById(R.id.point_info_desc)).setText(((OverlayItem)checkpoints.get(index).get("overlayItem")).getPoint().toString());
	}
}
