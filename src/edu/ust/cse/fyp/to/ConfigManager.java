package edu.ust.cse.fyp.to;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;

public class ConfigManager {
		
	
	public List<Map<String, Object>> parse(InputStream in) throws IOException, SAXException {
		RootElement config = new RootElement("config");
		
		Element checkpoint = config.getChild("checkpoints").getChild("checkpoint");
		final List<Map<String, Object>> checkpoints = new ArrayList<Map<String, Object>>(); 
		final Map<String, Integer> pos = new HashMap<String, Integer>();
		
		checkpoint.setStartElementListener(new StartElementListener() {
			public void start(Attributes attributes) {
				checkpoints.add(new HashMap<String, Object>());
			}
		});
		
		checkpoint.setEndElementListener(new EndElementListener() {
			public void end() {
				checkpoints.get(checkpoints.size() - 1).put("overlayItem", new OverlayItem(new GeoPoint(pos.get("latitude"), pos.get("longtitude")), "", ""));
			}
		});
		
		checkpoint.getChild("title").setEndTextElementListener(new EndTextElementListener() {
			public void end(String body) {
				checkpoints.get(checkpoints.size() - 1).put("title", body);
			}
		});
		
		checkpoint.getChild("desc").setEndTextElementListener(new EndTextElementListener() {
			public void end(String body) {
				checkpoints.get(checkpoints.size() - 1).put("desc", body);
			}
		});
		
		checkpoint.getChild("latitude").setEndTextElementListener(new EndTextElementListener() {
			public void end(String body) {
				pos.put("latitude", Integer.parseInt(body));
			}
		});
		
		checkpoint.getChild("longtitude").setEndTextElementListener(new EndTextElementListener() {
			public void end(String body) {
				pos.put("longtitude", Integer.parseInt(body));
			}
		});
		
		Xml.parse(in, Xml.Encoding.UTF_8, config.getContentHandler());
		
		return checkpoints;
	}
	
	public String toXml(List<Map<String, Object>> checkpoints) throws IllegalArgumentException, IllegalStateException, IOException {
		XmlSerializer serializer = Xml.newSerializer();
	    StringWriter writer = new StringWriter();
	    
        serializer.setOutput(writer);
        serializer.startDocument("UTF-8", true);
        
        serializer.startTag("", "config");
        
    	serializer.startTag("", "checkpoints");
        for (Map<String, Object> cp: checkpoints) {
        	serializer.startTag("", "checkpoint");
        	
        	serializer.startTag("", "title");
        	serializer.text((String) cp.get("title"));
        	serializer.endTag("", "title");
        	
        	serializer.startTag("", "desc");
        	serializer.text((String) cp.get("desc"));
        	serializer.endTag("", "desc");
        	
        	GeoPoint point = ((OverlayItem) cp.get("overlayItem")).getPoint();
        	
        	serializer.startTag("", "latitude");
        	serializer.text(point.getLatitudeE6() + "");
        	serializer.endTag("", "latitude");
        	
        	serializer.startTag("", "longtitude");
        	serializer.text(point.getLongitudeE6() + "");
        	serializer.endTag("", "longtitude");
        	
        	serializer.endTag("", "checkpoint");
        }
        serializer.endTag("", "checkpoints");
        
        serializer.endTag("", "config");
        
        serializer.endDocument();
        return writer.toString();
	}
}
