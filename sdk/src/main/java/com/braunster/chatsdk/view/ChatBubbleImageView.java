package com.braunster.chatsdk.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.braunster.chatsdk.R;
import com.braunster.chatsdk.Utils.Debug;
import com.braunster.chatsdk.Utils.ImageUtils;
import com.braunster.chatsdk.Utils.volley.VolleyUtils;
import com.braunster.chatsdk.network.BDefines;

/**
 * Created by braunster on 04/07/14.
 */
public class ChatBubbleImageView extends ImageView /*implements View.OnTouchListener */{

    public static final String TAG = ChatBubbleImageView.class.getSimpleName();
    public static final boolean DEBUG = Debug.ChatBubbleImageView;

    private Bitmap bubble, image;

    /** The max size that we would use for the image.*/
    public final float MAX_WIDTH = 200 * getResources().getDisplayMetrics().density;

    /** The size in pixels of the chat bubble point. i.e the the start of the bubble.*/
    private float pointSize = 4.2f * getResources().getDisplayMetrics().density;

    private int imagePadding = (int) (10 * getResources().getDisplayMetrics().density);

    private float roundRadius = /*18.5f*/ 6f * getResources().getDisplayMetrics().density;

    private boolean pressed = false;

    public static final int GRAVITY_LEFT = 0;
    public static final int GRAVITY_RIGHT = 1;

    public static final int BubbleDefaultPressedColor = Color.parseColor(BDefines.Defaults.BubbleDefaultColor);
    public static final int BubbleDefaultColor = Color.parseColor(BDefines.Defaults.BubbleDefaultPressedColor);


    private boolean showClickIndication = false;

    private int bubbleGravity = GRAVITY_LEFT, bubbleColor = Color.BLACK, pressedColor = BubbleDefaultPressedColor;

    public ChatBubbleImageView(Context context) {
        super(context);
    }

