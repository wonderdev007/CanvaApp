package com.com.drawingcanvas;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.com.drawingcanvas.brushes.BrushSettings;
import com.com.drawingcanvas.brushes.Brushes;

public class DrawingView extends View{

    private static final float MAX_SCALE = 5f;
    private static final float MIN_SCALE = 0.1f;
    public int colour = 0xFF000000;
    private Canvas mCanvas;
    private Bitmap mDrawingBitmap;
    private Bitmap mBGBitmap;
    private int mBGColor;//BackGroundColor

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

    public DrawingView(Context context) {
        this(context, null);
    }

    public DrawingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mBrushes = new Brushes(context.getResources());
        if (attrs != null)
            initializeAttributes(attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mDrawingBitmap == null && w != 0 && h != 0) {
            initializeDrawingBitmap(
                    w - getPaddingStart() - getPaddingEnd(),
                    h - getPaddingTop() - getPaddingBottom());
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //prevent drawing in the padding
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
        Bitmap bitmap = Bitmap.createBitmap(
                mDrawingBitmap.getWidth(),
                mDrawingBitmap.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(mBGColor);
        if (mBGBitmap != null)
            canvas.drawBitmap(mBGBitmap, 0, 0, null);
        canvas.drawBitmap(mDrawingBitmap, 0, 0, null);
        return bitmap;
    }

    public Bitmap exportDrawingWithoutBackground(){
        return mDrawingBitmap;
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
    public void setBackgroundImage(Bitmap bitmap) {
        privateSetBGBitmap(bitmap);
        initializeDrawingBitmap(mBGBitmap.getWidth(), mBGBitmap.getHeight());
        if (mActionStack != null) //if undo and redo is enabled, remove the old actions by creating a new instance.
            mActionStack = new ActionStack();
        invalidate();
    }

    public int getDrawingBackground() {
        return mBGColor;
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
            DrawingAction drawingAction = new DrawingAction(
                    Bitmap.createBitmap(mDrawingBitmap),
                    rect
            );
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
     * @return an instance of BrushSetting associated with this DrawingView.
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
        mCanvas.drawBitmap(
                action.mBitmap,
                action.mRect.left,
                action.mRect.top,
                mSrcPaint
        );
        invalidate();
    }

    private DrawingAction getOppositeAction(DrawingAction action){
        Rect rect = action.mRect;
        Bitmap bitmap = Bitmap.createBitmap(
                mDrawingBitmap,
                rect.left,
                rect.top,
                rect.right - rect.left,
                rect.bottom - rect.top
        );
        return new DrawingAction(bitmap, rect);
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
        String image_note = "iVBORw0KGgoAAAANSUhEUgAABfYAAAeiCAYAAADG/aLlAAAABHNCSVQICAgIfAhkiAAAIABJREFUeJzs3c1xI8naHtDnu9JeMKFMoAdTnwXTS+0aHgxlQZcsmJYFjWvBtAcNAxTRfS1ozF4R5I3QQrvWopBRCRAAC38skDwnAkESqJ8kyAJQT2a+lQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA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AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADg/7EHBwIAAAAAQP6vjaCqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqW4m1gAAACVNJREFUqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqrSHhwIAAAAAAjyt15hgAoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACuAD6pfH5I0zbUAAAAAElFTkSuQmCC";
        Bitmap previous_bitmap = BitmapFactory.decodeByteArray(Base64.decode(image_note, Base64.NO_WRAP), 0, Base64.decode(image_note, Base64.NO_WRAP).length);
        mDrawingBitmap = previous_bitmap.copy(Bitmap.Config.ARGB_8888, true);
//        mDrawingBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
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
                throw new IllegalArgumentException("DrawingView brush_size attribute should have a value between 0 and 1 in your xml file");
            settings.setSelectedBrushSize(size);

            mBGColor = typedArray.getColor(R.styleable.DrawingView_drawing_background_color, -1);//default to white

        } finally {
            typedArray.recycle();
        }
    }

    private void privateSetBGBitmap(Bitmap bitmap){
        float canvasWidth = getWidth() - getPaddingStart() - getPaddingEnd();
        float canvasHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        float bitmapWidth = bitmap.getWidth();
        float bitmapHeight = bitmap.getHeight();
        float scaleFactor = 1;
        //if the bitmap is smaller than the view -> find a scale factor to scale it down
        if (bitmapWidth > canvasWidth && bitmapHeight > canvasHeight) {
            scaleFactor = canvasHeight/bitmapHeight;
            if ((int) (bitmap.getWidth() * scaleFactor) > canvasWidth)
                scaleFactor = canvasWidth/bitmapWidth;
        } else if (bitmapWidth > canvasWidth && bitmapHeight < canvasHeight)
            scaleFactor = canvasWidth/bitmapWidth;
        else if (bitmapWidth < canvasWidth && bitmapHeight > canvasHeight)
            scaleFactor = canvasHeight/bitmapHeight;

        if (scaleFactor != 1)//if the bitmap is larger than the view scale it down
//            bitmap = Utilities.resizeBitmap(bitmap, ((int) (bitmap.getWidth() * scaleFactor)), (int) (bitmap.getHeight() * scaleFactor));

        mBGBitmap = bitmap;

        mScaleFactor = calcAppropriateScaleFactor(mBGBitmap.getWidth(), mBGBitmap.getHeight());

        //align the bitmap in the center
//        mDrawingTranslationX = (canvasWidth - mBGBitmap.getWidth() * mScaleFactor)/2;
//        mDrawingTranslationY = (canvasHeight - mBGBitmap.getHeight() * mScaleFactor)/2;

        mDrawingTranslationX = 0;
        mDrawingTranslationY = 0;
    }

    public void drawText() {
        if(true) {
            if(mDrawingBitmap == null){
                this.mDrawingBitmap = Bitmap.createBitmap(mBGBitmap.getWidth(), mBGBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            }
            TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setColor(colour);
            textPaint.setTextSize(30);
//            Typeface font = ResourcesCompat.getFont(context, R.font.font_new);
//            Typeface typeface = Typeface.create(font, Typeface.NORMAL);  //Typeface.DEFAULT
//            textPaint.setTypeface(typeface);

            String text = "Mahendra Ramchandra Gawade";
            StaticLayout mTextLayout = new StaticLayout(text, textPaint, mDrawingBitmap.getWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

            Canvas canvas = new Canvas(mDrawingBitmap);
            canvas.save();
            int[] location = new int[2];
//            edt.getLocationOnScreen(location);
            int edt_x = 65;
            int edt_y = 65;

//            int edt_x = location[0]+10;
//            int edt_y = location[1]-60;

//            Vector2D convertedTouch = new Vector2D(edt_x, edt_y).subtract(mPosition);
//            convertedTouch.scale(1.0f / mScale);
//            this.mDrawPath.moveTo(convertedTouch.x(), convertedTouch.y());
//            this.mX = convertedTouch.x();
//            this.mY = convertedTouch.y();
//            canvas.translate(edt_x, edt_y);
            mTextLayout.draw(canvas);
            canvas.restore();
            invalidate();
//            imageView2.setImageBitmap(previous_bitmap);
        }
    }

    public void pasteBitmap(Bitmap Cropped_image) {
        if(true) {
            if(mDrawingBitmap == null){
                this.mDrawingBitmap = Bitmap.createBitmap(mBGBitmap.getWidth(), mBGBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            }
            Canvas canvas = new Canvas(mDrawingBitmap);
            canvas.save();
//            int[] location = new int[2];
//            crop_image.getLocationOnScreen(location);
            int img_x = 15;
            int img_y = 15;

//            int img_y = location[1]-80;

//            Vector2D convertedTouch = new Vector2D(img_x, img_y).subtract(mPosition);
//            convertedTouch.scale(1.0f / mScale);
//            this.mDrawPath.moveTo(convertedTouch.x(), convertedTouch.y());
//            this.mX = convertedTouch.x();
//            this.mY = convertedTouch.y();

            canvas.translate(img_x, img_y);
            canvas.drawBitmap(Cropped_image, img_x, img_y, mSrcPaint);
            canvas.restore();
            invalidate();
        }
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

    private class MyDrawingPerformerListener implements DrawingPerformer.DrawingPerformerListener{

        @Override
        public void onDrawingPerformed(Bitmap bitmap, Rect rect) {
            mCleared = false;
            if (mActionStack !=  null)
                storeAction(rect);
            mCanvas.drawBitmap(bitmap, rect.left, rect.top, null);
            invalidate();
            if (mOnDrawListener != null)
                mOnDrawListener.onDraw();
        }

        @Override
        public void onDrawingPerformed(Path path, Paint paint, Rect rect) {
            mCleared = false;
            if (mActionStack !=  null)
                storeAction(rect);
            mCanvas.drawPath(path, paint);
            invalidate();
            if (mOnDrawListener != null)
                mOnDrawListener.onDraw();
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
    }
}
