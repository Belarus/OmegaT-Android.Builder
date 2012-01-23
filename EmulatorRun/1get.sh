#!/bin/sh
set -x

FROM=alex@192.168.159.129:/data/cyanogen/


scp -rC $FROM/out/target/product/generic/*.img .
scp -rC $FROM/out/target/product/generic/root .
scp -rC $FROM/out/target/product/generic/system .
scp -rC $FROM/prebuilt/android-arm/kernel/kernel-qemu-armv7 .

#    mksdcard 1024M out/target/product/generic/sdcard.img
