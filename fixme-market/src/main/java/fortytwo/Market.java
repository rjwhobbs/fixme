package fortytwo;

import fortytwo.constants.FixConstants;
import fortytwo.fixexceptions.FixFormatException;
import fortytwo.fixexceptions.FixMessageException;
import fortytwo.utils.FixUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Market {
    private AsynchronousSocketChannel client;
    private Future<Void> future;
    private static Pattern idPattern = Pattern.compile(
            "^Yello, you are now connected to the router, your ID is (\\d+)"
    );
    private static String marketId = null;
    private String[] instruments = {"Guitars", "Keyboards", "Basses", "Violins", "Saxophones"};
    private double basePrice = 100.00;
    private HashMap<String, Integer> Stock;

    Market() {
      Stock = new HashMap<String, Integer>();
      int stockSelector;

      for (int i = 0; i < 3; i++) {
        stockSelector = ThreadLocalRandom.current().nextInt(0, 5);
        if (!Stock.containsKey(this.instruments[stockSelector]))
          Stock.put(this.instruments[stockSelector], 50);
        else
          i--;
      }
      // varied markets test -- shows market stock -- feel free to comment out
      System.out.println(Stock);
    }

    void start() throws IOException, ExecutionException, InterruptedException {
      client = AsynchronousSocketChannel.open();
      InetSocketAddress hostAddress = new InetSocketAddress("localhost", 5001);
      future = client.connect(hostAddress);
      future.get();
    }

    void readId() throws ExecutionException, InterruptedException, IOException {
      String msgFromRouter;
      ByteBuffer buffer = ByteBuffer.allocate(128);
      int bytesRead = client.read(buffer).get();
      if (bytesRead == -1) {
        System.out.println("Server has disconnected.");
        this.client.close();
        System.exit(0);
      }
      buffer.flip();
      msgFromRouter = new String(buffer.array());
      msgFromRouter = msgFromRouter.trim();
      Matcher m = idPattern.matcher(msgFromRouter);
      if (m.find()) {
        this.marketId = m.group(1);
      }
      if (this.marketId == null) {
        System.out.println("Server might be full, please try again later. Closing...");
        this.client.close();
      }
      else {
        System.out.println("Market #" + this.marketId + " received");
      }
    }

    void readHandler() throws ExecutionException, InterruptedException, IOException {
      String senderId;
      String clientOrdId;
      int limit;
      byte[] bytes;
      ByteBuffer buffer = ByteBuffer.allocate(512);
      int bytesRead = client.read(buffer).get();

      if (bytesRead == -1) {
        System.out.println("Server has disconnected.");
        this.client.close();
        System.exit(0);
      }

      buffer.flip();
      limit = buffer.limit();
      bytes = new byte[limit];
      buffer.get(bytes, 0, limit);
      buffer.clear();

      try {
        FixMessage fixMsg = FixMsgFactory.createMsg(bytes);
        System.out.println("This was the raw message from the router: " + fixMsg.getFixMsgString());
        senderId = fixMsg.msgMap.get(FixConstants.internalSenderIDTag);
        clientOrdId = fixMsg.msgMap.get(FixConstants.clientOrdIDTag);
        int resCode = 0;
        if (fixMsg.msgMap.get(FixConstants.sideTag).equals(FixConstants.BUY_SIDE)) {
          resCode = MarketOps(this.Stock, fixMsg.msgMap.get(FixConstants.symbolTag),
              Integer.parseInt(fixMsg.msgMap.get(FixConstants.orderQtyTag)),
              Double.parseDouble(fixMsg.msgMap.get(FixConstants.priceTag)),
              "buy");

          if (resCode == 420) {
            FixMessage fixMsgResponse = FixMsgFactory.createExecFilledMsg(this.marketId, senderId, clientOrdId);
            client.write(ByteBuffer.wrap(fixMsgResponse.getRawFixMsgBytes())).get();
            return;
          } else
            rejectHandler(resCode, senderId, clientOrdId);
        } else if (fixMsg.msgMap.get(FixConstants.sideTag).equals(FixConstants.SELL_SIDE)) {
          resCode = MarketOps(this.Stock, fixMsg.msgMap.get(FixConstants.symbolTag),
              Integer.parseInt(fixMsg.msgMap.get(FixConstants.orderQtyTag)),
              Double.parseDouble(fixMsg.msgMap.get(FixConstants.priceTag)),
              "sell");
          if (resCode == 420) {
            FixMessage fixMsgResponse = FixMsgFactory.createExecFilledMsg(this.marketId, senderId, clientOrdId);
            client.write(ByteBuffer.wrap(fixMsgResponse.getRawFixMsgBytes())).get();
            return;
          } else
            rejectHandler(resCode, senderId, clientOrdId);
        }
      } catch (FixFormatException | FixMessageException e) {
        System.out.println("There was an error building the FIX message: " + e.getMessage());
      }
    }

    int MarketOps(HashMap<String, Integer> stock, String instrument, int amount, double price, String op) {
      double maxMP = basePrice * 50.00;
      double minMP = basePrice / 4.00;
      double marketPrice;
        if (stock.containsKey(instrument)) {
          if (op.toLowerCase() == "buy") {
            if (stock.get(instrument) <= 0)
              return 504;
            // marketPrice cannot exceed maxMP else stock has decreased to 0 or below -- this is checked in the above if() block
            marketPrice = this.basePrice * (50.00/stock.get(instrument));
            if (marketPrice < minMP) 
              marketPrice = minMP;
            if (stock.get(instrument) < amount)
              return 504;
            if (price < 0.9 * marketPrice || price < minMP)
              return 601;
            else if (price > 1.2 * marketPrice)
              return 602;
            else {
              stock.put(instrument, (stock.get(instrument) - amount));
              // observe stock changes
              // System.out.println(this.Stock);
              return 420;
            }
          }
          else if (op.toLowerCase() == "sell") {
            if (stock.get(instrument) <= 0)
              marketPrice = maxMP;
            else
              marketPrice = this.basePrice * (50/stock.get(instrument));
            // prevents oversaturation of market stocks and excessive deflation of stock value
            if (marketPrice < minMP)
              return 505;
            if (amount + stock.get(instrument) > 200 )
              return 506;
            if (price < 0.8 * marketPrice || price < minMP)
              return 601;
            else if (price > 1.1 * marketPrice || price > maxMP)
              return 602;
            else {
              stock.put(instrument, (stock.get(instrument) + amount));
              this.Stock = stock;
              // observe stock changes
              // System.out.println(this.Stock);
              return 420;
            }
          }
        }
        else
          return 404;
      return -1;
    }

    void rejectHandler(int resCode, String senderId, String clientOrdId)
        throws ExecutionException, InterruptedException {
      try {
        if (resCode == 404) {
          FixMessage fixMsgResponse = FixMsgFactory.createExecRejectedMsg(
            this.marketId, senderId, clientOrdId, "Instrument not stocked at market# " + this.marketId
          );
          client.write(ByteBuffer.wrap(fixMsgResponse.getRawFixMsgBytes())).get();
          return ;
        }
        else if (resCode == 504) {
          FixMessage fixMsgResponse = FixMsgFactory.createExecRejectedMsg(
            this.marketId, senderId, clientOrdId, "Instrument stock insufficient at market#" + this.marketId
          );
          client.write(ByteBuffer.wrap(fixMsgResponse.getRawFixMsgBytes())).get();
          return ;
        }
        else if (resCode == 505) {
          FixMessage fixMsgResponse = FixMsgFactory.createExecRejectedMsg(
            this.marketId, senderId, clientOrdId, "Instrument stock oversaturated at market#" + this.marketId
          );
          client.write(ByteBuffer.wrap(fixMsgResponse.getRawFixMsgBytes())).get();
          return ;
        }
        else if (resCode == 506) {
          FixMessage fixMsgResponse = FixMsgFactory.createExecRejectedMsg(
            this.marketId, senderId, clientOrdId, "Order will oversaturate instrument stock at market#" + this.marketId
          );
          client.write(ByteBuffer.wrap(fixMsgResponse.getRawFixMsgBytes())).get();
          return ;
        }
        else if (resCode == -1) {
          FixMessage fixMsgResponse = FixMsgFactory.createExecRejectedMsg(
            this.marketId, senderId, clientOrdId, "Aw hell, I dunno."
          );
          client.write(ByteBuffer.wrap(fixMsgResponse.getRawFixMsgBytes())).get();
          return ;
        }
        else
          priceReject(resCode, senderId, clientOrdId);
      } catch (FixFormatException | FixMessageException e) {
        System.out.println("There was an error building the FIX message: " + e.getMessage());
      }
    }

    void priceReject(int resCode, String senderId, String clientOrdId)
    throws ExecutionException, InterruptedException {
      try {
        if (resCode == 601) {
          FixMessage fixMsgResponse = FixMsgFactory.createExecRejectedMsg(
            this.marketId, senderId, clientOrdId, "Instrument stock undervalued at market# " + this.marketId
          );
          client.write(ByteBuffer.wrap(fixMsgResponse.getRawFixMsgBytes())).get();
          return ;
        }
        else if (resCode == 602) {
          FixMessage fixMsgResponse = FixMsgFactory.createExecRejectedMsg(
            this.marketId, senderId, clientOrdId, "Instrument stock overvalued at market#" + this.marketId
          );
          client.write(ByteBuffer.wrap(fixMsgResponse.getRawFixMsgBytes())).get();
          return ;
        }
      } catch (FixFormatException | FixMessageException e) {
        System.out.println("There was an error building the FIX message: " + e.getMessage());
      }
    }

    public static void main(String[] args) {
        Market market = new Market();
     
        try {
            market.start();
            market.readId();
            while (true) {
                market.readHandler();
            }
        } catch (InterruptedException e) {
            System.out.println("Something went wrong communicating with the server: " + e.getMessage());
        } catch (ExecutionException | IOException e) {
            System.out.println("Unable to establish connection with router: " + e.getMessage());
        }
    }
}

