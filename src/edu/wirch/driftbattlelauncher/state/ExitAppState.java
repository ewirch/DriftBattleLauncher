package edu.wirch.driftbattlelauncher.state;

import android.util.Log;

public class ExitAppState extends AbstractState {
	private static final String TAG = ExitAppState.class.getName();

	@Override
	public void _start(Object arg) {
		Log.i(TAG, "invoking finish on main activity");
		getMessageHandler().obtainMessage(State.MESSAGE_FINISH_APPLICATION).sendToTarget();
	}

	@Override
	public void onActivityRegistered() {

	}
}
