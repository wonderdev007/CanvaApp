package com.pencil.prescription.activites;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.com.drawingcanvas.DrawingCanvas;
import com.com.drawingcanvas.brushes.BrushSettings;
import com.com.drawingcanvas.brushes.Brushes;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.pencil.prescription.ColorArrayAdapter;
import com.pencil.prescription.CourseModal;
import com.pencil.prescription.CropImageView;
import com.pencil.prescription.DBHandler;
import com.pencil.prescription.GlobalConstant;
import com.pencil.prescription.R;
import com.pencil.prescription.ZoomImageView;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Bitmap sample_image;
    float scale = 1;
    Bitmap bitmap_to_copy = null;
    CropImageView cropImageView;
    RelativeLayout relativedraw, lyt_text_cncl, lyt_tool, lyt_start, lyt_copy_cncl;
    private DrawingCanvas pv;
    private ImageView btn_add_img, btn_add_camera, undo_tool, redo_tool, btn_move_lock, view_img1, view_img2, view_save, view_change;
    Uri imageUri;
    int max_form_width = 1600;
    int max_form_height = 2000;
    protected float stroke = 3F, strokeeraser = 15F;
    int selected_pen = Brushes.PEN;
    String canvas_image_name = "canvas1.png";

    private DBHandler dbHandler;

    private SeekBar fontSizeSeekBar;

    private Spinner colorSpinner;

    private Spinner fontSpinner;

    List<TextView> importedTextViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        setContentView(R.layout.activity_main);

        importedTextViews = new ArrayList<>();

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

        // creating a new dbhandler class
        // and passing our context to it.
        dbHandler = new DBHandler(MainActivity.this);
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

        view_change = (ImageView) findViewById(R.id.view_change);

        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar);

        fontSpinner = findViewById(R.id.fontSpinner);
        fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Set font size based on progress
                for(int i = 0; i < importedTextViews.size() ; i++)
                    importedTextViews.get(i).setTextSize(progress);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // You can implement this if needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // You can implement this if needed
            }
        });


        colorSpinner = findViewById(R.id.colorSpinner);

        // Initialize color names and values
        String[] colorNames = {
                "Red", "Green", "Blue", "Yellow", "Cyan", "Magenta",
                "Black", "White", "Gray", "Dark Red", "Dark Green",
                "Dark Blue", "Light Gray", "Dark Gray", "Orange", "Purple"
        };

        int[] colorValues = {
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA,
                Color.BLACK, Color.WHITE, Color.GRAY, Color.rgb(139, 0, 0), Color.rgb(0, 100, 0),
                Color.rgb(0, 0, 139), Color.LTGRAY, Color.DKGRAY, Color.rgb(255, 165, 0), Color.rgb(128, 0, 128)
        };


        // Create an instance of the custom adapter
        ColorArrayAdapter adapter = new ColorArrayAdapter(this, colorNames, colorValues);

        // Apply the adapter to the spinner
        colorSpinner.setAdapter(adapter);

        // Spinner item selection Listener
        colorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Handle color selection
                int selectedColor = colorValues[position];
                for(int i = 0; i < importedTextViews.size() ; i++)
                    importedTextViews.get(i).setTextColor(selectedColor);
                // Use selectedColor as needed
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Can leave this empty
            }
        });


        String[] fontFamilies = {
                "sans-serif", "sans-serif-condensed", "sans-serif-black",
                "sans-serif-light", "sans-serif-thin", "sans-serif-medium",
                "serif", "monospace", "casual", "cursive", "small-caps"
        };


        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fontFamilies);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        fontSpinner.setAdapter(adapter1);

        fontSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Typeface typeface = Typeface.create(fontFamilies[position], Typeface.NORMAL);
                for(int i = 0; i < importedTextViews.size() ; i++)
                    importedTextViews.get(i).setTypeface(typeface);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        this.pv = findViewById(R.id.drawing_view);
        this.pv.setUndoAndRedoEnable(true);

