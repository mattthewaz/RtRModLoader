#!/bin/sh
cd "$(dirname "$0")"
mvn package
cp launch-modloader.sh launch-modloader.command launch-modloader.bat target/
jar --create --file target/RtRModLoader.zip \
    -C target RtRModLoader.jar \
    -C target launch-modloader.sh \
    -C target launch-modloader.command \
    -C target launch-modloader.bat
echo "[RtRModLoader] Packaged to target/RtRModLoader.zip"
