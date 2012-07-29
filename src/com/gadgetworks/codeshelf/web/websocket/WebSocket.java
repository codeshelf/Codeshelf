package com.gadgetworks.codeshelf.web.websocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.web.websocket.Draft.HandshakeState;
import com.gadgetworks.codeshelf.web.websocket.Framedata.Opcode;
import com.gadgetworks.codeshelf.web.websocket.drafts.Draft_10;
import com.gadgetworks.codeshelf.web.websocket.drafts.Draft_17;
import com.gadgetworks.codeshelf.web.websocket.drafts.Draft_75;
import com.gadgetworks.codeshelf.web.websocket.drafts.Draft_76;
import com.gadgetworks.codeshelf.web.websocket.exceptions.IncompleteHandshakeException;
import com.gadgetworks.codeshelf.web.websocket.exceptions.InvalidDataException;
import com.gadgetworks.codeshelf.web.websocket.exceptions.InvalidFrameException;
import com.gadgetworks.codeshelf.web.websocket.exceptions.InvalidHandshakeException;

public final class WebSocket implements IWebSocket {

	public enum Role {
		CLIENT,
		SERVER
	}

	/**
	 * The default port of WebSockets, as defined in the spec. If the nullary
	 * constructor is used, DEFAULT_PORT will be the port the WebSocketServer
	 * is binded to. Note that ports under 1024 usually require root permissions.
	 */
	public static final int				DEFAULT_PORT			= 80;

	private static final Log			LOGGER					= LogFactory.getLog(WebSocket.class);

	private static final byte[]			FLASH_POLICY_REQUEST	= Charsetfunctions.utf8Bytes("<policy-file-request/>");
	private static int					BUFFER_SIZE				= 65558;
	private static int					BUFFER_QUEUE_SIZE		= 1024;

	// Internally used to determine whether to receive data as part of the remote handshake, or as part of a text frame.
	private boolean						mIsHandshakeComplete;
	private boolean						mIsHandshakeClosed;
	// The listener to notify of WebSocket events.
	private IWebSocketListener			mWebSocketListener;
	// Buffer where data is read to from the socket
	private ByteBuffer					mSocketBuffer;
	// Queue of buffers that need to be sent to the client.
	private BlockingQueue<ByteBuffer>	mBufferQueue;
	private Draft						mDraft;
	private Role						mRole;
	private Framedata					mCurrentFrame;
	private Handshakedata				mHandshakeRequest;
	private List<Draft>					mKnownDraftsList;
	private int							mFlashPolicyIndex;
	private SocketChannel				mSocketChannel;

	/**
	 * Used in {@link WebSocketServer} and {@link WebSocketClient}.
	 * 
	 * @param socketChannel
	 *            The <tt>SocketChannel</tt> instance to read and
	 *            write to. The channel should already be registered
	 *            with a Selector before construction of this object.
	 * @param inListener
	 *            The {@link IWebSocketListener} to notify of events when
	 *            they occur.
	 */
	public WebSocket(final IWebSocketListener inListener, final Draft inDraft, final SocketChannel inSocketChannel) {
		init(inListener, inDraft, inSocketChannel);
	}

	public WebSocket(final IWebSocketListener inListener, final List<Draft> inDraftList, final SocketChannel inSocketChannel) {
		init(inListener, null, inSocketChannel);
		mRole = Role.SERVER;
		if (mKnownDraftsList == null || mKnownDraftsList.isEmpty()) {
			mKnownDraftsList = new ArrayList<Draft>(1);
			mKnownDraftsList.add(new Draft_17());
			mKnownDraftsList.add(new Draft_10());
			mKnownDraftsList.add(new Draft_76());
			mKnownDraftsList.add(new Draft_75());
		} else {
			mKnownDraftsList = inDraftList;
		}
	}

	private void init(IWebSocketListener inListener, Draft inDraft, SocketChannel inSocketChannel) {
		mSocketChannel = inSocketChannel;
		mBufferQueue = new LinkedBlockingQueue<ByteBuffer>(BUFFER_QUEUE_SIZE);
		mSocketBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		mSocketBuffer.flip();
		mWebSocketListener = inListener;
		mRole = Role.CLIENT;
		mDraft = inDraft;
	}

