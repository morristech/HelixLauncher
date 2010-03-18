/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.helixproject.launcher;

import android.widget.GridView;
import android.widget.AdapterView;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Canvas;

// Faruq: new imports
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.util.Log;

public class AllAppsGridView extends GridView implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener, DragSource {

    private DragController mDragger;
    private Launcher mLauncher;
    private Bitmap mTexture;
    private Paint mPaint;
    private int mTextureWidth;
    private int mTextureHeight;

	// Faruq: new properties
	private static final int CLOSE_VELOCITY = 2000;
	private static final int CLOSE_POS = 130;
	private float mLastMotionX;
    private float mLastMotionY;
	private VelocityTracker mVelocityTracker;
    private int mMaximumVelocity;

    public AllAppsGridView(Context context) {
        super(context);
    }

    public AllAppsGridView(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.gridViewStyle);
    }

    public AllAppsGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AllAppsGridView, defStyle, 0);
        final int textureId = a.getResourceId(R.styleable.AllAppsGridView_texture, 0);
        if (textureId != 0) {
            mTexture = BitmapFactory.decodeResource(getResources(), textureId);
            mTextureWidth = mTexture.getWidth();
            mTextureHeight = mTexture.getHeight();

            mPaint = new Paint();
            mPaint.setDither(false);
        }
        a.recycle();

		final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    @Override
    public boolean isOpaque() {
        return !mTexture.hasAlpha();
    }

    @Override
    protected void onFinishInflate() {
        setOnItemClickListener(this);
        setOnItemLongClickListener(this);
    }

    @Override
    public void draw(Canvas canvas) {
        final Bitmap texture = mTexture;
        final Paint paint = mPaint;

        final int width = getWidth();
        final int height = getHeight();

        final int textureWidth = mTextureWidth;
        final int textureHeight = mTextureHeight;

        int x = 0;
        int y;

        while (x < width) {
            y = 0;
            while (y < height) {
                canvas.drawBitmap(texture, x, y, paint);
                y += textureHeight;
            }
            x += textureWidth;
        }

        super.draw(canvas);
    }

    public void onItemClick(AdapterView parent, View v, int position, long id) {
        ApplicationInfo app = (ApplicationInfo) parent.getItemAtPosition(position);
        mLauncher.startActivitySafely(app.intent);
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (!view.isInTouchMode()) {
            return false;
        }

        ApplicationInfo app = (ApplicationInfo) parent.getItemAtPosition(position);
        app = new ApplicationInfo(app);

        mDragger.startDrag(view, this, app, DragController.DRAG_ACTION_COPY);
        mLauncher.closeAllApplications();

        return true;
    }

    public void setDragger(DragController dragger) {
        mDragger = dragger;
    }

    public void onDropCompleted(View target, boolean success) {
    }

    void setLauncher(Launcher launcher) {
        mLauncher = launcher;
    }

	@Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();
        final float x = ev.getX();
		final float y = ev.getY();

		//Log.d("AllAppsGridView", "lastX: "+mLastMotionX+"; x: "+x+"; lastY: "+mLastMotionY+"; y: "+y);

        switch (action) {
        	case MotionEvent.ACTION_DOWN:
	            // Remember where the motion event started
	            mLastMotionX = x;
				mLastMotionY = y;
	            break;
	        case MotionEvent.ACTION_UP:
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocityX = (int) velocityTracker.getXVelocity();
				int velocityY = (int) velocityTracker.getYVelocity();

				//Log.d("AllAppsGridView", "Y: "+y+"; LastMotionY: "+mLastMotionY+"; VelocityY: "+velocityY);
                if (mLastMotionY <= CLOSE_POS && velocityY >= CLOSE_VELOCITY) {
					//Log.d("AllAppsGridView", "Close Drawer");
					mLauncher.closeDrawer();
				}
				else if (mLastMotionX <= CLOSE_POS && velocityX >= (CLOSE_VELOCITY - 500)) {
					//Log.d("AllAppsGridView", "Close Drawer");
					mLauncher.closeDrawer();
				}

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
	            break;
	        case MotionEvent.ACTION_CANCEL:
        }

        return super.onTouchEvent(ev);
    }
}
