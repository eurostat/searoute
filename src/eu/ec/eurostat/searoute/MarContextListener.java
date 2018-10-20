/**
 * 
 */
package eu.ec.eurostat.searoute;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @author julien
 *
 */
public class MarContextListener implements ServletContextListener{
	//ServletContext context;

	@Override
	public void contextInitialized(ServletContextEvent contextEvent) {
		//context = contextEvent.getServletContext();
	}

	@Override
	public void contextDestroyed(ServletContextEvent contextEvent) {
		// This manually deregisters JDBC driver, which prevents Tomcat 7 from complaining about memory leaks wrto this class
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			Driver driver = drivers.nextElement();
			try {
				DriverManager.deregisterDriver(driver);
				//LOG.log(Level.INFO, String.format("deregistering jdbc driver: %s", driver));
			} catch (SQLException e) {
				//LOG.log(Level.SEVERE, String.format("Error deregistering driver %s", driver), e);
			}
		}
	}

}
