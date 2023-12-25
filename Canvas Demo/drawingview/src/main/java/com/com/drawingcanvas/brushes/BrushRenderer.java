package com.com.drawingcanvas.brushes;


import android.graphics.Canvas;

import com.com.drawingcanvas.DrawingEvent;

public interface BrushRenderer {
    void draw(Canvas canvas);
    void onTouch(DrawingEvent drawingEvent);
    void setBrush(Brush brush);
}
