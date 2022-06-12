package com.faddy.testshadowsocks;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.org.outline.CallbackContext;
import com.org.outline.OutlinePlugin;
import com.org.outline.PluginResult;
import com.org.outline.vpn.VpnServiceStarter;
import com.org.outline.vpn.VpnTunnelService;

import org.json.JSONArray;
import org.json.JSONObject;
import org.outline.IVpnTunnelService;
import org.outline.TunnelConfig;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MainActivity extends AppCompatActivity {
    private IVpnTunnelService vpnTunnelService;
    private String errorReportingApiKey = "";
    private StartVpnRequest startVpnRequest;
    private VpnTunnelBroadcastReceiver vpnTunnelBroadcastReceiver =
            new VpnTunnelBroadcastReceiver();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 788712 && resultCode == RESULT_OK) {
            fun2();
            Toast.makeText(this, "fun 2", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "hello", Toast.LENGTH_SHORT).show();

        }
    }

    private Intent fun1() {
        return VpnService.prepare(getBaseContext());
    }

    private void fun2() {
        String action = "";
        JSONObject obj = new JSONObject();
        try {
            obj.put("host", "212.8.243.30");
            obj.put("port", 64772);
            obj.put("password", "kjYT32");
            obj.put("method", "aes-256-gcm");
            JSONArray args = new JSONArray().put(obj);
            final String tunnelId = "23423423";
            Log.d("Hello", String.format(Locale.ROOT, "Starting VPN tunnel %s", "asdasd"));
            final TunnelConfig tunnelConfig;
            try {
                tunnelConfig = VpnTunnelService.makeTunnelConfig(tunnelId /*config*/);
                vpnTunnelService.startTunnel(tunnelConfig);
                Log.d("Hello", String.format(Locale.ROOT, "Started VPN tunnel %s", "asdasd"));
            } catch (Exception e) {
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

        pluginInitialize();

        findViewById(R.id.startShadowsocks).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent prepareVpnIntent = VpnService.prepare(getBaseContext());
                if (prepareVpnIntent != null) {
                    startActivityForResult(prepareVpnIntent, 788712);
                }

            }
        });
        findViewById(R.id.stopShadowSocks).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Context context = getBaseContext();
                //context.unregisterReceiver(vpnTunnelBroadcastReceiver);
                context.unbindService(vpnServiceConnection);

            }
        });
        findViewById(R.id.checkShadowsocks).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this," " + isTunnelActive("23423423"), Toast.LENGTH_SHORT).show();

            }
        });
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
            // Rebind the service so the VPN automatically reconnects if the service process crashed.
            Context context = getBaseContext();
            Intent rebind = new Intent(context, VpnTunnelService.class);
            rebind.putExtra(VpnServiceStarter.AUTOSTART_EXTRA, true);
            // Send the error reporting API key so the potential crash is reported.
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
        private final Map<String, CallbackContext> tunnelStatusListeners = new ConcurrentHashMap<>();

        public VpnTunnelBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String tunnelId = intent.getStringExtra(OutlinePlugin.MessageData.TUNNEL_ID.value);
            if (tunnelId == null) {
                Log.d("Hello", "Tunnel status broadcast missing tunnel ID");
                return;
            }
            CallbackContext callback = tunnelStatusListeners.get(tunnelId);
            if (callback == null) {
                Log.d("Hello", String.format(
                        Locale.ROOT, "Failed to retrieve status listener for tunnel ID %s", tunnelId));
                return;
            }
            int status = intent.getIntExtra(OutlinePlugin.MessageData.PAYLOAD.value, OutlinePlugin.TunnelStatus.INVALID.value);
            Log.d("Hello", String.format(Locale.ROOT, "VPN connectivity changed: %s, %d", tunnelId, status));

            PluginResult result = new PluginResult(PluginResult.Status.OK, status);
            // Keep the tunnel status callback so it can be called multiple times.
            result.setKeepCallback(true);
            callback.sendPluginResult(result);
        }
    }

    private static class StartVpnRequest {
        public final JSONArray args;
        public final CallbackContext callback;

        public StartVpnRequest(JSONArray args, CallbackContext callback) {
            this.args = args;
            this.callback = callback;
        }
    }
}