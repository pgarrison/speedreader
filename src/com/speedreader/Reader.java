package com.speedreader;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;


public class Reader extends Activity {

	private TextSurface switcher;

	//private final int SENTENCE_END_WAIT = 2; // TODO Number of times to wait at the end of a sentence.
											 	//A value of 1 will treat all words equally
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader);
        
        switcher = (TextSurface) findViewById(R.id.switcher);
	}
    
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if(!(keyCode == KeyEvent.KEYCODE_BACK)) // if they are going back, it's all shutdown;
    		switcher.buttonPushed();			// without this, we leak windows.
    	else 
    		switcher.killTheExecutor();
    	return super.onKeyDown(keyCode, event);
    }
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		//Do nothing - it's always landscape
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	protected void onStop() {
		switcher.end();
		super.onStop();
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		this.finish();
	}
}