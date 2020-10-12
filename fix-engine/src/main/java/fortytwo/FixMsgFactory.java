package fortytwo;

import fortytwo.constants.FixConstants;
import fortytwo.fixexceptions.FixFormatException;
import fortytwo.fixexceptions.FixMessageException;
import fortytwo.utils.FixUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class FixMsgFactory {
  private static final Pattern pricePattern = Pattern.compile("\\.\\d{2}$");

  public static FixMessage createMsg(String msg) throws FixMessageException, FixFormatException {
    FixMessage fixMessage = new FixMessage(msg);

    fixMessage.checkFixFormat();
    fixMessage.parseRawBytes();
    fixMessage.parseTagValueLists();
    fixMessage.validateMsgMap();

    return fixMessage;
  }

  public static FixMessage createMsg(byte[] msg) throws FixMessageException, FixFormatException {
    FixMessage fixMessage = new FixMessage(msg);

    fixMessage.checkFixFormat();
    fixMessage.parseRawBytes();
    fixMessage.parseTagValueLists();
    fixMessage.validateMsgMap();

    return fixMessage;
  }

  public static FixMessage createBuyMsg(
          String internalSenderID,
          String internalTargetID,
          String symbol,
          String quantity,
          String price
  ) throws FixMessageException, FixFormatException {
    valQuantityInput(quantity);
    valPriceInput(price);
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
    valQuantityInput(quantity);
    valPriceInput(price);
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

  public static FixMessage createExecFilledMsg(
          String internalSenderID,
          String internalTargetID,
          String clientOrdID
  ) throws FixMessageException, FixFormatException {
    String finalFilledMsg = execReportTemplate(
            FixConstants.ORDER_FILLED,
            internalSenderID,
            internalTargetID,
            clientOrdID
    );

    FixMessage fixMessage = new FixMessage(finalFilledMsg);

    fixMessage.checkFixFormat();
    fixMessage.appendCheckSum();
    fixMessage.parseRawBytes();
    fixMessage.parseTagValueLists();
    fixMessage.validateMsgMap();

    return fixMessage;
  }

  public static FixMessage createExecRejectedMsg(
          String internalSenderID,
          String internalTargetID,
          String clientOrdID
  ) throws FixMessageException, FixFormatException {
    String finalFilledMsg = execReportTemplate(
            FixConstants.ORDER_REJECTED,
            internalSenderID,
            internalTargetID,
            clientOrdID
    );

    FixMessage fixMessage = new FixMessage(finalFilledMsg);

    fixMessage.checkFixFormat();
    fixMessage.appendCheckSum();
    fixMessage.parseRawBytes();
    fixMessage.parseTagValueLists();
    fixMessage.validateMsgMap();

    return fixMessage;
  }

  private static void valPriceInput(String input) throws FixFormatException {
    try {
      double testInput = Double.parseDouble(input);
      if (testInput <= 0) {
        throw new FixFormatException(FixFormatException.priceFormat);
      }
      Matcher m = pricePattern.matcher(input);
      if (!m.find()) {
        throw new FixFormatException(FixFormatException.priceFormat);
      }
    }
    catch (NumberFormatException e) {
      throw new FixFormatException(FixFormatException.priceFormat);
    }
  }

  private static void valQuantityInput(String input) throws FixFormatException {
    try {
      int testInput = Integer.parseInt(input);
      if (testInput < 1) {
        throw new FixFormatException(FixFormatException.quantityFormat);
      }
    }
    catch (NumberFormatException e) {
      throw new FixFormatException(FixFormatException.quantityFormat);
    }
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
            + FixConstants.priceTag + "=" + price + FixConstants.printableDelimiter
            + FixConstants.orderQtyTag + "=" + quantity + FixConstants.printableDelimiter
            + FixConstants.clientOrdIDTag + "=" + FixUtils.createUniqueID() + FixConstants.printableDelimiter;
  }

  private static String execReportTemplate(
          String execType,
          String internalSenderID,
          String internalTargetID,
          String clientOrdID
  ) {
    return FixConstants.internalSenderIDTag + "=" + internalSenderID + FixConstants.printableDelimiter
            + FixConstants.internalTargetIDTag + "=" + internalTargetID + FixConstants.printableDelimiter
            + FixConstants.msgTypeTag + "=" + FixConstants.EXEC_REPORT + FixConstants.printableDelimiter
            + FixConstants.clientOrdIDTag + "=" + clientOrdID + FixConstants.printableDelimiter
            + FixConstants.execTypeTag + "=" + execType + FixConstants.printableDelimiter;
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
      System.out.println("________________________________");
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
      System.out.println("________________________________");
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
      System.out.println("________________________________");
    }

    // Testing bad quantity
    try {
      FixMessage test_four = FixMsgFactory.createBuyMsg(
              "1",
              "2",
              "AAL",
              "a20s",
              "11.11"
      );
      System.out.println("__________Test Four______________");
      System.out.println(test_four.getFixMsgString());
      System.out.println("________________________________");
    }
    catch (FixFormatException | FixMessageException e) {
      System.out.println("Test Four error: " + e);
      System.out.println("________________________________");
    }

    // Testing bad price format
    try {
      FixMessage test_five = FixMsgFactory.createSellMsg(
              "1",
              "2",
              "AAL",
              "20",
              "0.00"
      );
      System.out.println("__________Test Five______________");
      System.out.println(test_five.getFixMsgString());
      System.out.println("________________________________");
    }
    catch (FixFormatException | FixMessageException e) {
      System.out.println("Test Five error: " + e);
      System.out.println("________________________________");
    }

    // Testing exec report filled
    try {
      FixMessage test_six = FixMsgFactory.createExecFilledMsg(
              "1",
              "2",
              "qe3_123"
      );
      System.out.println("__________Test Six______________");
      System.out.println(test_six.getFixMsgString());
      System.out.println("________________________________");
    }
    catch (FixFormatException | FixMessageException e) {
      System.out.println("Test Six error: " + e);
      System.out.println("________________________________");
    }

    // Testing exec report rejected
    try {
      FixMessage test_seven = FixMsgFactory.createExecRejectedMsg(
              "1",
              "2",
              "qe3_123"
      );
      System.out.println("__________Test Seven______________");
      System.out.println(test_seven.getFixMsgString());
      System.out.println("________________________________");
    }
    catch (FixFormatException | FixMessageException e) {
      System.out.println("Test Seven error: " + e);
      System.out.println("________________________________");
    }

    // Usage test
    try {
      System.out.println("------Usage test one--------------");
      FixMessage fixMessage = FixMsgFactory.createBuyMsg(
              "000001",
              "000002",
              "AAL",
              "20",
              "11.11"

      );
      if (fixMessage.msgMap.get(FixConstants.msgTypeTag).equals(FixConstants.ORDER_SINGLE)) {
        System.out.println("Message is a single order.");
      }

      System.out.println("--------------------");
    }
    catch (FixFormatException | FixMessageException e) {
      System.out.println("------Usage test one error--------------");
      System.out.println(e.getMessage());
    }

    // Testing with raw input
    try {
      System.out.println("------Usage test two--------------");
      String rawInput = FixConstants.internalSenderIDTag + "=1" + FixConstants.printableDelimiter
              + FixConstants.internalTargetIDTag + "=2" + FixConstants.printableDelimiter
              + FixConstants.msgTypeTag + "=" + FixConstants.ORDER_SINGLE + FixConstants.printableDelimiter
              + FixConstants.clientOrdIDTag + "=" + FixUtils.createUniqueID() + FixConstants.printableDelimiter
              + FixConstants.symbolTag + "=AAL" + FixConstants.printableDelimiter
              + FixConstants.orderQtyTag + "=10" + FixConstants.printableDelimiter
              + FixConstants.priceTag + "=0.50" + FixConstants.printableDelimiter;
      FixMessage fixMessage = FixMsgFactory.createMsg(
             rawInput + FixConstants.checkSumTag + "="
                     + FixUtils.createCheckSumString(rawInput.getBytes())
                     + FixConstants.printableDelimiter
      );
      if (fixMessage.msgMap.get(FixConstants.msgTypeTag).equals(FixConstants.ORDER_SINGLE)) {
        System.out.println("Message is a single order.");
        System.out.println(fixMessage.getFixMsgString());
      }

      System.out.println("--------------------");
    }
    catch (FixFormatException | FixMessageException e) {
      System.out.println("------Usage test two error--------------");
      System.out.println(e.getMessage());
    }
  }
}