    public ChatBubbleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttrs(attrs);
    }

    public ChatBubbleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttrs(attrs);
        // Note style not supported.
    }

    private void getAttrs(AttributeSet attrs){
        TypedArray a=getContext().obtainStyledAttributes(
                attrs,
                R.styleable.ChatBubbleImageView);

        try {
            // Gravity of the bubble. Left or Right.
            bubbleGravity = a.getInt(
                    R.styleable.ChatBubbleImageView_bubble_gravity, GRAVITY_LEFT);

            // Bubble color. The color could be changed when loading the the image url.
            bubbleColor = a.getColor(R.styleable.ChatBubbleImageView_bubble_color, BubbleDefaultColor);

            // The color of the bubble when pressed.
            pressedColor = a.getColor(R.styleable.ChatBubbleImageView_bubble_pressed_color, BubbleDefaultPressedColor);

            imagePadding = a.getDimensionPixelSize(R.styleable.ChatBubbleImageView_image_padding, imagePadding);

            showClickIndication = a.getBoolean(R.styleable.ChatBubbleImageView_bubble_with_click_indicator, false);
        } finally {
            a.recycle();
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isInEditMode())
            return;

        if (bubble == null)
        {
            Log.e(TAG, "BUBBLE IS NULL");
            clearCanvas(canvas);
            return;
        }

        if (showClickIndication)
        {
            if (pressed)
                bubble = setBubbleColor(bubble, pressedColor);
            else bubble = setBubbleColor(bubble, bubbleColor);
        }

        if (bubbleGravity == GRAVITY_RIGHT)
        {
            canvas.drawBitmap(bubble, getMeasuredWidth() - bubble.getWidth(), 0 , null);

            if (image == null)
                return;

            canvas.drawBitmap(image,  imagePadding /2 , imagePadding /2 , null);
        }
        else
        {
            canvas.drawBitmap(bubble,0, 0 , null);

            if (image == null)
                return;

            canvas.drawBitmap(image, imagePadding /2 +  pointSize, imagePadding /2 , null);
        }
    }

    private void clearCanvas(Canvas canvas){
        canvas.drawColor(Color.TRANSPARENT);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (!showClickIndication)
            return;

        if (DEBUG) Log.v(TAG, "drawableStateChanged, "
                + (isPressed()?"Pressed":"Not Pressed")
                + ", " + (isFocused()?"Focused":"Not Focused")
                + ", " + (isEnabled()?"Enabled":"Not Enabled")
                + ".");

        if (!pressed && isPressed())
        {
            pressed = true;
            invalidate();
        }
        else if (pressed && !isPressed())
        {
            pressed = false;
            invalidate();
        }
    }

    private void setImage(Bitmap image) {
        this.image = image;
    }

    private void setBubble(Bitmap bubble) {
        this.bubble = bubble;
    }

    public void loadFromUrl(String url, int maxWidth, LoadDone loadDone){
       loadFromUrl(url, BubbleDefaultColor, maxWidth, loadDone);
    }

    public void loadFromUrl(String url, String color, int maxWidth, LoadDone loadDone){
        int bubbleColor = -1;
        try{
            bubbleColor = Color.parseColor(color);
        }
        catch (Exception e){}

        loadFromUrl(url, bubbleColor, maxWidth, loadDone);
    }

    public void loadFromUrl(final String url, final int color,final int maxWidth, final LoadDone loadDone){
        VolleyUtils.getImageLoader().get(url, new ImageLoader.ImageListener() {

            boolean firstOnResponse = true;

            @Override
            public void onResponse(final ImageLoader.ImageContainer response, boolean isImmediate) {
                if (DEBUG) Log.v(TAG, "Response,Url: " + url + ", Immediate: " + isImmediate);

                if (firstOnResponse){
                    if (loadDone != null)
                        loadDone.immediate(response.getBitmap() != null);

                    firstOnResponse = false;
                }

                if (response.getBitmap() != null) {

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

                            if (DEBUG) Log.d(TAG, "MaxWidth = " + maxWidth + " , MAX_WIDTH = " + MAX_WIDTH);
                            bubbleColor = color;

                            // Calculating the image width so we could scale it.
                            // If the wanted width is bigger then MAX_WIDTH we will use MAX_WIDTH not the given width.
                            final int width = (int) MAX_WIDTH;

                            if (DEBUG) Log.d(TAG, "new image size: " + width);

                            // The image bitmap from Volley.
                            Bitmap img = response.getBitmap();
                            Bitmap bubble;

                            // scaling the image to the needed width.
                            img = ImageUtils.scaleImage(img, width);

                            // Getting the bubble nine patch image for given size.
                            if (bubbleGravity == GRAVITY_LEFT)
                                bubble = ImageUtils.get_ninepatch(R.drawable.bubble_left, (int) (img.getWidth() + imagePadding + pointSize), (int) (img.getHeight() + imagePadding), getContext());
                            else
                                bubble = ImageUtils.get_ninepatch(R.drawable.bubble_right, (int) (img.getWidth() + imagePadding + pointSize), (int) (img.getHeight() + imagePadding), getContext());

                            if (DEBUG) Log.v(TAG, "Response,Url: " + url + ", Bubble Width: " + bubble.getWidth() + ", Height: " + bubble.getHeight());

                            if (!showClickIndication)
                                bubble = setBubbleColor(bubble, bubbleColor);

                            // Setting the bubble bitmap. It will be used in onDraw
                            setBubble(bubble);

                            // rounding the corners of the image.
                            img = getRoundedCornerBitmap(img, roundRadius);

                            // Setting the image bitmap. It will be used in onDraw
                            setImage(img);

                            // Notifying the view that we are done.
                            Message message = new Message();
                            message.arg1 = bubble.getWidth();
                            message.arg2 = bubble.getHeight();
                            message.obj = loadDone;
                            handler.sendMessage(message);
                        }
                    }).start();
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                if (DEBUG){
                    Log.e(TAG, "Image Load Error: " + error.getMessage());
                    error.printStackTrace();
                }
            }
        });
    }

    public void loadFromUrl(final String url, final int color,final int width, final int height, final LoadDone loadDone){
        bubble = null;
        image = null;

        final int bubbleWidth = (int) (width + imagePadding + pointSize);
        final int bubbleHeight = height + imagePadding;

        new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

                bubbleColor = color;

                // Getting the bubble nine patch image for given size.
                if (bubbleGravity == GRAVITY_LEFT)
                    bubble = ImageUtils.get_ninepatch(R.drawable.bubble_left, bubbleWidth,  bubbleHeight, getContext());
                else
                    bubble = ImageUtils.get_ninepatch(R.drawable.bubble_right, bubbleWidth, bubbleHeight, getContext());

                if (DEBUG) Log.v(TAG, "Response,Url: " + url + ", Bubble Width: " + bubble.getWidth() + ", Height: " + bubble.getHeight());

                if (!showClickIndication)
                {
                    Log.e(TAG, "BUBBLE SET COLOR");
                    bubble = setBubbleColor(bubble, color);
                }

                // Setting the bubble bitmap. It will be used in onDraw
                setBubble(bubble);

                handler.sendEmptyMessage(BUBBLE_IS_LOADDED);
            }
        }).start();


        VolleyUtils.getImageLoader().get(url, new ImageLoader.ImageListener() {

            boolean firstOnResponse = true;

            @Override
            public void onResponse(final ImageLoader.ImageContainer response, boolean isImmediate) {
                if (DEBUG) Log.v(TAG, "Response,Url: " + url + ", Immediate: " + isImmediate);

                if (firstOnResponse){
                    if (loadDone != null)
                        loadDone.immediate(response.getBitmap() != null);

                    firstOnResponse = false;
                }

                if (response.getBitmap() != null) {

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

                            // The image bitmap from Volley.
                            Bitmap img = response.getBitmap();

                            // scaling the image to the needed width.
                            img = ImageUtils.scaleImage(img, (int) MAX_WIDTH);

                            // rounding the corners of the image.
                            img = getRoundedCornerBitmap(img, roundRadius);

                            // Setting the image bitmap. It will be used in onDraw
                            setImage(img);

                            // Notifying the view that we are done.
                            Message message = new Message();
                            message.what = IMAGE_IS_LOADED;
                            message.arg1 = bubbleWidth;
                            message.arg2 = bubbleHeight;
                            message.obj = loadDone;
                            handler.sendMessage(message);
                        }
                    }).start();
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                if (DEBUG){
                    Log.e(TAG, "Image Load Error: " + error.getMessage());
                    error.printStackTrace();
                }
            }
        });
    }

    public static final int BUBBLE_IS_LOADDED = 0;
    public static final int IMAGE_IS_LOADED = 1;

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what)
            {
                case BUBBLE_IS_LOADDED:
                    invalidate();
                    break;

                case IMAGE_IS_LOADED:
                    ViewGroup.LayoutParams params = getLayoutParams();
                    params.width = msg.arg1;
                    params.height = msg.arg2;
                    // existing height is ok as is, no need to edit it
                    setLayoutParams(params);

                    ((LoadDone) msg.obj).onDone();

                    invalidate();
                    break;
            }

        }
    };

    public static Bitmap setBubbleColor(Bitmap bubble, int color){
        if (DEBUG) Log.v(TAG, "setBubbleColor, color: " + color);
        return replaceIntervalColor(bubble, 40, 75, 130, 140, 190, 210, color);
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        final Rect strikeRect = new Rect(0, 0, bitmap.getWidth() + 10, bitmap.getHeight() + 10);
        final RectF strokeRectF = new RectF(strikeRect);
        final Paint strokePaint = new Paint();
        strokePaint.setColor(0xff000000);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);

