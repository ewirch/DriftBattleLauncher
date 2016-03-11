package edu.wirch.driftbattlelauncher;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

public class ConnectThread extends Thread {
	public static final int MESSAGE_CONNECTED = 1;

	public static final int MESSAGE_FAILURE = 2;

	private static String TAG = ConnectThread.class.getName();

	private final String SPP_UUI = "00001101-0000-1000-8000-00805F9B34FB";
	private final BluetoothSocket mmSocket;
	private final Handler messageHandler;
	private volatile boolean stopping;

	public ConnectThread(BluetoothDevice device, Handler messageHandler) {
		this.messageHandler = messageHandler;
		try {
			mmSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUI));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void run() {
		stopping = false;
		try {
			Log.d(TAG, "connection started");
			mmSocket.connect();
			Log.d(TAG, "connected");

			messageHandler.obtainMessage(MESSAGE_CONNECTED, 0, 0, mmSocket).sendToTarget();
		} catch (IOException connectException) {
			try {
				mmSocket.close();
				Log.d(TAG, "closed 1");
				if (!stopping) {
					// only send error when not explicitly called cancel()
					messageHandler.obtainMessage(MESSAGE_FAILURE, 0, 0, connectException).sendToTarget();
				}
			} catch (IOException closeException) {
				messageHandler.obtainMessage(MESSAGE_FAILURE, 0, 0, closeException).sendToTarget();
			}
		}
	}

	/** Will cancel an in-progress connection, and close the socket */
	public void cancel() {
		try {
			stopping = true;
			// closing socket, while connecting, will end connect() with an
			// exception
			mmSocket.close();
			Log.d(TAG, "closed 2");
		} catch (IOException e) {
			messageHandler.obtainMessage(MESSAGE_FAILURE, 0, 0, e).sendToTarget();
		}
	}

}
