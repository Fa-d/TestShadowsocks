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
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.org.outline.android.OutlinePlugin;
import com.org.outline.vpn.VpnServiceStarter;
import com.org.outline.vpn.VpnTunnelService;

import org.json.JSONArray;
import org.json.JSONObject;
import org.outline.IVpnTunnelService;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends AppCompatActivity {
    private IVpnTunnelService vpnTunnelService;
    private final VpnTunnelBroadcastReceiver vpnTunnelBroadcastReceiver = new VpnTunnelBroadcastReceiver();
    final String tunnelId = "23423423";

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 788712 && resultCode == RESULT_OK) {
            fun2();
        }
    }

    private void fun2() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("host", "212.8.243.30");
            obj.put("port", 64772);
            obj.put("password", "kjYT32");
            obj.put("method", "aes-256-gcm");
            JSONArray args = new JSONArray().put(obj);

            Log.d("Hello", String.format(Locale.ROOT, "Starting VPN tunnel %s", "asdasd"));
            try {
                vpnTunnelService.startTunnel(VpnTunnelService.makeTunnelConfig(tunnelId));
                Log.d("Hello", String.format(Locale.ROOT, "Started VPN tunnel %s", "asdasd"));
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("Hello", "Failed to retrieve the tunnel proxy config.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isTunnelActive(final String tunnelId) {
        try {
            return vpnTunnelService.isTunnelActive(tunnelId);
        } catch (Exception e) {
            Log.d("hello",
                    String.format(Locale.ROOT, "Failed to determine if tunnel is active: %s", tunnelId), e);
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.startShadowsocks).setOnClickListener(v -> startingVPN());
        findViewById(R.id.stopShadowSocks).setOnClickListener(v -> stopVPN());
        findViewById(R.id.checkShadowsocks).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, " " + isTunnelActive(tunnelId), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startingVPN() {
        pluginInitialize();
        Intent prepareVpnIntent = VpnService.prepare(getApplicationContext());
        if (prepareVpnIntent != null) {
            startActivityForResult(prepareVpnIntent, 788712);
        } else {
            onActivityResult(788712, RESULT_OK, null);
        }
    }

    private void stopVPN() {
        try {
            vpnTunnelService.stopTunnel(tunnelId);
            getBaseContext().unbindService(vpnServiceConnection);
            getBaseContext().unregisterReceiver(vpnTunnelBroadcastReceiver);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private final ServiceConnection vpnServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            vpnTunnelService = IVpnTunnelService.Stub.asInterface(binder);
            Log.d("Hello", "VPN service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d("Hello", "VPN service disconnected");
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
        context.registerReceiver(vpnTunnelBroadcastReceiver, broadcastFilter);
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
                Log.d("Hello", "Tunnel status broadcast missing tunnel ID");
                return;
            }
            String callback = tunnelStatusListeners.get(tunnelId);
            if (callback == null) {
                Log.d("Hello", String.format(
                        Locale.ROOT, "Failed to retrieve status listener for tunnel ID %s", tunnelId));
                return;
            }
            int status = intent.getIntExtra(OutlinePlugin.MessageData.PAYLOAD.value, OutlinePlugin.TunnelStatus.INVALID.value);
            Log.d("Hello", String.format(Locale.ROOT, "VPN connectivity changed: %s, %d", tunnelId, status));
        }
    }

}