package org.fitark.edrive1;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

public class MainActivity extends Activity {

	private BluetoothAdapter mBluetoothAdapter;
	private final static int REQUEST_ENABLE_BT = 2001;
	private BluetoothDevice mDevice;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// 使用此检查确定BLE是否支持在设备上，然后你可以有选择性禁用BLE相关的功能
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "当前手机不支持ble", Toast.LENGTH_SHORT).show();
			finish();
		}

		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_OK) {
				scanLeDevice(true); // 开始扫描设备
			} else {
				// finish();
			}
		}
	}

	private boolean mScanning;
	private Handler mHandler;
	// 10秒后停止寻找.
	private static final long SCAN_PERIOD = 10000;

	private void scanLeDevice(final boolean enable) {
		if (enable) {
			// 经过预定扫描期后停止扫描
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = false;
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
				}
			}, SCAN_PERIOD);
			mScanning = true;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
	}

	private List<BluetoothDevice> scan_devices = new ArrayList<BluetoothDevice>();
	protected BluetoothGatt mBluetoothGatt;
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			int i = 0;
			// 检查是否是搜索过的设备，并且更新
			for (i = 0; i < scan_devices.size(); i++) {
				if (0 == device.getAddress().compareTo(
						scan_devices.get(i).getAddress())) {
					return;
				}
			}
			// 增加新设备
			scan_devices.add(device);
			if ("EDRIVE".equals(device.getName())) {
				scanLeDevice(false);
				mDevice = device;
				mBluetoothGatt = device.connectGatt(getApplicationContext(),
						false, mGattCallback);

				return;
			}

		}

	};
	public final static String ACTION_DATA_CHANGE = "org.fitark.edrive.bluetooth.le.ACTION_DATA_CHANGE";
	public final static String ACTION_RSSI_READ = "org.fitark.edrive.bluetooth.le.ACTION_RSSI_READ";
	public final static String ACTION_STATE_CONNECTED = "org.fitark.edrive.bluetooth.le.ACTION_STATE_CONNECTED";
	public final static String ACTION_STATE_DISCONNECTED = "org.fitark.edrive.bluetooth.le.ACTION_STATE_DISCONNECTED";
	public final static String ACTION_WRITE_OVER = "org.fitark.edrive.bluetooth.le.ACTION_WRITE_OVER";
	public final static String ACTION_READ_OVER = "org.fitark.edrive.bluetooth.le.ACTION_READ_OVER";
	public final static String ACTION_READ_Descriptor_OVER = "org.fitark.edrive.bluetooth.le.ACTION_READ_Descriptor_OVER";
	public final static String ACTION_WRITE_Descriptor_OVER = "org.fitark.edrive.bluetooth.le.ACTION_WRITE_Descriptor_OVER";
	public final static String ACTION_ServicesDiscovered_OVER = "org.fitark.edrive.bluetooth.le.ACTION_ServicesDiscovered_OVER";
	private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			super.onConnectionStateChange(gatt, status, newState);
			if (newState == BluetoothProfile.STATE_CONNECTED) { // 链接成功
				System.out.println("CONNECTED");
				mBluetoothGatt.discoverServices();
				broadcastUpdate(ACTION_STATE_CONNECTED);

			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) { // 断开链接
				System.out.println("UNCONNECTED");
				broadcastUpdate(ACTION_STATE_DISCONNECTED);
			}

		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			super.onServicesDiscovered(gatt, status);
			if (status == BluetoothGatt.GATT_SUCCESS) {
				System.out.println("onServicesDiscovered");
				broadcastUpdate(ACTION_ServicesDiscovered_OVER, status);
			}
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			super.onDescriptorRead(gatt, descriptor, status);

			broadcastUpdate(ACTION_READ_Descriptor_OVER, status);
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicRead(gatt, characteristic, status);

			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_READ_OVER, characteristic.getValue());
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			super.onCharacteristicChanged(gatt, characteristic);
			broadcastUpdate(ACTION_DATA_CHANGE, characteristic.getValue());
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicWrite(gatt, characteristic, status);
			broadcastUpdate(ACTION_WRITE_OVER, status);
		}

	};

	// 发送广播消息
	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent(action);
		sendBroadcast(intent);
	}

	// 发送广播消息
	private void broadcastUpdate(final String action, int value) {
		final Intent intent = new Intent(action);
		intent.putExtra("value", value);
		sendBroadcast(intent);
	}

	// 发送广播消息
	private void broadcastUpdate(final String action, byte value[]) {
		final Intent intent = new Intent(action);
		intent.putExtra("value", value);
		sendBroadcast(intent);
	}

}
