package ph.edu.addu.richardleosala.piggyback.Routing;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.widget.Toast;

public class CheckWifi {
    WifiManager wifiManager;
    public boolean checked(){
        if(wifiManager.isWifiEnabled() == true){
            return true;
        }else{
            return false;
        }
    }
    public void turnOnWifi(){
        wifiManager.setWifiEnabled(true);
    }
}
