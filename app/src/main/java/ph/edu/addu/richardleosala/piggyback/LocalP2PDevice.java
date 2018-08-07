package ph.edu.addu.richardleosala.piggyback;

import android.net.wifi.p2p.WifiP2pDevice;

public class LocalP2PDevice {
    private WifiP2pDevice localDevice;

    private static final LocalP2PDevice instance = new LocalP2PDevice();

    /**
     * Method to get the instance of this class.
     * @return instance of this class.
     */
    public static LocalP2PDevice getInstance() {
        return instance;
    }

    /**
     * Private constructor, because is a singleton class.
     */
    private LocalP2PDevice(){
        localDevice = new WifiP2pDevice();
    }
}
