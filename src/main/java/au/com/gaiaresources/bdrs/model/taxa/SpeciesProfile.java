/**
 *
 */
package au.com.gaiaresources.bdrs.model.taxa;

import java.util.*;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Store;

import au.com.gaiaresources.bdrs.annotation.CompactAttribute;
import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import au.com.gaiaresources.bdrs.model.index.IndexingConstants;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

/**
 * @author timo
 *              
 */
@Entity
@FilterDef(name=PortalPersistentImpl.PORTAL_FILTER_NAME, parameters=@ParamDef( name="portalId", type="integer" ) )
@Filter(name=PortalPersistentImpl.PORTAL_FILTER_NAME, condition=":portalId = PORTAL_ID")
@Table(name = "SPECIES_PROFILE")
@AttributeOverride(name = "id", column = @Column(name = "SPECIES_PROFILE_ID"))
public class SpeciesProfile extends PortalPersistentImpl {
    public static final String SPECIES_PROFILE_IMAGE = "profile_img";
    public static final String SPECIES_PROFILE_IMAGE_40x40 = "profile_img_40x40";
    public static final String SPECIES_PROFILE_IMAGE_32x32 = "profile_img_32x32";
    public static final String SPECIES_PROFILE_IMAGE_16x16 = "profile_img_16x16";
    
    public static final String SPECIES_PROFILE_IMAGE_CREDIT = "profile_img_credit";
    public static final String SPECIES_PROFILE_THUMBNAIL = "thumb";
    public static final String SPECIES_PROFILE_THUMBNAIL_SMALL = "thumb_small";
    public static final String SPECIES_PROFILE_TEXT = "text";
    
    public static final String SPECIES_PROFILE_MAP = "map";
    public static final String SPECIES_PROFILE_MAP_40x40 = "map_40x40";
    public static final String SPECIES_PROFILE_MAP_THUMB = "map_thumb";
    
    public static final String SPECIES_PROFILE_AUDIO = "audio";
    public static final String SPECIES_PROFILE_AUDIO_CREDIT = "audio_credit";
    public static final String SPECIES_PROFILE_IMAGE_MEDIUM = "profile_img_med";
    
    public static final String SPECIES_PROFILE_SILHOUETTE = "silhouette";
    public static final String SPECIES_PROFILE_SILHOUETTE_40x40 = "silhouette_40x40";
    
    public static final String SPECIES_PROFILE_STATUS = "status";
    //Note we need to be careful not to confuse this with commonName in IndicatorSpecies
    public static final String SPECIES_PROFILE_COMMONNAME = "common_name";
    public static final String SPECIES_PROFILE_IDENTIFIER = "identifier";
    public static final String SPECIES_PROFILE_CLIMATEWATCH = "climatewatch";
    
    public static final String SPECIES_PROFILE_PUBLICATION = "publication";
    public static final String SPECIES_PROFILE_SCIENTIFICNAME = "scientificname";
    public static final String SPECIES_PROFILE_SOURCE = "source";
    
    public static final Map<String, String> SPECIES_PROFILE_TYPE_VALUES;
    public static final List<String> SPECIES_PROFILE_FILE_TYPE_VALUES;
    private static final String[] SPECIES_PROFILE_IMG_TYPE_VALUES;
    private static final String[] SPECIES_PROFILE_IMG_THUMB_TYPE_VALUES;
    
    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put(SPECIES_PROFILE_IMAGE, "Image");
        map.put(SPECIES_PROFILE_IMAGE_MEDIUM, "Image Medium");
        map.put(SPECIES_PROFILE_IMAGE_40x40, "Image 40x40");
        map.put(SPECIES_PROFILE_IMAGE_32x32, "Image 32x32");
        map.put(SPECIES_PROFILE_IMAGE_16x16, "Image 16x16");
        
        map.put(SPECIES_PROFILE_THUMBNAIL, "Thumbnail");
        map.put(SPECIES_PROFILE_THUMBNAIL_SMALL, "Thumbnail Small");
        
        map.put(SPECIES_PROFILE_TEXT, "Text");
        
        map.put(SPECIES_PROFILE_MAP, "Map");
        map.put(SPECIES_PROFILE_MAP_THUMB, "Map Thumbnail");
        map.put(SPECIES_PROFILE_MAP_40x40, "Map 40x40");
        
        map.put(SPECIES_PROFILE_AUDIO, "Audio");
        
        map.put(SPECIES_PROFILE_SILHOUETTE, "Silhouette");
        map.put(SPECIES_PROFILE_SILHOUETTE_40x40, "Silhouette 40x40");
        
        map.put(SPECIES_PROFILE_STATUS, "Status");
        map.put(SPECIES_PROFILE_COMMONNAME, "Common Name");
        map.put(SPECIES_PROFILE_IDENTIFIER, "Identifier");
        map.put(SPECIES_PROFILE_PUBLICATION, "Publication");
        map.put(SPECIES_PROFILE_SCIENTIFICNAME, "Scientific Name");
        map.put(SPECIES_PROFILE_SOURCE, "Source");
        
        SPECIES_PROFILE_TYPE_VALUES = Collections.unmodifiableMap(map);
        
