package filum.android.sdk.common;

import org.json.JSONException;
import org.json.JSONObject;

public interface Serializable {
    JSONObject serialize() throws JSONException;
}
