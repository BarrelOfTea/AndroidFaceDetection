package com.tech.testfacedetection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;

import java.util.List;

public class DrawContours {

    public Bitmap drawContours(Bitmap bitmap, List<PointF> landmarks){
        Bitmap output = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();

        paint.setColor(Color.RED);
        paint.setStrokeWidth(8F);
        paint.setStyle(Paint.Style.STROKE);

        for (PointF point : landmarks){
            canvas.drawPoint(point.x, point.y, paint);
        }

        return output;
    }

    public Bitmap drawRect(Bitmap bitmap, Rect rect){
        Bitmap output = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();

        paint.setColor(Color.RED);
        paint.setStrokeWidth(8F);
        paint.setStyle(Paint.Style.STROKE);

        canvas.drawRect(rect, paint);

        return output;
    }


}
