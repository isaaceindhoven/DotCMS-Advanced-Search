package nl.isaac.dotcms.util.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotmarketing.util.VelocityUtil;

public class MonitoringServlet extends HttpServlet {
	private static final String testMacro =
			"#macro(test $boolean)#if($boolean) OK\n#else NOK\n#end#end\n";

	// Use the pluginName to distinguish between MonitoringServlets of different plugins and avoid a "Servlet instance already registered"
	private final String pluginName;

	public MonitoringServlet(String pluginName) {
		this.pluginName = pluginName;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String velocity = getMonitoringVelocity();
		ChainedContext velocityContext = getStrictVelocityContext(request, response);

		try {
			String result = VelocityUtil.eval(velocity, velocityContext);
			if (result.contains("$") || result.contains("NOK")) {
				response.setStatus(500);
			}
			response.getWriter().write(result);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private ChainedContext getStrictVelocityContext(HttpServletRequest request,
			HttpServletResponse response) {
		ChainedContext velocityContext = VelocityUtil.getWebContext(request, response);
		velocityContext.getVelocityEngine().setProperty(VelocityEngine.RUNTIME_REFERENCES_STRICT, true);
		velocityContext.getVelocityEngine()
				.setProperty(VelocityEngine.RUNTIME_REFERENCES_STRICT_ESCAPE, true);
		return velocityContext;
	}

	private String getMonitoringVelocity() throws IOException {
		Bundle bundle = FrameworkUtil.getBundle(this.getClass());
		URL resourceURL = bundle.getResource("ext/monitoring.vtl");
		if (resourceURL != null) {
			try(InputStream is = resourceURL.openStream()) {
				String velocity = IOUtils.toString(is, "UTF-8");
				return testMacro + velocity;
			}
		} else {
			return "/ext/monitoring.vtl does not exist";
		}
	}

	@Override public boolean equals(Object obj) {
		if (obj instanceof MonitoringServlet) {
			return super.equals(obj) && getPluginName().equals(((MonitoringServlet) obj).getPluginName());
		}

		return false;
	}

	@Override public int hashCode() {
		return super.hashCode() + pluginName.hashCode();
	}

	public String getPluginName() {
		return pluginName;
	}
}
