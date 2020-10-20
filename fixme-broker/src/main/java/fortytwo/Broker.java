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

public class Broker {
    private AsynchronousSocketChannel client;
    private Future<Void> future;
    private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private static Pattern idPattern = Pattern.compile(
            "^Yello, you are now connected to the router, your ID is (\\d+)"
    );
    private static String brokerId;
    private HashMap<String, Object> attachment = new HashMap<>();
    private static Boolean runInputReader = true;

    void start() throws ExecutionException, InterruptedException, IOException {
        client = AsynchronousSocketChannel.open();
        InetSocketAddress hostAddress = new InetSocketAddress("localhost", 5000);
        future = client.connect(hostAddress);
        future.get();
    }

    void readId() throws ExecutionException, InterruptedException {
        String msgFromRouter;
        ByteBuffer buffer = ByteBuffer.allocate(64);
        int bytesRead = client.read(buffer).get();
        if (bytesRead == -1) {
            System.out.println("Server has disconnected, exiting broker.");
            stop();
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
        }  catch (InterruptedException | ExecutionException e) {
            System.out.println("There was an error taking input from the broker: " + e.getMessage());
            System.out.println("Exiting...");
            stop();
        }

    }

    private void brokerInputReader() throws ExecutionException, InterruptedException {
        String line = "";
        String orderSide = "";
        String targetId = "";
        String symbol = "";
        String quantity = "";
        String price = "";
        FixMessage fixMessage = null;
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
                    System.out.println(
                            "Here is your message preview: "
                            + FixUtils.fixMsgPreview(orderSide, brokerId, targetId, symbol, quantity, price)
                            + "\nN.B Client order ID and checksum will be added once confirmed"
                    );
                    System.out.println("Do you wish to continue with your transaction? (Y)es or (N)o");
                    if ((line = getNextLine()).toLowerCase().trim().equals("exit")) {
                        runInputReader = false;
                        break ;
                    }
                    if (line.toLowerCase().trim().equals("y") || line.toLowerCase().trim().equals("yes")) {
                        try {
                            fixMessage = FixMsgFactory.createMsg(
                                    brokerId, targetId, orderSide, symbol, quantity, price
                            );
                            System.out.println("Client order ID# "
                                    + fixMessage.msgMap.get(FixConstants.clientOrdIDTag)
                                    + " sent : " + fixMessage.getFixMsgString()
                            );
                            client.write(ByteBuffer.wrap(fixMessage.getRawFixMsgBytes())).get();
                            i = 0;
                        }
                        catch (FixMessageException | FixFormatException e) {
                            System.out.println("There was an error in your message:");
                            System.out.println(e.getMessage());
                            fixMessage = null;
                            i = 0;
                        }
                    }
                    else if (line.toLowerCase().trim().equals("n") || line.toLowerCase().trim().equals("no")) {
                        System.out.println("Message not sent");
                        i = 0;
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
                printMessage(bytes);
                attachment.buffer.clear();
                client.read(attachment.buffer, attachment, this);
            } else {
                runInputReader = false;
                System.out.println("Server has disconnected, please hit enter to close the broker.");
                stop();
            }
        }

        private void printMessage(byte[] message) {
            byte[] tempBytes = Arrays.copyOf(message, message.length);
            String rawMessage = new String(FixUtils.insertPrintableDelimiter(tempBytes));
            String marketID;
            String clientOrderID;
            String rejectReason;
            String sender = "market";

            System.out.println("Raw message from server: " + rawMessage);

            try {
                FixMessage fixMessage = FixMsgFactory.createMsg(message);
                marketID = fixMessage.msgMap.get(FixConstants.internalSenderIDTag);
                clientOrderID = fixMessage.msgMap.get(FixConstants.clientOrdIDTag);
                if (fixMessage.msgMap.get(FixConstants.execTypeTag).equals(FixConstants.ORDER_FILLED)) {
                    System.out.println("Order #" + clientOrderID + " from market #" + marketID + " has been filled.");
                }
                else if (fixMessage.msgMap.get(FixConstants.execTypeTag).equals(FixConstants.ORDER_REJECTED)) {
                    if (marketID.equals("100000")) {
                        sender = "router";
                    }
                    System.out.println("Order #" + clientOrderID + " from " + sender + " #" + marketID + " has been rejected.");
                    rejectReason = fixMessage.msgMap.get(FixConstants.textTag);
                    if (rejectReason != null) {
                        System.out.println("Reject reason: " + rejectReason);
                    }
                }
            } catch (FixFormatException e) {
                System.out.println("Error creating fix message from server: " + e.getMessage());
            } catch (FixMessageException e) {
                System.out.println("Error creating fix message from server: " + e.getMessage());
            }
        }

        @Override
        public void failed(Throwable exc, ReadAttachment attachment) {
            System.out.println("There was an error reading from the server.");
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
            return "EXIT";
        }
    }

    public static void main(String[] args) {
        Broker broker = new Broker();
        try {
            broker.start();
            broker.readId();
            broker.readWriteHandler();
        } catch (InterruptedException | ExecutionException | IOException e ) {
            System.out.println(
                    "There was an error connecting to the server, please ensure that it is online: "
                            + e. getMessage()
            );
        }
    }
}
