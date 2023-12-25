package com.pencil.prescription.callbacks;

import android.graphics.Bitmap;

import com.pencil.prescription.CropImageView;

public class CropRequest {

  private CropImageView cropImageView;
  private Bitmap sourceUri;

  public CropRequest(CropImageView cropImageView, Bitmap sourceUri) {
    this.cropImageView = cropImageView;
    this.sourceUri = sourceUri;
  }

  public void execute(CropCallback cropCallback, boolean is_eraser) {
    cropImageView.cropAsync(sourceUri, cropCallback, is_eraser);
  }
}
