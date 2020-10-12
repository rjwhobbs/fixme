package fortytwo;

import fortytwo.constants.FixConstants;
import fortytwo.fixexceptions.FixCheckSumException;
import fortytwo.fixexceptions.FixFormatException;
import fortytwo.fixexceptions.FixMessageException;
import fortytwo.utils.FixUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FixMessage {

//  Byte string will contain the SOH char
  private byte[] rawFixMessageBytes;
//  String object will contain our printable delimiter
  private String fixMessageString;
  public HashMap<String, String> msgMap = new HashMap<>();
  private List<String> tags = new ArrayList<>();
  private List<String> values = new ArrayList<>();

  FixMessage(String message) {
    this.rawFixMessageBytes = FixUtils.insertSOHDelimiter(message.getBytes());
    this.fixMessageString = message;
  }

//  Use this constructor when you are getting a raw byte string from the buffer,
//  it is assumed that it should contain the SOH delimiter.
//  It is also assumed that it will contain the checkSum as it is coming from a buffer read().
  FixMessage(byte[] message) {
    this.rawFixMessageBytes = message;
    this.fixMessageString = new String(FixUtils.insertPrintableDelimiter(message));
  }

  void parseRawBytes() {
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

  void parseTagValueLists() throws FixFormatException {

    if ((tags.size() == 0 || values.size() == 0) || (tags.size() != values.size())) {
      throw new FixFormatException(FixFormatException.missingTagValue);
    }
    if (!tags.get(0).equals(FixConstants.internalSenderIDTag)) {
      throw new FixFormatException(FixFormatException.startMsgError);
    }

    for (int i = 0; i < tags.size(); i++) {
      if(tags.get(i).isEmpty() || values.get(i).isEmpty()) {
        throw new FixFormatException(FixFormatException.missingTagValue);
      }
      if (!msgMap.containsKey(tags.get(i))) {
        msgMap.put(tags.get(i), values.get(i));
      }
      else {
        throw new FixFormatException(FixFormatException.duplicateTags);
      }
    }
  }

  void validateMsgMap() throws FixMessageException {
    if (msgMap.get(FixConstants.internalTargetIDTag) == null) {
      throw new FixMessageException(FixMessageException.missingInternalTargetID);
    }
  }

  void checkFixFormat() throws FixFormatException {
    // This is checking if the byte string ends with a SOH char
    if (rawFixMessageBytes[rawFixMessageBytes.length - 1] != FixConstants.SOHDelimiter) {
      throw new FixFormatException(FixFormatException.missingDelimiter);
    }
  }

  void appendCheckSum() {
    // Append to string
    this.fixMessageString =
            this.fixMessageString
                    + FixConstants.checkSumTag + "="
                    + FixUtils.createCheckSumString(this.rawFixMessageBytes)
                    + FixConstants.printableDelimiter;

    // Append to byte[]
    byte[] tempByteArr = new byte[rawFixMessageBytes.length + 7];
    byte[] checkSumTag = (FixConstants.checkSumTag + "=").getBytes();
    byte[] checkSumBytes = FixUtils.createCheckSumString(rawFixMessageBytes).getBytes();

    System.arraycopy(rawFixMessageBytes, 0, tempByteArr, 0, rawFixMessageBytes.length);
    System.arraycopy(checkSumTag, 0, tempByteArr, rawFixMessageBytes.length, 3);
    System.arraycopy(checkSumBytes, 0, tempByteArr, rawFixMessageBytes.length + 3, 3);
    tempByteArr[tempByteArr.length - 1] = FixConstants.SOHDelimiter;

    this.rawFixMessageBytes = tempByteArr;
  }

  public byte[] getRawFixMsgBytes() {
    return rawFixMessageBytes;
  }

  public String getFixMsgString() {
    return fixMessageString;
  }
}

class TestEngine {
  public static void main(String[] args) {
    FixMessage fm0 = new FixMessage("24242=1|42424=2|35=V|");
    // Test cases without '=' char
    FixMessage fm1 = new FixMessage("24242=1|42424=2|35|");
    FixMessage fm2 = new FixMessage("24242|42424=2|35=V|");
    // Test without internal target ID
    FixMessage fm3 = new FixMessage("24242=1|35=V|");
    // Test with hard coded wrong checksum
    FixMessage fm4 = new FixMessage("24242=1|42424=2|35=V|10=111|");
    // Test without checksum
    FixMessage fm5 = new FixMessage("24242=1|42424=2|35=V|");
    // Test for duplicate tags
    FixMessage fm6 = new FixMessage("24242=1|42424=2|10=V|");

    // NB These tests are designed to happen sequentially,
    // ie, you will need to parse the raw bytes before validating the map.
    // Once we have factory methods these will probably be private methods
    // used in the constructors/factories.
    try {
      System.out.println(fm0.getFixMsgString());
      fm0.checkFixFormat();
      fm0.appendCheckSum();
      fm0.parseRawBytes();
      fm0.parseTagValueLists();
      fm0.validateMsgMap();
      fm0.checkFixFormat();
      FixUtils.valCheckSum(fm0.getFixMsgString());
      System.out.println("fm0: " + fm0.getFixMsgString() + "\n" + fm0.msgMap.entrySet());
      System.out.println("------------------------");
    }
    catch (FixFormatException e) {
      System.out.println("fm0 error: " + e);
      System.out.println("------------------------");
    }
    catch (FixMessageException e) {
      System.out.println("fm0 error: " + e);
    }
    catch (FixCheckSumException e) {
      System.out.println("fm0 error: " + e);
    }

    try {
      System.out.println(fm1.getFixMsgString());
      fm1.parseRawBytes();
      fm1.parseTagValueLists();
      fm1.validateMsgMap();
      fm1.appendCheckSum();
      System.out.println(fm0.getFixMsgString());
      fm1.checkFixFormat();
      System.out.println("------------------------");
    }
    catch (FixFormatException e) {
      System.out.println("fm1 error: " + e);
      System.out.println("------------------------");
    }
    catch (FixMessageException e) {
      System.out.println("fm1 error: " + e);
      System.out.println("------------------------");
    }

    try {
      System.out.println(fm2.getFixMsgString());
      fm2.parseRawBytes();
      fm2.parseTagValueLists();
      fm2.appendCheckSum();
      System.out.println(fm0.getFixMsgString());
      fm2.checkFixFormat();
      System.out.println("------------------------");
    }
    catch (FixFormatException e) {
      System.out.println("fm2 error: " + e);
      System.out.println("------------------------");
    }

    try {
      System.out.println(fm3.getFixMsgString());
      fm3.parseRawBytes();
      fm3.parseTagValueLists();
      fm3.validateMsgMap();
      fm3.checkFixFormat();
      System.out.println("------------------------");
    }
    catch (FixFormatException e) {
      System.out.println("fm3 error: " + e);
      System.out.println("------------------------");
    }
    catch (FixMessageException e) {
      System.out.println("fm3 error: " + e);
      System.out.println("------------------------");
    }

    try {
      System.out.println(fm4.getFixMsgString());
      FixUtils.valCheckSum(fm4.getFixMsgString());
      System.out.println("fm4: " + fm4.getFixMsgString());
      System.out.println("------------------------");
    }
    catch (FixCheckSumException e) {
      System.out.println("fm4 error: " + e);
      System.out.println("------------------------");
    }

    try {
      System.out.println(fm5.getFixMsgString());
      FixUtils.valCheckSum(fm5.getFixMsgString());
      System.out.println("fm5: " + fm5.getFixMsgString());
      System.out.println("------------------------");
    }
    catch (FixCheckSumException e) {
      System.out.println("fm5 error: " + e);
      System.out.println("------------------------");
    }

    try {
      System.out.println(fm6.getFixMsgString());
      fm6.appendCheckSum();
      fm6.parseRawBytes();
      fm6.parseTagValueLists();
      System.out.println("fm6: " + fm6.getFixMsgString());
      System.out.println("------------------------");
    }
    catch (FixFormatException e) {
      System.out.println("fm6 error: " + e.getMessage());
      System.out.println("------------------------");
    }

  }
}