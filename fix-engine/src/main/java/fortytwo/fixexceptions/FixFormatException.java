package fortytwo.fixexceptions;

public class FixFormatException extends Exception{
  private static final long serialVersionUID = 1L;
  public FixFormatException(String error) {
    super(error);
  }

  public static final String startMsgError =
          "FIX message must start with the internal sender ID.";
  public static final String missingTagValue =
          "One or more tag value pairs are missing.";
  public static final String missingDelimiter =
          "Error in FIX message format, one or more delimiters is missing.";
  public static final String duplicateTags =
          "Your message cannot have duplicate tags";
  public static final String quantityFormat =
          "Quantity format error: Quantity needs to be a positive signed int greater than 0.";
  public static final String priceFormat =
          "Price format error: Price needs to be a positive double with 2 decimal places.";
}
