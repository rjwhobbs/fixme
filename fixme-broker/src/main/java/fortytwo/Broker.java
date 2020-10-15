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
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import fortytwo.FixMessage;

public class Broker {
    private AsynchronousSocketChannel client;
    private Future<Void> future;
    private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private static BufferedReader blockerReader = new BufferedReader(new InputStreamReader(System.in));
    private static Pattern senderPattern = Pattern.compile("^market#(\\d+)");
    private static Pattern idPattern = Pattern.compile(
            "^Yello, you are now connected to the router, your ID is (\\d+)"
    );
    private static String brokerId;
    private HashMap<String, Object> attachment = new HashMap<>();
    private static Boolean runInputReader = true;

    Broker() {
        try {
            client = AsynchronousSocketChannel.open();
            InetSocketAddress hostAddress = new InetSocketAddress("localhost", 5000);
            future = client.connect(hostAddress);
            future.get();
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
            this.brokerId = m.group(1);
        }
        System.out.println("Broker id #" + this.brokerId + " received");
    }

    private void readWriteHandler() {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        ReadAttachment readAttachment = new ReadAttachment(buffer);
        client.read(readAttachment.buffer, readAttachment, new ReadHandler());

        try {
            brokerInputReader();
            System.out.println("Broker has disconnected.");
            stop();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void brokerInputReader() throws IOException, ExecutionException, InterruptedException {
        String line = "";
        String orderSide = "";
        String targetId = "";
        String symbol = "";
        String quantity = "";
        String price = "";
        int i = 0;
        System.out.println("Type \"EXIT\" to quit.");
        while (runInputReader) {
            switch (i) {
                case 0:
                    System.out.println("Choose order type. (1) Buy. (2) Sell: ");
                    if ((line = getNextLine()).toLowerCase().equals("exit")) {
                        runInputReader = false;
                        break ;
                    }
                    if (!(orderSide = BrokerUtils.processOrderType(line)).equals("")) {
                        ++i;
                    }
                    break;
                case 1:
                    System.out.println("Give the target market ID: ");
                    if ((line = getNextLine()).toLowerCase().equals("exit")) {
                        runInputReader = false;
                        break ;
                    }
                    targetId = line;
                    ++i;
                    break;
                case 2:
                    System.out.println("Give the instrument symbol: ");
                    if ((line = getNextLine()).toLowerCase().equals("exit")) {
                        runInputReader = false;
                        break ;
                    }
                    symbol = line;
                    ++i;
                    break;
                case 3:
                    System.out.println("Give the quantity: ");
                    if ((line = getNextLine()).toLowerCase().equals("exit")) {
                        runInputReader = false;
                        break ;
                    }
                    quantity = line;
                    ++i;
                    break;
                case 4:
                    System.out.println("Give the price (format: x.xx) : ");
                    if ((line = getNextLine()).toLowerCase().equals("exit")) {
                        runInputReader = false;
                        break ;
                    }
                    price = line;
                    ++i;
                    break;
                case 5:
                    String[] userInputs = {brokerId, targetId, orderSide, symbol, quantity, price};
                    System.out.println("Here is your message: " + Arrays.toString(userInputs) + ".");
                    System.out.println("Send (y/n) ?: ");
                    if ((line = getNextLine()).toLowerCase().equals("exit")) {
                        runInputReader = false;
                        break ;
                    }
                    if (line.toLowerCase().equals("y")) {
                        String query = targetId + " " + orderSide;
                        client.write(ByteBuffer.wrap(query.getBytes())).get();
                        System.out.println("Message sent");
                        i = 0;
                        orderSide = "";
                        targetId = "";
                    }
                    else if (line.toLowerCase().equals("n")) {
                        System.out.println("Message not sent");
                        i = 0;
                        orderSide = "";
                        targetId = "";
                    }
                    else {
                        System.out.println("Input \"" + line + "\" not recognized.");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    class ReadHandler implements CompletionHandler<Integer, ReadAttachment> {
        @Override
        public void completed(Integer result, ReadAttachment attachment) {
            if (result != -1) {
                attachment.buffer.flip();
                int limit = attachment.buffer.limit();
                byte[] bytes = new byte[limit];
                attachment.buffer.get(bytes, 0, limit);
                String line = new String(bytes);
                System.out.print(line);
                ;
                attachment.buffer.clear();
                client.read(attachment.buffer, attachment, this);
            } else {
                runInputReader = false;
                System.out.println("Server has disconnected, please hit enter to close the broker.");
                stop();
            }
        }

        @Override
        public void failed(Throwable exc, ReadAttachment attachment) {

        }
    }

    class ReadAttachment {
        public ByteBuffer buffer;

        ReadAttachment(ByteBuffer buffer) {
            this.buffer = buffer;
        }
    }

    private void stop() {
        try {
            client.close();
            reader.close();
//            System.out.println("cheers");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private static String getNextLine() {
        String line;
        try {
            line = reader.readLine();
            if (line == null) {
                return "EXIT";
            }
            if (!runInputReader) {
                return "EXIT";
            }
            return line.trim();
        }
        catch (IOException e) {
//            System.out.println("There was an error reading from the console: " + e.getMessage());
            return "EXIT";
        }
    }

    public static void main(String[] args) {
        Broker broker = new Broker();
        // Needs error handling for in case the server isn't running when first connecting.
        try {
            broker.readId();
            while (true) {
                broker.readWriteHandler();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
