package com.zagvladimir.fraerish.image;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;

final class PublicHostPolicy {

    private PublicHostPolicy() {
    }

    static boolean allows(URI uri) {
        if (uri.getHost() == null) {
            return false;
        }
        try {
            return Arrays.stream(InetAddress.getAllByName(uri.getHost())).noneMatch(PublicHostPolicy::isInternal);
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private static boolean isInternal(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isUniqueLocalIpv6(address);
    }

    private static boolean isUniqueLocalIpv6(InetAddress address) {
        return address instanceof Inet6Address && (address.getAddress()[0] & 0xFE) == 0xFC;
    }
}