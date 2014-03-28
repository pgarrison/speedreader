package com.speedreader;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class About extends Activity implements OnClickListener{

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		
		Button quit = (Button) this.findViewById(R.id.quit_button);
		quit.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		this.finish();		
	}
}
