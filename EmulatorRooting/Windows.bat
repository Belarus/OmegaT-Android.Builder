adb remount
adb push build.prop /system/build.prop
adb install GoogleServicesFramework.apk
adb install Vending.apk
adb install Gmail.apk
adb shell rm /system/app/SdkSetup.apk
adb shell mount -o rw,remount -t yaffs2 /dev/block/mtdblock03 /system
adb push su /system/xbin/su
adb push busybox /system/xbin/busybox
adb push installbusybox.sh /system/xbin/installbusybox.sh
adb shell chmod 06755 /system/*
adb shell chmod 06755 /system/xbin/*
adb install superuser.apk
adb shell installbusybox.sh