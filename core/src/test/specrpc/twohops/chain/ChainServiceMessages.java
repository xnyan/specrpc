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

package specrpc.twohops.chain;

public class ChainServiceMessages {
  public static final String END_SERVER_RESPONSE_PREFIX = "End Chain Response ";
  public static final String END_SERVER_SPEC_RESPONSE_PREFIX_1 = "End Spec Chain Response 1st ";
  public static final String END_SERVER_SPEC_RESPONSE_PREFIX_2 = "End Spec Chain Response 2nd ";
  public static final String END_SERVER_SPEC_RESPONSE_PREFIX_3 = "End Spec Chain Response 3rd ";
  public static final String END_SERVER_SPEC_RESPONSE_PREFIX_4 = "End Spec Chain Response 4th ";
  public static final String END_SERVER_EXCEPTION_PREFIX = "End Chain Exception ";

  public static final String MID_SERVER_RESPONSE_PREFIX = "Middle ";
  public static final String MID_SERVER_SPEC_RESPONSE_PREFIX_1 = "Middle Spec 1st ";
  public static final String MID_SERVER_SPEC_RESPONSE_PREFIX_2 = "Middle Spec 2nd ";
  public static final String MID_SERVER_SPEC_RESPONSE_PREFIX_3 = "Middle Spec 3rd ";
  public static final String MID_SERVER_SPEC_RESPONSE_PREFIX_4 = "Middle Spec 4th ";
  public static final String MID_SERVER_EXCEPTION_PREFIX = "Middle Exception ";

  public static final String MID_SERVER_CORRECT_SPECULATION = END_SERVER_RESPONSE_PREFIX + ChainClient.REQUEST_VALUE;
  public static final String MID_SERVER_INCORRECT_SPECULATION = "Middle Server Incorrect Speculation Value";
}
