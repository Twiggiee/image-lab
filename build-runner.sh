#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes build/dex
javac --release 8 -encoding UTF-8 -cp /root/android-sdk/android-36/android.jar:runtime/litert-api.jar:runtime/kotlin-stdlib.jar -d build/classes src/com/buddyapps/imagelab/*.java
jar cf build/runner-classes.jar -C build/classes .
d8 --release --min-api 26 --lib /root/android-sdk/android-36/android.jar --output build/dex runtime/litert-api.jar runtime/kotlin-stdlib.jar build/runner-classes.jar > build/d8.log 2>&1
printf 'Runner built: %s\n' "$PWD/build/dex/classes.dex"
