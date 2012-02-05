package com.gadgetworks.codeshelf.web.websocket;

import java.io.IOException;

/**
 * Implemented by <tt>WebSocketClient</tt> and <tt>WebSocketServer</tt>.
 * The methods within are called by <tt>WebSocket</tt>.
 * 
 * @author Nathan Rajlich
 */
public interface WebSocketListener {

	/**
	 * Called when the socket connection is first established, and the WebSocket
	 * handshake has been recieved.
	 */
	HandshakeBuilder onHandshakeRecievedAsServer(WebSocket conn, Draft draft, Handshakedata request) throws IOException;

	boolean onHandshakeRecievedAsClient(WebSocket conn, Handshakedata request, Handshakedata response) throws IOException;

	/**
	 * Called when an entire text frame has been recieved. Do whatever you want
	 * here...
	 * 
	 * @param conn
	 *            The <tt>WebSocket</tt> instance this event is occuring on.
	 * @param message
	 *            The UTF-8 decoded message that was recieved.
	 */
	void onMessage(WebSocket conn, String message);

	void onMessage(WebSocket conn, byte[] blob);

	/**
	 * Called after <var>onHandshakeRecieved</var> returns <var>true</var>.
	 * Indicates that a complete WebSocket connection has been established,
	 * and we are ready to send/recieve data.
	 * 
	 * @param conn
	 *            The <tt>WebSocket</tt> instance this event is occuring on.
	 */
	void onOpen(WebSocket conn);

	/**
	 * Called after <tt>WebSocket#close</tt> is explicity called, or when the
	 * other end of the WebSocket connection is closed.
	 * 
	 * @param conn
	 *            The <tt>WebSocket</tt> instance this event is occuring on.
	 */
	void onClose(WebSocket conn);

	void onError(WebSocket conn, Exception ex);

	void onPing(WebSocket conn, Framedata f);

	void onPong(WebSocket conn, Framedata f);

	String getFlashPolicy(WebSocket conn);

	void onWriteDemand(WebSocket conn);
}
