package py.com.ccp.eapn;

import org.apache.camel.main.Main;
import py.com.ccp.eapn.route.StartupRoute;

public final class App {

    private App() {
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.configure().addRoutesBuilder(new StartupRoute());
        main.run(args);
    }
}