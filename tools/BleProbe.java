import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.*;
import android.os.*;
import java.util.concurrent.*;

/** Bounded BLE advertiser; no pairing service and no repeated popup cycling. */
public class BleProbe {
  public static void main(String[] args) throws Exception {
    Looper.prepareMainLooper();
    BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
    if (bt == null) throw new IllegalStateException("No Bluetooth adapter");
    if (args.length > 0 && args[0].equals("off")) {
      System.out.println("Bluetooth disable: " + bt.disable()); return;
    }
    boolean originallyOn = bt.isEnabled();
    boolean enabledByUs = false;
    BluetoothLeAdvertiser advertiser = null;
    AdvertiseCallback callback = null;
    try {
      System.out.println("Initially enabled: " + originallyOn);
      if (!originallyOn) {
        enabledByUs = bt.enable();
        if (!enabledByUs) throw new IllegalStateException("Bluetooth enable rejected");
        long deadline = System.currentTimeMillis() + 15000;
        while (!bt.isEnabled() && System.currentTimeMillis() < deadline) Thread.sleep(200);
      }
      if (!bt.isEnabled()) throw new IllegalStateException("Bluetooth did not enable");
      System.out.println("Multiple advertisement supported: " + bt.isMultipleAdvertisementSupported());
      System.out.println("LE extended advertising supported: " + bt.isLeExtendedAdvertisingSupported());
      System.out.println("LE advertising maximum data length: " + bt.getLeMaximumAdvertisingDataLength());
      if (args.length == 0 || args[0].equals("check")) return;
      int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 10;
      if (seconds < 1 || seconds > 60) throw new IllegalArgumentException("Duration must be 1..60 seconds");
      AdvertiseData.Builder data = new AdvertiseData.Builder().setIncludeDeviceName(false);
      if (args[0].equals("ordinary")) {
        data.addServiceUuid(ParcelUuid.fromString("8c41eb50-9ef9-4e6c-a079-5d05001e5630"));
      } else if (args[0].matches("[0-9a-fA-F]{6}")) {
        int id = Integer.parseInt(args[0], 16);
        data.addServiceData(ParcelUuid.fromString("0000fe2c-0000-1000-8000-00805f9b34fb"),
            new byte[]{(byte)(id >> 16), (byte)(id >> 8), (byte)id});
        data.setIncludeTxPowerLevel(true);
        System.out.println("Fast Pair discovery model ID: " + args[0]);
      } else throw new IllegalArgumentException("Use check, ordinary, or a six-digit model ID");
      advertiser = bt.getBluetoothLeAdvertiser();
      if (advertiser == null) throw new IllegalStateException("No advertiser available");
      CountDownLatch started = new CountDownLatch(1);
      int[] result = {-1};
      callback = new AdvertiseCallback() {
        public void onStartSuccess(AdvertiseSettings settings) {
          result[0] = 0; System.out.println("ADVERTISEMENT_STARTED " + settings); started.countDown();
        }
        public void onStartFailure(int error) {
          result[0] = error; System.out.println("ADVERTISEMENT_FAILED code=" + error); started.countDown();
        }
      };
      AdvertiseSettings settings = new AdvertiseSettings.Builder()
          .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
          .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
          .setConnectable(true).setTimeout(seconds * 1000).build();
      advertiser.startAdvertising(settings, data.build(), callback);
      // Callback delivery requires the main looper, so end the loop on a worker.
      final int duration = seconds;
      final BluetoothLeAdvertiser adv = advertiser;
      final AdvertiseCallback cb = callback;
      final boolean restoreOff = enabledByUs;
      new Thread(() -> {
        try {
          if (!started.await(8, TimeUnit.SECONDS)) System.out.println("Start callback timed out");
          else if (result[0] == 0) Thread.sleep(duration * 1000L);
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        finally {
          try {
            adv.stopAdvertising(cb);
            System.out.println("Advertisement stopped");
            if (restoreOff) System.out.println("Restoring Bluetooth to off: " + bt.disable());
          } finally { System.exit(result[0] == 0 ? 0 : 1); }
        }
      }).start();
      Looper.loop();
      if (result[0] != 0) throw new IllegalStateException("Advertising unsuccessful: " + result[0]);
    } finally {
      if (advertiser != null && callback != null) {
        advertiser.stopAdvertising(callback);
        System.out.println("Advertisement stopped");
      }
      if (enabledByUs) {
        System.out.println("Restoring Bluetooth to off: " + bt.disable());
      }
    }
  }
}
