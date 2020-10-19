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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Market {
    private AsynchronousSocketChannel client;
    private Future<Void> future;
    private static Pattern idPattern = Pattern.compile(
            "^Yello, you are now connected to the router, your ID is (\\d+)"
    );
    private static String marketId;
    private HashMap<String, Integer> Stock;

    Market() {
        Stock = new HashMap<String, Integer>();
        Stock.put("Guitars", 42);
        Stock.put("Keyboards", 42);
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
        System.out.println("Market #" + this.marketId + " received");
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
              resCode = MarketOps(
                        this.Stock,
                        fixMsg.msgMap.get(FixConstants.symbolTag),
                        Integer.parseInt(fixMsg.msgMap.get(FixConstants.orderQtyTag)),
                        "buy");

              if (resCode == 420) {
                FixMessage fixMsgResponse = FixMsgFactory.createExecFilledMsg(
                    this.marketId, senderId, clientOrdId
                );
                client.write(ByteBuffer.wrap(fixMsgResponse.getRawFixMsgBytes())).get();
                return ;
              }
              else
                rejectHandler(resCode, senderId, clientOrdId);
            }
            else if (fixMsg.msgMap.get(FixConstants.sideTag).equals(FixConstants.SELL_SIDE)) {
              resCode = MarketOps(
                        this.Stock,
                        fixMsg.msgMap.get(FixConstants.symbolTag),
                        Integer.parseInt(fixMsg.msgMap.get(FixConstants.orderQtyTag)),
                        "sell");
              if (resCode == 420) {
                FixMessage fixMsgResponse = FixMsgFactory.createExecFilledMsg(
                    this.marketId, senderId, clientOrdId
                );
                client.write(ByteBuffer.wrap(fixMsgResponse.getRawFixMsgBytes())).get();
                return ;
              }
              else
                rejectHandler(resCode, senderId, clientOrdId);
            }
        }
        catch (FixFormatException | FixMessageException e) {
            System.out.println("There was an error building the FIX message: " + e.getMessage());
        }
    }

    int MarketOps(HashMap<String, Integer> stock, String instrument, int amount, String op) {
        if (stock.containsKey(instrument)) {
          if (op.toLowerCase() == "buy") {
              if (stock.get(instrument) < amount)
                return 504;
              else {
                  stock.put(instrument, (stock.get(instrument) - amount));
                  this.Stock = stock;
                  return 420;
                }
            }
            else if (op.toLowerCase() == "sell") {
              stock.put(instrument, (stock.get(instrument) + amount));
              this.Stock = stock;
              return 420;
            }
        }
        else
          return 404;
      return -1;
    }

    void rejectHandler(int resCode, String senderId, String clientOrdId)
        throws ExecutionException, InterruptedException, IOException {
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
        else if (resCode == -1) {
          FixMessage fixMsgResponse = FixMsgFactory.createExecRejectedMsg(
            this.marketId, senderId, clientOrdId, "Aw hell, I dunno."
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

