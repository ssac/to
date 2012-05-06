package edu.ust.cse.fyp.to;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.os.Environment;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.util.Xml;

public class ConfigManager {
	private static URI storageURI = Environment.getExternalStorageDirectory().toURI().resolve("TheOrienteer/");
	
	public static String[] list() {
		File dir = new File(storageURI);
		String[] names = dir.list(new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".to");
			}
		});

		return names == null ? new String[0] : names;
	}
	
	public static String saveToExternalStorage(String filename, List<Checkpoint> checkpoints) {
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			throw new RuntimeException("Filesystem is currently unavailable.");
		}
		
		try {
			File dir = new File(storageURI);
			dir.mkdir();
			File file = new File(dir, filename.concat(".to"));
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(toXml(checkpoints).getBytes());
			fos.close();
			return file.getPath();
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static List<Checkpoint> loadFromExternalStorage(String filename) {
		List<Checkpoint> checkpoints;
		
		if(!(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) || Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY))) {
			throw new RuntimeException("Filesystem is currently unavailable.");
		}
		
		try {
			FileInputStream fis = new FileInputStream(new File(storageURI.resolve(filename)));
			checkpoints = parse(fis);
			fis.close();
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		return checkpoints;
	}
	
	public static void saveToInternalStorage(Context context, List<Checkpoint> checkpoints) {
		try {
			FileOutputStream fos = context.openFileOutput("conf.to", Context.MODE_PRIVATE);
			fos.write(toXml(checkpoints).getBytes());
			fos.close();
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static List<Checkpoint> loadFromInternalStorage(Context context) {
		List<Checkpoint> checkpoints;
		
		try {
			FileInputStream fis = context.openFileInput("conf.to");
			checkpoints = parse(fis);
			fis.close();
		}
		catch(FileNotFoundException e) {
			checkpoints = new ArrayList<Checkpoint>();
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		return checkpoints;
	}
	
	private static List<Checkpoint> parse(InputStream in) {
		RootElement config = new RootElement("to");
		
		Element checkpoint = config.getChild("checkpoints").getChild("checkpoint");
		final List<Checkpoint> checkpoints = new ArrayList<Checkpoint>(); 
		final Checkpoint cp = new Checkpoint(0, 0);
		
		checkpoint.setEndElementListener(new EndElementListener() {
			public void end() {
				checkpoints.add(new Checkpoint(cp));
			}
		});
		
		checkpoint.getChild("title").setEndTextElementListener(new EndTextElementListener() {
			public void end(String body) {
				cp.setTitle(body);
			}
		});
		
		checkpoint.getChild("desc").setEndTextElementListener(new EndTextElementListener() {
			public void end(String body) {
				cp.setDesc(body);
			}
		});
		
		checkpoint.getChild("latitude").setEndTextElementListener(new EndTextElementListener() {
			public void end(String body) {
				cp.setLatitude(Double.parseDouble(body));
			}
		});
		
		checkpoint.getChild("longtitude").setEndTextElementListener(new EndTextElementListener() {
			public void end(String body) {
				cp.setLongitude(Double.parseDouble(body));
			}
		});
		
		try {
			Xml.parse(in, Xml.Encoding.UTF_8, config.getContentHandler());
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		return checkpoints;
	}
	
	private static String toXml(List<Checkpoint> checkpoints) {
		XmlSerializer serializer = Xml.newSerializer();
	    StringWriter writer = new StringWriter();
	    
        try {
			serializer.setOutput(writer);
	        serializer.startDocument("UTF-8", true);
	        
	        serializer.startTag("", "to");
	        
	    	serializer.startTag("", "checkpoints");
	        for (Checkpoint cp: checkpoints) {
	        	serializer.startTag("", "checkpoint");
	        	
	        	serializer.startTag("", "title");
	        	serializer.text(cp.getTitle());
	        	serializer.endTag("", "title");
	        	
	        	serializer.startTag("", "desc");
	        	serializer.text(cp.getDesc());
	        	serializer.endTag("", "desc");
	        	
	        	serializer.startTag("", "latitude");
	        	serializer.text(cp.getLatitude() + "");
	        	serializer.endTag("", "latitude");
	        	
	        	serializer.startTag("", "longtitude");
	        	serializer.text(cp.getLongitude() + "");
	        	serializer.endTag("", "longtitude");
	        	
	        	serializer.endTag("", "checkpoint");
	        }
	        serializer.endTag("", "checkpoints");
	        
	        serializer.endTag("", "to");
	        
	        serializer.endDocument();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
        
        return writer.toString();
	}
}
