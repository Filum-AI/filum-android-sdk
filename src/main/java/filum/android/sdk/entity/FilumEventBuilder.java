package filum.android.sdk.entity;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;


interface EventBuilder{
    void setAnonymousID(String anonymousID);
    void setUserID(String userID);
    void setContext(JSONObject context);
    void setTimestamp(String timestamp);
    void setOriginalTimestamp(String originalTimestamp);
    void setSentAt(String sentAt);
    void setReceivedAt(String receivedAt);
    void setEventName(String eventName);
    void setEventType(String eventType);
    void setEventParams(JSONObject eventParams);
    void setOrigin(String origin);

    Event getEvent();
}


public class FilumEventBuilder implements EventBuilder{
    private Event event;

    public FilumEventBuilder(){
        this.event = new Event();
        this.event.setEventID(UUID.randomUUID().toString());
        this.setAnonymousID(null);
        this.setUserID(null);
        this.setTimestamp(null);
        this.setOriginalTimestamp(null);
        this.setSentAt(null);
        this.setReceivedAt(null);
        this.setOrigin(null);
    }

    @Override
    public void setAnonymousID(String anonymousID) {
        if(anonymousID != null) {
            this.event.setAnonymousID(anonymousID);
        }
        else{
            this.event.setAnonymousID("");
        }
    }

    @Override
    public void setUserID(String userID) {
        if(userID != null){
            this.event.setUserID(userID);
        }
        else{
            this.event.setUserID("");
        }
    }

    @Override
    public void setContext(JSONObject context) {
        if(context != null){
            this.event.setContext(context);
        }
    }

    @Override
    public void setTimestamp(String timestamp) {
        if(timestamp != null){
            this.event.setTimestamp(timestamp);
        }
        else{
            this.event.setTimestamp(getCurrentTimeISO());
        }
    }

    @Override
    public void setOriginalTimestamp(String originalTimestamp) {
        if(originalTimestamp != null){
            this.event.setOriginalTimestamp(originalTimestamp);
        }
        else{
            this.event.setOriginalTimestamp(getCurrentTimeISO());
        }
    }

    @Override
    public void setSentAt(String sentAt) {
        if(sentAt != null){
            this.event.setSentAt(sentAt);
        }
        else{
            this.event.setSentAt(getCurrentTimeISO());
        }
    }

    @Override
    public void setReceivedAt(String receivedAt) {
        if(receivedAt != null){
            this.event.setReceivedAt(receivedAt);
        }
        else{
            this.event.setReceivedAt(getCurrentTimeISO());
        }
    }

    @Override
    public void setEventName(String eventName) {
        if(eventName != null){
            this.event.setEventName(eventName);
        }
    }

    @Override
    public void setEventType(String eventType) {
        this.event.setEventType(eventType);
    }

    @Override
    public void setEventParams(JSONObject eventParams) {
        if(eventParams != null){
            this.event.setEventParams(eventParams);
        }
    }

    @Override
    public void setOrigin(String origin) {
        if(origin != null){
            this.event.setOrigin(origin);
        }
        else{
            this.event.setOrigin("");
        }
    }

    @Override
    public Event getEvent() {
        return this.event;
    }

    public static String getCurrentTimeISO(){
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());
        return nowAsISO;
    }
}
