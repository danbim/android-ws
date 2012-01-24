//The MIT License
//
//Copyright (c) 2009 Carl Bystršm
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in
//all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//THE SOFTWARE.

package com.example;

import android.util.Log;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.websocketx.*;
import org.jboss.netty.util.CharsetUtil;

public class WebSocketClientHandler extends SimpleChannelUpstreamHandler {

	private final WebSocketClientHandshaker handshaker;

	public WebSocketClientHandler(WebSocketClientHandshaker handshaker) {
		this.handshaker = handshaker;
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		System.out.println("WebSocket Client disconnected!");
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

		Channel ch = ctx.getChannel();

		if (!handshaker.isHandshakeComplete()) {
			handshaker.finishHandshake(ch, (HttpResponse) e.getMessage());
			System.out.println("WebSocket client connected!");
			ch.write(new TextWebSocketFrame("Hello "));
			return;
		}

		if (e.getMessage() instanceof HttpResponse) {
			HttpResponse response = (HttpResponse) e.getMessage();
			throw new RuntimeException(
					"Unexpected HttpResponse (status=" + response.getStatus() + ", "
							+ "content=" + response.getContent().toString(CharsetUtil.UTF_8) + ")"
			);
		}

		WebSocketFrame frame = (WebSocketFrame) e.getMessage();
		if (frame instanceof TextWebSocketFrame) {

			TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
			Log.i(WebSocketService.class.getName(), "WebSocket Client received message: " + textFrame.getText());
			ctx.sendUpstream(e);

		} else if (frame instanceof PongWebSocketFrame) {

			Log.i(WebSocketService.class.getName(), "WebSocket Client received pong");

		} else if (frame instanceof CloseWebSocketFrame) {

			Log.i(WebSocketService.class.getName(), "WebSocket Client received closing");
			ch.close();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		Log.e(WebSocketService.class.getName(), "Exception caught in WebSocketClientHandler: " + e, e.getCause());
		e.getChannel().close();
	}
}