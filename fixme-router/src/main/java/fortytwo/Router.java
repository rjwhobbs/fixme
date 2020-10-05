package fortytwo;

public class Router {
    public static void main( String[] args ) {
        System.out.println( "Hello from router" );
        FixEngine.testOne();
        FixEngine fixEngine = new FixEngine();
        fixEngine.testTwo();
    }
}
