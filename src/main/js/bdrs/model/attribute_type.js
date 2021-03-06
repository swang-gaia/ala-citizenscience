if (window.bdrs === undefined) {
    window.bdrs = {};
}

if (bdrs.model === undefined) {
    bdrs.model = {};
}

if (bdrs.model.taxa === undefined) {
    bdrs.model.taxa = {};
}

if (bdrs.model.taxa.attributeType === undefined) {
    bdrs.model.taxa.attributeType = {};
}

if (bdrs.model.taxa.attributeType.code === undefined) {
    bdrs.model.taxa.attributeType.code = {};
}

if (bdrs.model.taxa.attributeType.value === undefined) {
    bdrs.model.taxa.attributeType.value = {};
}

/**
 * A Javascript representation of the AttributeType object.
 * @param [string] value the name of the enumeration.1
 * @param [string] code the type code of this attribute type.
 * @param [string] name the name of this attribute type.
 */
bdrs.model.taxa.attributeType.AttributeType = function(value, code, name) {
    this.value = value;
    this.code = code;
    this.name = name;

    // Registration with the various lookups.
    bdrs.model.taxa.attributeType.code[this.code] = this;
    bdrs.model.taxa.attributeType.value[this.value] = this;
    
    /**
     * @return [boolean] true if this is a file type, false otherwise.
     */
    this.isFileType = function() {
        return (bdrs.model.taxa.attributeType.value.IMAGE === this) || 
            (bdrs.model.taxa.attributeType.value.FILE === this) || 
            (bdrs.model.taxa.attributeType.value.AUDIO === this) ||
            (bdrs.model.taxa.attributeType.value.VIDEO === this);
    };
    
    /**
     * @return [boolean] true if this is an html type, false otherwise.
     */
    this.isHtmlType = function() {
        return (bdrs.model.taxa.attributeType.value.HTML === this) || 
            (bdrs.model.taxa.attributeType.value.HTML_NO_VALIDATION === this) || 
            (bdrs.model.taxa.attributeType.value.HTML_COMMENT === this) || 
            (bdrs.model.taxa.attributeType.value.HTML_HORIZONTAL_RULE === this);
    };
    
    this.isCensusMethodType = function() {
        return (bdrs.model.taxa.attributeType.value.CENSUS_METHOD_ROW === this) ||
               (bdrs.model.taxa.attributeType.value.CENSUS_METHOD_COL === this);
    }
    
    return this;
};

// Initialising the attribute types.
bdrs.model.taxa.attributeType.INTEGER = new bdrs.model.taxa.attributeType.AttributeType('INTEGER', 'IN', 'Integer');
bdrs.model.taxa.attributeType.INTEGER_WITH_RANGE = new bdrs.model.taxa.attributeType.AttributeType('INTEGER_WITH_RANGE', 'IR', 'Integer Range');
bdrs.model.taxa.attributeType.DECIMAL = new bdrs.model.taxa.attributeType.AttributeType('DECIMAL', 'DE', 'Decimal');
bdrs.model.taxa.attributeType.BARCODE = new bdrs.model.taxa.attributeType.AttributeType('BARCODE', 'BC', 'Bar Code');
bdrs.model.taxa.attributeType.REGEX = new bdrs.model.taxa.attributeType.AttributeType('REGEX', 'RE', 'Regular Expression');
bdrs.model.taxa.attributeType.DATE = new bdrs.model.taxa.attributeType.AttributeType('DATE', 'DA', 'Date');
bdrs.model.taxa.attributeType.TIME = new bdrs.model.taxa.attributeType.AttributeType('TIME', 'TM', 'Time');
bdrs.model.taxa.attributeType.STRING = new bdrs.model.taxa.attributeType.AttributeType('STRING', 'ST', 'Short Text');
bdrs.model.taxa.attributeType.STRING_AUTOCOMPLETE = new bdrs.model.taxa.attributeType.AttributeType('STRING_AUTOCOMPLETE', 'SA', 'Short Text (Auto Complete)');
bdrs.model.taxa.attributeType.TEXT = new bdrs.model.taxa.attributeType.AttributeType('TEXT', 'TA', 'Long Text');
bdrs.model.taxa.attributeType.HTML = new bdrs.model.taxa.attributeType.AttributeType('HTML', 'HL', 'HTML (Validated)');
bdrs.model.taxa.attributeType.HTML_NO_VALIDATION = new bdrs.model.taxa.attributeType.AttributeType('HTML_NO_VALIDATION', 'HV', 'HTML (Not Validated)');
bdrs.model.taxa.attributeType.HTML_COMMENT = new bdrs.model.taxa.attributeType.AttributeType('HTML_COMMENT', 'CM', 'Comment');
bdrs.model.taxa.attributeType.HTML_HORIZONTAL_RULE = new bdrs.model.taxa.attributeType.AttributeType('HTML_HORIZONTAL_RULE', 'HR', 'Horizontal Rule');
bdrs.model.taxa.attributeType.STRING_WITH_VALID_VALUES = new bdrs.model.taxa.attributeType.AttributeType('STRING_WITH_VALID_VALUES', 'SV', 'Selection');
bdrs.model.taxa.attributeType.SINGLE_CHECKBOX = new bdrs.model.taxa.attributeType.AttributeType('SINGLE_CHECKBOX', 'SC', 'Single Checkbox');
bdrs.model.taxa.attributeType.MULTI_CHECKBOX = new bdrs.model.taxa.attributeType.AttributeType('MULTI_CHECKBOX', 'MC', 'Multi Checkbox');
bdrs.model.taxa.attributeType.MULTI_SELECT = new bdrs.model.taxa.attributeType.AttributeType('MULTI_SELECT', 'MS', 'Multi Select');
bdrs.model.taxa.attributeType.IMAGE = new bdrs.model.taxa.attributeType.AttributeType('IMAGE', 'IM', 'Image File');
bdrs.model.taxa.attributeType.FILE = new bdrs.model.taxa.attributeType.AttributeType('FILE', 'FI', 'Data File');
bdrs.model.taxa.attributeType.AUDIO = new bdrs.model.taxa.attributeType.AttributeType('AUDIO', 'AU', 'Audio File');
bdrs.model.taxa.attributeType.VIDEO = new bdrs.model.taxa.attributeType.AttributeType('VIDEO', 'VI', 'Video File');
bdrs.model.taxa.attributeType.SPECIES = new bdrs.model.taxa.attributeType.AttributeType('SPECIES', 'SP', 'Species');
bdrs.model.taxa.attributeType.CENSUS_METHOD_ROW = new bdrs.model.taxa.attributeType.AttributeType('CENSUS_METHOD_ROW', 'CR', 'Census Method Row');
bdrs.model.taxa.attributeType.CENSUS_METHOD_COL = new bdrs.model.taxa.attributeType.AttributeType('CENSUS_METHOD_COL', 'CC', 'Census Method Column');


