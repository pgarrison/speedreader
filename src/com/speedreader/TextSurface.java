package com.speedreader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

public class TextSurface extends SurfaceView implements SurfaceHolder.Callback {
	
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private static TextThread thread;
	private Context context;
	private static SeekBarDialog prog;
	
	private final String PREFS_NAME = Options.PREFS_NAME;
	
	private int wpm;
	private int wordsAtATime;
	private int charsAtATime;
	
	public class TextThread extends Thread {
		
		private final int BUFFER_SIZE = 2048; // Bigger means we estimate the progress more accurately (and more slowly)
		@SuppressWarnings("unused")
		private final int SCANNER_BUFFER_SIZE = 8*1024; // Defined in the scanner class? When we make a new scanner, 
														// it automatically jumps forward this many bytes. 
														// But it could be bits, because its times 8.
		private final int DEFAULT_BACKGROUND = Options.DEFAULT_BACKGROUND;
		private final int DEFAULT_TEXTCOLOR = Options.DEFAULT_TEXTCOLOR;
		private final double MAX_FONT_SIZE = 110;
		private final String PREFS_NAME = Options.PREFS_NAME;
		
		private boolean isRunning = false;
		private Scanner scanner;
		private int bytesRead = 0;
		private Paint paint;
		
		private SurfaceHolder holder;
		private Context context;
		private String defaultPath;
		
		private int backgroundColor;
		private int textColor;
		
		private String charset;
		private File file;
		private FileChannel chan;
		
		public TextThread(SurfaceHolder theHolder, Context theContext, Handler theHandler) {
			holder = theHolder;
			context = theContext;
			defaultPath = Environment.getExternalStorageDirectory() + "/Android/data/"
					+ context.getPackageName() + "/files/" + context.getString(R.string.default_file);
					//TODO Find a better way to do this
			
			restorePrefs();
			
			try {
				RandomAccessFile raf = new RandomAccessFile(file, "r"); // r = read only
				chan = raf.getChannel();
				charset = Options.getCharset(file);
				newScanner();
			} catch (FileNotFoundException e) {
				// This should never happen. File is checked when the Go! button is pressed.
			} catch (IOException e) {
				System.out.println(e);
			}
			
			paint = new Paint();
			paint.setColor(textColor);
			paint.setTextAlign(Paint.Align.CENTER);
			paint.setAntiAlias(true);
			paint.setTypeface(Typeface.SERIF);			
		}
		
		@Override
		public void run() {
			if(isRunning) {
				String word;
				
				if (scanner.hasNext()) {
					word = scanner.next();
					int chars = word.length();
					if(charsAtATime <= 0) {
						for(int i = 1; i < wordsAtATime && scanner.hasNext(); i++) {
							word += " " + scanner.next();
						}
					} else {
						for(int words = 1; (words < wordsAtATime || chars < charsAtATime) && scanner.hasNext(); words++) {
							word += " " + scanner.next();
							chars = word.length();
						}
					}


					// Each word has a space after it that needs to be counted in the bytes.
					String wordWithSpace = word + " ";
					int newBytes = wordWithSpace.getBytes().length;
					bytesRead += newBytes;
					prog.setProgress(bytesRead);
				} else {
					setRunning(false); // When we're out of words, stop showing them.
					word = context.getString(R.string.EOF);
					prog.setProgress(prog.getMax()); // Sometimes it ends up at 99%
				}
				System.out.println(word);
				 
				/* Draw the word */
				Canvas can = holder.lockCanvas();
				
				// Fill with the background color to clear old word.
				can.drawColor(backgroundColor);
				
				int x = can.getWidth()/2;
				int y = can.getHeight()/2;
				
				Rect bounds = new Rect();
				int textSize = (int) (MAX_FONT_SIZE); 
				
				do {
					textSize--;
					paint.setTextSize(textSize);
					paint.getTextBounds(word, 0, word.length(), bounds);
				}
				while(bounds.width() > can.getWidth());
				
				paint.setTextSize(textSize);
				
				can.drawText(word, x, y, paint);
				holder.unlockCanvasAndPost(can);
			}
		} // End run()
		
		protected void setRunning(boolean running) {
			isRunning = running;
		}
		
		protected boolean getRunning() {
			return isRunning;
		}
		
		protected Bookmark createBookmark() {
			int wordsInSnippet = getResources().getInteger(R.integer.words_in_bookmark_description);
			String snippet = "\"";
			for(int i = 0; i < wordsInSnippet && scanner.hasNext(); i++) {
				snippet += scanner.next() + " "; // TODO whitespace
			}
			snippet += "...\"";
			Bookmark bm = new Bookmark(file, bytesRead, snippet);
			prog.setProgress(bytesRead);
			updatePosition();
			return bm;
		}

		protected void close() {
			scanner.close();
		}
		
		private void updatePosition() {
			try {
				int progress = prog.getProgress();
				if(chan.position() != progress) { // Allows for added granularity.
					chan.position(progress);
					bytesRead = progress; 
					newScanner();
				}
			} catch (IOException e) {
				System.out.println("updatePosition failed " + e);
			}
		}
		
		
		/*
		 * @return the position in the file, measured in bytes.
		 * 		   If this value is greater than the max int value, returns -1.
		 */
		protected int getPosition() {
			return bytesRead;
		}
		
		protected int getMax() {
			return (int) file.length();
		}
		
		protected int getMaxWords() {
			double a = wordsPerByte();
			long b = file.length();
			double maximum = b*a;
			int max = (int) Math.round(maximum);
			return max;
		}
		
