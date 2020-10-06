package fortytwo;

import fortytwo.constants.FixConstants;
import fortytwo.fixexceptions.FixFormatException;
import fortytwo.utils.FixUtils;

import java.util.ArrayList;
import java.util.List;

public class FixMessage {

//  Byte string will contain the SOH char
  private byte[] rawFixMessageBytes;
//  String object will contain our printable delimiter
  private String fixMessageString;

  private String internalRouterID = null;
  private String msgType = null;
  private String senderCompID = null;
  private String targetCompID = null;
  private String side = null;
  private String sideValue = null;
  private String symbol = null;
  private String symbolValue = null;

  private List<String> tags = new ArrayList<>();
  private List<String> values = new ArrayList<>();

  public FixMessage(String message) {
    this.rawFixMessageBytes = FixUtils.insertSOHDelimiter(message.getBytes());
    this.fixMessageString = message;
  }

//  Use this constructor when you are getting a raw byte string from the buffer,
//  it is assumed that it should contain the SOH delimiter.
//  It is also assumed that it will contain the checkSum as it is coming from a buffer read().
  public FixMessage(byte[] message) {
    this.rawFixMessageBytes = message;
    this.fixMessageString = new String(FixUtils.insertPrintableDelimiter(message));
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
        while (this.rawFixMessageBytes[i] != '=' && this.rawFixMessageBytes[i] != FixConstants.SOHDelimiter) {
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
    if (!tags.get(0).equals(FixConstants.internalSenderIDTag)) {
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

  public void appendCheckSumToString() {
    this.fixMessageString =
            this.fixMessageString
                    + FixConstants.checkSumTag + "="
                    + FixUtils.createCheckSumString(this.rawFixMessageBytes)
                    + FixConstants.printableDelimiter;
  }

  public void appendCheckSumToBytes() {

    byte[] tempByteArr = new byte[rawFixMessageBytes.length + 7];
    byte[] checkSumTag = (FixConstants.checkSumTag + "=").getBytes();
    byte[] checkSumBytes = FixUtils.createCheckSumString(rawFixMessageBytes).getBytes();

    System.arraycopy(rawFixMessageBytes, 0, tempByteArr, 0, rawFixMessageBytes.length);
    System.arraycopy(checkSumTag, 0, tempByteArr, rawFixMessageBytes.length, 3);
    System.arraycopy(checkSumBytes, 0, tempByteArr, rawFixMessageBytes.length + 3, 3);
    tempByteArr[tempByteArr.length - 1] = FixConstants.SOHDelimiter;

    this.rawFixMessageBytes = tempByteArr;
  }

  public byte[] getRawFixMessageBytes() {
    return rawFixMessageBytes;
  }

  public String getFixMessageString() {
    return fixMessageString;
  }

  public void printRawBytes() {
  }
}

class TestEngine {
  public static void main(String[] args) {
    FixMessage fm0 = new FixMessage("24242=1|35=V|");
    // Test cases without '=' char
    FixMessage fm1 = new FixMessage("24242=1|35|");
    FixMessage fm2 = new FixMessage("24242|35=V|");


    try {
      System.out.println(fm0.getFixMessageString());
      fm0.parseRawBytes();
//      fm0.appendCheckSumToString();
      fm0.appendCheckSumToBytes();

//      System.out.println(fm0.getFixMessageString());
      fm0.checkFixFormat();
    }
    catch (FixFormatException e) {
      System.out.println(e);
    }

    try {
      System.out.println(fm1.getFixMessageString());
      fm1.parseRawBytes();
      fm1.parseTagValueLists();
      fm1.appendCheckSumToString();
      System.out.println(fm0.getFixMessageString());
      fm1.checkFixFormat();
    }
    catch (FixFormatException e) {
      System.out.println(e);
    }

    try {
      System.out.println(fm2.getFixMessageString());
      fm2.parseRawBytes();
      fm2.parseTagValueLists();
      fm2.appendCheckSumToString();
      System.out.println(fm0.getFixMessageString());
      fm2.checkFixFormat();
    }
    catch (FixFormatException e) {
      System.out.println(e);
    }
  }
}