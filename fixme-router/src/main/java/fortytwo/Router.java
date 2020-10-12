package fortytwo;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

final public class Router {
    public static void main( String[] args ) {
        final Server server = new Server();
        Executor pool = Executors.newFixedThreadPool(2);
        pool.execute(new Runnable() {
            @Override
            public void run() {
                server.acceptBroker();
            }
        });
        pool.execute(new Runnable() {
            @Override
            public void run() {
                server.acceptMarket();
            }
        });
    }
}
