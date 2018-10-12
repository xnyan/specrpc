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

package specrpc.iterativeMultiServers;

import java.util.ArrayList;

import specrpc.common.RpcSignature;

public class Transaction {

  private final String ID;
  private ArrayList<RpcSignature> operations;
  private ArrayList<Parameter> rpcParameters;// parameter for operation
  private ArrayList<ILocalCalculation> localCals;

  public Transaction(String ID) {
    this.ID = ID;
    this.operations = new ArrayList<RpcSignature>();
    this.rpcParameters = new ArrayList<Parameter>();
    this.localCals = new ArrayList<ILocalCalculation>();

  }

  public String getID() {
    return this.ID;
  }

  public void addOperation(RpcSignature operation, Parameter arg, ILocalCalculation cal) {
    this.operations.add(operation);
    this.rpcParameters.add(arg);
    this.localCals.add(cal);

  }

  public RpcSignature getOperation(int index) {
    if (index < this.operations.size())
      return this.operations.get(index);
    return null;
  }

  public ArrayList<RpcSignature> getOperations() {
    return this.operations;
  }

  public ArrayList<Parameter> getParameters() {
    return this.rpcParameters;
  }

  public ArrayList<ILocalCalculation> getLocalCals() {
    return this.localCals;
  }

  public int getNumOfOperations() {
    return this.operations.size();
  }
}
