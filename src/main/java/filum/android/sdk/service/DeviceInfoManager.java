package filum.android.sdk.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.content.pm.ApplicationInfo;
import androidx.annotation.VisibleForTesting;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONObject;
import org.json.JSONException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import filum.android.sdk.BuildConfig;
import filum.android.sdk.constant.DeviceInfoProps;
import filum.android.sdk.constant.StorageKey;
import filum.android.sdk.util.Logger;

public class DeviceInfoManager {
    private Map<String, Object> staticProps = new HashMap<>();
    private ConfigManager configManager;
    private Storage storage;

    public DeviceInfoManager(ConfigManager configManager, Storage storage) {
        this.configManager = configManager;
        this.storage = storage;

        Context context = configManager.getContext();
        PackageManager packageManager = context.getPackageManager();

        // Get version's info
        Long foundAppVersionCode = null;
        String foundAppVersionName = null;
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            foundAppVersionCode = (long) packageInfo.versionCode;
            foundAppVersionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.warn("System information constructed with a context that apparently doesn't exist.");
        }

        // Get NFC / Telephony availability
        Boolean foundNFC = null;
        Boolean foundTelephony = null;
        // We can't count on these features being available, since we need to
        // run on old devices. Thus, the reflection fandango below...
        Class<? extends PackageManager> packageManagerClass = packageManager.getClass();
        Method hasSystemFeatureMethod = null;
        try {
            hasSystemFeatureMethod = packageManagerClass.getMethod("hasSystemFeature", String.class);
        } catch (NoSuchMethodException e) {
            // Nothing, this is an expected outcome
        }
        if (null != hasSystemFeatureMethod) {
            try {
                foundNFC = (Boolean) hasSystemFeatureMethod.invoke(packageManager, "android.hardware.nfc");
                foundTelephony = (Boolean) hasSystemFeatureMethod.invoke(packageManager, "android.hardware.telephony");
            } catch (InvocationTargetException e) {
                Logger.warn("System version appeared to support PackageManager.hasSystemFeature, but we were unable to call it.");
            } catch (IllegalAccessException e) {
                Logger.warn("System version appeared to support PackageManager.hasSystemFeature, but we were unable to call it.");
            }
        }

