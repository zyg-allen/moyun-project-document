package com.moyun.util.security;

public class DesensitizedUtil
{
    public static String password(String password)
    {
        if (com.moyun.util.string.StringUtils.isBlank(password))
        {
            return com.moyun.util.string.StringUtils.EMPTY;
        }
        return com.moyun.util.string.StringUtils.repeat('*', password.length());
    }

    public static String carLicense(String carLicense)
    {
        if (com.moyun.util.string.StringUtils.isBlank(carLicense))
        {
            return com.moyun.util.string.StringUtils.EMPTY;
        }
        if (carLicense.length() == 7)
        {
            carLicense = com.moyun.util.string.StringUtils.hide(carLicense, 3, 6);
        }
        else if (carLicense.length() == 8)
        {
            carLicense = com.moyun.util.string.StringUtils.hide(carLicense, 3, 7);
        }
        return carLicense;
    }
}
