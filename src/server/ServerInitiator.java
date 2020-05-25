package server;

public class ServerInitiator {

	/**
	 * The default port to listen on.
	 */
	final public static int DEFAULT_PORT = 5555;

	/**
	 * This method is responsible for the creation of 
	 * the server instance (there is no UI in this phase).
	 *
	 * @param args[0] The port number to listen on.  Defaults to 5555 
	 *          if no argument is entered.
	 */
	public static void main(String[] args) {
		int port = 0; //Port to listen on

		try
		{
			port = Integer.parseInt(args[0]); //Get port from command line
		}
		catch(Throwable t)
		{
			port = DEFAULT_PORT; //Set port to 5555
		}

		try 
		{
			RequestsHandler server = new RequestsHandler(port);
			server.listen(); //Start listening for connections
		} 
		catch (Exception ex) 
		{
			ex.printStackTrace();
			//System.out.println("ERROR - Could not listen for clients!");
		}
	}

}
