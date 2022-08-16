package net.jvmhost.dit.zoom;

import java.sql.Connection;

import javax.servlet.annotation.WebServlet;

import utils.Handler;
import utils.zoom.AbstractWebhookServlet;

@WebServlet("/handler")
public class WebhookServlet extends AbstractWebhookServlet {

	private static final long serialVersionUID = 1L;
	
	public WebhookServlet() {
		super("jdbc/zoom");
	}
	
	@Override
	protected Handler getHandler(Request request) {
		return () -> {
			try (Connection connection = getConnection()) {
				Event event = new Event(connection, request.body, request::getHeader);
				event.insert();
			}
		};
	}

}