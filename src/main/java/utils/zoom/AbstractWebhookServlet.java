package utils.zoom;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.json.JSONObject;

import utils.Handler;
import utils.J9;

public abstract class AbstractWebhookServlet extends HttpServlet {

	private static final String WEBHOOK_ERROR_HEADER = "webhook error -> ";
	private static final String WEBHOOK_INFRASTRUCTURE_ERROR = "(infrastructure) " + WEBHOOK_ERROR_HEADER;
	private static final String LS_WEBHOOK_ERROR_HEADER = "(last resource) " + WEBHOOK_ERROR_HEADER;
	
	private static final long serialVersionUID = 1L;

	private final String data;
	private final String errors;
	private final String errorTable;
	
	protected AbstractWebhookServlet(String dataDb, String errorsDb, String errorTable) {
		this.data = dataDb;
		this.errors = errorsDb;
		this.errorTable = errorTable;
	}
	
	protected AbstractWebhookServlet(String jndi) {
		this(jndi, jndi, "error");
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (!"POST".equals(request.getMethod())) {
			response.setHeader("Allow", "POST");
			response.setStatus(405);
		} else {
			doPost(request, response);
		}
	}

	protected abstract Handler getHandler(Request request);

	protected class Request {
		private final HttpServletRequest jee;
		public final JSONObject body;
		private String uri;
		private Map<String, String> headers = new HashMap<>();
		public String getHeader(String name) {
			return headers.get(name);
		}
		public String getUri() {
			return uri;
		}
		private Request(HttpServletRequest request) throws IOException {
			jee = request;
			byte[] bytes = J9.readAllBytes(request.getInputStream());
			String string = new String(bytes, StandardCharsets.UTF_8);
			body = new JSONObject(string);
		}
		private boolean isValidation() {
			return "endpoint.url_validation".equals(body.getString("event"));
		}
		private void execute(Runnable code) {
			Enumeration<String> names = jee.getHeaderNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				headers.put(name, jee.getHeader(name));
			}
			uri = jee.getRequestURI();
			ExecutorService threads = (ExecutorService) jee.getServletContext().getAttribute("threads");
			threads.execute(code);
		}
	}
	
	@Override
	protected final void doPost(HttpServletRequest jeeRequest, HttpServletResponse response) {
		try {
			Request request = new Request(jeeRequest);
			if (request.isValidation()) {
				throw new IllegalStateException("Not implemented yet");
			} else {
				request.execute(() -> {
					try {
						getHandler(request).handle();
					} catch (Throwable error) { // NOSONAR
						register(request, error);
					}
				});
			}
		} catch (Throwable thrown) { //NOSONAR
			getLogger().log(Level.SEVERE, WEBHOOK_INFRASTRUCTURE_ERROR, thrown);
		}
		response.setStatus(204);
	}

	protected Logger getLogger() {
		return Logger.getGlobal();
	}
	
	private void register(Request request, Throwable thrown) {
		Map<String, String> map = request.headers;
		String headers = map.keySet().stream().map(key -> key + ": " + map.get(key)).collect(Collectors.joining("; "));
		if (errors == null) {
			error(request.uri, headers, request.body, thrown, null);
		} else {
			String sql = "insert into " + errorTable + " set uri = ?, headers = ?, body = ?, error = ?";
			try (PreparedStatement insert = getConnection(errors).prepareStatement(sql)) {
				StringWriter error = new StringWriter();
				PrintWriter printer = new PrintWriter(error);
				thrown.printStackTrace(printer);
				insert.setString(1, request.uri);
				insert.setString(2, headers);
				insert.setString(3, request.body.toString());
				insert.setString(4, error.toString());
				insert.executeUpdate();
			} catch (Throwable extra) { // NOSONAR
				error(request.uri, headers, request.body, thrown, extra);
			}	
		}
	}

	private void error(String uri, String headers, JSONObject json, Throwable thrown, Throwable extra) {
		Logger logger = getLogger();
		String header = extra == null? WEBHOOK_ERROR_HEADER : LS_WEBHOOK_ERROR_HEADER; 
		logger.severe(() -> header + uri);
		logger.severe(() -> header + headers);
		logger.severe(() -> header + json);
		logger.log(Level.SEVERE, header, thrown);
		if (extra != null) {
			logger.log(Level.SEVERE, header, extra);	
		}
	}
	
	protected Connection getConnection(String database) throws SQLException {
		return getDataSource(database).getConnection();
	}
	
	protected Connection getConnection() throws SQLException {
		return getDataSource().getConnection();
	}

	protected DataSource getDataSource() {
		return getDataSource(data);
	}

	protected DataSource getDataSource(String name) {
		try {
			InitialContext jndi = new InitialContext();
			DataSource source = (DataSource) jndi.lookup("java:/comp/env/" + name);
			if (source == null) {
				throw new IllegalStateException("jndi.lookup('java:/comp/env/" + name + "') == null");
			}
			return source;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

}