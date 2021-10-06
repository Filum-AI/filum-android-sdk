package filum.android.sdk;

import android.app.Application;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import filum.android.sdk.entity.Event;
import filum.android.sdk.entity.FilumEventBuilder;
import filum.android.sdk.exception.FILUMInstanceExistsException;
import filum.android.sdk.service.ConfigManager;
import filum.android.sdk.service.DeviceInfoManager;
import filum.android.sdk.service.ExceptionHandler;
import filum.android.sdk.service.IdentityManager;
import filum.android.sdk.service.NetworkManager;
import filum.android.sdk.service.Storage;
import filum.android.sdk.service.TaskManager;
import filum.android.sdk.support.FILUMActivityLifecycleCallbacks;
import filum.android.sdk.util.Logger;

/**
 * Main class of FILUM Android SDK.
 *
 * <p>Call {@link #initialize(String, String, Context)} first to initialize FILUM and its
 * sub-services.</p>
 *
 * <p>Then call {@link #getInstance()} to get singleton instance. CAUTION: getInstance can return
 * null if {@link #initialize(String, String, Context)} is not called first.</p>
 *
 * <p>Call {@link #track(String, JSONObject)} to emit your event to the system:</p>
 * <pre>
 * {@code
 * final FILUM filum = FILUM.initialize(
 *     "http://server-url.example",
 *     "your_project_token",
 *     MainActivity.this
 * );
 *
 * try {
 *     JSONObject props = new JSONObject();
 *     props.put("price", 100);
 *     props.put("package_sku", "package_1_free");
 *     filum.track("Purchase", props);
 * } catch (JSONException e) {}
 * }
 * </pre>
 */
public class FILUM {

    private static FILUM instance = null;
    private ExceptionHandler exceptionHandler;
    private ConfigManager configManager;
    private Storage storage;
    private DeviceInfoManager deviceInfoManager;
    private NetworkManager networkManager;
    private IdentityManager identityManager;
    private TaskManager taskManager;

    /**
     * @return Instance of FILUM. Should be called after FILUM.initialize()
     */
    public static FILUM getInstance() {
        return instance;
    }

    public FILUM() {
    }

    private FILUM(String serverUrl, String token, Context context) {
        exceptionHandler = ExceptionHandler.makeInstance(FILUM.this);
        configManager = ConfigManager.makeInstance(context, serverUrl, token);
        storage = Storage.makeInstance(configManager);
        deviceInfoManager = DeviceInfoManager.makeInstance(configManager, storage);
        networkManager = NetworkManager.makeInstance(configManager);
        identityManager = IdentityManager.makeInstance(storage);
        taskManager = TaskManager.makeInstance(storage, identityManager, networkManager);

        // Only called after initialized all services
        registerFILUMActivityLifecycleCallbacks(context);
    }

    /**
     * Method to initialize single instance of FILUM and all its sub-services.
     * Should always be call first before interacting with FILUM.
     *
     * @param serverUrl URL of FILUM Platform Core API
     * @param token     Token of your product
     * @param context   Application's context that you want to track
     * @return Instance of FILUM
     */
    public static FILUM initialize(String serverUrl, String token, Context context) {
        if (instance == null) {
            synchronized (FILUM.class) {
                if (instance == null) {
                    instance = new FILUM(serverUrl, token, context);
                    return instance;
                }
            }
        }
        throw new FILUMInstanceExistsException();
    }

    private void registerFILUMActivityLifecycleCallbacks(Context context) {
        if (context.getApplicationContext() instanceof Application) {
            final Application app = (Application) context.getApplicationContext();
            FILUMActivityLifecycleCallbacks callbacks = new FILUMActivityLifecycleCallbacks(FILUM.this);
            app.registerActivityLifecycleCallbacks(callbacks);
        } else {
            Logger.warn("Context is not an Application. We won't be able to automatically flush on background.");
        }
    }

    public void onUncaughtException() {
        if (taskManager != null) {
            taskManager.stop();
        }
    }

    public void onPause() {
        if (taskManager != null) {
            taskManager.stop();
        }
    }

    public void onResume() {
        if (taskManager != null) {
            taskManager.restart();
        }
    }

    /**
     * Method to emit your event, without any custom properties.
     *
     * @param eventName Name of the event you want to track
     */
    public void track(String eventName) {
        track(eventName, null);
    }

    /**
     * Method to emit your event, with custom properties.
     *
     * @param eventName   Name of the event you want to track
     * @param eventParams Custom props as key-value (valid JSON object)
     */
    public void track(String eventName, JSONObject eventParams) {
        Event event;
        FilumEventBuilder eventBuilder = new FilumEventBuilder();
        JSONObject filumContext = deviceInfoManager.getFilumContext();

        String userID = identityManager.getUserID();
        String anonymousID = identityManager.getAnonymousID();

        eventBuilder.setUserID(userID);
        eventBuilder.setAnonymousID(anonymousID);
        eventBuilder.setEventName(eventName);
        eventBuilder.setEventType("track");
        eventBuilder.setEventParams(eventParams);
        eventBuilder.setContext(filumContext);
        event = eventBuilder.getEvent();
        taskManager.createEventTask(event);
    }

    public void identify(String userId, JSONObject eventParams) {
        FilumEventBuilder eventBuilder = new FilumEventBuilder();
        JSONObject filumContext = deviceInfoManager.getFilumContext();

        String anonymousID = identityManager.getAnonymousID();

        eventBuilder.setAnonymousID(anonymousID);
        eventBuilder.setUserID(userId);

        eventBuilder.setEventName("Identify");
        eventBuilder.setEventType("identify");
        eventBuilder.setEventParams(eventParams);
        eventBuilder.setContext(filumContext);

        Event event = eventBuilder.getEvent();
        taskManager.createIdentifyTask(event);
    }

    /**
     * Method to generate new distinctId for new user/visitor after current user take log out action.
     * Should always be called after user take log out action.
     */
    public void reset() {
        identityManager.resetIDs();
    }
}
