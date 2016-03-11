package edu.wirch.driftbattlelauncher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class DeviceDiscoveryActivity extends Activity implements OnItemClickListener {

	public static final String EXTRA_BT_DEVICE = "extra.bt.device";

	private static final String ADDRESS = "address";

	private static final String NAME = "name";

	private final ArrayList<HashMap<String, String>> deviceListAdapterData = new ArrayList<HashMap<String, String>>();
	private SimpleAdapter deviceListAdapter;
	private ListView devicesList;
	private BluetoothAdapter btAdapter;
	private final HashMap<String, BluetoothDevice> devices = new HashMap<String, BluetoothDevice>();
	private BroadcastReceiver broadcastReciever;
	private View discoveryInProgressFooter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_discovery);
		// Show the Up button in the action bar.
		setupActionBar();

		setUpListView();
	}

	@Override
	protected void onStart() {
		super.onStart();
		createBroadcastReceiver();
		startBtDiscovery();
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(broadcastReciever);
		broadcastReciever = null;
		btAdapter = null;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("devices", devices);
		outState.putSerializable("deviceListAdapterData", deviceListAdapterData);
		super.onSaveInstanceState(outState);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		HashMap<String, BluetoothDevice> savedDevices = (HashMap<String, BluetoothDevice>) savedInstanceState.getSerializable("devices");
		ArrayList<HashMap<String, String>> savedDeviceListAdapterData = (ArrayList<HashMap<String, String>>) savedInstanceState
				.getSerializable("deviceListAdapterData");

		if (savedDevices != null && savedDeviceListAdapterData != null) {
			devices.clear();
			devices.putAll(savedDevices);
			deviceListAdapterData.clear();
			deviceListAdapterData.addAll(savedDeviceListAdapterData);
		}
	}

	private void createBroadcastReceiver() {
		broadcastReciever = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
					BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
						addDevice(device);
					}
				} else if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
					discoveryFinished();
				}
			}
		};
		IntentFilter foundIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		IntentFilter discoveryFinishedIntent = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(broadcastReciever, foundIntent);
		registerReceiver(broadcastReciever, discoveryFinishedIntent);
	}

	private void startBtDiscovery() {
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		devices.clear();
		deviceListAdapterData.clear();
		addBondedDevicesToDevicesList();
		btAdapter.startDiscovery();
	}

	private void discoveryFinished() {
		devicesList.removeFooterView(discoveryInProgressFooter);
	}

	private void addBondedDevicesToDevicesList() {
		Set<BluetoothDevice> bondedDevices = btAdapter.getBondedDevices();
		for (BluetoothDevice device : bondedDevices) {
			addDevice(device);
		}
	}

	private void addDevice(BluetoothDevice device) {
		String key = device.getAddress();
		if (!devices.containsKey(key)) {
			devices.put(key, device);

			HashMap<String, String> item = new HashMap<String, String>();
			item.put(NAME, device.getName());
			item.put(ADDRESS, device.getAddress());
			deviceListAdapterData.add(item);
			deviceListAdapter.notifyDataSetChanged();
		}
	}

	private void setUpListView() {
		devicesList = (ListView) findViewById(R.id.devicesLlistView);

		discoveryInProgressFooter = getLayoutInflater().inflate(R.layout.discovery_in_progress, null, false);
		devicesList.addFooterView(discoveryInProgressFooter);

		deviceListAdapter = new SimpleAdapter(this, deviceListAdapterData, android.R.layout.simple_list_item_2, new String[] { NAME, ADDRESS }, new int[] {
				android.R.id.text1, android.R.id.text2 });
		devicesList.setAdapter(deviceListAdapter);

		devicesList.setOnItemClickListener(this);
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Map<String, String> itemFromDeviceList = deviceListAdapterData.get(position);
		String address = itemFromDeviceList.get(ADDRESS);
		BluetoothDevice device = devices.get(address);
		cancelDiscoveryAndReturnDevice(device);
	}

	private void cancelDiscoveryAndReturnDevice(BluetoothDevice device) {
		btAdapter.cancelDiscovery();

		Intent i = new Intent();
		i.putExtra(EXTRA_BT_DEVICE, device);
		setResult(Activity.RESULT_OK, i);
		finish();
	}

}
