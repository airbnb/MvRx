#! /bin/bash

if [ -e "$ANDROID_SDK" ]; then
    echo "Android SDK exists. Exiting"
    exit 0
fi

curl https://dl.google.com/android/repository/sdk-tools-linux-4333796.zip --output /tmp/sdk.zip
unzip /tmp/sdk.zip $ANDROID_HOME

sdkmanager "platform-tools" "platforms;android-27" "system-images;android-27;default;x86_64"