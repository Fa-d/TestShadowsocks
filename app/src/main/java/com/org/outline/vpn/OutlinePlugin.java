// Copyright 2018 The Outline Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.org.outline.vpn;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class OutlinePlugin {
    private static final Logger LOG = Logger.getLogger(OutlinePlugin.class.getName());

    // Actions supported by this plugin.
    public enum Action {
        START("start"),
        STOP("stop"),
        ON_STATUS_CHANGE("onStatusChange"),
        IS_RUNNING("isRunning"),
        IS_REACHABLE("isServerReachable"),
        INIT_ERROR_REPORTING("initializeErrorReporting"),
        REPORT_EVENTS("reportEvents"),
        QUIT("quitApplication");

        private final static Map<String, Action> actions = new HashMap<>();

        static {
            for (Action action : Action.values()) {
                actions.put(action.value, action);
            }
        }

        // Returns whether |value| is a defined action.
        public static boolean hasValue(final String value) {
            return actions.containsKey(value);
        }

        public final String value;

        Action(final String value) {
            this.value = value;
        }

        // Returns whether |action| is the underlying value of this instance.
        public boolean is(final String action) {
            return this.value.equals(action);
        }
    }

    // Plugin error codes. Keep in sync with outlinePlugin.js.
    public enum ErrorCode {
        NO_ERROR(0),
        UNEXPECTED(1),
        VPN_PERMISSION_NOT_GRANTED(2),
        INVALID_SERVER_CREDENTIALS(3),
        UDP_RELAY_NOT_ENABLED(4),
        SERVER_UNREACHABLE(5),
        VPN_START_FAILURE(6),
        ILLEGAL_SERVER_CONFIGURATION(7),
        SHADOWSOCKS_START_FAILURE(8),
        CONFIGURE_SYSTEM_PROXY_FAILURE(9),
        NO_ADMIN_PERMISSIONS(10),
        UNSUPPORTED_ROUTING_TABLE(11),
        SYSTEM_MISCONFIGURED(12);

        public final int value;

        ErrorCode(int value) {
            this.value = value;
        }
    }

    public enum TunnelStatus {
        INVALID(-1), // Internal use only.
        CONNECTED(0),
        DISCONNECTED(1),
        RECONNECTING(2);

        public final int value;

        TunnelStatus(int value) {
            this.value = value;
        }
    }

    // IPC message and intent parameters.
    public enum MessageData {
        TUNNEL_ID("tunnelId"),
        TUNNEL_CONFIG("tunnelConfig"),
        ACTION("action"),
        PAYLOAD("payload"),
        ERROR_REPORTING_API_KEY("errorReportingApiKey");

        public final String value;

        MessageData(final String value) {
            this.value = value;
        }
    }
}
