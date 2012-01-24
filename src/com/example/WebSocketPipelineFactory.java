package com.example;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;

import java.net.URI;

public class WebSocketPipelineFactory implements ChannelPipelineFactory {

	private ChannelHandler applicationLogicHandler;

	private URI wsUri;

	private WebSocketClientHandshaker handshaker;

	public WebSocketPipelineFactory(final ChannelHandler applicationLogicHandler, final URI wsUri,
									final WebSocketClientHandshaker handshaker) {
		this.applicationLogicHandler = applicationLogicHandler;
		this.wsUri = wsUri;
		this.handshaker = handshaker;
	}

	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addFirst("applicationLogicHandler", applicationLogicHandler);
		pipeline.addFirst("webSocketClientHandler", new WebSocketClientHandler(handshaker));
		pipeline.addFirst("encoder", new HttpRequestEncoder());
		pipeline.addFirst("decoder", new HttpResponseDecoder());
		return pipeline;
	}
}
