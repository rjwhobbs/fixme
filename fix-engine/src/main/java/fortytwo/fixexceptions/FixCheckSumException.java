package fortytwo.fixexceptions;

public class FixCheckSumException extends Exception{
  public FixCheckSumException(String error) {
    super(error);
  }

  public static final String checkSumIncorrect = "Checksum is incorrect";
  public static final String checkSumMissing = "Checksum is missing or in incorrect format";
}
