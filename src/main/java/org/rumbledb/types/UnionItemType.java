package org.rumbledb.types;

import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.rumbledb.context.Name;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UnionItemType implements ItemType {

    private static Set<FacetTypes> allowedFacets = new HashSet<>(Arrays.asList(FacetTypes.CONTENT));

    private final Name name;
    private final ItemType baseType;
    private final int typeTreeDepth;
    private final UnionContentDescriptor content;

    UnionItemType(Name name, ItemType baseType, UnionContentDescriptor content){
        this.name = name;
        this.baseType = baseType;
        this.typeTreeDepth = baseType.getTypeTreeDepth() + 1;
        this.content = content;
    }

    UnionItemType(Name name, UnionContentDescriptor content){
        this.name = name;
        this.baseType = BuiltinTypesCatalogue.item;
        this.typeTreeDepth = 1;
        this.content = content;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ItemType)) {
            return false;
        }
        return this.toString().equals(other.toString());
    }

    @Override
    public boolean isUnionType() {
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
    public ItemType getBaseType() {
        return this.baseType;
    }

    @Override
    public boolean isUserDefined() {
        return true;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public ItemType getPrimitiveType() {
        return BuiltinTypesCatalogue.item;
    }

    @Override
    public Set<FacetTypes> getAllowedFacets() {
        return allowedFacets;
    }

    @Override
    public UnionContentDescriptor getUnionContentFacet() {
        return this.content;
    }

    @Override
    public String toString() {
        // TODO : consider providing more info
        return this.name.toString();
    }

    @Override
    public DataType toDataFrameType() {
        return DataTypes.BinaryType;
    }
}
