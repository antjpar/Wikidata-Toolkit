package org.wikidata.wdtk.dumpfiles.constraint.builder;

/*
 * #%L
 * Wikidata Toolkit Dump File Handling
 * %%
 * Copyright (C) 2014 Wikidata Toolkit Developers
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.dumpfiles.constraint.model.ConstraintQualifiers;
import org.wikidata.wdtk.dumpfiles.constraint.template.Template;

/**
 * 
 * An object of this class is a builder of a 'Qualifiers' constraint.
 * 
 * @author Julian Mendez
 * 
 */
class ConstraintQualifiersBuilder implements ConstraintBuilder {

	/**
	 * Constructs a new builder.
	 */
	public ConstraintQualifiersBuilder() {
	}

	@Override
	public ConstraintQualifiers parse(PropertyIdValue constrainedProperty,
			Template template) {
		ConstraintQualifiers ret = null;
		String listStr = template.getValue(ConstraintBuilderConstant.P_LIST);
		if ((constrainedProperty != null) && (listStr != null)) {
			ret = new ConstraintQualifiers(constrainedProperty,
					ConstraintMainBuilder.parseListOfPropertyValues(listStr));
		}
		return ret;
	}

}