	/**
	 * Should be called when a Selector has a key that is writable for this
	 * WebSocket's SocketChannel connection.
	 * 
	 * @throws IOException
	 *             When socket related I/O errors occur.
	 * @throws InterruptedException
	 */
	public void handleRead() throws InterruptedException, IOException {
		if (!mSocketBuffer.hasRemaining()) {
			mSocketBuffer.rewind();
			mSocketBuffer.limit(mSocketBuffer.capacity());
			if (mSocketChannel.read(mSocketBuffer) == -1) {
				close();
			}

			mSocketBuffer.flip();
		}

		if (mSocketBuffer.hasRemaining()) {
			LOGGER.debug("process(" + mSocketBuffer.remaining() + "): {"
					+ (mSocketBuffer.remaining() > 1000 ? "too big to display" : new String(mSocketBuffer.array(), mSocketBuffer.position(), mSocketBuffer.remaining())) + "}");
			if (!mIsHandshakeComplete) {
				Handshakedata handshake;
				HandshakeState handshakestate = null;

				handshakestate = isFlashEdgeCase(mSocketBuffer);
				if (handshakestate == HandshakeState.MATCHED) {
					channelWrite(ByteBuffer.wrap(Charsetfunctions.utf8Bytes(mWebSocketListener.getFlashPolicy(this))));
					return;
				}
				mSocketBuffer.mark();
				try {
					if (mRole == Role.SERVER) {
						if (mDraft == null) {
							for (Draft d : mKnownDraftsList) {
								try {
									d.setParseMode(mRole);
									mSocketBuffer.reset();
									handshake = d.translateHandshake(mSocketBuffer);
									handshakestate = d.acceptHandshakeAsServer(handshake);
									if (handshakestate == HandshakeState.MATCHED) {
										HandshakeBuilder response = mWebSocketListener.onHandshakeRecievedAsServer(this, d, handshake);
										channelWrite(d.createHandshake(d.postProcessHandshakeResponseAsServer(handshake, response), mRole));
										mDraft = d;
										open();
										handleRead();
										return;
									} else if (handshakestate == HandshakeState.MATCHING) {
										if (mDraft != null) {
											throw new InvalidHandshakeException("multible drafts matching");
										}
										mDraft = d;
									}
								} catch (InvalidHandshakeException e) {
									// go on with an other draft
								} catch (IncompleteHandshakeException e) {
									if (mSocketBuffer.limit() == mSocketBuffer.capacity()) {
										abort("socketBuffer to small");
									}
									// read more bytes for the handshake
									mSocketBuffer.position(mSocketBuffer.limit());
									mSocketBuffer.limit(mSocketBuffer.capacity());
									return;
								}
							}
							if (mDraft == null) {
								abort("no draft matches");
							}
							return;
						} else {
							// special case for multiple step handshakes
							handshake = mDraft.translateHandshake(mSocketBuffer);
							handshakestate = mDraft.acceptHandshakeAsServer(handshake);

							if (handshakestate == HandshakeState.MATCHED) {
								open();
								handleRead();
							} else if (handshakestate != HandshakeState.MATCHING) {
								abort("the handshake did finaly not match");
							}
							return;
						}
					} else if (mRole == Role.CLIENT) {
						mDraft.setParseMode(mRole);
						handshake = mDraft.translateHandshake(mSocketBuffer);
						handshakestate = mDraft.acceptHandshakeAsClient(mHandshakeRequest, handshake);
						if (handshakestate == HandshakeState.MATCHED) {
							open();
							handleRead();
						} else if (handshakestate == HandshakeState.MATCHING) {
							return;
						} else {
							abort("draft " + mDraft.getClass().getSimpleName() + " or server refuses handshake");
						}
					}
				} catch (InvalidHandshakeException e) {
					abort("draft " + mDraft + " refuses handshake: " + e.getMessage());
				}
			} else {
				// Receiving frames
				List<Framedata> frames;
				try {
					frames = mDraft.translateFrame(mSocketBuffer);
				} catch (InvalidDataException e1) {
					abort(/*"detected protocol violations"*/);
					return;
				}
				for (Framedata f : frames) {
					LOGGER.debug("matched frame: " + f);
					Opcode curop = f.getOpcode();
					if (curop == null)// Ignore undefined opcodes
						continue;
					else if (curop == Opcode.CLOSING) {
						sendFrame(new FramedataImpl1(f));
						close();
						continue;
					} else if (curop == Opcode.PING) {
						mWebSocketListener.onPing(this, f);
						continue;
					} else if (curop == Opcode.PONG) {
						mWebSocketListener.onPong(this, f);
						continue;
					}
					if (mCurrentFrame == null) {
						if (f.isFin()) {
							if (f.getOpcode() == Opcode.TEXT) {
								mWebSocketListener.onMessage(this, Charsetfunctions.stingUtf8(f.getPayloadData()));
							} else if (f.getOpcode() == Opcode.BINARY) {
								mWebSocketListener.onMessage(this, f.getPayloadData());
							} else {
								LOGGER.debug("Ignoring frame:" + f.toString());
							}
						} else {
							mCurrentFrame = f;
						}
					} else if (f.getOpcode() == Opcode.CONTINIOUS) {
						try {
							mCurrentFrame.append(f);
						} catch (InvalidFrameException e) {
							mWebSocketListener.onError(this, e);
							abort("invalid frame: " + e.getMessage());
						}
						if (f.isFin()) {
							mWebSocketListener.onMessage(this, Charsetfunctions.stingUtf8(f.getPayloadData()));
							mCurrentFrame = null;
						}
					}
				}
			}
		}
	}

