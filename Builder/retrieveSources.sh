#!/bin/bash

set -x

STORAGE=/data/sources-android

ga() {
  echo "===== Retrieve branch $3 from $2 into $1 ====="
  pushd $STORAGE/$1 || exit
  ../repo init -u $2 -b $3
  ../repo sync -n
  popd
}

g() {
  echo "===== Unpack branch $3 from $2 into $1, then add as $4 ====="
  echo rm -rf $STORAGE/$1/*
  rm -rf $STORAGE/$1/*
  pushd $STORAGE/$1 || exit
  ../repo init -u $2 -b $3 || exit 1
  ../repo sync -l
  popd
  java -cp classes:../Installer/bin/classes:lib/commons-io-1.4.jar UnpackCyanogenResources $STORAGE/$1 $4 || exit 1
}

rm -rf ../../Android.OmegaT/source

ALL_PROXY=
all_proxy=

#see page https://github.com/CyanogenMod/android.git for list of branches
g cyanogen git://github.com/CyanogenMod/android.git froyo-stable           cyanogen-2.2
g cyanogen git://github.com/CyanogenMod/android.git gingerbread-release    cyanogen-2.3
g cyanogen git://github.com/CyanogenMod/android.git cm-9.1.0               cyanogen-4.0
g cyanogen git://github.com/CyanogenMod/android.git jellybean-release      cyanogen-4.1
g cyanogen git://github.com/CyanogenMod/android.git cm-10.1                cyanogen-4.2
g cyanogen git://github.com/CyanogenMod/android.git cm-10.2                cyanogen-4.3

#see page https://android.googlesource.com/platform/manifest/ for list of branches
g source https://android.googlesource.com/platform/manifest android-2.2.3_r2.1    android-2.2
g source https://android.googlesource.com/platform/manifest android-2.3.7_r1      android-2.3
g source https://android.googlesource.com/platform/manifest android-4.0.4_r2.1    android-4.0
g source https://android.googlesource.com/platform/manifest android-4.1.2_r2.1    android-4.1
g source https://android.googlesource.com/platform/manifest android-4.2.2_r1.2b   android-4.2
g source https://android.googlesource.com/platform/manifest android-4.3.1_r1      android-4.3

java -cp classes:lib/commons-io-1.4.jar:lib/junit-4.10.jar:../Installer/bin/classes UnpackBinaryResources $STORAGE/binaries
