package micro.benchmark;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import micro.common.MicroConstants;

public class ServerArgs {
  
  private static final Logger logger = LoggerFactory.getLogger(MicroConstants.LOGGER_TYPE);

  // Arguments
  private static final String ARG_ID = "id";
  private static final String ARG_IP = "ip";
  private static final String ARG_PORT = "port";
  private static final String ARG_CONFIG_FILE = "config";
  private static final String ARG_TRADRPC = "t";
  private static final String ARG_SPECRPC = "s";
  private static final String ARG_GRPC = "g";
  
  public static Properties parseArgs(String args[]) {
    // Arguments
    Options optionList = new Options();

    // ID
    Option dcIdOption = new Option(ARG_ID, true, "server ID");
    dcIdOption.setRequired(true);
    optionList.addOption(dcIdOption);

    // Server IP
    Option ipOption = new Option(ARG_IP, true, "server ip");
    ipOption.setRequired(true);
    optionList.addOption(ipOption);

    // Server Port
    Option portOption = new Option(ARG_PORT, true, "server port");
    portOption.setRequired(true);
    optionList.addOption(portOption);

    // Server configuration file
    Option configOption = new Option(ARG_CONFIG_FILE, true, "config");
    configOption.setRequired(true);
    optionList.addOption(configOption);

    // Server RPC frameworks
    Option tradRpcOption = new Option(ARG_TRADRPC, false, "using TradRPC");
    Option specRpcOption = new Option(ARG_SPECRPC, false, "using SpecRPC");
    Option gRpcOption = new Option(ARG_GRPC, false, "using gRPC");
    OptionGroup rpcGroupOption = new OptionGroup();
    rpcGroupOption.addOption(tradRpcOption);
    rpcGroupOption.addOption(specRpcOption);
    rpcGroupOption.addOption(gRpcOption);
    rpcGroupOption.setRequired(true);
    optionList.addOptionGroup(rpcGroupOption);

    // Parses arguments
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(optionList, args);
    } catch (ParseException e) {
      logger.error("Parsing failed.  Error Info: " + e.getMessage());
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("Usage:", optionList);
      System.exit(MicroConstants.INIT_FAIL_ERROR_CODE);
      return null;
    }

    // Reads in arguments
    String id = cmd.getOptionValue(ARG_ID);
    String ip = cmd.getOptionValue(ARG_IP);
    String port = cmd.getOptionValue(ARG_PORT);
    String configFile = cmd.getOptionValue(ARG_CONFIG_FILE);
    
    Properties config = new Properties();
    try {
      config.load(new FileInputStream(configFile));
    } catch (IOException e) {
      logger.error("Failed to read configuration file: " + configFile);
      System.exit(MicroConstants.INIT_FAIL_ERROR_CODE);
    }

    // Requires specifying RPC framework, which overwrites the one in the
    // configuration file.
    if (cmd.hasOption(ARG_TRADRPC)) {
      config.setProperty(MicroConstants.RPC_FRAMEWORK_PROPERTY, MicroConstants.RPC_FRAMEWORK.TRADRPC + "");
    } else if (cmd.hasOption(ARG_SPECRPC)) {
      config.setProperty(MicroConstants.RPC_FRAMEWORK_PROPERTY, MicroConstants.RPC_FRAMEWORK.SPECRPC + "");
    } else if (cmd.hasOption(ARG_GRPC)) {
      config.setProperty(MicroConstants.RPC_FRAMEWORK_PROPERTY, MicroConstants.RPC_FRAMEWORK.GRPC + "");
    } else {
      logger.error("RPC framework is not defined.");
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("Usage:", optionList);
      System.exit(MicroConstants.INIT_FAIL_ERROR_CODE);
    }
    
    // Sets server id, ip, port, and config file location
    config.setProperty(MicroConstants.SERVER_ID_PROPERTY, id);
    config.setProperty(MicroConstants.SERVER_IP_PROPERTY, ip);
    config.setProperty(MicroConstants.SERVER_PORT_PROPERTY, port);
    
    // Random seed
    long rndSeed = Long.parseLong(config.getProperty(MicroConstants.RANDOM_SEED_PROPERTY, MicroConstants.DEFAULT_RANDOM_SEED));
    if (rndSeed == 0) {
      // Dynamic random seed
      logger.debug("Random seed is set as dynamic.");
      rndSeed = System.nanoTime();
    } else {
      // To make sure every server (or client) has a different random seed
      rndSeed += id.hashCode();
    }
    config.setProperty(MicroConstants.RANDOM_SEED_PROPERTY, rndSeed + "");
    
    return config;
  }
}
