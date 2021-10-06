package filum.android.sdk.service;

import java.util.UUID;

import filum.android.sdk.constant.StorageKey;

public class IdentityManager {
    private String userID = null;
    private String anonymousID = null;
    private Storage storage;

    public IdentityManager(Storage storage) {
        this.storage = storage;

        String distinctId = storage.getString(StorageKey.ANONYMOUS_ID);
        if (distinctId == null) {
            resetIDs();
        } else {
            this.anonymousID = distinctId;
        }

        String userID = storage.getString(StorageKey.USER_ID);
        if (userID != null) {
            this.userID = userID;
        }
        else{
            this.userID = "";
        }
    }

    public static IdentityManager makeInstance(Storage storage) {
        return new IdentityManager(storage);
    }

    public String getAnonymousID() {
        return anonymousID;
    }

    public String getUserID() {
        return userID;
    }

    public String resetIDs() {
        anonymousID = UUID.randomUUID().toString();
        updateAnonymousID(anonymousID);
        updateUserID("");
        return anonymousID;
    }

    public void updateAnonymousID(String anonymousID) {
        this.anonymousID = anonymousID;
        storage.put(StorageKey.ANONYMOUS_ID, anonymousID);
    }

    public void updateUserID(String userID) {
        this.userID = userID;
        storage.put(StorageKey.USER_ID, userID);
    }
}
