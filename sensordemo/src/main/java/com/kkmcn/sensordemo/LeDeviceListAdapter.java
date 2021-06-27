package com.kkmcn.sensordemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAccSensorValue;
import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvPacketEddyTLM;
import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvPacketSensor;
import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvType;
import com.kkmcn.kbeaconlib2.KBeacon;


public class LeDeviceListAdapter extends BaseAdapter {

	// Adapter for holding devices found through scanning.
	public interface ListDataSource {
		KBeacon getBeaconDevice(int nIndex);

		int getCount();
	}

	private ListDataSource mDataSource;
	private Context mContext;

	public LeDeviceListAdapter(ListDataSource c, Context ctx) {
		super();
		mDataSource = c;
		mContext = ctx;
	}

	@Override
	public int getCount() {
		return mDataSource.getCount();
	}

	@Override
	public Object getItem(int i) {
		return mDataSource.getBeaconDevice(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
	}


	@Override
	public View getView(int i, View view, ViewGroup viewGroup)
	{
		ViewHolder viewHolder;
		// General ListView optimization code.
		if (view == null)
		{
			view = LayoutInflater.from(mContext).inflate(R.layout.listitem_device, null);
			viewHolder = new ViewHolder();
			viewHolder.deviceName = view
					.findViewById(R.id.beacon_name);

			viewHolder.deviceMacAddr =  view
					.findViewById(R.id.beacon_mac_address);

			viewHolder.rssiState = view
					.findViewById(R.id.beacon_rssi);

			viewHolder.deviceBatteryVoltage = view
					.findViewById(R.id.beacon_battery_voltage);

			viewHolder.deviceTemperature = view
					.findViewById(R.id.beacon_temperature);

			viewHolder.deviceHumidity = view
					.findViewById(R.id.beacon_humidity);

			viewHolder.deviceAccPosition = view
					.findViewById(R.id.beacon_acc_position);

			view.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) view.getTag();
		}

		KBeacon device = mDataSource.getBeaconDevice(i);
		if (device == null) {
			return null;
		}

		if (device.getName() != null && device.getName().length() > 0) {
			viewHolder.deviceName.setText(device.getName());
		}

		//common field
		String strMacAddress = mContext.getString(R.string.BEACON_MAC_ADDRESS) + device.getMac();
		viewHolder.deviceMacAddr.setText(strMacAddress);

		String strRssiValue = mContext.getString(R.string.BEACON_RSSI_VALUE) + device.getRssi();
		viewHolder.rssiState.setText(strRssiValue);

		//ibeacon data
		KBAdvPacketEddyTLM eddyTLM = (KBAdvPacketEddyTLM) device.getAdvPacketByType(KBAdvType.EddyTLM);
		KBAdvPacketSensor kSensor = (KBAdvPacketSensor) device.getAdvPacketByType(KBAdvType.Sensor);
		if (eddyTLM != null)
		{
			//battery percent
			String strBatteryPercent = mContext.getString(R.string.BEACON_BATTERY) + eddyTLM.getBatteryLevel();
			viewHolder.deviceBatteryVoltage.setText(strBatteryPercent);

			//temperature
			String strMajorID = mContext.getString(R.string.BEACON_TEMPERATURE) + eddyTLM.getTemperature();
			viewHolder.deviceTemperature.setText(strMajorID);
		}

		//eddy tlm
		if (kSensor != null)
		{
			if (kSensor.getBatteryLevel() != null)
			{
				String strTemp = mContext.getString(R.string.BEACON_VOLTAGE) + kSensor.getBatteryLevel() + "mV";
				viewHolder.deviceBatteryVoltage.setText(strTemp);
			}

			if (kSensor.getTemperature() != null)
			{
				String strTemp = mContext.getString(R.string.BEACON_TEMPERATURE) + kSensor.getTemperature() + "℃";
				viewHolder.deviceTemperature.setText(strTemp);
			}

			if (kSensor.getHumidity() != null)
			{
				String strTemp = mContext.getString(R.string.BEACON_HUM) + kSensor.getHumidity() + "%";
				viewHolder.deviceHumidity.setText(strTemp);
			}

			KBAccSensorValue accSensorValue = kSensor.getAccSensor();
			if (accSensorValue != null)
			{
				String strTemp = mContext.getString(R.string.BEACON_ACC_POS) + "x=" + accSensorValue.xAis
						+ ",y=" + accSensorValue.yAis + ",z=" + accSensorValue.zAis;
				viewHolder.deviceAccPosition.setText(strTemp);
			}
		}

		return view;
	}

	class ViewHolder {
		TextView deviceName;      //名称

		TextView rssiState;     //状态

		TextView deviceBatteryVoltage;

		TextView deviceMacAddr;

		TextView deviceTemperature;

		TextView deviceHumidity;

		TextView deviceAccPosition;
	}
}
