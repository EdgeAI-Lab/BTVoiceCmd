package com.fan.myvoice;



import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Window;
import android.widget.TextView;

public class Help extends Activity {
	
	private TextView help = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.help);
		help = (TextView) findViewById(R.id.tHelp);
		help.setMovementMethod(new ScrollingMovementMethod());
		
	}

}
