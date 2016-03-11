package edu.wirch.driftbattlelauncher.state;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import edu.wirch.driftbattlelauncher.R;
import edu.wirch.driftbattlelauncher.arduino.BtArduino;

public class CheckForArduinoState extends AbstractState {
	private BtArduino btArduino;
	private ProgressDialog checkingDialog;
	BluetoothSocket socket;

	@Override
	public void _start(Object arg) {
		socket = (BluetoothSocket) arg;
		onActivityRegistered();
	}

	@Override
	public void onActivityRegistered() {
		if (getActivity() != null) {
			btArduino = new BtArduino(socket, getApplicationContext());
			btArduino.setParentMessageHandler(new Handler(btArduinoCallback));
			btArduino.checkForArduino();
			showCheckingDialog();
		}
	}

	@Override
	public void cancel() {
		if (checkingDialog != null) {
			closeSpinner();
		}
		if (btArduino != null) {
			btArduino.close();
			btArduino = null;
		}
	}

	private void closeSpinner() {
		checkingDialog.dismiss();
		checkingDialog = null;
	}

	private void proceedToErrorState(Throwable ex) {
		cancel();
		exitDialog(ex);
	}

	private void proceedToErrorState(CharSequence msg) {
		cancel();
		exitDialog(msg);
	}

	private void proceedToNextState() {
		closeSpinner();

		BtArduino tmpBtArduino = btArduino;
		// we pass controll of btArduino to the next state. Set the reference to
		// null so this state will not interfere with the next.
		btArduino.setParentMessageHandler(null);
		btArduino = null;
		getNextState().start(tmpBtArduino);
	}

	private void showCheckingDialog() {
		checkingDialog = new ProgressDialog(getActivity(), ProgressDialog.STYLE_SPINNER);
		checkingDialog.setMessage(getApplicationContext().getText(R.string.checkingArduino));
		checkingDialog.show();
		checkingDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				cancel();
				getErrorState().start(null);
			}
		});
	}

	private final Callback btArduinoCallback = new Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case BtArduino.MESSAGE_FAILURE:
				proceedToErrorState((Throwable) msg.obj);
				break;
			case BtArduino.MESSAGE_TIMEOUT:
				proceedToErrorState(getApplicationContext().getText(R.string.btTimeout));
				break;
			case BtArduino.MESSAGE_CHECK_FOR_ARDUINO_RESULT:
				if (msg.arg1 == BtArduino.TYPE_ARDUINO) {
					proceedToNextState();
				} else {
					proceedToErrorState(getApplicationContext().getText(R.string.notAnArduino));
				}
				break;
			}
			return false;
		}
	};
}
