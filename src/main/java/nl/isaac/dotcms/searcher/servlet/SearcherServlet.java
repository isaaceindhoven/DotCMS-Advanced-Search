package nl.isaac.dotcms.searcher.servlet;
/**
* dotCMS Searcher plugin by ISAAC - The Full Service Internet Agency is licensed
* under a Creative Commons Attribution 3.0 Unported License
* - http://creativecommons.org/licenses/by/3.0/
* - http://www.geekyplugins.com/
*
* @copyright Copyright (c) 2012 ISAAC Software Solutions B.V. (http://www.isaac.nl)
*/

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import nl.isaac.dotcms.searcher.dao.HostDAO;
import nl.isaac.dotcms.searcher.service.SearcherService;
import nl.isaac.dotcms.searcher.shared.PortletHit;
import nl.isaac.dotcms.searcher.shared.SearchMode;
import nl.isaac.dotcms.searcher.shared.Status;
import nl.isaac.dotcms.searcher.shared.Status.StatusEnum;
import nl.isaac.dotcms.searcher.shared.Type;
import nl.isaac.dotcms.searcher.shared.UserSearchValues;
import nl.isaac.dotcms.searcher.shared.Version;
import nl.isaac.dotcms.searcher.util.UserUtil;
import nl.isaac.dotcms.util.osgi.RequestHelper;

public class SearcherServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final String PAGE_QUERY_PARAM_REGEX = "&page=[^&]+";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (req.getParameter("clearSearcherSession") != null) {
			try {
				boolean clear = "true".equals(req.getParameter("clearSearcherSession"));
				if (clear) {
					clearSearcherSession(req, resp);
				} else {
					Logger.info(this, "Searcher Session not cleared, boolean is not true!");
				}
			} catch (Exception e) {
				Logger.error(this, "Error!", e);
			}
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		UserSearchValues userSearchValues = paramsToUserSearchValues(req);

		User user = UserUtil.getLoggedInUser(req);
		SearcherService searcherService = new SearcherService(userSearchValues, user);

		Collection<PortletHit> searchResults = searcherService.getPortletHits();

		req.getSession().setAttribute("searcher_filteredResults", searchResults);

		userSearchValues.getAttributesMap().forEach((attributeKey, attributeValue) -> {
			req.getSession().setAttribute(attributeKey, attributeValue);
		});

		Logger.info(this, "Returned " + searchResults.size() + " search results");

		resp.sendRedirect(req.getParameter("redirectPage").replaceAll(PAGE_QUERY_PARAM_REGEX, ""));
	}

	private void clearSearcherSession(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		HttpSession session = req.getSession();
		String redirectTo = req.getParameter("redirectPage");
		if (session != null && !StringUtils.isBlank(redirectTo)) {
			session.removeAttribute("searcher_filteredResults");
			session.removeAttribute("pageSize");
			UserSearchValues.getAttributeKeys().forEach((attributeKey) -> {
				session.removeAttribute(attributeKey);
			});

			Logger.info(this, "Cleared Searcher Session!");

			resp.sendRedirect(redirectTo.replaceAll(PAGE_QUERY_PARAM_REGEX, ""));
		} else {
			Logger.info(this, "Session or RedirectPage is not found in URL...");
		}
	}

	private UserSearchValues paramsToUserSearchValues(HttpServletRequest req) {
		Logger.info(this, "Parsing posted parameters to SearchFilterRequest Object");

		RequestHelper requestHelper = new RequestHelper(req);

		String text = requestHelper.getParamAsString("text");

		String searchModeAsString = requestHelper.getParamAsString("searchMode");
		SearchMode searchMode = SearchMode.valueOf(searchModeAsString.toUpperCase());

		String languageId = requestHelper.getParamAsString("language_id");

		String statusAsString = requestHelper.getParamAsString("status");
		Status status = new Status();
		status.setStatus(StatusEnum.valueOf(statusAsString.toUpperCase()));

		String versionAsString = requestHelper.getParamAsString("version");
		Version version = Version.valueOf(versionAsString.toUpperCase());

		String typeId = requestHelper.getParamAsString("type_id");
		Type type = Type.valueOf(typeId.toUpperCase());

		String hostString = requestHelper.getParamAsString("hostName", new HostDAO().getCurrentHost(req).getHostname());

		int snippetSizeBefore = requestHelper.getParamAsInteger("snippetSizeBefore", 200);
		int snippetSizeAfter = requestHelper.getParamAsInteger("snippetSizeAfter", 200);

		String excludeText = req.getParameter("excludeText");
		if (excludeText != null && excludeText.trim().isEmpty()) {
			excludeText = null;
		}

		int maxResults = requestHelper.getParamAsInteger("maxResults", 100);
		int resultsPerPage = requestHelper.getParamAsInteger("resultsPerPage", 10);

		return new UserSearchValues(searchMode, type, text, hostString, languageId, status, version,
				Integer.valueOf(snippetSizeBefore), Integer.valueOf(snippetSizeAfter), excludeText, maxResults,
				resultsPerPage);
	}

}
