package au.id.weston.scott.Watts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// Spark up our battery monitoring service no matter if the UI is started
// or not.

public class WattsServiceMinder extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("WattsServiceMinder", "intent: " + intent);
		context.startService(new Intent(context, WattsService.class));
	}
}