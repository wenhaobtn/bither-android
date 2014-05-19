/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.util;

import net.bither.BitherApplication;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

public class NetworkUtil {
	private NetworkUtil() {

	}

	public enum NetworkType {
		Wifi, ThirdG, NoConnect,
	}

	public static boolean BluetoothIsConnected() {
		try {
			BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			int state = adapter.getState();
			return state == BluetoothAdapter.STATE_ON;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean isConnected() {
		// BluetoothDevice.ACTION_ACL_CONNECTED;
		// The base Context in the ContextWrapper has not been set yet, which is
		// causing the NullPointerException
		try {
			ConnectivityManager ConnectivityManager = (ConnectivityManager) BitherApplication.mContext
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo netinfo = ConnectivityManager.getActiveNetworkInfo();
			if (netinfo == null) {
				return false;
			} else {
				return netinfo.isAvailable();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * judge whether the network connection
	 */
	public static NetworkType isConnectedType() {
		try {
			ConnectivityManager mConnectivity = (ConnectivityManager) BitherApplication.mContext
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			TelephonyManager mTelephony = (TelephonyManager) BitherApplication.mContext
					.getSystemService(Context.TELEPHONY_SERVICE);
			NetworkInfo info = mConnectivity.getActiveNetworkInfo();
			if (info == null)
				return NetworkType.NoConnect;
			int netType = info.getType();
			int netSubtype = info.getSubtype();
			if (netType == android.net.ConnectivityManager.TYPE_WIFI
					&& info.isConnectedOrConnecting())
				return NetworkType.Wifi;
			else if (netType == android.net.ConnectivityManager.TYPE_MOBILE
					&& netSubtype == TelephonyManager.NETWORK_TYPE_UMTS
					&& !mTelephony.isNetworkRoaming()) {
				if (info.isConnected()) {
					return NetworkType.ThirdG;
				} else
					return NetworkType.NoConnect;
			} else
				return NetworkType.NoConnect;
		} catch (Exception e) {
			LogUtil.w("Exception", e.getMessage() + "\n" + e.getStackTrace());
			return NetworkType.NoConnect;
		}

	}

}
