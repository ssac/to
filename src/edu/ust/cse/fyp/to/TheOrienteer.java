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
import java.util.List;

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

public class TheOrienteer extends MapActivity {
	
	MapView mapView;
	
	boolean addMode = false;
	
	ArrayList<OverlayItem> checkpoints = new ArrayList<OverlayItem>();

	public class CheckpointItemizedOverlay extends ItemizedOverlay<OverlayItem> {

		public CheckpointItemizedOverlay(Drawable defaultMarker) {
			super(boundCenterBottom(defaultMarker));
		}

		@Override
		protected OverlayItem createItem(int i) {
			return checkpoints.get(i);
		}

		@Override
		public int size() {
			return checkpoints.size();
		}
		
		public void add(OverlayItem overlay) {
			checkpoints.add(overlay);
		    populate();
		}

		@Override
		protected boolean onTap(int index) {
			return super.onTap(index);
		}
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        
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

	    		overlay.add(new OverlayItem(mapView.getProjection().fromPixels((int)event.getX(), (int)event.getY()), "test", "testing"));
	    		
	    		if(!overlays.contains(overlay)) {
	    			overlays.add(overlay);
	    		}
	    		
	    		addMode = false;
	    		
	    		return false;
        	}
        });
        
        mapView.setOnTouchListener(new OnTouchListener() {
        	
        	public boolean onTouch(View v, MotionEvent event) {
		    	return addMode && detector.onTouchEvent(event);
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
		
	}
}
