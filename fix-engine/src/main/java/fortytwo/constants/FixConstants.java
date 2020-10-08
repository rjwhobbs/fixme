package fortytwo.constants;

public class FixConstants {
  // Delimiter
  public final static char printableDelimiter = '|';
  public final static char SOHDelimiter = 1;

  // tags
  public final static String checkSumTag          = "10";
  public final static String clientOrdID          = "11";
  public final static String msgTypeTag           = "35";
  public final static String orderQty             = "38";
  public final static String price                = "44";
  public final static String senderCompIDTag      = "49";
  public final static String sideTag              = "54";
  public final static String symbolTag            = "55";
  public final static String targetCompIDTag      = "56";
  public final static String execType             = "150";
  public final static String internalSenderIDTag  = "24242";
  public final static String internalTargetIDTag  = "42424";

  // values
  public final static String MARKET_DATA_REQUEST  = "V";
  public final static String BUY_SIDE             = "1";
  public final static String SELL_SIDE            = "2";
  public final static String ORDER_FILLED         = "2";
  public final static String ORDER_REJECTED       = "8";
}
