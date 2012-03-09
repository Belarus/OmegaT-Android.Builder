#!/bin/sh
set -x

export PATH=/cygdrive/C/cygwin/bin:$PATH

rsync -v -c --chmod=ugo=rwX -r -e "ssh -C -l alex" *.php *.apk ../out/* android.mounik.org:public_html/android/
rsync -v -c --chmod=ugo=rwX -r -e "ssh -C -l alex" *.php *.apk  android.mounik.org:public_html/android/
read i

