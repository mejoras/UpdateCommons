package com.indra.isl.malaga;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
 
public class GetCookiePrintAndSetValue {
 
  public static void main(String args[]) throws Exception {
 
    HttpClient client = new HttpClient();
//    client.getParams().setParameter("j_username", "user");
//    client.getParams().setParameter("j_password", "pass");
//	  HttpClient client = createHttpClient();
      client.getState().setCredentials(AuthScope.ANY,new UsernamePasswordCredentials("user","pass"));
//      GetMethod method = new GetMethod("/" + getWebappContext() + "/auth/basic");

 
    GetMethod method = new GetMethod("https://slmaven.indra.es/nexus/service/local/repositories/davinci20/content/libreras-externas/substance/5.2.1/substance-5.2.1.jar");
    method.setDoAuthentication(true);
//    GetMethod method = new GetMethod("http://localhost:8080/");
    try{
      int status = client.executeMethod(method);
//      if(HttpStatus.OK == status){
    	  InputStream in = new BufferedInputStream(method.getResponseBodyAsStream());
    	  OutputStream out = new BufferedOutputStream(new FileOutputStream(new File("substance-5.2.1.jar")));
    	  IOUtils.copy(in,out);
//      }
//      Cookie[] cookies = client.getState().getCookies();
//      for (int i = 0; i < cookies.length; i++) {
//        Cookie cookie = cookies[i];
//        System.err.println(
//          "Cookie: " + cookie.getName() +
//          ", Value: " + cookie.getValue() +
//          ", IsPersistent?: " + cookie.isPersistent() +
//          ", Expiry Date: " + cookie.getExpiryDate() +
//          ", Comment: " + cookie.getComment());
//        }
//      client.executeMethod(method);
    } catch(Exception e) {
      System.err.println(e);
    } finally {
      method.releaseConnection();
    }
  }
}