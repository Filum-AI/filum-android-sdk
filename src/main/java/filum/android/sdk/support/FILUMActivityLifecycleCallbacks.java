package filum.android.sdk.support;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;

import filum.android.sdk.FILUM;
import filum.android.sdk.util.Logger;

public class FILUMActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private boolean isBackground = false;
    private FILUM filumInstance;

    public FILUMActivityLifecycleCallbacks(FILUM filumInstance) {
        this.filumInstance = filumInstance;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Logger.log("LIFECYCLE: created!");
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
        isBackground = true;
        Logger.log("LIFECYCLE: foreground -> background");
        filumInstance.onPause();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (isBackground) {
            Logger.log("LIFECYCLE: background -> foreground");
            filumInstance.onResume();
        }
        isBackground = false;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        Logger.log("LIFECYCLE: stopped");
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Logger.log("LIFECYCLE: destroyed");
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    boolean getBackground() {
        return isBackground;
    }
}
