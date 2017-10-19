package nl.isaac.dotcms.searcher.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class HostDAO {

	public Collection<Host> getHosts(String host, User user) {
		Collection<Host> hosts = new ArrayList<>();
		try {
			if (host.equals("all_hosts")) {
				hosts.addAll(APILocator.getHostAPI().findAll(user, true));
			} else {
				hosts.add(APILocator.getHostAPI().findByName(host, user, true));
			}
		} catch (DotDataException | DotSecurityException e) {
			Logger.warn(this, "Exception while retrieving hosts: " + host, e);
			throw new RuntimeException(e);
		}

		return hosts;
	}

	public List<String> getAllHosts(User user) {
		return getHosts("all_hosts", user).stream().map(Host::getHostname).collect(Collectors.toCollection(ArrayList::new));
	}

	public Host getCurrentHost(HttpServletRequest request) {
		try {
			return WebAPILocator.getHostWebAPI().getCurrentHost(request);
		} catch (PortalException | SystemException | DotDataException | DotSecurityException e) {
			Logger.warn(this, "Exception while retrieving current host", e);
			throw new RuntimeException(e);
		}
	}
}
