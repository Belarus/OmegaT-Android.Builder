#!/bin/bash
set -x

javah -o jni/stat.h -classpath bin/classes org.alex73.android.common.JniWrapper
/data/opt/android-ndk-r8c/ndk-build  NDK_DEBUG=0
