package nl.isaac.dotcms.searcher.util;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class UserUtil {

	public static User getLoggedInUser(HttpServletRequest req) {
		try {
			return WebAPILocator.getUserWebAPI().getLoggedInUser(req);
		} catch (DotRuntimeException | PortalException | SystemException e) {
			throw new RuntimeException(e);
		}
	}
}
