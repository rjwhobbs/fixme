package fortytwo;

import fortytwo.constants.FixConstants;
import fortytwo.fixexceptions.FixFormatException;

public class FixEngine {

//  Byte string will contain the SOH char
  private byte[] rawFixMessageBytes;
//  String object will contain our printable delimiter
  private String fixMessageString;

  public String routerInternalID;
  public String messageType;
  public String senderCompID;
  public String targetCompID;

  public FixEngine(String message) {
    this.rawFixMessageBytes = insertSOHDelimiter(message.getBytes());
    this.fixMessageString = message;
  }

//  Use this constructor when you are getting a raw byte string from the buffer,
//  it is assumed that it should contain the SOH delimiter.
//  It is also assumed that it will contain the checkSum as it is coming from a buffer read().
  public FixEngine(byte[] message) {
    this.rawFixMessageBytes = message;
    this.fixMessageString = new String(insertPrintableDelimiter(message));
  }

  public void parseRawBytes() throws FixFormatException {
    int i = 0;
    int len = this.rawFixMessageBytes.length;
    int tagsParsed = 0;
    String tempTag;
    String tempValue;

    while (i < len) {
      tempTag = "";
      tempValue = "";
      while (this.rawFixMessageBytes[i] != 1) {
        while (this.rawFixMessageBytes[i] != '=') {
          tempTag = tempTag + (char)this.rawFixMessageBytes[i];
          i++;
        }
        if (this.rawFixMessageBytes[i] == '=') {
          i++;
          while (this.rawFixMessageBytes[i] != 1) {
            tempValue = tempValue + (char)this.rawFixMessageBytes[i];
            i++;
          }
        }
        else {
          throw new FixFormatException("FIX message is missing value on tag: " + tempTag);
        }
      }
      System.out.println(tempTag);
      System.out.println(tempValue);
      if (i < len) {
        i++;
      }
    }
  }

  public void checkFixFormat() throws FixFormatException {
    // This is checking if the byte string ends with a SOH char
    if (rawFixMessageBytes[rawFixMessageBytes.length - 1] != 1) {
      throw new FixFormatException("Error in FIX message format, one or more delimiters is missing");
    }
  }

  public static int sum(byte[] arr) {
    int sum = 0;

    for (int i = 0; i < arr.length; i++) {
      sum += arr[i];
    }

    return sum;
  }

  public static byte[] insertPrintableDelimiter(byte[] arr) {
    for (int i = 0; i < arr.length; i++) {
      if (arr[i] == 1) {
        arr[i] = FixConstants.printableDelimiter;
      }
    }

    return arr;
  }

  public static byte[] insertSOHDelimiter(byte[] arr) {

    for (int i = 0; i < arr.length; i++) {
      if (arr[i] == FixConstants.printableDelimiter) {
        arr[i] = 1;
      }
    }
    return arr;
  }

  public String createCheckSumString(String message) {
    String checkSumString;
    int checkSum;
    int originalTotal;

    originalTotal = this.sum(this.insertSOHDelimiter(message.getBytes()));
    checkSum = originalTotal % 256;
    checkSumString = Integer.toString(checkSum);

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

  public void appendCheckSumToString() {
    this.fixMessageString =
            this.fixMessageString
                    + FixConstants.checkSum + "="
                    + createCheckSumString(this.fixMessageString)
                    + FixConstants.printableDelimiter;
  }

  public byte[] getRawFixMessageBytes() {
    return rawFixMessageBytes;
  }

  public String getFixMessageString() {
    return fixMessageString;
  }

  public void printRawBytes() {
  }

  public static void testOne() {
    System.out.println("Hello form engine test one");
  }

  public void testTwo() {
    System.out.println("Hello form engine test two");
  }
}

class TestEngine {
  public static void main(String[] args) {
    FixEngine fe = new FixEngine("24242=1|35=V|");

    try {
      System.out.println(fe.getFixMessageString());
      fe.parseRawBytes();
      fe.appendCheckSumToString();
      System.out.println(fe.getFixMessageString());
      fe.checkFixFormat();
    }
    catch (FixFormatException e) {
      System.out.println(e);
    }

  }
}