	// PUBLIC INSTANCE METHODS /////////////////////////////////////////////////
	public void abort() {
		abort("");
	}

	public void abort(String inProblemMessage) {
		LOGGER.debug("Aborting: " + inProblemMessage);
		close();
	}

	/**
	 * Closes the underlying SocketChannel, and calls the listener's onClose
	 * event handler.
	 */
	public void close() {
		if (mIsHandshakeClosed) {
			return;
		}
		// TODO Send error/closecode here in some cases
		mIsHandshakeClosed = true;
		try {
			handleWrite();
			mSocketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mWebSocketListener.onClose(this);
		if (mDraft != null)
			mDraft.reset();
		mCurrentFrame = null;
		mHandshakeRequest = null;
	}

	/**
	 * @return True if all of the text was sent to the client by this thread or the given data is empty
	 *         False if some of the text had to be buffered to be sent later.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void send(String inSendString) throws InterruptedException {
		if (inSendString == null)
			throw new IllegalArgumentException("Cannot send 'null' data to a WebSocket.");
		send(mDraft.createFrames(inSendString, mRole == Role.CLIENT));
	}

	// TODO there should be a send for bytebuffers
	public void send(byte[] inSendBytes) throws InterruptedException {
		if (inSendBytes == null)
			throw new IllegalArgumentException("Cannot send 'null' data to a WebSocket.");
		send(mDraft.createFrames(inSendBytes, mRole == Role.CLIENT));
	}

	// TODO instead of throwing or returning an error this method maybe should block on queue jams
	private void send(Collection<Framedata> inFramesCollection) throws InterruptedException {
		if (!mIsHandshakeComplete)
			throw new NotYetConnectedException();
		for (Framedata f : inFramesCollection) {
			sendFrame(f); // TODO high frequently calls to sendFrame are inefficient.
		}
	}

	public void sendFrame(Framedata inFrameData) throws InterruptedException {
		LOGGER.debug("send frame: " + inFrameData);
		channelWrite(mDraft.createBinaryFrame(inFrameData));
	}

	public boolean hasBufferedData() {
		return !mBufferQueue.isEmpty();
	}

	/**
	 * @return True if all data has been sent to the client, false if there
	 *         is still some buffered.
	 */
	public void handleWrite() throws IOException {
		ByteBuffer buffer = mBufferQueue.peek();
		while (buffer != null) {
			mSocketChannel.write(buffer);
			if (buffer.remaining() > 0) {
				continue;
			} else {
				mBufferQueue.poll(); // Buffer finished. Remove it.
				buffer = mBufferQueue.peek();
			}
		}
	}

	public HandshakeState isFlashEdgeCase(ByteBuffer inRequest) {
		if (mFlashPolicyIndex >= FLASH_POLICY_REQUEST.length)
			return HandshakeState.NOT_MATCHED;
		inRequest.mark();
		for (; inRequest.hasRemaining() && mFlashPolicyIndex < FLASH_POLICY_REQUEST.length; mFlashPolicyIndex++) {
			if (FLASH_POLICY_REQUEST[mFlashPolicyIndex] != inRequest.get()) {
				inRequest.reset();
				return HandshakeState.NOT_MATCHED;
			}
		}
		return inRequest.remaining() >= FLASH_POLICY_REQUEST.length ? HandshakeState.MATCHED : HandshakeState.MATCHING;
	}

	public void startHandshake(HandshakeBuilder inHandshakeData) throws InvalidHandshakeException, InterruptedException {
		if (mIsHandshakeComplete)
			throw new IllegalStateException("Handshake has allready been sent.");
		mHandshakeRequest = inHandshakeData;
		channelWrite(mDraft.createHandshake(mDraft.postProcessHandshakeRequestAsClient(inHandshakeData), mRole));
	}

	private void channelWrite(ByteBuffer inByteBuffer) throws InterruptedException {
		LOGGER.debug("write(" + inByteBuffer.limit() + "): {" + (inByteBuffer.limit() > 1000 ? "too big to display" : new String(inByteBuffer.array())) + "}");
		inByteBuffer.rewind();
		mBufferQueue.put(inByteBuffer);
		mWebSocketListener.onWriteDemand(this);
	}

	private void channelWrite(List<ByteBuffer> inByteBufferList) throws InterruptedException {
		for (ByteBuffer b : inByteBufferList) {
			channelWrite(b);
		}
	}

	private void open() throws InterruptedException, IOException {
		LOGGER.debug("open using draft: " + mDraft.getClass().getSimpleName());
		mIsHandshakeComplete = true;
		mWebSocketListener.onOpen(this);
	}

	public InetSocketAddress getRemoteSocketAddress() {
		return (InetSocketAddress) mSocketChannel.socket().getRemoteSocketAddress();
	}

	public InetSocketAddress getLocalSocketAddress() {
		return (InetSocketAddress) mSocketChannel.socket().getLocalSocketAddress();
	}

	@Override
	public String toString() {
		return super.toString();
	}

}
