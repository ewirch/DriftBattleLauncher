package edu.wirch.driftbattlelauncher.state;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import edu.wirch.driftbattlelauncher.DeviceDiscoveryActivity;
import edu.wirch.driftbattlelauncher.R;

public class GetDeviceState extends AbstractState {
	private static final int REQUEST_GET_DEVICE = 1;

	@Override
	public void onActivityRegistered() {
		Activity activity = getActivity();
		if (activity != null) {
			Intent i = new Intent(getApplicationContext(), DeviceDiscoveryActivity.class);
			activity.startActivityForResult(i, REQUEST_GET_DEVICE);
		}
	}

	@Override
	public void _start(Object arg) {
		onActivityRegistered();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_GET_DEVICE) {
			if (resultCode == Activity.RESULT_OK) {
				BluetoothDevice device = data.getParcelableExtra(DeviceDiscoveryActivity.EXTRA_BT_DEVICE);
				getNextState().start(device);
			} else {
				exitDialog(getApplicationContext().getText(R.string.btDeviceRequired));
			}
		}
	}
}
