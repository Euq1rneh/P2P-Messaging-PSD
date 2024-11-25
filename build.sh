#!/bin/bash

# Variables
SRC_DIR="src"
COMMON_DIR="common"
BIN_DIR="bin"
DIST_DIR="dist"
KEYSTORE_DIR="keystores"
TRUSTSTORE_DIR="truststores"
CLIENT_JAR_NAME="messagingAppPeer.jar"
SERVER_JAR_NAME="messagingAppServer.jar"
MANIFEST_FILE="MANIFEST.MF"

# Ensure bin and dist directories exist
if [ ! -d "$BIN_DIR" ]; then
  mkdir -p "$BIN_DIR"
fi
if [ ! -d "$DIST_DIR" ]; then
  mkdir -p "$DIST_DIR"
fi

# Compile all Java files
echo "Compiling Java source files..."
javac -d "$BIN_DIR" -sourcepath "$SRC_DIR" $(find "$SRC_DIR" -name "*.java")

# Check if compilation was successful
if [ $? -eq 0 ]; then
  echo "Compilation completed successfully."
else
  echo "Compilation failed. Please check the error messages."
  exit 1
fi

# Function to copy keystore and truststore
copy_stores() {
  local target_dir=$1
  local keystore_pattern=$2
  local truststore_pattern=$3

  mkdir -p "$target_dir/keystore"
  mkdir -p "$target_dir/truststore"

  # Copy the keystore matching the pattern
  for keystore in $(find "$KEYSTORE_DIR" -type f -name "$keystore_pattern"); do
    cp "$keystore" "$target_dir/keystore/"
  done

  # Copy the truststore matching the pattern
  for truststore in $(find "$TRUSTSTORE_DIR" -type f -name "$truststore_pattern"); do
    cp "$truststore" "$target_dir/truststore/"
  done
}

# Create a Manifest File for the Client JAR with Class-Path entry
echo "Main-Class: client.peer.Main" > "$MANIFEST_FILE"
echo "Class-Path: common/" >> "$MANIFEST_FILE"

# Create the Peer JAR (includes common package) and copy keystores/truststores
echo "Building Peer JAR..."
for i in {1..4}; do
  PEER_DIR="$DIST_DIR/fc$i$i$i$i$i"
  mkdir -p "$PEER_DIR"
  jar cmf "$MANIFEST_FILE" "$PEER_DIR/$CLIENT_JAR_NAME" -C "$BIN_DIR" client -C "$BIN_DIR" common

  # Copy corresponding keystores and truststore
  copy_stores "$PEER_DIR" "fc$i$i$i$i$i-keystore.jceks" "truststore.jceks"
done

# Create a Manifest File for the Server JAR with Class-Path entry
echo "Main-Class: server.server.Main" > "$MANIFEST_FILE"
echo "Class-Path: common/" >> "$MANIFEST_FILE"

# Create the Server JAR (includes common package) and copy keystores/truststores
echo "Building Server JAR..."
for server in amazonServer oracleServer googleServer; do
  SERVER_DIR="$DIST_DIR/$server"
  mkdir -p "$SERVER_DIR"
  jar cmf "$MANIFEST_FILE" "$SERVER_DIR/$SERVER_JAR_NAME" -C "$BIN_DIR" server -C "$BIN_DIR" common

  # Copy corresponding keystores and truststore
  copy_stores "$SERVER_DIR" "${server}-keystore.jceks" "truststore.jceks"
done

# Remove the MANIFEST file after creating the JARs
rm -f "$MANIFEST_FILE"

echo "Build completed successfully. JAR files and keystore/truststore directories are in the $DIST_DIR directory."
