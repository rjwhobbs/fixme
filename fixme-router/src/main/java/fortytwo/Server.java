package fortytwo;

import fortytwo.constants.FixConstants;
import fortytwo.fixexceptions.FixCheckSumException;
import fortytwo.fixexceptions.FixFormatException;
import fortytwo.fixexceptions.FixMessageException;
import fortytwo.utils.FixUtils;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Server {
    private static final Logger log = Logger.getLogger("Logger").getParent();
    private static BufferedReader blockerReader;
    private static Pattern pattern;
    private static Executor pool;
    private static int brokersIndex;
    private static int marketsIndex;
    private static HashMap<String, ClientAttachment> brokers;
    private static HashMap<String, ClientAttachment> markets;

    public Server() {
        blockerReader = new BufferedReader(new InputStreamReader(System.in));
        pattern = Pattern.compile("^\\\\(\\d+)\\s+(.+)");
        pool = Executors.newFixedThreadPool(200);
        brokers = new HashMap<>();
        markets = new HashMap<>();
        brokersIndex = 1;
        marketsIndex = 1;
    }

    public void acceptBroker() {
        try (final AsynchronousServerSocketChannel brokerChannel = AsynchronousServerSocketChannel.open()) {
            InetSocketAddress hostAddress = new InetSocketAddress("localhost", 5000);
            brokerChannel.bind(hostAddress);
            while (true) {
                brokerChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
                    @Override
                    public void completed(AsynchronousSocketChannel result, Object attachment) {
                        if (result.isOpen()) {
                            brokerChannel.accept(null, this);
                            log.info("Broker Connected");
                            registerBroker(result);
                        }
                    }

                    private void registerBroker(AsynchronousSocketChannel client)  {
                        try {
                            //TODO generate unique six digit ID
                            String brokerID = Integer.toString(brokersIndex++);
                            String welcomeMessage =
                                    "Yello, you are now connected to the router, your ID is " + brokerID + "\n";

                            client.write(ByteBuffer.wrap(welcomeMessage.getBytes())).get();
                            ClientAttachment clientAttachment = new ClientAttachment(client, brokerID);
                            brokers.put(brokerID, clientAttachment);
                            log.info("Connected Brokers " + brokers.entrySet());
                            client.read(clientAttachment.buffer, clientAttachment, new BrokerHandler());
                        } catch (InterruptedException | ExecutionException e) {
                            System.err.println("Something went wrong while trying to register a broker");
                        }
                    }

                    @Override
                    public void failed(Throwable exc, Object attachment) {
                        System.err.println("Something went wrong while connecting Broker to Router: " + exc.getMessage());
                    }
                });
                log.info("Listening on port 5000");
                blocker();
            }
        } catch (Exception e) {
            System.err.println("Router Error in acceptBroker(): " + e.getMessage());

        }
    }

    public void acceptMarket() {
        try (final AsynchronousServerSocketChannel marketChannel = AsynchronousServerSocketChannel.open()) {
            InetSocketAddress hostAddress = new InetSocketAddress("localhost", 5001);
            marketChannel.bind(hostAddress);

            while (true) {
                marketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
                    @Override
                    public void completed(AsynchronousSocketChannel result, Object attachment) {
                        if (result.isOpen()) {
                            marketChannel.accept(null, this);
                            System.out.println("Market Connected");
                            registerMarket(result);
                        }
                    }

                    private void registerMarket(AsynchronousSocketChannel client) {
                        try {
                            String marketID = Integer.toString(marketsIndex++);
                            String welcomeMessage =
                                    "Yello, you are now connected to the router, your ID is " + marketID + "\n";

                            client.write(ByteBuffer.wrap(welcomeMessage.getBytes())).get();
                            ClientAttachment clientAttachment = new ClientAttachment(client, marketID);
                            markets.put(marketID, clientAttachment);
                            log.info("Connected Markets" + brokers.entrySet());
                            client.read(clientAttachment.buffer, clientAttachment, new MarketHandler());
                        } catch (InterruptedException | ExecutionException e) {
                             System.err.println("Something went wrong while trying to register a market");
                        }
                    }

                    @Override
                    public void failed(Throwable exc, Object attachment) {
                        System.err.println("Something went wrong while connecting Market to Router");
                    }
                });
                log.info("Listening on port 5001");
                blocker();
            }
        } catch (Exception e) {
            System.err.println("Router Error in acceptMarket(): " + e.getMessage());
        }
    }

    public static void blocker() {
        try {
            blockerReader.readLine();
            blocker();
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void sendToMarket(byte[] message) {
        try {
            FixMessage fixMessage = FixMsgFactory.createMsg(message);
            //Debug
            log.info(fixMessage.getFixMsgString());
            FixUtils.valCheckSum(fixMessage.getFixMsgString());
            String senderID = fixMessage.msgMap.get(FixConstants.internalSenderIDTag);
            pool.execute(new SendToMarket(message, senderID));
        } catch (FixCheckSumException e) {
            //TODO display on the client
            System.err.println("Failed checksum " + e.getMessage());
        } catch (FixFormatException e) {
            System.err.println("Fix format exception " + e.getMessage());
        } catch (FixMessageException e) {
            System.err.println("Failed to create Fixed message " + e.getMessage());
        }
    }

    private void sendToBroker(String message) {

        try {
            FixMessage fixMessage = FixMsgFactory.createMsg(message);
            System.out.println(fixMessage.getFixMsgString());
            FixUtils.valCheckSum(fixMessage.getFixMsgString());
            String senderID = fixMessage.msgMap.get(FixConstants.internalSenderIDTag);
            pool.execute(new SendToBroker(message, senderID));
        } catch (FixCheckSumException e) {
            System.err.println("Failed checksum " + e.getMessage());
        } catch (FixFormatException e) {
            System.err.println("Fix format exception " + e.getMessage());
        } catch (FixMessageException e) {
            System.err.println("Failed to create Fixed message " + e.getMessage());
        }
    }

    class BrokerHandler implements CompletionHandler<Integer, ClientAttachment> {
        @Override
        public void completed(Integer result, ClientAttachment attachment) {
            try {
                if (result != -1) {
                    attachment.buffer.flip();
                    int limit = attachment.buffer.limit();
                    byte[] bytes = new byte[limit];
                    attachment.buffer.get(bytes, 0, limit);
                    sendToMarket(bytes);
                    attachment.buffer.clear();
                    attachment.client.read(attachment.buffer, attachment, this);
                } else {
                    attachment.client.close();
                    attachment.client = null;
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }

        @Override
        public void failed(Throwable exc, ClientAttachment attachment) {
            System.err.println("Failed method in BrokerHandler called: " + exc.getMessage());
        }

    }

    class MarketHandler implements CompletionHandler<Integer, ClientAttachment> {

        @Override
        public void completed(Integer result, ClientAttachment attachment) {
            try {
                if (result != -1) {
                    attachment.buffer.flip();
                    int limit = attachment.buffer.limit();
                    byte[] bytes = new byte[limit];
                    attachment.buffer.get(bytes, 0, limit);
                    String line = new String(FixUtils.insertPrintableDelimiter(bytes));
                    sendToBroker(line);
                    attachment.buffer.clear();
                    attachment.client.read(attachment.buffer, attachment, this);
                } else {
                    attachment.client.close();
                    attachment.client = null;
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }

        @Override
        public void failed(Throwable exc, ClientAttachment attachment) {
            System.err.println("Failed method in MarketHandler called: " + exc.getMessage());
        }
    }

    class SendToMarket implements Runnable {
        private byte[] message;
        private String senderID;

        SendToMarket(byte[] message, String senderID) {
            this.message = message;
            this.senderID = senderID;
        }

        @Override
        public void run() {
            try {
                String marketID = FixMsgFactory.createMsg(message).msgMap.get(FixConstants.internalTargetIDTag);
                log.info(Arrays.toString(message));
                ClientAttachment clientAttachment = markets.get(marketID);
                if (clientAttachment != null && clientAttachment.client != null) {
                    clientAttachment.client.write(ByteBuffer.wrap(message)).get();
                } else {
                    printToSender("Market has disconnected.\n");
                }
            } catch (InterruptedException | ExecutionException e) {
                System.err.println(e.getMessage());
            } catch (FixFormatException | FixMessageException e) {
                e.printStackTrace();
            }
        }

        private void printToSender(String msg) throws ExecutionException, InterruptedException {
            ClientAttachment sendingClient = brokers.get(senderID);
            if (sendingClient != null) {
                sendingClient.client.write(ByteBuffer.wrap(msg.getBytes())).get();
            }
        }
    }

    class SendToBroker implements Runnable {
        private String message;
        private String senderID;

        public SendToBroker(String message, String senderID) {
            this.message = message;
            this.senderID = senderID;
        }

        @Override
        public void run() {
            Matcher m = pattern.matcher(message);
            String brokerID;
            String extractedMessage;

            try {
                if (m.find()) {
                    brokerID = m.group(1);
                    extractedMessage = m.group(2) + "\n";
                    ClientAttachment clientAttachment = brokers.get(brokerID);
                    if (clientAttachment != null && clientAttachment.client != null) {
                        clientAttachment.client.write(ByteBuffer.wrap(extractedMessage.getBytes())).get();
                    } else {
                        printToSender("Broker has disconnected.\n");
                    }
                } else {
                    printToSender("Bad message format. usage: \\<id> <your message>.\n");
                }
            } catch (InterruptedException | ExecutionException e) {
                System.err.println(e.getMessage());
            }
        }

        private void printToSender(String msg) throws ExecutionException, InterruptedException {
            ClientAttachment sendingClient = markets.get(senderID);
            if (sendingClient != null) {
                sendingClient.client.write(ByteBuffer.wrap(msg.getBytes())).get();
            }
        }
    }

}