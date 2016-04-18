import java.io.BufferedReader;
import java.io.IOException;
import org.jsoup.Connection.Response;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.net.URL;
import java.net.URLConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
	public static String url;
	public static int numberOfThreads;
	public static List<String> internalLinks;
	
	public static void main(String[] args) throws IOException{
		String xssString = "<script> alert(1) </script>"; // script to test
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
//			System.out.println("\n completeLink: " + completeLink);
//
//
//			if(!completeLink.equals(urlBase) ){
//				//System.out.println("\n link: " + url + link.attr("href"));
//				//System.out.println("text : " + link.text());
//				//System.out.println("value : " + link);
//				if (link.attr("href").startsWith("/") & !internalLinks.contains(completeLink)){
//					System.out.println("\n ADDING link: " + urlBase + link.attr("href"));
//					internalLinks.add(completeLink);
//					Document docLink = Jsoup.connect(completeLink).get(); // connect and get Doc
//					internalLinks.add(completeLink);
//					getInternalLinks(docLink, completeLink);
//				}
//			}
//			urlBase = completeLink;

		}
		
		//return internalLinks;
	}
	
	 // JDBC driver name and database URL
	
		
	
	
	
		  
	
	
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
	   
	   static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
		static final String DB_URL = "jdbc:postgresql://localhost/EMP";
		
		//  Database credentials
		static final String USER = "juanhidalgo";
		static final String PASS = "password";
		   
		public static void saveIntoDatabase(String URL, String input) {
			Connection conn = null;
		    Statement stmt = null;
		    try{
		      //STEP 2: Register JDBC driver
		    	Class.forName("org.postgresql.Driver");
		
		    	//STEP 3: Open a connection
		    	System.out.println("Connecting to database...");
		    	conn = (Connection) DriverManager.getConnection(DB_URL,USER,PASS);
		
		    	//STEP 4: Execute a query
		    	System.out.println("Creating statement...");
		    	stmt = ((java.sql.Connection) conn).createStatement();
		    	String sql;
		    	sql = "INSERT INTO URLS (url, input) VALUES (" + URL + "," + input + ")" ;
		    	ResultSet rs = stmt.executeQuery(sql);
		
		    	//STEP 5: Extract data from result set
//		    	while(rs.next()){
//		    		//Retrieve by column name
//		    		int id  = rs.getInt("id");
//		    		int age = rs.getInt("age");
//		    		String first = rs.getString("first");
//		    		String last = rs.getString("last");
	//	
//		    		//Display values
//		    		System.out.print("ID: " + id);
//		    		System.out.print(", Age: " + age);
//		    		System.out.print(", First: " + first);
//		    		System.out.println(", Last: " + last);
//		    	}
		    	//STEP 6: Clean-up environment
		    	rs.close();
		    	stmt.close();
		    	((BufferedReader) conn).close();
		   }catch(SQLException se){
		      //Handle errors for JDBC
		      se.printStackTrace();
		   }catch(Exception e){
		      //Handle errors for Class.forName
		      e.printStackTrace();
		   }finally{
		      //finally block used to close resources
		  try{
		     if(stmt!=null)
		        stmt.close();
		  }catch(SQLException se2){
		  }// nothing we can do
		  try{
		     if(conn!=null)
		        ((java.sql.Connection) conn).close();
		  }catch(SQLException se){
		     se.printStackTrace();
		  }//end finally try
		   }//end try
		}//end main
		
	   
	}
