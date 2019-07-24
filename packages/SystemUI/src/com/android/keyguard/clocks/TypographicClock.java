package com.android.keyguard.clocks;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.res.Resources;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.support.v7.graphics.Palette;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.util.Log;

import com.android.keyguard.R;

import java.lang.IllegalStateException;
import java.lang.NullPointerException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class TypographicClock extends TextView {

    private int mAccentColor;
    private String mDescFormat;
    private final String[] mHours;
    private final String[] mMinutes;
    private final Resources mResources;
    private final Calendar mTime;
    private TimeZone mTimeZone;
    private Context mContext;
    private final Animation fadeIn;
    private final Animation fadeOut;

    private final BroadcastReceiver mTimeZoneChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                onTimeZoneChanged(TimeZone.getTimeZone(tz));
                onTimeChanged();
            }
        }
    };

    public TypographicClock(Context context) {
        this(context, null);
    }

    public TypographicClock(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TypographicClock(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);

        mContext = context;

        mTime = Calendar.getInstance(TimeZone.getDefault());
        mDescFormat = ((SimpleDateFormat) DateFormat.getTimeFormat(context)).toLocalizedPattern();
        mResources = context.getResources();
        mHours = mResources.getStringArray(R.array.type_clock_hours);
        mMinutes = mResources.getStringArray(R.array.type_clock_minutes);
        mAccentColor = mResources.getColor(R.color.custom_text_clock_top_color, null);

        fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.setDuration(300);

        fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setStartOffset(300);
        fadeOut.setDuration(300);
    }

    public void onTimeChanged() {
        mTime.setTimeInMillis(System.currentTimeMillis());

        setContentDescription(DateFormat.format(mDescFormat, mTime));

        int hours = mTime.get(Calendar.HOUR) % 12;
        int minutes = mTime.get(Calendar.MINUTE) % 60;

        SpannedString rawFormat = (SpannedString) mResources.getQuantityText(R.plurals.type_clock_header, hours);
        Annotation[] annotationArr = (Annotation[]) rawFormat.getSpans(0, rawFormat.length(), Annotation.class);
        SpannableString colored = new SpannableString(rawFormat);

        Bitmap mBitmap;
        //Get wallpaper as bitmap
        WallpaperManager manager = WallpaperManager.getInstance(mContext);
        ParcelFileDescriptor pfd = manager.getWallpaperFile(WallpaperManager.FLAG_LOCK);

        //Sometimes lock wallpaper maybe null as getWallpaperFile doesnt return builtin wallpaper
        if (pfd == null)
            pfd = manager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM);
        try {
            if (pfd != null) mBitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
            else {
                //Incase both cases return null wallpaper, generate a yellow bitmap
                mBitmap = drawEmpty();
            }
            Palette palette = Palette.generate(mBitmap);

            //For monochrome and single color bitmaps, the value returned is 0
            if (Color.valueOf(palette.getLightVibrantColor(0x000000)).toArgb() == 0) {
                //So get bodycolor on dominant color instead as a hacky workaround
                mAccentColor = palette.getDominantSwatch().getBodyTextColor();
            //On Black Wallpapers set color to White
            } else if(String.format("#%06X", (0xFFFFFF & (palette.getLightVibrantColor(0x000000)))) == "#000000") {
                mAccentColor = Color.WHITE;
            } else {
                mAccentColor = (Color.valueOf(palette.getLightVibrantColor(0xff000000))).toArgb();
            }

          //Just a fallback, although I doubt this case will ever come
        } catch (NullPointerException e) {
            mAccentColor = Color.WHITE;
        }

        for (Annotation annotation : annotationArr) {
            if ("color".equals(annotation.getValue())) {
                colored.setSpan(new ForegroundColorSpan(mAccentColor),
                        colored.getSpanStart(annotation),
                        colored.getSpanEnd(annotation),
                        Spanned.SPAN_POINT_POINT);
            }
        }

        fadeOut.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
                setText(TextUtils.expandTemplate(colored, mHours[hours], mMinutes[minutes]));
                startAnimation(fadeIn);
            }

            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });

        if (!getText().toString().endsWith(mMinutes[minutes]))
            startAnimation(fadeOut);
    }

    public void onTimeZoneChanged(TimeZone timeZone) {
        mTimeZone = timeZone;
        mTime.setTimeZone(timeZone);
    }

    public void setClockColor(int i) {
        mAccentColor = i;
        onTimeChanged();
    }

    private Bitmap drawEmpty() {
        Bitmap convertedBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(convertedBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        canvas.drawPaint(paint);
        return convertedBitmap;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        TimeZone timeZone = mTimeZone == null ? TimeZone.getDefault() : mTimeZone;
        mTime.setTimeZone(timeZone);
        onTimeChanged();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getContext().registerReceiver(mTimeZoneChangedReceiver, filter);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(mTimeZoneChangedReceiver);
    }
}