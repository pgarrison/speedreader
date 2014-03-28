/*
 * Copyright (C) 2007 The Android Open Source Project
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
 * 
 * I edited this to use a SeekBar.
 */

package com.speedreader;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.text.NumberFormat;

/**
 * <p>ProgressDialog, edited to use a SeekBar. </p>
 * <p>A dialog showing a progress indicator and an optional text message or view.
 * Only a text message or a view can be used at the same time.</p>
 * <p>The dialog can be made cancelable on back key press.</p>
 * <p>The progress range is 0..10000.</p>
 */
public class SeekBarDialog extends AlertDialog {
    
    /** Creates a SeekBarDialog with a ciruclar, spinning progress
     * bar.
     */
    //public static final int STYLE_SPINNER = 0;
    
    /** Creates a SeekBarDialog with a horizontal progress bar. This is the default.
     */
    public static final int STYLE_HORIZONTAL = 1;
    
    private SeekBar mProgress;
    private TextView mMessageView;
    
    private Button mButt; // Poop joke.
    private Button mBookmarkButt; // Oh and I added this (these?)
    
    private int mProgressStyle = STYLE_HORIZONTAL;
    private TextView mProgressNumber;
    private String mProgressNumberFormat;
    private TextView mProgressPercent;
    private NumberFormat mProgressPercentFormat;
    
    private int mMax;
    private int mProgressVal;
    private int mSecondaryProgressVal;
    private int mIncrementBy;
    private int mIncrementSecondaryBy;
    private Drawable mProgressDrawable;
    private Drawable mIndeterminateDrawable;
    private CharSequence mMessage;
    private boolean mIndeterminate;
    
    private boolean mHasStarted;
    private Handler mViewUpdateHandler;

	private int mMaxWords;
    
    public SeekBarDialog(Context context) {
        super(context);
    }

    public SeekBarDialog(Context context, int theme) {
        super(context, theme);
    }

    public static SeekBarDialog show(Context context, CharSequence title,
            CharSequence message) {
        return show(context, title, message, false);
    }

    public static SeekBarDialog show(Context context, CharSequence title,
            CharSequence message, boolean indeterminate) {
        return show(context, title, message, indeterminate, false, null);
    }

    public static SeekBarDialog show(Context context, CharSequence title,
            CharSequence message, boolean indeterminate, boolean cancelable) {
        return show(context, title, message, indeterminate, cancelable, null);
    }

    public static SeekBarDialog show(Context context, CharSequence title,
            CharSequence message, boolean indeterminate,
            boolean cancelable, OnCancelListener cancelListener) {
        SeekBarDialog dialog = new SeekBarDialog(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        //dialog.setIndeterminate(indeterminate);
        dialog.setCancelable(cancelable);
        dialog.setOnCancelListener(cancelListener);
        dialog.show();
        return dialog;
    }
    
    @Override
    public void cancel() {
    	hide();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        
        /* Use a separate handler to update the text views as they
         * must be updated on the same thread that created them.
         */
        mViewUpdateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                
                /* Update the number and percent */
                int progress = mProgress.getProgress();
                int max = mProgress.getMax();
                double percent = (double) progress / (double) max;
				String format = mProgressNumberFormat;
                //mProgressNumber.setText(String.format(format, progress, max));
                mProgressNumber.setText(String.format(getContext().getString(R.string.seek_dialog_format), mMaxWords));
                	// We don't know what word they're on.
                SpannableString tmp = new SpannableString(mProgressPercentFormat.format(percent));
                tmp.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                        0, tmp.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                mProgressPercent.setText(tmp);
            }
        };
        View view = inflater.inflate(R.layout.seek_bar_dialog, null);
        mProgress = (SeekBar) view.findViewById(R.id.seek_bar);
        mProgressNumber = (TextView) view.findViewById(R.id.progress_number);
        mProgressNumberFormat = "of roughly %d words"; // was "%d/%d"
        mProgressPercent = (TextView) view.findViewById(R.id.progress_percent);
        mProgressPercentFormat = NumberFormat.getPercentInstance();
        mProgressPercentFormat.setMaximumFractionDigits(0);
        
