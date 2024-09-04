package com.kbeacon.eddystone;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvPacketEddyTLM;
import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvPacketEddyUID;
import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvPacketEddyURL;
import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvType;
import com.kkmcn.kbeaconlib2.KBeacon;

public class LeDeviceListAdapter extends BaseAdapter {

    private final static String LOG_TAG = "LeDeviceListAdapter";

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
			viewHolder.deviceSid = view
					.findViewById(R.id.beacon_sid);
			viewHolder.deviceUrl = view
					.findViewById(R.id.beacon_url);
			viewHolder.deviceNid = view
					.findViewById(R.id.beacon_nid);


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
		}else{
			viewHolder.deviceName.setText("N/A");
		}

		//common field
		String strMacAddress = mContext.getString(R.string.BEACON_MAC_ADDRESS) + device.getMac();
		viewHolder.deviceMacAddr.setText(strMacAddress);

		//rssi
		String strRssiValue = mContext.getString(R.string.BEACON_RSSI_VALUE) + device.getRssi();
		viewHolder.rssiState.setText(strRssiValue);

		//battery percent
		String strBattLvl = mContext.getString(R.string.BEACON_BATTERY) + device.getBatteryPercent();
		viewHolder.deviceBattery.setText(strBattLvl);

		//tlm data
		KBAdvPacketEddyTLM tlmBeacon = (KBAdvPacketEddyTLM) device.getAdvPacketByType(KBAdvType.EddyTLM);
		if (tlmBeacon != null) {
		    String strBatteryLvel = tlmBeacon.getBatteryLevel().toString();
            Log.i(LOG_TAG, "battery level" + strBatteryLvel);

			String strTemp = mContext.getString(R.string.BEACON_TEMP) + tlmBeacon.getTemperature().toString();
			viewHolder.deviceTempeture.setText(strTemp);
		}

		//uid data
		KBAdvPacketEddyUID uidBeacon = (KBAdvPacketEddyUID) device.getAdvPacketByType(KBAdvType.EddyUID);
		if (uidBeacon != null)
		{
			//sid
			String strSid = mContext.getString(R.string.EDDYSTONE_SID) + uidBeacon.getSid();
			viewHolder.deviceSid.setText(strSid);

			//sid
			String strNid = mContext.getString(R.string.EDDYSTONE_NID) + uidBeacon.getNid();
			viewHolder.deviceNid.setText(strNid);
		}

		//eddy url
		KBAdvPacketEddyURL urlBeacon = (KBAdvPacketEddyURL) device.getAdvPacketByType(KBAdvType.EddyURL);
		if (urlBeacon != null)
		{
			String strUrl = mContext.getString(R.string.EDDYSTONE_URL) + urlBeacon.getUrl();
			viewHolder.deviceUrl.setText(strUrl);
		}

		return view;
	}

	class ViewHolder {
		TextView deviceName;      //名称
		TextView rssiState;     //状态

		TextView deviceBattery;

		TextView deviceMacAddr;

		TextView deviceTempeture;

		TextView deviceSid;

		TextView deviceNid;

		TextView deviceUrl;
	}
}
