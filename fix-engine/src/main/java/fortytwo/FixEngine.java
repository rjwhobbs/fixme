package fortytwo;

import fortytwo.constants.FixConstants;
import fortytwo.fixexceptions.FixFormatException;

import java.util.ArrayList;
import java.util.List;

public class FixEngine {

//  Byte string will contain the SOH char
  private byte[] rawFixMessageBytes;
//  String object will contain our printable delimiter
  private String fixMessageString;

  private String internalRouterlID = null;
  private String msgType = null;
  private String senderCompID = null;
  private String targetCompID = null;

  private List<String> tags = new ArrayList<>();
  private List<String> values = new ArrayList<>();

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

  public void parseRawBytes() {
    int i = 0;
    int len = this.rawFixMessageBytes.length;
    String tempTag;
    String tempValue;

    while (i < len) {
      tempTag = "";
      tempValue = "";
      while (this.rawFixMessageBytes[i] != FixConstants.SOHDelimiter) {
        while (this.rawFixMessageBytes[i] != '=') {
          tempTag = tempTag + (char)this.rawFixMessageBytes[i];
          i++;
        }
        if (this.rawFixMessageBytes[i] == '=') {
          i++;
          while (this.rawFixMessageBytes[i] != FixConstants.SOHDelimiter) {
            tempValue = tempValue + (char)this.rawFixMessageBytes[i];
            i++;
          }
        }
      }
      tags.add(tempTag);
      values.add(tempValue);
      if (i < len) {
        i++;
      }
    }
  }

  public void parseTagValueLists() throws FixFormatException {

    if ((tags.size() == 0 || values.size() == 0) || (tags.size() != values.size())) {
      throw new FixFormatException("One or more tag value pairs are missing.");
    }
    if (!tags.get(0).equals(FixConstants.internalRouterIDTag)) {
      throw new FixFormatException("FIX message must start with the internal router ID.");
    }

    for (int i = 0; i < tags.size(); i++) {
      if(tags.get(i).isEmpty() || values.get(i).isEmpty()) {
        throw new FixFormatException("One or more tag value pairs are missing.");
      }
    }

  }

  public void checkFixFormat() throws FixFormatException {
    // This is checking if the byte string ends with a SOH char
    if (rawFixMessageBytes[rawFixMessageBytes.length - 1] != FixConstants.SOHDelimiter) {
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
      if (arr[i] == FixConstants.SOHDelimiter) {
        arr[i] = FixConstants.printableDelimiter;
      }
    }

    return arr;
  }

  public static byte[] insertSOHDelimiter(byte[] arr) {

    for (int i = 0; i < arr.length; i++) {
      if (arr[i] == FixConstants.printableDelimiter) {
        arr[i] = FixConstants.SOHDelimiter;
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
                    + FixConstants.checkSumTag + "="
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