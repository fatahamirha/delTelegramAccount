package org.telegram.android.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.*;
import android.widget.Checkable;
import org.telegram.android.R;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.core.wireframes.MessageWireframe;
import org.telegram.android.log.Logger;
import org.telegram.android.preview.AvatarLoader;
import org.telegram.android.preview.ImageHolder;
import org.telegram.android.preview.ImageReceiver;
import org.telegram.android.ui.FontController;
import org.telegram.android.ui.Placeholders;
import org.telegram.i18n.I18nUtil;

/**
 * Author: Korshakov Stepan
 * Created: 18.08.13 19:36
 */
public abstract class BaseMsgView extends BaseView implements Checkable {

    private static final String TAG = "BaseMsgView";

    private static final long AVATAR_FADE_TIME = 150;
    protected static final long FADE_ANIMATION_TIME = 150;
    protected static final long STATE_ANIMATION_TIME = 120;
    protected static final long GLOBAL_STATE_ANIMATION_TIME = 200;

    private static final int TOUCHED_NONE = 0;
    private static final int TOUCHED_OUTSIDE = 4;
    private static final int TOUCHED_AVATAR = 1;
    private static final int TOUCHED_BUBBLE = 2;
    private static final int TOUCHED_ADD_CONTACT = 3;

    private static int BUBBLE_PADDING;
    private static int AVATAR_OFFSET;
    private static int AVATAR_SIZE;
    private static int AVATAR_LEFT;
    private static int AVATAR_BOTTOM;
    private static int UNREAD_HEIGHT;
    private static boolean isLoaded;

    private TextPaint timeDivPaint;
    private TextPaint newDivPaint;
    private Paint avatarPaint;
    private Paint placeHolderBgPaint;
    private Bitmap placeholder;
    private ImageReceiver receiver;
    private ImageHolder avatarHolder;
    // private ImageReceiver receiver;
    private long avatarImageTime;

    private Paint selectorPaint;
    private Paint newMessagesPaint;

    private boolean showAvatar;
    private boolean isOut;
    private String timeDivText;
    private int timeDivMeasure;

    private String newDivText;
    private int newDivMeasure;

    private int bubbleContentWidth;
    private int bubbleContentHeight;
    private int unreadOffset;

    private Drawable bubbleInDrawable;
    private Drawable bubbleInDrawableOverlay;
    private Rect bubbleInPadding;
    private Drawable bubbleOutDrawable;
    private Drawable bubbleOutDrawableOverlay;
    private Rect bubbleOutPadding;
    private Drawable unreadBg;
    private Drawable unreadArrow;

    private Drawable currentBubbleDrawable;
    private Rect currentBubblePadding;

    private Drawable serviceDrawable;
    private Rect servicePadding;

    private Rect avatarRect;

    private int oldId = -1;

    private int touchedElement = TOUCHED_NONE;

    private boolean showTimeSeparator;

    private boolean showUnreadMessagesNotify;
    private int unreadMessagesCount;

    private MessageWireframe message;

    private OnClickListener onBubbleClick;
    private OnClickListener onAvatarClick;

    private boolean isBubblePressed = false;
    private boolean isBubbleChecked = false;

    private Rect rect = new Rect();

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable longClickRunnable = new Runnable() {
        @Override
        public void run() {
            if (performLongClick()) {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                touchedElement = TOUCHED_NONE;
            }
        }
    };

    public BaseMsgView(Context context) {
        super(context);
        init();
    }

    public BaseMsgView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BaseMsgView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private static void checkResources(Context context) {
        if (isLoaded) {
            return;
        }

        AVATAR_OFFSET = px(40);
        AVATAR_SIZE = px(42);
        AVATAR_LEFT = px(6);
        AVATAR_BOTTOM = px(4);
        BUBBLE_PADDING = px(40);
        UNREAD_HEIGHT = px(38);
        // unreadOffset = px(24);

        isLoaded = true;
    }


