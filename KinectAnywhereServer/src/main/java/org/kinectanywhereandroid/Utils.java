package org.kinectanywhereandroid;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class Utils {
    static public String getIpAddress() throws SocketException {
        // TODO: just for genymotion...
//        String ip = "";
//
//        Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
//        while (enumNetworkInterfaces.hasMoreElements()) {
//            NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
//            Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
//            while (enumInetAddress.hasMoreElements()) {
//                InetAddress inetAddress = enumInetAddress.nextElement();
//
//                if (inetAddress.isSiteLocalAddress()) {
//                    return inetAddress.getHostAddress();
//                }
//
//            }
//
//        }
//
//        return ip;

        return "192.168.1.106";
    }

    static public String getBroadcastingAddress() throws SocketException {
        String ip = getIpAddress();

        int indexOfLastDot = ip.lastIndexOf(".");
        String broadcastIPAddress = ip.substring(0, indexOfLastDot + 1) + "255";

        return broadcastIPAddress;
    }
}
