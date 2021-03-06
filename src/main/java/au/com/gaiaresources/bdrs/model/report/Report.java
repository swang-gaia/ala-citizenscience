package au.com.gaiaresources.bdrs.model.report;

import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.model.python.AbstractPythonRenderable;
import au.com.gaiaresources.bdrs.model.report.impl.ReportView;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * Describes a readonly view of the data in the system.
 */
@Entity
@FilterDef(name = PortalPersistentImpl.PORTAL_FILTER_NAME, parameters = @ParamDef(name = "portalId", type = "integer"))
@Filter(name = PortalPersistentImpl.PORTAL_FILTER_NAME, condition = ":portalId = PORTAL_ID")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "REPORT")
@AttributeOverride(name = "id", column = @Column(name = "REPORT_ID"))
public class Report extends AbstractPythonRenderable {
    /**
     * The target directory that will contain the report after extraction.
     */
    public static final String REPORT_DIR = "report";

    private String name;
    private String description;
    private String iconFilename;
    private String userRole;
    private boolean active = true;

    private Set<ReportCapability> capabilities = new HashSet<ReportCapability>();
    private Set<ReportView> views = new HashSet<ReportView>();

    /**
     * Creates a new blank (and invalid) report.
     */
    public Report() {
    }

    /**
     * Creates a new report.
     *
     * @param name         the name of the new report.
     * @param description  a short description of the report.
     * @param iconFilename the relative path to the report icon.
     * @param active       true if this report is active, false otherwise.
     */
    public Report(String name, String description, String iconFilename,
                  boolean active) {
        super();
        this.name = name;
        this.description = description;
        this.iconFilename = iconFilename;
        this.active = active;
    }

    /**
     * @return the name
     */
    @Column(name = "NAME", nullable = false)
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    @Column(name = "DESCRIPTION", nullable = false)
    @Lob
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the active
     */
    @Column(name = "ACTIVE", nullable = false)
    public boolean isActive() {
        return active;
    }

    /**
     * Return the minimum level of user role required to access this report
     * @return
     */
    @Column(name = "USER_ROLE", nullable = true)
    public String getUserRole() {
        return userRole;
    }

    /**
     * Set the minimum level of user role required to access this report
     * @param userRole
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    /**
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * @return the iconFilename
     */
    @Column(name = "ICONFILENAME", nullable = false)
    public String getIconFilename() {
        return iconFilename;
    }

    /**
     * @param iconFilename the iconFilename to set
     */
    public void setIconFilename(String iconFilename) {
        this.iconFilename = iconFilename;
    }

    /**
     * Returns the ability of this report to consume data passed to it via the content function.
     *
     * @return the capabilities
     */
    @CollectionOfElements(targetElement = ReportCapability.class)
    @JoinTable(name = "REPORT_REPORT_CAPABILITY", joinColumns = @JoinColumn(name = "REPORT_ID"))
    @Column(name = "CAPABILITY", nullable = false)
    @Enumerated(EnumType.STRING)
    public Set<ReportCapability> getCapabilities() {
        return capabilities;
    }

    /**
     * Sets the ability of this report to consume data passed to it via the content function.
     *
     * @param capabilities the capabilities to set
     */
    public void setCapabilities(Set<ReportCapability> capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * Returns the views where this report shall be displayed.
     *
     * @return the locations where this report shall be displayed.
     */
    @CollectionOfElements(targetElement = ReportView.class)
    @JoinTable(name = "REPORT_REPORT_VIEW", joinColumns = @JoinColumn(name = "REPORT_ID"))
    @Column(name = "VIEW", nullable = false)
    @Enumerated(EnumType.STRING)
    public Set<ReportView> getViews() {
        return this.views;
    }

    /**
     * Sets the views where this report shall be available.
     *
     * @param views the pages where this report shall be available.
     */
    public void setViews(Set<ReportView> views) {
        this.views = views;
    }

    @Transient
    @Override
    public String getContentDir() {
        return Report.REPORT_DIR;
    }
}
