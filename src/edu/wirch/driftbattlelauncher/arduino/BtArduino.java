package edu.wirch.driftbattlelauncher.arduino;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import edu.wirch.driftbattlelauncher.SettingsActivity;
import edu.wirch.driftbattlelauncher.arduino.ProgramExecutor.Program;
import edu.wirch.driftbattlelauncher.arduino.ProgramExecutor.ProgramLine;
import edu.wirch.driftbattlelauncher.bt.QueueSender;
import edu.wirch.driftbattlelauncher.bt.QueueSender.CommandWithReply;

public class BtArduino {
	private static final int TYPE_RED_AND_GREEN = 42;
	private static final byte TYPE_RED_GREEN_AND_ORANGE = 43;
	public static final int MESSAGE_FAILURE = 1;
	public static final int MESSAGE_TIMEOUT = 2;
	public static final int MESSAGE_CHECK_FOR_ARDUINO_RESULT = 3;

	public static final int TYPE_ARDUINO = 1;
	public static final int TYPE_NOT_ARDUINO = 2;

	private static final byte CMD_PREFIX = 56;
	private static final byte[] CMD_GET_TYPE = { CMD_PREFIX, 7, 0 };
	private static final byte[] CMD_LIGHT_ON = { CMD_PREFIX, 11, 0 };
	private static final byte[] CMD_LIGHT_OFF = { CMD_PREFIX, 12, 0 };
	private static final byte[] CMD_TONE_ON = { CMD_PREFIX, 54, 0 };
	private static final byte[] CMD_TONE_OFF = { CMD_PREFIX, 55, 0 };

	enum AfterStart {
		green_on, delayed_off, delayed_red
	}

	protected static final String TAG = BtArduino.class.getName();
	private static final int DEFAULT_DELAY_BETWEEN_STEPS = 1000;
	private static final int DEFAULT_TONE_DURATION = 1000;
	private static final int AFTER_START_DELAY = 4000;
	private static final boolean DEFAULT_USE_START_TONE = true;
	private static final AfterStart DEFAULT_AFTER_START = AfterStart.delayed_off;
	private int delayBetweenSteps = DEFAULT_DELAY_BETWEEN_STEPS;
	private int toneDuration = DEFAULT_TONE_DURATION;
	private boolean useStartTone = DEFAULT_USE_START_TONE;
	private AfterStart afterStart = DEFAULT_AFTER_START;
	private int redLedCount = 1;
	private int orangeLedCount = 0;
	private int greenLedCount = 1;
	private int ledCount = 2;

