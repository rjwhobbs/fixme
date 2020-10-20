package fortytwo;

import fortytwo.constants.FixConstants;
import fortytwo.fixexceptions.FixCheckSumException;
import fortytwo.fixexceptions.FixFormatException;
import fortytwo.fixexceptions.FixMessageException;
import fortytwo.utils.FixUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

final class Server {
    private static final Logger log = Logger.getLogger("Logger").getParent();
    private final int MAX_CLIENTS = 999999;
    private final int ROUTER_ID = 100000;
    private static BufferedReader blockerReader;
    private static Executor pool;
    private static int brokersIndex;
    private static int marketsIndex;
    private static HashMap<String, ClientAttachment> brokers;
    private static HashMap<String, ClientAttachment> markets;
    private MessageHandler dispatchOne = new DispatchOne();
    private MessageHandler dispatchTwo = new DispatchTwo();

    public Server() {
        blockerReader = new BufferedReader(new InputStreamReader(System.in));
        pool = Executors.newFixedThreadPool(200);
        brokers = new HashMap<>();
        markets = new HashMap<>();
        brokersIndex = ROUTER_ID + 1;
        marketsIndex = ROUTER_ID + 1;
        dispatchOne.setSuccessor(dispatchTwo);
        dispatchTwo.setSuccessor(null);
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
                            if (brokersIndex < MAX_CLIENTS) {
                                String brokerID = Integer.toString(brokersIndex++);
                                String welcomeMessage =
                                        "Yello, you are now connected to the router, your ID is " + brokerID + "\n";
                                client.write(ByteBuffer.wrap(welcomeMessage.getBytes())).get();
                                ClientAttachment clientAttachment = new ClientAttachment(client, brokerID);
                                brokers.put(brokerID, clientAttachment);
                                log.info("Connected Brokers " + brokers.entrySet());
                                client.read(clientAttachment.buffer, clientAttachment, new BrokerHandler());
                            } else {
                                // TODO send reject message to client
                                System.out.println("Maximum number of clients reached...try again later");
                            }
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
                            log.info("Market Connected");
                            registerMarket(result);
                        }
                    }

                    private void registerMarket(AsynchronousSocketChannel client) {
                        try {
                            if (marketsIndex < MAX_CLIENTS) {
                                String marketID = Integer.toString(marketsIndex++);
                                String welcomeMessage =
                                        "Yello, you are now connected to the router, your ID is " + marketID + "\n";
                                client.write(ByteBuffer.wrap(welcomeMessage.getBytes())).get();
                                ClientAttachment clientAttachment = new ClientAttachment(client, marketID);
                                markets.put(marketID, clientAttachment);
                                log.info("Connected Markets" + brokers.entrySet());
                                client.read(clientAttachment.buffer, clientAttachment, new MarketHandler());
                            } else {
                                // TODO send reject message to client
                                System.out.println("Maximum number of clients reached...try again later");
                            }
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
            log.info(fixMessage.getFixMsgString());
            FixUtils.valCheckSum(fixMessage.getFixMsgString());
            String senderID = fixMessage.msgMap.get(FixConstants.internalSenderIDTag);
            String targetID = fixMessage.msgMap.get(FixConstants.internalTargetIDTag);
            String clientOrdId = fixMessage.msgMap.get(FixConstants.clientOrdIDTag);
            pool.execute(new SendToMarket(message, senderID, targetID, clientOrdId));
        } catch (FixCheckSumException e) {
            System.err.println("Failed checksum " + e.getMessage());
        } catch (FixFormatException e) {
            System.err.println("Fix format exception " + e.getMessage());
        } catch (FixMessageException e) {
            System.err.println("Failed to create Fixed message " + e.getMessage());
        }
    }

    private void sendToBroker(byte[] message) {
        try {
            FixMessage fixMessage = FixMsgFactory.createMsg(message);
            System.out.println(fixMessage.getFixMsgString());
            FixUtils.valCheckSum(fixMessage.getFixMsgString());
            String senderID = fixMessage.msgMap.get(FixConstants.internalSenderIDTag);
            String targetID = fixMessage.msgMap.get(FixConstants.internalTargetIDTag);
            pool.execute(new SendToBroker(message, senderID, targetID));
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
//                    sendToMarket(bytes);
                    dispatchOne.handleMessage(bytes, "broker");
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
//                    sendToBroker(bytes);
                    dispatchOne.handleMessage(bytes, "market");
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
        private String targetID;
        private String clientOrdId;

        SendToMarket(byte[] message, String senderID, String targetID, String clientOrdId) {
            this.message = message;
            this.senderID = senderID;
            this.targetID = targetID;
            this.clientOrdId = clientOrdId;
        }

        @Override
        public void run() {
            try {
                ClientAttachment clientAttachment = markets.get(targetID);
                if (clientAttachment != null && clientAttachment.client != null) {
                    clientAttachment.client.write(ByteBuffer.wrap(message)).get();
                } else {
                    FixMessage fixRejectMsg = FixMsgFactory.createExecRejectedMsg(
                            Integer.toString(ROUTER_ID),
                            senderID,
                            clientOrdId,
                            "Market #" + targetID + " is not available"
                    );
                    printToSender(fixRejectMsg);
                }
            } catch (InterruptedException | ExecutionException e) {
                System.err.println(e.getMessage());
            } catch (FixFormatException e) {
                System.err.println(e.getMessage());
            } catch (FixMessageException e) {
                System.err.println(e.getMessage());
            }
        }

        private void printToSender(FixMessage fixMessage) throws ExecutionException, InterruptedException {
            ClientAttachment sendingClient = brokers.get(senderID);
            if (sendingClient != null) {
                sendingClient.client.write(ByteBuffer.wrap(fixMessage.getRawFixMsgBytes())).get();
            }
        }
    }

    class SendToBroker implements Runnable {
        private byte[] message;
        private String senderID;
        private String targetID;

        public SendToBroker(byte[] message, String senderID, String targetID) {
            this.message = message;
            this.senderID = senderID;
            this.targetID = targetID;
        }

        @Override
        public void run() {
            try {
                ClientAttachment clientAttachment = brokers.get(targetID);
                if (clientAttachment != null && clientAttachment.client != null) {
                    clientAttachment.client.write(ByteBuffer.wrap(message)).get();
                } else {
                    printToSender("Broker has disconnected.\n");
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

    abstract class MessageHandler {
        MessageHandler successor;

        void handleMessage(byte[] message, String from){};

        public void setSuccessor(MessageHandler successor) {
            this.successor = successor;
        }
    }

    class DispatchOne extends MessageHandler {
        @Override
        void handleMessage(byte[] message, String from){
            if (from.equals("broker")) {
                sendToMarket(message);
            }
            else if (successor != null) {
                successor.handleMessage(message, from);
            }
        };
    }

    class DispatchTwo extends MessageHandler {
        void handleMessage(byte[] message, String from){
            if (from.equals("market")) {
                sendToBroker(message);
            }
            else if (successor != null) {
                successor.handleMessage(message, from);
            }
        };
    }

}