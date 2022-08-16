package utils;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;


/**
 * Registers and unregisters a MySQL JDBC driver deployed inside the web application (not as an application server lib)
 */
@WebListener
public class UnregisterJdbcDriver implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		// Nothing to do
	}

	/**
	 * Prevents Tomcat 7 from complaining about memory leaks by unregistering the JDBC driver
	 */
	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		ClassLoader self = Thread.currentThread().getContextClassLoader();
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			Driver driver = drivers.nextElement();
			if (driver.getClass().getClassLoader() == self) {
				try {
					DriverManager.deregisterDriver(driver);
				} catch (SQLException e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
				Log.info(() -> "A JDBC Driver has been unregistered: " + driver);
			}
		}
	}

}