package com.com.drawingcanvas;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.com.drawingcanvas.brushes.BrushSettings;
import com.com.drawingcanvas.brushes.Brushes;
import java.io.ByteArrayOutputStream;

public class DrawingCanvas extends View{

    private static final float MAX_SCALE = 5f;
    private static final float MIN_SCALE = 0.1f;
    public String Exact_canvas_Resolution = "";
    String canvas_image_note_text = "";
    int BGBitmap_width = 0, BGBitmap_height = 0, exact_width = 0, exact_height = 0, max_form_width = 1600, max_form_height = 2000;
    private Canvas mCanvas;
    public Bitmap mDrawingBitmap;
    private Bitmap mBGBitmap;
    private int mBGColor;//BackGroundColor
    Paint draw_paint;
    boolean shrink = false;
    //if true, do not drawFromTo any thin. Just zoom and translate thr drawing in onTouchEvent()
    private boolean mZoomMode = false;

    private float mDrawingTranslationX = 0f;
    private float mDrawingTranslationY = 0f;
    private float mScaleFactor = 1f;

    private float mLastX[] = new float[2];
    private float mLastY[] = new float[2];

    private ActionStack mActionStack;//This is used for undo/redo, if null this means the undo and redo are disabled

    private DrawingPerformer mDrawingPerformer;

    private OnDrawListener mOnDrawListener;

    private Brushes mBrushes;

    private boolean mCleared = true;

    private Paint mSrcPaint = new Paint(){{
            setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        }};

