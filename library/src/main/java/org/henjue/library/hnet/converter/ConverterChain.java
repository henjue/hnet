package org.henjue.library.hnet.converter;

import java.lang.reflect.Type;
import java.util.LinkedList;

/**
 * Created by android on 2015/11/4.
 */
public class ConverterChain {
    private LinkedList<Converter> converterLinkedList = new LinkedList<>();

    public ConverterChain() {
    }

    public void add(Converter converter) {
        converterLinkedList.add(converter);
    }

    public Converter match(Type type) {
        for (int i = 0; i < converterLinkedList.size(); i++) {
            Converter converter = converterLinkedList.get(i);
            if (converter.match(type)) {
                return converter;
            }
        }

        StringConverter stringConverter = new StringConverter();
        stringConverter.match(type);
        return stringConverter;
    }
}