//      Bitmap bitmapDrawing = this.pv.exportDrawing()

        BrushSettings brushSettings = pv.getBrushSettings();
        brushSettings.setSelectedBrushSize(stroke/100f);
        brushSettings.setSelectedBrush(Brushes.PEN);
        String stylus_color = "#0000FF";
        int initialColor = Color.parseColor(stylus_color);
        brushSettings.setColor(initialColor);

        initTesseractAPI();

        view_change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap curBitmap = pv.exportDrawing();

                // Assuming 'curBitmap' is your Bitmap object
                File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File imageFile = new File(picturesDirectory, "myImage.png"); // Name of the image file

                // Make sure the Pictures directory exists
                picturesDirectory.mkdirs();

                try {
                    FileOutputStream fos = new FileOutputStream(imageFile);
                    curBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos); // Compress and write the bitmap to the FileOutputStream
                    fos.flush();
                    fos.close();

                    // Inform the media scanner about the new file so that it is immediately available to the user.
                    MediaScannerConnection.scanFile(MainActivity.this, new String[]{imageFile.getAbsolutePath()}, null, null);

//                    drawTextOnBitmap(getBitmapFromView(findViewById(R.id.main_relative_draw)), "Hello World", 200, 200, 24, Color.BLUE);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                detectText(pv.exportDrawing());
            }
        });

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

    public Bitmap getBitmapFromView(View view) {
        // Create a bitmap with the same dimensions as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);

        // Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);

        // Get the view's background
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            // If the view has a background, draw it on the canvas
            bgDrawable.draw(canvas);
        } else {
            // Else, draw a white background
            canvas.drawColor(Color.WHITE);
        }

        // Draw the view on the canvas
        view.draw(canvas);

        // Return the bitmap
        return returnedBitmap;
    }


    public Bitmap drawTextOnBitmap(Bitmap bitmap, String text, int x, int y, int textSize, int textColor) {
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = new Paint();
        paint.setColor(textColor);
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);

        canvas.drawText(text, x, y, paint);

        return mutableBitmap;
    }


    private void initTesseractAPI() {
        // load database for tesseract
        loadDatabaseTesseract();

        GlobalConstant.tesseractApi = new TessBaseAPI();
        GlobalConstant.tesseractApi.setDebug(true);
        Log.d("VERSION: ", String.valueOf(GlobalConstant.tesseractApi.getVersion()));
        try {
            String dataPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tesseract";
            boolean success = GlobalConstant.tesseractApi.init(dataPath, "eng");

            Toast.makeText(getApplicationContext(),"TessBaseAPI:" + String.valueOf(success), Toast.LENGTH_SHORT).show();
            Log.d("Tesseract: ", String.valueOf(success));
            if (!success) return;
            Toast.makeText(getApplicationContext(), "Load database file successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.d("Tesseract: ", "No traineddata files found.");
            Toast.makeText(getApplicationContext(), "Error while reading database", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDatabaseTesseract() {
        // Specify the file name in the assets folder
        final File externalStorage = Environment.getExternalStorageDirectory();
        // Create a directory for Tessdata in the app's private storage

        File[] subDirs = new File(externalStorage.getAbsolutePath() + "/tesseract/tessdata").listFiles();
        for (File dir : subDirs) {
            Log.d("sad", dir.getName());
        }

        File engTrainedFile = new File(externalStorage.getAbsolutePath() + "/tesseract/tessdata/eng.traineddata");
        Toast.makeText(getApplicationContext(), "Train Data Exists: " + String.valueOf(engTrainedFile.exists()), Toast.LENGTH_SHORT).show();
        Log.d("sad", externalStorage.getAbsolutePath() + "/tesseract/tessdata/eng.traineddata");

        Log.d("Tesseract: ", "Set Readable to true? " + String.valueOf(engTrainedFile.setReadable(true)));
        Log.d("Tesseract: ", "Train Data Exists?" + String.valueOf(engTrainedFile.exists()));
        Log.d("Tesseract: ", "Train Data Readable?" + String.valueOf(engTrainedFile.canRead()));

        if (engTrainedFile.exists() && engTrainedFile.canRead()) {
            // File exists and is readable
            // Proceed with reading the file
            try {
                FileInputStream fileInputStream = new FileInputStream(engTrainedFile);
                byte[] buffer = new byte[fileInputStream.available()];
                fileInputStream.read(buffer);
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            Log.e("Tesseract fabsolutepath", externalStorage.getAbsolutePath());
            Log.e("Tesseract f", "Doesn't exist tesseract database");
            // File doesn't exist or is not readable
            // Handle the error or take appropriate action
        }
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


    public void addTextView(String text, int posX, int posY) {
        // Create TextView
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(Color.RED);

        textView.setTextSize(20);

        // Set Layout Parameters
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);

        // Set absolute position
        params.leftMargin = posX; // Replace 'x' with your X coordinate in pixels
        params.topMargin = posY; // Replace 'y' with your Y coordinate in pixels

        // Apply the Layout Parameters to the TextView
        textView.setLayoutParams(params);

        textView.setOnTouchListener(new View.OnTouchListener() {
            private int lastAction;
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = view.getLeft();
                        initialY = view.getTop();
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        lastAction = event.getAction();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int x = initialX + (int) (event.getRawX() - initialTouchX);
                        int y = initialY + (int) (event.getRawY() - initialTouchY);

                        // Update view layout
                        view.layout(x, y, x + view.getWidth(), y + view.getHeight());
                        lastAction = event.getAction();
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (lastAction == MotionEvent.ACTION_DOWN) {
                            // Optional: handle click event if needed
                        }
                        return true;

                    default:
                        return false;
                }
            }
        });


        // Add TextView to your parent layout
        RelativeLayout parentLayout = findViewById(R.id.main_relative_draw); // Replace with your layout ID
        parentLayout.addView(textView);

        importedTextViews.add(textView);
    }

    //  Detect Logic
    void detectText(Bitmap newBitmap) {
        GlobalConstant.tesseractApi.setImage(newBitmap);
        String extractedText = GlobalConstant.tesseractApi.getUTF8Text();
        Toast.makeText(this, extractedText, Toast.LENGTH_SHORT).show();


        pv.clear();
//
        final ResultIterator iterator = GlobalConstant.tesseractApi.getResultIterator();
        String lastUTF8Text;
        float lastConfidence;
        int[] lastBoundingBox;
        int count = 0;
        iterator.begin();

        do {
            lastUTF8Text = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD);
            lastConfidence = iterator.confidence(TessBaseAPI.PageIteratorLevel.RIL_WORD);
            lastBoundingBox = iterator.getBoundingBox(TessBaseAPI.PageIteratorLevel.RIL_WORD);

            Log.d("POSITION", String.valueOf(lastBoundingBox[0]) + ", " + String.valueOf(lastBoundingBox[1]) + ", " + String.valueOf(lastBoundingBox[2]) + ", " + String.valueOf(lastBoundingBox[3]));

            addTextView(lastUTF8Text, lastBoundingBox[0], lastBoundingBox[1]);

            count++;
        }
        while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD));



//        int indexOfWords = 0 ;
//        for (String line: lines) {
//            try{
//                Pixa region = GlobalConstant.tesseractApi.getRegions();
//
//                int centerX = region.getBoxRect(indexOfWords).centerX(), centerY = region.getBoxRect(indexOfWords).centerY();
//                Log.d(String.valueOf(centerX), String.valueOf(centerY));
//
//                addTextView(line, centerX, centerY);
//                indexOfWords ++;
//            } catch(Exception ex) {
//
//            }
//        }

        dbHandler.addNewData(0, 0, extractedText, 12, "Arial", "black");

        ArrayList<CourseModal> courses = dbHandler.readCourses();
        for (CourseModal course : courses) {
            Log.d("TTT", course.getText());
        }

    }
}