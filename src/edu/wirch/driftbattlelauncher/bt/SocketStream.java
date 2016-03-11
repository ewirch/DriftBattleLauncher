package edu.wirch.driftbattlelauncher.bt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;

public class SocketStream extends Thread {
	public static final int MESSAGE_READ = 1;
	public static final int MESSAGE_FAILURE = 2;

	private final BluetoothSocket mmSocket;
	private final InputStream mmInStream;
	private final OutputStream mmOutStream;
	private final Handler messageHandler;
	private volatile boolean closing;

	public SocketStream(BluetoothSocket socket, Handler messageHandler) {
		try {
			closing = false;
			mmSocket = socket;
			this.messageHandler = messageHandler;
			mmInStream = socket.getInputStream();
			mmOutStream = socket.getOutputStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void run() {
		byte[] buffer = new byte[1024];
		int bytes;

		// Keep listening to the InputStream until an exception occurs
		while (!closing) {
			try {
				bytes = mmInStream.read(buffer);
				messageHandler.obtainMessage(MESSAGE_READ, bytes, -1, cloneArray(buffer, bytes)).sendToTarget();
			} catch (IOException e) {
				if (!closing)
					messageHandler.obtainMessage(MESSAGE_FAILURE, 0, 0, e).sendToTarget();
			}
		}
	}

	private byte[] cloneArray(byte[] buffer, int bytes) {
		byte[] bufferCopy = new byte[bytes];
		System.arraycopy(buffer, 0, bufferCopy, 0, bytes);
		return bufferCopy;
	}

	/* Call this from the main activity to send data to the remote device */
	public void write(byte[] bytes) throws IOException {
		mmOutStream.write(bytes);
	}

	/* Call this from the main activity to shutdown the connection */
	public void cancel() {
		try {
			closing = true;
			this.interrupt();
			mmInStream.close();
			mmOutStream.close();
			mmSocket.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}