# Release signing (sharing the APK with colleagues)

A debug APK is fine for trying the app. For colleagues, build a **signed release APK**
so that future versions install as updates over the old one. Android only accepts an
update if it is signed with the **same key**, so create the key once and keep it safe.

## 1. Create a key (once)

Android Studio: **Build > Generate Signed App Bundle / APK > APK > Create new...**
Or from a terminal (keytool comes with Android Studio's Java):

```
keytool -genkeypair -v -keystore ~/keys/book-reader.jks -alias bookreader \
  -keyalg RSA -keysize 4096 -validity 10000
```

Keep the `.jks` file and both passwords somewhere safe (a password manager).
If you lose them, colleagues have to uninstall the app (losing their library) to get updates.
Never put the key in the project folder, email or chat.

## 2. Tell the build where the key is

Create `keystore.properties` in the `book-reader` folder (it is git-ignored):

```
storeFile=/home/you/keys/book-reader.jks
storePassword=<your store password>
keyAlias=bookreader
keyPassword=<your key password>
```

## 3. Build

```
./gradlew assembleRelease
```

The signed APK is `app/build/outputs/apk/release/app-release.apk`.
Without `keystore.properties`, the release build is unsigned and won't install.

## 4. Share

Send `app-release.apk` through Drive, email or a shared folder. Colleagues tap it and
allow "Install unknown apps" for the app they opened it from. For each new version,
raise `versionCode` and `versionName` in `app/build.gradle.kts` and sign with the same key.
