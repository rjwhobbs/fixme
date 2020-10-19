package fortytwo;

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
    //  private static AsyncChatClient instance;
//  private Boolean clientOpenState;
    private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private static Pattern senderPattern = Pattern.compile("^broker#(\\d+)");
    private static Pattern idPattern = Pattern.compile("^Welcome to whisper chat, your ID is (\\d+)$");
    private static String marketId;
    private HashMap<String, Integer> Stock;

    Market() {
        try {
            client = AsynchronousSocketChannel.open();
            InetSocketAddress hostAddress = new InetSocketAddress("localhost", 5001);
            future = client.connect(hostAddress);
            future.get();
            Stock = new HashMap<String, Integer>();
            Stock.put("Guitars", 42);
            Stock.put("Keyboards", 42);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    void readId() throws ExecutionException, InterruptedException, IOException {
        String msgFromRouter;
        ByteBuffer buffer = ByteBuffer.allocate(64);
        int bytesRead = client.read(buffer).get();
        if (bytesRead == -1) {
            System.out.println("Server has disconnected.");
            // Do other things
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
        String msgFromRouter;
        String senderId = "";
        String response = "";
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        int bytesRead = client.read(buffer).get();
        if (bytesRead == -1) {
            System.out.println("Server has disconnected.");
            // Do other things
            this.client.close();
            System.exit(0);
        }
        buffer.flip();
        msgFromRouter = new String(buffer.array());
        System.out.println(msgFromRouter);
        Matcher m = senderPattern.matcher(msgFromRouter);
        if (m.find()) {
            senderId = m.group(1);
            response = "\\" + senderId + " acknowledged\n";
            client.write(ByteBuffer.wrap(response.getBytes())).get();
        }
    }

    boolean MarketOps(HashMap<String, Integer> stock, String instrument, int amount, String op) {
        if (stock.containsKey(instrument)) {
            if (op.toLowerCase() == "buy") {
                if (stock.get(instrument) < amount)
                    return false;
                else {
                    stock.put(instrument, (stock.get(instrument) - amount));
                    this.Stock = stock;
                    return true;
                }
            }
            else if (op.toLowerCase() == "sell") {
                stock.put(instrument, (stock.get(instrument) + amount));
                this.Stock = stock;
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        Market market = new Market();
        // readId
        try {
            market.readId();
            while (true) {
                market.readHandler();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException | IOException e) {
            e.printStackTrace();
        }
    }
}

