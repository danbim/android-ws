package com.example;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.jboss.netty.util.internal.ExecutorUtil;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSocketService extends Service {

	private Channel channel;

	private ResultReceiver receiver;

	private ExecutorService bossExecutor;

	private ExecutorService workerExecutor;

	private SimpleChannelUpstreamHandler webSocketHandler = new SimpleChannelUpstreamHandler() {
		@Override
		public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

			if (e.getMessage() instanceof TextWebSocketFrame) {

				Bundle bundle = new Bundle();
				bundle.putString("message", ((TextWebSocketFrame) e.getMessage()).getText());
				receiver.send(0, bundle);

			} else {
				Log.wtf(WebSocketService.class.getName(),
						"Unexpected message type received: " + e.getMessage().getClass()
				);
			}
		}
	};

	class IncomingHandler extends Handler {

		@Override
		public void handleMessage(final Message msg) {
			// do something later with this
		}
	}

	final Messenger messenger = new Messenger(new IncomingHandler());

	@Override
	public IBinder onBind(final Intent intent) {

		receiver = (ResultReceiver) intent.getExtras().get("receiver");

		bossExecutor = Executors.newCachedThreadPool();
		workerExecutor = Executors.newCachedThreadPool();
		ClientBootstrap bootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(
						bossExecutor, workerExecutor
				)
		);
		final URI wsUri;
		try {
			wsUri = new URI("ws://opium.itm.uni-luebeck.de:1234/websocket");
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		final WebSocketClientHandshaker handshaker = new WebSocketClientHandshakerFactory().newHandshaker(
				wsUri, WebSocketVersion.V13, null, false, null
		);

		bootstrap.setPipelineFactory(new WebSocketPipelineFactory(webSocketHandler, wsUri, handshaker));


		final InetSocketAddress remoteAddress = new InetSocketAddress(wsUri.getHost(), wsUri.getPort());
		Log.i(WebSocketService.class.getName(), "Trying to connect to " + remoteAddress);

		ChannelFuture future = bootstrap.connect(remoteAddress);
		future.addListener(new ChannelFutureListener() {
			public void operationComplete(final ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					channel = future.getChannel();
					Log.i(WebSocketService.class.getName(), "Socket connection to " + remoteAddress + " established!");
					Log.i(WebSocketService.class.getName(), "Starting handshake...");
					handshaker.handshake(channel).addListener(new ChannelFutureListener() {
						public void operationComplete(final ChannelFuture future) throws Exception {
							if (future.isSuccess()) {
								Log.i(WebSocketService.class.getName(), "Websocket handshake complete!");
							} else {
								Log.e(WebSocketService.class.getName(),
										"Exception during websocket handshake: " + future.getCause(),
										future.getCause()
								);
							}
						}
					}
					);
				} else {
					Log.e(WebSocketService.class.getName(),
							"Failed to connect to websocket endpoint " + remoteAddress + ". Reason: " + future
									.getCause(), future.getCause()
					);
				}
			}
		}
		);

		return messenger.getBinder();
	}

	@Override
	public boolean onUnbind(final Intent intent) {
		try {
			channel.close().await();
		} catch (InterruptedException e) {
			Log.e(WebSocketService.class.getName(), "Exception while closing WebSocket connection: " + e, e);
		}
		ExecutorUtil.terminate(bossExecutor, workerExecutor);
		return super.onUnbind(intent);
	}
}
