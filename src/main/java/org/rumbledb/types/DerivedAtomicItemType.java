package org.rumbledb.types;

import org.apache.commons.collections.ListUtils;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.rumbledb.api.Item;
import org.rumbledb.context.Name;

import java.util.List;
import java.util.Set;

public class DerivedAtomicItemType implements ItemType {

    private final ItemType baseType, primitiveType;
    private final int typeTreeDepth;
    private final boolean isUserDefined;
    private final Name name;
    private final Item minInclusive, maxInclusive, minExclusive, maxExclusive;
    private final Integer minLength, length, maxLength, totalDigits, fractionDigits;
    private final List<String> constraints;
    private final List<Item> enumeration;
    private final TimezoneFacet explicitTimezone;
    private final DataType dataFrameType;

    DerivedAtomicItemType(Name name, ItemType baseType, ItemType primitiveType, Facets facets, DataType dataFrameType){
        this(name, baseType, primitiveType, facets, true, dataFrameType);
    }
    // TODO : turn builtin derived atomic types into this class

    private DerivedAtomicItemType(Name name, ItemType baseType, ItemType primitiveType, Facets facets, boolean isUserDefined, DataType dataFrameType) {
        // TODO : check in item factory that: name not already used or invalid, facets are correct and allowed according to baseType
        this.name = name;
        this.baseType = baseType;
        this.primitiveType = primitiveType;
        this.isUserDefined = isUserDefined;
        this.typeTreeDepth = baseType.getTypeTreeDepth() + 1;

        this.minInclusive = facets.getMinInclusive();
        this.maxInclusive = facets.getMaxInclusive();
        this.minExclusive = facets.getMinExclusive();
        this.maxExclusive = facets.getMaxExclusive();

        this.minLength = facets.getMinLength();
        this.length = facets.getLength();
        this.maxLength = facets.getMaxLength();
        this.totalDigits = facets.getTotalDigits();
        this.fractionDigits = facets.getFractionDigits();

        this.explicitTimezone = facets.getExplicitTimezone();

        this.constraints = facets.getConstraints();
        this.enumeration = facets.getEnumeration();

        this.dataFrameType = dataFrameType;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ItemType)) {
            return false;
        }
        return this.toString().equals(other.toString());
    }

    @Override
    public boolean isAtomicItemType() {
        return true;
    }

    @Override
    public boolean hasName() {
        return true;
    }

    @Override
    public Name getName() {
        return this.name;
    }

    @Override
    public int getTypeTreeDepth() {
        return this.typeTreeDepth;
    }

    @Override
    public boolean isStaticallyCastableAs(ItemType other) {
        // TODO: what about further restrictions like string without num from int?
        ItemType castFrom = this.baseType;
        while(castFrom.isUserDefined()){
            castFrom = castFrom.getBaseType();
        }
        ItemType castTo = other;
        while (castTo.isUserDefined()){
            castTo = castTo.getBaseType();
        }
        return castFrom.isStaticallyCastableAs(castTo);
    }

    @Override
    public boolean canBePromotedTo(ItemType other) {
        // TODO : how about restriction types
        if (other.equals(BuiltinTypesCatalogue.stringItem)) {
            return this.isSubtypeOf(BuiltinTypesCatalogue.stringItem) || this.isSubtypeOf(BuiltinTypesCatalogue.anyURIItem);
        }
        if (other.equals(BuiltinTypesCatalogue.doubleItem)) {
            return this.isNumeric();
        }
        return false;
    }

    @Override
    public boolean isUserDefined() {
        return this.isUserDefined;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public ItemType getPrimitiveType() {
        return this.primitiveType;
    }

    @Override
    public ItemType getBaseType() {
        return this.baseType;
    }

    @Override
    public Set<FacetTypes> getAllowedFacets() {
        return this.primitiveType.getAllowedFacets();
    }

    @Override
    public List<Item> getEnumerationFacet(){
        if(!this.getAllowedFacets().contains(FacetTypes.ENUMERATION)){
            throw new UnsupportedOperationException(this.toString() + " item type does not support the enumeration facet");
        }
        return this.enumeration == null ? this.baseType.getEnumerationFacet() : this.enumeration;
    }

    @Override
    public List<String> getConstraintsFacet(){
        if(!this.getAllowedFacets().contains(FacetTypes.CONSTRAINTS)){
            throw new UnsupportedOperationException(this.toString() + " item type does not support the constraints facet");
        }
        return ListUtils.union(this.baseType.getConstraintsFacet(), this.constraints);
    }

    @Override
    public Integer getMinLengthFacet(){
        if(!this.getAllowedFacets().contains(FacetTypes.MINLENGTH)){
            throw new UnsupportedOperationException(this.toString() + " item type does not support the minimum length facet");
        }
        return this.minLength == null ? this.baseType.getMinLengthFacet() : this.minLength;
    }

    @Override
    public Integer getLengthFacet(){
        if(!this.getAllowedFacets().contains(FacetTypes.LENGTH)){
            throw new UnsupportedOperationException(this.toString() + " item type does not support the length facet");
        }
        return this.length == null ? this.baseType.getLengthFacet() : this.length;
    }

    @Override
    public Integer getMaxLengthFacet(){
        if(!this.getAllowedFacets().contains(FacetTypes.MAXLENGTH)){
            throw new UnsupportedOperationException(this.toString() + " item type does not support the maximum length facet");
        }
        return this.maxLength == null ? this.baseType.getMaxLengthFacet() : this.maxLength;
    }

    @Override
    public Item getMinExclusiveFacet(){
        if(!this.getAllowedFacets().contains(FacetTypes.MINEXCLUSIVE)){
            throw new UnsupportedOperationException(this.toString() + " item type does not support the minimum exclusive facet");
        }
        return this.minExclusive == null ? this.baseType.getMinExclusiveFacet() : this.minExclusive;
    }

    @Override
    public Item getMinInclusiveFacet(){
        if(!this.getAllowedFacets().contains(FacetTypes.MININCLUSIVE)){
            throw new UnsupportedOperationException(this.toString() + " item type does not support the minimum inclusive facet");
        }
        return this.minInclusive == null ? this.baseType.getMinInclusiveFacet() : this.minInclusive;
    }

    @Override
    public Item getMaxExclusiveFacet(){
        if(!this.getAllowedFacets().contains(FacetTypes.MAXEXCLUSIVE)){
            throw new UnsupportedOperationException(this.toString() + " item type does not support the maximum exclusive facet");
        }
        return this.maxExclusive == null ? this.baseType.getMaxExclusiveFacet() : this.maxExclusive;
    }

    @Override
    public Item getMaxInclusiveFacet(){
        if(!this.getAllowedFacets().contains(FacetTypes.MAXINCLUSIVE)){
            throw new UnsupportedOperationException(this.toString() + " item type does not support the maximum inclusive facet");
        }
        return this.maxInclusive == null ? this.baseType.getMaxInclusiveFacet() : this.maxInclusive;
    }

    @Override
    public Integer getTotalDigitsFacet(){
        if(!this.getAllowedFacets().contains(FacetTypes.TOTALDIGITS)){
            throw new UnsupportedOperationException(this.toString() + " item type does not support the total digits facet");
        }
        return this.totalDigits == null ? this.baseType.getTotalDigitsFacet() : this.totalDigits;
    }

    @Override
    public Integer getFractionDigitsFacet(){
        if(!this.getAllowedFacets().contains(FacetTypes.FRACTIONDIGITS)){
            throw new UnsupportedOperationException(this.toString() + " item type does not support the fraction digits facet");
        }
        return this.fractionDigits == null ? this.baseType.getFractionDigitsFacet() : this.fractionDigits;
    }

    @Override
    public TimezoneFacet getExplicitTimezoneFacet(){
        if(!this.getAllowedFacets().contains(FacetTypes.EXPLICITTIMEZONE)){
            throw new UnsupportedOperationException(this.toString() + " item type does not support the explicit timezone facet");
        }
        return this.explicitTimezone == null ? this.baseType.getExplicitTimezoneFacet() : this.explicitTimezone;
    }

    @Override
    public String toString() {
        // TODO : Consider added facets restriction and base type
        return this.name.toString();
    }

    @Override
    public DataType toDataFrameType() {
        return this.dataFrameType != null ? this.dataFrameType : this.baseType.toDataFrameType();
    }
}