        staticProps.put(DeviceInfoProps.DEVICE_ID, getDeviceId());
        staticProps.put(DeviceInfoProps.OS, "Android");
        staticProps.put(DeviceInfoProps.LIB, "FILUM-android");
        staticProps.put(DeviceInfoProps.LIB_VERSION, BuildConfig.VERSION_NAME);
        DisplayMetrics displayMetrics = getScreenMetrics();
        staticProps.put(DeviceInfoProps.SCREEN_HEIGHT, displayMetrics.heightPixels);
        staticProps.put(DeviceInfoProps.SCREEN_WIDTH, displayMetrics.widthPixels);
        staticProps.put(DeviceInfoProps.SCREEN_DPI, displayMetrics.densityDpi);
        staticProps.put(DeviceInfoProps.OS_VERSION, Build.VERSION.RELEASE);
        staticProps.put(DeviceInfoProps.APP_BUILD_NUMBER, foundAppVersionCode);
        staticProps.put(DeviceInfoProps.APP_VERSION_STRING, foundAppVersionName);
        staticProps.put(DeviceInfoProps.CARRIER, getCarrier());
        staticProps.put(DeviceInfoProps.MANUFACTURER, Build.MANUFACTURER);
        staticProps.put(DeviceInfoProps.MODEL, Build.MODEL);
        staticProps.put(DeviceInfoProps.BRAND, Build.BRAND);
        staticProps.put(DeviceInfoProps.BLUETOOTH_VERSION, getBluetoothVersion());
        staticProps.put(DeviceInfoProps.HAS_NFC, foundNFC);
        staticProps.put(DeviceInfoProps.HAS_TELEPHONE, foundTelephony);
        staticProps.put(DeviceInfoProps.GOOGLE_PLAY_SERVICES, getGooglePlayServices());
    }

    public static DeviceInfoManager makeInstance(ConfigManager configManager, Storage storage) {
        return new DeviceInfoManager(configManager, storage);
    }

    public JSONObject getDeviceInfo() {
        Map<String, Object> props = getDeviceInfoAsMap();

        return new JSONObject(props);
    }

    public JSONObject getFilumContext() {
        JSONObject deviceInfo = getDeviceInfo();

        try {
            JSONObject context = new JSONObject();

            context.put("active", 1);

            JSONObject appDict = new JSONObject();
            Context _context = configManager.getContext();
            appDict.put("name", getApplicationName(_context));
            appDict.put("version", deviceInfo.getString(DeviceInfoProps.APP_VERSION_STRING));
            appDict.put("build", String.valueOf(deviceInfo.getLong(DeviceInfoProps.APP_BUILD_NUMBER)));
            context.put("app", appDict);

            context.put("campaign", replaceWithJSONNull(null));

            JSONObject deviceDict = new JSONObject();
            deviceDict.put("id", getDeviceId());
            deviceDict.put("advertising_id", replaceWithNull(null));
            deviceDict.put("manufacturer", Build.MANUFACTURER);
            deviceDict.put("model", Build.MODEL);
            deviceDict.put("name", Build.BRAND);
            deviceDict.put("type", Build.MODEL);
            deviceDict.put("version", Build.VERSION.RELEASE);
            context.put("device", deviceDict);

            context.put("ip", replaceWithNull(null));

            JSONObject defaultLibrary = new JSONObject();
            defaultLibrary.put("name", "filum-android-sdk");
            defaultLibrary.put("version", BuildConfig.VERSION_NAME);
            context.put("library", defaultLibrary);

            context.put("locale", replaceWithNull(null));
            context.put("location", replaceWithJSONNull(null));

            JSONObject networkDict = new JSONObject();
            if (isBluetoothEnabled() == null) {
                networkDict.put("bluetooth", replaceWithNull(isBluetoothEnabled()));
            }
            else {
                networkDict.put("bluetooth", isBluetoothEnabled() ? 1 : 0);
            }
            networkDict.put("carrier", getCarrier());
            networkDict.put("cellular", 0);
            networkDict.put("wifi", isWifiConnected() ? 1: 0);
            context.put("network", networkDict);

            JSONObject osDict = new JSONObject();
            osDict.put("name", "Android");
            osDict.put("version", Build.VERSION.RELEASE);
            context.put("os", osDict);

            context.put("page", replaceWithJSONNull(null));
            context.put("referrer", replaceWithJSONNull(null));

            JSONObject screenDict = new JSONObject();
            DisplayMetrics displayMetrics = getScreenMetrics();
            screenDict.put("density", displayMetrics.densityDpi);
            screenDict.put("width", displayMetrics.widthPixels);
            screenDict.put("height", displayMetrics.heightPixels);
            context.put("screen", screenDict);

            context.put("user_agent", replaceWithJSONNull(null));
            return context;
        } catch (JSONException e) {
            Logger.error("Error creating Context instance");
            return new JSONObject();
        }
    }

    public static String getApplicationName(Context context) {
        try {
            ApplicationInfo applicationInfo = context.getApplicationInfo();
            int stringId = applicationInfo.labelRes;
            return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
        } catch (Exception e) {
            Logger.warn("Cannot get application name.");
            return "";
        }
    }

    /** internal methods */
    protected Object replaceWithJSONNull(Object obj) {
        return obj == null ? JSONObject.NULL : obj;
    }

    protected Object replaceWithNull(Object obj) {
        return obj == null ? null : obj;
    }

    /**
     * Support testing
     */
    Map<String, Object> getDeviceInfoAsMap() {
        // Copy all static props
        Map<String, Object> props = new HashMap<>(staticProps);
        props.put(DeviceInfoProps.WIFI, isWifiConnected());
        props.put(DeviceInfoProps.BLUETOOTH_ENABLED, isBluetoothEnabled());
        return props;
    }

    /**
     * For testing
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    Map<String, Object> getStaticProps() {
        return staticProps;
    }

    private String getDeviceId() {
        String deviceId = storage.getString(StorageKey.DEVICE_ID);
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            storage.put(StorageKey.DEVICE_ID, deviceId);
        }
        return deviceId;
    }

    private DisplayMetrics getScreenMetrics() {
        return configManager
                .getContext()
                .getResources()
                .getDisplayMetrics();
    }

    private String getCarrier() {
        String carrier = null;

        TelephonyManager telephonyManager = (TelephonyManager) configManager
                .getContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (null != telephonyManager) {
            carrier = telephonyManager.getNetworkOperatorName();
        }

        return carrier;
    }

    private String getBluetoothVersion() {
        String bluetoothVersion = null;
        PackageManager packageManager = configManager
                .getContext()
                .getPackageManager();
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bluetoothVersion = "ble";
        } else if (packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            bluetoothVersion = "classic";
        }
        return bluetoothVersion;
    }

    private String getGooglePlayServices() {
        String googlePlayServices = null;
        try {
            try {
                final int servicesAvailable = GoogleApiAvailability
                        .getInstance()
                        .isGooglePlayServicesAvailable(configManager.getContext());
                switch (servicesAvailable) {
                    case ConnectionResult.SUCCESS:
                        googlePlayServices = "available";
                        break;
                    case ConnectionResult.SERVICE_MISSING:
                        googlePlayServices = "missing";
                        break;
                    case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                        googlePlayServices = "out of date";
                        break;
                    case ConnectionResult.SERVICE_DISABLED:
                        googlePlayServices = "disabled";
                        break;
                    case ConnectionResult.SERVICE_INVALID:
                        googlePlayServices = "invalid";
                        break;
                }
            } catch (RuntimeException e) {
                googlePlayServices = "not configured";
            }
        } catch (NoClassDefFoundError e) {
            googlePlayServices = "not included";
        }
        return googlePlayServices;
    }

    @SuppressLint("MissingPermission")
    @SuppressWarnings("MissingPermission")
    private Boolean isWifiConnected() {
        Context context = configManager.getContext();

        if (PackageManager.PERMISSION_GRANTED != context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            return null;
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return null;
        }

        if  (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (capabilities != null) {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            }

            return null;
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null
                    && networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                    && networkInfo.isConnected();
        }
    }

    @SuppressLint("MissingPermission")
    @SuppressWarnings("MissingPermission")
    private Boolean isBluetoothEnabled() {
        Context context = configManager.getContext();
        Boolean isBluetoothEnabled = null;
        try {
            PackageManager pm = context.getPackageManager();
            int hasBluetoothPermission = pm.checkPermission(Manifest.permission.BLUETOOTH, context.getPackageName());
            if (hasBluetoothPermission == PackageManager.PERMISSION_GRANTED) {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter != null) {
                    isBluetoothEnabled = bluetoothAdapter.isEnabled();
                }
            }
        } catch (SecurityException e) {
            // do nothing since we don't have permissions
        } catch (NoClassDefFoundError e) {
            // Some phones doesn't have this class. Just ignore it
        }
        return isBluetoothEnabled;
    }
}
