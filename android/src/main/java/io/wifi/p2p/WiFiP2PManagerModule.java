package io.wifi.p2p;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static android.os.Looper.getMainLooper;

/**
 * Created by zyusk on 01.05.2018.
 */
public class WiFiP2PManagerModule extends ReactContextBaseJavaModule implements WifiP2pManager.ConnectionInfoListener {
    private WifiP2pInfo wifiP2pInfo;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private final ReactApplicationContext reactContext;
    private static final String TAG = "RNWiFiP2P";
    private final WiFiP2PDeviceMapper mapper = new WiFiP2PDeviceMapper();
    public static final String SERVICE_INSTANCE = "_rnwifip2preborn";
    public static final String SERVICE_TYPE = "_presence._tcp";

    public WiFiP2PManagerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "WiFiP2PManagerModule";
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        this.wifiP2pInfo = info;
    }

    private void sendEvent(String eventName, @Nullable WritableMap params) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    private WritableArray toWritableArray(Object[] array) {
        WritableArray writableArray = Arguments.createArray();

        try{
            for (Object value : array) {
                if (value == null) {
                    writableArray.pushNull();
                }
                if (value instanceof Boolean) {
                    writableArray.pushBoolean((Boolean) value);
                }
                if (value instanceof Double) {
                    writableArray.pushDouble((Double) value);
                }
                if (value instanceof Integer) {
                    writableArray.pushInt((Integer) value);
                }
                if (value instanceof String) {
                    writableArray.pushString((String) value);
                }
                if (value instanceof Map) {
                    writableArray.pushMap(toWritableMap((Map<String, Object>) value));
                }
                if (value != null && value.getClass().isArray()) {
                    if (value instanceof Object[]) {
                        writableArray.pushArray(toWritableArray((Object[]) value));
                    }
                }
            }
        }catch(Error e){
            Log.d(TAG,"Error in toWritableArray", e);
        }



        return writableArray;
    }

    private WritableMap toWritableMap(Map<String, Object> map) {
        WritableMap writableMap = Arguments.createMap();
        Iterator iterator = map.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry)iterator.next();
            Object value = pair.getValue();

            if (value == null) {
                writableMap.putNull((String) pair.getKey());
            } else if (value instanceof Boolean) {
                writableMap.putBoolean((String) pair.getKey(), (Boolean) value);
            } else if (value instanceof Double) {
                writableMap.putDouble((String) pair.getKey(), (Double) value);
            } else if (value instanceof Integer) {
                writableMap.putInt((String) pair.getKey(), (Integer) value);
            } else if (value instanceof String) {
                writableMap.putString((String) pair.getKey(), (String) value);
            } else if (value instanceof Map) {
                writableMap.putMap((String) pair.getKey(), toWritableMap((Map<String, Object>) value));
            } else if (value.getClass() != null && value.getClass().isArray()) {
                writableMap.putArray((String) pair.getKey(), toWritableArray((Object[]) value));
            }

            iterator.remove();
        }

        return writableMap;
    }

    private HashMap<String, Object> toHashMap(ReadableMap map) {
        HashMap<String, Object> hashMap = new HashMap<>();
        ReadableMapKeySetIterator iterator = map.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            switch (map.getType(key)) {
                case Null:
                    hashMap.put(key, null);
                    break;
                case Boolean:
                    hashMap.put(key, map.getBoolean(key));
                    break;
                case Number:
                    hashMap.put(key, map.getDouble(key));
                    break;
                case String:
                    hashMap.put(key, map.getString(key));
                    break;
                case Map:
                    hashMap.put(key,toHashMap(map.getMap(key)));
                    break;
                default:
                    throw new IllegalArgumentException("Could not convert object with key: " + key + ".");
            }
        }
        return hashMap;
    }

    @ReactMethod
    public void startServiceRegistration(ReadableMap record, final Promise promise){
        Map newRecord = toHashMap(record);
        final WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance(SERVICE_INSTANCE, SERVICE_TYPE, newRecord);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        manager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                manager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        // Command successful! Code isn't necessarily needed here,
                        // Unless you want to update the UI or add logging statements.
                        promise.resolve(true);
                    }

                    @Override
                    public void onFailure(int arg0) {
                        // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                        promise.reject("Error occured", String.valueOf(arg0));
                    }
                });
            }

            @Override
            public void onFailure(int error) {
                // react to failure of clearing the local services
            }
        });

    }

    @ReactMethod
    public void discoverService(){

        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            /* Callback includes:
             * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
             * record: TXT record dta as a map of key/value pairs.
             * device: The device running the advertised service.
             */
            @Override
            public void onDnsSdTxtRecordAvailable(
                    String fullDomain, Map record, WifiP2pDevice device) {
                Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());
                WritableMap map = new WritableNativeMap();
                map.putString("fullDomain", fullDomain);
                WritableMap recordMap = toWritableMap(record);
                map.putMap("record", recordMap);
                WritableMap deviceMap = mapper.mapDeviceInfoToReactEntity(device);
                map.putMap("device",deviceMap);
                sendEvent("WIFI_P2P:DNSTXTRECORDAVAILABLE", map);
            }
        };

        WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice resourceType) {

                if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {
                    Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
                    WritableMap map = new WritableNativeMap();
                    map.putString("instanceName", instanceName);
                    map.putString("registrationType", registrationType);
                    WritableMap resourceTypeMap = mapper.mapDeviceInfoToReactEntity(resourceType);
                    map.putMap("device", resourceTypeMap);
                    sendEvent("WIFI_P2P:DNSSDSERVICEAVAILABLE", map);
                }
            }
        };

        manager.setDnsSdResponseListeners(channel, servListener, txtListener);

        final WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance(SERVICE_TYPE);

        manager.removeServiceRequest(channel, serviceRequest,
                    new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            manager.addServiceRequest(channel,
                                    serviceRequest,
                                    new WifiP2pManager.ActionListener() {
                                        @Override
                                        public void onSuccess() {
                                            // Success!
                                            manager.discoverServices(channel, new WifiP2pManager.ActionListener() {

                                                @Override
                                                public void onSuccess() {
                                                    // Success!
                                                }

                                                @Override
                                                public void onFailure(int code) {
                                                    // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                                                    if (code == WifiP2pManager.P2P_UNSUPPORTED) {
                                                        Log.d(TAG, "P2P isn't supported on this device.");
                                                    }
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(int code) {
                                            // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                                        }
                                    });


                        }

                        @Override
                        public void onFailure(int reason) {
                            // react to failure of removing service request
                        }
                    });



    }

    @ReactMethod
    public void getConnectionInfo(final Promise promise) {
        manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInformation) {
                Log.i(TAG, wifiP2pInformation.toString());

                wifiP2pInfo = wifiP2pInformation;

                promise.resolve(mapper.mapWiFiP2PInfoToReactEntity(wifiP2pInformation));
            }
        });
    }

    @ReactMethod
    public void getGroupInfo(final Promise promise) {
        manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if (group != null) {
                    promise.resolve(mapper.mapWiFiP2PGroupInfoToReactEntity(group));
                }
                else {
                    promise.resolve(null);
                }
            }
        });
    }

    @ReactMethod
    public void getPeerList(final Promise promise) {
        manager.requestPeers(channel, new WifiP2pManager.PeerListListener(){
            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                if(wifiP2pDeviceList != null){
                    promise.resolve(mapper.mapDeviceListToReactEntityArray(wifiP2pDeviceList));
                }else{
                    promise.resolve(null);
                }
            }
        });
    }

    @ReactMethod
    public void init(Promise promise) {
        if (manager != null) { // prevent reinitialization
            return;
        }

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        Activity activity = getCurrentActivity();
        if (activity != null) {
            try {
                manager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
                channel = manager.initialize(activity, getMainLooper(), null);

                WiFiP2PBroadcastReceiver receiver = new WiFiP2PBroadcastReceiver(manager, channel, reactContext);
                activity.registerReceiver(receiver, intentFilter);

                promise.resolve(manager != null && channel != null);
            } catch (NullPointerException e) {
                promise.reject("0x1", "can not get WIFI_P2P_SERVICE");
            }
        }

        promise.reject("0x0", this.getName() + " module can not be initialized, since main activity is `null`");
    }

    @ReactMethod
    public void createGroup(final Callback callback) {
        manager.createGroup(channel,  new WifiP2pManager.ActionListener()  {
            public void onSuccess() {
                callback.invoke(); // Group creation successful
            }

            public void onFailure(int reason) {
                callback.invoke(Integer.valueOf(reason)); // Group creation failed
            }
        });
    }

    @ReactMethod
    public void removeGroup(final Callback callback) {
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.invoke();
            }

            @Override
            public void onFailure(int reason) {
                callback.invoke(Integer.valueOf(reason));
            }
        });
    }

    @ReactMethod
    public void getAvailablePeersList(final Promise promise) {
        manager.requestPeers(channel, new PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList deviceList) {
                WritableMap params = mapper.mapDevicesInfoToReactEntity(deviceList);
                promise.resolve(params);
            }
        });
    }

    @ReactMethod
    public void discoverPeers(final Callback callback) {
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.invoke();
            }

            @Override
            public void onFailure(int reasonCode) {
                callback.invoke(reasonCode);
            }
        });
    }

    @ReactMethod
    public void stopPeerDiscovery(final Callback callback) {
        manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.invoke();
            }

            @Override
            public void onFailure(int reasonCode) {
                callback.invoke(Integer.valueOf(reasonCode));
            }
        });
    }

    @ReactMethod
    public void cancelConnect(final Callback callback) {
        manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.invoke();
            }

            @Override
            public void onFailure(int reasonCode) {
                callback.invoke(Integer.valueOf(reasonCode));
            }
        });
    }

    @ReactMethod
    public void connectWithConfig(ReadableMap readableMap, final Callback callback) {
        Bundle bundle = Arguments.toBundle(readableMap);
        WifiP2pConfig config = new WifiP2pConfig();

        String deviceAddress = bundle.getString("deviceAddress");
        config.deviceAddress = deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        if (bundle.containsKey("groupOwnerIntent")){
            config.groupOwnerIntent = (int) bundle.getDouble("groupOwnerIntent");
        };

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                callback.invoke(); // WiFiP2PBroadcastReceiver notifies us. Ignore for now.
            }

            @Override
            public void onFailure(int reasonCode) {
                callback.invoke(Integer.valueOf(reasonCode));
            }
        });
    }

    @ReactMethod
    public void sendFile(String filePath, final Promise promise) {
        // User has picked a file. Transfer it to group owner i.e peer using FileTransferService
        Uri uri = Uri.fromFile(new File(filePath));
        String hostAddress = wifiP2pInfo.groupOwnerAddress.getHostAddress();
        Log.i(TAG, "Sending: " + uri);
        Log.i(TAG, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(getCurrentActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, hostAddress);
        serviceIntent.putExtra(FileTransferService.REQUEST_RECEIVER_EXTRA, new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == 0) { // successful transfer
                    promise.resolve(mapper.mapSendFileBundleToReactEntity(resultData));
                } else { // error
                    promise.reject(String.valueOf(resultCode), resultData.getString("error"));
                }
            }
        });
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        getCurrentActivity().startService(serviceIntent);
    }

    @ReactMethod
    public void receiveFile(String folder, String fileName, final Boolean forceToScanGallery, final Callback callback) {
        final String destination = folder + fileName;
        manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                if (info.groupFormed && info.isGroupOwner) {
                    new FileServerAsyncTask(getCurrentActivity(), callback, destination, new CustomDefinedCallback() {
                        @Override
                        public void invoke(Object object) {
                            if (forceToScanGallery) { // fixes: https://github.com/kirillzyusko/react-native-wifi-p2p/issues/31
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                    final File file = new File(destination);
                                    final Uri contentUri = Uri.fromFile(file);
                                    scanIntent.setData(contentUri);
                                    reactContext.sendBroadcast(scanIntent);
                                } else {
                                    final Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()));
                                    reactContext.sendBroadcast(intent);
                                }
                            }
                        }
                    }).execute();
                } else if (info.groupFormed) {
                    // The other device acts as the client
                }
            }
        });
    }

    @ReactMethod
    public void sendMessage(String message, final Promise promise) {
        Log.i(TAG, "Sending message: " + message);
        Intent serviceIntent = new Intent(getCurrentActivity(), MessageTransferService.class);
        serviceIntent.setAction(MessageTransferService.ACTION_SEND_MESSAGE);
        serviceIntent.putExtra(MessageTransferService.EXTRAS_DATA, message);
        serviceIntent.putExtra(MessageTransferService.EXTRAS_GROUP_OWNER_ADDRESS, wifiP2pInfo.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(MessageTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        serviceIntent.putExtra(MessageTransferService.REQUEST_RECEIVER_EXTRA, new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == 0) { // successful transfer
                    promise.resolve(mapper.mapSendMessageBundleToReactEntity(resultData));
                } else { // error
                    promise.reject(String.valueOf(resultCode), resultData.getString("error"));
                }
            }
        });
        getCurrentActivity().startService(serviceIntent);
    }

    @ReactMethod
    public void receiveMessage(final Callback callback) {
        manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                if (info.groupFormed && info.isGroupOwner) {
                    new MessageServerAsyncTask(callback)
                            .execute();
                } else if (info.groupFormed) {
                    // The other device acts as the client
                }
            }
        });
    }
}