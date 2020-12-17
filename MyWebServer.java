/*
   1. Name: Sarah Semeen
   2. Java version used:  java version "1.8.0_181"
   3. Precise command-line compilation examples / instructions:
    e.g.:
    Compile:
    > javac MyWebServer.java
   4. Precise examples / instructions to run this program:
    Compile:
    > javac MyWebServer.java
    Run:
    > java MyWebServer
    Open FireFox Browser. Enter url: http://localhost:2540/ or http://localhost:2540 to get server's root directory
   5. List of files needed for running the program.
    a. MyWebServer.java
    b. http-streams.txt
    c. serverlog.txt
*/


import java.io.*;  // Get the Input Output libraries
import java.net.*; // Get the Java networking libraries
import java.nio.file.*; //For reading files and paths 

class WebServerWorker extends Thread 
{
	//The class Socket implements client sockets. A socket is an endpoint for communication between two machines.
	Socket socket;
	
	//Creating a constructor for Worker with socket as the argument
	//When a socket(s) is passed to the Worker class, the global variable socket is given the value of the input socket passed as an argument. 
	WebServerWorker (Socket sock)
	{
		socket = sock;
	}
	
	//The method run() from the thread extension is overriden as it is the entry point of new thread.
	// As cited : https://jaxenter.com/learn-from-brilliant-java-programmers-to-create-a-thread-with-implements-runnable-vs-extends-thread-124627.html
	
	public void run()
	{
		//Getting the contents of the passed socket
		//Defining the output of the socket in case an output is expected. Initially set to null. 
        PrintStream socket_output = null;
		//Defining the input of the socket. Initially set to null
		BufferedReader socket_input = null;
		/*Adress to the file http-streams 
		File outputfile = new File("C:/Users/Sarah/Desktop/MS/COURSES/DISTRIBUTED SYSTEMS/Week 5/Sarah_WebServer/http-streams.txt");
		//Writing into the http-streams text file
		PrintStream textfileoutput = null;*/
		
		try 
		{
			//Reading the input from the socket into the socket_input variable
			socket_input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			//Storing and possibly printing the output from the socket into the socket_output variable
			socket_output = new PrintStream(socket.getOutputStream());
			/*putting the output in file 
			textfileoutput = new PrintStream(new FileOutputStream(outputfile));*/
			
			//Initializing a variable to store the value read from the socket input
			String socketdata;
			
			//Reading data from socket - information requested by user
			//We are concerned only about the first line of the data as it contains all the request information we need
			socketdata = socket_input.readLine();
			
			//Reading the request that the browser sends to us in an object GetRequestDetails
            GetRequestDetails browser_request = new GetRequestDetails(socketdata);
			
			//If the input is not empty then Read the socket input and parse to use further 
			if(socketdata != null)
			{
				browser_request.readInputData();
			}
			
			//Now that the request is read we need to make arrangments to send the response data 
			// which is mostly in the form 
			/*HTTP/1.1 301 Moved Permanently
              Date: Sun, 09 Feb 2020 03:22:43 GMT
              Server: Apache
              Location: https://condor.depaul.edu:80/dog.txt
              Content-Length: 244
              Content-Type: text/html; charset=iso-8859-1

              <!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML 2.0//EN">
              <html><head>
              <title>301 Moved Permanently</title>
              </head><body>
              <h1>Moved Permanently</h1>
              <p>The document has moved <a href="https://condor.depaul.edu:80/dog.txt">here</a>.</p>
              </body></html>*/
			  
			//Creating a variable to determine the content header with the the protocol followed by the 200 OK message
			//and the content length and content type; .html is of type text/html, .java and .txt are of the type text/plain
			String responseInformation = "";
			String filetoberead;
			
			MakeResponseToSend responseToSend = new MakeResponseToSend(browser_request);
			
			//response to send 
            //ignoring the /favicon.ico file 
			try
			{
				filetoberead = responseToSend.getBrowserRequestFile();
				
				if (!filetoberead.equals("/favicon.ico"))
				{
					//the response to be sent is generated based on the file sent through the browser
					//Removing the "/" from the file path to just have the file name
					filetoberead = filetoberead.substring(1);
					//System.out.print("\n Substringed file: " +filetoberead);
					//gives the content header to be sent by using the sendMessage method
					responseInformation = responseToSend.sendMessage(filetoberead);
                    
					System.out.println(responseInformation);
					//Sending the response content header to the browser
					socket_output.println(responseInformation);
					System.out.println("\r\n");
					socket_output.println("\r\n");
					
					//Printing the actual contents of the file to the browser
					//If the file is null, then send a cannot read file message
					if(responseToSend.getFileContent() == null)
					{
						socket_output.println("The file could not be found");
					}
                    else
					{
						System.out.print(responseToSend.getFileContent());
						socket_output.println(responseToSend.getFileContent());
					}
					
					System.out.println("\n");
					System.out.println("\nThis is the request that was processed");
					System.out.println(socketdata);
                    
					//printing the full request sent from the browser
			        do
			        {
				        String socketdata_full = socket_input.readLine();
				
				        if(socketdata_full != null)
				        {
					        System.out.println(socketdata_full);
					        System.out.flush();
							socket.close();
				        }
			        } while(true);					
					
					
				}
			}
			catch (Exception exception)
			{
				System.out.println("\n Please send another request via the browser");
			}
			
		}
		
		//Catching any exception that occured while reading from or writing into the socket
		catch(IOException ioe)
		{
			System.out.println(ioe);
		}
		
	}
	
}