	private Handler parentMessageHandler;
	private final QueueSender sender;
	private final ProgramExecutor programExecutor;
	private final SharedPreferences preferences;
	private final OnSharedPreferenceChangeListener preferenceChangeListener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			readPreferences();
		}
	};

	public BtArduino(BluetoothSocket socket, Context appContext) {
		sender = new QueueSender(socket);
		sender.start();
		programExecutor = new ProgramExecutor(sender);
		preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
		preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
		readPreferences();
	}

	private void readPreferences() {
		delayBetweenSteps = preferences.getInt(SettingsActivity.KEY_SWITCH_DURATION, DEFAULT_DELAY_BETWEEN_STEPS);
		toneDuration = preferences.getInt(SettingsActivity.KEY_START_TONE_DURATION, DEFAULT_TONE_DURATION);
		useStartTone = preferences.getBoolean(SettingsActivity.KEY_USE_START_TONE, DEFAULT_USE_START_TONE);
		afterStart = parseAfterStart();
	}

	private AfterStart parseAfterStart() {
		String afterStartString = preferences.getString(SettingsActivity.KEY_AFTER_START, AfterStart.delayed_off.toString());
		if (afterStartString.equals(AfterStart.green_on.toString())) {
			return AfterStart.green_on;
		} else if (afterStartString.equals(AfterStart.delayed_red.toString())) {
			return AfterStart.delayed_red;
		} else {
			return AfterStart.delayed_off;
		}
	}

	public void checkForArduino() {
		sender.sendCommandWithReply(CMD_GET_TYPE, 3, new Handler(responseCallback));
	}

	public void startTrafficLight() {
		programExecutor.cancel();

		List<ProgramLine> steps = new ArrayList<ProgramExecutor.ProgramLine>();

		ProgramLine.Builder lineBuilder = new ProgramLine.Builder();
		for (int i = 0; i < ledCount; i++) {
			lineBuilder.send(getCommandWithData(CMD_LIGHT_OFF, i));
		}
		lineBuilder.send(CMD_TONE_OFF);
		ProgramLine cancelLine = lineBuilder.build();

		// first: all off
		steps.add(cancelLine);

		// then all red on
		lineBuilder = new ProgramLine.Builder();
		for (int i = 0; i < redLedCount; i++) {
			lineBuilder.send(getCommandWithData(CMD_LIGHT_ON, getRedLedNr(i)));
		}
		lineBuilder.delay(delayBetweenSteps);
		steps.add(lineBuilder.build());

		// then turn one off, after another. just for the last one, also turn
		// green on, and horn
		for (int i = 0; i < redLedCount; i++) {
			lineBuilder = new ProgramLine.Builder();

			lineBuilder.send(getCommandWithData(CMD_LIGHT_OFF, getRedLedNr(i)));

			if (isLastRedLed(i)) {
				// last LED
				for (int j = 0; j < greenLedCount; j++) {
					lineBuilder.send(getCommandWithData(CMD_LIGHT_ON, getGreenLedNr(j)));
				}
				if (useStartTone) {
					lineBuilder.send(CMD_TONE_ON);
				}
				lineBuilder.delay(toneDuration);
			} else {
				lineBuilder.delay(delayBetweenSteps);
			}
			steps.add(lineBuilder.build());
		}

		// turn off the buzz
		lineBuilder = new ProgramLine.Builder();
		lineBuilder.send(CMD_TONE_OFF);
		lineBuilder.delay(AFTER_START_DELAY);
		steps.add(lineBuilder.build());

		// last step
		lineBuilder = new ProgramLine.Builder();
		switch (afterStart) {
		case green_on:
			for (int j = 0; j < greenLedCount; j++) {
				lineBuilder.send(getCommandWithData(CMD_LIGHT_ON, getGreenLedNr(j)));
			}
			steps.add(lineBuilder.build());
			break;
		case delayed_red:
			for (int j = 0; j < greenLedCount; j++) {
				lineBuilder.send(getCommandWithData(CMD_LIGHT_OFF, getGreenLedNr(j)));
			}
			for (int j = 0; j < redLedCount; j++) {
				lineBuilder.send(getCommandWithData(CMD_LIGHT_ON, getRedLedNr(j)));
			}
			steps.add(lineBuilder.build());
			break;
		case delayed_off:
			for (int j = 0; j < greenLedCount; j++) {
				lineBuilder.send(getCommandWithData(CMD_LIGHT_OFF, getGreenLedNr(j)));
			}
			steps.add(lineBuilder.build());
			break;
		}

		Program program = new Program(steps, cancelLine);
		programExecutor.execute(program);
	}

	private int getRedLedNr(int index) {
		// red LEDs are attached at the beginning, so index==nr
		return index;
	}

	private int getGreenLedNr(int index) {
		// green LEDs are the last ones
		return redLedCount + orangeLedCount + index;
	}

	private int getOrangeLedNr(int index) {
		// orange LEDs follow red LEDs
		return index + redLedCount;
	}

	private boolean isLastRedLed(int i) {
		return i == redLedCount - 1;
	}

	public void hornOn() {
		sender.sendCommand(CMD_TONE_ON);
		for (int i = 0; i < orangeLedCount; i++) {
			sender.sendCommand(getCommandWithData(CMD_LIGHT_ON, getOrangeLedNr(i)));
		}
	}

	public void hornOff() {
		sender.sendCommand(CMD_TONE_OFF);
		for (int i = 0; i < orangeLedCount; i++) {
			sender.sendCommand(getCommandWithData(CMD_LIGHT_OFF, getOrangeLedNr(i)));
		}
	}

	public void redOn() {
		programExecutor.cancel();

		ProgramLine.Builder cmdBuilder = new ProgramLine.Builder();
		for (int i = 0; i < redLedCount; i++) {
			cmdBuilder.send(getCommandWithData(CMD_LIGHT_ON, getRedLedNr(i)));
		}
		for (int i = 0; i < greenLedCount; i++) {
			cmdBuilder.send(getCommandWithData(CMD_LIGHT_OFF, getGreenLedNr(i)));
		}
		sender.sendCommand(cmdBuilder.build().getCommands());
	}

	public void greenOn() {
		programExecutor.cancel();

		ProgramLine.Builder cmdBuilder = new ProgramLine.Builder();
		for (int i = 0; i < redLedCount; i++) {
			cmdBuilder.send(getCommandWithData(CMD_LIGHT_OFF, getRedLedNr(i)));
		}
		for (int i = 0; i < greenLedCount; i++) {
			cmdBuilder.send(getCommandWithData(CMD_LIGHT_ON, getGreenLedNr(i)));
		}
		sender.sendCommand(cmdBuilder.build().getCommands());
	}

	public void allOff() {
		programExecutor.cancel();

		ProgramLine.Builder cmdBuilder = new ProgramLine.Builder();
		for (int i = 0; i < ledCount; i++) {
			cmdBuilder.send(getCommandWithData(CMD_LIGHT_OFF, i));
		}
		sender.sendCommand(cmdBuilder.build().getCommands());
	}

	private byte[] getCommandWithData(byte[] cmd, int data) {
		byte copy[] = new byte[cmd.length];
		System.arraycopy(cmd, 0, copy, 0, cmd.length);
		copy[2] = (byte) data;
		return copy;
	}

	public void close() {
		programExecutor.cancel();
		sender.cancel();
	}

	public void setParentMessageHandler(Handler parentMessageHandler) {
		this.parentMessageHandler = parentMessageHandler;
	}

	private final Callback responseCallback = new Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			Log.d(TAG, "handleMessage");
			switch (msg.what) {
			case QueueSender.MESSAGE_REPLY_RECIEVED:
				Log.d(TAG, "message reply");
				CommandWithReply command = (CommandWithReply) msg.obj;
				if (command.getCommandBytes()[1] == CMD_GET_TYPE[1]) {
					onRecieve_CheckingForArduino(command);
				}
				break;
			case QueueSender.MESSAGE_FAILURE:
				Log.d(TAG, "message failure");
				if (parentMessageHandler != null) {
					parentMessageHandler.obtainMessage(MESSAGE_FAILURE, 0, 0, msg.obj).sendToTarget();
				}
				break;
			}
			return true;
		}
	};

	private void onRecieve_CheckingForArduino(CommandWithReply command) {
		Log.d(TAG, "onRecieve_CheckingForArduino");
		if (parentMessageHandler != null) {
			Log.d(TAG, "parentMessageHandler != null");
			Throwable failure = command.getFailure();
			if (failure != null) {
				if (failure instanceof TimeoutException) {
					Log.d(TAG, "timeout");
					parentMessageHandler.obtainMessage(MESSAGE_CHECK_FOR_ARDUINO_RESULT, TYPE_NOT_ARDUINO, 0, null).sendToTarget();
				} else {
					Log.d(TAG, "other exception");
					parentMessageHandler.obtainMessage(MESSAGE_FAILURE, 0, 0, failure).sendToTarget();
				}
			} else {
				byte[] data = command.getReply();
				Log.d(TAG, "TYPE REPLY " + Integer.valueOf(data[0]).toString());
				if (data.length >= 3 && data[0] == TYPE_RED_AND_GREEN || data[0] == TYPE_RED_GREEN_AND_ORANGE) {
					Log.d(TAG, "reply correct");
					redLedCount = data[1] & 0xF;
					if (data[0] == TYPE_RED_GREEN_AND_ORANGE) {
						// this version of Arduino software encodes the red led
						// count in the lower nibble
						// and the orange LED count in the higher nibble.
						orangeLedCount = data[1] & 0xF0;
						orangeLedCount >>= 4;
					} else {
						orangeLedCount = 0;
					}

					ledCount = data[2];
					greenLedCount = ledCount - redLedCount - orangeLedCount;
					parentMessageHandler.obtainMessage(MESSAGE_CHECK_FOR_ARDUINO_RESULT, TYPE_ARDUINO, 0, null).sendToTarget();
				} else {
					Log.d(TAG, "wrong reply");
					parentMessageHandler.obtainMessage(MESSAGE_CHECK_FOR_ARDUINO_RESULT, TYPE_NOT_ARDUINO, 0, null).sendToTarget();
				}
			}

		}
	}

}
