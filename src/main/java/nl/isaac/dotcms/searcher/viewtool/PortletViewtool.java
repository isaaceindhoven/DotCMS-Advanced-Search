package nl.isaac.dotcms.searcher.viewtool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotcms.repackage.org.apache.commons.lang.StringEscapeUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;

import nl.isaac.dotcms.searcher.dao.HostDAO;
import nl.isaac.dotcms.searcher.service.SearcherParser;
import nl.isaac.dotcms.searcher.shared.PortletHit;
import nl.isaac.dotcms.searcher.shared.SearchMode;
import nl.isaac.dotcms.searcher.shared.SearchResult;
import nl.isaac.dotcms.searcher.util.Pagination;
import nl.isaac.dotcms.searcher.util.UserUtil;

public class PortletViewtool implements ViewTool {
	private HttpServletRequest request;

	@Override
	public void init(Object arg0) {
		ViewContext context = (ViewContext) arg0;
		this.request = context.getRequest();
	}

	public Collection<SearchResult> paginateResultsAndGetSnippets(int pageNumber, HttpServletRequest req) {

		// User has performed a search before
		if (req != null
				&& req.getSession().getAttribute("searcher_filteredResults") != null
				&& req.getSession().getAttribute("searcher_text") != null
				&& req.getSession().getAttribute("searcher_mode") != null
				&& req.getSession().getAttribute("searcher_snippetSizeBefore") != null
				&& req.getSession().getAttribute("searcher_snippetSizeAfter") != null) {

			String searchString = (String) req.getSession().getAttribute("searcher_text");
			String searchMode = (String) req.getSession().getAttribute("searcher_mode");
			String excludeText = (String) req.getSession().getAttribute("searcher_excludeText");
			int snippetSizeBefore = (int) req.getSession().getAttribute("searcher_snippetSizeBefore");
			int snippetSizeAfter = (int) req.getSession().getAttribute("searcher_snippetSizeAfter");
			int resultsPerPage = (int) req.getSession().getAttribute("searcher_resultsPerPage");

			@SuppressWarnings("unchecked")
			ArrayList<PortletHit> allHits = new ArrayList<>((List<PortletHit>) req.getSession().getAttribute("searcher_filteredResults"));

			int resultSize = allHits.size();
			int pageSize = (int) Math.ceil((double) resultSize / resultsPerPage);

			pageNumber = pageNumber > 0 ? pageNumber : 1;

			int fromIndex = (pageNumber - 1) * resultsPerPage;
			int toIndex = fromIndex + resultsPerPage;

			Collection<PortletHit> paginatedHits = allHits.subList(Math.max(0, fromIndex), Math.min(toIndex, resultSize));

			SearcherParser parser = new SearcherParser(searchString, searchMode, snippetSizeBefore, snippetSizeAfter, excludeText);

			// Parse hits to SearchResult and get snippets
			Collection<SearchResult> paginatedSearchResults = parser.parse(paginatedHits);

			req.getSession().setAttribute("pageSize", pageSize);
			req.setAttribute("paginatedResultsStartIndex", fromIndex + 1);
			req.setAttribute("paginatedResultsEndIndex", Math.min(toIndex, resultSize));

			Pagination pagination = new Pagination(pageNumber, pageSize, allHits.size(), fromIndex + 1, Math.min(toIndex, resultSize));

			req.setAttribute("pagination", pagination);

			return paginatedSearchResults.size() > 0 ? paginatedSearchResults : null;
		}

		return null;
	}

	public String replaceHTMLEncodedTextWithMatchHighlight(String encodedResult, String searchParam, String searcModeAsString) {
		String result = "";
		SearchMode searchMode = SearchMode.valueOf(searcModeAsString.toUpperCase());

		if (searchMode == SearchMode.TEXT) {
			result = encodedResult.replace(searchParam, "<span class=\"match\">" + searchParam + "</span>");
		} else if (searchMode == SearchMode.REGEX) {
			String decodedResult = StringEscapeUtils.unescapeHtml(encodedResult);
			String decodedSearchParam = StringEscapeUtils.unescapeHtml(searchParam);

			Pattern pattern = Pattern.compile(decodedSearchParam);
			Matcher matcher = pattern.matcher(decodedResult);

			while (matcher.find()) {
				String encodedMatch = StringEscapeUtils.escapeHtml(matcher.group());
				String reEncodedResult = StringEscapeUtils.escapeHtml(decodedResult);
				result += reEncodedResult.replace(encodedMatch, "<span class=\"match\">" + encodedMatch + "</span>");
			}
		}

		return result;
	}

	public String getLanguagesSelector() {
		List<Language> languages = APILocator.getLanguageAPI().getLanguages();
		String result = "{	id : '0', value : '', lang : 'All',	imageurl : '/html/images/languages/all.gif', label : '<span style=\"background-image:url(/html/images/languages/all.gif);\"></span>All' }";
		for (Language language : languages) {
			result += ", {	id : '" + language.getId() + "', value : '" + language.getId() + "', " + "lang : '"
					+ language.getLanguage() + "-" + language.getCountry() + "', "
					+ "imageurl : '/html/images/languages/" + language.getLanguageCode() + "_"
					+ language.getCountryCode() + ".gif', "
					+ "label : '<span style=\"background-image:url(/html/images/languages/" + language.getLanguageCode()
					+ "_" + language.getCountryCode() + ".gif);\">" + "</span>" + language.getLanguage() + "-"
					+ language.getCountry() + "' }";
		}
		return result;

	}

	public List<String> getAllHosts() throws DotDataException, DotSecurityException {
		User user = UserUtil.getLoggedInUser(request);
		return new HostDAO().getAllHosts(user);
	}

}
