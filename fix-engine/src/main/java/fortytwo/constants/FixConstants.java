package fortytwo.constants;

public class FixConstants {
  // Delimiter
  public final static char printableDelimiter = '|';
  public final static char SOHDelimiter = 1;

  // tags
  public final static String checkSumTag          = "10";
  public final static String clientOrdIDTag       = "11";
  public final static String msgTypeTag           = "35";
  public final static String orderQtyTag          = "38";
  public final static String priceTag             = "44";
  public final static String sideTag              = "54";
  public final static String symbolTag            = "55";
  public final static String targetCompIDTag      = "56";
  public final static String execTypeTag          = "150";
  public final static String internalSenderIDTag  = "49";
  public final static String internalTargetIDTag  = "56";
  public final static String textTag              = "58";

  // values
  public final static String ORDER_SINGLE         = "D";
  public final static String MARKET_DATA_REQUEST  = "V";
  public final static String BUY_SIDE             = "1";
  public final static String SELL_SIDE            = "2";
  public final static String ORDER_REJECTED       = "2";
  public final static String ORDER_FILLED         = "7";
  public final static String EXEC_REPORT          = "8";
}
