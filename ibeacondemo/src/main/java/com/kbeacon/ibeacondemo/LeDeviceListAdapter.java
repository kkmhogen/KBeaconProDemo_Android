package com.kbeacon.ibeacondemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvPacketEddyTLM;
import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvPacketIBeacon;
import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvType;
import com.kkmcn.kbeaconlib2.KBeacon;
import com.kbeacon.ibeacondemo.R;


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
			viewHolder.rssiState = view
					.findViewById(R.id.beacon_rssi);

			viewHolder.deviceBattery = view
					.findViewById(R.id.beacon_battery);
			viewHolder.deviceMacAddr =  view
					.findViewById(R.id.beacon_mac_address);
			viewHolder.deviceTempeture = view
					.findViewById(R.id.beacon_temp);
			viewHolder.deviceMajor = view
					.findViewById(R.id.beacon_major);
			viewHolder.deviceMinor = view
					.findViewById(R.id.beacon_minor);
			viewHolder.deviceUUID = view
					.findViewById(R.id.beacon_uuid);

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

		//ibeacon data
		KBAdvPacketIBeacon iBeacon = (KBAdvPacketIBeacon) device.getAdvPacketByType(KBAdvType.IBeacon);
		if (iBeacon != null)
		{
			String strRssiValue = mContext.getString(R.string.BEACON_RSSI_VALUE) + iBeacon.getRssi();
			viewHolder.rssiState.setText(strRssiValue);

			//battery percent
			String strBatteryPercent = mContext.getString(R.string.BEACON_BATTERY) + device.getBatteryPercent();
			viewHolder.deviceBattery.setText(strBatteryPercent);

			//conn major
			String strMajorID = mContext.getString(R.string.BEACON_MAJOR_LIST) + iBeacon.getMajorID();
			viewHolder.deviceMajor.setText(strMajorID);

			//conn minor
			String strMinorID = mContext.getString(R.string.BEACON_MINOR_LIST) + iBeacon.getMinorID();
			viewHolder.deviceMinor.setText(strMinorID);

			//conn minor
			String strUuid =  mContext.getString(R.string.BEACON_UUID) + iBeacon.getUuid();
			viewHolder.deviceUUID.setText(strUuid);
		}

		//eddy tlm
		KBAdvPacketEddyTLM tlmBeacon = (KBAdvPacketEddyTLM) device.getAdvPacketByType(KBAdvType.EddyTLM);
		if (tlmBeacon != null)
		{
			String strTemputure = mContext.getString(R.string.BEACON_TEMP) + tlmBeacon.getTemperature();
			viewHolder.deviceTempeture.setText(strTemputure);
		}

		return view;
	}

	class ViewHolder {
		TextView deviceName;      //名称
		TextView rssiState;     //状态

		TextView deviceBattery;

		TextView deviceMacAddr;


		TextView deviceTempeture;

		TextView deviceMajor;

		TextView deviceMinor;

		TextView deviceUUID;
	}
}
