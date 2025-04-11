# Gamepad to ESP32 Controller

An Android app that reads USB gamepad input and transmits formatted joystick data to an ESP32 via Bluetooth Classic.

## Features

- Connects to ESP32 via Bluetooth Classic (SPP)
- Reads input from USB gamepads connected to Android phone
- Transmits formatted joystick data: `R:<lx> A:<rx> E:<-ry>`
- Value range: -100 to 100
- Visual joystick position display
- Automatic reconnection
- Debug information display
- Material Design UI with card-based layout

## Screenshots

(Screenshots will be added after building the app)

## Getting the APK

### Option 1: Export from Replit and Build Locally

1. Click on the three-dot menu in the top-right corner of this Replit
2. Select "Download as ZIP"
3. Extract the ZIP file on your computer
4. Open the extracted folder and run the `build-apk.sh` script if you have the Android SDK installed
   ```
   chmod +x build-apk.sh
   ./build-apk.sh
   ```

### Option 2: Build in Android Studio

1. Download the ZIP as described above
2. Open the project in Android Studio
3. Let Gradle sync the project
4. Connect your Android device with USB debugging enabled
5. Click "Run" to install the app on your device

Alternatively, build an APK directly in Android Studio:
1. Select "Build" -> "Build Bundle(s) / APK(s)" -> "Build APK(s)"
2. Find the APK in `app/build/outputs/apk/debug/app-debug.apk`

## Usage Instructions

1. Pair your ESP32 device with your Android phone in the Bluetooth settings
2. Connect a USB gamepad to your Android phone using an OTG adapter if needed
3. Launch the app
4. Press the "Connect" button to establish a connection with the ESP32
5. Move the joysticks to transmit data

## Data Format

The app transmits data in the following format:

```
R:<lx> A:<rx> E:<-ry>
```

Where:
- `lx` is the left joystick X-axis value (-100 to 100)
- `rx` is the right joystick X-axis value (-100 to 100)
- `-ry` is the inverted right joystick Y-axis value (-100 to 100)

Example: `R:15 A:-8 E:20`

## ESP32 Setup

On your ESP32, use the Serial Port Profile (SPP) to receive the data. Example Arduino code to read the data:

```cpp
#include "BluetoothSerial.h"

BluetoothSerial SerialBT;
String receivedString = "";

void setup() {
  Serial.begin(115200);
  SerialBT.begin("ESP32"); // Name your ESP32 (should include "ESP32" for auto-discovery)
  Serial.println("ESP32 Bluetooth ready to receive gamepad data");
}

void loop() {
  // Check if data is received
  if (SerialBT.available()) {
    receivedString = SerialBT.readStringUntil('\n');
    Serial.print("Received: ");
    Serial.println(receivedString);
    
    // Process the received data (parse R, A, E values)
    parseCommands(receivedString);
  }
  delay(20);
}

void parseCommands(String command) {
  int roll = 0;
  int aileron = 0;
  int elevator = 0;
  
  // Parse roll (R:)
  int rIndex = command.indexOf("R:");
  if (rIndex >= 0) {
    int spaceIndex = command.indexOf(" ", rIndex);
    if (spaceIndex > rIndex) {
      roll = command.substring(rIndex + 2, spaceIndex).toInt();
    }
  }
  
  // Parse aileron (A:)
  int aIndex = command.indexOf("A:");
  if (aIndex >= 0) {
    int spaceIndex = command.indexOf(" ", aIndex);
    if (spaceIndex > aIndex) {
      aileron = command.substring(aIndex + 2, spaceIndex).toInt();
    }
  }
  
  // Parse elevator (E:)
  int eIndex = command.indexOf("E:");
  if (eIndex >= 0) {
    elevator = command.substring(eIndex + 2).toInt();
  }
  
  // Now use these values to control your device
  Serial.printf("Roll: %d, Aileron: %d, Elevator: %d\n", roll, aileron, elevator);
}
```

## License

MIT License

## Credits

Created by gamepadbt for remote control applications.