package com.speedreader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.ArrayList;

public class Bookmark implements Serializable{
	
	/*
	 * Uhm, what?
	 */
	private static final long serialVersionUID = -1158002922929658102L;

	private final static String NEW_LINE = "\n";
	private final static Integer FIRST_BM = 1; // The Bookmark files start from 1, not 0
	protected final static String INDEX_NAME = "index";
	
	protected File file;
	protected int location;
	protected String words;
	protected File bmFile; // The location of the bookmark itself
	
	/*
	 * Only for being serializable
	 */
	public Bookmark() {
	}
	
	public Bookmark(File theFile, int theLocation, String someWords) {
		file = theFile;
		location = theLocation;
		words = someWords;
	}
	
	/*
	 * Adds this Bookmark to the directory toDir.
	 * 
	 * Call newBookmarksDir() before this.
	 */
	protected void addBookmark(File toDir) {
		File indexFile = new File(toDir + INDEX_NAME);
		int numBookmarks;
		try {
			if(indexFile.exists() && indexFile.isFile()) {
				ObjectInputStream indexOis = new ObjectInputStream(new FileInputStream(indexFile));
				numBookmarks = indexOis.readInt();
				indexOis.close();
			} else {
				indexFile.createNewFile();
				numBookmarks = 0;
			}
			numBookmarks++;
			ObjectOutputStream indexOos = new ObjectOutputStream(new FileOutputStream(indexFile));
			indexOos.writeInt(numBookmarks);
			indexOos.flush();
			indexOos.close();
			
			bmFile = new File(toDir + Integer.toString(numBookmarks));
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(bmFile)); 
			oos.writeObject(this);
			oos.flush();
			oos.close();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Converts fromFile to ArrayList<Bookmark>
	 * TODO should throw some exception(s)
	 */
	protected static ArrayList<Bookmark> fromDirectory(File fromDirectory) {
		File indexFile = new File(fromDirectory + INDEX_NAME);
		ArrayList<Bookmark> bms = new ArrayList<Bookmark>();
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(indexFile));
			int numBookmarks = ois.readInt();
			ois.close();
			for(Integer which = FIRST_BM; which <= numBookmarks; which++) {
				File bmFile = new File(fromDirectory + which.toString());
				if(bmFile.exists() && bmFile.isFile()) {
					ObjectInputStream bmOis = new ObjectInputStream(new FileInputStream(bmFile));
					Bookmark bm = (Bookmark) bmOis.readObject();
					bms.add(0, bm); // Put new ones at the beginning
					bmOis.close();
				}
			}
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return bms;
	}
	
	protected void suicide() {
		if(bmFile != null && bmFile.exists()) // Once got a NullPointerException
			bmFile.delete();
	}
	
	protected static void newBookmarksDir(File where, Bookmark defaultBookmark) {
		where.mkdirs();
		File indexFile = new File(where + INDEX_NAME);
		try {
			ObjectOutputStream indexOos = new ObjectOutputStream(new FileOutputStream(indexFile)); 
			indexOos.writeInt(1);
			indexOos.flush();
			indexOos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File defaultBmFile = new File(where + FIRST_BM.toString()); // TODO put this somewhere else
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(defaultBmFile)); 
			oos.writeObject(defaultBookmark);
			oos.flush();
			oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String toString() {
		String fileName = file.toString();
		int beginning = fileName.lastIndexOf(file.separatorChar) + 1;
		String shortFile = fileName.substring(beginning, fileName.length());
		return(shortFile + NEW_LINE + words);
	}
	
	protected File getFile() {
		return file;
	}
	
	protected int getLocation() {
		return location;
	}
}
