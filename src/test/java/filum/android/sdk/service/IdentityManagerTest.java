package filum.android.sdk.service;

import org.junit.Before;
import org.junit.Test;

import filum.android.sdk.constant.StorageKey;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class IdentityManagerTest {
    private IdentityManager identityManager;
    private Storage storage;
    private final String savedAnonymousId = "123";


    @Before
    public void setUp() throws Exception {
        storage = mock(Storage.class);
        when(storage.getString(StorageKey.ANONYMOUS_ID)).thenReturn(savedAnonymousId);
        identityManager = spy(IdentityManager.makeInstance(storage));
    }

    @Test
    public void constructor_DistinctId_NotExisted() {
        when(storage.getString(StorageKey.ANONYMOUS_ID)).thenReturn(null);
        identityManager = spy(IdentityManager.makeInstance(storage));
        assertNotEquals(savedAnonymousId, identityManager.getAnonymousID());
    }

    @Test
    public void constructor_DistinctId_Existed() {
        assertEquals(savedAnonymousId, identityManager.getAnonymousID());
    }

    @Test
    public void makeInstance() {
        IdentityManager newInstance = IdentityManager.makeInstance(storage);
        assertNotSame(newInstance, identityManager);
    }

    @Test
    public void getDistinctId() {
        String distinctId = identityManager.getAnonymousID();
        assertEquals(distinctId, savedAnonymousId);
    }

    @Test
    public void generateNewDistinctId() {
        String oldDistinctId = identityManager.getAnonymousID();
        String distinctId = identityManager.resetIDs();
        verify(identityManager, times(1)).updateAnonymousID(distinctId);
        assertNotEquals(distinctId, oldDistinctId);
    }

    @Test
    public void updateUserId() {
        final String userID = "456";
        identityManager.updateUserID(userID);
        verify(storage, times(1)).put(StorageKey.USER_ID, userID);
        assertEquals(identityManager.getUserID(), userID);
    }
}