        ArrayList<String> fileTypes = new ArrayList<String>();
        fileTypes.add(SPECIES_PROFILE_IMAGE);
        fileTypes.add(SPECIES_PROFILE_IMAGE_40x40);
        fileTypes.add(SPECIES_PROFILE_IMAGE_32x32);
        fileTypes.add(SPECIES_PROFILE_IMAGE_16x16);
        fileTypes.add(SPECIES_PROFILE_MAP);
        fileTypes.add(SPECIES_PROFILE_MAP_40x40);
        fileTypes.add(SPECIES_PROFILE_SILHOUETTE);
        fileTypes.add(SPECIES_PROFILE_SILHOUETTE_40x40);
        fileTypes.add(SPECIES_PROFILE_AUDIO);
        Collections.sort(fileTypes);
        
        SPECIES_PROFILE_FILE_TYPE_VALUES = Collections.unmodifiableList(fileTypes);
        
        String[] imgTypes = {
                SPECIES_PROFILE_IMAGE,
                SPECIES_PROFILE_IMAGE_40x40,
                SPECIES_PROFILE_IMAGE_32x32,
                SPECIES_PROFILE_IMAGE_16x16,
                
                SPECIES_PROFILE_MAP,
                SPECIES_PROFILE_MAP_40x40,
                
                SPECIES_PROFILE_SILHOUETTE,
                SPECIES_PROFILE_SILHOUETTE_40x40,
                SPECIES_PROFILE_THUMBNAIL, // This is for source that have a custom thumbnail, eg ALA.
                SPECIES_PROFILE_THUMBNAIL_SMALL
            };
        
        Arrays.sort(imgTypes);
        SPECIES_PROFILE_IMG_TYPE_VALUES = imgTypes;
        
        String[] thumbTypes = {
                SPECIES_PROFILE_IMAGE_40x40,
                SPECIES_PROFILE_IMAGE_32x32,
                SPECIES_PROFILE_IMAGE_16x16,
                SPECIES_PROFILE_MAP_40x40,
                SPECIES_PROFILE_SILHOUETTE_40x40,
                SPECIES_PROFILE_THUMBNAIL, // This is for source that have a custom thumbnail, eg ALA.
                SPECIES_PROFILE_THUMBNAIL_SMALL
            };
        
        Arrays.sort(thumbTypes);
        SPECIES_PROFILE_IMG_THUMB_TYPE_VALUES = thumbTypes;
    }
	
    private String header;
    private String description;
    private String content;
    private String type;
    private Set<Metadata> metadata = new HashSet<Metadata>();

    @CompactAttribute
    @Column(name = "HEADER")
    @Field
    public String getHeader() {
        return header;
    }

    @CompactAttribute
    @Column(name = "DESCRIPTION", columnDefinition="TEXT")
    @Field
    public String getDescription() {
        return description;
    }

    @CompactAttribute
    @Column(name = "CONTENT", columnDefinition="TEXT")
    @Field(index = org.hibernate.search.annotations.Index.TOKENIZED, store = Store.YES, analyzer=@Analyzer(definition=IndexingConstants.FULL_TEXT_ANALYZER))
    public String getContent() {
        return content;
    }

    @CompactAttribute
    @Column(name = "TYPE")
    public String getType() {
        return type;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    // Many to many is a work around (read hack) to prevent a unique
    // constraint being applied on the metadata id.
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    public Set<Metadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(Set<Metadata> metadata) {
        this.metadata = metadata;
    }
    
    @Transient
    public boolean isFileType() {
        if(this.type == null) {
            return false;
        }
        return Collections.binarySearch(SPECIES_PROFILE_FILE_TYPE_VALUES, this.type) > -1;
    }
    
    @Transient
    public boolean isImgType() {
        if(this.type == null) {
            return false;
        }
        
        return Arrays.binarySearch(SpeciesProfile.SPECIES_PROFILE_IMG_TYPE_VALUES, this.type) > -1;
    }
    
    @Transient
    public boolean isThumbnailType() {
        if(this.type == null) {
            return false;
        }
        
        return Arrays.binarySearch(SpeciesProfile.SPECIES_PROFILE_IMG_THUMB_TYPE_VALUES, this.type) > -1;
    }
    
    @Transient
    public boolean isAudioType() {
        return this.type != null && SpeciesProfile.SPECIES_PROFILE_AUDIO.equals(this.type);
    }
    
    @Transient
    public boolean isScientificNameType() {
        return this.type != null && SpeciesProfile.SPECIES_PROFILE_SCIENTIFICNAME.equals(this.type);
    }

    @Transient
    public boolean isTextType() {
        return this.type != null && SpeciesProfile.SPECIES_PROFILE_TEXT.equals(this.type);
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!other.getClass().equals(this.getClass())) {
            return false;
        }
        
        SpeciesProfile that = (SpeciesProfile) other;
        
        return ((this.type == null && that.type == null) || (this.type != null && this.type.equals(that.type))) && 
               ((this.header == null && that.header == null) || (this.header != null && this.header.equals(that.header))) && 
               ((this.description == null && that.description == null) || (this.description != null && this.description.equals(that.description)));
    }
    
    @Override
    public int hashCode() {
        return super.hashCode();
    };
    
    @Transient
    public String getSource() {
        for(Metadata md : metadata) {
            if (md.getKey().equals(Metadata.TAXON_SOURCE)) {
                return md.getValue();
            }
        }
        return null;
    }
}
