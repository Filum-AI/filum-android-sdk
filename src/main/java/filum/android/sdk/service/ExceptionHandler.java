package filum.android.sdk.service;

import androidx.annotation.VisibleForTesting;

import filum.android.sdk.FILUM;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final int SLEEP_TIMEOUT_MS = 400;
    private final Thread.UncaughtExceptionHandler defaultExceptionHandler;
    private FILUM filumInstance;

    public ExceptionHandler(FILUM filumInstance) {
        this.filumInstance = filumInstance;
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public static ExceptionHandler makeInstance(FILUM filumInstance) {
        return new ExceptionHandler(filumInstance);
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        filumInstance.onUncaughtException();

        if (defaultExceptionHandler != null) {
            defaultExceptionHandler.uncaughtException(t, e);
        } else {
            killProcessAndExit();
        }
    }

    private void killProcessAndExit() {
        try {
            Thread.sleep(SLEEP_TIMEOUT_MS);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        android.os.Process.killProcess(android.os.Process.myPid());
        exit();
    }

    @VisibleForTesting
    void exit() {
        System.exit(10);
    }
}
