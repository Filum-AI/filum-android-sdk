package filum.android.sdk;

import android.app.Activity;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import filum.android.sdk.entity.Event;
import filum.android.sdk.exception.FILUMInstanceExistsException;
import filum.android.sdk.mock.JSONObjectMock;
import filum.android.sdk.service.ConfigManager;
import filum.android.sdk.service.DeviceInfoManager;
import filum.android.sdk.service.ExceptionHandler;
import filum.android.sdk.service.IdentityManager;
import filum.android.sdk.service.NetworkManager;
import filum.android.sdk.service.Storage;
import filum.android.sdk.service.TaskManager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ExceptionHandler.class,
        ConfigManager.class,
        Storage.class,
        DeviceInfoManager.class,
        NetworkManager.class,
        IdentityManager.class,
        TaskManager.class,
})
public class FILUMTest {
    private final String serverUrl = "http://test.url";
    private final String token = "123456";
    private Activity context;
    private FILUM filum;

    @Mock
    private ExceptionHandler exceptionHandler;
    @Mock
    private ConfigManager configManager;
    @Mock
    private Storage storage;
    @Mock
    private DeviceInfoManager deviceInfoManager;
    @Mock
    private NetworkManager networkManager;
    @Mock
    private IdentityManager identityManager;
    @Mock
    private TaskManager taskManager;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @BeforeClass
    public static void beforeAll() {
    }

    @Before
    public void beforeEach() throws Exception {
        context = mock(Activity.class);

        PowerMockito.mockStatic(ExceptionHandler.class);
        when(ExceptionHandler.makeInstance(any(FILUM.class)))
                .thenReturn(exceptionHandler);

        PowerMockito.mockStatic(ConfigManager.class);
        when(ConfigManager.makeInstance(any(Activity.class), anyString(), anyString()))
                .thenReturn(configManager);

        PowerMockito.mockStatic(Storage.class);
        when(Storage.makeInstance(any(ConfigManager.class)))
                .thenReturn(storage);

        PowerMockito.mockStatic(DeviceInfoManager.class);
        when(DeviceInfoManager.makeInstance(any(ConfigManager.class), any(Storage.class)))
                .thenReturn(deviceInfoManager);

        PowerMockito.mockStatic(NetworkManager.class);
        when(NetworkManager.makeInstance(any(ConfigManager.class)))
                .thenReturn(networkManager);

        PowerMockito.mockStatic(IdentityManager.class);
        when(IdentityManager.makeInstance(any(Storage.class)))
                .thenReturn(identityManager);

        PowerMockito.mockStatic(TaskManager.class);
        when(TaskManager.makeInstance(any(Storage.class), any(IdentityManager.class), any(NetworkManager.class)))
                .thenReturn(taskManager);

        filum = FILUM.initialize(serverUrl, serverUrl, context);
    }

    @After
    public void afterEach() throws Exception {
        TestHelper.resetSingleton(FILUM.class);
    }

    @Test
    public void initialize() {
        // Initialized -> initialize() again -> should throw error
        boolean thrownException = false;
        try {
            FILUM.initialize(serverUrl, token, context);
        } catch (FILUMInstanceExistsException e) {
            thrownException = true;
        }
        assertTrue(thrownException);

        // Should initialized all services
        PowerMockito.verifyStatic(ExceptionHandler.class, times(1));
        ExceptionHandler.makeInstance(any(FILUM.class));

        PowerMockito.verifyStatic(ConfigManager.class, times(1));
        ConfigManager.makeInstance(any(Activity.class), anyString(), anyString());

        PowerMockito.verifyStatic(Storage.class, times(1));
        Storage.makeInstance(any(ConfigManager.class));

        PowerMockito.verifyStatic(DeviceInfoManager.class, times(1));
        DeviceInfoManager.makeInstance(any(ConfigManager.class), any(Storage.class));

        PowerMockito.verifyStatic(NetworkManager.class, times(1));
        NetworkManager.makeInstance(any(ConfigManager.class));

        PowerMockito.verifyStatic(IdentityManager.class, times(1));
        IdentityManager.makeInstance(any(Storage.class));

        PowerMockito.verifyStatic(TaskManager.class, times(1));
        TaskManager.makeInstance(any(Storage.class), any(IdentityManager.class), any(NetworkManager.class));
    }

    @Test
    public void getInstance() {
        FILUM instance = FILUM.getInstance();
        assertNotNull(instance);
    }

    @Test
    public void track() {
        filum.track("event_name");
        verify(taskManager, times(1)).createEventTask(any(Event.class));

        JSONObject mockJSONObject = new JSONObjectMock().getMock();
        filum.track("event_name", mockJSONObject);
        verify(taskManager, times(2)).createEventTask(any(Event.class));
    }

    @Test
    public void identify() {
        String userId = "test_user_id";
        filum.identify(userId, new JSONObject());
        verify(taskManager, times(1)).createIdentifyTask(any(Event.class));
    }

    @Test
    public void reset() {
        filum.reset();
        verify(identityManager, times(1)).resetIDs();
    }

    @Test
    public void onUncaughtException() {
        filum.onUncaughtException();
        verify(taskManager, times(1)).stop();
    }

    @Test
    public void onPause() {
        filum.onPause();
        verify(taskManager, times(1)).stop();
    }

    @Test
    public void onResume() {
        filum.onResume();
        verify(taskManager, times(1)).restart();
    }
}