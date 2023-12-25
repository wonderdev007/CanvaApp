package com.com.drawingcanvas;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;


class DrawingAction {

    Bitmap mBitmap;
    Rect mRect;
    Matrix mMatrix;

    DrawingAction(Bitmap bitmap, Rect rect){
        mBitmap = bitmap;
        mRect = new Rect(rect);
    }

    DrawingAction(Bitmap bitmap, Matrix matrix) {
        mBitmap = bitmap;
        mMatrix =matrix;
    }

    int getSize() {
        return mBitmap.getAllocationByteCount();
    }

}
