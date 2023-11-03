package nl.roboteamtwente.autoref.ui;
import java.util.*;

import javafx.application.Application;

import org.apache.commons.cli.*;

public class AutoRef {

    private static AutoRefController controller;

    public static void main(String[] args) throws Exception {

        Options options = new Options();

        Option wip_opt = new Option("wip", "world-ip", true, "world ip");
        options.addOption(wip_opt);
        Option wp_opt = new Option("wp", "world-port", true, "world port");
        options.addOption(wp_opt);
        Option gcip_opt = new Option("gcip", "gc-ip", true, "game controller ip");
        options.addOption(gcip_opt);
        Option gcport_opt = new Option("gcp", "gc-port", true, "game controller port");
        options.addOption(gcport_opt);
        Option active_opt = new Option("active", "active or passive mode");
        active_opt.setRequired(false);
        options.addOption(active_opt);
        Option headless_opt = new Option("cli", "headless mode");
        headless_opt.setRequired(false);
        options.addOption(headless_opt);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            formatter.printHelp("RTT AutoRef", options);
            System.exit(1);
        }

        String wip = cmd.getOptionValue("wip","127.0.0.1");
        String wp = cmd.getOptionValue("wp","5558");
        String gcip = cmd.getOptionValue("gcip","127.0.0.1");
        String gcport = cmd.getOptionValue("gcp","10007");
        boolean active = cmd.hasOption("active");
        boolean headless = cmd.hasOption("cli");

        if(headless){
            System.out.println("Running Headless");
            controller = new AutoRefController();
            controller.initialize_headless();
            controller.start(wip,wp,gcip,gcport,active,headless);
        }
        else
        {
            Application.launch(AutoRefUi.class,wip,wp,gcip,gcport,
                            String.valueOf(active),String.valueOf(headless));
        }
    }
}