    protected void init() {

        checkResources(getContext());

        if (FontController.USE_SUBPIXEL) {
            timeDivPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        } else {
            timeDivPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        }
        timeDivPaint.setTextSize(getSp(15));
        timeDivPaint.setColor(0xffFFFFFF);
        timeDivPaint.setTypeface(FontController.loadTypeface(getContext(), "regular"));

        if (FontController.USE_SUBPIXEL) {
            newDivPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        } else {
            newDivPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        }
        newDivPaint.setTextSize(getSp(14));
        newDivPaint.setColor(0xffFFFFFF);
        newDivPaint.setTypeface(FontController.loadTypeface(getContext(), "medium"));

        avatarPaint = new Paint();

        placeHolderBgPaint = new Paint();

        bubbleInPadding = new Rect();
        bubbleOutPadding = new Rect();

        bubbleInDrawable = getResources().getDrawable(getInBubbleResource());
        bubbleOutDrawable = getResources().getDrawable(getOutBubbleResource());
        int resource = getOutPressedBubbleResource();
        if (resource != 0) {
            bubbleOutDrawableOverlay = getResources().getDrawable(resource);
        } else {
            bubbleOutDrawableOverlay = null;
        }

        resource = getInPressedBubbleResource();
        if (resource != 0) {
            bubbleInDrawableOverlay = getResources().getDrawable(resource);
        } else {
            bubbleInDrawableOverlay = null;
        }

        bubbleInDrawable.getPadding(bubbleInPadding);
        bubbleOutDrawable.getPadding(bubbleOutPadding);

        serviceDrawable = getResources().getDrawable(org.telegram.android.R.drawable.st_bubble_service);
        servicePadding = new Rect();
        serviceDrawable.getPadding(servicePadding);

        unreadBg = getResources().getDrawable(R.drawable.st_bubble_unread);
        unreadArrow = getResources().getDrawable(R.drawable.st_bubble_unread_arrow);

        avatarRect = new Rect();

        selectorPaint = new Paint();
        selectorPaint.setColor(0x6633b5e5);

        newMessagesPaint = new Paint();
        newMessagesPaint.setColor(0x66435266);

        receiver = new ImageReceiver() {
            @Override
            public void onImageReceived(ImageHolder mediaHolder, boolean intermediate) {
                releaseAvatar();
                avatarHolder = mediaHolder;
                if (intermediate) {
                    avatarImageTime = 0;
                } else {
                    avatarImageTime = SystemClock.uptimeMillis();
                }
                invalidate();
            }
        };
    }

    protected float scaleEasing(float src) {
        //return 1 - (1 - src) * (1 - src) * (1 - src) * (1 - src);
        return 1 - (1 - src) * (1 - src);
    }

    protected float progressEasing(float src) {
        float border = 0.25f;
        float factor = 0.5f;
        if (src < border) {
            return src * factor;
        }


        float separator = (src - border) / (1 - border);
        float base = src * factor;
        //float sec = src * src * src * src;
        float sec = (float) Math.sin(src * Math.PI / 2);

        return (1 - separator) * base + separator * sec;
    }

    protected float progressScaleEasing(float src) {
        if (src < 0.1) {
            return 1.0f;
        }
        // return src * src * src;
        return (1 - src);
    }

    protected float fadeEasing(float src) {
        return 1 - (1 - src) * (1 - src);
    }

    protected float easeStateFade(float src) {
        return 1 - (1 - src) * (1 - src);
    }

    protected float easeCheck(float src) {
        return (float) (-Math.pow(2, -10 * src) + 1);
    }

    public final void rebind() {
        bind(message, showTimeSeparator, showUnreadMessagesNotify, unreadMessagesCount);
    }

    public final boolean bind(MessageWireframe message, boolean showTime, boolean showUnread, int unreadMessagesCount) {
        boolean isUpdated = false;
        this.message = message;
        if (showTimeSeparator != showTime) {
            requestLayout();
        }
        if (showUnreadMessagesNotify != showUnread) {
            requestLayout();
        }

        this.showUnreadMessagesNotify = showUnread;
        this.unreadMessagesCount = unreadMessagesCount;

        if (showUnreadMessagesNotify) {
            newDivText = I18nUtil.getInstance().getPluralFormatted(R.plurals.st_new_messages, unreadMessagesCount);
        }

        showTimeSeparator = showTime;
        if (showTimeSeparator) {
            timeDivText = org.telegram.android.ui.TextUtil.formatDateLong(message.message.getDate());
        }
        bindCommonInt(message);
        bindCommon(message);
        Logger.d(TAG, "Bind old=" + oldId + ", new=" + message.message.getDatabaseId() + " of instance #" + hashCode());
        if (oldId != -1 && message.message.getDatabaseId() == oldId) {
            bindUpdateInt(message);
            bindUpdate(message);
            isUpdated = true;
        } else {
            bindNewInt(message);
            bindNewView(message);
        }
        oldId = message.message.getDatabaseId();
        refreshDrawableState();
        return isUpdated;
    }

