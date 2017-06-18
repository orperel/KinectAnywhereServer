package org.kinectanywhereandroid.network;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class Utils {
    static public String getIpAddress() throws SocketException {

        String ip = "";

        Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (enumNetworkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
            Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
            while (enumInetAddress.hasMoreElements()) {
                InetAddress inetAddress = enumInetAddress.nextElement();

                if (inetAddress.isSiteLocalAddress()) {
                    return inetAddress.getHostAddress();
                }

            }

        }

//        For debug with Genymotion emulator put your IP here:
//        return "192.168.1.106";

        return ip;
    }

    static public String getBroadcastingAddress() throws SocketException {
        String ip = getIpAddress();

        int indexOfLastDot = ip.lastIndexOf(".");
        String broadcastIPAddress = ip.substring(0, indexOfLastDot + 1) + "255";

        return broadcastIPAddress;
    }
}
