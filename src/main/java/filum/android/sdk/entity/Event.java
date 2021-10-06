package filum.android.sdk.entity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Iterator;
import filum.android.sdk.common.Serializable;
import filum.android.sdk.constant.CommonProps;

public class Event implements Serializable {
    private String anonymousID;
    private String userID;
    private String eventID;
    private JSONObject context = new JSONObject();
    private String timestamp;
    private String originalTimestamp;
    private String sentAt;
    private String receivedAt;
    private String eventName;
    private String eventType;
    private JSONObject eventParams = new JSONObject();
    private String origin;

    public void setAnonymousID(String anonymousID) {
        this.anonymousID = anonymousID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserID() {
        return userID;
    }

    public void setEventID(String eventID) {
        this.eventID = eventID;
    }

    public void setContext(JSONObject context) {
        this.context = context;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setOriginalTimestamp(String originalTimestamp) {
        this.originalTimestamp = originalTimestamp;
    }

    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }

    public void setReceivedAt(String receivedAt) {
        this.receivedAt = receivedAt;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setEventParams(JSONObject eventParams) {
        this.eventParams = eventParams;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(CommonProps.ANONYMOUS_ID, this.anonymousID);
        json.put(CommonProps.USER_ID, this.userID);
        json.put(CommonProps.EVENT_ID, this.eventID);
        json.put(CommonProps.TIMESTAMP, this.timestamp);
        json.put(CommonProps.ORIGINAL_TIMESTAMP, this.originalTimestamp);
        json.put(CommonProps.SENT_AT, this.sentAt);
        json.put(CommonProps.RECEIVED_AT, this.receivedAt);
        json.put(CommonProps.EVENT_NAME, this.eventName);
        json.put(CommonProps.EVENT_TYPE, this.eventType);
        json.put(CommonProps.ORIGIN, this.origin);

        json.put(CommonProps.CONTEXT, context);

        JSONArray eventParamsJSONArray = new JSONArray();
        // Append customProps
        if (eventParams != null) {
            Iterator<String> customEventParamKeys = eventParams.keys();
            while (customEventParamKeys.hasNext()) {
                String key = customEventParamKeys.next();
                Object value = null;
                if (!eventParams.isNull(key)) {
                    value = eventParams.get(key);
                    JSONObject filumValue = convertToFilumItem(key, value);
                    eventParamsJSONArray.put(filumValue);
                }
            }
        }
        json.put(CommonProps.EVENT_PARAMS, eventParamsJSONArray);
        return json;
    }

    public static JSONObject convertToFilumItem(String key, Object value) throws JSONException {
        JSONObject json = new JSONObject();
        if(value instanceof String){
            json.put("key", key);
            JSONObject value_json = new JSONObject();
            value_json.put("string_value", value);
            json.put("value", value_json);
        }
        else if(value instanceof Integer){
            json.put("key", key);
            JSONObject value_json = new JSONObject();
            value_json.put("int_value", value);
            json.put("value", value_json);
        }
        else if(value instanceof Float){
            json.put("key", key);
            JSONObject value_json = new JSONObject();
            value_json.put("float_value", value);
            json.put("value", value_json);
        }
        else if(value instanceof Double){
            json.put("key", key);
            JSONObject value_json = new JSONObject();
            value_json.put("double_value", value);
            json.put("value", value_json);
        }
        else if(value instanceof Date){
            json.put("key", key);
            JSONObject value_json = new JSONObject();
            value_json.put("datetime_value", value);
            json.put("value", value_json);
        }
        return json;
    }
}