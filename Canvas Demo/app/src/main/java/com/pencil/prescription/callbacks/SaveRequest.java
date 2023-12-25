package com.pencil.prescription.callbacks;

import android.graphics.Bitmap;
import android.net.Uri;

import com.pencil.prescription.CropImageView;

public class SaveRequest {

  private CropImageView cropImageView;
  private Bitmap image;
  private int compressQuality = -1;

  public SaveRequest(CropImageView cropImageView, Bitmap image) {
    this.cropImageView = cropImageView;
    this.image = image;
  }

  private void build() {
    if (compressQuality >= 0) {
      cropImageView.setCompressQuality(compressQuality);
    }
  }

  public void execute(Uri saveUri, SaveCallback callback) {
    build();
    cropImageView.saveAsync(saveUri, image, callback);
  }
}
