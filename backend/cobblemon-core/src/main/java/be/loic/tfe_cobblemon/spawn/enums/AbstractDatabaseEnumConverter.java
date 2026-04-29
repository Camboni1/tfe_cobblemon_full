package be.loic.tfe_cobblemon.spawn.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public abstract class AbstractDatabaseEnumConverter<E extends Enum<E> & DatabaseValuedEnum>
    implements AttributeConverter<E, String>
{
    private final Class<E> enumClass;

    protected AbstractDatabaseEnumConverter(Class<E> enumClass) {
        this.enumClass = enumClass;
    }
    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.getDatabaseValue();
    }
    @Override
    public E convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        for (E constant : enumClass.getEnumConstants()) {
            if (constant.getDatabaseValue().equals(dbData)) {
                return constant;
            }
        }
        throw new IllegalArgumentException(
                "Unknown database value '" + dbData + "' for enum " + enumClass.getSimpleName()
        );
    }
}
