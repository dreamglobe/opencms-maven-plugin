package com.kw.opencms.module.mojo.util;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;


public class ManifestBean {

	final static public DateFormat dateFormat = new SimpleDateFormat(
			"EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);

	static {
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	ManifestInfoBean info;
	ModuleInfoBean module;
	AccountsBean accounts;
	List<ResourceFileBean> files = new ArrayList<ResourceFileBean>();

	public ManifestBean(Map properties) {
		info = (ManifestInfoBean) SimpleBeanLoader.load(new ManifestInfoBean(),
				properties, "manifest.info");
		module = (ModuleInfoBean) SimpleBeanLoader.load(new ModuleInfoBean(),
				properties, "manifest.module");
		accounts = (AccountsBean) SimpleBeanLoader.load(new AccountsBean(),
				properties, "manifest.accounts");
	}

	public ManifestInfoBean getInfo() {
		return info;
	}

	public void setInfo(ManifestInfoBean info) {
		this.info = info;
	}

	public ModuleInfoBean getModule() {
		return module;
	}

	public void setModule(ModuleInfoBean module) {
		this.module = module;
	}

	public AccountsBean getAccounts() {
		return accounts;
	}

	public void setAccounts(AccountsBean accounts) {
		this.accounts = accounts;
	}

	public void setFiles(List<ResourceFileBean> files) {
		this.files = files;
	}

	public List<ResourceFileBean> getFiles() {
		return files;
	}
	
	class ManifestInfoBean {
		public String creator;
		public String opencmsversion;
		public String createdate = ManifestBean.dateFormat.format(new Date());
		public String project;
		public String exportversion;
	}
	
	public class ModuleInfoBean {
		public String name;
		public String nicename;
		public String group;
		public String moduleclass;
		public String description;
		public String version;
		public String authorname;
		public String authoremail;
		public String datecreated;
		public String dependencies_str;
		public String dependencies;
		public String exportpoints_str;
		public String exportpoints;
		public String resources_str;
		public String resources;
		public String resourcetypes_str;
		public String resourcetypes;
		public String explorertypes_str;
		public String parameters_str;
		public String parameters;
	}
	
	public class AccountsBean {
		public String accounts_str;
		public String accounts;
	}
	
	static public class ResourceFileBean {
		String source;
		String destination;
		String type;
		String uuidstructure;
		String uuidresource;
		String datelastmodified;
		String userlastmodified;
		String datecreated;
		String usercreated;
		String flags = "0";
		Set<Entry<String, String>> properties;
		Set<Entry<String, String>> sharedProperties;
		Set<Entry<String, PermissionSet>> acl;
		Set<CategoryBean> relations;
		
		public String getSource() {
			return source;
		}
		
		public void setSource(String source) {
			this.source = source;
		}
		
		public String getDestination() {
			return destination;
		}
		
		public void setDestination(String destination) {
			this.destination = destination;
		}
		
		public String getType() {
			return type;
		}
		
		public void setType(String type) {
			this.type = type;
		}
		
		public String getUuidstructure() {
			return uuidstructure;
		}
		
		public void setUuidstructure(String uuidstructure) {
			this.uuidstructure = uuidstructure;
		}
		
		public String getUuidresource() {
			return uuidresource;
		}
		
		public void setUuidresource(String uuidresource) {
			this.uuidresource = uuidresource;
		}
		
		public String getDatelastmodified() {
			return datelastmodified;
		}
		
		public void setDatelastmodified(String datelastmodified) {
			this.datelastmodified = datelastmodified;
		}
		
		public String getUserlastmodified() {
			return userlastmodified;
		}
		
		public void setUserlastmodified(String userlastmodified) {
			this.userlastmodified = userlastmodified;
		}
		
		public String getDatecreated() {
			return datecreated;
		}
		
		public void setDatecreated(String datecreated) {
			this.datecreated = datecreated;
		}
		
		public String getUsercreated() {
			return usercreated;
		}
		
		public void setUsercreated(String usercreated) {
			this.usercreated = usercreated;
		}
		
		public String getFlags() {
			return flags;
		}
		
		public void setFlags(String flags) {
			this.flags = flags;
		}
		
		public Set<Entry<String, String>> getProperties() {
			return properties;
		}
		
		public void setProperties(Set<Entry<String, String>> properties) {
			this.properties = properties;
		}
		
		public Set<Entry<String, String>> getSharedProperties() {
			return sharedProperties;
		}
		
		public void setSharedProperties(Set<Entry<String, String>> sharedProperties) {
			this.sharedProperties = sharedProperties;
		}
		
		public Set<Entry<String, PermissionSet>> getAcl() {
			return acl;
		}
		
		public void setAcl(Set<Entry<String, PermissionSet>> acl) {
			this.acl = acl;
		}
		
		public Set<CategoryBean> getRelations() {
			return relations;
		}
		
		public void setRelations(Set<CategoryBean> relations) {
			this.relations = relations;
		}
		
		public boolean isEmptyProperties() {
			return this.properties.isEmpty() && this.sharedProperties.isEmpty();
		}
		
		public boolean isEmptyAcl() {
			return this.acl.isEmpty();
		}
		
		public boolean isEmptyRelations() {
			return this.relations.isEmpty();
		}
	}

	static public enum Filetype {
		image, jsp, plain, binary, folder, downloadgallery, imagegallery, linkgallery, custom, sibling;
		
		private String custonName;
		
		public static Filetype newCustomFiletype(String name) {
			try {
				return Filetype.valueOf(name);
			} catch (IllegalArgumentException e) {
				Filetype custom = Filetype.custom;
				custom.setCustonName(name);
				return custom;
			}
		}
		
		public static Filetype calculateType(File file) {
			String[] names = file.getName().split("\\.");
			String extension = names.length > 1
			? names[names.length - 1]
			        : names[0];
			return getFiletypeByExtension(extension);
		}
		
		public static Filetype getFiletypeByExtension(String extension) {
			try {
				if (extension.equals("class")) {
					extension = "clas";
				}
				switch (Extension.valueOf(extension.toLowerCase())) {
					case jpg :
					case jpeg :
					case png :
					case gif :
					case bmp :
					case tif :
					case tiff :
						return Filetype.image;
					case jsp :
						return Filetype.jsp;
					case pdf :
					case zip :
					case clas :
					case ppt :
					case doc :
					case xls :
					case jar :
					case db :
						return Filetype.binary;
					case sibling :
						return Filetype.sibling;
					default :
						return Filetype.plain;
				}
			} catch (IllegalArgumentException e) {
				return Filetype.plain;
			}
		}
		
		public void setCustonName(String custonName) {
			this.custonName = custonName;
		}

		public String getCustonName() {
			return custonName;
		}

		static public enum Extension {
			html, htm, css, xml, xsl, xsd, dtd, txt, js, properties, jpg, jpeg, png, gif, tiff, tif, bmp, jsp, pdf, zip, ppt, doc, xls, db, jar, clas, sibling;
			
		}
	}
	
	static public class PermissionSet {
		/** The permission to control a resource. */
		public static final int PERMISSION_CONTROL = 8;
		
		/** The permission to direct publish a resource. */
		public static final int PERMISSION_DIRECT_PUBLISH = 16;
		
		/** No permissions for a resource (used especially for denied permissions). */
		public static final int PERMISSION_EMPTY = 0;
		
		public static final int PERMISSION_READ = 1;
		
		/** The permission to view a resource. */
		public static final int PERMISSION_VIEW = 4;
		
		/** The permission to write a resource. */
		public static final int PERMISSION_WRITE = 2;
		
		int m_allowed = 0;
		int m_denied = 0;
		String m_flags = "0";
		
		public final int getAllowed() {
			return m_allowed;
		}
		
		public final void setAllowed(int mAllowed) {
			m_allowed = mAllowed;
		}
		
		public final int getDenied() {
			return m_denied;
		}
		
		public final void setDenied(int mDenied) {
			m_denied = mDenied;
		}
		
		public final String getFlags() {
			return m_flags;
		}
		
		public final void setFlags(String mFlags) {
			m_flags = mFlags;
		}
		
		public PermissionSet(String permissionString) {
			init(permissionString, "0");
		}
		
		public PermissionSet(String permissionString, String flags) {
			init(permissionString, flags);
		}
		
		public void init(String permissionString, String flags) {
			m_allowed = 0;
			m_denied = 0;
			m_flags = flags;
			
			if(permissionString.length()>0){
				StringTokenizer tok = new StringTokenizer(permissionString, "+-", true);
				while (tok.hasMoreElements()) {
					String prefix = tok.nextToken();
					String suffix = tok.nextToken();
					switch (suffix.charAt(0)) {
						case 'R' :
						case 'r' :
							if (prefix.charAt(0) == '+') {
								m_allowed |= PermissionSet.PERMISSION_READ;
							}
							if (prefix.charAt(0) == '-') {
								m_denied |= PermissionSet.PERMISSION_READ;
							}
							break;
						case 'W' :
						case 'w' :
							if (prefix.charAt(0) == '+') {
								m_allowed |= PermissionSet.PERMISSION_WRITE;
							}
							if (prefix.charAt(0) == '-') {
								m_denied |= PermissionSet.PERMISSION_WRITE;
							}
							break;
						case 'V' :
						case 'v' :
							if (prefix.charAt(0) == '+') {
								m_allowed |= PermissionSet.PERMISSION_VIEW;
							}
							if (prefix.charAt(0) == '-') {
								m_denied |= PermissionSet.PERMISSION_VIEW;
							}
							break;
						case 'C' :
						case 'c' :
							if (prefix.charAt(0) == '+') {
								m_allowed |= PermissionSet.PERMISSION_CONTROL;
							}
							if (prefix.charAt(0) == '-') {
								m_denied |= PermissionSet.PERMISSION_CONTROL;
							}
							break;
						case 'D' :
						case 'd' :
							if (prefix.charAt(0) == '+') {
								m_allowed |= PermissionSet.PERMISSION_DIRECT_PUBLISH;
							}
							if (prefix.charAt(0) == '-') {
								m_denied |= PermissionSet.PERMISSION_DIRECT_PUBLISH;
							}
							break;
						default :
							// ignore
							break;
					}
				}
			}
		}
	}
	
	static public class CategoryBean {
		String id;
		String path;
		
		public String getId() {
			return id;
		}
		
		public void setId(String id) {
			this.id = id;
		}
		
		public String getPath() {
			return path;
		}
		
		public void setPath(String path) {
			this.path = path;
		}
}

}