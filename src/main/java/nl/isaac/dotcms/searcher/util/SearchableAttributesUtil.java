package nl.isaac.dotcms.searcher.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.dotcms.repackage.org.apache.pdfbox.io.IOUtils;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;

import nl.isaac.dotcms.searcher.shared.SearchableAttribute;
import nl.isaac.dotcms.searcher.shared.Type;

@SuppressWarnings("deprecation")
public final class SearchableAttributesUtil<T> {
	
	public SearchableAttributesUtil() {
		super();
	}
	
	public static Collection<SearchableAttribute> getTemplateAttributes(Template template) {
		Collection<SearchableAttribute> searchableAttributes = new ArrayList<>(4);
		searchableAttributes.add(new SearchableAttribute(Type.TEMPLATE, template.getTitle(), "Title", template.getTitle()));
		searchableAttributes.add(new SearchableAttribute(Type.TEMPLATE, template.getTitle(), "Body", template.getBody()));
		searchableAttributes.add(new SearchableAttribute(Type.TEMPLATE, template.getTitle(), "Header", template.getHeader()));
		searchableAttributes.add(new SearchableAttribute(Type.TEMPLATE, template.getTitle(), "Footer", template.getFooter()));
		return searchableAttributes;
	}
	
	public static Collection<SearchableAttribute> getContainerAttributes(Container container) {
		Collection<SearchableAttribute> searchableAttributes = new ArrayList<>(3);
		searchableAttributes.add(new SearchableAttribute(Type.CONTAINER, container.getTitle(), "Code", container.getCode()));
		searchableAttributes.add(new SearchableAttribute(Type.CONTAINER, container.getTitle(), "Preloop", container.getPreLoop()));
		searchableAttributes.add(new SearchableAttribute(Type.CONTAINER, container.getTitle(), "Postloop", container.getPostLoop()));
		return searchableAttributes;
	}
	
	public static Collection<SearchableAttribute> getHtmlPageAttributes(HTMLPage htmlPage) {
		Collection<SearchableAttribute> searchableAttributes = new ArrayList<>(5);
		searchableAttributes.add(new SearchableAttribute(Type.HTMLPAGE, htmlPage.getTitle(), "Title", htmlPage.getTitle()));
		searchableAttributes.add(new SearchableAttribute(Type.HTMLPAGE, htmlPage.getTitle(), "FriendlyName", htmlPage.getFriendlyName()));
		searchableAttributes.add(new SearchableAttribute(Type.HTMLPAGE, htmlPage.getTitle(), "Meta Data", htmlPage.getMetadata()));
		searchableAttributes.add(new SearchableAttribute(Type.HTMLPAGE, htmlPage.getTitle(), "SEO Description", htmlPage.getSeoDescription()));
		searchableAttributes.add(new SearchableAttribute(Type.HTMLPAGE, htmlPage.getTitle(), "SEO Keywords", htmlPage.getSeoKeywords()));
		return searchableAttributes;
	}
	
	public static Collection<SearchableAttribute> getFolderAttributes(Folder folder) {
		Collection<SearchableAttribute> searchableAttributes = new ArrayList<>(2);
		searchableAttributes.add(new SearchableAttribute(Type.FOLDER, folder.getTitle(), "Name", folder.getName()));
		searchableAttributes.add(new SearchableAttribute(Type.FOLDER, folder.getTitle(), "Title", folder.getTitle()));
		return searchableAttributes;
	}
	
	public static Collection<SearchableAttribute> getStructureAttributes(Structure structure) {
		Collection<SearchableAttribute> searchableAttributes = new ArrayList<>();
		List<Field> fields = FieldsCache.getFieldsByStructureInode(structure.getInode());

		for (Field f : fields) {
			if (f.getFieldType().equalsIgnoreCase(FieldType.CUSTOM_FIELD.toString())
					|| f.getFieldType().equalsIgnoreCase(FieldType.TEXT_AREA.toString())
					|| f.getVelocityVarName().equalsIgnoreCase("widgetCode")) {

				searchableAttributes.add(new SearchableAttribute(Type.STRUCTURE, structure.getName(), f.getFieldName(),f.getValues() + " " + f.getDefaultValue()));
			}
		}
		
		return searchableAttributes;
	}
	
	public static Collection<SearchableAttribute> getFileAttributes(FileAsset file) {
		Collection<SearchableAttribute> searchableAttributes = new ArrayList<>();
		
		if ("vtl".equalsIgnoreCase(file.getExtension()) || "js".equalsIgnoreCase(file.getExtension())) {
			
			String fileText = null;
			try {
				fileText = new String(IOUtils.toByteArray(file.getFileInputStream()));
			} catch (IOException e) {
				Logger.warn(SearchableAttributesUtil.class, "Error while converting bytes to array of file: " + file.getFileName(), e);
			}
			
			searchableAttributes.add(new SearchableAttribute(Type.FILE, file.getFileName(), "Text of file", fileText));
		}
		
		return searchableAttributes;
	}
	
	// Content or Widget
	public static Collection<SearchableAttribute> getContentletAttributes(Type type, Contentlet contentlet) {
		Collection<SearchableAttribute> searchableAttributes = new ArrayList<>();
		
		Map<String, Object> row = contentlet.getMap();
		row.put("structureName", contentlet.getStructure().getName());

		// Extract the title in case it hasn't been loaded yet,
		// if it isn't, it will cause a
		// ConcurrentModificationException in the loop below
		final String title = contentlet.getTitle();

		row.entrySet().forEach((entry) -> {
			if (entry.getValue() instanceof String) {
				searchableAttributes.add(new SearchableAttribute(type, title, entry.getKey(), (String) entry.getValue()));
			}
		});
		
		return searchableAttributes;
	}
	
}