//        canvas.drawRoundRect(strokeRectF, roundPx, roundPx, strokePaint);
        canvas.drawRoundRect(rectF, pixels, pixels, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public static Bitmap replaceIntervalColor(Bitmap bitmap, int oldColor, int newColor){
        return replaceIntervalColor(bitmap,
                Color.red(oldColor), Color.red(oldColor),
                Color.green(oldColor), Color.green(oldColor),
                Color.blue(oldColor), Color.blue(oldColor),
                newColor);
    }

    public static Bitmap replaceIntervalColor(Bitmap bitmap,
                                              int redStart, int redEnd,
                                              int greenStart, int greenEnd,
                                              int blueStart, int blueEnd,
                                              int colorNew) {
        if (bitmap != null) {
            int picw = bitmap.getWidth();
            int pich = bitmap.getHeight();
            int[] pix = new int[picw * pich];
            bitmap.getPixels(pix, 0, picw, 0, 0, picw, pich);
            for (int y = 0; y < pich; y++) {
                for (int x = 0; x < picw; x++) {
                    int index = y * picw + x;
                    if (
                            ((Color.red(pix[index]) >= redStart)&&(Color.red(pix[index]) <= redEnd))&&
                                    ((Color.green(pix[index]) >= greenStart)&&(Color.green(pix[index]) <= greenEnd))&&
                                    ((Color.blue(pix[index]) >= blueStart)&&(Color.blue(pix[index]) <= blueEnd)) ||
                                    Color.alpha(pix[index]) > 0
                            ){

                        // If the alpha is not full that means we are on the edges of the bubbles so we create the new color with the old alpha.
                        if (Color.alpha(pix[index]) > 0)
                        {
//                            Log.i(TAG, "PIX: " + Color.alpha(pix[index]));
                            pix[index] = Color.argb(Color.alpha(pix[index]), Color.red(colorNew), Color.green(colorNew), Color.blue(colorNew));
                        }
                        else
                            pix[index] = colorNew;
                    }
                }
            }

            return Bitmap.createBitmap(pix, picw, pich,Bitmap.Config.ARGB_8888);
        }
        return null;
    }

    public interface LoadDone{
        public void onDone();
        public void immediate(boolean immediate);
    }

    public void setBubbleGravity(int bubbleGravity) {
        this.bubbleGravity = bubbleGravity;
    }

    public void setImagePadding(int imagePadding) {
        this.imagePadding = imagePadding;
    }

    public void setBubbleColor(int bubbleColor) {
        this.bubbleColor = bubbleColor;
    }

    public int getBubbleGravity() {
        return bubbleGravity;
    }

    public int getBubbleColor() {
        return bubbleColor;
    }

    public int getImagePadding() {
        return imagePadding;
    }

    public float getPointSize() {
        return pointSize;
    }
}
