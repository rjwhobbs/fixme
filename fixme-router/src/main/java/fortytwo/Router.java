package fortytwo;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.List;

public class Router {
    private List<Client> brokerTable;
    private List<Client> marketTable;

    public void acceptBroker() {
        try (final AsynchronousServerSocketChannel brokerChannel = AsynchronousServerSocketChannel.open()) {
            InetSocketAddress hostAddress = new InetSocketAddress("localhost", 5000);
            brokerChannel.bind(hostAddress);
            while (true) {
                brokerChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
                    /**
                     * Invoked when an operation has completed.
                     *
                     * @param result     The result of the I/O operation.
                     * @param attachment
                     */
                    @Override
                    public void completed(AsynchronousSocketChannel result, Object attachment) {
                        if (result.isOpen()) {
                            brokerChannel.accept(null, this);
                            System.out.println("Broker Connected");
                        }
                    }

                    /**
                     * Invoked when an operation fails.
                     *
                     * @param exc        The exception to indicate why the I/O operation failed
                     * @param attachment
                     */
                    @Override
                    public void failed(Throwable exc, Object attachment) {

                    }
                });
                System.out.println("Listening on port 5000");
                System.in.read();
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
                    /**
                     * Invoked when an operation has completed.
                     *
                     * @param result     The result of the I/O operation.
                     * @param attachment
                     */
                    @Override
                    public void completed(AsynchronousSocketChannel result, Object attachment) {
                        if (result.isOpen()) {
                            marketChannel.accept(null, this);
                            System.out.println("Market Connected");
                        }
                    }

                    /**
                     * Invoked when an operation fails.
                     *
                     * @param exc        The exception to indicate why the I/O operation failed
                     * @param attachment
                     */
                    @Override
                    public void failed(Throwable exc, Object attachment) {
                        System.out.println("Something went wrong");
                    }
                });
            }
        } catch (Exception e) {

        }
        System.out.println("Listening on port 5001");
    }


    public static void main( String[] args ) {
        System.out.println( "Hello from router" );
        Router router = new Router();
        //TODO do we need Thread to open multiple ports?
        router.acceptBroker();
        router.acceptMarket();
    }
}

class Client {

}
