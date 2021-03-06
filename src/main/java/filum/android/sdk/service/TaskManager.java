package filum.android.sdk.service;

import filum.android.sdk.BuildConfig;
import androidx.annotation.VisibleForTesting;

import com.android.volley.NoConnectionError;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import filum.android.sdk.constant.CommonConstant;
import filum.android.sdk.constant.CommonProps;
import filum.android.sdk.constant.StorageKey;
import filum.android.sdk.constant.TaskType;
import filum.android.sdk.entity.Event;
import filum.android.sdk.entity.Task;
import filum.android.sdk.util.Logger;

public class TaskManager {
    private Queue<Task> taskQueue = new LinkedList<>();
    private Queue<Task> processingQueue = new LinkedList<>();
    private Boolean flushing = false;
    private Boolean started = false;
    private Boolean shouldNotRestart = false;
    private ScheduledExecutorService scheduler = null;
    private ScheduledFuture<?> scheduledJobHandler = null;
    private Storage storage;
    private IdentityManager identityManager;
    private NetworkManager networkManager;

    public TaskManager(Storage storage, IdentityManager identityManager, NetworkManager networkManager) {
        this.storage = storage;
        this.identityManager = identityManager;
        this.networkManager = networkManager;

        initScheduler();
        start();
    }

    public static TaskManager makeInstance(Storage storage, IdentityManager identityManager, NetworkManager networkManager) {
        return new TaskManager(storage, identityManager, networkManager);
    }

    private void storeTasks() {
        Logger.log("TASK MANAGER: storing tasks into Storage ...");
        JSONArray array = new JSONArray();
        Queue<Task> clonedQueue = new LinkedList<>(taskQueue);
        while (clonedQueue.size() > 0) {
            Task task = clonedQueue.poll();
            JSONObject serializedTask;
            try {
                serializedTask = task.serialize();
            } catch (JSONException exception) {
                Logger.error(exception);
                continue;
            }
            array.put(serializedTask);
        }
        storage.put(StorageKey.STORED_TASKS, array.toString());
        Logger.log("TASK MANAGER: storing has completed!");
    }

    private void loadStoredTasks() {
        Logger.log("TASK MANAGER: loading tasks from Storage ...");
        String storedTasks = storage.getString(StorageKey.STORED_TASKS);
        if (storedTasks != null) {
            JSONArray array;
            try {
                array = new JSONArray(storedTasks);
                Logger.log("TASK MANAGER: found " + array.length() + " task(s) in storage.");
            } catch (JSONException e) {
                Logger.error(e);
                return;
            }
            for (int i = 0; i < array.length(); i++) {
                try {
                    JSONObject serializedTask = array.getJSONObject(i);
                    Task task = new Task(serializedTask);
                    // Only accept tasks from current SDK version
                    if (BuildConfig.VERSION_NAME.equals(task.getSdkVersion())) {
                        addTask(task);
                    } else {
                        Logger.log("TASK MANAGER: ignore task from old SDK version ...");
                    }
                } catch (JSONException e) {
                    Logger.error(e);
                }
            }
        }
        Logger.log("TASK MANAGER: loading saved tasks completed!");
    }

    private void addTask(Task task) {
        while (taskQueue.size() >= CommonConstant.TASK_QUEUE_SIZE_LIMIT) {
            Logger.warn(
                    "TASK MANAGER: exceeding task queue limit ("
                    + CommonConstant.TASK_QUEUE_SIZE_LIMIT + "). "
                    + "Removing oldest event in the queue ..."
            );
            taskQueue.poll();
        }
        if (shouldNotRestart) {
            Logger.warn("TASK MANAGER: Service stopped permanently. Ignore this " + task.getType() + " task.");
            return;
        }
        taskQueue.add(task);
        Logger.log("TASK MANAGER: added new task to the queue!");
    }

    public void createEventTask(Event event) {
        Task task;
        try {
            task = new Task(TaskType.TRACK, event.serialize());
        } catch (JSONException exception) {
            Logger.error(exception);
            return;
        }
        addTask(task);
    }

