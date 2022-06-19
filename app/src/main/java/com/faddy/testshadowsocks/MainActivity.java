package com.faddy.testshadowsocks;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.VpnService;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.org.outline.vpn.OutlinePlugin;
import com.org.outline.vpn.VpnServiceStarter;
import com.org.outline.vpn.VpnTunnelService;

import org.json.JSONObject;
import org.outline.IVpnTunnelService;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends AppCompatActivity {
    private IVpnTunnelService shadowSocksVpnTunnelService;
    private final VpnTunnelBroadcastReceiver shadowSocksVpnTunnelBroadcastReceiver = new VpnTunnelBroadcastReceiver();
    final String shadowSocksTunnelId = "23423423";
    final Integer shadowSocksOnActivityResultCode = 788712;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pluginInitialize();
        setContentView(R.layout.activity_main);
        findViewById(R.id.startShadowsocks).setOnClickListener(v -> startingVPN());
        findViewById(R.id.stopShadowSocks).setOnClickListener(v -> stopVPN());
        findViewById(R.id.checkShadowsocks).setOnClickListener(v ->
                Toast.makeText(MainActivity.this, " " + isTunnelActive(shadowSocksTunnelId), Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == shadowSocksOnActivityResultCode && resultCode == RESULT_OK) {
            connectToShadowSocksWithCredential();
        }
    }

    private void connectToShadowSocksWithCredential() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("host", "212.8.243.30");
            obj.put("port", 64772);
            obj.put("password", "kjYT32@");
            obj.put("method", "aes-256-gcm");
            try {
                shadowSocksVpnTunnelService.startTunnel(VpnTunnelService.makeTunnelConfig(shadowSocksTunnelId, obj));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isTunnelActive(final String tunnelId) {
        try {
            return shadowSocksVpnTunnelService.isTunnelActive(tunnelId);
        } catch (Exception e) {
            Log.d("hello",
                    String.format(Locale.ROOT, "Failed to determine if tunnel is active: %s", tunnelId), e);
        }
        return false;
    }


    private void startingVPN() {

        Intent prepareVpnIntent = VpnService.prepare(getApplicationContext());
        if (prepareVpnIntent != null) {
            startActivityForResult(prepareVpnIntent, shadowSocksOnActivityResultCode);
        } else {
            connectToShadowSocksWithCredential();
        }
    }

    private void stopVPN() {
        try {
            shadowSocksVpnTunnelService.stopTunnel(shadowSocksTunnelId);
   /*         getBaseContext().unbindService(vpnServiceConnection);
            getBaseContext().unregisterReceiver(vpnTunnelBroadcastReceiver);*/
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private final ServiceConnection vpnServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            shadowSocksVpnTunnelService = IVpnTunnelService.Stub.asInterface(binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Context context = getBaseContext();
            Intent rebind = new Intent(context, VpnTunnelService.class);
            rebind.putExtra(VpnServiceStarter.AUTOSTART_EXTRA, true);
            String errorReportingApiKey = "";
            rebind.putExtra(OutlinePlugin.MessageData.ERROR_REPORTING_API_KEY.value, errorReportingApiKey);
            context.bindService(rebind, vpnServiceConnection, Context.BIND_AUTO_CREATE);
        }
    };

    protected void pluginInitialize() {
        Context context = getBaseContext();
        IntentFilter broadcastFilter = new IntentFilter();
        broadcastFilter.addAction(OutlinePlugin.Action.ON_STATUS_CHANGE.value);
        broadcastFilter.addCategory(context.getPackageName());
        context.registerReceiver(shadowSocksVpnTunnelBroadcastReceiver, broadcastFilter);
        context.bindService(new Intent(context, VpnTunnelService.class), vpnServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    private static class VpnTunnelBroadcastReceiver extends BroadcastReceiver {
        private final Map<String, String> tunnelStatusListeners = new ConcurrentHashMap<>();

        public VpnTunnelBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String tunnelId = intent.getStringExtra(OutlinePlugin.MessageData.TUNNEL_ID.value);
            if (tunnelId == null) {
                return;
            }
            String callback = tunnelStatusListeners.get(tunnelId);
            if (callback == null) {
                return;
            }
            int status = intent.getIntExtra(OutlinePlugin.MessageData.PAYLOAD.value, OutlinePlugin.TunnelStatus.INVALID.value);
        }
    }

}