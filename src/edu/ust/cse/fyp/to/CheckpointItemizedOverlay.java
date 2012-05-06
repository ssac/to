package edu.ust.cse.fyp.to;

import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

import edu.ust.cse.fyp.to.TheOrienteer.MenuMode;

public class CheckpointItemizedOverlay extends ItemizedOverlay<OverlayItem> {
	TheOrienteer to;
	
	public CheckpointItemizedOverlay(TheOrienteer context, Drawable defaultMarker) {
		super(boundCenter(defaultMarker));
		this.to = context;
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		Checkpoint cp = to.getCheckpoints().get(i);
		OverlayItem item = new OverlayItem(cp.getPoint(), cp.getTitle(), cp.getDesc());
		item.setMarker(boundCenter(CheckpointDrawableFactory.create(to.getResources(), i + 1, to.getMenuMode() == MenuMode.STARTED && i == to.getNextIndex())));
		return item;
	}

	@Override
	public int size() {
		return to.getCheckpoints().size();
	}
	
	public void update() {
	    populate();
	    setLastFocusedIndex(-1);
	}

	@Override
	protected boolean onTap(int index) {
		super.onTap(index);
		to.setCheckpointSelected(index);						
		return true;
	}
}
