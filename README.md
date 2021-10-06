# FILUM ANDROID SDK

## Example App

Example app is in Filum Android Example repository at https://github.com/Filum-AI/filum-android-example , use Android Studio to build and run the app.

Make sure to update `serverUrl` and `token` in `FILUM.initialize()` before running.

## Installation

**Step 1: Add this repository as a submodule**

```sh
git submodule add https://github.com/Filum-AI/filum-android-sdk
```

**Step 2: Import above module as a module**

- In Android Studio, select `File` -> `New` -> `Import Module...`
- Choose `filum_android_sdk`

**Step 3: Implement filum_android_sdk in build.gradle**

- Open `build.gradle` of your app
- Add this line

```text
...
dependencies {
    ...
    implementation project(':filum_android_sdk')
    ...
}
```

## Usage

### Initialize

```java
final FILUM filum = FILUM.initialize(
    "http://server-url.example",
    "your_project_token",
    MainActivity.this
);
```

### Identify

Use this method right after user has just logged in or anytime a trait of an user is updated.

```java
try{
    JSONObject user_properties = new JSONObject();
    user_properties.put("username", "username1);
    user_properties.put("email", "user_email@gmail.com");
} catch (JSONException e) {
    e.printStackTrace();
}
filum.identify(userId, user_properties);
```

### Track event

Use a string to represent the event name and a JSONObject to contain all custom properties.

```java
// With custom props
try {
    JSONObject props = new JSONObject();
    props.put("price", 100);
    props.put("package_sku", "package_1_free");
    filum.track("Item Purchased", props);
} catch (JSONException e) {
    e.printStackTrace();
}

// Without custom props
filum.track("End session");
```

### Reset

Use this method right after the user has just logged out. It will generate new anonymous ID for a new user.

```java
filum.reset();
```