    private void bindCommonInt(MessageWireframe message) {
        showAvatar = !message.message.isOut() && message.message.getPeerType() == PeerType.PEER_CHAT;
        isOut = message.message.isOut();

        if (isOut) {
            currentBubbleDrawable = bubbleOutDrawable;
            currentBubblePadding = bubbleOutPadding;
        } else {
            currentBubbleDrawable = bubbleInDrawable;
            currentBubblePadding = bubbleInPadding;
        }

        if (showAvatar) {
            placeholder = ((BitmapDrawable) getResources().getDrawable(R.drawable.st_user_placeholder_chat)).getBitmap();

            User user = message.senderUser;
            if (user != null) {
                placeHolderBgPaint.setColor(Placeholders.getBgColor(user.getUid()));
                if (user.getPhoto() instanceof TLLocalAvatarPhoto) {
                    TLLocalAvatarPhoto profilePhoto = (TLLocalAvatarPhoto) user.getPhoto();
                    if (profilePhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                        if (!application.getUiKernel().getAvatarLoader().requestAvatar(
                                profilePhoto.getPreviewLocation(),
                                AvatarLoader.TYPE_SMALL,
                                receiver)) {
                            releaseAvatar();
                        }
                    } else {
                        releaseAvatar();
                        application.getUiKernel().getAvatarLoader().cancelRequest(receiver);
                    }
                } else {
                    releaseAvatar();
                    application.getUiKernel().getAvatarLoader().cancelRequest(receiver);
                }
            } else {
                releaseAvatar();
                application.getUiKernel().getAvatarLoader().cancelRequest(receiver);
            }
        } else {
            releaseAvatar();
            application.getUiKernel().getAvatarLoader().cancelRequest(receiver);
        }
    }

    private void bindNewInt(MessageWireframe message) {

    }

    private void bindUpdateInt(MessageWireframe message) {

    }

    protected void bindNewView(MessageWireframe message) {

    }

    protected void bindUpdate(MessageWireframe message) {

    }

    protected void bindCommon(MessageWireframe message) {

    }

    public OnClickListener getOnAvatarClickListener() {
        return onAvatarClick;
    }

    public void setOnAvatarClickListener(OnClickListener onAvatarClick) {
        this.onAvatarClick = onAvatarClick;
    }

    public OnClickListener getOnBubbleClickListener() {
        return onBubbleClick;
    }

    public void setOnBubbleClickListener(OnClickListener onBubbleClick) {
        this.onBubbleClick = onBubbleClick;
    }

    public int getContentX() {
        if (isOut) {
            return getWidth() - currentBubblePadding.right - bubbleContentWidth;
        } else {
            if (showAvatar) {
                return AVATAR_OFFSET + currentBubblePadding.left;
            } else {
                return currentBubblePadding.left;
            }
        }
    }

    public int getContentY() {
        int offset = 0;
        if (showTimeSeparator) {
            offset += getPx(44);
        }
        offset += currentBubblePadding.top;
        return offset;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // long start = SystemClock.uptimeMillis();
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int bubbleMaxSize = width - BUBBLE_PADDING - currentBubblePadding.left - currentBubblePadding.right;
        if (showAvatar) {
            bubbleMaxSize -= AVATAR_OFFSET;
        }

        measureBubbleContent(bubbleMaxSize);

        int height = bubbleContentHeight + currentBubblePadding.bottom + currentBubblePadding.top;
        if (height < AVATAR_BOTTOM + AVATAR_SIZE && showAvatar) {
            height = AVATAR_BOTTOM + AVATAR_SIZE;
        }

        int realHeight = height;
        if (showTimeSeparator) {
            timeDivMeasure = (int) timeDivPaint.measureText(timeDivText);
            realHeight += getPx(44);
        }

        if (showUnreadMessagesNotify) {
            newDivMeasure = (int) newDivPaint.measureText(newDivText);
            newDivPaint.getTextBounds(newDivText, 0, newDivText.length(), rect);
            unreadOffset = (UNREAD_HEIGHT + Math.abs(rect.top)) / 2;
            realHeight += UNREAD_HEIGHT;
        }

        setMeasuredDimension(width, realHeight);

        if (showAvatar) {
            avatarRect.set(AVATAR_LEFT, height - AVATAR_BOTTOM - AVATAR_SIZE, AVATAR_LEFT + AVATAR_SIZE, height - AVATAR_BOTTOM);
        }
        // Logger.d(TAG, "onMeasure in " + (SystemClock.uptimeMillis() - start) + " ms at " + getClass().getSimpleName() + "#" + hashCode());
    }

