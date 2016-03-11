package edu.wirch.driftbattlelauncher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import edu.wirch.driftbattlelauncher.arduino.BtArduino;
import edu.wirch.driftbattlelauncher.state.State;

public class MainActivity extends Activity implements View.OnTouchListener {

	private static String TAG = MainActivity.class.getName();

	ImageButton hornButton;
	View startBattleButton;
	View redButton;
	View greenButton;
	View allOffButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "create");
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		hornButton = (ImageButton) findViewById(R.id.hornButton);
		hornButton.setOnTouchListener(this);
		startBattleButton = findViewById(R.id.startBattleButton);
		redButton = findViewById(R.id.redButton);
		greenButton = findViewById(R.id.greenButton);
		allOffButton = findViewById(R.id.allOffButton);

		Application app = getMyApplicationContext();
		app.registerMainActivity(this);
		app.registerMessageReciever(stateMessageListener);

		BtArduino btArduino = app.getBtArduino();
		if (btArduino == null) {
			// state not ready yet
			diableUi();
		}

		app.startStateChain();
		// PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "stop");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "destroy");
		getMyApplicationContext().removeMessageReciever(stateMessageListener);
		getMyApplicationContext().registerMainActivity(null);
		super.onDestroy();
	}

	@Override
	protected void onRestart() {
		Log.d(TAG, "onRestart");
		super.onRestart();
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
	}

	private final Callback stateMessageListener = new Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case State.MESSAGE_READY_FOR_COMMUNICATION:
				enableUi();
				return true;
			}
			return false;
		}

	};

	public void onStartBattle(View view) {
		BtArduino btArduino = getMyApplicationContext().getBtArduino();
		if (btArduino != null)
			btArduino.startTrafficLight();
	}

	private void onStartHorn() {
		BtArduino btArduino = getMyApplicationContext().getBtArduino();
		if (btArduino != null)
			btArduino.hornOn();
	}

	private void onEndHorn() {
		BtArduino btArduino = getMyApplicationContext().getBtArduino();
		if (btArduino != null)
			btArduino.hornOff();
	}

	public void onRedClicked(View v) {
		BtArduino btArduino = getMyApplicationContext().getBtArduino();
		if (btArduino != null) {
			btArduino.redOn();
		}
	}

	public void onGreenClicked(View v) {
		BtArduino btArduino = getMyApplicationContext().getBtArduino();
		if (btArduino != null) {
			btArduino.greenOn();
		}
	}

	public void onAllOffClicked(View v) {
		BtArduino btArduino = getMyApplicationContext().getBtArduino();
		if (btArduino != null) {
			btArduino.allOff();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void onSettingsClicked(MenuItem item) {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Application app = getMyApplicationContext();
		app.onActivityResult(requestCode, resultCode, data);
	}

	private Application getMyApplicationContext() {
		return (Application) getApplicationContext();
	}

	private void enableUi() {
		startBattleButton.setEnabled(true);
		hornButton.setEnabled(true);
		redButton.setEnabled(true);
		greenButton.setEnabled(true);
		allOffButton.setEnabled(true);
	}

	private void diableUi() {
		startBattleButton.setEnabled(false);
		hornButton.setEnabled(false);
		redButton.setEnabled(false);
		greenButton.setEnabled(false);
		allOffButton.setEnabled(false);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v == hornButton) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				Log.d(TAG, "horn: down");
				onStartHorn();
			}
			if (event.getAction() == MotionEvent.ACTION_UP) {
				Log.d(TAG, "horn: up");
				onEndHorn();
			}
			return true;
		}
		return false;
	}

}
