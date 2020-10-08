package fortytwo;

import fortytwo.constants.FixConstants;
import fortytwo.fixexceptions.FixFormatException;
import fortytwo.fixexceptions.FixMessageException;
import fortytwo.utils.FixUtils;

public abstract class FixMsgFactory {
  public static FixMessage createBuyMsg(
          String internalSenderID,
          String internalTargetID,
          String symbol,
          String quantity,
          String price
  ) throws FixMessageException, FixFormatException {
    FixMessage fixMessage;
    String finalBuyMsg = buySellTemplate(
            FixConstants.BUY_SIDE,
            internalSenderID,
            internalTargetID,
            symbol,
            quantity,
            price
    );

    fixMessage = new FixMessage(finalBuyMsg);

    fixMessage.checkFixFormat();
    fixMessage.appendCheckSum();
    fixMessage.parseRawBytes();
    fixMessage.parseTagValueLists();
    fixMessage.validateMsgMap();

    return fixMessage;
  }

  public static FixMessage createSellMsg(
          String internalSenderID,
          String internalTargetID,
          String symbol,
          String quantity,
          String price
  ) throws FixMessageException, FixFormatException {
    FixMessage fixMessage;
    String finalSellMsg = buySellTemplate(
            FixConstants.SELL_SIDE,
            internalSenderID,
            internalTargetID,
            symbol,
            quantity,
            price
    );

    fixMessage = new FixMessage(finalSellMsg);

    fixMessage.checkFixFormat();
    fixMessage.appendCheckSum();
    fixMessage.parseRawBytes();
    fixMessage.parseTagValueLists();
    fixMessage.validateMsgMap();

    return fixMessage;
  }

  private static String buySellTemplate(
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
            + FixConstants.price + "=" + price + FixConstants.printableDelimiter
            + FixConstants.orderQty + "=" + quantity + FixConstants.printableDelimiter
            + FixConstants.clientOrdID + "=" + FixUtils.createUniqueID() + FixConstants.printableDelimiter;
  }
}

class TestFactory {
  public static void main(String[] args) {
    // Correct input test on buy msg
    try {
      FixMessage test_one = FixMsgFactory.createBuyMsg(
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
      FixMessage test_two = FixMsgFactory.createBuyMsg(
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

    // Correct input test on sell msg
    try {
      FixMessage test_three = FixMsgFactory.createSellMsg(
              "1",
              "2",
              "AAL",
              "20",
              "11.11"
      );
      System.out.println("__________Test Three______________");
      System.out.println(test_three.getFixMsgString());
      System.out.println("________________________________");
    }
    catch (FixFormatException | FixMessageException e) {
      System.out.println("Test three error: " + e);
    }


  }
}


