# Samsung M30 BLE experiment

Device: SM-M305F / m30lte, Android 10, rooted with Magisk.
Radio: Samsung S612 EVT1.0, RF version 0x10b1.

Confirmed using Android BluetoothAdapter and BluetoothLeAdvertiser APIs:

- Multiple advertisement support: true.
- Extended advertising: false.
- Maximum advertising data length: 31 bytes.
- A connectable legacy advertisement started successfully; GattService recorded its bounded duration and stop.

The helper runs through app_process, without installing an APK. It uses Android's Bluetooth service, not direct access to the Samsung H4 device. Bluetooth is temporarily enabled if necessary and restored to off afterward. Advertising has a controller timeout of at most 60 seconds. There is no GATT Fast Pair service, pairing, audio, address rotation, or repeated broadcast loop.

Initial Fast Pair discovery experiment: model ID 000006, 30 seconds, low transmit power, service data UUID FE2C, TX power included. Android reported advertising success and a 30-second run. The user reported no popup on the receiving Nothing phone. Reception and receiver-side filtering have not been independently verified. This does not establish that Fast Pair discovery is impossible.

Run one ordinary BLE test:

```sh
./tools/run-ble-test.sh ordinary 10
```

Run one discovery test with a chosen model ID:

```sh
./tools/run-ble-test.sh 000006 30
```

The latter advertises an existing product identity for a controlled experiment; it does not make the M30 a working version of that product. A popup is not guaranteed.

Build prerequisites: JDK, d8, Android 29 android.jar (currently in .build/ble).

```sh
javac --release 8 -Xlint:-options -cp .build/ble/android.jar -d .build/ble tools/BleProbe.java
d8 --min-api 29 --lib .build/ble/android.jar --output .build/ble/ble-probe.jar .build/ble/BleProbe.class '.build/ble/BleProbe$1.class'
```

Receiver prerequisites: Bluetooth, Location, internet, and Google settings > All services > Devices > Scan for nearby devices enabled; keep it unlocked and close to the M30. Paths vary across Android versions.

## Successful receiver test

Nothing Phone (3a), A059, Android 16, connected over ADB. Bluetooth was initially off and the screen was locked. After enabling Bluetooth and having the user unlock it, the same model ID 000006 broadcast triggered Google Play services Fast Pair HalfSheetActivity. A screenshot confirms the system UI displayed “Google Pixel Buds” with Close and Connect buttons. No pairing was attempted. Receiver logs and screenshot are in .build/ble/logs. Temporary verbose log properties were restored.

The shell launcher targets the M30 explicitly, so connecting the receiver simultaneously does not make ADB device selection ambiguous. Override M30_SERIAL if using a different M30.