class GetRequestDetails
{
	//The object that reads the request that comes in from the browser
	//Reading and parsing it to read the:
	//Example: GET /cat.html HTTP/1.1
	//Request method - GET in our case 
	//File Path - path of the file that we are supposed to read in this case /cat.html
	//protocol - The protocol with which it was sent HTTP/1.1 in this case. 
	
	private String socketdata;
	private String requestype; //Mostly GET in our case
	private String filetoread; //Path of the file the user wants to read
	private String protocol; //protocol used
	
	//Constructor to create the GetRequestDetails 
	GetRequestDetails(String input_data)
	{
		this.socketdata = input_data;
	}
	
	//Reading the input data to seperate out the different segments
	
	public void readInputData()
	{
		//The request made from the browser is supposed to be in the form 
		//GET /cat.html HTTP/1.1 it is sperated by spaces, so splitting them by spaces and reading them into an array
		//The first elemnt of the array is going to be the request method 
		//The second element is the file path 
		//The thord is going to be the protocol
		String[] data = socketdata.split(" ");
		this.requestype = data[0];
		this.filetoread = data[1];
		this.protocol = data[2];
	}
	
	public String getRequestType()
	{
		//System.out.print("\n The request method is" +requestype);
		return requestype;
	}
	
	public String getFile()
	{
		//System.out.println("\n The file path is " +filetoread);
		return filetoread;
	}
	
	public String getProtocol()
	{
		//System.out.println(" The protocol is " +protocol);
		return protocol;
	}
	
	public String getSocketData()
	{
		return socketdata;
	}
	
}

//Method that takes in the browser request information from the GetRequestDetails method and uses it to create 
//the header that is to be sent to the browser

class MakeResponseToSend
{
	//The browser request object is directly imported along with all the data to use for the header 
	private GetRequestDetails requestfrombrowser;
	//The request type recieved from the browser: GET or POST (in our case GET)
	private String browserequestype;
	//The request file to be read, the path or the direct file name
	private String browserequestfile;
	//The request protocol sent in out case,eg. HTTP/1.1 
	private String browserequestprotocol;
	//The complete socketdata recieved without being parsed
	private String browserrequestsocketdata;
	//The respond to be sent, the content header in the form
	//HTTP/1.1 200 OK
	//Content-Length : 1234 da da da 
	//Content-Type: text/plain or text/html depending on the request made
	private String responsetosend;
	//Variable to read the file type requested to determine the Content-Type
	private String responsefileextension;
	//Vraiable to save the Content-Length after determining it 
	private String responsefilesize;
	
	//Constructor that creates a MakeResponseToSend object o send to the browser
    MakeResponseToSend(GetRequestDetails request)
	{
		this.requestfrombrowser = request;
		this.browserequestype = requestfrombrowser.getRequestType();
		this.browserequestfile = requestfrombrowser.getFile();
		this.browserequestprotocol = requestfrombrowser.getProtocol();
		this.browserrequestsocketdata = requestfrombrowser.getSocketData();
	}
    
