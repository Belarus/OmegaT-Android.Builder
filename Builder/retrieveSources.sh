#!/bin/bash

set -x

g2() {
  echo "===== Retrieve branch $3 from $2 into $1 ====="
  pushd /data/android/$1 || exit
  ../repo init -u $2 -b $3
  ../repo sync -n
  popd
}

g() {
  echo "===== Unpack branch $3 from $2 into $1, then add as $4 ====="
  echo rm -rf /data/android/$1/*
  rm -rf /data/android/$1/*
  pushd /data/android/$1 || exit
  ../repo init -u $2 -b $3 || exit 1
  ../repo sync -l
  popd
  java -cp classes:../Installer/bin/classes:lib/commons-io-1.4.jar UnpackCyanogenResources /data/android/$1 $4 || exit 1
}

rm -rf ../../Android.OmegaT/Android/source

ALL_PROXY=
all_proxy=

g cyanogen git://github.com/CyanogenMod/android.git froyo-stable           cyanogen-2.2
g cyanogen git://github.com/CyanogenMod/android.git gingerbread-release    cyanogen-2.3
g cyanogen git://github.com/CyanogenMod/android.git ics-release            cyanogen-4.0
g cyanogen git://github.com/CyanogenMod/android.git jellybean              cyanogen-4.1

#see page https://android.googlesource.com/platform/manifest/ for list of branches
g source https://android.googlesource.com/platform/manifest android-2.2.3_r2.1    android-2.2
g source https://android.googlesource.com/platform/manifest android-2.3.7_r1      android-2.3
g source https://android.googlesource.com/platform/manifest android-4.0.4_r2.1    android-4.0
g source https://android.googlesource.com/platform/manifest android-4.1.2_r1      android-4.1

java -cp classes:lib/commons-io-1.4.jar:lib/junit-4.10.jar:../Installer/bin/classes UnpackBinaryResources /data/android/binaries
