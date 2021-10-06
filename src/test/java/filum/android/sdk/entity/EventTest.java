package filum.android.sdk.entity;

import org.json.JSONObject;
import org.junit.Test;

import filum.android.sdk.constant.CommonConstant;
import filum.android.sdk.exception.FILUMInvalidPropsPrefixException;

import static org.junit.Assert.*;

public class EventTest {
    @Test
    public void updateTimestamp() {
        Event event = new Event("name", "123", new JSONObject(), null);
        Long oldTime = event.getTime();
        assertNotNull(oldTime);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        event.updateTimestamp();
        Long newTime = event.getTime();
        assertTrue(newTime > oldTime);
    }

    @Test
    public void addCustomProp_Invalid() {
        Event event = new Event("name", "123", new JSONObject(), null);
        boolean invalid = false;
        try {
            event.addCustomProp(CommonConstant.DEFAULT_PROP_PREFIX + "key", "value");
        } catch (FILUMInvalidPropsPrefixException e) {
            invalid = true;
        }
        assertTrue(invalid);
    }
}