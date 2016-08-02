/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private final String LOG_TAG = SunshineWatchFace.class.getSimpleName();

    /**
     * Update rate in milliseconds for interactive mode. We update twice a second to blink the
     * colon
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.MILLISECONDS.toMillis(500);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFace.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mTextPaint;
        Paint mHourPaint;
        Paint mColonPaint;
        Paint mMinutePaint;
        Paint mDatePaint;
        Paint mWeatherPaint;
        Paint mTempHighPaint;
        Paint mTempLowPaint;
        boolean mAmbient;
        boolean mShouldDrawColons;
        SimpleDateFormat mDayOfWeekFormat;
        java.text.DateFormat mDateFormat;

        Calendar mCalendar;
        Date mDate;

        int mIconID = -1;
        String mHighTemp;
        String mLowTemp;

        private GoogleApiClient mGoogleApiClient;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                initFormats();
                invalidate();
            }
        };

        float mXOffset;
        float mYOffset;
        float mYOffsetWeather;
        float mLineHeight;
        float mYOffsetTemp;
        float mXOffsetWeather;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = SunshineWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mYOffsetWeather = resources.getDimension(R.dimen.weather_y_offset);
            mYOffsetTemp = resources.getDimension(R.dimen.temp_line_y_offset);

            mBackgroundPaint = new Paint();


            mBackgroundPaint.setColor(getColor(R.color.primary));

            mLineHeight = resources.getDimension(R.dimen.digital_line_height);


            mTextPaint = createTextPaint(getColor(R.color.digital_text));
            mHourPaint = createTextPaint(getColor(R.color.digital_text));
            mColonPaint = createTextPaint(getColor(R.color.digital_text));
            mMinutePaint = createTextPaint(getColor(R.color.digital_text));
            mDatePaint = createTextPaint(getColor(R.color.primary_light));
            mWeatherPaint = new Paint();
            mTempHighPaint = createTextPaint(getColor(R.color.digital_text));
            mTempLowPaint = createTextPaint(getColor(R.color.primary_light));

            mCalendar = Calendar.getInstance();
            mDate = new Date();
            initFormats();

            mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFace.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            mGoogleApiClient.connect();

        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private void initFormats() {
            mDayOfWeekFormat = new SimpleDateFormat("EEE, MMM d yyyy ", Locale.getDefault());
            mDayOfWeekFormat.setCalendar(mCalendar);
            mDateFormat = DateFormat.getDateFormat(SunshineWatchFace.this);
            mDateFormat.setCalendar(mCalendar);
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone and date formats, in case they changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                initFormats();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            mXOffsetWeather = resources.getDimension(isRound
                    ? R.dimen.weather_x_offset_round : R.dimen.weather_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTextPaint.setTextSize(textSize);
            mHourPaint.setTextSize(textSize);
            mMinutePaint.setTextSize(textSize);
            mColonPaint.setTextSize(textSize);
            mDatePaint.setTextSize(resources.getDimension(R.dimen.digital_date_text_size));
            mTempHighPaint.setTextSize(resources.getDimension(R.dimen.temp_text_size));
            mTempLowPaint.setTextSize(resources.getDimension(R.dimen.temp_text_size));
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }


        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            // Show colons for the first half of each second so the colons blink on when the time
            // updates.
            long now = System.currentTimeMillis();
            mShouldDrawColons = (now % 1000) < 500;
            mCalendar.setTimeInMillis(now);
            mDate.setTime(now);

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            float x = mXOffset + 10;

            // Draw the hours
            String hourText = formatTwoDigitNumber(mCalendar.get(Calendar.HOUR_OF_DAY));
            canvas.drawText(hourText, x, mYOffset, mHourPaint);
            x += mHourPaint.measureText(hourText);

            // Draw the colon. Blink if interactive mode.
            if(mShouldDrawColons || isInAmbientMode())
                canvas.drawText(":", x, mYOffset, mColonPaint);
            x += mColonPaint.measureText(":");

            // Draw the minutes.
            String minuteText = formatTwoDigitNumber(mCalendar.get(Calendar.MINUTE));
            canvas.drawText(minuteText, x, mYOffset, mMinutePaint);

            // Draw everything besides time only if in interactive mode
            if(!isInAmbientMode()) {
                // Draw the date
                String dayOfWeekDisplay = mDayOfWeekFormat.format(mDate).toUpperCase();
                canvas.drawText(
                        dayOfWeekDisplay,
                        mXOffset, mYOffset + mLineHeight, mDatePaint);


                x = mXOffsetWeather;
                // Draw the weather icon
                if (mIconID != -1) {
                    int drawableRes = Utility.getIconResourceForWeatherCondition(mIconID);
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), drawableRes);
                    canvas.drawBitmap(bitmap, x, mYOffsetWeather, mWeatherPaint);
                }

                // Draw the high and low temp
                if (mHighTemp != null && mLowTemp != null) {
                    x += getResources().getInteger(R.integer.temp_line_x_offset);

                    String highTempDisplay = mHighTemp + "°";
                    canvas.drawText(highTempDisplay, x, mYOffsetWeather + mYOffsetTemp, mTempHighPaint);
                    x += mTempHighPaint.measureText(highTempDisplay) + 5;

                    String lowTempDisplay = mLowTemp + "°";
                    canvas.drawText(lowTempDisplay, x, mYOffsetWeather + mYOffsetTemp, mTempLowPaint);
                }
            }

        }

        private String formatTwoDigitNumber(int hour) {
            return String.format("%02d", hour);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Wearable.DataApi.addListener(mGoogleApiClient, this);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {

            // Get the weather data sent over from the mobile device
            for (DataEvent dataEvent : dataEventBuffer) {
                if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                    DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                    String path = dataEvent.getDataItem().getUri().getPath();
                    if(path.equals("/weather-data")){
                        mIconID = dataMap.getInt("icon-id");
                        mHighTemp = dataMap.getString("high-temp");
                        mLowTemp = dataMap.getString("low-temp");

                        // Refresh the screen
                        invalidate();
                    }
                }


            }
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }
    }
}
