package rc.benchmark;

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

import rc.common.RcConstants;

public class ClientArgs {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  // Arguments
  private static final String ARG_ID = "id";
  private static final String ARG_PORT = "port"; // Read proxy port, SpecRPC use only
  private static final String ARG_CONFIG_FILE = "config";
  private static final String ARG_TRADRPC = "t";
  private static final String ARG_SPECRPC = "s";
  private static final String ARG_GRPC = "g";
  private static final String ARG_SPEC_STAT = "specstat";

  public static Properties parseArgs(String args[]) {
    // Arguments
    Options optionList = new Options();

    // Client ID
    Option idOption = new Option(ARG_ID, true, "client id");
    idOption.setRequired(true);
    optionList.addOption(idOption);

    // Read proxy port for SpecRPC client lib only
    Option portOption = new Option(ARG_PORT, true, "read proxy port, for SpecRPC use only");
    portOption.setRequired(false);
    optionList.addOption(portOption);

    // Configuration file
    Option configOption = new Option(ARG_CONFIG_FILE, true, "configuration file");
    configOption.setRequired(true);
    optionList.addOption(configOption);

    // RPC frameworks
    Option tradRpcOption = new Option(ARG_TRADRPC, false, "using TradRPC");
    Option specRpcOption = new Option(ARG_SPECRPC, false, "using SpecRPC");
    Option gRpcOption = new Option(ARG_GRPC, false, "using gRPC");
    OptionGroup rpcGroupOption = new OptionGroup();
    rpcGroupOption.addOption(tradRpcOption);
    rpcGroupOption.addOption(specRpcOption);
    rpcGroupOption.addOption(gRpcOption);
    rpcGroupOption.setRequired(true);
    optionList.addOptionGroup(rpcGroupOption);
    
    // SpecRPC Statistics Enabled
    Option specStatOption = new Option(ARG_SPEC_STAT, false, "enabling SpecRPC statistics");
    specStatOption.setRequired(false);
    optionList.addOption(specStatOption);

    // Parses arguments
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(optionList, args);
    } catch (ParseException e) {
      logger.error("Parsing failed.  Error Info: " + e.getMessage());
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("Usage:", optionList);
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
      ;
    }

    // Reads in arguments
    String clientId = cmd.getOptionValue(ARG_ID);
    String configFile = cmd.getOptionValue(ARG_CONFIG_FILE);
    Properties config = new Properties();
    try {
      config.load(new FileInputStream(configFile));
    } catch (IOException e) {
      logger.error("Failed to read configuration file: " + configFile);
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }

    // Overwrites client ID
    config.setProperty(RcConstants.CLIENT_LIB_ID_PROPERTY, clientId);

    // Specifies RPC framework, which overwrites the one in the configuration file.
    if (cmd.hasOption(ARG_TRADRPC)) {
      config.setProperty(RcConstants.RPC_FRAMEWORK_PROPERTY, RcConstants.RPC_FRAMEWORK.TRADRPC + "");
    } else if (cmd.hasOption(ARG_SPECRPC)) {
      config.setProperty(RcConstants.RPC_FRAMEWORK_PROPERTY, RcConstants.RPC_FRAMEWORK.SPECRPC + "");
      if (cmd.hasOption(ARG_PORT)) {
        // Overwrites the property set in configuration file.
        String port = cmd.getOptionValue(ARG_PORT);
        config.setProperty(RcConstants.RPC_SPECRPC_READ_PROXY_PORT_PROPERTY, port);
      }
      if (cmd.hasOption(ARG_SPEC_STAT)) {
        config.setProperty(RcConstants.RPC_SPECRPC_STATISTICS_ENABLED_PROPERTY, "true");
      } else {
        // Uses the one in the rc configuration file
      }
    } else if (cmd.hasOption(ARG_GRPC)) {
      config.setProperty(RcConstants.RPC_FRAMEWORK_PROPERTY, RcConstants.RPC_FRAMEWORK.GRPC + "");
    } else {
      logger.error("RPC framework is not defined.");
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("Usage:", optionList);
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }

    return config;
  }
}