		private boolean newScanner() {
			if(charset != null) {
				scanner = new Scanner(chan, charset);
				return true; // Charset exists.
			}
			else {
				scanner = new Scanner(chan);
				return false; // No charset.
			}
		}
		
		/*
		 * @return The number of words in the first BUFFER_SIZE number of bytes of the file.
		 * 		   This value is >= 1 unless the file is empty (in which case it returns -1).
		 */
		private double wordsPerByte() {
			double numWords = 1; // Guess that we won't end on whitespace. Not a big deal if thats wrong.
							  // This also means we never return 0.
			double bytesRead = 1;
			FileInputStream readALittle = null;
			try {
				readALittle = new FileInputStream(file);
				byte[] buffer = new byte[BUFFER_SIZE];
				bytesRead = readALittle.read(buffer);
				String text = new String(buffer);
				
				// Get the number of words
				for(int i = 1; i < text.length(); i++) {
					Character thisChar = text.charAt(i);
					Character lastChar = text.charAt(i - 1);
					// If this is the end of a word, increase the number of words by one.
					if(Character.isWhitespace(thisChar) && !Character.isWhitespace(lastChar)) {
						numWords++;
					}
				}
			} catch (FileNotFoundException e) {
				// This doesn't happen.
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					readALittle.close();
				} catch (IOException e) {
					System.out.println(e);
				}
			}
			
			double wpb = numWords / bytesRead; // Words Per Byte
			return wpb;
		}
		
		private void restorePrefs() {
			SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
			backgroundColor = settings.getInt("Background", DEFAULT_BACKGROUND);
			textColor = settings.getInt("TextColor", DEFAULT_TEXTCOLOR);
			file = new File(settings.getString("Path", defaultPath));
		}
	}
	
	public TextSurface(Context theContext, AttributeSet attrs) {
		super(theContext, attrs);
		
		context = theContext;
		
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		wpm = settings.getInt("WPM", Integer.parseInt(context.getString(R.string.default_speed)));
		wordsAtATime = settings.getInt("NumWords", Integer.parseInt(context.getString(R.string.default_num_words)));
		charsAtATime = settings.getInt("NumChars", Integer.parseInt(context.getString(R.string.default_num_chars)));
				//Yeah, yeah. It's ugly. I know.
		
		thread = new TextThread(holder, context, getHandler());
		prog = new SeekBarDialog(context){
			@Override
			public void hide() {
				super.hide();
				thread.updatePosition();
				setThreadRunning(true);
			}
		};
		
		/* The order of these matters quite a bit. */
		prog.setMax(getMaxFromThread());
		prog.setMaxWords(getMaxWordsFromThread());
		prog.show(); // TODO try removing these
		prog.setBookmarkListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String defaultDir = Environment.getExternalStorageDirectory() + "/Android/data/"
				+ context.getPackageName() + "/files/";
				File bookmarkDir = new File(defaultDir + context.getString(R.string.bookmarks_dir) + "/");
				thread.createBookmark().addBookmark(bookmarkDir);
				
				Toast yummy = Toast.makeText(context, context.getString(R.string.bookmark_added), Toast.LENGTH_SHORT);
				yummy.show();
			}
		});
		prog.setProgress(restoreLocation());
		thread.updatePosition();
		prog.hide(); // This (accidentally?) calls updatePosition()
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// Do nothing - it's always landscape.
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		executor.scheduleWithFixedDelay(thread, 0, wpmToMillis(wpm), TimeUnit.MILLISECONDS);
	}
	
	/*
	 * Called by Reader.onRestart()
	 */
	protected void begin() {
		prog.setProgress(restoreLocation());
	}
	
	/*
	 * Called by Reader.onStop()
	 */
	protected void end() {
		// TODO enabling prog.dismiss() will cause random force closes. Why?
		// if we dont stop it, it could eplode, apparently.
		thread.setRunning(false);
		saveLocation();
		thread.close(); // Calls scanner.close(); Maybe already done?
		executor.shutdownNow();
		//prog.dismiss(); // So we don't leak the window.
	}
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
		pause();
		return false;
	}
	
	private int restoreLocation() {
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		int location = settings.getInt("Location", 0); // Always defaults to 0. Always.
		return location;
	}
	
	private void saveLocation() {
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("Location", thread.getPosition());
		editor.commit();
	}
	
	public void buttonPushed() {
		pause();
	}
	
	public void pause() {
		thread.setRunning(false);
		/*
		 * When we call show() the first time it's really dumb and sets the progress to 0 :(
		 * So we're gonna save it before we show it. 
		 */
		prog.show();
	}
	
	public void killTheExecutor() { // It's a revolt!
		executor.shutdownNow();
	}
	
	/*
	 * Allow events to pause/unpause the animation.
	 * 
	 * @param running true to start; false to stop
	 */
	public void setThreadRunning(boolean running) {
		thread.setRunning(running);
	}
	
	public boolean getThreadRunning() {
		return thread.getRunning();
	}
	
	// Measured in bytes.
	private static int getMaxFromThread() {
		return thread.getMax();
	}
	
	// Measured in words.
	private int getMaxWordsFromThread() {
		return thread.getMaxWords();
	}

	
	private static long wpmToMillis(int wpm) {
    	double wpmDouble = wpm;
    	double millispw = 1000*60/wpmDouble; //Milliseconds per word
    	long millis = Math.round(millispw);
    	return millis;
    }

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// Nothing here now.		
	}
}