    protected void setBubbleMeasuredContent(int w, int h) {
        bubbleContentWidth = w;
        bubbleContentHeight = h;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int bubbleStart, bubbleEnd;
            if (isOut) {
                bubbleEnd = getWidth();
                bubbleStart = 0;
                // bubbleStart = getWidth() - (bubbleContentWidth + currentBubblePadding.left + currentBubblePadding.right);
            } else {
                if (showAvatar) {
                    bubbleStart = AVATAR_OFFSET;
                    // bubbleEnd = AVATAR_OFFSET + bubbleContentWidth + currentBubblePadding.left + currentBubblePadding.right;
                    bubbleEnd = getWidth();
                } else {
                    bubbleStart = 0;
                    // bubbleEnd = bubbleContentWidth + currentBubblePadding.left + currentBubblePadding.right;
                    bubbleEnd = getWidth();
                }
            }

            boolean processed = false;
            if (this instanceof MessageContactView) {
                MessageContactView contactView = (MessageContactView) this;
                if (contactView.isShowAddButton()) {
                    if (event.getX() >= bubbleEnd - getPx(56) && event.getX() <= bubbleEnd) {
                        touchedElement = TOUCHED_ADD_CONTACT;
                        processed = true;
                    }
                }
            }

            if (!processed) {
                if (event.getX() >= bubbleStart && event.getX() <= bubbleEnd) {
                    touchedElement = TOUCHED_BUBBLE;
                } else if (event.getY() > getHeight() - AVATAR_BOTTOM - AVATAR_SIZE && event.getX() < AVATAR_OFFSET && showAvatar) {
                    touchedElement = TOUCHED_AVATAR;
                } else {
                    touchedElement = TOUCHED_NONE;
                }
            }

            if (touchedElement != TOUCHED_NONE) {
                handler.removeCallbacks(longClickRunnable);
                handler.postDelayed(longClickRunnable, ViewConfiguration.getLongPressTimeout());
            }
            applyDrawingState();
            invalidate();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (touchedElement != TOUCHED_NONE) {
                handler.removeCallbacks(longClickRunnable);
            }
            if (touchedElement == TOUCHED_BUBBLE) {
                if (onBubbleClick != null) {
                    playSoundEffect(SoundEffectConstants.CLICK);
                    onBubbleClick.onClick(this);
                }
            } else if (touchedElement == TOUCHED_AVATAR) {
                if (onAvatarClick != null) {
                    playSoundEffect(SoundEffectConstants.CLICK);
                    onAvatarClick.onClick(this);
                }
            } else if (touchedElement == TOUCHED_ADD_CONTACT) {
                MessageContactView contactView = (MessageContactView) this;
                contactView.onAddClicked();
            }
            touchedElement = TOUCHED_NONE;
            applyDrawingState();
            invalidate();
        } else if (event.getAction() == MotionEvent.ACTION_OUTSIDE || event.getAction() == MotionEvent.ACTION_CANCEL) {
            handler.removeCallbacks(longClickRunnable);
            touchedElement = TOUCHED_NONE;
            applyDrawingState();
            invalidate();
        }
        return super.onTouchEvent(event);
    }

    public void unbind() {
        releaseAvatar();
        oldId = -1;
        message = null;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        releaseAvatar();
        applyDrawingState();
        postInvalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        rebind();
        applyDrawingState();
        postInvalidate();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        bubbleOutDrawable.setState(getDrawableState());
        bubbleInDrawable.setState(getDrawableState());
        applyDrawingState();
        invalidate();
    }

    private void applyDrawingState() {
        if (isChecked()) {
            int[] drawableState = new int[]{android.R.attr.state_checked};
            bubbleOutDrawable.setState(drawableState);
            bubbleInDrawable.setState(drawableState);
            if (touchedElement == TOUCHED_BUBBLE) {
                isBubblePressed = true;
            } else {
                isBubblePressed = false;
            }
        } else if (touchedElement == TOUCHED_BUBBLE) {
            int[] drawableState = new int[]{android.R.attr.state_pressed};
            bubbleOutDrawable.setState(drawableState);
            bubbleInDrawable.setState(drawableState);
            isBubblePressed = true;
        } else {
            int[] drawableState = new int[0];
            bubbleOutDrawable.setState(drawableState);
            bubbleInDrawable.setState(drawableState);
            isBubblePressed = false;
        }
    }

    private void drawPlaceholder(Canvas canvas) {
        avatarPaint.setAlpha(255);
        rect.set(0, 0, placeholder.getWidth(), placeholder.getHeight());

        canvas.drawRect(avatarRect, placeHolderBgPaint);
        canvas.drawBitmap(placeholder, rect, avatarRect, avatarPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // long start = SystemClock.uptimeMillis();
        // applyDrawingState();

        boolean isAnimating = false;
        canvas.save();

        if (isBubbleChecked) {
            int topOffset = 1;

            if (showTimeSeparator) {
                topOffset += getPx(44);
            }
            if (showUnreadMessagesNotify) {
                topOffset += UNREAD_HEIGHT;
            }
            rect.set(0, topOffset, getWidth(), getHeight());
            canvas.drawRect(rect, selectorPaint);
        }

        if (showUnreadMessagesNotify) {
            unreadBg.setBounds(0, 0, getWidth(), UNREAD_HEIGHT);
            unreadBg.draw(canvas);
            int arrowTop = (UNREAD_HEIGHT - unreadArrow.getIntrinsicHeight()) / 2;
            unreadArrow.setBounds(getPx(20), arrowTop, getPx(20) + unreadArrow.getIntrinsicWidth(), arrowTop +
                    unreadArrow.getIntrinsicHeight());
            unreadArrow.draw(canvas);

            canvas.drawText(newDivText, (getWidth() - newDivMeasure) / 2, unreadOffset, newDivPaint);
            canvas.translate(0, UNREAD_HEIGHT);
        }

        if (showTimeSeparator) {
            serviceDrawable.setBounds(
                    getWidth() / 2 - timeDivMeasure / 2 - servicePadding.left,
                    getPx(44 - 8) - serviceDrawable.getIntrinsicHeight(),
                    getWidth() / 2 + timeDivMeasure / 2 + servicePadding.right,
                    getPx(44 - 8));
            serviceDrawable.draw(canvas);
            canvas.drawText(timeDivText, getWidth() / 2 - timeDivMeasure / 2, getPx(44 - 17), timeDivPaint);
            canvas.translate(0, getPx(44));
        }
        if (isOut) {
            canvas.translate(getWidth() - bubbleContentWidth - currentBubblePadding.left - currentBubblePadding.right, 0);
            currentBubbleDrawable.setBounds(0, 0,
                    bubbleContentWidth + currentBubblePadding.left + currentBubblePadding.right,
                    bubbleContentHeight + currentBubblePadding.top + currentBubblePadding.bottom);
            currentBubbleDrawable.draw(canvas);
            canvas.save();
            canvas.translate(currentBubblePadding.left, currentBubblePadding.top);

            // Logger.d(TAG, "onDraw:bubble in " + (SystemClock.uptimeMillis() - start) + " ms at " + getClass().getSimpleName() + "#" + hashCode());
            isAnimating = isAnimating | drawBubble(canvas);
            // Logger.d(TAG, "onDraw0 in " + (SystemClock.uptimeMillis() - start) + " ms at " + getClass().getSimpleName() + "#" + hashCode());
            canvas.restore();
            //canvas.translate(-currentBubblePadding.left, -currentBubblePadding.top);
            // Logger.d(TAG, "onDraw00 in " + (SystemClock.uptimeMillis() - start) + " ms at " + getClass().getSimpleName() + "#" + hashCode());

            if (isBubblePressed) {
                if (bubbleOutDrawableOverlay != null) {
                    bubbleOutDrawableOverlay.setBounds(0, 0,
                            bubbleContentWidth + currentBubblePadding.left + currentBubblePadding.right,
                            bubbleContentHeight + currentBubblePadding.top + currentBubblePadding.bottom);
                    bubbleOutDrawableOverlay.draw(canvas);
                }
            }
        } else {
            if (showAvatar) {
                if (avatarHolder != null) {
                    long animationTime = SystemClock.uptimeMillis() - avatarImageTime;
                    if (animationTime > AVATAR_FADE_TIME) {
                        avatarPaint.setAlpha(255);
                        canvas.drawBitmap(avatarHolder.getBitmap(), avatarRect.left, avatarRect.top, avatarPaint);
                    } else {
                        float animationPercent = fadeEasing((float) animationTime / AVATAR_FADE_TIME);
                        int avatarAlpha = (int) (animationPercent * 255);

                        drawPlaceholder(canvas);

                        avatarPaint.setAlpha(avatarAlpha);
                        canvas.drawBitmap(avatarHolder.getBitmap(), avatarRect.left, avatarRect.top, avatarPaint);

                        isAnimating = true;
                    }
                } else {
                    drawPlaceholder(canvas);
                }
                canvas.translate(AVATAR_OFFSET, 0);
            }
            currentBubbleDrawable.setBounds(0, 0,
                    bubbleContentWidth + currentBubblePadding.left + currentBubblePadding.right,
                    bubbleContentHeight + currentBubblePadding.top + currentBubblePadding.bottom);
            currentBubbleDrawable.draw(canvas);
            canvas.save();
            canvas.translate(currentBubblePadding.left, currentBubblePadding.top);
            isAnimating = isAnimating | drawBubble(canvas);
            canvas.restore();

            if (isBubblePressed) {
                if (bubbleInDrawableOverlay != null) {
                    bubbleInDrawableOverlay.setBounds(0, 0,
                            bubbleContentWidth + currentBubblePadding.left + currentBubblePadding.right,
                            bubbleContentHeight + currentBubblePadding.top + currentBubblePadding.bottom);
                    bubbleInDrawableOverlay.draw(canvas);
                }
            }
        }

        // Logger.d(TAG, "onDraw1 in " + (SystemClock.uptimeMillis() - start) + " ms at " + getClass().getSimpleName() + "#" + hashCode());

        canvas.restore();

        // Logger.d(TAG, "onDraw2 in " + (SystemClock.uptimeMillis() - start) + " ms at " + getClass().getSimpleName() + "#" + hashCode());

        if (isAnimating) {
            inavalidateForAnimation();
        }

        // Logger.d(TAG, "onDraw in " + (SystemClock.uptimeMillis() - start) + " ms at " + getClass().getSimpleName() + "#" + hashCode());
    }

    public int getBubbleContentWidth() {
        return bubbleContentWidth;
    }

    public int getBubbleContentHeight() {
        return bubbleContentHeight;
    }

    protected abstract void measureBubbleContent(int width);

    protected abstract int getInBubbleResource();

    protected abstract int getOutBubbleResource();

    protected int getOutPressedBubbleResource() {
        return 0;
    }

    protected int getInPressedBubbleResource() {
        return 0;
    }

    /**
     * Drawing buuble content
     *
     * @return isAnimating?
     */
    protected abstract boolean drawBubble(Canvas canvas);

    @Override
    public void setChecked(boolean checked) {
        isBubbleChecked = checked;
        refreshDrawableState();
        invalidate();
    }

    @Override
    public boolean isChecked() {
        return isBubbleChecked;
    }

    @Override
    public void toggle() {
        isBubbleChecked = !isBubbleChecked;
        invalidate();
    }

    public static int getBubblePadding(MessageWireframe wireframe, Rect bubblePadding, int width, TelegramApplication application1) {
        boolean showAvatar = !wireframe.message.isOut() && wireframe.message.getPeerType() == PeerType.PEER_CHAT;

        int bubbleMaxSize = width - BUBBLE_PADDING - bubblePadding.left - bubblePadding.right;
        if (showAvatar) {
            bubbleMaxSize -= AVATAR_OFFSET;
        }

        return bubbleMaxSize;
    }

    private void releaseAvatar() {
        if (avatarHolder != null) {
            avatarHolder.release();
            avatarHolder = null;
        }
    }
}