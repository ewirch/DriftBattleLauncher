package edu.wirch.driftbattlelauncher.state;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import edu.wirch.driftbattlelauncher.ConnectThread;
import edu.wirch.driftbattlelauncher.R;

public class ConnectingState extends AbstractState {
	private ConnectThread connectThread;
	private static final String TAG = ConnectingState.class.getName();
	private ProgressDialog connectingSpinner;
	BluetoothDevice device;

	@Override
	public void _start(Object arg) {
		device = (BluetoothDevice) arg;
		onActivityRegistered();
	}

	@Override
	public void onActivityRegistered() {
		if (getActivity() != null) {
			connectThread = new ConnectThread(device, new Handler(connectionListener));
			connectThread.start();
			showConnectingDialog();
		}
	}

	@Override
	public void cancel() {
		if (connectingSpinner != null) {
			closeSpinner();
		}
		if (connectThread != null) {
			connectThread.cancel();
			connectThread = null;
		}
	}

	private void closeSpinner() {
		connectingSpinner.dismiss();
		connectingSpinner = null;
	}

	private final Callback connectionListener = new Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			connectThread = null;
			switch (msg.what) {
			case ConnectThread.MESSAGE_CONNECTED:
				BluetoothSocket socket = (BluetoothSocket) msg.obj;
				proceedToNextState(socket);
				break;
			case ConnectThread.MESSAGE_FAILURE:
				proceedToErrorState((Throwable) msg.obj);
				break;
			default:
				proceedToErrorState(ConnectingState.class.getName() + ": unknown message from connectThread: " + msg.what);
			}
			return true;
		}

	};

	private void proceedToErrorState(Throwable ex) {
		cancel();
		exitDialog(ex);
	}

	private void proceedToErrorState(String msg) {
		cancel();
		exitDialog(msg);
	}

	private void proceedToNextState(BluetoothSocket socket) {
		closeSpinner();
		getNextState().start(socket);
	}

	private void showConnectingDialog() {
		connectingSpinner = new ProgressDialog(getActivity(), ProgressDialog.STYLE_SPINNER);
		connectingSpinner.setMessage(getApplicationContext().getText(R.string.connectionInProgress));
		connectingSpinner.show();
		connectingSpinner.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				cancel();
				getErrorState().start(null);
			}
		});
	}
}
