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

import android.widget.ImageView;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.graphics.RectF;
import android.graphics.drawable.TransitionDrawable;

// Faruq: new imports
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.MotionEvent;
import android.util.Log;
import java.io.File;


public class QuickShortcut extends ImageView implements View.OnClickListener, View.OnLongClickListener, DropTarget, DragController.DragListener {
    private static final int ORIENTATION_HORIZONTAL = 1;
    private static final int TRANSITION_DURATION = 250;
    private static final int ANIMATION_DURATION = 200;

    private final int[] mLocation = new int[2];
    
    private Launcher mLauncher;
    private boolean mAssignMode;

    private AnimationSet mInAnimation;
    private AnimationSet mOutAnimation;

    private int mOrientation;
    private DragLayer mDragLayer;

    private final RectF mRegion = new RectF();

	// Faruq: new properties
	private Intent intent;
	private String packageName;
	private PackageManager pm;
	private int appNumber;
	private int dropColor;

    public QuickShortcut(Context context) {
        super(context);
    }

    public QuickShortcut(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuickShortcut(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.QuickShortcut, defStyle, 0);
        mOrientation = a.getInt(R.styleable.QuickShortcut_direction, ORIENTATION_HORIZONTAL);
		appNumber = a.getInt(R.styleable.QuickShortcut_appNumber, 0);
        a.recycle();

		pm = context.getPackageManager();
		
		dropColor = context.getResources().getColor(R.color.shortcut_color_filter);
		
		this.setOnClickListener(this);
		this.setOnLongClickListener(this);
		setHapticFeedbackEnabled(true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset,
            Object dragInfo) {
		final ItemInfo item = (ItemInfo) dragInfo;

		//Log.d("Launcher2/QSApp", "Dropped onto QuickShortcut");
		//Log.d("Launcher2/QSApp", item.getClass().toString());

		if (item instanceof ApplicationInfo && ((ApplicationInfo)item).intent != null) {
			//Log.d("Launcher2/QSApp", "AcceptedDrop");
        	return true;
		} else {
			return false;
		}
    }
    
    public Rect estimateDropLocation(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo, Rect recycle) {
        return null;
    }

    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        final ItemInfo item = (ItemInfo) dragInfo;

		//if (item.container == -1) return;

		//Log.d("Launcher2/QSApp", "Accepted dropped onto QuickShortcut");
		//Log.d("Launcher2/QSApp", ((ApplicationInfo)item).intent.toString());

		String appName = "";
		String appClass = "";
		String uri = "";

		if (((ApplicationInfo)item).intent.getComponent() != null) {
			appName = ((ApplicationInfo)item).intent.getComponent().getPackageName();
			appClass = ((ApplicationInfo)item).intent.getComponent().getClassName();
		}	
		uri = ((ApplicationInfo)item).intent.toUri(0);

		mLauncher.saveBottomApp(appNumber, appName, appClass, uri, ((ApplicationInfo)item).icon);
        setApp(appName, appClass, uri);

		//Log.d("Launcher2/QSApp", "Dropped app "+packageName+" with uri "+((ApplicationInfo)item).intent.toUri(0));

        LauncherModel.deleteItemFromDatabase(mLauncher, item);
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset,
            Object dragInfo) {
		final ItemInfo item = (ItemInfo) dragInfo;

		Log.d("HelixLauncher", source.getClass().toString());
		if (item != null && item instanceof ApplicationInfo && ((ApplicationInfo)item).intent != null) {
			this.setColorFilter(new PorterDuffColorFilter(dropColor, PorterDuff.Mode.SRC_ATOP));
        	//((View)source).setColorFilter(new PorterDuffColorFilter(dropColor, PorterDuff.Mode.SRC_ATOP));
		}
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset,
            Object dragInfo) {
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset,
            Object dragInfo) {
		//((View)source).setColorFilter(null);
		this.setColorFilter(null);
    }

    public void onDragStart(View v, DragSource source, Object info, int dragAction) {
        final ItemInfo item = (ItemInfo) info;
		if (item != null && item instanceof ApplicationInfo && ((ApplicationInfo)item).intent != null) {
            mAssignMode = true;
			if (packageName == null) {
				this.setImageResource(R.drawable.quick_shortcut);
			}
            createAnimations();
            startAnimation(mInAnimation);
        }
    }

    public void onDragEnd() {
        if (mAssignMode) {
            mAssignMode = false;
			if (packageName == null) {
				this.setImageDrawable(null);
			}
            startAnimation(mOutAnimation);
        }
    }

	public void setApp(String appName, String appClass, String uri) {
        if ((appName.length() != 0 && appClass.length() != 0) || uri.length() != 0) {
            packageName = appName;
            if (uri.length() > 0) {
                try {
                    intent = Intent.parseUri(uri, 0);
                } catch (Exception e) {
                }
                
                if (appClass.length() != 0)
                    intent.setClassName(appName, appClass);
            } else {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName(appName, appClass);
            }
            
            //Log.d("Launcher2/QSApp", "Set intent: "+intent);
			Drawable icon = null;
			
			try {
				Bitmap bitmap = BitmapFactory.decodeFile(Launcher.CUSTOM_ICONS_FOLDER+appNumber+".png");
            	if (bitmap != null) {
					icon = new FastBitmapDrawable(bitmap);
				} else {
					icon = pm.getActivityIcon(intent);
				}

	        } catch (Exception e) {
	        }
            
			this.setImageDrawable(Utilities.createIconThumbnail(icon, getContext()));
            setFocusable(true);
            
        } else {
            this.setImageDrawable(null);
			File file = new File(Launcher.CUSTOM_ICONS_FOLDER+appNumber+".png");
			if (file.exists()) file.delete();
            packageName = null;
            intent = null;
            setFocusable(false);
        }
    }

