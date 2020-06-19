package server;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import configuration.Configuration;
import cor.organizer.CorFactory;
import cor.organizer.IOrganizer;
import db.services.Services;
import messages.Header.RequestType;
import messages.Message;
import messages.Request;
import messages.Response;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import pool.ObjectPool;

/**
 * @author shaielb
 *
 */
@SuppressWarnings("unchecked")
public class RequestsHandler extends AbstractServer  {

	//Class variables *************************************************
	/**
	 * 
	 */
	private Boolean _fromJar = true;

	/**
	 * 
	 */
	private Map<String, Object> _corChains;

	/**
	 * 
	 */
	private IOrganizer<RequestType, Message> _corOrganizer;

	/**
	 * 
	 */
	private ObjectPool<Message> _messagesPool = new ObjectPool<Message>(() -> new Message());

	//Constructors ****************************************************

	/**
	 * Constructs an instance of the echo server.
	 *
	 * @param port The port number to connect on.
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	/**
	 * @param port
	 * @throws Exception
	 */
	public RequestsHandler(int port) throws Exception 
	{
		super(port);
		constructCor();
		Services.initialize();
	}

	/**
	 * @throws Exception
	 */
	private void constructCor() throws Exception {
		_corOrganizer = CorFactory.createOrganizer();

		String lp = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation()
				.toURI()).getPath();
		_fromJar = lp.trim().endsWith(".jar");

		if (_fromJar) {
			InputStream in = getClass().getResourceAsStream("/Configurations/components.xml");
			_corOrganizer.getIocContainer().register(in, "components");

			in = getClass().getResourceAsStream("/Configurations/confuration.xml");
			_corChains = (Map<String, Object>) Configuration.configuration(in).get("chains");
		}
		else {
			String localPath = System.getProperty("user.dir");
			String xml = String.format("%s\\Configurations\\components.xml", localPath);
			_corOrganizer.getIocContainer().register(new File(xml), "components");

			localPath = System.getProperty("user.dir");
			xml = String.format("%s\\Configurations\\confuration.xml", localPath);
			_corChains = (Map<String, Object>) Configuration.configuration(xml).get("chains");
		}

		Map<RequestType, List<String>> map = new HashMap<RequestType, List<String>>();
		for (Entry<String, Object> entry : _corChains.entrySet()) {
			try {
				map.put(RequestType.valueOf(entry.getKey()), Arrays.asList(((String) entry.getValue()).split(",")));
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			}
		}

		_corOrganizer.initialize(map);
	}


	//Instance methods ************************************************

	/**
	 * This method handles any messages received from the client.
	 *
	 * @param msg The message received from the client.
	 * @param client The connection from which the message originated.
	 */
	/**
	 *
	 */
	@Override
	public void handleMessageFromClient(Object msg, ConnectionToClient client)
	{
		System.out.println("Message received: " + msg + " from " + client);
		Request request = (Request) msg;
		Message message = _messagesPool.pop();
		message.setRequest(request);
		_corOrganizer.execute(request.getHeader().getType(), message);
		try
		{
			Response response = message.getResponse();
			System.out.println("Sending Message response: " + response + " to " + client);
			client.sendToClient(response);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		try {
			_messagesPool.push(message);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method overrides the one in the superclass.  Called
	 * when the server starts listening for connections.
	 */
	/**
	 *
	 */
	protected void serverStarted()
	{
		System.out.println
		("Server listening for connections on port " + getPort());
	}

	/**
	 * This method overrides the one in the superclass.  Called
	 * when the server stops listening for connections.
	 */
	/**
	 *
	 */
	protected void serverStopped()
	{
		System.out.println
		("Server has stopped listening for connections.");
	}

	/**
	 * @return
	 */
	public IOrganizer<RequestType, Message> getCorOrganizer() {
		return _corOrganizer;
	}

	//Class methods ***************************************************
}
