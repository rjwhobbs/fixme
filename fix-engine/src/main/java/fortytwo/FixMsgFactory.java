package fortytwo;

import fortytwo.constants.FixConstants;
import fortytwo.utils.FixUtils;

public abstract class FixMsgFactory {
  public static FixMessage buyMsg(
          String internalSenderID,
          String internalTargetID,
          String symbol,
          String quantity,
          String price
  ) {
    String finalMessage =
            FixConstants.internalSenderIDTag + "=" + internalSenderID + FixConstants.printableDelimiter
            + FixConstants.internalTargetIDTag + "=" + internalTargetID + FixConstants.printableDelimiter
            + FixConstants.sideTag + "=" + FixConstants.BUY_SIDE + FixConstants.printableDelimiter
            + FixConstants.symbolTag + "=" + symbol + FixConstants.printableDelimiter
            + FixConstants.price + "=" + price + FixConstants.printableDelimiter
            + FixConstants.orderQty + "=" + quantity + FixConstants.printableDelimiter
            + FixConstants.clientOrdID + "=" + FixUtils.createUniqueID() + FixConstants.printableDelimiter;

    return new FixMessage(finalMessage);
  }
}
