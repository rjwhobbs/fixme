package fortytwo.fixexceptions;

public class FixMessageException extends Exception {
  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public FixMessageException(String error) {
    super(error);
  }

  public static final String missingInternalTargetID = "FIX message must contain an internal target ID.";
}
