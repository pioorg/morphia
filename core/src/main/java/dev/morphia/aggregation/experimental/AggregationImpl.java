package dev.morphia.aggregation.experimental;

import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.Expressions;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.stages.AddFields;
import dev.morphia.aggregation.experimental.stages.AutoBucket;
import dev.morphia.aggregation.experimental.stages.Bucket;
import dev.morphia.aggregation.experimental.stages.CollectionStats;
import dev.morphia.aggregation.experimental.stages.Count;
import dev.morphia.aggregation.experimental.stages.CurrentOp;
import dev.morphia.aggregation.experimental.stages.Facet;
import dev.morphia.aggregation.experimental.stages.GeoNear;
import dev.morphia.aggregation.experimental.stages.GraphLookup;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.IndexStats;
import dev.morphia.aggregation.experimental.stages.Limit;
import dev.morphia.aggregation.experimental.stages.Lookup;
import dev.morphia.aggregation.experimental.stages.Match;
import dev.morphia.aggregation.experimental.stages.Merge;
import dev.morphia.aggregation.experimental.stages.Out;
import dev.morphia.aggregation.experimental.stages.PlanCacheStats;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.aggregation.experimental.stages.Redact;
import dev.morphia.aggregation.experimental.stages.ReplaceRoot;
import dev.morphia.aggregation.experimental.stages.ReplaceWith;
import dev.morphia.aggregation.experimental.stages.Sample;
import dev.morphia.aggregation.experimental.stages.Skip;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.aggregation.experimental.stages.SortByCount;
import dev.morphia.aggregation.experimental.stages.Stage;
import dev.morphia.aggregation.experimental.stages.UnionWith;
import dev.morphia.aggregation.experimental.stages.Unset;
import dev.morphia.aggregation.experimental.stages.Unwind;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.reader.DocumentReader;
import dev.morphia.query.experimental.filters.Filter;
import dev.morphia.query.internal.MorphiaCursor;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @param <T>
 * @morphia.internal
 * @since 2.0
 */
public class AggregationImpl<T> implements Aggregation<T> {
    private final Datastore datastore;
    private final MongoCollection<T> collection;
    private final List<Stage> stages = new ArrayList<>();

    /**
     * Creates an instance.
     *
     * @param datastore  the datastore
     * @param collection the source collection
     * @morphia.internal
     */
    public AggregationImpl(Datastore datastore, MongoCollection<T> collection) {
        this.datastore = datastore;
        this.collection = collection;
    }

    @Override
    public Aggregation<T> addFields(AddFields fields) {
        stages.add(fields);
        return this;
    }

    @Override
    public Aggregation<T> autoBucket(AutoBucket bucket) {
        stages.add(bucket);
        return this;
    }

    @Override
    public Aggregation<T> bucket(Bucket bucket) {
        stages.add(bucket);
        return this;
    }

    @Override
    public Aggregation<T> collStats(CollectionStats stats) {
        stages.add(stats);
        return this;
    }

    @Override
    public Aggregation<T> count(String name) {
        stages.add(new Count(name));
        return this;
    }

    @Override
    public Aggregation<T> currentOp(CurrentOp currentOp) {
        stages.add(currentOp);
        return this;
    }

    @Override
    public <R> MorphiaCursor<R> execute(Class<R> resultType) {
        MongoCursor<R> cursor;
        EntityModel resultModel = datastore.getMapper().getEntityModel(resultType);
        if (resultModel != null && !resultType.equals(this.collection.getDocumentClass())) {
            MongoCollection<Document> collection = this.collection.withDocumentClass(Document.class);
            MongoCursor<Document> results = collection.aggregate(getDocuments()).iterator();
            String discriminator = datastore.getMapper().getEntityModel(this.collection.getDocumentClass()).getDiscriminatorKey();
            cursor = new MappingCursor<>(results, datastore.getMapper().getCodecRegistry().get(resultType), discriminator);
        } else {
            cursor = collection.aggregate(getDocuments(), resultType).iterator();
        }
        return new MorphiaCursor<>(cursor);
    }

    @Override
    public <R> MorphiaCursor<R> execute(Class<R> resultType, AggregationOptions options) {
        return new MorphiaCursor<>(options.apply(getDocuments(), collection, resultType)
                                          .iterator());
    }

    @Override
    public Aggregation<T> facet(Facet facet) {
        stages.add(facet);
        return this;
    }

    @Override
    public Aggregation<T> geoNear(GeoNear near) {
        stages.add(near);
        return this;
    }

    @Override
    public Aggregation<T> graphLookup(GraphLookup lookup) {
        stages.add(lookup);
        return this;
    }

