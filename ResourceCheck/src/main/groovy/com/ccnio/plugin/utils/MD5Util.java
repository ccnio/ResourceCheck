package com.ccnio.plugin.utils;


import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public class MD5Util {

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        if (bytes == null || bytes.length <= 0) {
            return null;
        }

        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() < 2) {
                sb.append(0);
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public static String getFileMD5(File file) {
        return bytesToHexString(getFileMD5Bytes(file));
    }

    public static byte[] getFileMD5Bytes(File file) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest;
        FileInputStream in;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return digest.digest();
    }
}
