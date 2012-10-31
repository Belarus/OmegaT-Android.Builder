#!/bin/bash

DEVICE=4.1.2-emulator

mkdir /tmp/$DEVICE
cd /tmp/$DEVICE

mkdir -p system/app system/framework
/opt/android-sdk-linux/platform-tools/adb pull /system/app system/app/
/opt/android-sdk-linux/platform-tools/adb pull /system/framework system/framework/
zip -9r /data/android/binaries/$DEVICE.zip *

