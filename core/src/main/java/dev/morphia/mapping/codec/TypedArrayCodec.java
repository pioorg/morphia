package dev.morphia.mapping.codec;

import dev.morphia.mapping.Mapper;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

class TypedArrayCodec implements Codec {
    private final Class type;
    private Codec codec;
    private final Mapper mapper;

    TypedArrayCodec(Class type, Mapper mapper) {
        this.type = type;
        this.mapper = mapper;
    }

    @Override
    public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
        writer.writeStartArray();
        int length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            getCodec().encode(writer, Array.get(value, i), encoderContext);
        }
        writer.writeEndArray();
    }

    @Override
    public Class getEncoderClass() {
        return Array.newInstance(type, 0).getClass();
    }

    private Codec getCodec() {
        if (codec == null) {
            codec = mapper.getCodecRegistry().get(type);
        }
        return codec;
    }

    @Override
    public Object decode(BsonReader reader, DecoderContext decoderContext) {
        reader.readStartArray();

        List list = new ArrayList<>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            list.add(getCodec().decode(reader, decoderContext));
        }

        reader.readEndArray();

        Object array = Array.newInstance(type, list.size());
        for (int i = 0; i < list.size(); i++) {
            Array.set(array, i, list.get(i));
        }

        return array;
    }

    @Override
    public String toString() {
        return format("%s<%s>", getClass().getName(), type.getSimpleName());
    }
}
