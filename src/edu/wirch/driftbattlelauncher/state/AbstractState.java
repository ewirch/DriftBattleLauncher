package edu.wirch.driftbattlelauncher.state;

import java.io.PrintWriter;
import java.io.StringWriter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import edu.wirch.driftbattlelauncher.Application;

public abstract class AbstractState implements State {

	private Handler handler;
	private State nextState;
	private State errorState;
	private Context applicationContext;
	private Activity activity;

	@Override
	public void setMessageHandler(Handler handler) {
		this.handler = handler;
	}

	@Override
	public void setApplicationContext(Context context) {
		this.applicationContext = context;
	}

	@Override
	public void setActivity(Activity activity) {
		this.activity = activity;
	}

	@Override
	public void setNextState(State nextState) {
		this.nextState = nextState;
	}

	@Override
	public void setErrorState(State errorState) {
		this.errorState = errorState;
	}

	public State getNextState() {
		return nextState;
	}

	public State getErrorState() {
		return errorState;
	}

	public Handler getMessageHandler() {
		return handler;
	}

	public Context getApplicationContext() {
		return applicationContext;
	}

	public Activity getActivity() {
		return activity;
	}

	Callback appContextMessageConsumer = new Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			if (msg.what == MESSAGE_MAIN_ACTIVITY_REGISTERED) {
				Activity activity = (Activity) msg.obj;
				setActivity(activity);
				onActivityRegistered();
			}
			return false;
		}

	};

	public abstract void onActivityRegistered();

	@Override
	public final void start(Object arg) {
		handler.obtainMessage(State.MESSAGE_NEW_STATE, this).sendToTarget();
		((Application) applicationContext).registerMessageReciever(appContextMessageConsumer);
		try {
			_start(arg);
		} catch (Exception ex) {
			exitDialog(ex);
		}
	}

	public abstract void _start(Object arg);

	private String getStackTrace(Throwable ex) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		return sw.toString();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	}

	public void exitDialog(Throwable exception) {
		String stackTrace = getStackTrace(exception);
		exitDialog(stackTrace);
	}

	public void exitDialog(CharSequence message) {
		AlertDialog dialog = new AlertDialog.Builder(activity).create();
		dialog.setCancelable(false); // blocks back button
		dialog.setMessage(message);
		dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				getErrorState().start(null);
			}
		});
		dialog.show();
	}

	@Override
	public void cancel() {
	}
}
