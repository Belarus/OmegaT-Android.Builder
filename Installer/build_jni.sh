#!/bin/bash
set -x

javah -o jni/stat.h -classpath bin/classes org.alex73.android.common.JniWrapper
/opt/android-ndk/ndk-build clean
/opt/android-ndk/ndk-build NDK_DEBUG=0

for i in libs/*; do
  archfrom=`basename $i`
  archto=`basename $i | tr - _`
  mv libs/$archfrom/replacer res/raw/replacer_$archto.bin
done
