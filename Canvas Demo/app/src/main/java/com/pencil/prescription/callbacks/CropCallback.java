package com.pencil.prescription.callbacks;

import android.graphics.Bitmap;
import android.graphics.Rect;

public interface CropCallback extends Callback {
  void onSuccess(Bitmap cropped, Rect ccs);
}
