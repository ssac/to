package edu.ust.cse.fyp.to;

import java.util.List;
import java.util.Map;

import android.R.color;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CheckpointListAdapter extends ArrayAdapter<Map<String, Object>> {
	TheOrienteer context;
	List<Map<String, Object>> items;
	
	public CheckpointListAdapter(Context context, List<Map<String, Object>> objects) {
		super(context, R.id.item_title, objects);
		this.context = (TheOrienteer) context;
		this.items = objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView == null) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.list_item, parent, false);
		}

		convertView.setBackgroundColor(position == context.getSelectedIndex() ? 0x14000000 : color.transparent); 
		
		((TextView) convertView.findViewById(R.id.item_title)).setText((String) items.get(position).get("title"));
		ImageView image = ((ImageView) convertView.findViewById(R.id.item_status));
		
		image.setImageDrawable(null);
		if(context.getMenuMode() == TheOrienteer.MenuMode.STARTED) {
			if(position < context.getNextIndex()) {
				image.setImageResource(android.R.drawable.ic_menu_myplaces);
			}
			else if(position == context.getNextIndex()) {
				image.setImageResource(android.R.drawable.ic_menu_compass);
			}
		}
		
		return convertView;
	}

}
