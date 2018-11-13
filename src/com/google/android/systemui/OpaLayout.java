package com.google.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.RenderNodeAnimator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.OverviewProxyService;
import com.android.systemui.OverviewProxyService.OverviewProxyListener;
import com.android.systemui.R;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.plugins.statusbar.phone.NavBarButtonProvider.ButtonInterface;
import com.android.systemui.shared.system.NavigationBarCompat;
import com.android.systemui.statusbar.phone.ShadowKeyDrawable;
import com.android.systemui.statusbar.policy.KeyButtonDrawable;
import com.android.systemui.statusbar.policy.KeyButtonView;

import com.google.android.systemui.elmyra.sensors.GestureSensor.DetectionProperties;

import java.util.ArrayList;

public class OpaLayout extends FrameLayout implements ButtonInterface {

    private static final int ANIMATION_STATE_NONE = 0;
    private static final int ANIMATION_STATE_DIAMOND = 1;
    private static final int ANIMATION_STATE_RETRACT = 2;
    private static final int ANIMATION_STATE_OTHER = 3;

    private static final int GESTURE_STATE_NONE = 0;
    private static final int GESTURE_STATE_ACTIVE = 1;

    private static final int MIN_DIAMOND_DURATION = 100;
    private static final int COLLAPSE_ANIMATION_DURATION_RY = 83;
    private static final int COLLAPSE_ANIMATION_DURATION_BG = 100;
    private static final int LINE_ANIMATION_DURATION_Y = 275;
    private static final int LINE_ANIMATION_DURATION_X = 133;
    private static final int RETRACT_ANIMATION_DURATION = 190;
    private static final int DIAMOND_ANIMATION_DURATION = 200;
    private static final int HALO_ANIMATION_DURATION = 100;
    private static final int OPA_FADE_IN_DURATION = 50;
    private static final int OPA_FADE_OUT_DURATION = 250;

    private static final int DOTS_RESIZE_DURATION = 200;
    private static final int HOME_RESIZE_DURATION = 83;

    private static final int HOME_REAPPEAR_ANIMATION_OFFSET = 33;
    private static final int HOME_REAPPEAR_DURATION = 150;

    private static final float DIAMOND_DOTS_SCALE_FACTOR = 0.8f;
    private static final float DIAMOND_HOME_SCALE_FACTOR = 0.625f;
    private static final float HALO_SCALE_FACTOR = 0.47619048f;

    private KeyButtonView mHome;

    private int mAnimationState;
    private final ArraySet<Animator> mCurrentAnimators;
    private final ArrayList<View> mAnimatedViews;

    private int mGestureState;
    private long mGestureAnimationSetDuration;
    private AnimatorSet mGestureAnimatorSet;
    private AnimatorSet mGestureLineSet;

    private boolean mDelayTouchFeedback;
    private boolean mIsVertical;
    private boolean mIsPressed;
    private boolean mLongClicked;
    private boolean mOpaEnabled;
    private boolean mOpaEnabledNeedsUpdate;
    private boolean mWindowVisible;
    private long mStartTime;

    private View mRed;
    private View mBlue;
    private View mGreen;
    private View mYellow;
    private ImageView mHalo;
    private int mHaloDiameter;
    private ImageView mWhite;
    private ImageView mWhiteCutout;

    private View mTop;
    private View mRight;
    private View mLeft;
    private View mBottom;

    private Resources mResources;

    private final Runnable mCheckLongPress;
    private final Runnable mRetract;

    private final Interpolator HOME_DISAPPEAR_INTERPOLATOR;
    private final Interpolator mDiamondInterpolator;

    private final Runnable mDiamondAnimation;
    private boolean mDiamondAnimationDelayed;

    private int mScrollTouchSlop;
    private int mTouchDownX;
    private int mTouchDownY;

    private final OverviewProxyListener mOverviewProxyListener;
    private OverviewProxyService mOverviewProxyService;

    public OpaLayout(Context context) {
        this(context, null);
    }

