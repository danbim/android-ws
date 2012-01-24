package com.example;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

public class WebSocketClientActivity extends Activity {

	private ResultReceiver resultReceiver = new ResultReceiver(new Handler()) {
		@Override
		protected void onReceiveResult(final int resultCode, final Bundle resultData) {
			Toast.makeText(getApplicationContext(), resultData.getString("message"), Toast.LENGTH_LONG).show();
		}
	};

	private final ServiceConnection serviceConnection;

	public WebSocketClientActivity() {
		
		serviceConnection = new ServiceConnection() {
			public void onServiceConnected(final ComponentName componentName, final IBinder iBinder) {
				Log.i(WebSocketClientActivity.class.getName(), "onServiceConnected()");
			}

			public void onServiceDisconnected(final ComponentName componentName) {
				Log.i(WebSocketClientActivity.class.getName(), "onServiceDisconnected()");
			}
		};
	}

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Intent serviceIntent = new Intent(this, WebSocketService.class);
		serviceIntent.putExtra("receiver", resultReceiver);
		bindService(serviceIntent, serviceConnection, Service.BIND_AUTO_CREATE);

	}

	@Override
	protected void onStop() {

		super.onStop();
		unbindService(serviceConnection);
	}
}
