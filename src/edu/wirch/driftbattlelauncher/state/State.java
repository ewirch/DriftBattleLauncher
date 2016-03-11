package edu.wirch.driftbattlelauncher.state;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

public interface State {
	static int MESSAGE_NEW_STATE = 1;
	static int MESSAGE_READY_FOR_COMMUNICATION = 2;
	static int MESSAGE_FINISH_APPLICATION = 3;
	static int MESSAGE_MAIN_ACTIVITY_REGISTERED = 4;

	void setMessageHandler(Handler handler);

	void setApplicationContext(Context context);

	void setActivity(Activity activity);

	void start(Object arg);

	void setNextState(State nextState);

	void setErrorState(State errorState);

	void onActivityResult(int requestCode, int resultCode, Intent data);

	void cancel();
}
