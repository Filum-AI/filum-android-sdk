package filum.android.sdk.entity;

import org.json.JSONException;
import org.json.JSONObject;

import filum.android.sdk.BuildConfig;
import filum.android.sdk.common.Serializable;
import filum.android.sdk.constant.TaskProps;

public class Task implements Serializable {
    private String type;
    private JSONObject data = null;
    private String sdkVersion = null;
    private Boolean processing = false;

    public Task(String type) {
        this.sdkVersion = BuildConfig.VERSION_NAME;
        this.type = type;
    }

    public Task(String type, JSONObject data) {
        this(type);
        this.data = data;
    }

    public Task(JSONObject serializedTask) throws JSONException {
        type = serializedTask.getString(TaskProps.TASK_TYPE);
        try {
            data = serializedTask.getJSONObject(TaskProps.DATA);
        } catch (JSONException e) {
            data = null;
        }
        try {
            sdkVersion = serializedTask.getString(TaskProps.SDK_VERSION);
        } catch (JSONException e) {
            sdkVersion = null;
        }
    }

    public String getType() {
        return type;
    }

    public JSONObject getData() {
        return data;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }

    public String getSdkVersion() {
        return sdkVersion;
    }

    public Boolean getProcessing() {
        return processing;
    }

    public void setProcessing(Boolean processing) {
        this.processing = processing;
    }

    public JSONObject serialize() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(TaskProps.TASK_TYPE, type);
        json.put(TaskProps.DATA, data);
        json.put(TaskProps.SDK_VERSION, sdkVersion);
        return json;
    }
}