    @Override
    public Aggregation<T> group(Group group) {
        stages.add(group);
        return this;
    }

    @Override
    public Aggregation<T> indexStats() {
        stages.add(IndexStats.of());
        return this;
    }

    @Override
    public Aggregation<T> limit(long limit) {
        stages.add(Limit.of(limit));
        return this;
    }

    @Override
    public Aggregation<T> lookup(Lookup lookup) {
        stages.add(lookup);
        return this;
    }

    @Override
    public Aggregation<T> match(Filter... filters) {
        stages.add(Match.on(filters));
        return this;
    }

    @Override
    public <M> void merge(Merge<M> merge) {
        stages.add(merge);
        collection.aggregate(getDocuments())
                  .toCollection();
    }

    @Override
    public <M> void merge(Merge<M> merge, AggregationOptions options) {
        stages.add(merge);
        Class<?> type = merge.getType() != null ? merge.getType() : Document.class;
        options.apply(getDocuments(), collection, type)
               .toCollection();
    }

    @Override
    public <O> void out(Out<O> out) {
        stages.add(out);
        collection.aggregate(getDocuments())
                  .toCollection();
    }

    @Override
    public <O> void out(Out<O> out, AggregationOptions options) {
        stages.add(out);
        Class<?> type = out.getType() != null ? out.getType() : Document.class;
        options.apply(getDocuments(), collection, type)
               .toCollection();
    }

    @Override
    public Aggregation<T> planCacheStats() {
        stages.add(PlanCacheStats.of());
        return this;
    }

    @Override
    public Aggregation<T> project(Projection projection) {
        stages.add(projection);
        return this;
    }

    @Override
    public Aggregation<T> redact(Redact redact) {
        stages.add(redact);
        return this;
    }

    @Override
    public Aggregation<T> replaceRoot(ReplaceRoot root) {
        stages.add(root);
        return this;
    }

    @Override
    public Aggregation<T> replaceWith(ReplaceWith with) {
        stages.add(with);
        return this;
    }

    @Override
    public Aggregation<T> sample(long sample) {
        stages.add(Sample.of(sample));
        return this;
    }

    @Override
    public Aggregation<T> skip(long skip) {
        stages.add(Skip.of(skip));
        return this;
    }

    @Override
    public Aggregation<T> sort(Sort sort) {
        stages.add(sort);
        return this;
    }

    @Override
    public Aggregation<T> sortByCount(Expression sort) {
        stages.add(SortByCount.on(sort));
        return this;
    }

    @Override
    public Aggregation<T> unionWith(Class<?> type, Stage first, Stage... others) {
        stages.add(new UnionWith(type, Expressions.toList(first, others)));
        return this;
    }

    @Override
    public Aggregation<T> unionWith(String collection, Stage first, Stage... others) {
        stages.add(new UnionWith(collection, Expressions.toList(first, others)));
        return this;
    }

    @Override
    public Aggregation<T> unset(Unset unset) {
        stages.add(unset);
        return this;
    }

    @Override
    public Aggregation<T> unwind(Unwind unwind) {
        stages.add(unwind);
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Document> getDocuments() {
        return stages.stream()
                     .map(s -> {
                         Codec codec = datastore.getMapper().getCodecRegistry().get(s.getClass());
                         DocumentWriter writer = new DocumentWriter();
                         codec.encode(writer, s, EncoderContext.builder().build());
                         return writer.getDocument();
                     })
                     .collect(Collectors.toList());
    }

    private static class MappingCursor<R> implements MongoCursor<R> {
        private final MongoCursor<Document> results;
        private final Codec<R> codec;
        private final String discriminator;

        <T> MappingCursor(MongoCursor<Document> results, Codec<R> codec, String discriminator) {
            this.results = results;
            this.codec = codec;
            this.discriminator = discriminator;
        }

        @Override
        public void close() {
            results.close();
        }

        @Override
        public boolean hasNext() {
            return results.hasNext();
        }

        @Override
        public R next() {
            return map(results.next());
        }

        @Override
        @Nullable
        public R tryNext() {
            return map(results.tryNext());
        }

        @Override
        @Nullable
        public ServerCursor getServerCursor() {
            return results.getServerCursor();
        }

        @Override
        public ServerAddress getServerAddress() {
            return results.getServerAddress();
        }

        private R map(Document next) {
            if (next != null) {
                next.remove(discriminator);
                return codec.decode(new DocumentReader(next), DecoderContext.builder().build());
            } else {
                return null;
            }
        }
    }

}
