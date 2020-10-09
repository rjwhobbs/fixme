package fortytwo.fixexceptions;

public class FixCheckSumException extends Exception{
  private static final long serialVersionUID = 1L;
  public FixCheckSumException(String error) {
    super(error);
  }

  public static final String checkSumIncorrect = "Checksum is incorrect";
  public static final String checkSumMissing = "Checksum is missing or in incorrect format";
}
