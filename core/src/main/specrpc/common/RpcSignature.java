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

package specrpc.common;

import java.util.Collection;
import java.util.LinkedList;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

public class RpcSignature {
  private static final String SEPARATOR = ";";
  public final String hostClass;
  public final String methodName;
  public final Class<?> returnType;
  public final Class<?>[] argTypes;

  public RpcSignature(String hostClass, String methodName, Class<?> returnType, Class<?>... argTypes) {
    this.hostClass = hostClass;
    this.methodName = methodName;
    this.returnType = returnType;
    this.argTypes = argTypes;
  }

  public RpcSignature(JsonElement jsonSign) throws JsonSyntaxException, ClassNotFoundException {
    Gson gson = new Gson();

    JsonArray array = gson.fromJson(jsonSign, JsonArray.class);
    this.hostClass = gson.fromJson(array.get(0), String.class);
    this.methodName = gson.fromJson(array.get(1), String.class);
    this.returnType = Class.forName(gson.fromJson(array.get(2), String.class));
    JsonArray jsonArgTypes = gson.fromJson(array.get(3), JsonArray.class);
    this.argTypes = new Class<?>[jsonArgTypes.size()];
    for (int i = 0; i < this.argTypes.length; i++) {
      this.argTypes[i] = Class.forName(gson.fromJson(jsonArgTypes.get(i), String.class));
    }
  }

  public Object[] parseArgs(JsonElement jsonArgs) {
    Object[] args = new Object[this.argTypes.length];
    Gson gson = new Gson();

    JsonArray array = gson.fromJson(jsonArgs, JsonArray.class);
    for (int i = 0; i < this.argTypes.length; i++) {
      args[i] = gson.fromJson(array.get(i), argTypes[i]);
    }
    return args;
  }

  public Collection<Object> toArray() {
    Collection<Object> result = new LinkedList<Object>();
    result.add(hostClass);
    result.add(methodName);
    result.add(returnType.getName());
    Collection<String> argTypesCollection = new LinkedList<String>();
    for (int i = 0; i < argTypes.length; i++) {
      argTypesCollection.add(argTypes[i].getName());
    }
    result.add(argTypesCollection);
    return result;
  }

  public String toString() {
    String id = returnType.getName() + SEPARATOR + hostClass + ";" + methodName;
    for (Class<?> type : this.argTypes) {
      id += SEPARATOR + type.getName();
    }
    return id;
  }

  @Override
  public boolean equals(Object signature2) {
    return this.toString().equals(signature2.toString());
  }

  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }
}
