#!/bin/bash

g() {
  echo "===== Retrieve branch $3 from $2 into $1 ====="
  pushd /data/android/$1 || exit
  ../repo init -u $2 -b $3
  ../repo sync -n
  popd
}

g2() {
  echo "===== Unpack branch $3 from $2 into $1, then add as $4 ====="
  echo rm -rf /data/android/$1/*
  rm -rf /data/android/$1/*
  pushd /data/android/$1 || exit
  ../repo init -u $2 -b $3 || exit 1
  ../repo sync -l || exit 1
  popd
  java -cp classes:lib/commons-io-1.4.jar UnpackCyanogenResources /data/android/$1 $4 || exit 1
}

g cyanogen git://github.com/CyanogenMod/android.git froyo-stable cyanogen-froyo
g cyanogen git://github.com/CyanogenMod/android.git gingerbread-release cyanogen-gingerbread
g cyanogen git://github.com/CyanogenMod/android.git ics-release cyanogen-ics
g cyanogen git://github.com/CyanogenMod/android.git jellybean cyanogen-jellybean

g source https://android.googlesource.com/platform/manifest froyo-release source-froyo
g source https://android.googlesource.com/platform/manifest gingerbread-release source-gingerbread
g source https://android.googlesource.com/platform/manifest ics-mr1-release source-ics
g source https://android.googlesource.com/platform/manifest jb-mr0-release source-jb

java -cp classes UnpackBinaryResources /data/android/binaries
