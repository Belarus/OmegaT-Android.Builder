#!/bin/bash
set -x

DEVICE=4.3.1-s2-cyanogen

rm -rf /tmp/$DEVICE
mkdir /tmp/$DEVICE
cd /tmp/$DEVICE

mkdir -p system/app system/framework
/opt/android-sdk-linux/platform-tools/adb pull /system/app system/app/
/opt/android-sdk-linux/platform-tools/adb pull /system/framework system/framework/
zip -9r /data/sources-android/binaries/$DEVICE.zip *

