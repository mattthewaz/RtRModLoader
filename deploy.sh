#!/bin/sh
cd "$(dirname "$0")"
OUT="$(realpath "$(pwd)/../RtR")"
cp target/RtRModLoader.jar "$OUT/"
echo "[RtRModLoader] Deployed to $OUT"
