package fortytwo;

import fortytwo.constants.FixConstants;
import fortytwo.fixexceptions.FixFormatException;
import fortytwo.fixexceptions.FixMessageException;
import fortytwo.utils.FixUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class FixMsgFactory {
  private static final Pattern pricePattern = Pattern.compile("\\.\\d{2}$");

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
    catch (FixFormatException | FixMessageException | NumberFormatException e) {
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
    catch (FixFormatException | FixMessageException | NumberFormatException e) {
      System.out.println("Test Five error: " + e);
      System.out.println("________________________________");
    }
  }
}