#!/usr/bin/env bash
set -euo pipefail

KEYSTORE_OUT="${1:-my-upload-key.jks}"
ALIAS_NAME="${2:-upload}"

if [ -f "$KEYSTORE_OUT" ]; then
  echo "Keystore '$KEYSTORE_OUT' already exists. Aborting to avoid overwriting existing keys."
  exit 1
fi

echo "Generating PKCS12 release upload keystore at '$KEYSTORE_OUT' (alias: '$ALIAS_NAME')..."
keytool -genkeypair -v \
  -keystore "$KEYSTORE_OUT" \
  -alias "$ALIAS_NAME" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storetype PKCS12

echo ""
echo "Keystore created. Fingerprints for Firebase / Google Play Console:"
keytool -list -v -keystore "$KEYSTORE_OUT" -alias "$ALIAS_NAME"
echo ""
echo "Export environment variables before running 'gradle :app:assembleRelease':"
echo "  export KEYSTORE_PATH=\"$(pwd)/$KEYSTORE_OUT\""
echo "  export STORE_PASSWORD=\"<your-store-password>\""
echo "  export KEY_PASSWORD=\"<your-key-password>\""
