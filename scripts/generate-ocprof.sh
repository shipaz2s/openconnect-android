#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 6 ]]; then
    echo "Usage:"
    echo "  $0 <address> <port> <code_word> <certificate.pem> <private_key.pem> <ca_certificate.pem>"
    exit 1
fi

ADDRESS="$1"
PORT="$2"
CODE_WORD="$3"
CERT_FILE="$4"
KEY_FILE="$5"
CA_FILE="$6"

if [[ -z "$ADDRESS" ]]; then
    echo "Error: address is empty"
    exit 1
fi

if ! [[ "$PORT" =~ ^[0-9]+$ ]] || (( PORT < 1 || PORT > 65535 )); then
    echo "Error: invalid port: $PORT"
    exit 1
fi

for FILE in "$CERT_FILE" "$KEY_FILE" "$CA_FILE"; do
    if [[ ! -f "$FILE" ]]; then
        echo "Error: file not found: $FILE"
        exit 1
    fi

    if [[ ! -s "$FILE" ]]; then
        echo "Error: file is empty: $FILE"
        exit 1
    fi
done

OUTPUT="${ADDRESS//[^a-zA-Z0-9._-]/_}.ocprof"

cat > "$OUTPUT" <<EOF
OCONNECT_PROFILE_V1
name=$ADDRESS
address=$ADDRESS
port=$PORT
code_word=$CODE_WORD

[CERTIFICATE]
EOF

cat "$CERT_FILE" >> "$OUTPUT"

cat >> "$OUTPUT" <<EOF

[PRIVATE_KEY]
EOF

cat "$KEY_FILE" >> "$OUTPUT"

cat >> "$OUTPUT" <<EOF

[CA_CERTIFICATE]
EOF

cat "$CA_FILE" >> "$OUTPUT"

# Гарантируем перевод строки в конце файла.
printf '\n' >> "$OUTPUT"

echo "Profile generated: $OUTPUT"