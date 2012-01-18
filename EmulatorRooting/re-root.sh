adb shell mount -o rw,remount -t yaffs2 /dev/block/mtdblock03 /system
adb shell rm /system/xbin/su
adb shell rm /system/xbin/busybox
adb push ./su /system/xbin/su
adb push ./busybox /system/xbin/busybox
adb push ./installbusybox.sh /system/xbin/installbusybox.sh
adb shell chmod 06755 /system/* /system/xbin/* /system/lib/* /system/framework/* /system/bin/* /system/app/* /system/build.prop
adb shell installbusybox.sh