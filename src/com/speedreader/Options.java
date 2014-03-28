package com.speedreader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.mozilla.universalchardet.UniversalDetector;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.epub.EpubReader;

import com.itextpdf.text.pdf.PRTokeniser;
import com.itextpdf.text.pdf.PdfReader;
import com.lamerman.FileDialog;
import com.speedreader.Reader;
import com.speedreader.ColorPickerDialog.OnColorChangedListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class Options extends Activity {
	
	private final String[] SUPPORTED_WEB_FORMATS = {".html", ".htm", ".shtml", ".shtm", ".xml"};
	
	private final int[] BACKGROUND_COLORS = {Color.WHITE, Color.LTGRAY, Color.DKGRAY};
	private final int[] TEXT_COLORS = {Color.BLACK, Color.DKGRAY, Color.LTGRAY};
	private final int WPM_INCREMENT = 50;
	private final int NUM_WORDS_INCREMENT = 1;
	private final int NUM_CHARS_INCREMENT = 1;
	public final static int DEFAULT_BACKGROUND = Color.WHITE;
	public final static int DEFAULT_TEXTCOLOR = Color.BLACK;
	private final int REQUEST_LOAD = 1;
	private final int REQUEST_BOOKMARK = 2;
	private final String bgMessage = "Background:";
	private final String textMessage = "Text:";
	
	private final String DONATE = "If you like Speed Reader, donate! \nSpeed Reader Gold is " +
								  "now available in the market";
	
	protected static final String PREFS_NAME = "SettingsFile";
	
	private static ProgressDialog parsing;
	private int wpm;
	private int wordsAtATime;
	private int charsAtATime;
	private int backgroundColor = Color.WHITE; //Defaults to white
	private int textColor = Color.BLACK; //Defaults to black
	private File file;
	private int location;
	private boolean donateAlert;
	private EditText speedField;
	private EditText pathField;
	private EditText numWordsField;
	private EditText numCharsField;
	private String defaultPath;
	private String defaultDir;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		defaultDir = Environment.getExternalStorageDirectory() + "/Android/data/"
				+ getPackageName() + "/files/";
		defaultPath = defaultDir + getString(R.string.default_file);
				//TODO Find a prettier way to do this
		
		setContentView(R.layout.main);
		
		restorePrefs();
		
		if(donateAlert == false) {
			alert(DONATE);
			donateAlert = true;
			savePrefs();
		}
		/*
		 * Add all the event listeners. Lots of buttons means lots of this.
		 */
		ImageButton[] colorButtons = new ImageButton[TEXT_COLORS.length];
		for(int i = 0; i < TEXT_COLORS.length; i++){
			colorButtons[i] = (ImageButton) this.findViewById(R.id.image_button_1 + i); // image_button_1 + 1
			colorButtons[i].setOnClickListener(new OnClickListener() {					// = image_button_2
				public void onClick(View v) {
					int which = v.getId() - R.id.image_button_1; //0: button 1, 1: button 2, 2: button 3
					backgroundColor = BACKGROUND_COLORS[which];
					textColor = TEXT_COLORS[which];
				}
			});
		}
		
		// This button will make two dialogs to pick the two colors.
		Button otherButton = (Button) this.findViewById(R.id.other_button);
		otherButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				OnColorChangedListener textColorListener = new OnColorChangedListener() {
					@Override
					public void colorChanged(int color) {
						textColor = color;
						//When they pick the textColor, open a new dialog to get the backgroundColor.
						OnColorChangedListener bgColorListener = new OnColorChangedListener() {
							@Override
							public void colorChanged(int color) {
								backgroundColor = color;
							}
						};
						ColorPickerDialog bgDialog = new ColorPickerDialog(Options.this, bgColorListener, 
																		   DEFAULT_BACKGROUND, bgMessage);
						bgDialog.show();
					}
				};
				ColorPickerDialog textDialog = new ColorPickerDialog(Options.this, textColorListener, 
																	 DEFAULT_TEXTCOLOR, textMessage);
				textDialog.show();
			}
		});
		
		/*
		 * Set up the wpm section (two buttons and a field).
		 */
		speedField = (EditText) this.findViewById(R.id.speed_box);
		String wpmString = ((Integer) wpm).toString();
		speedField.setText(wpmString, TextView.BufferType.EDITABLE);
		
		ImageButton plus = (ImageButton) this.findViewById(R.id.plus_button);
		plus.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				CharSequence oldText = speedField.getText();	
				try {
					Integer newNum = Integer.parseInt(oldText.toString()) + WPM_INCREMENT;
					speedField.setText(newNum.toString());
				}
				catch(NumberFormatException e) {
					speedField.setText(getString(R.string.default_speed));
				}
			}
		});
		
		ImageButton minus = (ImageButton) this.findViewById(R.id.minus_button);
		minus.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				CharSequence oldText = speedField.getText();
				try {
					Integer newNum = Integer.parseInt(oldText.toString()) - WPM_INCREMENT;
					speedField.setText(newNum.toString());
				}
				catch(NumberFormatException e) {
					speedField.setText(getString(R.string.default_speed));
				}
			}
		});
		
		/*
		 * Set up the wordsAtATime section (two buttons and a field).
		 */
		numWordsField = (EditText) this.findViewById(R.id.num_words_box);
		String numWordsString = ((Integer) wordsAtATime).toString();
		numWordsField.setText(numWordsString, TextView.BufferType.EDITABLE);
		
		ImageButton numWordsPlus = (ImageButton) this.findViewById(R.id.words_plus_button);
		numWordsPlus.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				CharSequence oldText = numWordsField.getText();	
				try {
					Integer newNum = Integer.parseInt(oldText.toString()) + NUM_WORDS_INCREMENT;
					numWordsField.setText(newNum.toString());
				}
				catch(NumberFormatException e) {
					numWordsField.setText(getString(R.string.default_num_words));
				}
			}
		});
		
		ImageButton numWordsMinus = (ImageButton) this.findViewById(R.id.words_minus_button);
		numWordsMinus.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				CharSequence oldText = numWordsField.getText();
				try {
					Integer newNum = Integer.parseInt(oldText.toString()) - NUM_WORDS_INCREMENT;
					numWordsField.setText(newNum.toString());
				}
				catch(NumberFormatException e) {
					numWordsField.setText(getString(R.string.default_num_words));
				}
			}
		});

		/*
		 * Set up the charsAtATime section (two buttons and a field).
		 */
		numCharsField = (EditText) this.findViewById(R.id.num_chars_box);
		String numCharsString = ((Integer) charsAtATime).toString();
		numCharsField.setText(numCharsString, TextView.BufferType.EDITABLE);
		
		ImageButton numCharsPlus = (ImageButton) this.findViewById(R.id.chars_plus_button);
		numCharsPlus.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				CharSequence oldText = numCharsField.getText();	
				try {
					Integer newNum = Integer.parseInt(oldText.toString()) + NUM_CHARS_INCREMENT;
					numCharsField.setText(newNum.toString());
				}
				catch(NumberFormatException e) {
					numCharsField.setText(getString(R.string.default_num_chars));
				}
			}
		});
		
		ImageButton numCharsMinus = (ImageButton) this.findViewById(R.id.chars_minus_button);
		numCharsMinus.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				CharSequence oldText = numCharsField.getText();	
				try {
					Integer newNum = Integer.parseInt(oldText.toString()) - NUM_CHARS_INCREMENT;
					numCharsField.setText(newNum.toString());
				}
				catch(NumberFormatException e) {
					numCharsField.setText(getString(R.string.default_num_chars));
				}
			}
		});
		
		/*
		 * Set up the path field.
		 */
		pathField = (EditText) this.findViewById(R.id.path_box);
		pathField.setText(file.toString());
		pathField.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				location = 0;
				savePrefs();
			}
			
		});
		
		Button browseButton = (Button) this.findViewById(R.id.browse_button);
		browseButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(), FileDialog.class);
				if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					intent.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory().toString());
				} else {
					intent.putExtra(FileDialog.START_PATH, Environment.getRootDirectory().toString());
				}
				startActivityForResult(intent, REQUEST_LOAD);
			}
		});
		
		Button goButton = (Button) this.findViewById(R.id.go_button);
		goButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {				
				go();
			}
		});
	}
	
	protected void onStart() {
		super.onStart();
		
		// Put the default file on the sd card if it isn't there.
		// This is in onStart because they could leave the app, delete the file, and come back.
		// (I've done it.)
		File defaultFile = new File(defaultPath);
		if(!defaultFile.isFile()) {
			File dir = new File(defaultDir);
			dir.mkdirs();
			try {
				defaultFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			makeExternalText();
		}
	}
	
	private void go() {
		// This is kind of a hack in case we come back here after reading a bit.
		// Why the hell does this work? Don't delete it. Seriously.
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		location = settings.getInt("Location", 0);
		
		file = new File(pathField.getText().toString());
		
		try {
			String wpmString = speedField.getText().toString().trim();
			wpm = Integer.parseInt(wpmString);
			if(wpm < 0) {
				wpm *= -1; // e.g. -250 becomes 250
			}
		} catch (NumberFormatException e) {
			speedField.setText(getString(R.string.default_speed));
			wpm = Integer.parseInt(getString(R.string.default_speed)); // if they enter gibberish, 
																	   // set it to 300
		}
		try {
			String numWordsString = numWordsField.getText().toString().trim();
			wordsAtATime = Integer.parseInt(numWordsString);
			if(wordsAtATime <= 0) {
				wordsAtATime = 1; // if they want to disregard this, require it to show at least one word
			}
		} catch(NumberFormatException e) {
			numWordsField.setText(getString(R.string.default_num_words));
			wordsAtATime = Integer.parseInt(getString(R.string.default_num_words));
		}
		try {
			String numCharsString = numCharsField.getText().toString().trim();
			charsAtATime = Integer.parseInt(numCharsString);
		if(charsAtATime <= 0) {
				charsAtATime = 1; // if they want to disregard this, require it to show at least one character
			}
		} catch (NumberFormatException e) {
			numCharsField.setText(getString(R.string.default_num_chars));
			charsAtATime = Integer.parseInt(getString(R.string.default_num_chars));
		}
		savePrefs();
		
		if(file.isFile()) {
			if(file.toString().endsWith(".txt")) {
				startActivity(new Intent(getApplicationContext(), Reader.class));
			}
			else if(file.toString().endsWith(".epub")) {
				/* Set up a loading dialog */
				setUpParsing();
				
				/* Do work */
				Thread parse = new Thread() {
					@Override
					public void run() {
						EpubReader reader = new EpubReader();
						try {
							/* Set up our temporary file. */
							File tempFile = prepareTempFile();
							BufferedWriter out = new BufferedWriter(new FileWriter(tempFile));
							
							/* Get text from the .epub and write it to the file. */
							Spine resources = reader.readEpub(new FileInputStream(file)).getSpine();
							List<SpineReference> things = resources.getSpineReferences();
							for(SpineReference thing : things) {
								String xhtml = new String(thing.getResource().getData());
								String text = Jsoup.parse(xhtml).text();
								out.write(text);
							}
							file = tempFile;
							savePrefs();
							startActivity(new Intent(getApplicationContext(), Reader.class));
						} catch (FileNotFoundException e) {
							// This won't happen.
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
						/* All done; close the loading dialog. */
						handler.sendEmptyMessage(0);
					}
					
					private Handler handler = new Handler() {
			            @Override
			            public void handleMessage(Message msg) {
			            	parsing.dismiss();
			            }
			        };
				};
				parse.start();
			}
			/* If it's a webpage, use Jsoup */
			else if(isWebPage(file)) {
				/* Set up a loading dialog */
				setUpParsing();
				
				/* Do work */
				Thread parse = new Thread() {
					@Override
					public void run() {
						/* Set up our temporary file. */
						File tempFile = prepareTempFile();
						try {
							BufferedWriter out = new BufferedWriter(new FileWriter(tempFile));
							String text = Jsoup.parse(file, getCharset(file)).text();
							out.write(text);
							
							out.flush();
					        out.close();
							file = tempFile;
							savePrefs();
							startActivity(new Intent(getApplicationContext(), Reader.class));
						} catch(IOException e) {
							e.printStackTrace();
						}
						/* All done; close the loading dialog. */
						handler.sendEmptyMessage(0);
					}
					
					private Handler handler = new Handler() {
			            @Override
			            public void handleMessage(Message msg) {
			            	parsing.dismiss();
			            }
			        };
				};
				parse.start();
			}
			else if(file.toString().endsWith(".pdf")) {
				/* Set up a loading dialog */
				setUpParsing();
				
				Thread parse = new Thread() {
					@Override
					public void run() {
						File tempFile = prepareTempFile();
						try {
							PdfReader reader = new PdfReader(file.toString());
							BufferedWriter out = new BufferedWriter(new FileWriter(tempFile));
							int numPages = reader.getNumberOfPages();
							
							/* Do work */
							for(int page = 1; page <= numPages; page++) {
								byte[] streamBytes = reader.getPageContent(page);
						        PRTokeniser tokenizer = new PRTokeniser(streamBytes);
					        	while (tokenizer.nextToken()) {
									if (tokenizer.getTokenType() == PRTokeniser.TokenType.STRING) {
										String str = tokenizer.getStringValue();
										if(str.equals("en")) // TODO (messes with spanish, etc)
											str = " ";
										out.write(str);
								    }
								}
							}
							out.flush();
					        out.close();
					        file = tempFile;
					        savePrefs();
							startActivity(new Intent(getApplicationContext(), Reader.class));
						} catch (IOException e) {
							e.printStackTrace();
						}
						/* All done; close the loading dialog. */
						handler.sendEmptyMessage(0);
					}
					
					private Handler handler = new Handler() {
			            @Override
			            public void handleMessage(Message msg) {
			            	parsing.dismiss();
			            }
			        };
				};
				parse.start();
			}
			else {
				alert(getString(R.string.bad_file)); //tell them if they choose .doc or something
			}
		}
		else {
			alert(getString(R.string.no_file));
		}
	}
	
	/*
	 * Uses InputStream and FileOutputStream to copy the file in assets/SpeedREADME.txt to the sdcard.
	 */
	public void makeExternalText() {
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			InputStream in = null;
			FileOutputStream out = null;
			try {
				in = getAssets().open(getString(R.string.default_file), AssetManager.ACCESS_STREAMING);
				out = new FileOutputStream(defaultPath);
				byte[] buffer = new byte[1024];
			    int read;
			    while((read = in.read(buffer)) != -1){
			      out.write(buffer, 0, read);
			    }
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if(in != null)
						in.close();
					if(out != null)
						out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case(R.id.about):
			startActivity(new Intent(getApplicationContext(), About.class));
			return true;
		case(R.id.bookmark_from_menu):
			startActivityForResult(new Intent(getApplicationContext(),
					BookmarkActivity.class), REQUEST_BOOKMARK);
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private File prepareTempFile() {
		String oldFileName = file.toString();
		String newFileName = oldFileName.substring(0, oldFileName.lastIndexOf('.'));
		newFileName += ".txt";
		File tempFile = new File(newFileName);
		tempFile.delete();
		File dir = new File(defaultDir);
		dir.mkdirs();
		try {
			tempFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tempFile;
	}
	
	private boolean isWebPage(File theFile) {
		boolean answer = false;
		for(String fileType : SUPPORTED_WEB_FORMATS) {
			if(theFile.toString().endsWith(fileType))
				answer = true;
		}
		return answer;
	}
	
	private void setUpParsing() {
		parsing = new ProgressDialog(this);
		parsing.setTitle(getString(R.string.loading));
		parsing.setMessage(getString(R.string.loading_text));
		//parsing.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL); TODO can't change the dialog 
		parsing.setCancelable(false);								// from within the thread?
		//parsing.setProgress(0);	These go along with the
		//parsing.setMax(1);		previous comment.
		parsing.show();
	}
	
	protected synchronized void onActivityResult(final int requestCode, int resultCode, final Intent data) {
		if(resultCode == Activity.RESULT_OK) {
			if(requestCode == REQUEST_LOAD) {
				pathField.setText(data.getStringExtra(FileDialog.RESULT_PATH));
				location = 0;
				savePrefs();
			} else if(requestCode == REQUEST_BOOKMARK) {
				File fromDirectory = new File(defaultDir + getString(R.string.bookmarks_dir) + "/");
				ArrayList<Bookmark> bms = Bookmark.fromDirectory(fromDirectory);
				Bookmark bm = bms.get(data.getIntExtra(BookmarkActivity.INTENT_KEY, 0));
				file = bm.getFile();
				pathField.setText(file.toString());
				location = bm.getLocation(); // This has to go after setText, because setText also
				savePrefs();				 // sets the location to 0.
				/* Start Reader. */
				go();
			}
		} else if(resultCode == Activity.RESULT_CANCELED) {
			System.out.println("File or bookmark not selected");
		}
	}
	
	public void alert(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message)
		       .setNeutralButton(getString(R.string.okay), new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.dismiss();
		           }
		       });
		AlertDialog a = builder.create();
		a.show();
	}
	
	protected static String getCharset(File charsetFile) throws IOException {
		byte[] buf = new byte[4096];

	    java.io.FileInputStream fis = new java.io.FileInputStream(charsetFile);

	    // Construct an instance of org.mozilla.universalchardet.UniversalDetector.
	    UniversalDetector detector = new UniversalDetector(null);

	    // Feed some data (typically several thousands bytes) to the 
	    // detector by calling UniversalDetector.handleData().
	    int nread;
	    while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
	      detector.handleData(buf, 0, nread);
	    }
	    // Notify the detector of the end of data by calling UniversalDetector.dataEnd().
	    detector.dataEnd();

	    // Get the detected encoding name by calling UniversalDetector.getDetectedCharset().
	    String encoding = detector.getDetectedCharset();
	    if (encoding == null) {
	    	System.out.println("No encoding detected.");
	    }

	    // Don't forget to call UniversalDetector.reset() before you reuse the detector instance.
	    detector.reset();
		return encoding;
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		restorePrefs();
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	private void restorePrefs() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		wpm = settings.getInt("WPM", Integer.parseInt(getString(R.string.default_speed)));
		wordsAtATime = settings.getInt("NumWords", Integer.parseInt(getString(R.string.default_num_words)));
		charsAtATime = settings.getInt("NumChars", Integer.parseInt(getString(R.string.default_num_chars))); 
		backgroundColor = settings.getInt("Background", DEFAULT_BACKGROUND);
		textColor = settings.getInt("TextColor", DEFAULT_TEXTCOLOR);
		file = new File(settings.getString("Path", defaultPath));
		location = settings.getInt("Location", 0); // Always defaults to 0. Always.
		donateAlert = settings.getBoolean("Donate", false);
	}
	
	private void savePrefs() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("WPM", wpm);
		editor.putInt("NumWords", wordsAtATime);
		editor.putInt("NumChars", charsAtATime);
		editor.putInt("Background", backgroundColor);
		editor.putInt("TextColor", textColor);
		editor.putString("Path", file.toString());
		editor.putInt("Location", location);
		editor.putBoolean("Donate", donateAlert);
		editor.commit();
	}
}
