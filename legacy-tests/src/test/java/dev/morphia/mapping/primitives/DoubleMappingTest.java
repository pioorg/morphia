package dev.morphia.mapping.primitives;


import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.eq;

public class DoubleMappingTest extends TestBase {
    @Test
    public void testMapping() {
        getMapper().map(Doubles.class);
        final Doubles ent = new Doubles();
        ent.listWrapperArray.add(new Double[]{1.1, 2.2});
        ent.listPrimitiveArray.add(new double[]{2.0, 3.6, 12.4});
        ent.listWrapper.addAll(Arrays.asList(1.1, 2.2));
        ent.singlePrimitive = 100.0;
        ent.singleWrapper = 40.7;
        ent.primitiveArray = new double[]{5.0, 93.5};
        ent.wrapperArray = new Double[]{55.7, 16.2, 99.9999};
        ent.nestedPrimitiveArray = new double[][]{{42.0, 49152.0}, {5.0, 93.5}};
        ent.nestedWrapperArray = new Double[][]{{42.0, 49152.0}, {5.0, 93.5}};
        getDs().save(ent);

        final Doubles loaded = getDs().find(Doubles.class)
                                      .filter(eq("_id", ent.id))
                                      .first();
        Assert.assertNotNull(loaded.id);

        Assert.assertArrayEquals(ent.listWrapperArray.get(0), loaded.listWrapperArray.get(0));
        Assert.assertEquals(ent.listWrapper, loaded.listWrapper);
        Assert.assertArrayEquals(ent.listPrimitiveArray.get(0), loaded.listPrimitiveArray.get(0), 0.0);

        Assert.assertEquals(ent.singlePrimitive, loaded.singlePrimitive, 0);
        Assert.assertEquals(ent.singleWrapper, loaded.singleWrapper, 0);

        Assert.assertArrayEquals(ent.primitiveArray, loaded.primitiveArray, 0.0);
        Assert.assertArrayEquals(ent.wrapperArray, loaded.wrapperArray);
        Assert.assertArrayEquals(ent.nestedPrimitiveArray, loaded.nestedPrimitiveArray);
        Assert.assertArrayEquals(ent.nestedWrapperArray, loaded.nestedWrapperArray);
    }

    @Entity
    private static class Doubles {
        private final List<Double[]> listWrapperArray = new ArrayList<>();
        private final List<double[]> listPrimitiveArray = new ArrayList<>();
        private final List<Double> listWrapper = new ArrayList<>();
        @Id
        private ObjectId id;
        private double singlePrimitive;
        private Double singleWrapper;
        private double[] primitiveArray;
        private Double[] wrapperArray;
        private double[][] nestedPrimitiveArray;
        private Double[][] nestedWrapperArray;
    }
}
