package edu.ust.cse.fyp.to;

import java.util.List;
import android.R.color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CheckpointListAdapter extends ArrayAdapter<Checkpoint> {
	TheOrienteer to;
	
	public CheckpointListAdapter(TheOrienteer context, List<Checkpoint> checkpoints) {
		super(context, R.id.item_title, checkpoints);
		this.to = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView == null) {
			convertView = to.getLayoutInflater().inflate(R.layout.list_item, parent, false);
		}

		convertView.setBackgroundColor(position == to.getSelectedIndex() ? 0x14000000 : color.transparent); 
		
		((TextView) convertView.findViewById(R.id.item_title)).setText((String) to.getCheckpoints().get(position).getTitle());
		ImageView image = ((ImageView) convertView.findViewById(R.id.item_status));
		
		image.setImageDrawable(null);
		if(to.getMenuMode() == TheOrienteer.MenuMode.STARTED) {
			if(position < to.getNextIndex()) {
				image.setImageResource(android.R.drawable.ic_menu_myplaces);
			}
			else if(position == to.getNextIndex()) {
				image.setImageResource(android.R.drawable.ic_menu_compass);
			}
		}
		
		return convertView;
	}

}
