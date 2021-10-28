package filum.android.sdk.entity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import java.util.Iterator;


import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class EventTest {
    @Test
    public void updateTimestamp() {
        Event event;
        EventBuilder builder = new FilumEventBuilder();
        builder.setEventName("name");
        builder.setEventType("123");
        event = builder.getEvent();
        String oldTime = event.getTimestamp();
        assertNotNull(oldTime);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        event.setTimestamp(FilumEventBuilder.getCurrentTimeISO());
        String newTime = event.getTimestamp();
        assertTrue(newTime != oldTime);
    }

    @Test
    public void handleFloatValue() {
        try {
            FilumEventBuilder builder = new FilumEventBuilder();
            builder.setEventName("name");
            builder.setEventType("123");
            JSONObject props = new JSONObject();
            props.put("float", 14.514145f);
            double doubleValue = 14514.51313;
            props.put("double", doubleValue);
            builder.setEventParams(props);
            Event event = builder.getEvent();
            JSONObject serialized = event.serialize();

            JSONArray kvItems = serialized.getJSONArray("event_params");
            for (int i = 0 ; i < kvItems.length(); i++) {
                JSONObject item = (JSONObject) kvItems.get(i);
                JSONObject value = item.getJSONObject("value");
                Iterator<String> keys = value.keys();
                while(keys.hasNext()){
                    String key = keys.next();
                    assertTrue("double_value" == key);
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}