package com.pencil.prescription.activites;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.pencil.prescription.ZoomImageView;
import com.pencil.prescription.CropImageView;
import com.pencil.prescription.R;
import com.com.drawingcanvas.DrawingCanvas;
import com.com.drawingcanvas.brushes.BrushSettings;
import com.com.drawingcanvas.brushes.Brushes;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    Bitmap sample_image;
    float scale = 1;
    Bitmap bitmap_to_copy = null;
    CropImageView cropImageView;
    RelativeLayout relativedraw, lyt_text_cncl, lyt_tool, lyt_start, lyt_copy_cncl;
    private DrawingCanvas pv;
    private ImageView btn_add_img, btn_add_camera, undo_tool, redo_tool, btn_move_lock, view_img1, view_img2, view_save;
    Uri imageUri;
    int max_form_width = 1600;
    int max_form_height = 2000;
    protected float stroke = 3F, strokeeraser = 15F;
    int selected_pen = Brushes.PEN;
    String canvas_image_name = "canvas1.png";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        setContentView(R.layout.activity_main);

        String[] PERMISSIONS = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE,android.Manifest.permission.READ_EXTERNAL_STORAGE};
        if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, 122 );
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        lyt_copy_cncl = (RelativeLayout) findViewById(R.id.lyt_copy_cncl);

        LoadDrawingTools();
        BrushSettings brushSettings = pv.getBrushSettings();
        brushSettings.setSelectedBrushSize(stroke/100f);
        pv.clear();
        Bitmap bg = BitmapFactory.decodeResource(getResources(),R.drawable.test);
        pv.setBackgroundImage(max_form_width, max_form_height, bg);
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/gallery_test/"+canvas_image_name);
        try {
            if(file.exists()){
                Bitmap b = BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/gallery_test/"+canvas_image_name);
                if(b!=null) {
                    this.pv.drawBitmap("", b);
                }else{
                    this.pv.drawBitmap("", null);
                }
            }else{
                this.pv.drawBitmap("", null);
            }
        } catch (Exception e) {}
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void LoadDrawingTools(){
        lyt_tool = (RelativeLayout) findViewById(R.id.lyt_tool);
        lyt_start = (RelativeLayout) findViewById(R.id.lyt_start);
        btn_add_img = (ImageView) findViewById(R.id.btn_add_img);
        btn_add_camera = (ImageView) findViewById(R.id.btn_add_camera);
        undo_tool = (ImageView) findViewById(R.id.undo_tool);
        redo_tool = (ImageView) findViewById(R.id.redo_tool);
        btn_move_lock = (ImageView) findViewById(R.id.btn_move_lock);
        view_img1 = (ImageView) findViewById(R.id.view_img1);
        view_img2 = (ImageView) findViewById(R.id.view_img2);
        view_save = (ImageView) findViewById(R.id.view_save);
        relativedraw = (RelativeLayout) findViewById(R.id.main_relative_draw);
        lyt_text_cncl = (RelativeLayout) findViewById(R.id.lyt_text_cncl);
        cropImageView = (CropImageView) findViewById(R.id.cropImageView);

        this.pv = findViewById(R.id.drawing_view);
        this.pv.setUndoAndRedoEnable(true);
        BrushSettings brushSettings = pv.getBrushSettings();
        brushSettings.setSelectedBrushSize(stroke/100f);
        brushSettings.setSelectedBrush(Brushes.PEN);
        String stylus_color = "#0000FF";
        int initialColor = Color.parseColor(stylus_color);
        brushSettings.setColor(initialColor);

        findViewById(R.id.no).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lyt_copy_cncl.setVisibility(View.GONE);
                cropImageView.setVisibility(View.GONE);
                setBrushSelected(selected_pen);
            }
        });

        findViewById(R.id.yes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                cropImageView.crop(bitmap_to_copy).execute(mCropCallback, false);
                lyt_copy_cncl.setVisibility(View.GONE);
                cropImageView.setVisibility(View.GONE);
            }
        });

        btn_add_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] PERMISSIONS = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE,android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.CAMERA};
                if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
                    ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, 123 );
                }else{
                    Start_Gallery_Picker();
                }
            }
        });

        view_img1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                canvas_image_name = "canvas1.png";
                pv.clear();
                Bitmap bg = BitmapFactory.decodeResource(getResources(),R.drawable.test);
                pv.setBackgroundImage(max_form_width, max_form_height, bg);
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/gallery_test/"+canvas_image_name);
                try {
                    if(file.exists()){
                        Bitmap b = BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/gallery_test/"+canvas_image_name);
                        if(b!=null) {
                            pv.drawBitmap("", b);
                        }else{
                            pv.drawBitmap("", null);
                        }
                    }else{
                        pv.drawBitmap("", null);
                    }
                } catch (Exception e) {}
            }
        });

        view_img2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                canvas_image_name = "canvas2.png";
                pv.clear();
                Bitmap bg = BitmapFactory.decodeResource(getResources(),R.drawable.testing);
                pv.setBackgroundImage(max_form_width, max_form_height, bg);
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/gallery_test/"+canvas_image_name);
                try {
                    if(file.exists()){
                        Bitmap b = BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/gallery_test/"+canvas_image_name);
                        if(b!=null) {
                            pv.drawBitmap("", b);
                        }else{
                            pv.drawBitmap("", null);
                        }
                    }else{
                        pv.drawBitmap("", null);
                    }
                } catch (Exception e) {}
            }
        });

        view_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap bitmap = pv.exportDrawing();
                File file_dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/gallery_test");
                file_dir.mkdirs();
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/gallery_test/"+canvas_image_name);
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();
                    out.close();
                    Toast.makeText(MainActivity.this, "Image File saved Successfully to gallery with name : "+canvas_image_name, Toast.LENGTH_LONG).show();
                } catch (Exception e) {}
            }
        });

        btn_add_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] PERMISSIONS = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE,android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.CAMERA};
                if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
                    ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, 123 );
                }else{
                    Start_Camera_Picker();
                }
            }
        });

        undo_tool.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pv.undo();
            }
        });

        redo_tool.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pv.redo();
            }
        });

        btn_move_lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pv.isInZoomMode()) {
                    pv.exitZoomMode();
                    btn_move_lock.setImageResource(R.drawable.ic_lock);
                    return;
                }
                pv.enterZoomMode();
                btn_move_lock.setImageResource(R.drawable.ic_unlock);
            }
        });
    }


    private void setBrushSelected(int brushID){
        if(Brushes.ERASER != brushID) {
            selected_pen = brushID;
        }
        BrushSettings brushSettings = pv.getBrushSettings();
        brushSettings.setSelectedBrush(brushID);
        brushSettings.setSelectedBrushSize(stroke/100f);

        if(brushID == Brushes.CALLIGRAPHY & stroke <= 3){
            brushSettings.setSelectedBrushSize(4/100f);
        }

        lyt_copy_cncl.setVisibility(View.GONE);
        cropImageView.setVisibility(View.GONE);
    }

    private void Start_Gallery_Picker() {
        Intent pictureActionIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pictureActionIntent, 12);
    }

    private void Start_Camera_Picker() {
        try {
            String fileName = "temp.jpg";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, fileName);
            imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        }catch (Exception e){}
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        intent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        startActivityForResult(intent, 14);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);

                    int nh = (int) ( bitmap.getHeight() * (512.0 / bitmap.getWidth()) );
                    bitmap = Bitmap.createScaledBitmap(bitmap, 512, nh, true);
                    Load_Scale_Img(bitmap);

                } catch (IOException e) {}
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        } else if (resultCode == RESULT_OK && requestCode == 12) {
            Uri selectedImage = data.getData();
            CropImage.activity(selectedImage).start(this);
        } else if (resultCode == RESULT_OK && requestCode == 14) {
            CropImage.activity(imageUri).start(this);
        }else{
            setBrushSelected(selected_pen);
        }
    }

    public void Load_Scale_Img(Bitmap sampledBitmap){
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.canvas_gallery, null);
        ZoomImageView zoomimageView1 = view.findViewById(R.id.zoomimageView1);
        ImageView imageView = ((ImageView) view.findViewById(R.id.imageView1));
        ImageView dragHandle = ((ImageView) view.findViewById(R.id.imageView2));
        zoomimageView1.setImageBitmap(sampledBitmap);
        sample_image = sampledBitmap;

        imageView.setOnTouchListener(new View.OnTouchListener() {
            float centerX, centerY, startR, startScale, startX, startY;

            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {

                    // calculate center of image
                    centerX = (imageView.getLeft() + imageView.getRight()) / 2f;
                    centerY = (imageView.getTop() + imageView.getBottom()) / 2f;

                    // recalculate coordinates of starting point
                    startX = e.getRawX() - dragHandle.getX() + centerX;
                    startY = e.getRawY() - dragHandle.getY() + centerY;

                    // get starting distance and scale
                    startR = (float) Math.hypot(e.getRawX() - startX, e.getRawY() - startY);
                    startScale = imageView.getScaleX();

                } else if (e.getAction() == MotionEvent.ACTION_MOVE) {

                    // calculate new distance
                    float newR = (float) Math.hypot(e.getRawX() - startX, e.getRawY() - startY);

                    // set new scale
                    float newScale = newR / startR * startScale;
                    imageView.setScaleX(newScale);
                    imageView.setScaleY(newScale);

                    // move handler image
                    dragHandle.setX(centerX + imageView.getWidth()/2f * newScale);
                    dragHandle.setY(centerY + imageView.getHeight()/2f * newScale);

                    scale = newScale;

                } else if (e.getAction() == MotionEvent.ACTION_UP) {

                }
                return true;
            }
        });

        view.findViewById(R.id.yes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*Matrix matrix = new Matrix();
                matrix.postScale(scale, scale);
                view.setVisibility(View.GONE);
                Bitmap scaledBitmap = Bitmap.createBitmap(sample_image, 0, 0, sample_image.getWidth(), sample_image.getHeight(), matrix, true);
                toggleCropImage(scaledBitmap);*/
                    pv.pasteBitmap(zoomimageView1, ((BitmapDrawable) zoomimageView1.getDrawable()).getBitmap());

                view.setVisibility(View.GONE);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setBrushSelected(selected_pen);
                        }
                    }, 100);
            }
        });
        view.findViewById(R.id.no).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.setVisibility(View.GONE);
                setBrushSelected(selected_pen);
            }
        });
        relativedraw.addView(view);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.clear();
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.gc();
    }
}