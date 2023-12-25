package com.pencil.prescription.callbacks;

import com.pencil.prescription.entity.ChosenVideo;

import java.util.List;

/**
 * Created by kbibek on 2/23/16.
 */
public interface VideoPickerCallback extends PickerCallback {
    void onVideosChosen(List<ChosenVideo> videos);
}
