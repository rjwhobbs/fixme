package fortytwo;

import fortytwo.constants.FixConstants;

public class BrokerUtils {
  public static String processOrderType(String input) {
    if (input.equals("1")) {
      return FixConstants.BUY_SIDE;
    }
    else if (input.equals("2")) {
      return FixConstants.SELL_SIDE;
    }
    else {
      System.out.println("Input \"" + input + "\" not recognised.");
    }
    return "";
  }
}
