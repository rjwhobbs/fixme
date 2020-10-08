package fortytwo.fixexceptions;

import fortytwo.FixMessage;

public class FixMessageException extends Exception {
  public FixMessageException(String error) {
    super(error);
  }

  public static final String missingInternalTargetID = "FIX message must contain an internal target ID.";
}