	public void onClick(View v) {
		if (intent != null) {
			//Log.d("Launcher2/QSApp", "Starting "+intent);
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES
.ECLAIR_MR1) { 
            	int[] pos = new int[2];
	            v.getLocationOnScreen(pos);
	            intent.setSourceBounds(
	                    new Rect(pos[0], pos[1], pos[0]+v.getWidth(), pos[1]+v.getHeight()));
			}
			mLauncher.startActivitySafely(intent);
		}
	}

	public boolean onLongClick(View v) {
		if (intent != null) {
			new AlertDialog.Builder(getContext())
				  .setTitle("Confirm")
			      .setMessage("Confirm delete shortcut?")
			      .setPositiveButton("Yes", deleteShortcut)
				  .setNegativeButton("No", cancelDelete)
			      .show();
		}
		return true;
	}
	
	DialogInterface.OnClickListener deleteShortcut =
		new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				setApp("", "", "");
				mLauncher.saveBottomApp(appNumber, "", "", "");
			}
	};

	DialogInterface.OnClickListener cancelDelete =
		new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
	};
	
	@Override
    public boolean onTouchEvent(MotionEvent ev) {
 
        final int action = ev.getAction();
        final float x = ev.getX();
 
		if (intent != null) {
	        switch (action) {
	        	case MotionEvent.ACTION_DOWN:
		            this.setBackgroundResource(R.drawable.focused_application_background);
					break;
		        case MotionEvent.ACTION_UP:
		            this.setBackgroundDrawable(null);
					break;
	        }
		}
 
        return super.onTouchEvent(ev);
    }
 
	@Override
	public void onFocusChanged (boolean gainFocus, int direction, Rect previouslyFocusedRect) {
		if (intent != null) {
			if (gainFocus) {
				this.setBackgroundResource(R.drawable.focused_application_background);
			} else {
				this.setBackgroundDrawable(null);
			}
		}
	}

    private void createAnimations() {
        if (mInAnimation == null) {
            mInAnimation = new FastAnimationSet();
            final AnimationSet animationSet = mInAnimation;
            animationSet.setInterpolator(new AccelerateInterpolator());
            animationSet.addAnimation(new AlphaAnimation(0.0f, 1.0f));
            if (mOrientation == ORIENTATION_HORIZONTAL) {
                animationSet.addAnimation(new TranslateAnimation(Animation.ABSOLUTE, 0.0f,
                        Animation.ABSOLUTE, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f));
            } else {
                animationSet.addAnimation(new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                        1.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.ABSOLUTE, 0.0f,
                        Animation.ABSOLUTE, 0.0f));
            }
            animationSet.setDuration(ANIMATION_DURATION);
        }
        if (mOutAnimation == null) {
            mOutAnimation = new FastAnimationSet();
            final AnimationSet animationSet = mOutAnimation;
            animationSet.setInterpolator(new AccelerateInterpolator());
            animationSet.addAnimation(new AlphaAnimation(1.0f, 0.0f));
            if (mOrientation == ORIENTATION_HORIZONTAL) {
                animationSet.addAnimation(new FastTranslateAnimation(Animation.ABSOLUTE, 0.0f,
                        Animation.ABSOLUTE, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 1.0f));
            } else {
                animationSet.addAnimation(new FastTranslateAnimation(Animation.RELATIVE_TO_SELF,
                        0.0f, Animation.RELATIVE_TO_SELF, 1.0f, Animation.ABSOLUTE, 0.0f,
                        Animation.ABSOLUTE, 0.0f));
            }
            animationSet.setDuration(ANIMATION_DURATION);
        }
    }

    void setLauncher(Launcher launcher) {
        mLauncher = launcher;
    }

    void setDragController(DragLayer dragLayer) {
        mDragLayer = dragLayer;
    }

    private static class FastTranslateAnimation extends TranslateAnimation {
        public FastTranslateAnimation(int fromXType, float fromXValue, int toXType, float toXValue,
                int fromYType, float fromYValue, int toYType, float toYValue) {
            super(fromXType, fromXValue, toXType, toXValue,
                    fromYType, fromYValue, toYType, toYValue);
        }

        @Override
        public boolean willChangeTransformationMatrix() {
            return true;
        }

        @Override
        public boolean willChangeBounds() {
            return false;
        }
    }

    private static class FastAnimationSet extends AnimationSet {
        FastAnimationSet() {
            super(false);
        }

        @Override
        public boolean willChangeTransformationMatrix() {
            return true;
        }

        @Override
        public boolean willChangeBounds() {
            return false;
        }
    }
}
