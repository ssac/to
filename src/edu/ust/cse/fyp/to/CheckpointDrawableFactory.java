package edu.ust.cse.fyp.to;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;

public class CheckpointDrawableFactory {
	public static BitmapDrawable create(Resources res, int num, boolean current) {
		Bitmap b = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(b);
		Paint p = new Paint();

		p.setAntiAlias(true);
		p.setStyle(Paint.Style.FILL);
		p.setColor(Color.BLACK);
		c.drawCircle(30, 30, 15, p);
		
		p.setColor(current ? 0xFFCC3535 : 0xFF35CC49);
		c.drawCircle(30, 30, 13, p);
		
		if(num != -1) {
			p.setTextSize(16);
			p.setFakeBoldText(true);
			p.setTextAlign(Paint.Align.CENTER);
			p.setColor(Color.BLACK);
			c.drawText(num + "", 30, 35, p);
		}
		
		return new BitmapDrawable(res, b);
	}
}
