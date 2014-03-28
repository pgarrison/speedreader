package com.speedreader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Scanner;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class BookmarkActivity extends ListActivity {
	
	protected final static String INTENT_KEY = "ThePosition";
	private String defaultDir;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED, getIntent());
		
		defaultDir = Environment.getExternalStorageDirectory() + "/Android/data/"
		+ getPackageName() + "/files/";
		final Context context = this;
		
		refresh();
		
		// Set the onLongClickListener
		getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() { 
			int thePosition;
			
			@Override 
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				thePosition = position; 
				
				/*
				 * Make a dialog and ask if they want to delete it.
				 */
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage(getString(R.string.delete_bookmark))
				       .setCancelable(true)
				       .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
				    	   public void onClick(DialogInterface dialog, int id) {
				    		  removeView();
				    	   }
				       })
				       .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
				    	   public void onClick(DialogInterface dialog, int id) {
				    		   dialog.cancel();
				    	   }
				       });
				AlertDialog alert = builder.create();
				alert.show();
				return true; // We have consumed the event; don't call onClick(). 
			}
			
			public void removeView() {
				Bookmark deadBm = (Bookmark) getListAdapter().getItem(thePosition);
				deadBm.suicide(); // Delete it's file.
				refresh();
			}
		}); 
	}
	
	private void refresh() {
		// Prepare list
		List<Bookmark> bookmarksList = getBookmarks();
		
		// Put all the things on the screen
		ArrayAdapter<Bookmark> adapter = new ArrayAdapter<Bookmark>(this, R.layout.list_item, 
																	bookmarksList);
		setListAdapter(adapter);
		adapter.notifyDataSetChanged();
	}
	
	protected List<Bookmark> getBookmarks() {
		File fromDir = new File(defaultDir + getString(R.string.bookmarks_dir) + "/");
		File indexFile = new File(fromDir + Bookmark.INDEX_NAME);
		// First time
		if(!indexFile.exists()) {
			setUpBookmarksFile(fromDir);
		} else if(!indexFile.isFile()) {
			setUpBookmarksFile(fromDir);
		}
		
		return Bookmark.fromDirectory(fromDir);
	}

	private void setUpBookmarksFile(File where) {
		File defaultFile = new File(defaultDir + getString(R.string.default_file));
		Bookmark bm = null;
		
		try {
			/* The snippet will be like: "blah blah blah blah blah blah ..." */
			Scanner scan = new Scanner(new FileReader(defaultFile));
			int numWords = getResources().getInteger(R.integer.words_in_bookmark_description);
			String words = "\"";
			for(int i = 0; i < numWords && scan.hasNext(); i++) {
				words += scan.next();
				words += " ";
			}
			scan.close();
			words += "...\"";
			
			bm = new Bookmark(defaultFile, 0, words); // Default to beginning of file.
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		
		Bookmark.newBookmarksDir(where, bm);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		getIntent().putExtra(INTENT_KEY, position); // position is also the index of 
												   // the Bookmark in the file
												   // TODO do this better
		setResult(RESULT_OK, getIntent());
		this.finish();
	}
}
