import java.io.IOException;

import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.iq80.leveldb.*;

import static org.fusesource.leveldbjni.JniDBFactory.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
	public static String url;
	public static int numberOfThreads;
	public static List<String> internalLinks;
	public static String cookie;
	
	public static void main(String[] args) throws IOException{
		url = args[0]; // the URL 
		numberOfThreads = Integer.parseInt(args[1]); // number of threads to use
		cookie = null;
		if(args.length == 3){
			cookie = args[2];
		}
		if(checkInDatabase(url)){
			System.out.println("This URL has already been analized " );
			String value = getValue(url) ;
			if(value != null){
				System.out.println("This URL has : " + value + " vulerable places to  attack" );
			}
			int valueInt = Integer.parseInt(value);
			for (int i = 0 ; i < valueInt ; i++){
				String search = url + value;
				String input = getValue(search);
				System.out.println("The input : " + input + " is vulerable to  attack" );
			}
			return;
		}
		ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads); //threadPool

		System.out.println("URL To scan : " + url);
		System.out.println("Number of threads to run : " + numberOfThreads);
		Document doc = Jsoup.connect(url).get(); // connect and get Doc
	
		internalLinks = new ArrayList<String>();
		internalLinks.add(url);
		getInternalLinks(doc,url);
		//System.out.println(internalLinks);
		for (int i = 0 ; i < internalLinks.size() ; i++){
			String completeURL = url;
			if(! url.equals(internalLinks.get(i))){
				completeURL = url + internalLinks.get(i);
			}
			analizerWithThread thread = new analizerWithThread(completeURL, cookie);
			executor.execute(thread);
		}
		executor.shutdown();
		// Wait until all threads are finish
		while (!executor.isTerminated()) {
 
		}
		System.out.println("\nFinished all threads");
	}
	
	public static void getInternalLinks(Document doc, String urlBase) throws IOException{
		Elements links = doc.select("a[href]");
		for( Element link : links){
			String completeLink = urlBase + link.attr("href");
			if(internalLinks.contains(link.attr("href"))){
				continue;
			}
			if(link.attr("href").startsWith("/")){
				System.out.println("\n Adding the internal link:" + link.attr("href"));
				internalLinks.add(link.attr("href"));
				System.out.println( link.attr("href"));
				Document docLink = Jsoup.connect(completeLink).get(); // connect and get Doc
				getInternalLinks(docLink, urlBase);
			}
		}
	}
	public static boolean isScriptInResponse(Document doc, String script){
		Elements elements = doc.getElementsContainingText(script);
		if(elements != null) return true;
		return false;
	}
	
	public static String getValue(String URL) throws IOException {
		Options options = new Options();
		options.createIfMissing(true);
		DB db = factory.open(new File("example"), options);
		try {
			String value = asString(db.get(bytes(URL)));
			return value;
		} finally {
		  db.close();
		}
	}
	
	public static boolean checkInDatabase(String URL) throws IOException {
		Options options = new Options();
		options.createIfMissing(true);
		DB db = factory.open(new File("example"), options);
		try {
			String value = asString(db.get(bytes(URL)));
			if ( value != null){
				return true;
			}
			return  false;
		} finally {
		  db.close();
		}
	}
}


class analizerWithThread implements Runnable {
	private Thread t;
	private final String url;
	private final String cookie;
	private static String sessionID;
	private static String xssScript = "<script> alert(1) </script>"; // malicious script	   
	analizerWithThread( String urlToAnalize, String cookieToUse){
		url = urlToAnalize;
		cookie = cookieToUse;
	}
	public void run() {
	try {
		int cont = 0;
		sessionID = null;
		List<String> vulnerableInputs = new ArrayList<String>();
		System.out.println(" --- Analizing : " + url + " ---");
	  	Document doc = Jsoup.connect(url).get(); // connect and get Doc
	  	Elements form = doc.getElementsByAttribute("method"); // look for the method			
		for (Element element : form){
			Elements elementsOfForm = element.getElementsByAttribute("name");
			for (Element inputs : elementsOfForm){
				System.out.println(" \n Analizing Input ID = :" + inputs.id());
				inputs.val(xssScript);
				if (form.attr("method").equals("GET") || form.attr("method").equals("GET") ){
					Response response;
					if(sessionID == null){
						response = Jsoup.connect(url)
							.method(Connection.Method.GET)
							.data(inputs.tagName(), xssScript)
							.execute();// connect and get Doc
					}
					else{
						response = Jsoup.connect(url)
								.method(Connection.Method.GET)
								.data(inputs.tagName(), xssScript)
								.cookie(cookie, sessionID)
								.execute();// connect and get Doc
						
					}
					Document responseDoc = response.parse();
					
					//System.out.println(isScriptInResponse(doc, xssScript));
					if ( isScriptInResponse(responseDoc, xssScript) ){
						cont++; // add 1 to the total of vulnrable places
						vulnerableInputs.add(inputs.id());
						System.out.println("The URL:  "+ url+ " is vulnerable to a XSS attack in the and input: " + inputs.id() );
						System.out.println("Saved in Database ");
						writeInDatabase(url, vulnerableInputs);
						//saveIntoDatabase(url, inputs.id());
					}
				}
				if (form.attr("method").equals("post") || form.attr("method").equals("POST") ){
					Response resp;
					if(sessionID == null){
						resp = Jsoup.connect(url)
								.data(inputs.tagName(), xssScript)
								.method(Method.POST)
								.execute();
					}
					else {
						resp = Jsoup.connect(url)
								.data(inputs.tagName(), xssScript)
								.method(Method.POST)
								.cookie(cookie, sessionID)
								.execute();
					}
					  // System.out.println(resp);
					Document responseDoc = resp.parse();
					if(cookie != null){
						sessionID = resp.cookie(cookie);
					}

					if ( isScriptInResponse(responseDoc, xssScript) ){
						System.out.println("The URL:  "+ url+ " is vulnerable to a XSS attack in the and input: " + inputs.id() );
						System.out.println("Saved in Database ");
						saveIntoDatabase(url, inputs.id());
					}	
				}
					
				}
			}
			System.out.println("\n --- Finish analizing : " + url + " ---");

	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Thread " +  url + " exiting.");
	   }
	   
	   public void start (){
	      System.out.println("Starting " +  url );
		      if (t == null){
		         t = new Thread (this, url);
		         t.start ();
		      }
	   }
	   // function that checks if the script is in the response, then is vulnerable to an attack
	   public static boolean isScriptInResponse(Document doc, String script){
			Elements elements = doc.getElementsContainingText(script);
			if(elements != null) return true;
			return false;
		}
	   // saves the URL and the input where is the attack is posible in a DB
		public static void saveIntoDatabase(String URL, String input) throws IOException {
			Options options = new Options();
			options.createIfMissing(true);
			DB db = factory.open(new File("example"), options);
			try {
				db.put(bytes(URL), bytes(input));
			} finally {
			  db.close();
			}
		}
		public static void writeInDatabase(String URL, List<String> vulnerableInputs) throws IOException {
			Options options = new Options();
			options.createIfMissing(true);
			DB db = factory.open(new File("example"), options);
			try {
				String size = Integer.toString(vulnerableInputs.size());
				db.put(bytes(URL), bytes(size));
				int aux = 1;
				for (String element : vulnerableInputs){
					db.put(bytes(URL+aux),bytes( element));
					String urlToWrite = URL + aux;
					db.put(bytes(urlToWrite), bytes(element));
					//System.out.println("URL:" +  urlToWrite + "element :" + element + "written" );
					aux++;
				}
				
			} finally {
			  db.close();
			}
		}
		

		
	   
	}
