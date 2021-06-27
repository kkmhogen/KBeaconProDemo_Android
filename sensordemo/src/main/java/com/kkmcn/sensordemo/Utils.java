/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kkmcn.sensordemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class Utils {
    @TargetApi(Build.VERSION_CODES.M)
    public static boolean isLocationBluePermission(final Context context) {
        if (!Utils.isMPhone()) {
            return true;
        } else {
            boolean result = true;
            if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                result = false;
            }
            return result;
        }
    }

    public static boolean isPositiveInteger(String strNumber)
    {
        String pattern = "^\\+?[1-9][0-9]*$";
        return Pattern.matches(pattern, strNumber);
    }

    public static boolean isMinusInteger(String strNumber)
    {
        String pattern = "^((-\\d+)|(0+))$";
        return Pattern.matches(pattern, strNumber);
    }

    public static boolean isMPhone() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static byte[] hexStringToBytes(String hexString){
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        char []hexCharacter = hexString.toCharArray();
        for (int i = 0; i < hexCharacter.length; i++){
            if (-1 == charToByte(hexCharacter[i])){
                return null;
            }
        }

        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));

        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }
    
    public static String bytesToHexString(byte[] src){  
        StringBuilder stringBuilder = new StringBuilder("");  
        if (src == null || src.length <= 0) {  
            return null;  
        }  
        for (int i = 0; i < src.length; i++) {  
            int v = src[i] & 0xFF;  
            String hv = Integer.toHexString(v);  
            if (hv.length() < 2) {  
                stringBuilder.append(0);  
            }  
            stringBuilder.append(hv);  
        }  
        return stringBuilder.toString();  
    }

    public static String macAddressAddOne(String adres) {
        String address = adres;
        String[] addressArr = address.split(":");
        String strHex = addressArr[5];
        int l = strHex.length() / 2;
        byte[] ret = new byte[l];
        for (int i = 0; i < l; i++) {
            ret[i] = (byte) Integer
                    .valueOf(strHex.substring(i * 2, i * 2 + 2), 16).byteValue();
        }
        if (ret[0] == -1) {
            ret[0] = 0;
        } else {
            ret[0] += 1;
        }
        String last = bytesToHexString(ret);
        String newAddress = addressArr[0] + ":" + addressArr[1] + ":" + addressArr[2] + ":" + addressArr[3] + ":" + addressArr[4] + ":" + last;
        return newAddress;
    }

    public static String FormatHexUUID2User(String strUUID)
    {
        if (strUUID.length() != 32 && strUUID.length() != 34)
        {
            return "";
        }
        if (strUUID.length() == 34)
        {
            strUUID = strUUID.toLowerCase();
            strUUID = strUUID.replace("0x", "");
        }

        String strUserUUID;
        strUserUUID = strUUID.substring(0, 8);
        strUserUUID += "-";

        strUserUUID += strUUID.substring(8, 12);
        strUserUUID += "-";

        strUserUUID += strUUID.substring(12, 16);
        strUserUUID += "-";

        strUserUUID += strUUID.substring(16, 20);
        strUserUUID += "-";

        strUserUUID += strUUID.substring(20);

        return strUserUUID;
    }

    public static String ReadTxtFile(File file)
    {
        StringBuilder content = new StringBuilder();

        try {
            InputStream instream = new FileInputStream(file);
            InputStreamReader inputreader = new InputStreamReader(instream);
            BufferedReader buffreader = new BufferedReader(inputreader);
            String line;
            //分行读取
            while (( line = buffreader.readLine()) != null) {
                content.append(line);
            }
            instream.close();
        }
        catch (java.io.FileNotFoundException e)
        {
            return null;
        }
        catch (IOException e)
        {
            return null;
        }

        return content.toString();
    }
}
