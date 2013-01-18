/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.controller.transform.description;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ModelVersionRange;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.PathTransformation;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class TransformingDescription extends AbstractDescription implements TransformationDescription, ResourceTransformer, OperationTransformer {

    private final DiscardPolicy discardPolicy;
    private final List<TransformationDescription> children;
    private final Map<String, AttributeTransformationDescription> attributeTransformations;
    private final List<TransformationRule> rules = Collections.emptyList();
    private final Map<String, OperationTransformer> operationTransformers = new HashMap<String, OperationTransformer>();
    private final ResourceTransformer resourceTransfomer;

    public TransformingDescription(final PathElement pathElement, final PathTransformation pathTransformation,
                                   final DiscardPolicy discardPolicy,
                                   final ResourceTransformer resourceTransformer,
                                   final Map<String, AttributeTransformationDescription> attributeTransformations,
                                   final List<TransformationDescription> children) {
        super(pathElement, pathTransformation);
        this.children = children;
        this.discardPolicy = discardPolicy;
        this.resourceTransfomer = resourceTransformer;
        this.attributeTransformations = attributeTransformations;

        // TODO override more global operations?
        operationTransformers.put(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, new WriteAttributeTransformer());
        operationTransformers.put(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION, new UndefineAttributeTransformer());
    }
    @Override
    public OperationTransformer getOperationTransformer() {
        return this;
    }

    @Override
    public ResourceTransformer getResourceTransformer() {
        return this;
    }

    @Override
    public Map<String, OperationTransformer> getOperationTransformers() {
        return Collections.unmodifiableMap(operationTransformers);
    }

    @Override
    public List<TransformationDescription> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public OperationTransformer.TransformedOperation transformOperation(final TransformationContext ctx, final PathAddress address, final ModelNode operation) throws OperationFailedException {
        if(discardPolicy.discard(operation, address, ctx)) {
            return OperationTransformer.DISCARD.transformOperation(ctx, address, operation);
        }
        final Iterator<TransformationRule> iterator = rules.iterator();
        final ModelNode originalModel = operation.clone();
        originalModel.protect();
        final TransformationRule.OperationContext context = new TransformationRule.OperationContext(ctx, originalModel) {

            @Override
            void invokeNext(OperationTransformer.TransformedOperation transformedOperation) throws OperationFailedException {
                recordTransformedOperation(transformedOperation);
                if(iterator.hasNext()) {
                    final TransformationRule next = iterator.next();
                    // TODO hmm, do we need to change the address?
                    next.transformOperation(transformedOperation.getTransformedOperation(), address, this);
                }
            }
        };
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address.toModelNode());
        // Kick off the chain
        final TransformationRule first = new AttributeTransformationRule(attributeTransformations);
        first.transformOperation(operation, address, context);
        // Create the composite operation result
        return context.createOp();
    }

    @Override
    public void transformResource(final ResourceTransformationContext ctx, final PathAddress address, final Resource original) throws OperationFailedException {
        final ModelNode originalModel = original.getModel().clone();
        originalModel.protect();
        if(discardPolicy.discard(originalModel, address, ctx)) {
            return; // discard
        }
        final Iterator<TransformationRule> iterator = rules.iterator();
        final TransformationRule.ResourceContext context = new TransformationRule.ResourceContext(ctx, originalModel) {
            @Override
            void invokeNext(final Resource resource) throws OperationFailedException {
                if(iterator.hasNext()) {
                    final TransformationRule next = iterator.next();
                    next.tranformResource(resource, address, this);
                } else {
                    resourceTransfomer.transformResource(ctx, address, resource);
                }
            }
        };
        // Kick off the chain
        final TransformationRule rule = new AttributeTransformationRule(attributeTransformations);
        rule.tranformResource(original, address, context);
    }

    private class WriteAttributeTransformer implements OperationTransformer {

        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {

            final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
            final AttributeTransformationDescription description = attributeTransformations.get(attributeName);
            if(description == null) {
                return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
            }

            ModelNode attributeValue = operation.get(ModelDescriptionConstants.VALUE);


            // Process
            final ModelNode originalModel = operation.clone();
            TransformationRule.AbstractTransformationContext ctx = new TransformationRule.AbstractTransformationContext(context, originalModel) {
                @Override
                protected TransformationContext getContext() {
                    return super.getContext();
                }
            };
            originalModel.protect();
            //discard what can be discarded
            if (description.shouldDiscard(attributeValue, ctx)) {
                return OperationTransformer.DISCARD.transformOperation(context, address, operation);
            }

            //Check the rest of the model can be transformed
            final boolean reject = !description.checkAttributeValueIsValid(attributeValue, ctx);
            final OperationRejectionPolicy policy;
            if(reject) {
                policy = new OperationRejectionPolicy() {
                    @Override
                    public boolean rejectOperation(ModelNode preparedResult) {
                        return true;
                    }

                    @Override
                    public String getFailureDescription() {
                        return "";
                    }
                };
            } else {
                policy = DEFAULT_REJECTION_POLICY;
            }

            //Now transform the value
            description.convertValue(attributeValue, ctx);

            //Store the rename until we are done
            String newName = description.getNewName();
            if (newName != null) {
                operation.get(NAME).set(newName);
            }

            return new TransformedOperation(operation, policy, OperationResultTransformer.ORIGINAL_RESULT);
        }
    }

    static final ModelNode UNDEFINED = new ModelNode();

    private class UndefineAttributeTransformer implements OperationTransformer {
        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {

            final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
            final AttributeTransformationDescription description = attributeTransformations.get(attributeName);
            if(description == null) {
                return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
            }

            // Process
            final ModelNode originalModel = operation.clone();
            TransformationRule.AbstractTransformationContext ctx = new TransformationRule.AbstractTransformationContext(context, originalModel) {
                @Override
                protected TransformationContext getContext() {
                    return super.getContext();
                }
            };
            originalModel.protect();
            //discard what can be discarded
            if (description.shouldDiscard(UNDEFINED, ctx)) {
                return OperationTransformer.DISCARD.transformOperation(context, address, operation);
            }

            return new TransformedOperation(operation, DEFAULT_REJECTION_POLICY, OperationResultTransformer.ORIGINAL_RESULT);
        }
    }
}
