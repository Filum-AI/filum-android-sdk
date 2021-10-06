package filum.android.sdk.support;

import android.app.Activity;

import org.junit.Before;
import org.junit.Test;

import filum.android.sdk.FILUM;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FILUMActivityLifecycleCallbacksTest {
    private FILUMActivityLifecycleCallbacks callbacks;
    private FILUM filumInstance;
    private Activity context = mock(Activity.class);

    @Before
    public void setUp() throws Exception {
        filumInstance = mock(FILUM.class);
        callbacks = new FILUMActivityLifecycleCallbacks(filumInstance);
    }

    @Test
    public void onActivityPaused() {
        callbacks.onActivityPaused(context);
        assertTrue(callbacks.getBackground());
        verify(filumInstance, times(1)).onPause();
    }

    @Test
    public void onActivityResumed() {
        // If first resume (not from background) then do nothing
        assertFalse(callbacks.getBackground());
        callbacks.onActivityResumed(context);
        verify(filumInstance, times(0)).onResume();

        // If back from background, do callback
        callbacks.onActivityPaused(context);
        assertTrue(callbacks.getBackground());
        callbacks.onActivityResumed(context);
        assertFalse(callbacks.getBackground());
        verify(filumInstance, times(1)).onResume();
    }
}