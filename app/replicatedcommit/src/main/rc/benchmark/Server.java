/* 
 * Copyright 2017 SpecRPC authors                                                                           
 *                                                                                                                       
 * Licensed under the Apache License, Version 2.0 (the "License");                                                      
 * you may not use this file except in compliance with the License.                                                     
 * You may obtain a copy of the License at                                                                              
 *                                                                                                                      
 *     http://www.apache.org/licenses/LICENSE-2.0                                                                       
 *                                                                                                                      
 * Unless required by applicable law or agreed to in writing, software                                                  
 * distributed under the License is distributed on an "AS IS" BASIS,                                                    
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.                                             
 * See the License for the specific language governing permissions and                                                  
 * limitations under the License. 
 */

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
import rc.server.RcServer;

public class Server {
  
  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  // Arguments
  private static final String ARG_DC_ID = "dc";
  private static final String ARG_SHARD_ID = "shard";
  private static final String ARG_IP = "ip";
  private static final String ARG_PORT = "port";
  private static final String ARG_CONFIG_FILE = "config";
  private static final String ARG_TRADRPC = "t";
  private static final String ARG_SPECRPC = "s";
  private static final String ARG_GRPC = "g";
  
  public static void main(String args[]) {
    // Arguments
    Options optionList = new Options();
    
    // Datacenter ID
    Option dcIdOption = new Option(ARG_DC_ID, true, "Datacenter ID");
    dcIdOption.setRequired(true);
    optionList.addOption(dcIdOption);
    
    // Shard ID
    Option shardIdOption = new Option(ARG_SHARD_ID, true, "shard ID");
    shardIdOption.setRequired(true);
    optionList.addOption(shardIdOption);
    
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
      return;
    }
    
    // Reads in arguments
    String dcId = cmd.getOptionValue(ARG_DC_ID);
    String shardId = cmd.getOptionValue(ARG_SHARD_ID);
    String ip = cmd.getOptionValue(ARG_IP);
    int port = Integer.parseInt(cmd.getOptionValue(ARG_PORT));
    String configFile = cmd.getOptionValue(ARG_CONFIG_FILE);
    Properties config = new Properties();
    try {
      config.load(new FileInputStream(configFile));
    } catch (IOException e) {
      logger.error("Failed to read configuration file: " + configFile);
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
    
    // Requires specifying RPC framework, which overwrites the one in the configuration file.
    if (cmd.hasOption(ARG_TRADRPC)) {
      config.setProperty(RcConstants.RPC_FRAMEWORK_PROPERTY, RcConstants.RPC_FRAMEWORK.TRADRPC + "");
    } else if (cmd.hasOption(ARG_SPECRPC)) {
      config.setProperty(RcConstants.RPC_FRAMEWORK_PROPERTY, RcConstants.RPC_FRAMEWORK.SPECRPC + "");
    } else if (cmd.hasOption(ARG_GRPC)) {
      config.setProperty(RcConstants.RPC_FRAMEWORK_PROPERTY, RcConstants.RPC_FRAMEWORK.GRPC + "");
    } else {
      logger.error("RPC framework is not defined.");
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("Usage:", optionList);
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
    
    RcServer server = new RcServer(dcId, shardId, ip, port, config);
    Thread serverThread = new Thread(server);
    serverThread.start();
    
    logger.debug("RcServer starts id = " + server.serverId + 
        ", dcId = " + server.dcId +
        ", shardId = " + server.shardId +
        ", ip = " + server.ip + 
        ", port = " + server.port + 
        ", configuration = " + configFile);
  }
}
