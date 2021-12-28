/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 simileas.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.nopadding.banana;

import java.util.regex.Pattern;

public class RegexUtil {

  private static final Pattern PATTERN = Pattern.compile(
      "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

  private static final Pattern IPV6_STD_PATTERN = Pattern.compile(
      "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");

  private static final Pattern IPV6_HEX_COMPRESSED_PATTERN = Pattern.compile(
      "^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::"
          + "((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$");

  public static boolean isIpv4Address(final String ip) {
    return PATTERN.matcher(ip).matches();
  }

  public static boolean isIPv6StdAddress(final String input) {
    return IPV6_STD_PATTERN.matcher(input).matches();
  }

  public static boolean isIPv6HexCompressedAddress(final String input) {
    return IPV6_HEX_COMPRESSED_PATTERN.matcher(input).matches();
  }

  public static boolean isIpv6Address(final String input) {
    return isIPv6StdAddress(input) || isIPv6HexCompressedAddress(input);
  }
}
