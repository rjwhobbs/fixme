package fortytwo.utils;

import fortytwo.constants.FixConstants;
import fortytwo.fixexceptions.FixCheckSumException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Random;

public class FixUtils {

  private static final Random rand = new Random();
  private static final Pattern checkSumPattern = Pattern.compile("\\|(10=(\\d{3})\\|)$");

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

  public static void valCheckSum(String message) throws FixCheckSumException {
    Matcher m = checkSumPattern.matcher(message);
    String strippedMessage;
    String strToRemove;
    String checkSumString;
    int checkSumVal;

    if (m.find()) {
      strToRemove = m.group(1);
      checkSumString = m.group(2);
      checkSumVal = Integer.parseInt(checkSumString);
      strippedMessage = message.replace(strToRemove, "");
      if (checkSumVal == createCheckSum(insertSOHDelimiter(strippedMessage.getBytes()))) {
        return ;
      }
      else {
        throw new FixCheckSumException(FixCheckSumException.checkSumIncorrect);
      }
    }
    throw new FixCheckSumException(FixCheckSumException.checkSumMissing);
  }


  public static String createUniqueID() {
    int randNum = rand.nextInt(100000);
    long timeStamp = System.currentTimeMillis();

    return Integer.toHexString(randNum) + "_" + Long.toString(timeStamp);
  }

  public static String buySellTemplate(
          String side,
          String internalSenderID,
          String internalTargetID,
          String symbol,
          String quantity,
          String price
  ) {
    return FixConstants.internalSenderIDTag + "=" + internalSenderID + FixConstants.printableDelimiter
            + FixConstants.internalTargetIDTag + "=" + internalTargetID + FixConstants.printableDelimiter
            + FixConstants.msgTypeTag + "=" + FixConstants.ORDER_SINGLE + FixConstants.printableDelimiter
            + FixConstants.sideTag + "=" + side + FixConstants.printableDelimiter
            + FixConstants.symbolTag + "=" + symbol + FixConstants.printableDelimiter
            + FixConstants.priceTag + "=" + price + FixConstants.printableDelimiter
            + FixConstants.orderQtyTag + "=" + quantity + FixConstants.printableDelimiter
            + FixConstants.clientOrdIDTag + "=" + FixUtils.createUniqueID() + FixConstants.printableDelimiter;
  }

  private static int createCheckSum(byte[] arr) {
    int summedBytes = byteSum(arr);
    return summedBytes % 256;
  }
}
