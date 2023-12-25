package com.com.drawingcanvas.brushes.androidpathbrushes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;

import com.com.drawingcanvas.brushes.Brush;
import com.com.drawingcanvas.DrawingEvent;
import com.com.drawingcanvas.brushes.BrushRenderer;


public class PathBrushRenderer implements BrushRenderer {

    public Path mPath;
    public Paint mCurrentPathToolPaint;

    public PathBrushRenderer() {
        mPath = new Path();
    }

    @Override
    public void onTouch(DrawingEvent drawingEvent) {

        int pointsLength = drawingEvent.size();

        int action = drawingEvent.getAction();
        switch (action){
            case MotionEvent.ACTION_DOWN:
                mPath.reset();
                mPath.moveTo(drawingEvent.mPoints[0], drawingEvent.mPoints[1]);
                for (int i = 2 ; i + 1 < pointsLength ; i += 2)
                    mPath.lineTo(drawingEvent.mPoints[i], drawingEvent.mPoints[i + 1]);
                break;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                for (int i = 0 ; i + 1 < pointsLength ; i+=2)
                    mPath.lineTo(drawingEvent.mPoints[i], drawingEvent.mPoints[i + 1]);
                break;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawPath(mPath, mCurrentPathToolPaint);
    }

    @Override
    public void setBrush(Brush brush) {
        mCurrentPathToolPaint = brush.getPaint();
    }

}
