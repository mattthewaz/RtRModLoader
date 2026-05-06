#!/bin/sh
cd "$(dirname "$0")"
OUT="$(realpath "$(pwd)/../RtR")"
cp target/RtRModLoader.jar "$OUT/"
cp launch-modloader.sh launch-modloader.command launch-modloader.bat "$OUT/"
echo "[RtRModLoader] Deployed to $OUT"
