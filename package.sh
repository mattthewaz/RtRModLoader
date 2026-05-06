#!/bin/sh
cd "$(dirname "$0")"
mvn package
zip -j target/RtRModLoader.zip \
    target/RtRModLoader.jar \
    launch-modloader.sh \
    launch-modloader.command \
    launch-modloader.bat
echo "[RtRModLoader] Packaged to target/RtRModLoader.zip"
