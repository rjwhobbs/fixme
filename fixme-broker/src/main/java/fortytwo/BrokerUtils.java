package fortytwo;

import fortytwo.constants.FixConstants;

import java.nio.ByteBuffer;

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

//  public static String processConfirmSend() {
//    if (line.toLowerCase().equals("y")) {
//      String query = targetId + " " + orderType;
//      client.write(ByteBuffer.wrap(query.getBytes())).get();
//      System.out.println("Message sent");
//      i = 0;
//      orderType = "";
//      targetId = "";
//    }
//    else if (line.toLowerCase().equals("n")) {
//      System.out.println("Message not sent");
//      i = 0;
//      orderType = "";
//      targetId = "";
//    }
//    else {
//      System.out.println("Input \"" + line + "\" not recognized.");
//    }
//  }

}
