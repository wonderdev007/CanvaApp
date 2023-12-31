package com.pencil.prescription.callbacks;

import com.pencil.prescription.entity.ChosenImage;

import java.util.List;

/**
 * Created by kbibek on 2/23/16.
 */
public interface ImagePickerCallback extends PickerCallback {
    void onImagesChosen(List<ChosenImage> images);
}
