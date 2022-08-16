package utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class CreateThreadPool implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		ExecutorService threads = Executors.newFixedThreadPool(4);
		event.getServletContext().setAttribute("threads", threads);
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		// Nothing to do
	}
	
}
