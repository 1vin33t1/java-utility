package com.xyz.utility.common.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.StringJoiner;

@Slf4j
@UtilityClass
public class HashUtil {

    public static String sha512(List<String> params, String delimiter) {
        StringJoiner input = new StringJoiner(delimiter);
        params.forEach(input::add);
        String encrypted = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.reset();
            digest.update(input.toString().getBytes(StandardCharsets.UTF_8));
            encrypted = String.format("%0128x", new BigInteger(1, digest.digest()));
        } catch (Exception e) {
            log.error("Error while generating hash", e);
        }
        return encrypted;
    }
}
