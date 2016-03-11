package edu.wirch.driftbattlelauncher;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.preference.PreferenceManager;
import edu.wirch.driftbattlelauncher.arduino.BtArduino;
import edu.wirch.driftbattlelauncher.state.CheckForArduinoState;
import edu.wirch.driftbattlelauncher.state.CommunicatingState;
import edu.wirch.driftbattlelauncher.state.ConnectingState;
import edu.wirch.driftbattlelauncher.state.EnableBluetoothAdapterState;
import edu.wirch.driftbattlelauncher.state.ExitAppState;
import edu.wirch.driftbattlelauncher.state.GetDeviceState;
import edu.wirch.driftbattlelauncher.state.State;

public class Application extends android.app.Application {

	private static final String TAG = "Application";

	private final Callback stateMessageListener = new Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case State.MESSAGE_NEW_STATE:
				currentState = (State) msg.obj;
				return true;
			case State.MESSAGE_READY_FOR_COMMUNICATION:
				return dispatchMessage(msg);
			case State.MESSAGE_FINISH_APPLICATION:
				finish();
				return true;
			}
			return false;
		}

	};
	private final Handler handler = new Handler(stateMessageListener);
	private final List<State> states = new ArrayList<State>();
	private State currentState;
	private final List<Callback> messageReciever = new ArrayList<Handler.Callback>();
	private boolean started = false;
	private MainActivity mainActivity;

	@Override
	public void onCreate() {
		super.onCreate();
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		setupStateChain();
	}

	@Override
	public void onTerminate() {
		if (currentState != null) {
			currentState.cancel();
		}
		super.onTerminate();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (currentState != null) {
			currentState.onActivityResult(requestCode, resultCode, data);
		}
	}

	public void registerMessageReciever(Callback callback) {
		messageReciever.add(callback);
	}

	public void removeMessageReciever(Callback callback) {
		if (callback != null) {
			messageReciever.remove(callback);
		}
	}

	public void startStateChain() {
		if (!started) {
			started = true;
			currentState.start(null);
		}
	}

	private void setupStateChain() {
		ExitAppState exitApp = configureState(new ExitAppState());
		EnableBluetoothAdapterState enableBluetoothAdapterState = configureState(new EnableBluetoothAdapterState());
		GetDeviceState getDevice = configureState(new GetDeviceState());
		ConnectingState connecting = configureState(new ConnectingState());
		CheckForArduinoState checkForArduino = configureState(new CheckForArduinoState());
		CommunicatingState communicating = configureState(new CommunicatingState());

		enableBluetoothAdapterState.setErrorState(exitApp);
		enableBluetoothAdapterState.setNextState(getDevice);
		getDevice.setErrorState(exitApp);
		getDevice.setNextState(connecting);
		connecting.setErrorState(getDevice);
		connecting.setNextState(checkForArduino);
		checkForArduino.setErrorState(getDevice);
		checkForArduino.setNextState(communicating);
		communicating.setErrorState(getDevice);

		currentState = enableBluetoothAdapterState;
	}

	private <T extends State> T configureState(T state) {
		state.setApplicationContext(getApplicationContext());
		state.setMessageHandler(handler);
		states.add(state);
		return state;
	}

	private boolean dispatchMessage(Message msg) {
		for (Callback callback : messageReciever) {
			if (callback.handleMessage(msg))
				return true;
		}
		return false;
	}

	public void registerMainActivity(MainActivity mainActivity) {
		this.mainActivity = mainActivity;
		for (State state : states) {
			state.setActivity(mainActivity);
		}
	}

	public BtArduino getBtArduino() {
		BtArduino btArduino = null;
		if (currentState != null) {
			if (currentState instanceof CommunicatingState) {
				btArduino = ((CommunicatingState) currentState).getBtArduino();
			}
		}
		return btArduino;
	}

	private void finish() {
		if (mainActivity != null) {
			mainActivity.finish();
		}
	}

}
