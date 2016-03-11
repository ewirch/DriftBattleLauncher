package edu.wirch.driftbattlelauncher.state;

import edu.wirch.driftbattlelauncher.arduino.BtArduino;

public class CommunicatingState extends AbstractState {
	private BtArduino btArduino;

	@Override
	public void _start(Object arg) {
		btArduino = (BtArduino) arg;
		sendReadyForCommunicationToParent();
	}

	private void sendReadyForCommunicationToParent() {
		getMessageHandler().obtainMessage(State.MESSAGE_READY_FOR_COMMUNICATION).sendToTarget();
	}

	public BtArduino getBtArduino() {
		return btArduino;
	}

	@Override
	public void cancel() {
		if (btArduino != null) {
			btArduino.close();
			btArduino = null;
		}
	}

	@Override
	public void onActivityRegistered() {

	}
}
