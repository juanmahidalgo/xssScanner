import java.io.IOException;
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
	
	public static void main(String[] args) throws IOException{
		url = args[0]; // the URL 
		numberOfThreads = Integer.parseInt(args[1]); // number of threads to use

		ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads); //threadPool

		System.out.println("URL To scan : " + url);
		System.out.println("Number of threads to run : " + numberOfThreads);
		Document doc = Jsoup.connect(url).get(); // connect and get Doc
	
		internalLinks = new ArrayList<String>();
		internalLinks.add(url);
		getInternalLinks(doc,url);
		System.out.println(internalLinks);

		//List<String> internalLinks = getInternalLinks(doc, url);
		//List<String> analizedLinks = new ArrayList<String>();
		
		for (int i = 0 ; i < internalLinks.size() ; i++){
			System.out.println(" UNICIO EL  THREAD " + i + " : ");
			System.out.println(" con el link : " + internalLinks.get(i) + " : ");
			String completeURL = url;
			if(! url.equals(internalLinks.get(i))){
				completeURL = url + internalLinks.get(i);
			}
			analizerWithThread thread = new analizerWithThread(completeURL);
			executor.execute(thread);
			//internalLinks.remove(i);
			//thread.start();
			//analizedLinks.add(internalLinks.get(i));
		}
		executor.shutdown();
		// Wait until all threads are finish
		while (!executor.isTerminated()) {
 
		}
		System.out.println("\nFinished all threads");
		//for (String link : internalLinks){
		//	System.out.println("WITH NO THREAAD : ");
		//	analizer.analize(link);
		//}
	}
	
	public static void getInternalLinks(Document doc, String urlBase) throws IOException{
		// get links
        //List<String> internalLinks = new ArrayList<String>();
		Elements links = doc.select("a[href]");
		//System.out.println(links);
		for( Element link : links){
			//if(link.attr("href").equals("/")){
			//	continue;
			//}
			String completeLink = urlBase + link.attr("href");
			if(internalLinks.contains(link.attr("href"))){
				continue;
			}
			if(link.attr("href").startsWith("/")){
				internalLinks.add(link.attr("href"));
				System.out.println( link.attr("href") + " ADDED--------- -----------------");
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
	
}



class analizerWithThread implements Runnable {
	private Thread t;
	private final String url;
	private static String xssScript = "<script> alert(1) </script>";	   
	analizerWithThread( String urlToAnalize){
		url = urlToAnalize;
		System.out.println("SE CREA EL THREAD CON EL URL :   " +  url + "");
	}
	public void run() {
	try {
		System.out.println("Dentro del thread  " +  url + "");
		System.out.println(" -- -------- Analizing the sublink : " + url);
	
	  		Document doc = Jsoup.connect(url).get(); // connect and get Doc
	  		Elements form = doc.getElementsByAttribute("method"); // look for the method
			System.out.println("el form es  :" + form);
			
			System.out.println(" AVER SI LO AGARRO BIEN  :" + form.attr("method"));

			for (Element element : form){
				Elements elementsOfForm = element.getElementsByAttribute("name");
				for (Element inputs : elementsOfForm){
					System.out.println(" \n id del elementi1 :" + inputs.id());
					System.out.println(" \n val del mehotd es :" + form.val());

					inputs.val(xssScript);
					System.out.println(" \n nuevo value del elementi1 :" + inputs.val());
					if (form.attr("method").equals("GET") || form.attr("method").equals("GET") ){
						Response response = Jsoup.connect(url)
								.method(Connection.Method.GET)
								.data(inputs.tagName(), xssScript)
								.execute();// connect and get Doc
						Document responseDoc = response.parse();
						System.out.println("la response es: " + responseDoc);
						System.out.println(isScriptInResponse(doc, xssScript));
						if ( isScriptInResponse(responseDoc, xssScript) ){
							System.out.println("SAVE IN DATABASE URL:  "+ url+ "and inputval" + inputs.id() );

							saveIntoDatabase(url, inputs.id());
						}
					}
					if (form.attr("method").equals("post") || form.attr("method").equals("POST") ){
						Document resp = Jsoup.connect(url)
								.data(inputs.tagName(), xssScript)
						  // and other hidden fields which are being passed in post request.
						  .post();
						   System.out.println("-------------- RESPONSE ES : --------- " );

						   System.out.println(resp);
					}
					
				}
			}
	
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
	   public static boolean isScriptInResponse(Document doc, String script){
			Elements elements = doc.getElementsContainingText(script);
			if(elements != null) return true;
			return false;
		}
	   
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
		
	   
	}