	//creates the content header 
    public String sendMessage(String file)
	{
		if(browserrequestsocketdata != null)
		{
			//if the filepath sent is a directory then create a dynamic html file to view the files in the 
			//directory
			if(Files.isDirectory(Paths.get(file)))
			{
				responsefileextension = "html";
				//As cited in https://condor.depaul.edu/elliott/435/hw/programs/mywebserver/ReadFiles.java
				//Spotting the root of the directory 
				
				if (file.length() == 0)
				{
					file = "./";
				}
				
				System.out.println("\n This a directory. We are in " + file );
				//Create a directory and its associated html page
				//Creating a html file starting off with creating a stringbuilder so it's easy to append and change around data
				StringBuilder display = new StringBuilder();
				
				//As cited in https://condor.depaul.edu/elliott/435/hw/programs/mywebserver/ReadFiles.java
				//Creating a file structure to save the file path sent 
				File f1 = new File(file);
				//Retrieving a list of files that are in this path 
				File[] fileslist = f1.listFiles();
				
				//System.out.println(fileslist);
				
				//Creating the html page 
				display.append("<html>");
				display.append("<body>");
				
				//Checking if the directory is the root and creating the heading of the Html file
				if(file.equals("./"))
				{
					display.append("<p> Index of  " +file+ "</p>");
				}
				else
				{
					display.append("<p> Index of  /" +file+ "</p>");
					
				}
				 
				
				for(int i=0; i< fileslist.length; i++)
				{
					//System.out.println(fileslist[i]);
					//System.out.println("Is Directory: " +fileslist[i].isDirectory());
					//System.out.println("Is File: " +fileslist[i].isFile());
					//If the file in the fileslist is a directory create a directory shortcut sign
					if(fileslist[i].isDirectory())
					{
						try
						{
							display.append( "[DIR]" + "<a href=\"" + fileslist[i]+ "\">" + fileslist[i].getName() + "</a> <br>");
						}
                        catch(Exception e) { System.out.print("\n") ;}						
					}
					else if (fileslist[i].isFile())
					{
						try
						{
						    display.append( "[TXT]" + "< a href = \"" + fileslist[i] + "\">" +fileslist[i].getName()+ "</a> <br>"); 	
						}
						catch(Exception e){ System.out.println("\n");}
					}
				}
				
				//Closing the html script
				display.append("</body>");
				display.append("</html>");
				
				//getting the content of the file to be sent 
				responsetosend = display.toString();
				
				//getting the content length
				this.responsefilesize = String.valueOf(responsetosend.getBytes().length);
				
			}
			
			//if the filepath is just a regular path then, just take the content and give the file. 
			else
			{
		        //reading the extension by splitting the file path given as a paramter
		        String[] extension = file.split("\\.");
			
			    try
			    {
				    //if the input is from the add-num it will contain the name fake-cgi in that case chang the extension to html
				    if(file.contains("fake-cgi"))
				    {
					    //changing the file extension to html 
					    responsefileextension = "html";
				    }
				    //reading the extension
				    else
				    {
					    responsefileextension = extension[1];
				    }
				    //System.out.println("The file extension" +responsefileextension);
				    //If the extension is txt or java then changing the extension to html 
				    //so that we can read the java file as a text file too
				    if(responsefileextension.equals("txt")|| responsefileextension.equals("java"))
				    {
					    responsefileextension = "plain";
				    }
				    //reading and updating the variable responsetosend with the contents of the file  
				    responsetosend = new String(Files.readAllBytes(Paths.get(file)));
				    //System.out.print(responsetosend);
				    //Calculating the response size based on the byte size 
				    this.responsefilesize = String.valueOf(responsetosend.getBytes().length);
			    }
			    catch(Exception exp)
			    {
				    System.out.println("This file could not be found");
			    }
			}
			
		}
		
		//The final header that is to be sent to the browser
		//System.out.println(browserequestprotocol + " 200 OK \n Content-Length:" +responsefilesize+ "\n Content-Type: text/" +responsefileextension);
		return browserequestprotocol + " 200 OK Content-Length:" +responsefilesize+ " Content-Type: text/" +responsefileextension;	
	}
	
	//returns the request object made from the browser
	public GetRequestDetails getBrowserRequest()
	{
		return requestfrombrowser;
	}
	
	//returns the socket data as is from the browser
	public String getBrowserSocketData()
	{
		return browserrequestsocketdata;
	}
	
	//returns the request type sent GET/POST
	public String getBrowserRequestType()
	{
		return browserequestype;
	}
	
	//returns the file path given from browser
	public String getBrowserRequestFile()
	{
		return browserequestfile;
	}
	
	//sets the file extension based on the input from browser 
	public String setBrowserRequestFileExtension(String extension)
	{
		return this.responsefileextension = extension;
	}
	
	//returns the file extension generated from based on the user input
	public String getBrowserRequestFileExtension()
	{
		return responsefileextension;
	}
	
	//returns the request protocol
	public String getBrowserRequestProtocol()
	{
		return browserequestprotocol;
	}
	
	//returns the contents of the file requested
	public String getFileContent()
	{
		return responsetosend;
	}
}


//Creating the class for InetServer so as to pass the Client signal to worker

public class MyWebServer 
{
	
	public static void main(String a[]) throws IOException 
	{
		//the queue length for the operating system
		int q_len = 6;
		
		//default port number for server is set to 1565
		int port = 2540;
		
		//Initializing a socket
		Socket socket;
		
		//Initializing the server socket 
		ServerSocket server_socket = new ServerSocket(port, q_len);
		
		//Printing the opening statement to the server window
		System.out.println("Sarah Semeen's Port listener running at 2540.\n");
		System.out.println("Please make a request on the browser \n");
		
		while(true)
		{
			//waiting for the client to connect 
			socket = server_socket.accept();
			//Passing the socket and it's details to a Worker and starting the thread so that the run() program is accessible
			new WebServerWorker(socket).start();
		}
	}
}
