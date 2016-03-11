package edu.wirch.driftbattlelauncher.state;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import edu.wirch.driftbattlelauncher.R;

public class EnableBluetoothAdapterState extends AbstractState {

	private static final int REQUEST_ENABLE_BT = 1;

	@Override
	public void onActivityRegistered() {
		if (getActivity() != null) {
			BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
			if (btAdapter == null) {
				exitDialog(getActivity().getText(R.string.btNotSupported));
			} else {
				if (!btAdapter.isEnabled()) {
					Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					getActivity().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
				} else {
					getNextState().start(null);
				}
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == Activity.RESULT_OK) {
				getNextState().start(null);
			} else {
				exitDialog(getActivity().getText(R.string.btEnablingFailed));
			}

		}
	}

	@Override
	public void _start(Object arg) {
		onActivityRegistered();
	}

}
