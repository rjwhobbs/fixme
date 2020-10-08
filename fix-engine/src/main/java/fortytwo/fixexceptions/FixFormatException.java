package fortytwo.fixexceptions;

public class FixFormatException extends Exception{
  public FixFormatException(String error) {
    super(error);
  }

  public static final String startMsgError =
          "FIX message must start with the internal sender ID.";
  public static final String missingTagValue =
          "One or more tag value pairs are missing.";
  public static final String missingDelimiter =
          "Error in FIX message format, one or more delimiters is missing.";
}
