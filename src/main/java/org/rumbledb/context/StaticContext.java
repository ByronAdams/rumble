/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors: Stefan Irimescu, Can Berker Cikis
 *
 */

package org.rumbledb.context;

import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.OurBadException;
import org.rumbledb.exceptions.SemanticException;
import org.rumbledb.expressions.ExecutionMode;
import org.rumbledb.types.FunctionSignature;
import org.rumbledb.types.SequenceType;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StaticContext implements Serializable, KryoSerializable {

    private static final long serialVersionUID = 1L;

    private transient Map<Name, InScopeVariable> inScopeVariables;
    private transient Map<String, String> staticallyKnownNamespaces;
    private transient UserDefinedFunctionExecutionModes userDefinedFunctionExecutionModes;
    private StaticContext parent;
    private URI staticBaseURI;
    private boolean emptySequenceOrderLeast;
    // TODO: should these be transient?
    private transient SequenceType contextItemStaticType;
    private transient Map<FunctionIdentifier, FunctionSignature> staticallyKnownFunctionSignatures;

    public StaticContext() {
        this.parent = null;
        this.staticBaseURI = null;
        this.inScopeVariables = null;
        this.userDefinedFunctionExecutionModes = null;
        this.emptySequenceOrderLeast = true;
        this.contextItemStaticType = null;
    }

    public StaticContext(URI staticBaseURI) {
        this.parent = null;
        this.staticBaseURI = staticBaseURI;
        this.inScopeVariables = new HashMap<>();
        this.userDefinedFunctionExecutionModes = null;
        this.emptySequenceOrderLeast = true;
        this.contextItemStaticType = null;
        this.staticallyKnownFunctionSignatures = new HashMap<>();
    }

    public StaticContext(StaticContext parent) {
        this.parent = parent;
        this.inScopeVariables = new HashMap<>();
        this.userDefinedFunctionExecutionModes = null;
        this.contextItemStaticType = null;
        this.staticallyKnownFunctionSignatures = new HashMap<>();
    }

    public StaticContext getParent() {
        return this.parent;
    }

    public URI getStaticBaseURI() {
        if (this.staticBaseURI != null) {
            return this.staticBaseURI;
        }
        if (this.parent != null) {
            return this.parent.getStaticBaseURI();
        }
        throw new OurBadException("Static context not set.");
    }

    public boolean isInScope(Name varName) {
        boolean found = false;
        if (this.inScopeVariables.containsKey(varName)) {
            return true;
        } else {
            StaticContext ancestor = this.parent;
            while (ancestor != null) {
                found = found || ancestor.getInScopeVariables().containsKey(varName);
                ancestor = ancestor.parent;
            }
        }
        return found;
    }

    private InScopeVariable getInScopeVariable(Name varName) {
        if (this.inScopeVariables.containsKey(varName)) {
            return this.inScopeVariables.get(varName);
        } else {
            StaticContext ancestor = this.parent;
            while (ancestor != null) {
                if (ancestor.inScopeVariables.containsKey(varName)) {
                    return ancestor.inScopeVariables.get(varName);
                }
                ancestor = ancestor.parent;
            }
            throw new SemanticException("Variable " + varName + " not in scope", null);
        }
    }

    public FunctionSignature getFunctionSignature(FunctionIdentifier identifier) {
        if (this.staticallyKnownFunctionSignatures.containsKey(identifier)) {
            return this.staticallyKnownFunctionSignatures.get(identifier);
        } else {
            StaticContext ancestor = this.parent;
            while (ancestor != null) {
                if (ancestor.staticallyKnownFunctionSignatures.containsKey(identifier)) {
                    return ancestor.staticallyKnownFunctionSignatures.get(identifier);
                }
                ancestor = ancestor.parent;
            }
            throw new SemanticException("function " + identifier + " not in scope", null);
        }
    }

    // replace the sequence type of an existing InScopeVariable, throws an error if the variable does not exists
    public void replaceVariableSequenceType(Name varName, SequenceType newSequenceType) {
        InScopeVariable variable = getInScopeVariable(varName);
        this.inScopeVariables.replace(
            varName,
            new InScopeVariable(varName, newSequenceType, variable.getMetadata(), variable.getStorageMode())
        );
    }

    public SequenceType getVariableSequenceType(Name varName) {
        return getInScopeVariable(varName).getSequenceType();
    }

    public ExceptionMetadata getVariableMetadata(Name varName) {
        return getInScopeVariable(varName).getMetadata();
    }

    public ExecutionMode getVariableStorageMode(Name varName) {
        return getInScopeVariable(varName).getStorageMode();
    }

    public void addVariable(
            Name varName,
            SequenceType type,
            ExceptionMetadata metadata,
            ExecutionMode storageMode
    ) {
        this.inScopeVariables.put(varName, new InScopeVariable(varName, type, metadata, storageMode));
    }

    public void addFunctionSignature(FunctionIdentifier identifier, FunctionSignature signature) {
        this.staticallyKnownFunctionSignatures.put(identifier, signature);
    }

    protected Map<Name, InScopeVariable> getInScopeVariables() {
        return this.inScopeVariables;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Static context with variables: ");
        this.inScopeVariables.keySet().forEach(a -> stringBuilder.append(a));
        stringBuilder.append("\n");
        if (this.userDefinedFunctionExecutionModes != null) {
            stringBuilder.append(this.userDefinedFunctionExecutionModes.toString());
        }
        if (this.parent != null) {
            stringBuilder.append("\nParent:");
            stringBuilder.append(this.parent.toString());
        }
        return stringBuilder.toString();
    }

    public boolean hasVariable(Name variableName) {
        if (this.inScopeVariables.containsKey(variableName)) {
            return true;
        }
        if (this.parent != null) {
            return this.parent.hasVariable(variableName);
        }
        return false;
    }

    public boolean bindNamespace(String prefix, String namespace) {
        if (this.staticallyKnownNamespaces == null) {
            this.staticallyKnownNamespaces = new HashMap<>();
        }
        if (!this.staticallyKnownNamespaces.containsKey(prefix)) {
            this.staticallyKnownNamespaces.put(prefix, namespace);
            return true;
        }
        return false;
    }

    public String resolveNamespace(String prefix) {
        if (this.staticallyKnownNamespaces != null) {
            if (this.staticallyKnownNamespaces.containsKey(prefix)) {
                return this.staticallyKnownNamespaces.get(prefix);
            } else {
                return null;
            }
        }
        if (this.parent != null) {
            return this.parent.resolveNamespace(prefix);
        }
        return null;
    }

    @Override
    public void write(Kryo kryo, Output output) {
        kryo.writeObjectOrNull(output, this.parent, StaticContext.class);
        kryo.writeObject(output, this.staticBaseURI);
        output.writeBoolean(this.emptySequenceOrderLeast);
    }

    @Override
    public void read(Kryo kryo, Input input) {
        this.parent = kryo.readObjectOrNull(input, StaticContext.class);
        this.staticBaseURI = kryo.readObject(input, URI.class);
        this.emptySequenceOrderLeast = input.readBoolean();
    }

    public void importModuleContext(StaticContext moduleContext, String targetNamespace) {
        for (Name name : moduleContext.inScopeVariables.keySet()) {
            if (name.getNamespace().contentEquals(targetNamespace)) {
                InScopeVariable variable = moduleContext.inScopeVariables.get(name);
                this.inScopeVariables.put(name, variable);
            }
        }
    }

    public void setUserDefinedFunctionsExecutionModes(
            UserDefinedFunctionExecutionModes staticallyKnownFunctionSignatures
    ) {
        if (this.parent != null) {
            throw new OurBadException("Statically known function signatures can only be stored in the module context.");
        }
        this.userDefinedFunctionExecutionModes = staticallyKnownFunctionSignatures;
    }

    public UserDefinedFunctionExecutionModes getUserDefinedFunctionsExecutionModes() {
        if (this.userDefinedFunctionExecutionModes != null) {
            return this.userDefinedFunctionExecutionModes;
        }
        if (this.parent != null) {
            return this.parent.getUserDefinedFunctionsExecutionModes();
        }
        throw new OurBadException("Statically known function signatures are not set up properly in static context.");
    }

    public void setEmptySequenceOrderLeast(boolean emptySequenceOrderLeast) {
        if (this.parent != null) {
            throw new OurBadException("Empty sequence ordering can only be set in the root static context.");
        }
        this.emptySequenceOrderLeast = emptySequenceOrderLeast;
    }

    public boolean isEmptySequenceOrderLeast() {
        if (this.parent != null) {
            return this.parent.isEmptySequenceOrderLeast();
        }
        return this.emptySequenceOrderLeast;
    }

    public static StaticContext createRumbleStaticContext() {
        try {
            return new StaticContext(new URI(Name.RUMBLE_NS));
        } catch (URISyntaxException e) {
            throw new OurBadException("Rumble namespace not recognized as a URI.");
        }
    }

    public StaticContext getModuleContext() {
        if (this.parent != null) {
            return this.parent.getModuleContext();
        }
        return this;
    }

    public SequenceType getContextItemStaticType() {
        return this.contextItemStaticType;
    }

    public void setContextItemStaticType(SequenceType contextItemStaticType) {
        this.contextItemStaticType = contextItemStaticType;
    }

    // replace all inScopeVariable in this context and all parents until [stopContext] with name not in [varToExclude]
    // with same variable with sequence type arity changed from 1 to + and form ? to *
    // used by groupBy cluse
    public void incrementArities(StaticContext stopContext, Set<Name> varToExclude) {
        this.inScopeVariables.replaceAll(
            (key, value) -> varToExclude.contains(value)
                ? value
                : new InScopeVariable(
                        value.getName(),
                        value.getSequenceType().incrementArity(),
                        value.getMetadata(),
                        value.getStorageMode()
                )
        );
        StaticContext current = this.parent;
        while (current != null && current != stopContext) {
            for (Map.Entry<Name, InScopeVariable> entry : current.inScopeVariables.entrySet()) {
                if (!this.inScopeVariables.containsKey(entry.getKey())) {
                    this.addVariable(
                        entry.getKey(),
                        entry.getValue().getSequenceType().incrementArity(),
                        entry.getValue().getMetadata(),
                        entry.getValue().getStorageMode()
                    );
                }
            }
            current = current.parent;
        }
    }
}
