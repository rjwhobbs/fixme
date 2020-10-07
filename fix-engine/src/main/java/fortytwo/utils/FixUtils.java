package fortytwo.utils;

import fortytwo.constants.FixConstants;

public class FixUtils {
  public static byte[] insertSOHDelimiter(byte[] arr) {
    for (int i = 0; i < arr.length; i++) {
      if (arr[i] == FixConstants.printableDelimiter) {
        arr[i] = FixConstants.SOHDelimiter;
      }
    }
    return arr;
  }

  public static byte[] insertPrintableDelimiter(byte[] arr) {
    for (int i = 0; i < arr.length; i++) {
      if (arr[i] == FixConstants.SOHDelimiter) {
        arr[i] = FixConstants.printableDelimiter;
      }
    }
    return arr;
  }

  public static int byteSum(byte[] arr) {
    int sum = 0;

    for (int i = 0; i < arr.length; i++) {
      sum += arr[i];
    }
    return sum;
  }

  public static String createCheckSumString(byte[] message) {
    String checkSumString;

    checkSumString = Integer.toString(createCheckSum(message));

    switch (checkSumString.length()) {
      case 1:
        checkSumString = "00" + checkSumString;
        break;
      case 2:
        checkSumString = "0" + checkSumString;
        break;
      default:
        break;
    }
    return checkSumString;
  }

  private static int createCheckSum(byte[] arr) {
    int summedBytes = byteSum(arr);
    return summedBytes % 256;
  }
}
