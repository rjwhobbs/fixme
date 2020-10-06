package fortytwo.fixexceptions;

import fortytwo.FixMessage;

public class FixMessageException extends Exception {
  public FixMessageException(String error) {
    super(error);
  }
}
