package filum.android.sdk.service;

import org.junit.Before;
import org.junit.Test;

import filum.android.sdk.FILUM;
import filum.android.sdk.exception.FILUMTestRuntimeException;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ExceptionHandlerTest {
    private ExceptionHandler exceptionHandler;
    private FILUM filumInstance;

    @Before
    public void setUp() throws Exception {
        filumInstance = mock(FILUM.class);
        exceptionHandler = spy(ExceptionHandler.makeInstance(filumInstance));
    }

    @Test
    public void makeInstance() {
        ExceptionHandler newInstance = ExceptionHandler.makeInstance(filumInstance);
        assertNotSame(newInstance, exceptionHandler);
    }

    @Test
    public void uncaughtException() {
        doThrow(FILUMTestRuntimeException.class).when(exceptionHandler).exit();
        Thread thread = mock(Thread.class);
        Throwable exception = mock(Exception.class);
        boolean calledExit = false;
        try {
            exceptionHandler.uncaughtException(thread, exception);
        } catch (FILUMTestRuntimeException e) {
            calledExit = true;
        }
        assertTrue(calledExit);
        verify(filumInstance, times(1)).onUncaughtException();
    }
}