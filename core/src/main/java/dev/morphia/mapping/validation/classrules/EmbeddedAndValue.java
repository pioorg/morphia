package dev.morphia.mapping.validation.classrules;


import dev.morphia.annotations.Embedded;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.validation.ClassConstraint;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.Set;

/**
 * Ensures value() isn't used on @Embedded
 */
@SuppressWarnings("removal")
public class EmbeddedAndValue implements ClassConstraint {

    @Override
    public void check(Mapper mapper, EntityModel entityModel, Set<ConstraintViolation> ve) {

        if (entityModel.getEmbeddedAnnotation() != null && !entityModel.getEmbeddedAnnotation().value().equals(Mapper.IGNORED_FIELDNAME)) {
            ve.add(new ConstraintViolation(Level.FATAL, entityModel, getClass(),
                "@" + Embedded.class.getSimpleName()
                + " classes cannot specify a fieldName value(); this is on applicable on fields"));
        }
    }

}
