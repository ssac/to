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

import java.util.EnumSet;
import java.util.List;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class TheOrienteer extends MapActivity {
	private int selectedIndex = -1;
	private int nextIndex = -1;
	private List<Checkpoint> checkpoints;
	private MapView mapView;
	private CheckpointItemizedOverlay itemizedOverlay;
	private MyLocationOverlay myLocationOverlay;
	private CheckpointListAdapter pointListAdapter;
	private ListView pointListView;
	private Menu optionsMenu;
	private long startTime;
	
	public enum MenuMode {
		STOPPED, STARTED, ADDMODE, SELECTED, DESELECTED
	}
	

	private MenuMode menuMode = MenuMode.STOPPED;
	private boolean isUserMode = true;
	
	private final int DIALOG_REVIEW = 0;
	private final int DIALOG_LOGIN = 1;
	private final int DIALOG_SET_PASSWORD = 2;
	private final int DIALOG_IMPORT = 3;
	private final int DIALOG_EXPORT = 4;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        mapView.getController().setZoom(18);
        
		((TextView) findViewById(R.id.timer)).setText("00:00");
		
		checkpoints = ConfigManager.loadFromInternalStorage(this);
        
        initOverlays();
        initListView();
    }

	@Override
	protected void onPause() {
		super.onPause();
		
        myLocationOverlay.disableMyLocation();
        myLocationOverlay.disableCompass();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableCompass();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.options, menu);
	    optionsMenu = menu;
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Checkpoint cp;
	    switch (item.getItemId()) {
	    	case R.id.my_location:
	    		GeoPoint point = myLocationOverlay.getMyLocation();
	    		if(point == null) {
	    			Toast.makeText(getApplicationContext(), "Unable to retrieve your location.", Toast.LENGTH_SHORT).show();
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
				setMenuMode(MenuMode.DESELECTED);
				cp = checkpoints.get(selectedIndex);
				cp.setTitle(((TextView) findViewById(R.id.point_info_title)).getText().toString());
				cp.setDesc(((TextView) findViewById(R.id.point_info_desc)).getText().toString());
				
				notifyCheckpointsUpdated(true);
	            return true;
	        case R.id.cancel:
	        	setMenuMode(MenuMode.DESELECTED);
	        	setCheckpointSelected(-1);
	            return true;
	        case R.id.up:
	        	if(selectedIndex == 0) return true;
	        case R.id.down:
	        	if(item.getItemId() == R.id.down && selectedIndex == checkpoints.size() - 1) return true;
	        	
	        	cp = checkpoints.remove(selectedIndex);
	        	selectedIndex = item.getItemId() == R.id.up ? selectedIndex - 1 : selectedIndex + 1;
	        	checkpoints.add(selectedIndex, cp);
				notifyCheckpointsUpdated(true);
				setCheckpointSelected(selectedIndex);
	            return true;
	        case R.id.remove:
				setMenuMode(MenuMode.DESELECTED);
				checkpoints.remove(selectedIndex);
				setCheckpointSelected(-1);
				notifyCheckpointsUpdated(true);
	            return true;
	        case R.id.start:
	        	if(checkpoints.size() == 0) {
	        		Toast.makeText(getApplicationContext(), "Cannot start with 0 control points!", Toast.LENGTH_SHORT).show();
	        		return true;
	        	}
	        	
				setMenuMode(MenuMode.STARTED);
	        	
	        	nextIndex = 0;
	        	for(int i=0; i<checkpoints.size(); i++) {
	        		checkpoints.get(i).setReached(false);
	        	}
	        	notifyCheckpointsUpdated(false);
	        	
				setTimer(true);
	            return true;
	        case R.id.checkin:
	        	Location curCp = myLocationOverlay.getLastFix();
	        	
	        	if(curCp == null) {
	    			Toast.makeText(getApplicationContext(), "Unable to retrieve your location.", Toast.LENGTH_SHORT).show();
	    			return true;
	        	}

	        	float[] dist = new float[1];
	        	Checkpoint nextCp = checkpoints.get(nextIndex);
	        	Location.distanceBetween(curCp.getLatitude(), curCp.getLongitude(), nextCp.getLatitude(), nextCp.getLongitude(), dist);
	        	
	        	String text;
	        	if(dist[0] < 10) {
	        		text = "Check-in success!";
	        		checkpoints.get(nextIndex).setReached(true);
	        		nextIndex++;
	        		notifyCheckpointsUpdated(false);
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
	        case R.id.set_password:
	        	showDialog(DIALOG_SET_PASSWORD);
	            return true;
	        case R.id.cfg_import:
	        	showDialog(DIALOG_IMPORT);
	            return true;
	        case R.id.cfg_export:
	        	showDialog(DIALOG_EXPORT);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	protected Dialog onCreateDialog(final int id) {
		Dialog dialog = null;
		LayoutInflater inflater = getLayoutInflater();
		
		switch(id) {
			case DIALOG_REVIEW:
				dialog = new AlertDialog.Builder(this)
					.setView(inflater.inflate(R.layout.review, null))
					.setTitle(nextIndex == checkpoints.size() ? "Game Complete!" : "Review")
					.setPositiveButton("OK", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {}
					})
					.create();
				break;
			case DIALOG_LOGIN:
			case DIALOG_SET_PASSWORD:
				final View loginView = inflater.inflate(R.layout.login, null);
				dialog = new AlertDialog.Builder(this)
					.setView(loginView)
					.setTitle("Enter password:")
					.setPositiveButton("OK", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							EditText editText = (EditText)loginView.findViewById(R.id.password);
							String password = editText.getText().toString();
							SharedPreferences pref = getPreferences(MODE_PRIVATE);
							
							if(id == DIALOG_LOGIN) {
								if(pref.getString("password", null).equals(password)) {
									setMenuMode(MenuMode.DESELECTED, true);
								}
								else {
									editText.setText("");
									Toast.makeText(getApplicationContext(), "Password incorrect!", Toast.LENGTH_SHORT).show();
								}
							}
							else {
								Editor editor = TheOrienteer.this.getPreferences(MODE_PRIVATE).edit();
								if(password.isEmpty()) {
									editor.remove("password");
								}
								else {
									editor.putString("password", password);
								}
								editor.commit();
							}
						}
					})
					.setNegativeButton("Cancel", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {}
					})
					.create();
				break;
			case DIALOG_IMPORT:
				final View importView = inflater.inflate(R.layout.cfg_import, null);
				final ListView importList = (ListView)importView.findViewById(R.id.import_list);
				
				importList.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						try {
							checkpoints.clear();
							checkpoints.addAll(ConfigManager.loadFromExternalStorage((String)importList.getAdapter().getItem(position)));
							notifyCheckpointsUpdated(true);
							Toast.makeText(getApplicationContext(), "Import success!", Toast.LENGTH_SHORT).show();
						}
						catch(RuntimeException e) {
							Toast.makeText(getApplicationContext(), "Import failed! Error: ".concat(e.getMessage()), Toast.LENGTH_LONG).show();
						}
					}
				});
				
				dialog = new AlertDialog.Builder(this)
					.setView(importView)
					.setTitle("Import Config")
					.setNegativeButton("Cancel", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {}
					})
					.create();
				
				break;
			case DIALOG_EXPORT:
				final View exportView = inflater.inflate(R.layout.cfg_export, null);
				dialog = new AlertDialog.Builder(this)
					.setView(exportView)
					.setTitle("Export Config")
					.setPositiveButton("OK", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							EditText editText = (EditText)exportView.findViewById(R.id.export_file);
							String filename = editText.getText().toString();
							if(filename.isEmpty()) {
								Toast.makeText(getApplicationContext(), "Config name cannot be empty!", Toast.LENGTH_SHORT).show();
							}
							else {
								try {
									String path = ConfigManager.saveToExternalStorage(filename, checkpoints);
									Toast.makeText(getApplicationContext(), "Exported successfully to ".concat(path), Toast.LENGTH_LONG).show();
								}
								catch(RuntimeException e) {
									Toast.makeText(getApplicationContext(), "Export failed! Error: ".concat(e.getMessage()), Toast.LENGTH_LONG).show();
								}
							}
						}
					})
					.setNegativeButton("Cancel", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {}
					})
					.create();
				break;
		}
		
		return dialog;
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		
		switch(id) {
			case DIALOG_REVIEW:
				TableLayout table = (TableLayout) dialog.findViewById(R.id.review_table);
				table.removeViews(1, table.getChildCount() - 1);
				
				long lastReachTime = startTime;
				for(int i=0; i<checkpoints.size(); i++) {
					Checkpoint cp = checkpoints.get(i);
					
					if(!cp.isReached()) break;
					
					long reachTime = cp.getReachedTime();
					
					TableRow row = (TableRow) getLayoutInflater().inflate(R.layout.review_row, null);
					System.out.println(row.getVirtualChildCount());
					for(int j=0; j<3; j++) {
						TextView tv = (TextView) row.getVirtualChildAt(j);
						switch(j) {
							case 0:
								tv.setText(cp.getTitle());
								break;
							case 1:
								tv.setText(DateUtils.formatElapsedTime((reachTime - startTime) / 1000));
								break;
							case 2:
								tv.setText(DateUtils.formatElapsedTime((reachTime - lastReachTime) / 1000));
								break;
						}
					}
					table.addView(row);
					
					lastReachTime = reachTime;
				}
				break;
			case DIALOG_LOGIN:
			case DIALOG_SET_PASSWORD:
				((EditText)dialog.findViewById(R.id.password)).setText("");
				break;
			case DIALOG_IMPORT:
				((ListView)dialog.findViewById(R.id.import_list)).setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, ConfigManager.list()));
				break;
			case DIALOG_EXPORT:
				((EditText)dialog.findViewById(R.id.export_file)).setText("");
				break;
		}
	}
	
	private void initOverlays() {
		List<Overlay> overlays = mapView.getOverlays();
		
		itemizedOverlay = new CheckpointItemizedOverlay(this, CheckpointDrawableFactory.create(getResources(), -1, false));
		overlays.add(itemizedOverlay);
        
        myLocationOverlay = new MyLocationOverlay(this, mapView);
        myLocationOverlay.runOnFirstFix(new Runnable() {
			public void run() {
				mapView.getController().animateTo(myLocationOverlay.getMyLocation());
			}
        });
        overlays.add(myLocationOverlay);
        
		overlays.add(new Overlay() {
        	private GestureDetector detector = new GestureDetector(TheOrienteer.this, new SimpleOnGestureListener() {
            	@Override
            	public boolean onSingleTapUp(MotionEvent event) {
            		addCheckpoint(mapView.getProjection().fromPixels((int)event.getX(), (int)event.getY()));
    	    		return true;
            	}
            });

			@Override
			public boolean onTouchEvent(MotionEvent e, MapView mapView) {
				if(menuMode == MenuMode.ADDMODE) {
					detector.onTouchEvent(e);
				}
				
				return super.onTouchEvent(e, mapView);
			}
        });
	}
	
	private void initListView() {
		pointListView = (ListView) findViewById(R.id.point_list);
		
		pointListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				setCheckpointSelected(position);
			}
		});

		pointListAdapter = new CheckpointListAdapter(this, checkpoints);
		pointListView.setAdapter(pointListAdapter);
	}
	
	private void addCheckpoint(GeoPoint point) {
		checkpoints.add(new Checkpoint(point));
		notifyCheckpointsUpdated(true);
	}
	
	public void notifyCheckpointsUpdated(boolean isCheckpointsModified) {
		if(isCheckpointsModified) {
			ConfigManager.saveToInternalStorage(this, checkpoints);
		}

		itemizedOverlay.update();
	    mapView.invalidate();
		pointListAdapter.notifyDataSetChanged();
	}
	
	public void setCheckpointSelected(int index) {
		selectedIndex = index;
		notifyCheckpointsUpdated(false);
		
		if(index != -1) {
			Checkpoint cp = checkpoints.get(index);
			
			mapView.getController().animateTo(cp.getPoint());
	
			((TextView) findViewById(R.id.point_info_title)).setText(cp.getTitle());
			((TextView) findViewById(R.id.point_info_desc)).setText(cp.getDesc());
	
			findViewById(R.id.point_info).setVisibility(View.VISIBLE);
			
			if(!isUserMode) setMenuMode(MenuMode.SELECTED);
		}
	}
	
	private void setMenuMode(MenuMode mode) {
		setMenuMode(mode, false);
	}
	
	private void setMenuMode(MenuMode mode, boolean bypass) {
		boolean isLastUserMode = isUserMode;
		isUserMode = EnumSet.of(MenuMode.STOPPED, MenuMode.STARTED).contains(mode);

		if(!bypass && !isUserMode && isUserMode != isLastUserMode && getPreferences(MODE_PRIVATE).contains("password")) {
			isUserMode = isLastUserMode;
			showDialog(DIALOG_LOGIN);
			return;
		}
		
		if(menuMode == MenuMode.STARTED) {
			setTimer(false);
		}
		
		menuMode = mode;
		
    	optionsMenu.setGroupVisible(R.id.admin_group, !isUserMode);
    	optionsMenu.setGroupVisible(R.id.user_group, isUserMode);
    	
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
	    	
	    	setCheckpointSelected(-1);
    	}
    	
    	if(isUserMode) {
    		optionsMenu.findItem(R.id.start).setVisible(mode == MenuMode.STOPPED);
    		optionsMenu.findItem(R.id.checkin).setVisible(mode == MenuMode.STARTED);
    	}
    	else {
    		optionsMenu.findItem(R.id.add).setVisible(mode == MenuMode.DESELECTED);
    		optionsMenu.findItem(R.id.OK).setVisible(mode == MenuMode.SELECTED);
    		optionsMenu.findItem(R.id.cancel).setVisible(mode == MenuMode.ADDMODE || mode == MenuMode.SELECTED);
    		optionsMenu.findItem(R.id.up).setVisible(mode == MenuMode.SELECTED);
    		optionsMenu.findItem(R.id.down).setVisible(mode == MenuMode.SELECTED);
    		optionsMenu.findItem(R.id.remove).setVisible(mode == MenuMode.SELECTED);
    	}
	}
	
	private Handler timerHandler = new Handler();
	private Runnable timerRunner = new Runnable() {
		public void run() {
			((TextView) findViewById(R.id.timer)).setText(DateUtils.formatElapsedTime((System.currentTimeMillis() - startTime) / 1000));
			timerHandler.postDelayed(this, 1000);
		}
	};
	
	private void setTimer(boolean enabled) {
		timerHandler.removeCallbacks(timerRunner);
		
		if(enabled) {
			startTime = System.currentTimeMillis();
			timerHandler.post(timerRunner);
		}
	}
	
	public List<Checkpoint> getCheckpoints() {
		return checkpoints;
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
		return menuMode;
	}
}
