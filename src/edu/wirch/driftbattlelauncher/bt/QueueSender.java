package edu.wirch.driftbattlelauncher.bt;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;
import edu.wirch.driftbattlelauncher.arduino.ByteChainBuilder;

public class QueueSender extends Thread implements BtSender {
	public static final int MESSAGE_REPLY_RECIEVED = 2;
	public static final int MESSAGE_FAILURE = 3;

	private static String TAG = QueueSender.class.getName();
	private static final long COMMUNICATION_TIMEOUT = 2000;

	private final BlockingQueue<Command> queue = new ArrayBlockingQueue<QueueSender.Command>(100);
	private final SocketStream stream;
	private volatile CommandWithReply currentCommand;
	private final Semaphore writePermission = new Semaphore(1);
	private final Handler delayedExecutor = new Handler();
	private Runnable currentTimeoutRunnable = null;
	private boolean exit = false;

	private static class Command {
		private final byte[] commandBytes;

		public Command(byte[] commandBytes) {
			this.commandBytes = commandBytes;
		}

		public byte[] getCommandBytes() {
			return commandBytes;
		}
	}

	public static class CommandWithReply extends Command {
		private final int replyBytesExpected;
		private final Handler replyTarget;
		private byte[] reply = new byte[0];
		private Throwable failure = null;

		public CommandWithReply(byte[] commandBytes, int replyBytesExpected, Handler replyDestination) {
			super(commandBytes);
			this.replyBytesExpected = replyBytesExpected;
			this.replyTarget = replyDestination;
		}

		public byte[] getReply() {
			return reply;
		}

		public Throwable getFailure() {
			return failure;
		}
	}

	public QueueSender(BluetoothSocket socket) {
		stream = new SocketStream(socket, new Handler(streamCallback));
		stream.start();
	}

	@Override
	public void sendCommand(byte[] command) {
		queue.add(new Command(command));
	}

	public void sendCommandWithReply(byte[] command, int replyBytesExpected, Handler replyDestination) {
		queue.add(new CommandWithReply(command, replyBytesExpected, replyDestination));
	}

	@Override
	public void run() {
		while (!exit) {
			try {
				writePermission.acquire();
				Command command = queue.take();
				try {
					stream.write(command.getCommandBytes());
				} catch (IOException e) {
					reportOrLogError(command, e);
				}

				if (command instanceof CommandWithReply) {
					currentCommand = (CommandWithReply) command;
					startTimeout();
				} else {
					// for CommandWithReply the reply will release the semaphore
					writePermission.release();
				}
			} catch (InterruptedException e) {
				// I've been interrupted, so exit
				exit = true;
			}
		}
	}

	public void cancel() {
		exit = true;
		this.interrupt();
		stream.cancel();
	}

	private void reportOrLogError(Command command, Throwable e) {
		if (command instanceof CommandWithReply) {
			Handler messageTarget = ((CommandWithReply) command).replyTarget;
			messageTarget.obtainMessage(MESSAGE_FAILURE, 0, 0, e).sendToTarget();
		} else {
			Log.e(TAG, e.getMessage());
		}
	}

	private final Callback streamCallback = new Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			if (currentCommand != null) {
				switch (msg.what) {
				case SocketStream.MESSAGE_READ:
					currentCommand.reply = new ByteChainBuilder().add(currentCommand.reply).add((byte[]) msg.obj).build();
					if (currentCommand.reply.length >= currentCommand.replyBytesExpected) {
						finishCommandWithReply();
					}
					break;
				case SocketStream.MESSAGE_FAILURE:
					currentCommand.failure = (Throwable) msg.obj;
					finishCommandWithReply();
					break;

				}
			} else {
				Log.e(TAG, "message recieved, but no reply target available: " + msg.what);
			}
			return false;
		}
	};

	private void finishCommandWithReply() {
		resetTimeout();
		CommandWithReply tmpCommand = currentCommand;
		currentCommand = null;
		writePermission.release();
		tmpCommand.replyTarget.obtainMessage(MESSAGE_REPLY_RECIEVED, 0, 0, tmpCommand).sendToTarget();
	}

	private void startTimeout() {
		currentTimeoutRunnable = new Runnable() {
			@Override
			public void run() {
				onTimeout();
			}
		};
		delayedExecutor.postDelayed(currentTimeoutRunnable, COMMUNICATION_TIMEOUT);
	}

	private void resetTimeout() {
		if (currentTimeoutRunnable != null) {
			delayedExecutor.removeCallbacks(currentTimeoutRunnable);
			currentTimeoutRunnable = null;
		}
	}

	private void onTimeout() {
		if (currentCommand != null) {
			currentTimeoutRunnable = null;
			currentCommand.failure = new TimeoutException();
			finishCommandWithReply();
		}
	}
}