        /* I added this. */
        mButt = (Button) view.findViewById(R.id.seek_button);
        mButt.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
                hide();
            }
        });
        
        mBookmarkButt = (Button) view.findViewById(R.id.bookmark_button);
        
        mProgress.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				setProgress(progress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// Do nothing.
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// Do nothing.
			}        	
        });
        
        /* End (this bit of) my code. */
        
        setView(view);
        
        if (mMax > 0) {
            setMax(mMax);
        }
        if (mProgressVal > 0) {
            setProgress(mProgressVal);
        }
        if (mSecondaryProgressVal > 0) {
            setSecondaryProgress(mSecondaryProgressVal);
        }
        if (mIncrementBy > 0) {
            incrementProgressBy(mIncrementBy);
        }
        if (mIncrementSecondaryBy > 0) {
            incrementSecondaryProgressBy(mIncrementSecondaryBy);
        }
        if (mProgressDrawable != null) {
            setProgressDrawable(mProgressDrawable);
        }
        if (mIndeterminateDrawable != null) {
            setIndeterminateDrawable(mIndeterminateDrawable);
        }
        if (mMessage != null) {
            setMessage(mMessage);
        }
        setIndeterminate(mIndeterminate);
        onProgressChanged();
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        mHasStarted = true;
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        mHasStarted = false;
    }
    
    /* 
     * I added this method, so the calling Activity can set the OnClickListener
     * of the "Add bookmark button.
     */
    public void setBookmarkListener(View.OnClickListener listener) {
    	mBookmarkButt.setOnClickListener(listener);
    }
    
    public void setProgress(int value) {
        if (mHasStarted) {
            mProgress.setProgress(value);
            onProgressChanged();
        } else {
            mProgressVal = value;
        }
    }

    public void setSecondaryProgress(int secondaryProgress) {
        if (mProgress != null) {
            mProgress.setSecondaryProgress(secondaryProgress);
            onProgressChanged();
        } else {
            mSecondaryProgressVal = secondaryProgress;
        }
    }

    public int getProgress() {
        if (mProgress != null) {
            return mProgress.getProgress();
        }
        return mProgressVal;
    }

    public int getSecondaryProgress() {
        if (mProgress != null) {
            return mProgress.getSecondaryProgress();
        }
        return mSecondaryProgressVal;
    }

    public int getMax() {
        if (mProgress != null) {
            return mProgress.getMax();
        }
        return mMax;
    }

    public void setMax(int max) {
        if (mProgress != null) {
            mProgress.setMax(max);
            onProgressChanged();
        } else {
            mMax = max;
        }
    }
    
    public void setMaxWords(int maxWords) {
        mMaxWords = maxWords;
    }

    public void incrementProgressBy(int diff) {
        if (mProgress != null) {
            mProgress.incrementProgressBy(diff);
            onProgressChanged();
        } else {
            mIncrementBy += diff;
        }
    }

    public void incrementSecondaryProgressBy(int diff) {
        if (mProgress != null) {
            mProgress.incrementSecondaryProgressBy(diff);
            onProgressChanged();
        } else {
            mIncrementSecondaryBy += diff;
        }
    }

    public void setProgressDrawable(Drawable d) {
        if (mProgress != null) {
            mProgress.setProgressDrawable(d);
        } else {
            mProgressDrawable = d;
        }
    }

    public void setIndeterminateDrawable(Drawable d) {
        if (mProgress != null) {
            mProgress.setIndeterminateDrawable(d);
        } else {
            mIndeterminateDrawable = d;
        }
    }

    public void setIndeterminate(boolean indeterminate) {
        if (mProgress != null) {
            mProgress.setIndeterminate(indeterminate);
        } else {
            mIndeterminate = indeterminate;
        }
    }

    public boolean isIndeterminate() {
        if (mProgress != null) {
            return mProgress.isIndeterminate();
        }
        return mIndeterminate;
    }
    
    @Override
    public void setMessage(CharSequence message) {
        if (mProgress != null) {
            if (mProgressStyle == STYLE_HORIZONTAL) {
                super.setMessage(message);
            } else {
                mMessageView.setText(message);
            }
        } else {
            mMessage = message;
        }
    }
    
    public void setProgressStyle(int style) {
        mProgressStyle = style;
    }

    /**
     * NOTICE: My default format uses one number, the total. This probably won't work now.
     * 
     * Change the format of Progress Number. The default is "current/max".
     * Should not be called during the number is progressing.
     * @param format Should contain two "%d". The first is used for current number
     * and the second is used for the maximum.
     * @hide
     */
    public void setProgressNumberFormat(String format) {
    	System.out.println("***Do not set the format of this SeekBarDialog***");
        mProgressNumberFormat = format;
    }
    
    private void onProgressChanged() {
        if (mProgressStyle == STYLE_HORIZONTAL) {
            mViewUpdateHandler.sendEmptyMessage(0);
        }
    }
    
    public Button getButton() {
    	return mButt;
    }
}
