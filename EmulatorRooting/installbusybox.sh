mount -o rw,remount -t yaffs2 /dev/block/mtdblock3 /system
cat /system/xbin/busybox > /data/local/busybox
chmod 755 /data/local/busybox
/data/local/busybox mkdir /system/xbin
cd /data/local
./busybox cp /data/local/busybox /system/xbin
cd /system/xbin
chmod 755 busybox
./busybox --install -s /system/xbin
rm /data/local/busybox
#reboot
