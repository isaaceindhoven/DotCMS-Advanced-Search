package nl.isaac.dotcms.searcher.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import nl.isaac.dotcms.searcher.service.BufferedSearchResultIterator;
import nl.isaac.dotcms.searcher.shared.Status;
import nl.isaac.dotcms.searcher.shared.Type;
import nl.isaac.dotcms.searcher.util.ContentletQuery;

@SuppressWarnings("deprecation")
public class PortletDAO {

	private final FolderDAO folderDAO;
	private final User user;

	public PortletDAO(User user) {
		super();
		this.folderDAO = new FolderDAO();
		this.user = user;
	}

	public Map<Type, Collection<? extends Object>> getAllByBuffer(BufferedSearchResultIterator buff) {
		Map<Type, Collection<? extends Object>> portletsToFilter = new LinkedHashMap<>();

		while (buff.hasNext()) {
			portletsToFilter.putAll(buff.next());
		}

		return portletsToFilter;
	}

	public List<Container> getAllContainers(Host host) {
		try {
			return new ArrayList<>(APILocator.getContainerAPI().findContainers(user, false, null, host.getHostThumbnail(), null, null, null, 0, 0, ""));
		} catch (DotDataException | DotSecurityException e) {
			Logger.warn(this, "Error while getting all containers", e);
		}

		return new ArrayList<>();
	}

	public List<Template> getAllTemplates(Host host) {
		try {
			return APILocator.getTemplateAPI().findTemplates(user, false, null, host.getIdentifier(), null, null, null, 0, 0, "");
		} catch (DotDataException | DotSecurityException e) {
			Logger.warn(this, "Error while getting all templates", e);
		}

		return new ArrayList<>();
	}

	public List<Structure> getAllStructures(Host host) {
		try {
			return StructureFactory.getStructures(user, true, true, " host = '" + host.getIdentifier() + "'", "", 0, 0, "").stream().filter(s -> s.getHost().equals(host.getIdentifier()))
					.collect(Collectors.toList());
		} catch (DotDataException e) {
			Logger.warn(this, "Error while getting all structures", e);
		}

		return new ArrayList<>();
	}

	public List<Contentlet> getWidgetContentlets(Host host, String languageId, Status status) {
		return getContentletsByStructureType(Structure.STRUCTURE_TYPE_WIDGET, host, languageId, status);
	}

	public List<Contentlet> getContentContentlets(Host host, String languageId, Status status) {
		return getContentletsByStructureType(Structure.STRUCTURE_TYPE_CONTENT, host, languageId, status);
	}

	public List<Contentlet> getFileContentlets(Host host, Status status) {
		return getContentletsByStructureType(Structure.STRUCTURE_TYPE_FILEASSET, host, null, status);
	}

	public List<Contentlet> getHtmlContentlets(Host host, String languageId, Status status) {
		return getContentletsByStructureType(Structure.STRUCTURE_TYPE_HTMLPAGE, host, languageId, status);
	}

	private List<Structure> getStructuresPerType(int structureType) {
		List<Structure> structuresPerType = new ArrayList<>();

		StructureFactory.getStructures().forEach((structure) -> {
			if (structure.getStructureType() == structureType) {
				structuresPerType.add(structure);
			}
		});

		return structuresPerType;
	}

	private List<Contentlet> getContentletsByStructureType(int structureType, Host host, String languageId,
			Status status) {
		List<Structure> structuresPerType = getStructuresPerType(structureType);

		if (structuresPerType.size() != 0) {
			ContentletQuery cq = new ContentletQuery(structuresPerType);
			cq.setUser(user);

			if (structureType == Structure.STRUCTURE_TYPE_FILEASSET) {
				cq.addHost(host.getIdentifier());
			} else {

				if (!languageId.equalsIgnoreCase("0")) {
					cq.addLanguage(languageId);
				}

				cq.addHostAndIncludeSystemHost(host.getIdentifier());
			}

			if (!(status.getStatus().getLive() == false && status.getStatus().getWorking() == false
					&& status.getStatus().getArchived() == false)) {
				cq.addDeleted(status.getStatus().getArchived());
				cq.addWorking(status.getStatus().getWorking());
				cq.addLive(status.getStatus().getLive());
			}

			return cq.executeSafe();
		}

		return new ArrayList<>();
	}

}
