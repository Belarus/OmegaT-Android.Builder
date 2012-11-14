rem From http://forum.xda-developers.com/showthread.php?t=821742

set PATH=c:\opt\android-sdk\platform-tools\;%PATH%

cmd /c adb -e remount
cmd /c adb -e push su /system/xbin/su
cmd /c adb -e shell chmod 06755 /system
cmd /c adb -e shell chmod 06755 /system/xbin/su
cmd /c adb -e install superuser.apk
pause