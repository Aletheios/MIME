# MIME prototype #

This is the prototype used in the user study for the MobileHCI 2015 paper "MIME: Teaching Mid-Air Pose-Command Mappings". It is based on OpenCV for Android and features basic hand pose recognition for 12 different poses. The app also manages the entire process of the user study as described in the paper.

The prototype has been tweaked for the Samsung Galaxy S2 (running Android 4.4) with a screen size of 800x480 **(!)**, but might run on other devices as well. Please be aware that the hand recognition requires a high computational effort which is why your device might heat up quickly.


### How to test the prototype ###

- Make sure your smartphone runs Android 4.1 Jelly Bean or higher
- Copy the APK file from the ``apk`` directory to your device
- Enable "Unknown Sources" (Settings > Security) before installing the APK
- Upon first launch you will be prompted to install the "OpenCV Manager" - it's available on Google Play
- Copy the ``.saved_poses`` file from the ``apk`` directory to a new directory on your SD card called ``MIME``
- Choose "Import" from the app's menu to import the set of 12 hand poses


### How to build the prototype yourself ###

- Download and install [Android Studio](https://developer.android.com/sdk/index.html) and the [Android NDK](https://developer.android.com/tools/sdk/ndk/index.html)
- Clone repository and import in Android Studio
- Install Android SDK level 19 in Android Studio if necessary
- Set path to NDK in ``local.properties`` (Windows users: make sure to use double backslashes in the path, e.g. ``C\:\\ndk``)
- Refresh Gradle tasks and run ``app > build``


### How to use the prototype ###

The hand recognition routine is based on background subtraction and color blob detection. It works best with a neutral background (e.g. green screen) and under good lighting conditions (bright white light).

When the app is launched, a background frame has to be set for the background subtraction; it will be set automatically after initially choosing the study mode, phase and user ID. Make sure your hand isn't in the camera's viewport.

The prototype's menu offers several options:

- **Set background:** Set a fresh background frame for the background subtraction algorithm
- **Show FPS meter** 
- **Enable study mode:** Hide menu items and automatically set background on app launch
- **Record background:** Save background frame to internal storage so it's directly available on app launch. Also averages several background frames for a better recognition.
- **Enable saved background:** Use recorded background so it doesn't have to be set everytime the app is launched.
- **Record poses:** Train the $N recognizer with a set of 12 hand poses. An example file is provided in the repository: ``apk > .saved_poses``
- **Show list of poses**
- **Show recognized pose**
- **Import:** Import saved background and poses from internal storage (``MIME`` directory on your SD card)
- **Reset app**
- **Settings:** Offers several parameters to directly control the hand shape recognizer

All measurements during the study are stored in an internal SQLite database. When the user completes a retention test (short term or long term), the app exports the current state of the database to the ``MIME`` directory on the SD card.