    public OpaLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OpaLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        HOME_DISAPPEAR_INTERPOLATOR = new PathInterpolator(0.65f, 0.0f, 1.0f, 1.0f);
        mDiamondInterpolator = new PathInterpolator(0.2f, 0.0f, 0.2f, 1.0f);
        mCurrentAnimators = new ArraySet();
        mAnimatedViews = new ArrayList();
        mAnimationState = ANIMATION_STATE_NONE;
        mGestureState = GESTURE_STATE_NONE;
        mRetract = new Runnable() {
            @Override
            public void run() {
                cancelCurrentAnimation();
                startRetractAnimation();
            }
        };
        mCheckLongPress = new Runnable() {
            @Override
            public void run() {
                if (mIsPressed) {
                    mLongClicked = true;
                }
            }
        };
        mOverviewProxyListener = new OverviewProxyListener() {
            @Override
            public void onConnectionChanged(boolean isConnected) {
                updateOpaLayout();
            }

            @Override
            public void onInteractionFlagsChanged(int flags) {
                updateOpaLayout();
            }
        };
        mDiamondAnimation = new Runnable() {
            @Override
            public void run() {
                startDiamondAnimation();
            }
        };
        mScrollTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public OpaLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();

        mResources = getResources();

        mRed = findViewById(R.id.red);
        mBlue = findViewById(R.id.blue);
        mYellow = findViewById(R.id.yellow);
        mGreen = findViewById(R.id.green);
        mWhite = (ImageView) findViewById(R.id.white);
        mWhiteCutout = (ImageView) findViewById(R.id.white_cutout);
        mHalo = (ImageView) findViewById(R.id.halo);
        mHome = (KeyButtonView) findViewById(R.id.home_button);

