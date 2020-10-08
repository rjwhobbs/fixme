package fortytwo;

import fortytwo.constants.FixConstants;
import fortytwo.fixexceptions.FixFormatException;
import fortytwo.fixexceptions.FixMessageException;
import fortytwo.utils.FixUtils;

public abstract class FixMsgFactory {
  public static FixMessage buyMsg(
          String internalSenderID,
          String internalTargetID,
          String symbol,
          String quantity,
          String price
  ) throws FixMessageException, FixFormatException {
    FixMessage fixMessage;
    String finalMessageString =
            FixConstants.internalSenderIDTag + "=" + internalSenderID + FixConstants.printableDelimiter
            + FixConstants.internalTargetIDTag + "=" + internalTargetID + FixConstants.printableDelimiter
            + FixConstants.sideTag + "=" + FixConstants.BUY_SIDE + FixConstants.printableDelimiter
            + FixConstants.symbolTag + "=" + symbol + FixConstants.printableDelimiter
            + FixConstants.price + "=" + price + FixConstants.printableDelimiter
            + FixConstants.orderQty + "=" + quantity + FixConstants.printableDelimiter
            + FixConstants.clientOrdID + "=" + FixUtils.createUniqueID() + FixConstants.printableDelimiter;

    fixMessage = new FixMessage(finalMessageString);

    fixMessage.checkFixFormat();
    fixMessage.appendCheckSumToBytes();
    fixMessage.appendCheckSumToString();
    fixMessage.parseRawBytes();
    fixMessage.parseTagValueLists();
    fixMessage.validateMsgMap();

    return fixMessage;
  }
}

class TestFactory {
  public static void main(String[] args) {
    // Correct input test
    try {
      FixMessage test_one = FixMsgFactory.buyMsg(
              "1",
              "2",
              "AAL",
              "20",
              "11.11"
      );
      System.out.println("__________Test One______________");
      System.out.println(test_one.getFixMsgString());
      System.out.println("________________________________");
    }
    catch (FixFormatException | FixMessageException e) {
      System.out.println("Test one error: " + e);
    }

    // Lol, tag injection.
    try {
      FixMessage test_two = FixMsgFactory.buyMsg(
              "2|3=20",
              "2",
              "AAL",
              "20",
              "11.11"
      );
      System.out.println("__________Test Two______________");
      System.out.println(test_two.getFixMsgString());
      System.out.println("________________________________");
    }
    catch (FixFormatException | FixMessageException e) {
      System.out.println("Test two error: " + e);
    }

  }
}


