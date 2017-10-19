package nl.isaac.dotcms.searcher.util;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * Provides functionality to build a Lucene query
 *
 * @author xander
 * @author jorith.vandenheuvel
 *
 */
public class ContentletQuery {
	protected StringBuilder query = new StringBuilder();
	private String structureName;
	private User user;

	// Paging & sorting
	private int offset = 0;
	private int limit = -1;
	private String sortBy = "";
	private long totalResults = -1;

	public ContentletQuery(List<Structure> structures) {
		String structureQuery = "+(";
		for(Structure s : structures) {
			structureQuery += "structureName:" + s.getVelocityVarName() + " ";
		}
		structureQuery += ") ";
		query.append(structureQuery);
	}

	public ContentletQuery(String structureName) {
		this(true, structureName);
	}

	public ContentletQuery(boolean include, String structureName) {
		this.structureName = structureName;
		query.append((include ? "+" : "-") + "structureName:" + structureName);
	}

	protected String getStructureName() {
		return structureName;
	}

	public void setSortBy(String sortBy) {
		this.sortBy = sortBy;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	/**
	 *
	 * @param include True if this must be included in the result, false otherwise
	 * @param name The field name
	 * @param value The value to search for
	 */
	public void addFieldLimitation(boolean include, String name, String value) {
		query.append(" " + (include ? "+" : "-"));
		addFieldLimitationString(name, value);
	}

	private void addFieldLimitationString(String name, String value) {
		if(value.contains("*")) {
			query.append(structureName + "." + name + ":" + escapeValue(value) + " ");
		} else {
			query.append(structureName + "." + name + ":\"" + escapeValue(value) + "\" ");
		}
	}

	/**
	 *
	 * @param include True if this must be included in the result, false otherwise
	 * @param name The field name
	 * @param values The values to search for
	 */
	public void addFieldLimitations(boolean include, String name, String... values) {
		query.append(" " + (include ? "+" : "-") + "(");
		for(String value: values) {
			addFieldLimitationString(name, value);
		}
		query.append(")");
	}

	/**
	 *
	 * @param name The field name
	 * @param from The lower bound value
	 * @param to The upper bound value
	 */
	public void addFieldRangeLimitation(String name, String from, String to) {
		query.append(" +" + structureName + "." + name + ":[" + from + " TO " + to + "] ");
	}


	private String escapeValue(String value) {
		return value.replace(":", "\\:").replace("\"", "\\\"");
	}

	public void addLatestLiveAndNotDeleted(boolean live) {
		addLive(live);
		addWorking(true);
		addDeleted(false);
	}


	/**
	 * Limit the results of the query to certain categories
	 * @param include True if this must be included in the result, false otherwise
	 * @param categoryVelocityVarNames The Velocity varnames of the categories to filter on
	 */
	public void addCategoryLimitations(boolean include, String... categoryVelocityVarNames) {
		query.append(" " + (include ? "+" : "-") + "(");
		for(String categoryVelocityVarName: categoryVelocityVarNames) {
			query.append("categories:" + escapeValue(categoryVelocityVarName) + " ");
		}
		query.append(")");
	}

	/**
	 * Limit the results of the query to certain categories
	 * @param include True if this must be included in the result, false otherwise
	 * @param categories The categories to filter on
	 */
	public void addCategoryLimitations(boolean include, Category... categories) {
		String[] categoryVelocityVarNames = new String[categories.length];
		for(int i=0; i<categories.length; i++) {
			categoryVelocityVarNames[i] = categories[i].getCategoryVelocityVarName();
		}

		addCategoryLimitations(include, categoryVelocityVarNames);
	}

	/**
	 * Adds +live to the query
	 * @param live
	 */
	public void addLive(boolean live) {
		query.append(" +live:" + live);
	}

	/**
	 * Adds +working to the query
	 * @param working
	 */
	public void addWorking(boolean working) {
		query.append(" +working:" + working);
	}

	/**
	 * Adds +deleted to the query
	 * @param deleted
	 */
	public void addDeleted(boolean deleted) {
		query.append(" +deleted:" + deleted);
	}

	/**
	 * Adds a language limit to the query
	 * @param language
	 */
	public void addLanguage(Language language) {
		addLanguage(language.getId());
	}

	/**
	 * Adds a language limit to the query
	 * @param languageId
	 */
	public void addLanguage(Long languageId) {
		if(languageId != null) {
			addLanguage(languageId.toString());
		} else {
			Logger.warn(this, "Tried to add languageId Null!");
		}
	}

	/**
	 * Adds a language limit to the query
	 * @param languageId
	 */
	public void addLanguage(Integer languageId) {
		if(languageId != null) {
			addLanguage(languageId.toString());
		} else {
			Logger.warn(this, "Tried to add languageId Null!");
		}
	}

	/**
	 * Adds a language limit to the query
	 * @param languageId
	 */
	public void addLanguage(String languageId) {
		query.append(" +languageId:" + languageId);
	}

	/**
	 *
	 * @return The total number of results, only when paging is set and the query has been executed. -1 otherwise.
	 */
	public long getTotalResults() {
		return this.totalResults;
	}

	/**
	 * Adds a host limit to the query
	 * @param host
	 * @return The updated ContentletQuery
	 */
	public ContentletQuery addHost(Host host) {
		return addHost(host.getIdentifier());
	}

	/**
	 * Adds a host limit to the query
	 * @param hostIdentifier
	 * @return The updated ContentletQuery
	 */
	public ContentletQuery addHost(String hostIdentifier) {
		query.append(" +conhost:" + hostIdentifier);
		return this;
	}

	/**
	 * Adds a host limit to the query (given host AND System HOST
	 * @param hostIdentifier
	 */
	public void addHostAndIncludeSystemHost(String hostIdentifier) {
		query.append(" +(conhost:SYSTEM_HOST conhost:" + hostIdentifier + ")");
	}

	/**
	 *
	 * @return The resulting query
	 */
	public String getQuery() {
		return query.toString();
	}

	/**
	 *
	 * @return The resulting query as a StringBuilder object
	 */
	public StringBuilder getQueryStringBuilder() {
		return query;
	}

	public void setUser(User user) {
		this.user = user;
	}

	/**
	 * Executes the query
	 * @return The resulting List of Contentlets
	 */
	public List<Contentlet> executeSafe() {
		try {
			if(user != null) {
				return APILocator.getContentletAPI().search(query.toString(), this.limit, this.offset, this.sortBy, user, true);
			} else {
				return APILocator.getContentletAPI().search(query.toString(), this.limit, this.offset, this.sortBy, APILocator.getUserAPI().getSystemUser(), false);
			}

		} catch (DotDataException | DotSecurityException e) {
			Logger.warn(this, "Exception while executing query", e);
		}

		return new ArrayList<>();
	}

	/**
	 * Combines two ContentletQuery objects into one query.
	 * @param and True if the resulting query must be an AND query. False if it must
	 * be an OR query.
	 * @param contentletQuery The query to add
	 */
	public void addQuery(boolean and, ContentletQuery contentletQuery) {
		if(and) {
			//just add them together
			query.append(" " + contentletQuery.getQuery());
		} else {
			//group them and make it an OR query
			query.insert(0, "(");
			query.append(") (");
			query.append(contentletQuery.getQuery());
			query.append(")");
		}
	}
}