        Context lightContext = new ContextThemeWrapper(getContext(), R.style.DualToneLightTheme);
        mHalo.setImageDrawable(KeyButtonDrawable.create(lightContext, lightContext.getDrawable(R.drawable.halo), new ContextThemeWrapper(getContext(), R.style.DualToneDarkTheme).getDrawable(R.drawable.halo), false));
        mHaloDiameter = mResources.getDimensionPixelSize(R.dimen.halo_diameter);
        Paint cutoutPaint = new Paint();
        cutoutPaint.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));
        mWhiteCutout.setLayerType(2, cutoutPaint);

        mAnimatedViews.add(mBlue);
        mAnimatedViews.add(mRed);
        mAnimatedViews.add(mYellow);
        mAnimatedViews.add(mGreen);
        mAnimatedViews.add(mWhite);
        mAnimatedViews.add(mWhiteCutout);
        mAnimatedViews.add(mHalo);

        mOpaEnabledNeedsUpdate = true;
        mOverviewProxyService = (OverviewProxyService) Dependency.get(OverviewProxyService.class);
    }

    public void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mWindowVisible = visibility == 0;
        if (visibility == 0) {
            updateOpaLayout();
            return;
        }
        cancelCurrentAnimation();
        skipToStartingValue();
    }

    public void setOnLongClickListener(View.OnLongClickListener l) {
        mHome.setOnLongClickListener(l);
    }

    public void setOnTouchListener(View.OnTouchListener l) {
        mHome.setOnTouchListener(l);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!getOpaEnabled() || !ValueAnimator.areAnimatorsEnabled() || this.mGestureState != GESTURE_STATE_NONE) {
            return false;
        }
        boolean exceededTouchSlopY = true;
        boolean isRetracting;
        switch (ev.getAction()) {
            case 0:
                this.mTouchDownX = (int) ev.getRawX();
                this.mTouchDownY = (int) ev.getRawY();
                isRetracting = false;
                if (!this.mCurrentAnimators.isEmpty()) {
                    if (this.mAnimationState != 2) {
                        return false;
                    }
                    endCurrentAnimation();
                    isRetracting = true;
                }
                this.mStartTime = SystemClock.elapsedRealtime();
                this.mLongClicked = false;
                this.mIsPressed = true;
                removeCallbacks(this.mDiamondAnimation);
                removeCallbacks(this.mRetract);
                removeCallbacks(this.mCheckLongPress);
                postDelayed(this.mCheckLongPress, (long) ViewConfiguration.getLongPressTimeout());
                if (this.mDelayTouchFeedback && !isRetracting) {
                    this.mDiamondAnimationDelayed = true;
                    postDelayed(this.mDiamondAnimation, (long) ViewConfiguration.getTapTimeout());
                    break;
                }
                this.mDiamondAnimationDelayed = false;
                startDiamondAnimation();
                break;
            case 1:
            case 3:
                if (this.mDiamondAnimationDelayed) {
                    if (this.mIsPressed && !this.mLongClicked) {
                        postDelayed(this.mRetract, 200);
                    }
                } else if (this.mAnimationState == 1) {
                    long targetTime = 100 - (SystemClock.elapsedRealtime() - this.mStartTime);
                    removeCallbacks(this.mRetract);
                    postDelayed(this.mRetract, targetTime);
                    removeCallbacks(this.mDiamondAnimation);
                    removeCallbacks(this.mCheckLongPress);
                    return false;
                } else {
                    if (!this.mIsPressed || this.mLongClicked) {
                        exceededTouchSlopY = false;
                    }
                    if (exceededTouchSlopY) {
                        this.mRetract.run();
                    }
                }
                this.mIsPressed = false;
                break;
            case 2:
                int quickStepTouchSlopPx;
                int quickScrubTouchSlopPx;
                int abs = Math.abs(((int) ev.getRawX()) - this.mTouchDownX);
                if (this.mIsVertical) {
                    quickStepTouchSlopPx = NavigationBarCompat.getQuickStepTouchSlopPx();
                } else {
                    quickStepTouchSlopPx = NavigationBarCompat.getQuickScrubTouchSlopPx();
                }
                isRetracting = abs > quickStepTouchSlopPx;
                quickStepTouchSlopPx = Math.abs(((int) ev.getRawY()) - this.mTouchDownY);
                if (this.mIsVertical) {
                    quickScrubTouchSlopPx = NavigationBarCompat.getQuickScrubTouchSlopPx();
                } else {
                    quickScrubTouchSlopPx = NavigationBarCompat.getQuickStepTouchSlopPx();
                }
                if (quickStepTouchSlopPx <= quickScrubTouchSlopPx) {
                    exceededTouchSlopY = false;
                }
                if (isRetracting || exceededTouchSlopY) {
                    abortCurrentGesture();
                    break;
                }
        }
        return false;
    }

    public void setAccessibilityDelegate(AccessibilityDelegate delegate) {
        super.setAccessibilityDelegate(delegate);
        mHome.setAccessibilityDelegate(delegate);
    }

    public void setImageDrawable(Drawable drawable) {
        mWhite.setImageDrawable(drawable);
        mWhiteCutout.setImageDrawable(drawable);
    }

    public void abortCurrentGesture() {
        mHome.abortCurrentGesture();
        mIsPressed = false;
        mLongClicked = false;
        mDiamondAnimationDelayed = false;
        removeCallbacks(mDiamondAnimation);
        removeCallbacks(mCheckLongPress);
        if (mAnimationState == 3 || mAnimationState == 1) {
            mRetract.run();
        }
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateOpaLayout();
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mOverviewProxyService.addCallback(mOverviewProxyListener);
        updateOpaLayout();
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mOverviewProxyService.removeCallback(mOverviewProxyListener);
    }

    private void startDiamondAnimation() {
        if (allowAnimations()) {
            mCurrentAnimators.clear();
            setDotsVisible();
            mCurrentAnimators.addAll(getDiamondAnimatorSet());
            mAnimationState = ANIMATION_STATE_DIAMOND;
            startAll(mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    private void startRetractAnimation() {
        if (allowAnimations()) {
            mCurrentAnimators.clear();
            mCurrentAnimators.addAll(getRetractAnimatorSet());
            mAnimationState = ANIMATION_STATE_RETRACT;
            startAll(mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    private void startLineAnimation() {
        if (allowAnimations()) {
            mCurrentAnimators.clear();
            mCurrentAnimators.addAll(getLineAnimatorSet());
            mAnimationState = ANIMATION_STATE_OTHER;
            startAll(mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    private void startCollapseAnimation() {
        if (allowAnimations()) {
            mCurrentAnimators.clear();
            mCurrentAnimators.addAll(getCollapseAnimatorSet());
            mAnimationState = ANIMATION_STATE_OTHER;
            startAll(mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    private void startAll(ArraySet<Animator> animators) {
        for (int i = animators.size() - 1; i >= 0; i--) {
            ((Animator) animators.valueAt(i)).start();
        }
    }

    private boolean allowAnimations() {
        return isAttachedToWindow() && this.mWindowVisible;
    }

    private ArraySet<Animator> getDiamondAnimatorSet() {
        ArraySet<Animator> animators = new ArraySet();
        animators.add(OpaUtils.getDeltaAnimatorY(this.mTop, this.mDiamondInterpolator, -OpaUtils.getPxVal(this.mResources, R.dimen.opa_diamond_translation), 200));
        animators.add(OpaUtils.getScaleAnimatorX(this.mTop, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorY(this.mTop, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getDeltaAnimatorY(this.mBottom, this.mDiamondInterpolator, OpaUtils.getPxVal(this.mResources, R.dimen.opa_diamond_translation), 200));
        animators.add(OpaUtils.getScaleAnimatorX(this.mBottom, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorY(this.mBottom, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getDeltaAnimatorX(this.mLeft, this.mDiamondInterpolator, -OpaUtils.getPxVal(this.mResources, R.dimen.opa_diamond_translation), 200));
        animators.add(OpaUtils.getScaleAnimatorX(this.mLeft, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorY(this.mLeft, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getDeltaAnimatorX(this.mRight, this.mDiamondInterpolator, OpaUtils.getPxVal(this.mResources, R.dimen.opa_diamond_translation), 200));
        animators.add(OpaUtils.getScaleAnimatorX(this.mRight, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorY(this.mRight, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorX(this.mWhite, 0.625f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorY(this.mWhite, 0.625f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorX(this.mWhiteCutout, 0.625f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorY(this.mWhiteCutout, 0.625f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorX(this.mHalo, 0.47619048f, 100, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorY(this.mHalo, 0.47619048f, 100, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getAlphaAnimator(this.mHalo, 0.0f, 100, Interpolators.FAST_OUT_SLOW_IN));
        getLongestAnim(animators).addListener((Animator.AnimatorListener)new AnimatorListenerAdapter() {
            public void onAnimationCancel(final Animator animator) {
                mCurrentAnimators.clear();
            }

            public void onAnimationEnd(final Animator animator) {
                startLineAnimation();
            }
        });
        return animators;
    }

    private ArraySet<Animator> getRetractAnimatorSet() {
        ArraySet<Animator> animators = new ArraySet<Animator>();
        animators.add(OpaUtils.getTranslationAnimatorX(this.mRed, OpaUtils.INTERPOLATOR_40_OUT, 190));
        animators.add(OpaUtils.getTranslationAnimatorY(this.mRed, OpaUtils.INTERPOLATOR_40_OUT, 190));
        animators.add(OpaUtils.getScaleAnimatorX(this.mRed, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(this.mRed, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getTranslationAnimatorX(this.mBlue, OpaUtils.INTERPOLATOR_40_OUT, 190));
        animators.add(OpaUtils.getTranslationAnimatorY(this.mBlue, OpaUtils.INTERPOLATOR_40_OUT, 190));
        animators.add(OpaUtils.getScaleAnimatorX(this.mBlue, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(this.mBlue, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getTranslationAnimatorX(this.mGreen, OpaUtils.INTERPOLATOR_40_OUT, 190));
        animators.add(OpaUtils.getTranslationAnimatorY(this.mGreen, OpaUtils.INTERPOLATOR_40_OUT, 190));
        animators.add(OpaUtils.getScaleAnimatorX(this.mGreen, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(this.mGreen, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getTranslationAnimatorX(this.mYellow, OpaUtils.INTERPOLATOR_40_OUT, 190));
        animators.add(OpaUtils.getTranslationAnimatorY(this.mYellow, OpaUtils.INTERPOLATOR_40_OUT, 190));
        animators.add(OpaUtils.getScaleAnimatorX(this.mYellow, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(this.mYellow, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorX(this.mWhite, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(this.mWhite, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorX(this.mWhiteCutout, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(this.mWhiteCutout, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorX(this.mHalo, 1.0f, 190, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorY(this.mHalo, 1.0f, 190, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getAlphaAnimator(this.mHalo, 1.0f, 190, Interpolators.FAST_OUT_SLOW_IN));
        getLongestAnim(animators).addListener((Animator.AnimatorListener) new AnimatorListenerAdapter() {
            public void onAnimationEnd(final Animator animator) {
                OpaLayout.this.mCurrentAnimators.clear();
                OpaLayout.this.mAnimationState = OpaLayout.ANIMATION_STATE_NONE;
            }
        });
        return animators;
    }

    private ArraySet<Animator> getCollapseAnimatorSet() {
        Animator translationAnimatorY;
        ArraySet<Animator> animators = new ArraySet();
        if (this.mIsVertical) {
            translationAnimatorY = OpaUtils.getTranslationAnimatorY(this.mRed, OpaUtils.INTERPOLATOR_40_OUT, 133);
        } else {
            translationAnimatorY = OpaUtils.getTranslationAnimatorX(this.mRed, OpaUtils.INTERPOLATOR_40_OUT, 133);
        }
        animators.add(translationAnimatorY);
        animators.add(OpaUtils.getScaleAnimatorX(this.mRed, 1.0f, 200, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(this.mRed, 1.0f, 200, OpaUtils.INTERPOLATOR_40_OUT));
        if (this.mIsVertical) {
            translationAnimatorY = OpaUtils.getTranslationAnimatorY(this.mBlue, OpaUtils.INTERPOLATOR_40_OUT, 150);
        } else {
            translationAnimatorY = OpaUtils.getTranslationAnimatorX(this.mBlue, OpaUtils.INTERPOLATOR_40_OUT, 150);
        }
        animators.add(translationAnimatorY);
        animators.add(OpaUtils.getScaleAnimatorX(this.mBlue, 1.0f, 200, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(this.mBlue, 1.0f, 200, OpaUtils.INTERPOLATOR_40_OUT));
        if (this.mIsVertical) {
            translationAnimatorY = OpaUtils.getTranslationAnimatorY(this.mYellow, OpaUtils.INTERPOLATOR_40_OUT, 133);
        } else {
            translationAnimatorY = OpaUtils.getTranslationAnimatorX(this.mYellow, OpaUtils.INTERPOLATOR_40_OUT, 133);
        }
        animators.add(translationAnimatorY);
        animators.add(OpaUtils.getScaleAnimatorX(this.mYellow, 1.0f, 200, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(this.mYellow, 1.0f, 200, OpaUtils.INTERPOLATOR_40_OUT));
        if (this.mIsVertical) {
            translationAnimatorY = OpaUtils.getTranslationAnimatorY(this.mGreen, OpaUtils.INTERPOLATOR_40_OUT, 150);
        } else {
            translationAnimatorY = OpaUtils.getTranslationAnimatorX(this.mGreen, OpaUtils.INTERPOLATOR_40_OUT, 150);
        }
        animators.add(translationAnimatorY);
        animators.add(OpaUtils.getScaleAnimatorX(this.mGreen, 1.0f, 200, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(this.mGreen, 1.0f, 200, OpaUtils.INTERPOLATOR_40_OUT));
        Animator homeScaleX = OpaUtils.getScaleAnimatorX(this.mWhite, 1.0f, 150, Interpolators.FAST_OUT_SLOW_IN);
        Animator homeScaleY = OpaUtils.getScaleAnimatorY(this.mWhite, 1.0f, 150, Interpolators.FAST_OUT_SLOW_IN);
        Animator homeCutoutScaleX = OpaUtils.getScaleAnimatorX(this.mWhiteCutout, 1.0f, 150, Interpolators.FAST_OUT_SLOW_IN);
        Animator homeCutoutScaleY = OpaUtils.getScaleAnimatorY(this.mWhiteCutout, 1.0f, 150, Interpolators.FAST_OUT_SLOW_IN);
        Animator haloScaleX = OpaUtils.getScaleAnimatorX(this.mHalo, 1.0f, 150, Interpolators.FAST_OUT_SLOW_IN);
        Animator haloScaleY = OpaUtils.getScaleAnimatorY(this.mHalo, 1.0f, 150, Interpolators.FAST_OUT_SLOW_IN);
        Animator haloAlpha = OpaUtils.getAlphaAnimator(this.mHalo, 1.0f, 150, Interpolators.FAST_OUT_SLOW_IN);
        homeScaleX.setStartDelay(33);
        homeScaleY.setStartDelay(33);
        homeCutoutScaleX.setStartDelay(33);
        homeCutoutScaleY.setStartDelay(33);
        haloScaleX.setStartDelay(33);
        haloScaleY.setStartDelay(33);
        haloAlpha.setStartDelay(33);
        animators.add(homeScaleX);
        animators.add(homeScaleY);
        animators.add(homeCutoutScaleX);
        animators.add(homeCutoutScaleY);
        animators.add(haloScaleX);
        animators.add(haloScaleY);
        animators.add(haloAlpha);
        getLongestAnim(animators).addListener((Animator.AnimatorListener) new AnimatorListenerAdapter() {
            public void onAnimationEnd(final Animator animator) {
                mCurrentAnimators.clear();
                mAnimationState = OpaLayout.ANIMATION_STATE_NONE;
            }
        });
        return animators;
    }

    private ArraySet<Animator> getLineAnimatorSet() {
        ArraySet<Animator> animators = new ArraySet();
        if (this.mIsVertical) {
            animators.add(OpaUtils.getDeltaAnimatorY(this.mRed, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_x_trans_ry), 225));
            animators.add(OpaUtils.getDeltaAnimatorX(this.mRed, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_y_translation), 133));
            animators.add(OpaUtils.getDeltaAnimatorY(this.mBlue, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_x_trans_bg), 225));
            animators.add(OpaUtils.getDeltaAnimatorY(this.mYellow, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_x_trans_ry), 225));
            animators.add(OpaUtils.getDeltaAnimatorX(this.mYellow, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_y_translation), 133));
            animators.add(OpaUtils.getDeltaAnimatorY(this.mGreen, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_x_trans_bg), 225));
        } else {
            animators.add(OpaUtils.getDeltaAnimatorX(this.mRed, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_x_trans_ry), 225));
            animators.add(OpaUtils.getDeltaAnimatorY(this.mRed, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_y_translation), 133));
            animators.add(OpaUtils.getDeltaAnimatorX(this.mBlue, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_x_trans_bg), 225));
            animators.add(OpaUtils.getDeltaAnimatorX(this.mYellow, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_x_trans_ry), 225));
            animators.add(OpaUtils.getDeltaAnimatorY(this.mYellow, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_y_translation), 133));
            animators.add(OpaUtils.getDeltaAnimatorX(this.mGreen, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_x_trans_bg), 225));
        }
        animators.add(OpaUtils.getScaleAnimatorX(this.mWhite, 0.0f, 83, this.HOME_DISAPPEAR_INTERPOLATOR));
        animators.add(OpaUtils.getScaleAnimatorY(this.mWhite, 0.0f, 83, this.HOME_DISAPPEAR_INTERPOLATOR));
        animators.add(OpaUtils.getScaleAnimatorX(this.mWhiteCutout, 0.0f, 83, this.HOME_DISAPPEAR_INTERPOLATOR));
        animators.add(OpaUtils.getScaleAnimatorY(this.mWhiteCutout, 0.0f, 83, this.HOME_DISAPPEAR_INTERPOLATOR));
        animators.add(OpaUtils.getScaleAnimatorX(this.mHalo, 0.0f, 83, this.HOME_DISAPPEAR_INTERPOLATOR));
        animators.add(OpaUtils.getScaleAnimatorY(this.mHalo, 0.0f, 83, this.HOME_DISAPPEAR_INTERPOLATOR));
        getLongestAnim(animators).addListener((Animator.AnimatorListener)new AnimatorListenerAdapter() {
            public void onAnimationCancel(final Animator animator) {
                mCurrentAnimators.clear();
            }

            public void onAnimationEnd(final Animator animator) {
                startCollapseAnimation();
            }
        });
        return animators;
    }

    public boolean getOpaEnabled() {
        if (this.mOpaEnabledNeedsUpdate) {
            ((AssistManagerGoogle) Dependency.get(AssistManager.class)).dispatchOpaEnabledState();
            if (this.mOpaEnabledNeedsUpdate) {
                Log.w("OpaLayout", "mOpaEnabledNeedsUpdate not cleared by AssistManagerGoogle!");
            }
        }
        return this.mOpaEnabled;
    }

    public void setOpaEnabled(boolean enabled) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Setting opa enabled to ");
        stringBuilder.append(enabled);
        Log.i("OpaLayout", stringBuilder.toString());
        this.mOpaEnabled = enabled;
        this.mOpaEnabledNeedsUpdate = false;
        updateOpaLayout();
    }

    public void updateOpaLayout() {
        boolean showQuickStepIcons = this.mOverviewProxyService.shouldShowSwipeUpUI();
        int i = 0;
        boolean haloShown = this.mOpaEnabled && !showQuickStepIcons;
        ImageView imageView = this.mHalo;
        if (!haloShown) {
            i = 4;
        }
        imageView.setVisibility(i);
        LayoutParams lp = (LayoutParams) this.mWhite.getLayoutParams();
        int i2 = -1;
        lp.width = showQuickStepIcons ? -1 : this.mHaloDiameter;
        if (!showQuickStepIcons) {
            i2 = this.mHaloDiameter;
        }
        lp.height = i2;
        this.mWhite.setLayoutParams(lp);
        this.mWhiteCutout.setLayoutParams(lp);
    }

    private void cancelCurrentAnimation() {
        if (!mCurrentAnimators.isEmpty()) {
            for (int i = mCurrentAnimators.size() - 1; i >= 0; i--) {
                Animator a = (Animator) mCurrentAnimators.valueAt(i);
                a.removeAllListeners();
                a.cancel();
            }
            mCurrentAnimators.clear();
            mAnimationState = ANIMATION_STATE_NONE;
        }
        if (mGestureAnimatorSet != null) {
            mGestureAnimatorSet.cancel();
            mGestureState = GESTURE_STATE_NONE;
        }
    }

    private void endCurrentAnimation() {
        if (!mCurrentAnimators.isEmpty()) {
            for (int i = mCurrentAnimators.size() - 1; i >= 0; i--) {
                Animator a = (Animator) mCurrentAnimators.valueAt(i);
                a.removeAllListeners();
                a.end();
            }
            mCurrentAnimators.clear();
        }
        mAnimationState = ANIMATION_STATE_NONE;
    }

    private Animator getLongestAnim(ArraySet<Animator> animators) {
        long longestDuration = Long.MIN_VALUE;
        Animator longestAnim = null;
        for (int i = animators.size() - 1; i >= 0; i--) {
            Animator a = (Animator) animators.valueAt(i);
            if (a.getTotalDuration() > longestDuration) {
                longestAnim = a;
                longestDuration = a.getTotalDuration();
            }
        }
        return longestAnim;
    }

    private void setDotsVisible() {
        int size = mAnimatedViews.size();
        for (int i = 0; i < size; i++) {
            ((View) mAnimatedViews.get(i)).setAlpha(1.0f);
        }
    }

    private void skipToStartingValue() {
        int size = mAnimatedViews.size();
        for (int i = 0; i < size; i++) {
            View v = (View) mAnimatedViews.get(i);
            v.setScaleY(1.0f);
            v.setScaleX(1.0f);
            v.setTranslationY(0.0f);
            v.setTranslationX(0.0f);
            v.setAlpha(0.0f);
        }
        mHalo.setAlpha(1.0f);
        mWhite.setAlpha(1.0f);
        mWhiteCutout.setAlpha(1.0f);
        mAnimationState = ANIMATION_STATE_NONE;
        mGestureState = GESTURE_STATE_NONE;
    }

    public void setVertical(boolean vertical) {
        if (!(mIsVertical == vertical || mGestureAnimatorSet == null)) {
            mGestureAnimatorSet.cancel();
            mGestureAnimatorSet = null;
            skipToStartingValue();
        }
        mIsVertical = vertical;
        mHome.setVertical(vertical);
        if (mIsVertical) {
            mTop = mGreen;
            mBottom = mBlue;
            mRight = mYellow;
            mLeft = mRed;
            return;
        }
        mTop = mRed;
        mBottom = mYellow;
        mLeft = mBlue;
        mRight = mGreen;
    }

    public void setDarkIntensity(float intensity) {
        if (mWhite.getDrawable() instanceof KeyButtonDrawable) {
            ((KeyButtonDrawable) mWhite.getDrawable()).setDarkIntensity(intensity);
        }
        ((KeyButtonDrawable) mHalo.getDrawable()).setDarkIntensity(intensity);
        mWhite.invalidate();
        mHalo.invalidate();
        mHome.setDarkIntensity(intensity);
    }

    public void setDelayTouchFeedback(boolean shouldDelay) {
        mHome.setDelayTouchFeedback(shouldDelay);
        mDelayTouchFeedback = shouldDelay;
    }

    public void onRelease() {
        if (mAnimationState == ANIMATION_STATE_NONE && mGestureState == GESTURE_STATE_ACTIVE) {
            if (mGestureAnimatorSet != null) {
                mGestureAnimatorSet.cancel();
            }
            mGestureState = GESTURE_STATE_NONE;
            startRetractAnimation();
        }
    }

    public void onProgress(float progress, int stage) {
        if (mGestureState != 2 && allowAnimations()) {
            if (mAnimationState == 2) {
                endCurrentAnimation();
            }
            if (mAnimationState == ANIMATION_STATE_NONE) {
                if (mGestureAnimatorSet == null) {
                    mGestureAnimatorSet = getGestureAnimatorSet();
                    mGestureAnimationSetDuration = mGestureAnimatorSet.getTotalDuration();
                }
                mGestureAnimatorSet.setCurrentPlayTime((long) (((float) (mGestureAnimationSetDuration - 1)) * progress));
                if (progress == 0.0f) {
                    mGestureState = GESTURE_STATE_NONE;
                } else {
                    mGestureState = GESTURE_STATE_ACTIVE;
                }
            }
        }
    }

    public void onResolve(DetectionProperties properties) {
        if (mAnimationState == ANIMATION_STATE_NONE) {
            if (mGestureState != 1 || mGestureAnimatorSet == null || mGestureAnimatorSet.isStarted()) {
                skipToStartingValue();
            } else {
                mGestureAnimatorSet.start();
                mGestureState = 2;
            }
        }
    }

    private AnimatorSet getGestureAnimatorSet() {
        if (this.mGestureLineSet != null) {
            this.mGestureLineSet.removeAllListeners();
            this.mGestureLineSet.cancel();
            return this.mGestureLineSet;
        }
        this.mGestureLineSet = new AnimatorSet();
        ObjectAnimator homeAnimator = OpaUtils.getScaleObjectAnimator(this.mWhite, 0.0f, 100, OpaUtils.INTERPOLATOR_40_OUT);
        ObjectAnimator homeCutoutAnimator = OpaUtils.getScaleObjectAnimator(this.mWhiteCutout, 0.0f, 100, OpaUtils.INTERPOLATOR_40_OUT);
        ObjectAnimator haloAnimator = OpaUtils.getScaleObjectAnimator(this.mHalo, 0.0f, 100, OpaUtils.INTERPOLATOR_40_OUT);
        homeAnimator.setStartDelay(50);
        homeCutoutAnimator.setStartDelay(50);
        this.mGestureLineSet.play(homeAnimator).with(homeCutoutAnimator).with(haloAnimator);
        this.mGestureLineSet.play(OpaUtils.getScaleObjectAnimator(this.mTop, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN)).with(homeAnimator).with(OpaUtils.getAlphaObjectAnimator(this.mRed, 1.0f, 50, 130, Interpolators.LINEAR)).with(OpaUtils.getAlphaObjectAnimator(this.mYellow, 1.0f, 50, 130, Interpolators.LINEAR)).with(OpaUtils.getAlphaObjectAnimator(this.mBlue, 1.0f, 50, 113, Interpolators.LINEAR)).with(OpaUtils.getAlphaObjectAnimator(this.mGreen, 1.0f, 50, 113, Interpolators.LINEAR)).with(OpaUtils.getScaleObjectAnimator(this.mBottom, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN)).with(OpaUtils.getScaleObjectAnimator(this.mLeft, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN)).with(OpaUtils.getScaleObjectAnimator(this.mRight, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        Animator redAnimator;
        if (this.mIsVertical) {
            redAnimator = OpaUtils.getTranslationObjectAnimatorY(this.mRed, OpaUtils.INTERPOLATOR_40_40, OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_x_trans_ry), this.mRed.getY() + OpaUtils.getDeltaDiamondPositionLeftY(), 350);
            redAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    OpaLayout.this.startCollapseAnimation();
                }
            });
            this.mGestureLineSet.play(redAnimator).with(haloAnimator).with(OpaUtils.getTranslationObjectAnimatorY(this.mBlue, OpaUtils.INTERPOLATOR_40_40, OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_x_trans_bg), this.mBlue.getY() + OpaUtils.getDeltaDiamondPositionBottomY(this.mResources), 350)).with(OpaUtils.getTranslationObjectAnimatorY(this.mYellow, OpaUtils.INTERPOLATOR_40_40, -OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_x_trans_ry), this.mYellow.getY() + OpaUtils.getDeltaDiamondPositionRightY(), 350)).with(OpaUtils.getTranslationObjectAnimatorY(this.mGreen, OpaUtils.INTERPOLATOR_40_40, -OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_x_trans_bg), this.mGreen.getY() + OpaUtils.getDeltaDiamondPositionTopY(this.mResources), 350));
        } else {
            redAnimator = OpaUtils.getTranslationObjectAnimatorX(this.mRed, OpaUtils.INTERPOLATOR_40_40, -OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_x_trans_ry), this.mRed.getX() + OpaUtils.getDeltaDiamondPositionTopX(), 350);
            redAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    OpaLayout.this.startCollapseAnimation();
                }
            });
            this.mGestureLineSet.play(redAnimator).with(homeAnimator).with(OpaUtils.getTranslationObjectAnimatorX(this.mBlue, OpaUtils.INTERPOLATOR_40_40, -OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_x_trans_bg), this.mBlue.getX() + OpaUtils.getDeltaDiamondPositionLeftX(this.mResources), 350)).with(OpaUtils.getTranslationObjectAnimatorX(this.mYellow, OpaUtils.INTERPOLATOR_40_40, OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_x_trans_ry), this.mYellow.getX() + OpaUtils.getDeltaDiamondPositionBottomX(), 350)).with(OpaUtils.getTranslationObjectAnimatorX(this.mGreen, OpaUtils.INTERPOLATOR_40_40, OpaUtils.getPxVal(this.mResources, R.dimen.opa_line_x_trans_bg), this.mGreen.getX() + OpaUtils.getDeltaDiamondPositionRightX(this.mResources), 350));
        }
        return this.mGestureLineSet;
    }
}
