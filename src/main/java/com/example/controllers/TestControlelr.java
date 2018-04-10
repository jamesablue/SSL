package com.example.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.example.SSL.SSLConfig;

@RestController
@RequestMapping("/")
public class TestControlelr {
	//private static final Logger logger = Logger.getLogger("SSLDemo");
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private SSLConfig sslConfig;
	
	@Value("$targeturl")
	private String targeturl;
	
	@Value("$targetport")
	private String targetport;
	
	@RequestMapping("test")
	public String test() {
		return "test";
	}
	
	@RequestMapping("") 
	public String root() {
		return "This is the root;";
	}
	
	@RequestMapping("/sendToSSL2")
	public String sendToSSL2()
	{
		try
		{
			String uri = "https://" + targeturl + ":" + targetport + "/api/v1/getMessage";
			String message = "message1";
			
			HttpHeaders requestHeaders = new HttpHeaders();
			requestHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
			HttpEntity<String> h = new HttpEntity<>(message, requestHeaders);
			
			CloseableHttpClient httpClient = this.sslConfig.httpClient();
			
			HttpGet getTest = new HttpGet("https://localhost:9092/api/v1/test");
			
			HttpPost postMessage = new HttpPost(uri);
			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			urlParameters.add(new BasicNameValuePair("message","message from SSL1"));
			
			
			// New stuff
			String response = restTemplate.postForObject(uri, h, String.class);
			
			System.out.println(response);			
			// End New stuff
			
			
			HttpResponse resp = httpClient.execute(postMessage);
			
			return getStringFromInputStream(resp.getEntity().getContent());
			
			//httpClient.e
			
			//String response = this.sslConfig.restTemplate().postForObject(uri, h, String.class);
			
			//return response;
		}
		catch (Exception e) 
		{
			System.out.println("sendToSSL: " + e.toString());
			return e.toString();
		}
	}
	
	@RequestMapping("/sendMessage")
	public String sendMessage()
	{
		try
		{
			URL url = new URL("https://localhost:9092/api/v1/getMessage");
			SSLSocketFactory sslSocketFactory;
			
			HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
			
			//conn.setSSLSocketFactory(sslSocketFactory);
			
			return null;
		}
		catch (Exception e) 
		{
			System.out.println("sendToSSL: " + e.toString());
			return e.toString();
		}
	}
	
	private static String getStringFromInputStream(InputStream is) {

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String line;
		try {

			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return sb.toString();

	}
}
