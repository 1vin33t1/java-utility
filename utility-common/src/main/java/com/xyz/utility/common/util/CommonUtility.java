package com.xyz.utility.common.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.StringJoiner;
import java.util.UUID;

@Slf4j
@UtilityClass
public class CommonUtility {

    public String generateId(String... mix) {
        StringJoiner joiner = new StringJoiner(":");
        for (String each : mix) {
            joiner.add(each);
        }
        joiner.add(UUID.randomUUID().toString());
        return joiner.toString();
    }

    public String getHostIp() {
        String ip = "";
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(new InetSocketAddress("github.com", 80));
            ip = socket.getLocalAddress().getHostAddress();
        } catch (SocketException e) {
            log.error("error while fetching ip", e);
        }
        return ip;
    }

}
