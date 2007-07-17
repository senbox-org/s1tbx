package com.bc.ceres.binding;

public interface Validator {
   void validateValue(ValueModel valueModel, Object value) throws ValidationException;
}
