#!/bin/bash
LWJGL=/tmp/opencode/lwjgl
CP=$(find "$LWJGL" -name "*.jar" | tr '\n' ':')
java --enable-native-access=ALL-UNNAMED -cp "$CP." Main "$@"
