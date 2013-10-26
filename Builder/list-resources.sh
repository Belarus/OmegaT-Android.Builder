#!/bin/sh

echo 'Гэты скрыпт зьбірае рэсурсы з усіх файлаў apk'
exit

aapt=/opt/android-sdk-linux/build-tools/18.1.1/aapt
cd /somewhere

set -x

find -name '*.apk.dump' -exec rm \{\} \;
rm dumps.zip

for apk in `find -name '*.apk'`; do 
  $aapt d --values resources $apk > $apk.dump
done

zip dumps.zip `find -name '*.apk.dump'`
