# App Architecture

This document outlines the architecture of the Gamepad to ESP32 Controller app.

## Component Diagram

```
                        +------------------+
                        |   MainActivity   |
                        |------------------|
                        | - UI Management  |
                        | - Event Handling |
                        | - Data Scheduler |
                        +--------+---------+
                                 |
                   +-------------+-------------+
                   |                           |
        +----------v-----------+    +---------v-----------+
        |   GamepadHandler     |    |   BluetoothService  |
        |----------------------|    |---------------------|
        | - Joystick Reading   |    | - BT Connection     |
        | - Input Processing   |    | - Data Transmission |
        | - Value Scaling      |    | - Auto-reconnection |
        +----------------------+    +---------------------+
                   |                           |
                   v                           v
          +------------------+       +-------------------+
          | USB Gamepad Input|       | ESP32 Device      |
          +------------------+       +-------------------+
```

## Data Flow

```
+-------------+     +---------------+     +----------------+     +-------------+
| USB Gamepad | --> | GamepadHandler| --> | MainActivity   | --> | Bluetooth   |
| Input       |     | (-100 to 100) |     | Format String  |     | Service     |
+-------------+     +---------------+     +----------------+     +-------------+
                                                                       |
                                                                       v
                                                              +------------------+
                                                              | ESP32 (Receiver) |
                                                              +------------------+
```

## Key Classes

### MainActivity
- Handles UI interactions
- Manages permissions
- Coordinates data flow
- Schedules periodic data transmission (20Hz)

### GamepadHandler
- Processes joystick input events
- Applies deadzone filtering
- Scales values to -100 to 100 range
- Provides joystick position data

### BluetoothService
- Manages Bluetooth connections
- Implements Serial Port Profile (SPP)
- Handles data transmission
- Provides connection status callbacks
- Supports auto-reconnection

## UI Components

```
+-----------------------------------------------+
|                                               |
|  +-------------------+                        |
|  | Connection Status |                        |
|  +-------------------+                        |
|                                               |
|  +-------------------+  +-------------------+ |
|  | Left Joystick     |  | Right Joystick    | |
|  | Visualization     |  | Visualization     | |
|  +-------------------+  +-------------------+ |
|                                               |
|  +-----------------------------------+        |
|  | Transmission Data                 |        |
|  | R:<lx> A:<rx> E:<-ry>            |        |
|  +-----------------------------------+        |
|                                               |
|  +-----------------------------------+        |
|  | Instructions                      |        |
|  +-----------------------------------+        |
|                                               |
+-----------------------------------------------+
```

## Permissions Required

- `BLUETOOTH` - For basic Bluetooth functionality
- `BLUETOOTH_ADMIN` - For Bluetooth device discovery
- `BLUETOOTH_CONNECT` - For connecting to devices (Android 12+)
- `BLUETOOTH_SCAN` - For scanning devices (Android 12+)
- `ACCESS_FINE_LOCATION` - Required for Bluetooth scanning on older Android versions