    private ScaleGestureDetector mScaleGestureDetector = new ScaleGestureDetector(
        getContext(),
        new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float xCenter = (mLastX[0] + mLastX[1])/2;
                float yCenter = (mLastY[0] + mLastY[1])/2;
                float xd = (xCenter - mDrawingTranslationX);
                float yd = (yCenter - mDrawingTranslationY);
                mScaleFactor *= detector.getScaleFactor();
                if (mScaleFactor == MAX_SCALE || mScaleFactor == MIN_SCALE)
                    return true;
                mDrawingTranslationX = xCenter - xd * detector.getScaleFactor();
                mDrawingTranslationY = yCenter - yd * detector.getScaleFactor();

                checkBounds();
                invalidate();
                return true;
            }
        }
    );

    public interface OnDrawListener{
        void onDraw();
    }

    public DrawingCanvas(Context context) {
        this(context, null);
    }

    public DrawingCanvas(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawingCanvas(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mBrushes = new Brushes(context.getResources());
        if (attrs != null)
            initializeAttributes(attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        exact_width = w;
        exact_height = h;
        Exact_canvas_Resolution = "Default Canvas Resolution is: Width: "+w +" Height: "+h;
        /*if (mDrawingBitmap == null && w != 0 && h != 0) {
            initializeDrawingBitmap(
                    w - getPaddingStart() - getPaddingEnd(),
                    h - getPaddingTop() - getPaddingBottom());
            invalidate();
        }*/
        if (w != 0 && h != 0) {
            if(BGBitmap_width < max_form_width && BGBitmap_height < max_form_height){
                if(mDrawingBitmap == null) {
                    mDrawingBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                }
                if(shrink == true) {
                    if (BGBitmap_width != w || BGBitmap_height != h) {
                        mBGBitmap = Bitmap.createScaledBitmap(mBGBitmap, w, h, true);
                    }
                    shrink = false;
                    BGBitmap_width = w;
                    BGBitmap_height = h;
                    initializeDrawingBitmap(BGBitmap_width, BGBitmap_height);
                }
                invalidate();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //prevent drawing in the padding
        if(mDrawingBitmap == null){
            this.mDrawingBitmap = Bitmap.createBitmap(BGBitmap_width, BGBitmap_height, Bitmap.Config.ARGB_8888);
        }
        canvas.clipRect(
                getPaddingStart(),
                getPaddingTop(),
                canvas.getWidth() - getPaddingRight(),
                canvas.getHeight() - getPaddingBottom()
        );

        //drawFromTo the background and the bitmap in the middle with scale and translation
        canvas.translate(getPaddingStart() + mDrawingTranslationX, getPaddingTop() + mDrawingTranslationY);
        canvas.scale(mScaleFactor, mScaleFactor);
        canvas.clipRect(//prevent drawing paths outside the bounds
                0,
                0,
                mDrawingBitmap.getWidth(),
                mDrawingBitmap.getHeight()
        );
        canvas.drawColor(mBGColor);
        if (mBGBitmap != null)
            canvas.drawBitmap(mBGBitmap, 0, 0, null);
        if (mDrawingPerformer.isDrawing())//true if the user is touching the screen
            mDrawingPerformer.draw(canvas, mDrawingBitmap);
        else
            canvas.drawBitmap(mDrawingBitmap,0, 0, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minDimension = (int) (250 * getResources().getDisplayMetrics().density);//150dp
        int contentWidth = minDimension + getPaddingStart() + getPaddingEnd();
        int contentHeight = minDimension + getPaddingTop() + getPaddingBottom();

        int measuredWidth = resolveSize(contentWidth, widthMeasureSpec);
        int measuredHeight = resolveSize(contentHeight, heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        if (mZoomMode)
            return handleZoomAndTransEvent(event);
        if (event.getPointerCount() > 1)
            return false;
        MotionEvent.PointerProperties pp = new MotionEvent.PointerProperties();
        event.getPointerProperties(0, pp);
//        if (pp.toolType != MotionEvent.TOOL_TYPE_STYLUS){
//            return false;
//        }
        float scaledX = (event.getX() - mDrawingTranslationX) / mScaleFactor;
        float scaledY = (event.getY() - mDrawingTranslationY) / mScaleFactor;
        event.setLocation(scaledX, scaledY);
        mDrawingPerformer.onTouch(event);
        invalidate();
        return true;
    }

    private int mPointerId;
    private boolean translateAction = true;
    public boolean handleZoomAndTransEvent(MotionEvent event) {
        super.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_POINTER_UP && event.getPointerCount() == 1)
            return false;
        if (event.getPointerCount() > 1){
            translateAction = false;
            mScaleGestureDetector.onTouchEvent(event);
        }else if (translateAction)
            switch (event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    mPointerId = event.getPointerId(0);
                    break;
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_CANCEL:
                    int pointerIndex = event.findPointerIndex(mPointerId);
                    if (pointerIndex != -1) {
                        mDrawingTranslationX += event.getX(pointerIndex) - mLastX[0];
                        mDrawingTranslationY += event.getY(pointerIndex) - mLastY[0];
                    }
                    break;
            }
        if (event.getActionMasked() == MotionEvent.ACTION_UP)
            translateAction = true; // reset

        mLastX[0] = event.getX(0);
        mLastY[0] = event.getY(0);
        if (event.getPointerCount() > 1) {
            mLastX[1] = event.getX(1);
            mLastY[1] = event.getY(1);
        }

        checkBounds();
        invalidate();
        return true;
    }

    public Bitmap exportDrawing(){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        mDrawingBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
//        Canvas canvas = new Canvas(bitmap);
//        canvas.drawColor(mBGColor);
//        if (mBGBitmap != null)
//            canvas.drawBitmap(mBGBitmap, 0, 0, null);
//        canvas.drawBitmap(mDrawingBitmap, 0, 0, null);
        return mDrawingBitmap;
    }

    public Bitmap exportDrawingWithoutBackground(){
        return mDrawingBitmap.copy(Bitmap.Config.ARGB_8888, true);
    }

    public int getDrawingBackground() {
        return mBGColor;
    }

    public void setDrawingBackground(int color){
        mBGColor = color;
        invalidate();
    }

    public void setUndoAndRedoEnable(boolean enabled){
        if (enabled)
            mActionStack = new ActionStack();
        else
            mActionStack = null;
    }

    /**
     * Set an image as a background so you can draw on top of it. NOTE that calling this method is
     * going to clear anything drawn previously and you will not be able to restore anything with undo().
     * @param bitmap to be used as a background image.
     */
    public void setBackgroundImage(int form_width, int form_height, Bitmap bitmap) {
        max_form_width = form_width;
        max_form_height = form_height;
        privateSetBGBitmap(bitmap);
        initializeDrawingBitmap(BGBitmap_width, BGBitmap_height);
        if (mActionStack != null) //if undo and redo is enabled, remove the old actions by creating a new instance.
            mActionStack = new ActionStack();
        invalidate();
    }

    public void resetZoom(){
        //if the bitmap is smaller than the view zoom in to make the bitmap fit the view
        float targetSF = calcAppropriateScaleFactor(mDrawingBitmap.getWidth(), mDrawingBitmap.getHeight());

        //align the bitmap in the center
        float targetX = (getWidth() - mDrawingBitmap.getWidth() * targetSF) / 2;
        float targetY = (getHeight() - mDrawingBitmap.getHeight() * targetSF) / 2;

        ObjectAnimator scaleAnimator = ObjectAnimator.ofFloat(this, "scaleFactor", mScaleFactor, targetSF);
        ObjectAnimator xTranslationAnimator
                = ObjectAnimator.ofFloat(this, "drawingTranslationX", mDrawingTranslationX, targetX);
        ObjectAnimator yTranslationAnimator
                = ObjectAnimator.ofFloat(this, "drawingTranslationY", mDrawingTranslationY, targetY);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleAnimator, xTranslationAnimator, yTranslationAnimator);
        animatorSet.start();
    }

    /**
     * This method clears the drawing bitmap. If this method is called consecutively only the first
     * call will take effect.
     * @return true if the canvas cleared successfully.
     */
    public boolean clear() {
        if (mCleared)
            return false;
        Rect rect = new Rect(
                0,
                0,
                mDrawingBitmap.getWidth(),
                mDrawingBitmap.getHeight()
        );
        if (mActionStack != null){
            DrawingAction drawingAction = new DrawingAction(Bitmap.createBitmap(mDrawingBitmap), rect);
            mActionStack.addAction(drawingAction);
        }
        mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();

        mCleared = true;
        return true;
    }

    public boolean isCleared() {
        return mCleared;
    }

    Brushes getBrushes() {
        return mBrushes;
    }

    public boolean undo(){
        if (mActionStack == null)
            throw new IllegalStateException("Undo functionality is disable you can enable it by calling setUndoAndRedoEnable(true)");
        if (mActionStack.isUndoStackEmpty() || mDrawingPerformer.isDrawing())
            return false;
        DrawingAction previousAction = mActionStack.previous();
        DrawingAction oppositeAction = getOppositeAction(previousAction);
        mActionStack.addActionToRedoStack(oppositeAction);
        performAction(previousAction);
        return true;
    }

    public boolean redo(){
        if (mActionStack == null)
            throw new IllegalStateException("Redo functionality is disable you can enable it by calling setUndoAndRedoEnable(true)");
        if (mActionStack.isRedoStackEmpty() || mDrawingPerformer.isDrawing())
            return false;
        DrawingAction nextAction = mActionStack.next();
        DrawingAction oppositeAction = getOppositeAction(nextAction);
        mActionStack.addActionToUndoStack(oppositeAction);
        performAction(nextAction);
        return true;
    }

    public boolean isUndoStackEmpty(){
        if (mActionStack == null)
            throw new IllegalStateException("Undo functionality is disable you can enable it by calling setUndoAndRedoEnable(true)");
        return mActionStack.isUndoStackEmpty();
    }

    public boolean isRedoStackEmpty(){
        if (mActionStack == null)
            throw new IllegalStateException("Undo functionality is disable you can enable it by calling setUndoAndRedoEnable(true)");
        return mActionStack.isRedoStackEmpty();
    }

    /**
     * Return an instance of BrushSetting, you can use it to change the selected brush. And change
     * the size of the selected brush and the color.
     * @return an instance of BrushSetting associated with this DrawingCanvas.
     */
    public BrushSettings getBrushSettings() {
        return mBrushes.getBrushSettings();
    }

    /**
     * Enter the zoom mode to be able to zoom and move the drawing. Note that you cannot enter
     * the zoom mode if the the user is drawing.
     * @return true if enter successfully, false otherwise.
     */
    public boolean enterZoomMode() {
        if (mDrawingPerformer.isDrawing())
            return false;
        mZoomMode = true;
        return true;
    }

    /**
     * Exit the zoom mode to be able to draw.
     */
    public void exitZoomMode() {
        mZoomMode = false;
    }

    public boolean isInZoomMode() {
        return mZoomMode;
    }

    /**
     * Set a listener to be notified whenever a new stroke or a point is drawn.
     */
    public void setOnDrawListener(OnDrawListener onDrawListener) {
        mOnDrawListener = onDrawListener;
    }

    public float getDrawingTranslationX() {
        return mDrawingTranslationX;
    }

    public void setDrawingTranslationX(float drawingTranslationX) {
        mDrawingTranslationX = drawingTranslationX;
        invalidate();
    }

    public float getDrawingTranslationY() {
        return mDrawingTranslationY;
    }

    public void setDrawingTranslationY(float drawingTranslationY) {
        mDrawingTranslationY = drawingTranslationY;
        invalidate();
    }

    public float getScaleFactor() {
        return mScaleFactor;
    }

    public void setScaleFactor(float scaleFactor) {
        mScaleFactor = scaleFactor;
        invalidate();
    }

    private void performAction(DrawingAction action) {
        mCleared = false;
        if(action.mMatrix==null) {
            mCanvas.drawBitmap(
                    action.mBitmap,
                    action.mRect.left,
                    action.mRect.top,
                    mSrcPaint
            );
        } else {

            action.mMatrix.setRotate(0);
            mCanvas.drawBitmap(action.mBitmap, action.mMatrix, mSrcPaint);
        }
        invalidate();
    }

    private DrawingAction getOppositeAction(DrawingAction action){
        Rect rect = action.mRect;
        Bitmap bitmap;
        if(action.mMatrix==null) {
            bitmap = Bitmap.createBitmap(
                    mDrawingBitmap,
                    rect.left,
                    rect.top,
                    rect.right - rect.left,
                    rect.bottom - rect.top
            );
            return new DrawingAction(bitmap, rect);
        } else {
            bitmap = Bitmap.createBitmap(
                    mDrawingBitmap
            );
            return new DrawingAction(bitmap, action.mMatrix);
        }
    }

    protected void checkBounds(){
        int width = mDrawingBitmap.getWidth();
        int height = mDrawingBitmap.getHeight();

        int contentWidth = (int) (width * mScaleFactor);
        int contentHeight = (int) (height * mScaleFactor);

        float widthBound = getWidth()/6;
        float heightBound = getHeight()/6;

        if (contentWidth < widthBound){
            if (mDrawingTranslationX < -contentWidth/2)
                mDrawingTranslationX = -contentWidth/2f;
            else if (mDrawingTranslationX > getWidth() - contentWidth/2)
                mDrawingTranslationX = getWidth() - contentWidth/2f;
        } else if (mDrawingTranslationX > getWidth() - widthBound)
            mDrawingTranslationX = getWidth() - widthBound;
        else if (mDrawingTranslationX + contentWidth < widthBound)
            mDrawingTranslationX = widthBound -  contentWidth;

        if (contentHeight < heightBound){
            if (mDrawingTranslationY < -contentHeight/2)
                mDrawingTranslationY = -contentHeight/2f;
            else if (mDrawingTranslationY > getHeight() - contentHeight/2)
                mDrawingTranslationY = getHeight() - contentHeight/2f;
        }else if (mDrawingTranslationY > getHeight() - heightBound)
            mDrawingTranslationY = getHeight() - heightBound;
        else if (mDrawingTranslationY + contentHeight < heightBound)
            mDrawingTranslationY = heightBound -  contentHeight;
    }

    private void initializeDrawingBitmap(int w, int h) {
        if(mDrawingBitmap == null) {
            mDrawingBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        }
        if(exact_width != 0) {
            if (mDrawingBitmap.getWidth() != w || mDrawingBitmap.getHeight() != h) {
                mDrawingBitmap = Bitmap.createScaledBitmap(mDrawingBitmap, w, h, true);
            }
        }
        mCanvas = new Canvas(mDrawingBitmap);
        if (mDrawingPerformer == null){
            mDrawingPerformer = new DrawingPerformer(mBrushes);
            mDrawingPerformer.setPaintPerformListener(new MyDrawingPerformerListener());
        }
        mDrawingPerformer.setWidthAndHeight(w, h);
    }

    private void initializeAttributes(AttributeSet attrs){
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.DrawingView,
                0, 0);
        try {
            BrushSettings settings = mBrushes.getBrushSettings();
            int brushColor = typedArray.getColor(R.styleable.DrawingView_brush_color, 0xFF000000);
            settings.setColor(brushColor);
            int selectedBrush = typedArray.getInteger(R.styleable.DrawingView_brush, Brushes.PENCIL);
            settings.setSelectedBrush(selectedBrush);
            float size = typedArray.getFloat(R.styleable.DrawingView_brush_size, 0.5f);
            if (size < 0 || size > 1)
                throw new IllegalArgumentException("DrawingCanvas brush_size attribute should have a value between 0 and 1 in your xml file");
            settings.setSelectedBrushSize(size);

            draw_paint = new Paint();
            draw_paint.setAntiAlias(true);

            mBGColor = Color.TRANSPARENT;
        } finally {
            typedArray.recycle();
        }
    }

    private void privateSetBGBitmap(Bitmap bitmap){
        mBGBitmap = bitmap;
        shrink = true;
        if(exact_width!=0 && (mBGBitmap.getWidth() < max_form_width && mBGBitmap.getHeight() < max_form_height)){
            BGBitmap_width = exact_width;
            BGBitmap_height = exact_height;
            mBGBitmap = Bitmap.createScaledBitmap(mBGBitmap, BGBitmap_width, BGBitmap_height, true);
        }else{
            BGBitmap_width = mBGBitmap.getWidth();
            BGBitmap_height = mBGBitmap.getHeight();
        }

        mScaleFactor = 1f;
        mDrawingTranslationX = 0;
        mDrawingTranslationY = 0;
    }

    public String get_cor(){
        String x_y = mDrawingTranslationX +","+ mDrawingTranslationY;
        return x_y;
    }

    public void drawBitmap(String canvas_image_note, Bitmap previous_note){
        if(previous_note!=null) {
            mDrawingBitmap = previous_note.copy(Bitmap.Config.ARGB_8888, true);
//            if(mDrawingBitmap.getWidth()!=BGBitmap_width) {
//                mDrawingBitmap = Bitmap.createScaledBitmap(mDrawingBitmap, BGBitmap_width, BGBitmap_height, true);
//            }
        }else{
            mDrawingBitmap = Bitmap.createBitmap(BGBitmap_width, BGBitmap_height, Bitmap.Config.ARGB_8888);
            canvas_image_note_text = canvas_image_note;
//            if(!canvas_image_note.equalsIgnoreCase("")){
//                draw_patient_details(canvas_image_note_text);
//            }
        }
        if(mBGBitmap != null) {
            initializeDrawingBitmap(BGBitmap_width, BGBitmap_height);
            if (mActionStack != null) //if undo and redo is enabled, remove the old actions by creating a new instance.
                mActionStack = new ActionStack();
            invalidate();
        }
    }

    public byte[] getBitmapAsByteArray() {
        return this.getBitmapAsByteArray(Bitmap.CompressFormat.PNG, 100);
    }

    public byte[] getBitmapAsByteArray(Bitmap.CompressFormat format, int quality) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        mDrawingBitmap.compress(format, quality, byteArrayOutputStream);

        return byteArrayOutputStream.toByteArray();
    }

    private float calcAppropriateScaleFactor(int bitmapWidth, int bitmapHeight){
        float canvasWidth = getWidth() - getPaddingStart() - getPaddingEnd();
        float canvasHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        if (bitmapWidth < canvasWidth && bitmapHeight < canvasHeight){
            float scaleFactor;
            //if the bitmap is smaller than the view -> zoom in
            scaleFactor = canvasHeight/bitmapHeight;//let us zoom in to make the bitmap height appears equal to the view height
            if (bitmapWidth * scaleFactor > canvasWidth){
                //OHHH NO,  the bitmap width is larger than the view width, let us make the scale factor depends
                // on the width and for sure the bitmap height will appear less than the view height.
                scaleFactor = canvasWidth/bitmapWidth;
            }
             return scaleFactor;
        }else { //otherwise just make sure the scale is 1
            return 1f;
        }
    }

    private class MyDrawingPerformerListener implements DrawingPerformer.DrawingPerformerListener {

        @Override
        public void onDrawingPerformed(Bitmap bitmap, Rect rect) {
            mCleared = false;
            if (mActionStack != null)
                storeAction(rect);
            mCanvas.drawBitmap(bitmap, rect.left, rect.top, null);
            invalidate();
            if (mOnDrawListener != null)
                mOnDrawListener.onDraw();
        }

        @Override
        public void onDrawingPerformed(Path path, Paint paint, Rect rect) {
            mCleared = false;
            if (mActionStack != null)
                storeAction(rect);
            mCanvas.drawPath(path, paint);
            invalidate();
            if (mOnDrawListener != null)
                mOnDrawListener.onDraw();
        }

    }

        private void storeAction(Rect rect) {
            Bitmap bitmap = Bitmap.createBitmap(
                    mDrawingBitmap,
                    rect.left,
                    rect.top,
                    rect.right - rect.left,
                    rect.bottom - rect.top
            );
            DrawingAction action = new DrawingAction(bitmap, rect);
            mActionStack.addAction(action);
        }

    private void storeAction(Matrix matrix) {
        Bitmap bitmap = Bitmap.createBitmap(
                mDrawingBitmap
        );
        DrawingAction action = new DrawingAction(bitmap, matrix);
        mActionStack.addAction(action);
    }




    public void draw_patient_details(TextView txt_pt_details, String text){
        txt_pt_details.setText(text);
        /*TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(25);
        Canvas canvas = new Canvas(mDrawingBitmap);
        canvas.save();

        int edt_x = 1;
        int edt_y = 5;

        float scaledX = (edt_x - mDrawingTranslationX) / mScaleFactor;
        float scaledY = (edt_y - mDrawingTranslationY) / mScaleFactor;

//        float rotateDegree = -90;

        Rect background = new Rect((int) (0), (int) (0), (int) (mDrawingBitmap.getWidth()), (int) (30));
        textPaint.setColor(Color.WHITE);
        canvas.drawRect(background, textPaint);

        textPaint.setColor(mBrushes.getBrushSettings().getColor());
        StaticLayout mTextLayout = new StaticLayout(text, textPaint, mDrawingBitmap.getWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        canvas.translate(scaledX, scaledY);
//        canvas.translate(30, BGBitmap_height - 30);
//        canvas.rotate(rotateDegree);
        mTextLayout.draw(canvas);
        canvas.restore();
        invalidate();*/
    }

    public void pasteBitmap(ImageView crop_image, Bitmap Cropped_image) {
        if(mDrawingBitmap == null){
            this.mDrawingBitmap = Bitmap.createBitmap(BGBitmap_width, BGBitmap_height, Bitmap.Config.ARGB_8888);
        }
        if(crop_image == null){
            return;
        }
        int[] location = new int[2];
        crop_image.getLocationOnScreen(location);
        int img_x = location[0];
//        int img_y = location[1]-45;

        int img_y = location[1];  //80

        float scaledX = (img_x - mDrawingTranslationX) / mScaleFactor;
        float scaledY = (img_y - mDrawingTranslationY) / mScaleFactor;
        //float scaledX = 0.0f;
        //float scaledY = 0.0f;

        Rect rect = new Rect(
                (int)scaledX,
                (int)scaledY,
                Cropped_image.getWidth() + (int)scaledX,
                Cropped_image.getHeight() + (int)scaledY
        );

        mCleared = false;
        if (mActionStack !=  null)
            storeAction(crop_image.getImageMatrix());
        mCanvas.drawBitmap(Cropped_image, crop_image.getImageMatrix(), null);
        invalidate();
        if (mOnDrawListener != null)
            mOnDrawListener.onDraw();


        /*mCanvas.save();
        invalidate();*/
    }
}
