package fortytwo;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Server {
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
                            System.out.println("Broker Connected");
                            registerBroker(result);
                        }
                    }

                    private void registerBroker(AsynchronousSocketChannel client) {
                        try {
                            //TODO generate unique six digit ID
                            String brokerID = Integer.toString(++brokersIndex);
                            String welcomeMessage = "Yellow, you are now connected to the router, your ID is " + brokerID;

                            client.write(ByteBuffer.wrap(welcomeMessage.getBytes())).get();
                            ClientAttachment clientAttachment = new ClientAttachment(client, brokerID);
                            //Debug
                            System.out.println(brokers.entrySet());
                            client.read(clientAttachment.buffer, clientAttachment, new BrokerHandler());
                        } catch (InterruptedException | ExecutionException e) {
//                            e.printStackTrace();
                            System.err.println("Something went wrong while trying to register a broker");
                        }
                    }

                    @Override
                    public void failed(Throwable exc, Object attachment) {

                    }
                });
                System.out.println("Listening on port 5000");
                blocker();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                        }
                    }

                    @Override
                    public void failed(Throwable exc, Object attachment) {
                        System.out.println("Something went wrong");
                    }
                });
                System.out.println("Listening on port 5001");
                blocker();
            }
        } catch (Exception e) {

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

    static class BrokerHandler implements CompletionHandler<Integer, ClientAttachment> {
        @Override
        public void completed(Integer result, ClientAttachment attachment) {
            try {
                if (result != -1) {
                    attachment.buffer.flip();
                    int limit = attachment.buffer.limit();
                    byte[] bytes = new byte[limit];
                    attachment.buffer.get(bytes, 0, limit);
                    String line = new String(bytes);
                    //Debug
                    System.out.println(line);
                    sendToMarket(line, attachment.id);
                    attachment.buffer.clear();
                    attachment.client.read(attachment.buffer, attachment, this);
                } else {
                    attachment.client.close();
                    attachment.client = null;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendToMarket(String message, String senderID) {
            pool.execute(new SendToMarket(message, senderID));
        }

        @Override
        public void failed(Throwable exc, ClientAttachment attachment) {
            System.err.println("Failed method in BrokerHandler called: " + exc.getMessage());
        }

        class SendToMarket implements Runnable {
            private String message;
            private String senderID;

            SendToMarket(String message, String senderID) {
                this.message = message.trim();
                this.senderID = senderID;
            }

            @Override
            public void run() {
                Matcher m = pattern.matcher(message);
                String marketID;
                String extractedMessage;

                try {
                    if (m.find()) {
                        marketID = m.group(1);
                        extractedMessage = m.group(2) + "\n";
                        ClientAttachment clientAttachment = markets.get(marketID);
                        if (clientAttachment != null && clientAttachment.client != null) {
                            clientAttachment.client.write(ByteBuffer.wrap(extractedMessage.getBytes())).get();
                        } else {
                            printToSender("Market has disconnected.\n");
                        }

                    } else {
                        printToSender("Bad message format. usage: \\<id> <your message>.\n");
                    }
                } catch (InterruptedException | ExecutionException e) {
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
    }
}