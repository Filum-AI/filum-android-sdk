package filum.android.sdk.entity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import filum.android.sdk.BuildConfig;
import filum.android.sdk.constant.TaskProps;
import filum.android.sdk.constant.TaskType;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TaskTest {

    @Test
    public void constructor() {
        Task noDataTask = new Task(TaskType.TRACK);
        assertEquals(TaskType.TRACK, noDataTask.getType());
        assertEquals(BuildConfig.VERSION_NAME, noDataTask.getSdkVersion());
        assertNull(noDataTask.getData());

        JSONObject data = mock(JSONObject.class);
        Task withDataTask = new Task(TaskType.TRACK, data);
        assertEquals(TaskType.TRACK, withDataTask.getType());
        assertSame(data, withDataTask.getData());

        try {
            JSONObject data2 = mock(JSONObject.class);
            JSONObject serialized = mock(JSONObject.class);
            when(serialized.getString(TaskProps.TASK_TYPE)).thenReturn(TaskType.IDENTIFY);
            when(serialized.getJSONObject(TaskProps.DATA)).thenReturn(data2);
            Task fromSerializedTask = new Task(serialized);
            assertEquals(TaskType.IDENTIFY, fromSerializedTask.getType());
            assertSame(data2, fromSerializedTask.getData());
        } catch (JSONException e) {
            fail();
        }
    }

    @Test
    public void getType() {
        Task task = new Task(TaskType.TRACK);
        assertEquals(TaskType.TRACK, task.getType());
    }

    @Test
    public void getSdkVersion() {
        Task task = new Task(TaskType.TRACK);
        assertEquals(BuildConfig.VERSION_NAME, task.getSdkVersion());
    }

    @Test
    public void get_set_Data() {
        JSONObject data = mock(JSONObject.class);
        Task task = new Task(TaskType.TRACK, data);
        assertSame(data, task.getData());

        JSONObject data2 = mock(JSONObject.class);
        task.setData(data2);
        assertSame(data2, task.getData());
    }

    @Test
    public void get_set_Processing() {
        Task task = new Task(TaskType.TRACK);
        assertFalse(task.getProcessing());
        task.setProcessing(true);
        assertTrue(task.getProcessing());
    }
}