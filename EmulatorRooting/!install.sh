#!/bin/sh
set -x

echo From http://forum.xda-developers.com/showthread.php?t=821742

/opt/android-sdk-linux/platform-tools/adb -e remount
/opt/android-sdk-linux/platform-tools/adb -e push su /system/xbin/su
/opt/android-sdk-linux/platform-tools/adb -e shell chmod 06755 /system
/opt/android-sdk-linux/platform-tools/adb -e shell chmod 06755 /system/xbin/su
/opt/android-sdk-linux/platform-tools/adb -e install Superuser.apk