    public void createIdentifyTask(Event event) {
        Task task;
        try{
             task = new Task(TaskType.IDENTIFY, event.serialize());
        }catch (JSONException exception) {
            Logger.error(exception);
            return;
        }
        addTask(task);
        String userId = event.getUserID();
        identityManager.updateUserID(userId);
    }

    private List<JSONObject> dequeueEvents() {
        Logger.log("DEQUEUE: events");
        return dequeueEvents(new ArrayList<JSONObject>());
    }

    private List<JSONObject> dequeueEvents(List<JSONObject> previousList) {
        if (processingQueue.size() == 0) {
            Logger.log("DEQUEUE: nothing left to dequeue");
            return previousList;
        }
        Task topTask = processingQueue.peek();
//        if (!topTask.getType().equals(TaskType.TRACK)) {
//            Logger.log("DEQUEUE: top item is not event, stop dequeue");
//            return previousList;
//        }
        topTask.setProcessing(true);
        processingQueue.poll();
        previousList.add(topTask.getData());
        Logger.log("DEQUEUE: added 1 event to the list");
        return dequeueEvents(previousList);
    }

    private Response.Listener<JSONObject> createSuccessCallback(final String taskName) {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Logger.log("FLUSHING: " + taskName + " task(s) succeed! - " + response.toString());
                cleanUpProcessingTasks();
                finishFlushing();
            }
        };
    }

    private Response.ErrorListener createErrorCallback() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                Logger.error(e);
                if (e instanceof NoConnectionError || e.networkResponse == null) {
                    Logger.log("FLUSHING: network error, retry!");
                } else {
                    int statusCode = e.networkResponse.statusCode;
                    int detailedStatusCode = -1;
                    try {
                        String body = new String(e.networkResponse.data, "UTF-8");
                        detailedStatusCode = new JSONObject(body).getInt(CommonProps.ERROR_CODE);
                    } catch (Exception e1) {
                        Logger.error(e);
                    }
                    if (detailedStatusCode == CommonConstant.DISABLED_TOKEN_ERROR_CODE) {
                        Logger.error("UNAUTHORIZED: This token is disabled at the moment. Stopping all FILUM's services and ignore all events.");
                        forceStopPermanently();
                        return;
                    } else if (statusCode >= CommonConstant.MIN_SERVER_ERROR_STATUS_CODE && statusCode <= CommonConstant.MAX_SERVER_ERROR_STATUS_CODE) {
                        Logger.log("FLUSHING: FILUM Platform Core internal error!");
                    } else {
                        cleanUpProcessingTasks();
                    }
                }
                finishFlushing();
            }
        };
    }

    private void cleanUpProcessingTasks() {
        while (taskQueue.size() > 0 && taskQueue.peek().getProcessing()) {
            taskQueue.poll();
        }
        Logger.log("FLUSHING: Cleaned up finished tasks!");
    }

    private void finishFlushing() {
        processingQueue.clear();
        if (!started) {
            Logger.log("FLUSHING: scheduler has stopped due to app's inactivity, save current queue into storage");
            storeTasks();
        }
        flushing = false;
        Logger.log("FLUSHING: Flushing finished!");
    }

    private final Runnable flush = new Runnable() {
        public void run() {
            if (flushing) {
                Logger.log("INCOMING FLUSHING: Another flushing is running. Ignore this flushing.");
                return;
            }
            if (taskQueue.size() == 0) {
                Logger.log("INCOMING FLUSHING: Nothing to flush. Ignore this flushing.");
                return;
            }
            flushing = true;
            Logger.log("INCOMING FLUSHING: New flushing has started!");

            processingQueue.clear();
            processingQueue.addAll(taskQueue);

            Task topTask = processingQueue.peek();
            switch (topTask.getType()) {
                case TaskType.TRACK: {
                    Logger.log("FLUSHING: trying to flush event tasks");
                    // Try to dequeue a batch of events at the top of the queue
                    List<JSONObject> eventBatch = dequeueEvents();
                    int eventBatchSize = eventBatch.size();
                    if (eventBatchSize > 0) {
                        Logger.log("FLUSHING: dequeue " + eventBatchSize + " events to the batch");
                        JSONArray bodyData = new JSONArray(eventBatch);
                        networkManager.track(
                                bodyData,
                                createSuccessCallback(topTask.getType()),
                                createErrorCallback()
                        );
                    } else {
                        Logger.warn("FLUSHING: empty event batch! (Should not happen)");
                    }
                    break;
                }

                case TaskType.IDENTIFY: {
                    Logger.log("FLUSHING: try to flush identify task");
                    List<JSONObject> eventBatch = dequeueEvents();
                    int eventBatchSize = eventBatch.size();
                    if (eventBatchSize > 0) {
                        Logger.log("FLUSHING: dequeue " + eventBatchSize + " events to the batch");
                        JSONArray bodyData = new JSONArray(eventBatch);
                        networkManager.track(
                                bodyData,
                                createSuccessCallback(topTask.getType()),
                                createErrorCallback()
                        );
                    } else {
                        Logger.warn("FLUSHING: empty event batch! (Should not happen)");
                    }
                    break;
                }
            }
        }
    };

    private synchronized void initScheduler() {
        if (scheduler != null) {
            return;
        }
        scheduler = new ScheduledThreadPoolExecutor(100) {
            @Override
            public void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                boolean isDone = false;
                if (t == null && r instanceof ScheduledFuture<?>) {
                    try {
                        isDone = ((ScheduledFuture<?>) r).isDone();
                        if (isDone) {
                            Logger.warn("SCHEDULED TASK: Scheduler is shut down.");
                            ((ScheduledFuture<?>) r).get();
                        }
                    } catch (CancellationException e) {
                        Logger.warn("SCHEDULED TASK: Scheduled job is cancelled!");
                    } catch (ExecutionException e) {
                        t = e.getCause();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                if (t != null) {
                    // Exception occurred
                    Logger.error(t, "SCHEDULED TASK: Uncaught exception is detected!");
                    // Restart the runnable again
                    if (isDone) {
                        Logger.log("Trying to restart the scheduler ...");
                        startScheduling();
                    }
                }
            }
        };
    }

    @VisibleForTesting
    void startScheduling() {
        if (shouldNotRestart) return;
        if (scheduler == null) {
            Logger.error("TASK MANAGER: scheduler not found (should not be). Can not start scheduling.");
            return;
        }
        if (scheduledJobHandler != null && !scheduledJobHandler.isDone()) {
            Logger.warn("TASK MANAGER: scheduling failed! Previous scheduled job has not done yet!");
            return;
        }
        scheduledJobHandler = scheduler.scheduleAtFixedRate(
                flush,
                CommonConstant.TASKS_FLUSHING_INTERVAL,
                CommonConstant.TASKS_FLUSHING_INTERVAL,
                TimeUnit.SECONDS
        );
        Logger.log("TASK MANAGER: scheduler has started. Flushing tasks every " + CommonConstant.TASKS_FLUSHING_INTERVAL + " second(s).");
    }

    public void start() {
        if (shouldNotRestart) return;
        if (started) {
            Logger.warn("TASK MANAGER: Scheduler has started. Call stop() before starting again.");
            return;
        }
        started = true;
        loadStoredTasks();
        startScheduling();
    }

    public void restart() {
        if (shouldNotRestart) return;
        if (started) {
            Logger.warn("TASK MANAGER: Scheduler has started. Call stop() before starting again.");
            return;
        }
        started = true;
        startScheduling();
    }

    public void stop() {
        if (!started) {
            Logger.warn("TASK MANAGER: Scheduler has not started yet.");
            return;
        }
        Logger.log("TASK MANAGER: stopping scheduler ...");
        if (scheduledJobHandler != null) {
            scheduledJobHandler.cancel(false);
        }
        storeTasks();
        started = false;
    }

    synchronized public void forceStopPermanently() {
        Logger.warn("TASK MANAGER: force stop permanently ...");
        shouldNotRestart = true;
        if (scheduledJobHandler != null) {
            scheduledJobHandler.cancel(true);
        }
        finishFlushing();
        started = false;
        Logger.warn("TASK MANAGER: Stopped permanently. Ignore incoming tasks.");
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    boolean hasStarted() {
        return started;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    Queue<Task> getTaskQueue() {
        return taskQueue;
    }
}
