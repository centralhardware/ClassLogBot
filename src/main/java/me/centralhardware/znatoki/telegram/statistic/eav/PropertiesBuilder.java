package me.centralhardware.znatoki.telegram.statistic.eav;

import io.vavr.control.Validation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.eav.types.Enumeration;
import org.apache.commons.lang3.tuple.Pair;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;

@RequiredArgsConstructor
public class PropertiesBuilder {

    private final List<PropertyDef> propertyDefs;
    @Getter
    private final List<Property> properties = new ArrayList<>();
    private PropertyDef current;

    public Optional<Pair<String, List<String>>> getNext(){
        if (propertyDefs.isEmpty()) return Optional.empty();

        current = propertyDefs.getFirst();
        propertyDefs.remove(current);

        return current.type() instanceof Enumeration?
                Optional.of(Pair.of(current.type().format(current.name(), current.isOptional()), List.of(current.enumeration()))):
                Optional.of(Pair.of(current.type().format(current.name(), current.isOptional()), Collections.emptyList()));
    }

    public List<String> getEnumeration(){
        return current.type() instanceof Enumeration?
                List.of(current.enumeration()) :
                Collections.emptyList();
    }

    public Validation<String, Void> validate(Update update){
        if (current.type() instanceof Enumeration){
            return current.type().validate(update, current.enumeration());
        } else {
            return current.type().validate(update);
        }
    }

    public boolean setProperty(Update value){
        if (value.hasMessage() && Objects.equals(value.getMessage().getText(), "/skip")) {
            properties.add(new Property(current.name(), current.type()));
            return true;
        }

        var content = current.type().extract(value);
        content.ifPresent(
                it -> {
                    properties.add(new Property(current.name(), current.type(),it));
                    current = null;
                }
        );
        return content.isPresent();
    }

}
